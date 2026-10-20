#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")"
[ -x node_modules/.bin/firebase ] || npm ci
npm run firebase:local
