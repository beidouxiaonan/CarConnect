param([string]$PythonPath='D:/Program Files/Python3.13.13/python.exe')
$ErrorActionPreference='Stop'
& "$PSScriptRoot/easyplay-integration17/build.ps1" -PythonPath $PythonPath
if($LASTEXITCODE -ne 0){throw "Current CarConnect build failed: $LASTEXITCODE"}
