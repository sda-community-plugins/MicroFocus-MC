// --------------------------------------------------------------------------------
// Upload an Application to Mobile Center
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
String appPath = props.notNull('appPath')
String appType = props.optional("appType")
String workspaceId = props.optional("workspaceId")
boolean useProxy = props.optionalBoolean("useProxy", false)
String proxyUrl = props.optional("proxyUrl")
String tenantId = props.optional("tenantId")
boolean debugMode = props.optionalBoolean("debugMode", false)

File appFile = new File(appPath)
if (!appFile.isAbsolute()) {
    appFile = new File(workDir, appPath)
}
if (!appFile.exists()) {
    throw new FileNotFoundException("File system path '${appFile.absolutePath}' does not exist.")
}

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
println "Workspace Id: ${workspaceId}"
println "Application Path: ${appPath}"
println "Application Type: ${appType}"
println "Use Proxy: ${useProxy}"
println "Proxy URL: ${proxyUrl}"
println "Tenant Id: ${tenantId}"
if (tenantId) { println "INFO - Tenant Id is not currently supported - ignoring"; tenantId = "" }
println "Debug mode value: ${debugMode}"
if (debugMode) { props.setDebugLoggingMode() }

println "----------------------------------------"
println "-- STEP EXECUTION"
println "----------------------------------------"

def appUUID

try {
    MCHelper mcHelper = new MCHelper(mcServerUrl, mcUsername, mcPassword, proxyUrl, tenantId, debugMode)

    def jsonApp
    if (appType.equalsIgnoreCase("ANDROID")) {
        println "Uploading Android application..."
        jsonApp = new JsonSlurper().parseText(
            mcHelper.uploadApp(MCHelper.APP_TYPE.ANDROID, appFile.getCanonicalPath(), workspaceId)
        )
    } else if (appType.equalsIgnoreCase("APPLE")) {
        println "Uploading Apple application..."
        jsonApp = new JsonSlurper().parseText(
            mcHelper.uploadApp(MCHelper.APP_TYPE.APPLE, appFile.getCanonicalPath(), workspaceId)
        )
    } else {
        throw new StepFailedException("Unknown Application Type: ${appType}")
    }

    appUUID = jsonApp?.data
    println "Uploaded application as id: ${appUUID}"

} catch (StepFailedException e) {
    println "ERROR: ${e.message}"
    System.exit 1
}

println "----------------------------------------"
println "-- STEP OUTPUTS"
println "----------------------------------------"

println("Setting \"appId\" output property to \"${appUUID}\"")
apTool.setOutputProperty("appId", appUUID)
apTool.storeOutputProperties()

//
// An exit with a zero value means the plugin step execution will be deemed successful.
//
System.exit(0)
