"""Backend de autenticación DeliTracker. Python 3.10+, sin dependencias externas.
Servidor para demostración local. Para despliegue público usar HTTPS y un servidor de producción.
"""
import argparse
import hashlib
import hmac
import json
import re
import secrets
import sqlite3
import threading
import time
from collections import defaultdict, deque
from contextlib import contextmanager
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path

ITERATIONS = 600_000
SESSION_SECONDS = 86400
DUMMY_SALT = b"delitracker-dummy"


class ApiError(Exception):
    def __init__(self, status, code):
        self.status, self.code = status, code


class AuthStore:
    def __init__(self, path):
        self.path = str(path)
        with self.connect() as db:
            db.executescript("""
                CREATE TABLE IF NOT EXISTS users (
                    id INTEGER PRIMARY KEY, name TEXT NOT NULL,
                    email TEXT NOT NULL UNIQUE, salt BLOB NOT NULL, password_hash BLOB NOT NULL
                );
                CREATE TABLE IF NOT EXISTS sessions (
                    token_hash TEXT PRIMARY KEY, user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                    expires_at INTEGER NOT NULL
                );
            """)

    @contextmanager
    def connect(self):
        db = sqlite3.connect(self.path, timeout=10)
        db.row_factory = sqlite3.Row
        db.execute("PRAGMA foreign_keys=ON")
        try:
            with db:
                yield db
        finally:
            db.close()

    @staticmethod
    def fields(body, password=False, name=False):
        email = body.get("email", "")
        pwd = body.get("password", "")
        display = body.get("name", "")
        if not isinstance(email, str) or len(email) > 254 or not re.fullmatch(r"[^\s@]+@[^\s@]+\.[^\s@]+", email.strip()):
            raise ApiError(400, "invalid_input")
        if password and (not isinstance(pwd, str) or not 8 <= len(pwd) <= 128):
            raise ApiError(400, "invalid_input")
        if name and (not isinstance(display, str) or not 1 <= len(display.strip()) <= 100):
            raise ApiError(400, "invalid_input")
        return display.strip() if isinstance(display, str) else "", email.strip().lower(), pwd

    @staticmethod
    def public(user):
        return {"id": user["id"], "name": user["name"], "email": user["email"]}

    @staticmethod
    def token_hash(token):
        return hashlib.sha256(token.encode()).hexdigest()

    def create_session(self, db, user):
        token = secrets.token_urlsafe(32)
        expires = int(time.time()) + SESSION_SECONDS
        db.execute("DELETE FROM sessions WHERE expires_at <= ?", (int(time.time()),))
        db.execute("INSERT INTO sessions VALUES (?, ?, ?)", (self.token_hash(token), user["id"], expires))
        return {"token": token, "expiresAt": expires, "user": self.public(user)}

    def register(self, body):
        name, email, password = self.fields(body, password=True, name=True)
        salt = secrets.token_bytes(16)
        hashed = hashlib.pbkdf2_hmac("sha256", password.encode(), salt, ITERATIONS)
        with self.connect() as db:
            try:
                cursor = db.execute("INSERT INTO users(name,email,salt,password_hash) VALUES (?,?,?,?)", (name, email, salt, hashed))
            except sqlite3.IntegrityError:
                raise ApiError(409, "email_exists")
            user = db.execute("SELECT * FROM users WHERE id=?", (cursor.lastrowid,)).fetchone()
            return self.create_session(db, user)

    def login(self, body):
        _, email, password = self.fields(body)
        if not isinstance(password, str) or not 1 <= len(password) <= 128:
            raise ApiError(401, "invalid_credentials")
        with self.connect() as db:
            user = db.execute("SELECT * FROM users WHERE email=?", (email,)).fetchone()
            hashed = hashlib.pbkdf2_hmac("sha256", password.encode(), user["salt"] if user else DUMMY_SALT, ITERATIONS)
            if user is None or not hmac.compare_digest(hashed, user["password_hash"]):
                raise ApiError(401, "invalid_credentials")
            return self.create_session(db, user)

    def authorized(self, db, token):
        user = db.execute("""SELECT u.* FROM users u JOIN sessions s ON u.id=s.user_id
            WHERE s.token_hash=? AND s.expires_at > ?""", (self.token_hash(token), int(time.time()))).fetchone()
        if user is None:
            raise ApiError(401, "unauthorized")
        return user

    def account(self, method, token, body):
        with self.connect() as db:
            user = self.authorized(db, token)
            if method == "PATCH":
                name, email, _ = self.fields(body, name=True)
                try:
                    db.execute("UPDATE users SET name=?,email=? WHERE id=?", (name, email, user["id"]))
                except sqlite3.IntegrityError:
                    raise ApiError(409, "email_exists")
                user = db.execute("SELECT * FROM users WHERE id=?", (user["id"],)).fetchone()
            if method == "DELETE":
                db.execute("DELETE FROM users WHERE id=?", (user["id"],))
                return {"ok": True}
            return {"user": self.public(user)}

    def logout(self, token):
        with self.connect() as db:
            db.execute("DELETE FROM sessions WHERE token_hash=?", (self.token_hash(token),))
        return {"ok": True}


class AuthServer(ThreadingHTTPServer):
    daemon_threads = True

    def __init__(self, address, path):
        self.store = AuthStore(path)
        self.attempts = defaultdict(deque)
        self.limit_lock = threading.Lock()
        super().__init__(address, Handler)

    def check_limit(self, ip):
        now = time.monotonic()
        with self.limit_lock:
            # Limita la memoria del control de intentos en una demostración prolongada.
            for key in list(self.attempts):
                if not self.attempts[key] or self.attempts[key][-1] <= now - 60:
                    del self.attempts[key]
            attempts = self.attempts[ip]
            while attempts and attempts[0] <= now - 60:
                attempts.popleft()
            if len(attempts) >= 10:
                raise ApiError(429, "rate_limited")
            attempts.append(now)


class Handler(BaseHTTPRequestHandler):
    server_version = "DeliTrackerAuth/1"

    def setup(self):
        super().setup()
        self.connection.settimeout(15)

    def log_message(self, *_):
        pass  # Nunca registrar contraseñas ni cabeceras de autorización.

    def respond(self, status, data):
        payload = json.dumps(data, ensure_ascii=False).encode()
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(payload)))
        self.send_header("Cache-Control", "no-store")
        self.end_headers()
        self.wfile.write(payload)

    def body(self):
        try:
            length = int(self.headers.get("Content-Length", "0"))
            if not 0 <= length <= 8192:
                raise ApiError(413, "invalid_input")
            value = json.loads(self.rfile.read(length) or b"{}")
            if not isinstance(value, dict):
                raise ValueError()
            return value
        except (ValueError, UnicodeError):
            raise ApiError(400, "invalid_input")

    def dispatch(self):
        try:
            route = (self.command, self.path)
            store = self.server.store
            auth = self.headers.get("Authorization", "")
            token = auth[7:] if auth.startswith("Bearer ") else ""
            body = self.body() if self.command in ("POST", "PATCH") else {}
            if route == ("GET", "/health"):
                return self.respond(200, {"ok": True})
            if route in (("POST", "/auth/register"), ("POST", "/auth/login")):
                self.server.check_limit(self.client_address[0])
                result = store.register(body) if self.path.endswith("register") else store.login(body)
                return self.respond(201 if self.path.endswith("register") else 200, result)
            if self.path == "/auth/me" and self.command in ("GET", "PATCH", "DELETE"):
                return self.respond(200, store.account(self.command, token, body))
            if route == ("POST", "/auth/logout"):
                return self.respond(200, store.logout(token))
            raise ApiError(404, "not_found")
        except ApiError as error:
            self.respond(error.status, {"error": error.code})
        except (sqlite3.Error, OSError):
            self.respond(500, {"error": "server_error"})

    do_GET = do_POST = do_PATCH = do_DELETE = dispatch


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--host", default="127.0.0.1")
    parser.add_argument("--port", type=int, default=8080)
    parser.add_argument("--db", default=str(Path(__file__).with_name("auth.sqlite3")))
    args = parser.parse_args()
    server = AuthServer((args.host, args.port), args.db)
    print(f"DeliTracker autenticación: http://{args.host}:{args.port}", flush=True)
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass
    finally:
        server.server_close()
