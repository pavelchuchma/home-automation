#! /bin/sh

sudo ln -s /dev/ttyAMA0 /dev/ttyS80
ls >out/ls.txt
cd $(dirname $0)
exec authbind --deep java -Xms16m -Xmx32m -Djava.library.path=/usr/lib/jni -cp ./homeAutomation.jar:/usr/share/java/RXTXcomm.jar -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=6070 Main