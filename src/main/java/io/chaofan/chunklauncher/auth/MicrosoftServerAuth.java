package io.chaofan.chunklauncher.auth;

import io.chaofan.chunklauncher.util.HttpFetcher;
import io.chaofan.chunklauncher.util.Lang;
import org.json.JSONArray;
import org.json.JSONObject;

import java.awt.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MicrosoftServerAuth extends ServerAuth {
    // Please change this to your own client_id if you want to use the code
    private final static String CLIENT_ID = "0e71947a-02cf-40f0-9fb4-09d8c5205f72";
    private final static String AUTHORITY = "https://login.microsoftonline.com/consumers/";
    private final static String SCOPE = "XboxLive.signin";

    public MicrosoftServerAuth(String name, String pass) {
        super(name, pass);
    }

    @Override
    public void login(AuthDoneCallback callback) {
        try {
            this.loginUnsafe(callback);
        } catch (SocketTimeoutException e) {
            System.out.println(Lang.getString("msg.auth.microsoft.failed.timeout"));
            callback.authDone(this, false);
        } catch (Exception e) {
            callback.authDone(this, false);
        }
    }

    public void loginUnsafe(AuthDoneCallback callback) throws IOException, URISyntaxException {
        System.out.println(Lang.getString("msg.auth.microsoft.login.msa"));
        String accessToken = loginToMicrosoftAccount();

        System.out.println(Lang.getString("msg.auth.microsoft.login.xbl"));
        String xboxLiveToken = loginToXboxLive(accessToken);
        JSONObject xstsQueryResponse = loginToXsts(xboxLiveToken);
        String uhs = xstsQueryResponse.getJSONObject("DisplayClaims").getJSONArray("xui").getJSONObject(0).getString("uhs");
        String xstsToken = xstsQueryResponse.getString("Token");

        System.out.println(Lang.getString("msg.auth.microsoft.login.mc"));
        String mcAuthToken = loginToMinecraft(uhs, xstsToken);

        Map<String, String> profileRequestHeader = new HashMap<>();
        profileRequestHeader.put("Authorization", "Bearer " + mcAuthToken);
        String profile = HttpFetcher.fetch("https://api.minecraftservices.com/minecraft/profile", profileRequestHeader);

        if (profile == null) {
            System.out.println(Lang.getString("msg.auth.microsoft.failed.dontowngame"));
            callback.authDone(this, false);
            return;
        }

        JSONObject profileObject = new JSONObject(profile);

        setUuid(profileObject.getString("id"));
        setPlayerName(profileObject.getString("name"));
        setAccessToken(mcAuthToken);
        setUserType("mojang");

        callback.authDone(this, true);
    }

    private String loginToMinecraft(String uhs, String xstsToken) {
        JSONObject mcAuthQuery = new JSONObject();
        mcAuthQuery.put("identityToken", "XBL3.0 x=" + uhs + ";" + xstsToken);
        JSONObject mcAuthQueryResponse = new JSONObject(HttpFetcher.fetchUsePostMethod("https://api.minecraftservices.com/authentication/login_with_xbox", mcAuthQuery));
        return mcAuthQueryResponse.getString("access_token");
    }

    private JSONObject loginToXsts(String xboxLiveToken) {
        JSONObject xstsQuery = new JSONObject();
        JSONObject xstsQueryProperties = new JSONObject();
        xstsQuery.put("Properties", xstsQueryProperties);
        xstsQuery.put("RelyingParty", "rp://api.minecraftservices.com/");
        xstsQuery.put("TokenType", "JWT");

        xstsQueryProperties.put("SandboxId", "RETAIL");
        xstsQueryProperties.put("UserTokens", new JSONArray(Arrays.asList(xboxLiveToken)));

        Map<String, String> xstsQueryHeaders = new HashMap<>();
        xstsQueryHeaders.put("Content-Type", "application/json");
        xstsQueryHeaders.put("x-xbl-contract-version", "1");

        return new JSONObject(HttpFetcher.fetchUsePostMethod("https://xsts.auth.xboxlive.com/xsts/authorize", xstsQuery.toString(), xstsQueryHeaders));
    }

    private String loginToXboxLive(String accessToken) {
        JSONObject xboxLiveQuery = new JSONObject();
        JSONObject xboxLiveQueryProperties = new JSONObject();
        xboxLiveQuery.put("Properties", xboxLiveQueryProperties);
        xboxLiveQuery.put("RelyingParty", "http://auth.xboxlive.com");
        xboxLiveQuery.put("TokenType", "JWT");

        xboxLiveQueryProperties.put("AuthMethod", "RPS");
        xboxLiveQueryProperties.put("SiteName", "user.auth.xboxlive.com");
        xboxLiveQueryProperties.put("RpsTicket", "d=" + accessToken);

        Map<String, String> xboxLiveQueryHeaders = new HashMap<>();
        xboxLiveQueryHeaders.put("Content-Type", "application/json");
        xboxLiveQueryHeaders.put("x-xbl-contract-version", "1");

        JSONObject xboxLiveQueryResponse = new JSONObject(HttpFetcher.fetchUsePostMethod("https://user.auth.xboxlive.com/user/authenticate", xboxLiveQuery.toString(), xboxLiveQueryHeaders));
        return xboxLiveQueryResponse.getString("Token");
    }

    private String loginToMicrosoftAccount() throws IOException, URISyntaxException {

        String url = AUTHORITY + "/oauth2/v2.0/authorize" +
                "?client_id=" + CLIENT_ID +
                "&response_type=code" +
                "&redirect_uri=http%3A%2F%2Flocalhost%3A8324%2F" +
                "&scope=" + URLEncoder.encode(SCOPE, "UTF-8") +
                "&login_hint=" + URLEncoder.encode(this.getName(), "UTF-8");
        String authenticationCode;

        try (ServerSocket serverSocket = new ServerSocket(8324)) {
            // 5 minutes timeout
            int TIMEOUT = 5 * 60 * 1000;
            serverSocket.setSoTimeout(TIMEOUT);

            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
            } else {
                System.out.println(Lang.getString("Failed to open browser"));
                throw new IOException("Failed to open browser");
            }

            Socket s = serverSocket.accept();
            s.setSoTimeout(TIMEOUT);
            InputStream in = s.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String firstLine = reader.readLine();
            String line = firstLine;
            while (line.length() > 0) {
                line = reader.readLine();
            }

            Pattern requestPattern = Pattern.compile("GET /\\?code=([^\\s]+) (HTTP/[0-9.]+)");
            Matcher matcher = requestPattern.matcher(firstLine);
            if (!matcher.find()) {
                OutputStream out = s.getOutputStream();
                sendResponse(Lang.getString("msg.auth.microsoft.failed.authenticationfaileddot"), "HTTP/1.0", out);
                out.close();
                s.close();
                System.out.println(Lang.getString("msg.auth.microsoft.failed.authenticationfailed"));
                throw new IOException("Authentication failed");
            }

            authenticationCode = matcher.group(1);
            String httpVersion = matcher.group(2);

            OutputStream out = s.getOutputStream();
            sendResponse("<p>" + Lang.getString("msg.auth.microsoft.succeeded") + "</p>" +
                    "<script>window.close()</script>", httpVersion, out);
            out.close();
            s.close();
        }

        Map<String, String> param = new HashMap<>();
        param.put("client_id", CLIENT_ID);
        param.put("code", authenticationCode);
        param.put("grant_type", "authorization_code");
        param.put("scope", SCOPE);
        param.put("redirect_uri", "http://localhost:8324/");
        String result = HttpFetcher.fetchUsePostMethod(AUTHORITY + "oauth2/v2.0/token", param);

        JSONObject resultObj = new JSONObject(result);
        return resultObj.getString("access_token");
    }

    private void loginToMicrosoftAccountMsal() {
/*
        Set<String> scopes = new HashSet<String>(Arrays.asList(SCOPE));

        PublicClientApplication app =
                PublicClientApplication
                        .builder(CLIENT_ID)
                        .authority(AUTHORITY)
                        .build();

        InteractiveRequestParameters interactiveRequestParameters = InteractiveRequestParameters
                .builder(new URI("http://localhost:8324"))
                .scopes(scopes)
                .build();

        IAuthenticationResult result = app.acquireToken(interactiveRequestParameters).join();
        String accessToken = result.accessToken();
*/
    }

    private void sendResponse(String stringContent, String httpVersion, OutputStream out) throws IOException {
        byte[] content = stringContent.getBytes(StandardCharsets.UTF_8);
        out.write((httpVersion + " 200 OK\r\nContent-type: text/html; charset=utf-8\r\nContent-length: ").getBytes());
        out.write(String.valueOf(content.length).getBytes());
        out.write("\r\n\r\n".getBytes());
        out.write(content);
        out.flush();
    }

    public static String getAuthTypeName() {
        return Lang.getString("ui.auth.type.microsoft");
    }

    public static String getAlias() {
        return "microsoft";
    }

    public static boolean canInputPassword() {
        return false;
    }
}
