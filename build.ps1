# Jimm Chat build wrapper, changed 2026-10-18. GPL-2.0-or-later; no warranty.
$ErrorActionPreference='Stop'
python (Join-Path $PSScriptRoot 'build.py') @args
if($LASTEXITCODE -ne 0) { throw 'Jimm Chat build failed' }
