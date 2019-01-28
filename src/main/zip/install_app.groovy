// --------------------------------------------------------------------------------
// Install an Application on to a Device
// --------------------------------------------------------------------------------

import com.serena.air.plugin.mc.*

import com.serena.air.StepFailedException
import com.serena.air.StepPropertiesHelper
import com.urbancode.air.AirPluginTool
import groovy.json.JsonSlurper

import java.text.DateFormat
import java.text.SimpleDateFormat

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
String appId = props.notNull('appId')
String deviceId = props.notNull("deviceId")
boolean instrumented = props.optionalBoolean("instrumented", false)
boolean reserveDevice = props.optionalBoolean("reserveDevice", false)
Integer reservationTime = props.optionalInt("reservationTime", 30)
boolean useProxy = props.optionalBoolean("useProxy", false)
String proxyUrl = props.optional("proxyUrl")
String jobId = props.optional("jobId")
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
println "Application Id: ${appId}"
println "Device Id: ${deviceId}"
println "Instrumented: ${instrumented}"
println "Reserve Device: ${reserveDevice}"
println "Reservation Time: ${reservationTime}"
println "Use Proxy: ${useProxy}"
println "Proxy URL: ${proxyUrl}"
println "Tenant Id: ${tenantId}"
if (tenantId) { println "INFO - Tenant Id is not currently supported - ignoring"; tenantId = "" }
println "Job Id: ${jobId} "
if (jobId) { println "INFO - Job Id is not currently supported - ignoring"; jobId = ""}
println "Debug mode value: ${debugMode}"
if (debugMode) { props.setDebugLoggingMode() }

println "----------------------------------------"
println "-- STEP EXECUTION"
println "----------------------------------------"

try {
    MCHelper mcHelper = new MCHelper(mcServerUrl, mcUsername, mcPassword, proxyUrl, tenantId, debugMode)

    if (reserveDevice) {
        // check if admin
        def jsonUser = new JsonSlurper().parseText(mcHelper.currentUser())
        if (!jsonUser.isAdmin) {
            println "User ${jsonUser.name} is not an admin, skipping device reservation..."
        } else {

            DateFormat dateFormat = new SimpleDateFormat("yyyy-mm-dd'T'kk:mm:ss'Z'")
            DateFormat reservationFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX")

            def jsonReservations = new JsonSlurper().parseText(mcHelper.reservations())
            jsonReservations.each { data ->
                if (debugMode) println "INFO - Found Reservation Id: ${data.reservationUid} for device \"${data.deviceCapabilities.deviceName}\" from ${data.startTime} to ${data.endTime}"
                Date min = dateFormat.parse(data.startTime)
                Date max = dateFormat.parse(data.endTime)
                if (now.after(min) && now.before(max)) {
                    if (data.reservedForUser.name != mcUsername) {
                        println "Device is already reserved by ${data.reservedForUser.name}"
                    } else {
                        println "Deleting current reservation"
                        mcHelper.deleteReservationByID(data.reservationUid)
                    }
                }
            }

            Date startDate = new Date()
            Calendar cal = Calendar.getInstance()
            cal.setTime(startDate)
            cal.add(Calendar.MINUTE, reservationTime)
            Date endDate = cal.getTime()
            println "Reserving device between " + dateFormat.format(startDate) + " and " + dateFormat.format(endDate)
            def jsonReservation = new JsonSlurper().parseText(mcHelper.reserveDeviceByID(reservationFormat.format(startDate),
                    reservationFormat.format(endDate), deviceId, false))
            def reservationId = jsonReservation.reservationUid
            println "Made reservation with id: ${reservationId}"
        }
    }

    def jsonInstall = new JsonSlurper().parseText(
            mcHelper.installAppByUUIDAndDeviceID(appId, deviceId, instrumented, jobId)
    )
    def appData = jsonInstall?.app
    println "Installed application \"${appData.name}\" version: ${appData.version} - counter: ${appData.counter}"

} catch (StepFailedException e) {
    println "ERROR: ${e.message}"
    System.exit 1
}

println "----------------------------------------"
println "-- STEP OUTPUTS"
println "----------------------------------------"

println "Setting \"appId\" output property to \"${appId}\""
apTool.setOutputProperty("appId", appId)
apTool.storeOutputProperties()

//
// An exit with a zero value means the plugin step execution will be deemed successful.
//
System.exit(0)
