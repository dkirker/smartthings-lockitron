/**
 *  Lockitron (Connect)
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
definition(
    name: "Lockitron (Connect)",
    namespace: "com.openmobl.app.lockitron",
    author: "Donald Kirker",
    description: "Connect your SmartThings world to your Lockitron world.",
    category: "SmartThings Labs",
    iconUrl: "https://lockitron.com/assets/store/lockitron.png",
    iconX2Url: "https://lockitron.com/assets/store/lockitron.png",
    iconX3Url: "https://lockitron.com/assets/store/lockitron.png",
    oauth: true) {
	appSetting "clientId"
	appSetting "clientSecret"
}

preferences {
	page(name: "auth", title: "Lockitron", nextPage: "deviceList", content: "authPage", uninstall: true)
	page(name: "deviceList", title: "Lockitron", content: "deviceListPage", install: true)
}

mappings {
	path("/receiveCode") {
		action: [
			GET: "receiveCode",
            POST: "receiveCode"
		]
	}
	path("/complete") {
		action: [
			GET: "complete",
            POST: "complete"
		]
	}
}

def getChildNamespace() { return "com.openmobl.device.lockitron" }
def getChildName() { return "Lockitron" }
def getServerUrl() { return "https://graph.api.smartthings.com" }
// Authorise
def getAuthCodeUrl() { return "https://api.lockitron.com/oauth/authorize" }
def getAccessTokenUrl() { return "https://api.lockitron.com/oauth/token" }
def getClientId() { return appSettings.clientId }
def getClientSecret() { return appSettings.clientSecret }
// Lockitron API Endpoints
def getLockitronApiUrl() { return "https://api.lockitron.com" }
def getLocksPath() { return "/v2/locks" }
def getLockPath(lockId) { return getLocksPath() + "/${lockId}" }
def getLocksUrl() { return getLockitronApiUrl() + getLocksPath() }
def getLockUrl(lockId) { return getLockitronApiUrl() + getLockPath(lockId) }
def getLockCmdName() { return "lock" }
def getUnlockCmdName() { return "unlock" }

def installed()
{
	debugPrint("Installed with settings: ${settings}")

	// createAccessToken()
	initialize()
}

def updated()
{
	debugPrint("Updated with settings: ${settings}")

	unsubscribe()
	initialize()
}

def initialize()
{
	def devices = locks.collect { dni ->

		def d = getChildDevice(dni)

		if(!d) {
        	def lockName = atomicState.lockCache[dni] ? atomicState.lockCache[dni] : "Lockitron"

			d = addChildDevice(getChildNamespace(), getChildName(), dni, null, [label: lockName, completedSetup: true])
			debugPrint("created ${d.displayName} with id $dni")
		} else {
			debugPrint("found ${d.displayName} with id $dni already exists")
		}

		return d
	}
}

def lock(child)
{
	debugPrint("smartapp lock")
	def lockId = child.device.deviceNetworkId
	sendLockCommand(lockId, getLockCmdName())
}

def unlock(child)
{
	debugPrint("smartapp unlock")
	def lockId = child.device.deviceNetworkId
	sendLockCommand(lockId, getUnlockCmdName())
}

def pollChild(child)
{
	debugPrint("smartapp pollChild")
    def lockId = child.device.deviceNetworkId
    // TODO: Probably cache this information?
    return getLockInfo(lockId)
}

def authPage()
{
	debugPrint("authPage()")

	if (!atomicState.lockCache) {
    	atomicState.lockCache = [:]
    }
	if (!atomicState.accessToken) {
		debugPrint("about to create access token")
		createAccessToken()
		atomicState.accessToken = state.accessToken
	}


	def description = "Required"
	def oauthTokenProvided = false

	if (atomicState.authCode) {
		description = "You are connected."
		oauthTokenProvided = true
	}

	if (!oauthTokenProvided) {
		def redirectUrl = oauthAuthUrl()

		debugPrint("redirectUrl = ${redirectUrl}")
        
		return dynamicPage(name: "auth", title: "Log In", nextPage: null, uninstall: false, install: false) {
			section(){
				paragraph "Tap below to log in to Lockitron and authorize SmartThings access. Be sure to scroll down and press the 'Allow' button."
				href url: redirectUrl, style: "embedded", required: true, title: "Lockitron", description: description
			}
		}
	} else {
		/*return dynamicPage(name: "auth", title: "Log In", nextPage: "deviceList", uninstall: true, install: true) {
			section(){
				paragraph "Tap Next to continue to setup your locks!"
				href url: buildRedirectUrl("complete"), style: "embedded", state: "complete", title: "Lockitron", description: description
			}
		}*/
        return deviceListPage()
	}
}

def deviceListPage()
{
	debugPrint("deviceListPage()")
    
    def locks = getAllLocks()
    
	def page = dynamicPage(name: "deviceList", title: "Select Your Locks", uninstall: true) {
		section(""){
			paragraph "Tap below to see the list of Lockitron locks available in your account and select the ones you want to connect to SmartThings."
            input(name: "locks", title:"Enabled Locks", type: "enum", required: true, multiple: true, description: "Tap to choose", metadata: [values: locks])
		}
	}
    
    debugPrint("page: ${page}")
    return page
}

def complete()
{
	def html = """
<!DOCTYPE html>
<html>
<head>
<meta name="viewport" content="width=100%, height=100%">
<title>Lockitron Connection</title>
<style type="text/css">
	@font-face {
		font-family: 'Swiss 721 W01 Thin';
		src: url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-thin-webfont.eot');
		src: url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-thin-webfont.eot?#iefix') format('embedded-opentype'),
			 url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-thin-webfont.woff') format('woff'),
			 url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-thin-webfont.ttf') format('truetype'),
			 url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-thin-webfont.svg#swis721_th_btthin') format('svg');
		font-weight: normal;
		font-style: normal;
	}
	@font-face {
		font-family: 'Swiss 721 W01 Light';
		src: url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-light-webfont.eot');
		src: url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-light-webfont.eot?#iefix') format('embedded-opentype'),
			 url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-light-webfont.woff') format('woff'),
			 url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-light-webfont.ttf') format('truetype'),
			 url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-light-webfont.svg#swis721_lt_btlight') format('svg');
		font-weight: normal;
		font-style: normal;
	}
	.container {
		width: 100%;
		/*background: #eee;*/
		text-align: center;
	}
	img {
		vertical-align: middle;
	}
	p {
		font-size: 2.2em;
		font-family: 'Swiss 721 W01 Thin';
		text-align: center;
		color: #666666;
		margin-bottom: 0;
	}
/*
	p:last-child {
		margin-top: 0px;
	}
*/
	span {
		font-family: 'Swiss 721 W01 Light';
	}
</style>
</head>
<body>
	<div class="container">
		<img src="https://lockitron.com/assets/store/lockitron.png" alt="Lockitron icon" />
		<img src="https://s3.amazonaws.com/smartapp-icons/Partner/support/connected-device-icn%402x.png" alt="connected device icon" />
		<img src="https://s3.amazonaws.com/smartapp-icons/Partner/support/st-logo%402x.png" alt="SmartThings logo" />
		<p>Your Lockitron Account is now connected to SmartThings!</p>
		<p>Click 'Done' to finish setup.</p>
	</div>
</body>
</html>
"""

	render contentType: 'text/html', data: html
}

def receiveCode()
{
	debugPrint("swapping token: $params")

	def oauthState = params.state
	atomicState.authCode = params.code

	// TODO: verify oauthState == atomicState.oauthInitState

	def tokenUrl = oauthAccessUrl()

	def jsonMap
	httpPost(uri: tokenUrl) { resp ->
		jsonMap = resp.data
	}

	debugPrint("Results $jsonMap")

	atomicState.accessExpires = jsonMap.expires
	atomicState.accessCode = jsonMap.access_token

	complete()
}

def getAllLocks()
{
	debugPrint("getting device list")

	def locks = getLockInfo(null)
	def stats = [:]
	
    locks.each { stat ->
		def id = stat.id
        def name = stat.name
        debugPrint("$name : $id")
		stats[id] = name
	}
    atomicState.lockCache = stats

	debugPrint("locks: $stats")

	return stats
}

/**
 * Retrieve information about all locks or a particular lock.
 * 
 * @param lockId The ID of a particular lock to return, or null to return all locks.
 * @return       An array of locks objects as described here: https://api.lockitron.com/#locks
 */
def getLockInfo(lockId)
{
	debugPrint("getLockInfo: ${lockId}")
    
    def accessToken = atomicState.accessCode
    def commandUrl = getLockitronApiUrl()
    def commandPath = (lockId != null) ? getLockPath(lockId) : getLocksPath()

	def params = [
    	uri: commandUrl,
        path: commandPath,
        contentType : TEXT,
        headers: [ "Authorization": "Bearer ${accessToken}" ],
        query: [ "access_token": accessToken ]
    ]
	def jsonMap
    
    debugPrint("Request: $params")
	httpGet(params) { resp ->
    	if(resp.status == 200) {
			jsonMap = resp.data
        } else {
        	jsonMap = [:]
        	debugPrint("Retreive locks error: Implement error handling") // TODO: Error handling
        }
	}

	debugPrint("Results $jsonMap")
    
    return jsonMap
}

/**
 * Send a particular command to a lock.
 * Note, Lockitron states that this command could take
 * minutes to execute if a lock is waiting to wake up.
 * 
 * @param lockId  The ID of the lock to send the command to. (Required)
 * @param command The command to send to a lock.
 * @return        The result from the server.
 */
def sendLockCommand(lockId, command)
{
	debugPrint("sendLockCommand: ${lockId}: ${command}")
    
    def accessToken = atomicState.accessCode
    def commandUrl = getLockitronApiUrl()
    def commandPath = getLockPath(lockId)

	def params = [
    	uri: commandUrl,
        path: commandPath,
        contentType : TEXT,
        headers: [ "Authorization": "Bearer ${accessToken}" ],
        query: [ "access_token": accessToken, "state": command ]
    ]
	def jsonMap
    
    debugPrint("Request: $params")
	httpPut(params) { resp ->
		jsonMap = resp.data
	}

	debugPrint("Results $jsonMap")
    
    return jsonMap
}

def toQueryString(Map m)
{
	return "?" + m.collect { k, v -> "${k}=${URLEncoder.encode(v.toString())}" }.sort().join("&")
}

def oauthAuthUrl()
{
	debugPrint("oauthAuthUrl")
	def clientId = getClientId();

	atomicState.oauthInitState = UUID.randomUUID().toString()

	def oauthParams = [
		response_type: "code",
		client_id: clientId,
		state: atomicState.oauthInitState,
		redirect_uri: buildRedirectUrl("receiveCode")
	]

	return getAuthCodeUrl() + toQueryString(oauthParams)
}

def oauthAccessUrl()
{
	debugPrint("oauthAccessUrl")
	def clientId = getClientId();
    def clientSecret = getClientSecret();

	def oauthParams = [
		grant_type: "authorization_code",
		client_id: clientId,
        client_secret: clientSecret,
		code: atomicState.authCode,
		redirect_uri: buildRedirectUrl("receiveCode")
	]

	return getAccessTokenUrl() + toQueryString(oauthParams)
}

def buildRedirectUrl(endPoint)
{
	debugPrint("buildRedirectUrl")
	return getServerUrl() + "/api/token/${atomicState.accessToken}/smartapps/installations/${app.id}/${endPoint}"
}

def debugPrint(msg)
{
	log.debug "$msg"
    sendNotificationEvent("$msg")
}


