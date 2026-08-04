#! /bin/bash

sudo ln -s /dev/ttyAMA0 /dev/ttyS80

cd $(dirname $0)
#prefix=x$(date +%Y%m%d-%H%M%S)
#cp ./out/app.log ./out/$prefix-app.log
#cp ./out/messages.log ./out/$prefix-messages.log

#javaOptions="-XX:+UnlockCommercialFeatures -XX:+FlightRecorder -Djava.rmi.server.hostname=192.168.68.150 -Dcom.sun.management.jmxremote.port=6077 -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false"

#exec authbind --deep java -Xms16m -Xmx32m $javaOptions -Djava.library.path=/usr/lib/jni -cp ./homeAutomation.jar -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=6070 Main

# Cap the JVM heap: without -Xmx the JVM ergonomics allow ~1/4 of RAM (~512MB
# on the 2GB Pi), which contributed to the 2026-08-03 memory livelock
# (see RaspberryPI/Install.md). Gradle start script picks up JAVA_OPTS.
export JAVA_OPTS="-Xms32m -Xmx128m"

exec authbind --deep ./home-controller-0.1.0/bin/home-controller
