import hashlib
import json
import tempfile
import threading
import time
import unittest
import urllib.error
import urllib.request
from pathlib import Path
from server import AuthServer


class AuthIntegrationTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(dir=Path(__file__).parent)
        self.server = AuthServer(('127.0.0.1', 0), Path(self.temp.name) / 'auth.sqlite3')
        self.thread = threading.Thread(target=self.server.serve_forever, daemon=True)
        self.thread.start()
        self.base = 'http://127.0.0.1:' + str(self.server.server_port)

    def tearDown(self):
        self.server.shutdown()
        self.server.server_close()
        self.thread.join()
        self.temp.cleanup()

    def request(self, method, path, body=None, token=None):
        headers = {'Content-Type': 'application/json'}
        if token:
            headers['Authorization'] = 'Bearer ' + token
        req = urllib.request.Request(self.base + path, data=json.dumps(body).encode() if body is not None else None, headers=headers, method=method)
        try:
            response = urllib.request.urlopen(req)
        except urllib.error.HTTPError as error:
            response = error
        with response:
            return response.code, json.load(response)

    def register(self, email='juliana@example.com'):
        status, data = self.request('POST', '/auth/register', {'name': 'Juliana', 'email': email, 'password': 'ClaveSegura123!'})
        self.assertEqual(status, 201)
        return data['token']

    def test_register_login_restore_update_logout_delete(self):
        token = self.register()
        self.assertEqual(self.request('GET', '/auth/me', token=token)[1]['user']['name'], 'Juliana')
        self.assertEqual(self.request('GET', '/auth/me')[0], 401)
        self.assertEqual(self.request('POST', '/auth/login', {'email': 'juliana@example.com', 'password': 'incorrecta'})[0], 401)
        status, result = self.request('POST', '/auth/login', {'email': ' JULIANA@example.com ', 'password': 'ClaveSegura123!'})
        self.assertEqual(status, 200)
        token2 = result['token']
        self.assertNotEqual(token, token2)
        self.assertEqual(self.request('PATCH', '/auth/me', {'name': 'Juliana Nueva', 'email': 'nueva@example.com'}, token)[0], 200)
        self.assertEqual(self.request('GET', '/auth/me', token=token2)[1]['user']['email'], 'nueva@example.com')
        self.assertEqual(self.request('POST', '/auth/logout', {}, token)[0], 200)
        self.assertEqual(self.request('GET', '/auth/me', token=token)[0], 401)
        self.assertEqual(self.request('DELETE', '/auth/me', token=token2)[0], 200)
        self.assertEqual(self.request('GET', '/auth/me', token=token2)[0], 401)
        self.assertEqual(self.request('POST', '/auth/login', {'email': 'nueva@example.com', 'password': 'ClaveSegura123!'})[0], 401)

    def test_validation_duplicate_and_storage(self):
        token = self.register()
        status, _ = self.request('POST', '/auth/register', {'name': 'Juliana', 'email': 'JULIANA@example.com', 'password': 'ClaveSegura123!'})
        self.assertEqual(status, 409)
        for body in ({}, {'name': 'J', 'email': 'bad', 'password': '12345678'}, {'name': 'J', 'email': 'j@example.com', 'password': '123'}):
            self.assertEqual(self.request('POST', '/auth/register', body)[0], 400)
        with self.server.store.connect() as db:
            user = db.execute('SELECT * FROM users').fetchone()
            self.assertNotEqual(user['password_hash'], b'ClaveSegura123!')
            stored = db.execute('SELECT * FROM sessions').fetchone()
            self.assertEqual(stored['token_hash'], hashlib.sha256(token.encode()).hexdigest())
            db.execute('UPDATE sessions SET expires_at=?', (int(time.time()) - 1,))
        self.assertEqual(self.request('GET', '/auth/me', token=token)[0], 401)

    def test_rate_limit(self):
        for _ in range(10):
            self.assertEqual(self.request('POST', '/auth/login', {})[0], 400)
        self.assertEqual(self.request('POST', '/auth/login', {})[0], 429)

    def test_persistence_and_account_isolation(self):
        token = self.register()
        other = self.register('otra@example.com')
        from server import AuthStore
        restored = AuthStore(self.server.store.path)
        self.assertEqual(restored.account('GET', token, {})['user']['email'], 'juliana@example.com')
        self.request('DELETE', '/auth/me', token=other)
        self.assertEqual(self.request('GET', '/auth/me', token=token)[0], 200)


if __name__ == '__main__':
    unittest.main(verbosity=2)
