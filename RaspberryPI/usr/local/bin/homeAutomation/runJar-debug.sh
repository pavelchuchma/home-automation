#! /bin/bash

sudo ln -s /dev/ttyAMA0 /dev/ttyS80

cd $(dirname $0)
prefix=x$(date +%Y%m%d-%H%M%S)
cp ./out/app.log ./out/$prefix-app.log
cp ./out/messages.log ./out/$prefix-messages.log

exec authbind --deep java -Xms16m -Xmx32m -Djava.library.path=/usr/lib/jni -cp ./homeAutomation.jar:/usr/share/java/RXTXcomm.jar -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=6070 Main

