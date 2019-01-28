package com.serena.air.plugin.mc;

import okhttp3.*;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MCHelper {

    // *************************************
    // Configurable fields
    // *************************************

    private String serverUrl;
    private String username;
    private String password;
    private String tenantId;
    private boolean useProxy = false;
    private String proxyUrl = "http://localhost";
    private int proxyPort = 8080;
    private boolean debugMode;

    public enum APP_TYPE { ANDROID, APPLE }

    // ************************************
    // Mobile Center APIs end-points
    // ************************************
    private static final String ENDPOINT_CLIENT_LOGIN = "client/login";
    private static final String ENDPOINT_CLIENT_LOGOUT = "client/logout";
    private static final String ENDPOINT_CLIENT_DEVICES = "deviceContent";
    private static final String ENDPOINT_CLIENT_APPS = "apps";
    private static final String ENDPOINT_CLIENT_INSTALL_APPS = "apps/install";
    private static final String ENDPOINT_CLIENT_UNINSTALL_APPS = "apps/uninstall";
    private static final String ENDPOINT_CLIENT_UPLOAD_APPS = "apps/upload?enforceUpload=true";
    private static final String ENDPOINT_CLIENT_USERS = "v2/users";
    private static final String ENDPOINT_CLIENT_DEVICE_LOG_COLLECTION = "v2/device/[deviceId]/logs/collection";
    private static final String ENDPOINT_CLIENT_DEVICE_LOG = "v2/device/[deviceId]/logs";
    private static final String ENDPOINT_CLIENT_RESERVATION = "v2/public/reservation";

    private OkHttpClient client;
    private String hp4msecret;
    private String jsessionid;
    private String reqTenantId;
    private String responseBodyStr;

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final MediaType APK = MediaType.parse("application/vnd.android.package-archive");
    private static final MediaType IPA = MediaType.parse("application/octet-stream");

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
        if (!this.serverUrl.endsWith("/")) this.serverUrl = this.serverUrl.concat("/");
        if (!this.serverUrl.endsWith("rest/")) this.serverUrl = this.serverUrl.concat("rest/");
    }
    public String getServerUrl() {
        return this.serverUrl;
    }
    public void setProxyUrl(String proxyUrl) {
        this.proxyUrl = proxyUrl;
        if (!this.proxyUrl.endsWith("/")) this.proxyUrl = this.proxyUrl.concat("/");
        this.useProxy = true;
    }
    public String getProxyUrl() {
        return this.proxyUrl;
    }
    public boolean isUseProxy() {
        return this.useProxy;
    }
    public void setProxyPort(int proxyPort) {
        this.proxyPort = proxyPort;
    }
    public int getProxyPort() {
        return this.proxyPort;
    }
    private boolean isTenantId() {
        return (this.tenantId != null && this.tenantId.length() > 0);
    }
    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
    }
    public boolean isDebugMode() {
        return this.debugMode;
    }

    // ******************************************************
    // MCHelper class constructor to store all info and call API methods
    // ******************************************************
    public MCHelper(String serverUrl, String username, String password, String proxyUrl,
                    String tenantId, boolean debugMode) throws IOException {
        this.setServerUrl(serverUrl);
        if (tenantId != null & tenantId.length() > 0){
            this.username = username + "#" + tenantId;
        } else {
            this.username = username;
        }
        this.password = password;
        if (proxyUrl != null && proxyUrl.length() > 0) {
            this.useProxy = true;
            URL url = new URL(proxyUrl);
            this.proxyUrl = url.getHost();
            this.proxyPort = url.getPort();
        }
        if (tenantId != null && tenantId.length() > 0) {
            this.tenantId = tenantId;
        }
        this.debugMode = debugMode;

        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder()
                .readTimeout(240, TimeUnit.SECONDS)
                .writeTimeout(240, TimeUnit.SECONDS)
                .connectTimeout(60, TimeUnit.SECONDS)
                .cookieJar(new CookieJar() {
                    private final HashMap<String, List<Cookie>> cookieStore = new HashMap<>();

                    @Override
                    public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
                        List<Cookie> storedCookies = cookieStore.get(url.host());
                        if (storedCookies == null) {
                            storedCookies = new ArrayList<>();
                            cookieStore.put(url.host(), storedCookies);
                        }
                        storedCookies.addAll(cookies);
                        for (Cookie cookie : cookies) {
                            if (cookie.name().equals("hp4msecret"))
                                hp4msecret = cookie.value();
                            if (cookie.name().equals("JSESSIONID"))
                                jsessionid = cookie.value();
                            if (cookie.name().equals("TENANT_ID_COOKIE"))
                                reqTenantId = cookie.value();
                        }
                    }

                    @Override
                    public List<Cookie> loadForRequest(HttpUrl url) {
                        List<Cookie> cookies = cookieStore.get(url.host());
                        return cookies != null ? cookies : new ArrayList<>();
                    }
                });

        if (useProxy) {
            if (isDebugMode()) {
                System.out.println("DEBUG - Using proxy: " + this.getProxyUrl() + ":" + this.getProxyPort());
            }
            clientBuilder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(this.proxyUrl, getProxyPort())));
        }

        client = clientBuilder.build();
        login(username, password);
    }

    // ***********************************************************
    // Login to Mobile Center for getting cookies to work with API
    // ***********************************************************
    public void login(String username, String password) throws IOException {
        String strCredentials = "{\"name\":\"" + username + "\",\"password\":\"" + password + "\"}";
        RequestBody body = RequestBody.create(JSON, strCredentials);
        executeRestAPI(ENDPOINT_CLIENT_LOGIN, HttpMethod.POST, body);
    }

    // ************************************
    // List all apps from Mobile Center
    // ************************************
    public String apps() throws IOException {
        return executeRestAPI(ENDPOINT_CLIENT_APPS);
    }

    // ************************************
    // List all users from Mobile Center
    // ************************************
    public String users() throws IOException {
        return executeRestAPI(ENDPOINT_CLIENT_USERS);
    }

    // ************************************
    // Get current from Mobile Center
    // ************************************
    public String currentUser() throws IOException {
        return executeRestAPI(ENDPOINT_CLIENT_USERS + "/currentUser");
    }

    // ************************************
    // List all devices from Mobile Center
    // ************************************
    public String deviceContent() throws IOException {
        return executeRestAPI(ENDPOINT_CLIENT_DEVICES);
    }

    // ************************************
    // Install application by file name, when there are multiple matches for file name in database, will select first application.
    // ************************************
    public String installAppByFileAndDeviceID(String filename, String deviceId, Boolean is_intrumented, String jobId) throws IOException {
        apps();
        String[] res = responseBodyStr.split(filename);
        if (res == null || res.length < 2) {
            return null;
        } else {
            String counter = parseProperty(res[0], "\"counter\":", ",");
            String package_name = parseProperty(res[1], "\"identifier\":\"", "\",");
            String str = "{\n" +
                    "  \"app\": {\n" +
                    "    \"counter\": " + counter + ",\n" +
                    "    \"id\": \"" + package_name + "\",\n" +
                    "    \"instrumented\": " + (is_intrumented ? "true" : "false") + "\n" +
                    "  },\n" +
                    "  \"deviceCapabilities\": {\n" +
                    "    \"udid\": \"" + deviceId + "\"\n" +
                    "  }";
            if (jobId != null && jobId.length() > 0) {
                str = str + ",\n " + " \"jobId\": \"" + jobId + "\"\n" + "}\n";
            } else {
                str = str + "\n}";
            }
            if (isDebugMode()) {
                System.out.println ("DEBUG - Calling \"" + ENDPOINT_CLIENT_INSTALL_APPS + "\" with: " + str);
            }
            RequestBody body = RequestBody.create(JSON, str);
            return executeRestAPI(ENDPOINT_CLIENT_INSTALL_APPS, HttpMethod.POST, body);
        }
    }

    // ************************************
    // Install application by file name, when there are multiple matches for file name in database, will select first application.
    // ************************************
    public String installAppByUUIDAndDeviceID(String uuid, String deviceId, Boolean is_intrumented, String jobId) throws IOException {
        String str = "{\n" +
                "  \"app\": {\n" +
                "    \"uuid\": \"" + uuid + "\",\n" +
                "    \"instrumented\": " + (is_intrumented ? "true" : "false") + "\n" +
                "  },\n" +
                "  \"deviceCapabilities\": {\n" +
                "    \"udid\": \"" + deviceId + "\"\n" +
                "  }";
        if (jobId != null && jobId.length() > 0) {
            str = str + ",\n " + " \"jobId\": \"" + jobId + "\"\n" + "}\n";
        } else {
            str = str + "\n}";
        }
        if (isDebugMode()) {
            System.out.println ("DEBUG - Calling \"" + ENDPOINT_CLIENT_INSTALL_APPS + "\" with: " + str);
        }
        RequestBody body = RequestBody.create(JSON, str);
        return executeRestAPI(ENDPOINT_CLIENT_INSTALL_APPS, HttpMethod.POST, body);
    }

    // ************************************
    // Uninstall application by uuid and device id.
    // ************************************
    public String uninstallAppByUUIDAndDeviceID(String uuid, String deviceId, String jobId) throws IOException {
        String str = "{\n" +
                "  \"app\": {\n" +
                "    \"uuid\": \"" + uuid + "\"\n" +
                "  },\n" +
                "  \"deviceCapabilities\": {\n" +
                "    \"udid\": \"" + deviceId + "\"\n" +
                "  }\n";
        if (jobId != null && jobId.length() > 0) {
            str = str + ",\n " + " \"jobId\": \"" + jobId + "\"\n" + "}\n";
        } else {
            str = str + "\n}";
        }
        if (isDebugMode()) {
            System.out.println ("DEBUG - Calling \"" + ENDPOINT_CLIENT_UNINSTALL_APPS + "\" with: " + str);
        }
        RequestBody body = RequestBody.create(JSON, str);
        return executeRestAPI(ENDPOINT_CLIENT_UNINSTALL_APPS, HttpMethod.POST, body);
    }

    // ************************************
    // Uninstall application by uuid and device id.
    // ************************************
    public String uninstallAppByIdAndDeviceName(String id, String deviceName, String jobId) throws IOException {
        String str = "{\n" +
                "  \"app\": {\n" +
                "    \"id\": \"" + id + "\"\n" +
                "  },\n" +
                "  \"deviceCapabilities\": {\n" +
                "    \"deviceName\": \"" + deviceName + "\"\n" +
                "  }\n";
        if (jobId != null && jobId.length() > 0) {
            str = str + ",\n " + " \"jobId\": \"" + jobId + "\"\n" + "}\n";
        } else {
            str = str + "}";
        }
        if (isDebugMode()) {
            System.out.println ("DEBUG - Calling \"" + ENDPOINT_CLIENT_UNINSTALL_APPS + "\" with: " + str);
        }
        RequestBody body = RequestBody.create(JSON, str);
        return executeRestAPI(ENDPOINT_CLIENT_UNINSTALL_APPS, HttpMethod.POST, body);
    }

    // ************************************
    // Upload Application to Mobile Center
    // ************************************
    @SuppressWarnings("unused")
    public String uploadApp(APP_TYPE appType, String filename, String workspaceId) throws IOException {
        String[] parts = filename.split("\\\\");
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", parts[parts.length - 1],
                        RequestBody.create((appType == APP_TYPE.ANDROID ? APK : IPA), new File(filename)))
                .build();

        Request request = new Request.Builder()
                .addHeader("content-type", "multipart/form-data")
                .addHeader("x-hp4msecret", hp4msecret)
                .addHeader("JSESSIONID", jsessionid)
                .addHeader("TENANT_ID_COOKIE", reqTenantId)
                .url(serverUrl + ENDPOINT_CLIENT_UPLOAD_APPS +
                        (workspaceId != null && workspaceId.length() > 0 ? "&workspaceId="+workspaceId : ""))
                .post(requestBody)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                if (isDebugMode()) System.out.println("DEBUG - " + response.toString());
                ResponseBody body = response.body();
                if (body != null) {
                    String responseStr = body.string();
                    if (isDebugMode()) System.out.println("DEBUG - " + responseStr);
                    return responseStr;
                } else {
                    return null;
                }
            } else {
                throw new IOException("Unexpected code " + response);
            }
            //response.close();

        }
    }

    // ************************************
    // List all reservations from Mobile Center
    // ************************************
    public String reservations() throws IOException {
        return executeRestAPI(ENDPOINT_CLIENT_RESERVATION);
    }

    // ************************************
    // Make a reservation by start time, end time and device id.
    // ************************************
    public String reserveDeviceByID(String startTime, String endTime, String deviceId, boolean releaseOnJobCompletion) throws IOException {
        String str = "{\n" +
                "  \"startTime\": \"" + startTime + "\",\n" +
                "  \"endTime\": \"" + endTime + "\",\n" +
                "  \"releaseOnJobCompletion\": " + String.valueOf(releaseOnJobCompletion) + ",\n" +
                "  \"deviceCapabilities\": {\n" +
                "    \"udid\": \"" + deviceId + "\"\n" +
                "  }\n" +
                "}";
        if (isDebugMode()) {
            System.out.println ("DEBUG - Calling \"" + ENDPOINT_CLIENT_RESERVATION+ "\" with: " + str);
        }
        RequestBody body = RequestBody.create(JSON, str);
        return executeRestAPI(ENDPOINT_CLIENT_RESERVATION, HttpMethod.POST, body);
    }

    // ************************************
    // Get a reservation by id.
    // ************************************
    public String getReservationByID(String reservationId) throws IOException {
        RequestBody body = RequestBody.create(JSON, "");
        return executeRestAPI(ENDPOINT_CLIENT_RESERVATION + "/" + reservationId, HttpMethod.GET, body);
    }

    // ************************************
    // Delete a reservation by id.
    // ************************************
    public String deleteReservationByID(String reservationId) throws IOException {
        RequestBody body = RequestBody.create(JSON, "");
        return executeRestAPI(ENDPOINT_CLIENT_RESERVATION + "/" + reservationId, HttpMethod.DELETE);
    }

    // ************************************
    // Logout from Mobile Center
    // ************************************
    public void logout() throws IOException {
        RequestBody body = RequestBody.create(JSON, "");
        executeRestAPI(ENDPOINT_CLIENT_LOGOUT, HttpMethod.POST, body);
    }

    // ************************************
    // PRIVATE METHODS
    // ************************************

    private String executeRestAPI(String endpoint) throws IOException {
        return executeRestAPI(endpoint, HttpMethod.GET);
    }

    private String executeRestAPI(String endpoint, HttpMethod httpMethod) throws IOException {
        return executeRestAPI(endpoint, httpMethod, null);
    }

    private String executeRestAPI(String endpoint, HttpMethod httpMethod, RequestBody body) throws IOException {

        // build the request URL and headers
        Request.Builder builder = new Request.Builder()
                .url(serverUrl + endpoint)
                .addHeader("Content-type", JSON.toString())
                .addHeader("Accept", JSON.toString());

        // add CRSF header
        if (hp4msecret != null) {
            builder.addHeader("x-hp4msecret", hp4msecret);
        }

        // build the http method
        if (HttpMethod.GET.equals(httpMethod)) {
            builder.get();
        } else if (HttpMethod.POST.equals(httpMethod)) {
            builder.post(body);
        } else if (HttpMethod.DELETE.equals(httpMethod)) {
            builder.delete(body);
        }


    Request request = builder.build();
        if (isDebugMode()) System.out.println("DEBUG -  " + request);

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                if (isDebugMode()) System.out.println("DEBUG - " + response.toString());
                final ResponseBody responseBody = response.body();
                if (responseBody != null) {
                    responseBodyStr = responseBody.string();
                    if (isDebugMode()) System.out.println("DEBUG - Body: " + responseBodyStr);
                    return responseBodyStr;
                }
            } else {
                throw new IOException("Unexpected code " + response);
            }
            response.close();
            return null;
        }
    }

    private enum HttpMethod {
        GET,
        POST,
        DELETE
    }

    private String parseProperty(String source, String prefix, String suffix) {
        try {
            String[] array = source.split(prefix);
            String str = array[array.length - 1];
            return str.split(suffix)[0];
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
