/**
 *  Lockitron
 *
 *  Copyright 2014 Donald Kirker
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
metadata {
	// Automatically generated. Make future change here.
	definition (name: "Lockitron", namespace: "com.openmobl.device.lockitron", author: "Donald Kirker", oauth: true) {
		//capability "Lock Codes"
		capability "Battery"
		/*capability "Acceleration Sensor"
		capability "Configuration"
		capability "Signal Strength"*/
		capability "Polling"
		capability "Refresh"
		capability "Lock"
	}

	simulator {
		// TODO: define status and reply messages here
	}

	tiles {
		standardTile("toggle", "device.lock", width: 2, height: 2) {
			state "locked", label:'locked', action:"lock.unlock", icon:"st.locks.lock.locked", backgroundColor:"#79b821", nextState:"unlocking"
			state "unlocked", label:'unlocked', action:"lock.lock", icon:"st.locks.lock.unlocked", backgroundColor:"#ffffff", nextState:"locking"
			state "unknown", label:"unknown", action:"lock.lock", icon:"st.locks.lock.unknown", backgroundColor:"#ffffff", nextState:"locking"
			state "locking", label:'locking', icon:"st.locks.lock.locked", backgroundColor:"#79b821"
			state "unlocking", label:'unlocking', icon:"st.locks.lock.unlocked", backgroundColor:"#ffffff"
		}
		standardTile("lock", "device.lock", inactiveLabel: false, decoration: "flat") {
			state "default", label:'lock', action:"lock.lock", icon:"st.locks.lock.locked", nextState:"locked"
		}
		standardTile("unlock", "device.lock", inactiveLabel: false, decoration: "flat") {
			state "default", label:'unlock', action:"lock.unlock", icon:"st.locks.lock.unlocked", nextState:"unlocked"
		}
		valueTile("battery", "device.battery", inactiveLabel: false, decoration: "flat") {
			state "battery", label:'${currentValue}% battery', unit:""
		}
		standardTile("refresh", "device.lock", inactiveLabel: false, decoration: "flat") {
			state "default", label:'', action:"refresh.refresh", icon:"st.secondary.refresh"
		}

		main "toggle"
		details(["toggle", "lock", "unlock", "battery", "refresh"])
	}
}

// parse events into attributes
def parse(String description) {
	log.debug "Parsing '${description}'"
	// TODO: handle 'battery' attribute
	// TODO: handle 'power' attribute
	// TODO: handle 'acceleration' attribute
	// TODO: handle 'lock' attribute
	// TODO: handle 'lqi' attribute
	// TODO: handle 'rssi' attribute
	// TODO: handle 'lock' attribute

}

// handle commands
def poll() {
	log.debug "devicetype poll"
    def results = parent.pollChild(this)
    generateEvents(results)
}

/*def configure() {
	log.debug "Executing 'configure'"
	// TODO: handle 'configure' command
}*/

def refresh() {
	log.debug "devicetype refresh"
    poll()
}

def lock() {
	log.debug "devicetype lock"
    parent.lock(this)
    refresh()
}

def unlock() {
	log.debug "devicetype unlock"
    parent.unlock(this)
    refresh()
}

/*def updateCodes() {
	log.debug "Executing 'updateCodes'"
	// TODO: handle 'updateCodes' command
}

def setCode() {
	log.debug "Executing 'setCode'"
	// TODO: handle 'setCode' command
}

def deleteCode() {
	log.debug "Executing 'deleteCode'"
	// TODO: handle 'deleteCode' command
}

def requestCode() {
	log.debug "Executing 'requestCode'"
	// TODO: handle 'requestCode' command
}

def reloadAllCodes() {
	log.debug "Executing 'reloadAllCodes'"
	// TODO: handle 'reloadAllCodes' command
}*/

def generateEvents(Map eventData)
{
	if (eventData) {
    	eventData.each { name, value -> 
        
        	log.debug "$name is $value"
            
        	if (name == "battery_voltage") {
            	def percentage = computeBatteryLevel(Float.parseFloat(value));
                
            	sendEvent(
					name: "device.battery",
					value: percentage,
                    displayed: true,
                    isStateChange: true)
            } else if (name == "state") {
            	def realState = value + "ed"
                
            	sendEvent(
					name: "device.lock",
					value: realState,
					unit: "",
                    displayed: true,
                    isStateChange: true)
            }
        }
    }
}

// Currently not necessarily precise!
def computeBatteryLevel(volts)
{
    def value = 100
    
	if (volts <= 6) {
		def minVolts = 4.5
    	def maxVolts = 6.0
		def pct = (volts - minVolts) / (maxVolts - minVolts)
		value = Math.min(100, (int) pct * 100)
	}
    
    return value
}

