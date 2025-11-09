# Setup on Raspberry PI
1. Get Headless Raspbian
   * setup ssh key 
   * hostname: `pi.local`
2. **Java 21** `sudo apt install openjdk-21-jre`
3. install **authbind** to allow to user pi access to port 80
   ```shell
   sudo apt-get install authbind
   sudo touch /etc/authbind/byport/80
   sudo chmod 500 /etc/authbind/byport/80
   sudo chown pi /etc/authbind/byport/80
   ```
4. Setup Tailscale VPN: https://tailscale.com/kb/1627/install-rpi-trixie
   
5. Setup daemon
   * copy `RaspberryPI/etc/init.d/homeAutomation` to `/etc/init.d/`
   * `sudo chmod 755 /etc/init.d/homeAutomation`
   * auto start after reboot: `sudo update-rc.d homeAutomation defaults`
5. Create app directory
   ```shell
   sudo mkdir /usr/local/bin/homeAutomation/
   sudo chown pi /usr/local/bin/homeAutomation/
   ```
6. Deploy app by running `home-controller/script/deploy-pi.sh`
   * running console: http://pi.local/
   * app logs: `/usr/local/bin/homeAutomation/out/`

# TODO
* [x] virtualize `configuration-pi.js' instead of string replace in servlet
* [x] refactor lights page
* [x] **BUG:** Broken stylesheets on http://pi/lights, louvers
* [x] refactor louvers page
* [x] reincarnate `generateMessageType.bat`
* [x] NodeInfoCollector: Convert node list to int->NodeInfo map
* [x] remove `String.format()` from debugs
* [x] refactor IOnOfActor.switchOn percent parameter from int to double
* [x] !!! refactor `LouverControllerImpl.setPosition()` percent parameter from int to double
* [x] use `Validate.inclusiveBetween` whenever possible
* [x] Fix Bzucak & Garaz actions
* [x] Merge Nodes & System pages including testNode
* [x] use Options to store js baseUrl value
* [x] Rename Node.Listener method: Replace ButtonUp/Down by State High/Low...
* [x] Rename SwitchListener to something more generic
* [x] Replace AbstractActionWithoutActor by GenericCodeActor
* [x] BUG: Mem leak - Missing periodic message log clean in NodeInfo
* [x] Use executors instead of explicit thread creation. See todo in SwitchListener
* [x] Add Solax inverter support
* [x] Persistent Louvers and AirValve state (survive app restart)
* [ ] Add Solax web control (change configuration)
* [ ] Merge GenericOutputDevice and GenericInputDevice into new GenericDevice
* [ ] Denon AVR support (using denon4j)
* [x] Lights in basement and bathroom heater on web app
* [ ] refactor web config
* [ ] fix tests
* [ ] water pump report
* [ ] deactivate PIR after manual switch off
* [ ] Alexa support
* [ ] PIN home entry
* [ ] control lights in bathroom by window switch
* [ ] refactor hvac control on web (more fan speeds, combine with valve control)
* [ ] face recognition
* [ ] BUG: Don't allow to map multiple louvers to single relay `addLouversController()` & `Relay16BoardDevice` 

