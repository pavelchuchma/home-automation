copy %~dp0..\out\homeAutomation.jar \\PI\root\usr\local\bin\homeAutomation\
@if errorlevel 1 pause
putty.exe -ssh -2 -i N:\Data\cert\sshKey\sshKey.private.ppk -l pi -m restart.sh pi
@if errorlevel 1 pause
