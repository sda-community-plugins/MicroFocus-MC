// --------------------------------------------------------------------------------
// Get a list of one or more devices based on platform, api, availability and so on.
// --------------------------------------------------------------------------------

import com.serena.air.plugin.mc.*

import com.serena.air.StepFailedException
import com.serena.air.StepPropertiesHelper
import com.urbancode.air.AirPluginTool
import groovy.json.JsonSlurper

//
// Create some variables that we can use throughout the plugin step.
// These are mainly for checking what operating system we are running on.
//
final def PLUGIN_HOME = System.getenv()['PLUGIN_HOME']
final String lineSep = System.getProperty('line.separator')
final String osName = System.getProperty('os.name').toLowerCase(Locale.US)
final String pathSep = System.getProperty('path.separator')
final boolean windows = (osName =~ /windows/)
final boolean vms = (osName =~ /vms/)
final boolean os9 = (osName =~ /mac/ && !osName.endsWith('x'))
final boolean unix = (pathSep == ':' && !vms && !os9)

//
// Initialise the plugin tool and retrieve all the properties that were sent to the step.
//
final def  apTool = new AirPluginTool(this.args[0], this.args[1])
final def  props  = new StepPropertiesHelper(apTool.getStepProperties(), true)

//
// Set a variable for each of the plugin steps's inputs.
// We can check whether a required input is supplied (the helper will fire an exception if not) and
// if it is of the required type.
//
File workDir = new File('.').canonicalFile
String mcServerUrl = props.notNull('serverUrl')
String mcUsername = props.notNull('username')
String mcPassword = props.notNull('password')
String deviceNameFilter = props.optional('deviceNameFilter')
String platformNameFilter = props.optional("platformNameFilter")
String platformVersionFilter = props.optional("platformVersionFilter")
boolean connectedFilter = props.optionalBoolean("connectedFilter", true)
boolean reservedForMeFilter = props.optionalBoolean("reservedForMeFilter", false)
boolean useProxy = props.optionalBoolean("useProxy", false)
String proxyUrl = props.optional("proxyUrl")
String tenantId = props.optional("tenantId")
boolean debugMode = props.optionalBoolean("debugMode", false)

println "----------------------------------------"
println "-- STEP INPUTS"
println "----------------------------------------"

//
// Print out each of the property values.
//
println "Working directory: ${workDir.canonicalPath}"
println "MC Server URL: ${mcServerUrl}"
println "MC Username: ${mcUsername}"
println "MC Password: ${mcPassword.replaceAll(".", "*")}"
println "Device Name Filter: ${deviceNameFilter}"
println "Platform Name Filter: ${platformNameFilter}"
println "Platform Version Filter: ${platformVersionFilter}"
println "Connected Filter: ${connectedFilter}"
println "Reserved For Me Filter: ${reservedForMeFilter}"
println "Use Proxy: ${useProxy}"
println "Proxy URL: ${proxyUrl}"
println "Tenant Id: ${tenantId}"
if (tenantId) { println "INFO - Tenant Id is not currently supported - ignoring"; tenantId = "" }
println "Debug mode value: ${debugMode}"
if (debugMode) { props.setDebugLoggingMode() }

println "----------------------------------------"
println "-- STEP EXECUTION"
println "----------------------------------------"

List<String> deviceIds = new ArrayList<String>()

try {
    MCHelper mcHelper = new MCHelper(mcServerUrl, mcUsername, mcPassword, proxyUrl, tenantId, debugMode)

    def jsonDevices = new JsonSlurper().parseText(mcHelper.deviceContent())
    jsonDevices.each { data ->
        if (debugMode) println "Checking device: ${data.udid}"
        String platformName = data.platformName
        String platformVersion = data.platformVersion
        String deviceName = data.deviceName
        String udid = data.udid
        String deviceType = data.deviceType
        boolean connected = data.connected
        if (connectedFilter && !connected) return
        if (deviceNameFilter && !deviceName.contains(deviceNameFilter)) return
        if (platformNameFilter && !platformName.equalsIgnoreCase(platformNameFilter)) return
        if (platformVersionFilter && !platformVersion.equalsIgnoreCase(platformVersionFilter)) return
        // TODO: check for reserved
        println "Found (${deviceType}) id: ${udid}, name: ${deviceName}, platform: ${platformName}-${platformVersion}"
        deviceIds.add(udid)
    }

} catch (StepFailedException e) {
    println "ERROR: ${e.message}"
    System.exit 1
}

println "----------------------------------------"
println "-- STEP OUTPUTS"
println "----------------------------------------"

if (deviceIds.size() > 0) {
    println "Setting \"deviceIds\" output property to \"${deviceIds.join(',')}\""
} else {
    println "No matching devices found"
}
apTool.setOutputProperty("deviceIds", deviceIds.join(','))
apTool.storeOutputProperties()

//
// An exit with a zero value means the plugin step execution will be deemed successful.
//
System.exit(0)
