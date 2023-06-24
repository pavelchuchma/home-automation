#! /bin/bash
rm -rf /usr/local/bin/homeAutomation/home-controller-0.1.0/
cd /usr/local/bin/homeAutomation/ || exit
tar -xvf home-controller-0.1.0.tar
sudo chmod 755 /usr/local/bin/homeAutomation/runJar-debug.sh
sudo /etc/init.d/homeAutomation restart