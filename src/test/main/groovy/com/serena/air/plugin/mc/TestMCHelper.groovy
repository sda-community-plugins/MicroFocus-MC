package com.serena.air.plugin.mc

import groovy.json.JsonSlurper

import java.text.DateFormat
import java.text.SimpleDateFormat

def mcServerUrl = "http://localhost:6081"
def mcUsername = "admin@default.com"
def mcPassword = ""
def mcProxyUrl = ""
def mcTenantId = "" // NOT YET USED

MCHelper mcHelper = new MCHelper(mcServerUrl, mcUsername, mcPassword, mcProxyUrl, mcTenantId, true)

def deviceId = "fd087b90-c5a1-4168-acca-74411941cf6f"
//def workspaceId = "07197f69-8f7a-46b3-9669-20c4b4ddd4fe"
def workspaceId = ""
def jobId = "" // NOT YET USED
DateFormat dateFormat = new SimpleDateFormat("yyyy-mm-dd'T'kk:mm:ss'Z'")
DateFormat reservationFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX")

Date now = new Date()
println("Current date is: " + dateFormat.format(now))
def jsonReservations = new JsonSlurper().parseText(mcHelper.reservations())
jsonReservations.each { data ->
    println "Reservation Id: ${data.reservationUid} for device \"${data.deviceCapabilities.deviceName}\" from ${data.startTime} to ${data.endTime}"
    Date min = dateFormat.parse(data.startTime)
    //println(dateFormat.format(min))
    Date max = dateFormat.parse(data.endTime)
    //println(dateFormat.format(max))
    if (now.after(min) && now.before(max)) {
        println "Device is currently reserved"
    }
}

Date startDate = new Date()
println("Reservation state date is: " + dateFormat.format(startDate))
Calendar cal = Calendar.getInstance()
cal.setTime(startDate)
cal.add(Calendar.HOUR_OF_DAY, 1)
Date endDate = cal.getTime()
println("Reservation end date is: " + dateFormat.format(endDate))

/*
Example output:
  {
    "reservationUid":"15451770-5808-43d3-a8d8-385d7c449103",
    "errorCode":0
  }
 */
def jsonReservation = new JsonSlurper().parseText(mcHelper.reserveDeviceByID(reservationFormat.format(startDate), reservationFormat.format(endDate), deviceId, true))
def reservationId = jsonReservation.reservationUid
println "Made reservation with id: ${reservationId}"

/*
Example output:
[
  {
    "reservationUid": "cb94d1e8-d5d6-4803-a286-307d8d98fbf6",
    "startTime": "2019-01-25T13:42:00Z",
    "endTime": "2019-01-25T14:57:00Z",
    "releaseOnJobCompletion": true,
    "deviceCapabilities": {
      "udid": "fd087b90-c5a1-4168-acca-74411941cf6f",
      "deviceName": "Android SDK built for x86"
    },
    "reservedForUser": {
      "name": "admin@default.com",
      "id": "d8b8cacd-996b-49c3-bb16-31d97ef86392"
    }
  }
]
 */
now = new Date()
println("Current date is: " + dateFormat.format(now))
jsonReservations = new JsonSlurper().parseText(mcHelper.reservations())
jsonReservations.each { data ->
    println "Reservation Id: ${data.reservationUid} for device \"${data.deviceCapabilities.deviceName}\" from ${data.startTime} to ${data.endTime}"
    Date min = dateFormat.parse(data.startTime)
    //println(dateFormat.format(min))
    Date max = dateFormat.parse(data.endTime)
    //println(dateFormat.format(max))
    if (now.after(min) && now.before(max)) {
        println "Device is currently reserved"
    }
}

println "Deleting reservation"
mcHelper.deleteReservationByID(reservationId)

/*
Example output:
{
    "platformName": "Android",
    "platformVersion": "9",
    "deviceName": "Google Android SDK built for x86",
    "udid": "fd087b90-c5a1-4168-acca-74411941cf6f",
    "deviceType": "emulator",
    "deviceHostingType": "PrivateOnPremise",
    "nickName": "Android SDK built for x86",
    "connected": true,
    "currentReservation": {
      "status": "Free"
    },
    "connectorName": "MC Server"
  },
*/
def jsonDevices = new JsonSlurper().parseText(mcHelper.deviceContent())
jsonDevices.each { data ->
    println "Device Id: ${data.udid}, nickName: ${data.nickName}, platform: ${data.platformName}-${data.platformVersion}"
}

/*
Example output:
{
  "messageCode": 0,
  "message": null,
  "data": [
    {
      "id": "53b19699-dd16-44f0-8518-a39ef7be5491",
      "name": "Advantage Shopping",
      "version": "1",
      "counter": 1,
      "dateTime": 1548338850547,
      "icon": "iVBORw0KGgoAAAANSUhEUgAAADAAAAAwCAMAAABg3Am1AAAB+FBMVEUAAABVn5V1vLh6wr59xcF+xcJrsq52wLxsvroAAABprqxyv7pyv7phnptuvbdhoZtCe3ttubMAAABfnplet7JenZgAAABgrKdhq6cAAABcsq0AAABXsq1Wsq4AAAAAAABOp6FNp6JDkIxDkYw5dnE5dXIxYF1Fk48xX1waOTkwZWIAAAApUlAybGgAAAAAAAAnUlAvZWIAAAAAAAAiS0kpWFY3dHE3dXESLCkoVVAnU1AkTEoiSkgmVFEAAAAAAAAAAAAAAAAAAAB/xsFguLNYtK5Us61guLKq2dbG5uTF5eOv3NlatrDU7Or///+W0MxTsqzb7+5mu7b+/v6Uz8y4393y+fnD5OJWtK5Vs62z3dvw+PhywLyJy8b9/v6a0s5dt7HZ7u3J5+Wx3Nru+PdxwLuAx8L1+/qg1dHB4+HP6uj3+/t+xsFXtK7o9fSu29i13tzV7Ot1wr3s9/b8/v2EycTE5eOs2teU0Mz+///c7+5kurXk8/L8/v6d09C03tvQ6uhmu7WDyMT5/PxywLuf1dFfuLJet7Kl19Sk1tOr2tdrvbiY0c77/f3M6OZYta+CyMOSz8tbtrDw+fj1+vp6xL/S6+rC5OJVtK5zwbyd1NCo2NV3wr6i1tLv+PdSsKqg1dK+4uC84d9Op6FFlI9Eko1Op6IiSEbZTEuyAAAAQ3RSTlMAGH7D7ftPxvoBa+rrV+tXH8cCjPuNBsTGCuwN/PsOD/z77+/R06X7pjrbEHzzBRqW8wgfft/8/EWs3/f9rSEvODw+E3rtFwAAAh1JREFUSMdjYBi0gJGJmYXVGQdgZWFmYkRVz8bO4eLqhhO4unCwsyEp5+TidncjANx5uDjhGnj5XN0IAlc+fph6AUEi1AN1CApA1AsJi7gRBUREhcAaxMTdiAQSYmANklIIIQ9PCPDy9sGiQUoSrEFaBiHk6wcD/lg0yEqDNcgFYNMQGISpIUAOrEEeq4bgECwa5LFq8PUIDQNS4RHEavBwi4wCUtExxGtwi40D0vEkaEhIBNJJEcRrcEsG0impJGhISwcyMkjQ4JMJZGT5EK/BLQ3IyM4hQUNuHpCVT4IGtwIgq9CHBA1FxX5+JaUkaMgtAzLLSdDgVlHp51dVTYKGoBo/v9o6EjS41QPZVcRoaGiEsJuagbmihQgNlYGgTN3a1h4I5HQQ0OAFz3F+nW5dQLKmG7+Gnl64hr7+CSA3TcSvYVL5ZJiGKVMnTQNS02fg1eAWMXPWbA8ImOM2F6hhXiqmhvm4SrqWeUAdCxD8+VANCrg0zFgICmVEClSEaJBTwlmYxoR6ei5ClGjKkIJMWmU+cWXxfFVIUSmppk6cBnUNSGEspqm1mBj1i7U0IcW9kLaOLhE6FuvqaEMqFAY9fQPDJQT8MX+JoYG+HqySMzI2MTVbumx+AA4wf9lSc1MTYyNEtWukb2FpZW2zHAewsbaytdA34kSqqfW07ewdHJ1wAEcHezttPdSmgJCYpLScPA4gJy0pJkRuowQA1doOy5ZLkfsAAAAASUVORK5CYII=",
      "iconUrl": "/rest/apps/53b19699-dd16-44f0-8518-a39ef7be5491/icon",
      "type": "ANDROID",
      "fileName": "AdvantageShopping.apk",
      "identifier": "com.Advantage.aShopping",
      "comment": null,
      "deviceFamily": null,
      "minimumOsVersion": "17",
      "instrumented": true,
      "svInstrumented": true,
      "instrumentationStatus": "Succeeded",
      "instrumentationFailureReason": null,
      "urlScheme": null,
      "source": "MC",
      "launchActivity": "com.Advantage.aShopping.SplashActivity",
      "appPackage": "com.Advantage.aShopping",
      "appActivity": "com.Advantage.aShopping.SplashActivity",
      "bundleId": null,
      "appUdid": "53b19699-dd16-44f0-8518-a39ef7be5491",
      "provisionedDevices": null,
      "appBuildVersion": "1",
      "appVersion": "1.1.4",
      "workspaces": [
        {
          "uuid": "1bdc3b06-4808-4a2c-801a-c5426f23734c",
          "name": "Shared assets",
          "description": "public device pool for devices to be accessible by any customer",
          "isPrivate": false,
          "isShared": true,
          "isDefault": false
        }
      ],
      "applicationExist": true,
      "instrumentedApplicationExist": true
    }
      ],
      "applicationExist": true,
      "instrumentedApplicationExist": true
    }
  ],
  "error": false
}
 */
def jsonApps = new JsonSlurper().parseText(mcHelper.apps())
jsonApps.data.each { data ->
    println "App Id: ${data.id}, name: ${data.name}, version: ${data.version}"
}

/*
Example output:
[
  {
    "name": "admin@default.com",
    "id": "d8b8cacd-996b-49c3-bb16-31d97ef86392",
    "isAdmin": true,
    "messages": [],
    "roles": [
      {
        "name": "ROLE_OPERATOR",
        "description": "Mobile Operator"
      },
      {
        "name": "ROLE_ADMIN",
        "description": "Mobile Administrator"
      }
    ],
    "locale": null
  },
  {
    "name": "appium.anonymous@default.com",
    "id": "3abb60cb-5aba-42af-acb2-abb655167fd4",
    "isAdmin": false,
    "messages": [],
    "roles": [
      {
        "name": "ROLE_USER",
        "description": "Mobile User"
      }
    ],
    "locale": null
  }
]
 */
def jsonUsers = new JsonSlurper().parseText(mcHelper.users())
jsonUsers.each { data ->
    println "User Id: ${data.id}, name: ${data.name}, admin: ${data.isAdmin}"
}

/*
Example output:
{
  "messageCode": 0,
  "message": null,
  "data": "f1905dd3-3024-4b8a-ba96-4c823390ac53",
  "error": false
}
*/
println "Uploading Android App..."
def jsonApp = new JsonSlurper().parseText(
        mcHelper.uploadApp(MCHelper.APP_TYPE.ANDROID, "C:\\Micro Focus\\Mobile Center Server\\server\\SampleApps\\AdvantageShopping.apk", workspaceId)
)
String appUUID = jsonApp?.data
println "Uploaded app with id = ${appUUID}"

println "Installing Android App on device"
/*
Example output
{
  "devices": {
    "success": [
      {
        "udid": "fd087b90-c5a1-4168-acca-74411941cf6f"
      }
    ],
    "failure": []
  },
  "app": {
    "instrumented": true,
    "counter": 8,
    "id": "com.Advantage.aShopping",
    "uuid": "com.Advantage.aShopping",
    "name": "Advantage Shopping",
    "version": "1"
  },
  "status": true
}
 */
def jsonInstall = new JsonSlurper().parseText(
        mcHelper.installAppByUUIDAndDeviceID(appUUID, deviceId, true, jobId)
)
println jsonInstall
def appData = jsonInstall?.app
println "Installed app \"${appData.name}\" version: ${appData.version} - ${appData.counter}"

println "Uninstalling Android App from device"
def jsonUninstall = new JsonSlurper().parseText(
        mcHelper.uninstallAppByUUIDAndDeviceID(appUUID, deviceId, jobId)
)

println "Installing Android App on device"
jsonInstall = new JsonSlurper().parseText(
        mcHelper.installAppByFileAndDeviceID("AdvantageShopping.apk", deviceId, true, jobId)
)
appData = jsonInstall?.app
println "Installed app \"${appData.name}\" version: ${appData.version} - ${appData.counter}"

println "Uploading iOS App..."
jsonApp = new JsonSlurper().parseText(
    mcHelper.uploadApp(MCHelper.APP_TYPE.APPLE, "C:\\Micro Focus\\Mobile Center Server\\server\\SampleApps\\AdvantageShopping.ipa", workspaceId)
)
appUUID = jsonApp?.data
println "Uploaded app with id = ${appUUID}"

//mcHelper.installAppByFileAndDeviceID("Advantage.apk", " fd087b90-c5a1-4168-acca-74411941cf6f", true)
//String appUUID = "39ae93d1-0804-4b9a-b9b0-c5b2b94c69a1"
//mcHelper.installAppByUUIDAndDeviceID(appUUID, "fd087b90-c5a1-4168-acca-74411941cf6f", true)

mcHelper.logout()


