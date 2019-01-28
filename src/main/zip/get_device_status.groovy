// --------------------------------------------------------------------------------
// Get the status of a specific device
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
String deviceId = props.notNull('deviceId')
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
println "Device Id: ${deviceId}"
println "Use Proxy: ${useProxy}"
println "Proxy URL: ${proxyUrl}"
println "Tenant Id: ${tenantId}"
if (tenantId) { println "INFO - Tenant Id is not currently supported - ignoring"; tenantId = "" }
println "Debug mode value: ${debugMode}"
if (debugMode) { props.setDebugLoggingMode() }

println "----------------------------------------"
println "-- STEP EXECUTION"
println "----------------------------------------"

boolean connectedStatus = ""
String reservationStatus = ""
boolean reservedForMe = false
boolean deviceFound = false

try {
    MCHelper mcHelper = new MCHelper(mcServerUrl, mcUsername, mcPassword, proxyUrl, tenantId, debugMode)

    /*def jsonUser = new JsonSlurper().parseText(mcHelper.currentUser())
    if (!jsonUser.isAdmin) {
        throw new StepFailedException("Current user ${mcUsername} is not an admin user.")
    }*/

    def jsonDevices = new JsonSlurper().parseText(mcHelper.deviceContent())
    jsonDevices.any { data ->
        if (deviceId == data.udid) {
            String platformName = data.platformName
            String platformVersion = data.platformVersion
            String deviceName = data.deviceName
            String udid = data.udid
            String deviceType = data.deviceType
            connectedStatus = data.connected
            reservationStatus= new String(data.currentReservation?.status).toLowerCase()
            String reservedForUser = ""
            if (reservationStatus != "free") {
                reservedForUser = data.currentReservation?.owner
                if (debugMode) println ""
                if (reservedForUser == mcUsername) {
                    reservedForMe = true
                }
            }
            println "Found (${deviceType}) id: ${udid}, name: ${deviceName}, platform: ${platformName}-${platformVersion}"
            println "Connected: ${connectedStatus}, reservation status: ${reservationStatus} " +
                    (reservedForUser ? "for ${reservedForUser}" : "")
            deviceFound = true
        }
    }
    if (!deviceFound) {
        println "INFO - No device with id ${deviceId} was found"
    }

} catch (StepFailedException e) {
    println "ERROR: ${e.message}"
    System.exit 1
}

println "----------------------------------------"
println "-- STEP OUTPUTS"
println "----------------------------------------"

if (deviceFound) {
    println "Setting output property \"deviceConnected\" to \"${connectedStatus}\""
    if (connectedStatus) {
        apTool.setOutputProperty("deviceConnected", "true")
    } else {
        apTool.setOutputProperty("deviceConnected", "false")
    }
    println "Setting output property \"deviceReserved\" to \"${reservationStatus}\""
    if (reservationStatus != "free") {
        apTool.setOutputProperty("deviceReserved", "true")
        println "Setting output property \"deviceReservedForMe\" to \"${reservedForMe}\""
        if (reservedForMe) {
            apTool.setOutputProperty("deviceReservedForMe", "true")
        } else {
            apTool.setOutputProperty("deviceReservedForMe", "false")
        }
    } else {
        apTool.setOutputProperty("deviceReserved", "false")
    }
    apTool.setOutputProperties()
}

//
// An exit with a zero value means the plugin step execution will be deemed successful.
//
System.exit(0)
