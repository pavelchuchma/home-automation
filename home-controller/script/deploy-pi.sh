host=pi
version=0.1.0
scp ../app/build/distributions/home-controller-$version.tar runJar-debug.sh pi@$host:/usr/local/bin/homeAutomation/ || exit
ssh -l pi $host < finish-deploy.sh
