#TODO
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

