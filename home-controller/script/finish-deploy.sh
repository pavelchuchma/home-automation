#! /bin/bash
rm -rf /usr/local/bin/homeAutomation/home-controller-0.1.0/
cd /usr/local/bin/homeAutomation/
tar -xvf home-controller-0.1.0.tar
sudo /etc/init.d/homeAutomation restart