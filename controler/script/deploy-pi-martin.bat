set host=10.0.0.2
pscp -2 -i N:\Data\cert\sshKey\sshKey.private.ppk %~dp0..\out\homeAutomation.jar pi@%host%:/usr/local/bin/homeAutomation/
@if errorlevel 1 pause
putty.exe -ssh -2 -i N:\Data\cert\sshKey\sshKey.private.ppk -l pi -m restart.sh %host%
@if errorlevel 1 pause
