set host=pi
set logFile=.restart-%host%.log
if exist %logFile% del %logFile%
pscp -2 -i N:\Data\cert\sshKey\sshKey.private.ppk %~dp0..\app\build\distributions\home-controller-0.1.0.tar %~dp0runJar-debug.sh pi@%host%:/usr/local/bin/homeAutomation/ || pause
putty.exe -ssh -2 -i N:\Data\cert\sshKey\sshKey.private.ppk -l pi -sessionlog %logFile% -m finish-deploy.sh %host%
@type %logFile%
@del %logFile%
