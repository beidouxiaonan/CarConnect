param([string]$PythonPath='D:/Program Files/Python3.13.13/python.exe')
$ErrorActionPreference='Stop'
& "$PSScriptRoot/easyplay-sd8227-unified/build-unified.ps1" -PythonPath $PythonPath
if($LASTEXITCODE -ne 0){throw "SD8227 unified build failed: $LASTEXITCODE"}
