#! /bin/bash

sudo ln -s /dev/ttyAMA0 /dev/ttyS80

cd $(dirname $0)
#prefix=x$(date +%Y%m%d-%H%M%S)
#cp ./out/app.log ./out/$prefix-app.log
#cp ./out/messages.log ./out/$prefix-messages.log

#javaOptions="-XX:+UnlockCommercialFeatures -XX:+FlightRecorder -Djava.rmi.server.hostname=192.168.68.150 -Dcom.sun.management.jmxremote.port=6077 -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false"

exec authbind --deep ./home-controller-0.1.0/bin/home-controller
