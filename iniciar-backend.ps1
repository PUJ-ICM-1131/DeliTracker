$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot
if (Get-Command py -ErrorAction SilentlyContinue) {
    & py -3 backend/server.py
} elseif (Get-Command python -ErrorAction SilentlyContinue) {
    & python backend/server.py
} else {
    throw 'Instala Python 3.10 o posterior para iniciar el backend.'
}
