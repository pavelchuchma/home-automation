:: putty.exe -ssh -2 -i N:\Data\cert\sshKey\sshKey.private.ppk -m test.command pi
:: putty.exe -ssh -2 -i N:\Data\cert\sshKey\sshKey.private.ppk -l pi -m test.command pi
putty.exe -ssh -2 -i N:\Data\cert\sshKey\sshKey.private.ppk -l pi -m restart.sh pi