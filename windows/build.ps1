# Created 2026-10-18. GPL-2.0-or-later.
param([switch]$Offline)
$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
$out=Join-Path $PSScriptRoot 'out'
New-Item -ItemType Directory -Force -Path $out | Out-Null
if($Offline){ & python (Join-Path $root 'build.py') --offline }else{ & python (Join-Path $root 'build.py') }
if($LASTEXITCODE -ne 0){throw 'Java build failed'}
& python (Join-Path $PSScriptRoot 'source-package.py')
if($LASTEXITCODE -ne 0){throw 'Source packaging failed'}
$compiler=Join-Path $env:WINDIR 'Microsoft.NET\Framework64\v4.0.30319\csc.exe'
if(!(Test-Path $compiler)){throw '.NET Framework 4.x compiler is required to build the Windows companion.'}
$argsList=@('/nologo','/target:winexe','/platform:anycpu','/optimize+',('/out:'+(Join-Path $out 'JimmChatSetup-0.2.0.exe')),'/r:System.Windows.Forms.dll','/r:System.Drawing.dll','/r:System.IO.Compression.dll','/r:System.IO.Compression.FileSystem.dll','/r:System.Web.Extensions.dll',('/resource:'+(Join-Path $root 'dist\JimmChat.jar')+',template.jar'),('/resource:'+(Join-Path $root 'dist\JimmChat.jad')+',template.jad'),('/resource:'+(Join-Path $out 'source.zip')+',source.zip'),('/resource:'+(Join-Path $root 'COPYING')+',COPYING'))
$sources=Get-ChildItem -LiteralPath $PSScriptRoot -Filter '*.cs' | ForEach-Object {$_.FullName}
& $compiler @argsList @sources
if($LASTEXITCODE -ne 0){throw 'Windows compilation failed'}
Write-Output (Join-Path $out 'JimmChatSetup-0.2.0.exe')
