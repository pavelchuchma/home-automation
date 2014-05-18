#! /bin/bash

sudo ln -s /dev/ttyAMA0 /dev/ttyS80
cd $(dirname $0)
ls >out/ls.txt

echo Before>>out/ls.txt
echo Before2>>out/ls.txt
exec authbind --deep java -Xms16m -Xmx32m -Djava.library.path=/usr/lib/jni -cp ./homeAutomation.jar:/usr/share/java/RXTXcomm.jar -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=6070 Main
echo After>>out/ls.txt