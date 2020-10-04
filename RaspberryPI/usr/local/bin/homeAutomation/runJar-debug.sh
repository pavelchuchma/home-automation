#! /bin/bash

sudo ln -s /dev/ttyAMA0 /dev/ttyS80

cd $(dirname $0)
prefix=x$(date +%Y%m%d-%H%M%S)
cp ./out/app.log ./out/$prefix-app.log
cp ./out/messages.log ./out/$prefix-messages.log

#javaOptions="-XX:+UnlockCommercialFeatures -XX:+FlightRecorder -Djava.rmi.server.hostname=10.10.0.150 -Dcom.sun.management.jmxremote.port=6077 -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false"

exec authbind --deep java -Xms16m -Xmx32m $javaOptions -Djava.library.path=/usr/lib/jni -cp ./homeAutomation.jar:/usr/share/java/RXTXcomm.jar -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=6070 Main


