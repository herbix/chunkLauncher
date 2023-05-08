package io.chaofan.chunklauncher.auth;

import com.sun.javafx.webkit.WebConsoleListener;
import io.chaofan.chunklauncher.WebViewDialog;
import io.chaofan.chunklauncher.util.HttpFetcher;
import io.chaofan.chunklauncher.util.Lang;
import javafx.application.Platform;
import javafx.scene.web.WebEngine;
import org.json.JSONArray;
import org.json.JSONObject;

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
            System.out.println(e);
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

    @Override
    public String getClientId() {
        return CLIENT_ID;
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

        String url = AUTHORITY + "oauth2/v2.0/authorize" +
                "?client_id=" + CLIENT_ID +
                "&response_type=code" +
                "&redirect_uri=" + URLEncoder.encode("https://login.microsoftonline.com/common/oauth2/nativeclient", "UTF-8") +
                "&scope=" + URLEncoder.encode(SCOPE, "UTF-8") +
                "&login_hint=" + URLEncoder.encode(this.getName(), "UTF-8");
        String authenticationCode;
        String[] locationContainer = new String[1];
        WebViewDialog webViewDialog = new WebViewDialog();
        Platform.runLater(() -> {
            WebEngine engine = webViewDialog.webView.getEngine();
            engine.documentProperty().addListener((prop, oldDoc, newDoc) -> {
                if (engine.getLocation().startsWith("https://login.microsoftonline.com/common/oauth2/nativeclient")) {
                    locationContainer[0] = engine.getLocation();
                    webViewDialog.setVisible(false);
                }
                System.out.println("OnChanged " + engine.getLocation());
            });
            WebConsoleListener.setDefaultListener((webView, message, lineNumber, sourceId) -> {
                System.out.println(message + "[at " + lineNumber + "]");
            });
            engine.load(url);
        });

        webViewDialog.setVisible(true);

        String location = locationContainer[0];
        if (location == null) {
            System.out.println(Lang.getString("msg.auth.microsoft.failed.authenticationfailed"));
            throw new IOException("Authentication failed");
        }

        Pattern requestPattern = Pattern.compile("\\?code=([^\\s]+)");
        Matcher matcher = requestPattern.matcher(location);
        if (!matcher.find()) {
            System.out.println(Lang.getString("msg.auth.microsoft.failed.authenticationfailed"));
            throw new IOException("Authentication failed");
        }

        authenticationCode = matcher.group(1);

        Map<String, String> param = new HashMap<>();
        param.put("client_id", CLIENT_ID);
        param.put("code", authenticationCode);
        param.put("grant_type", "authorization_code");
        param.put("scope", SCOPE);
        param.put("redirect_uri", "https://login.microsoftonline.com/common/oauth2/nativeclient");
        String result = HttpFetcher.fetchUsePostMethod(AUTHORITY + "oauth2/v2.0/token", param);

        JSONObject resultObj = new JSONObject(result);
        return resultObj.getString("access_token");
    }

    /*private String loginToMicrosoftAccountMsal() throws IOException, URISyntaxException {

        Set<String> scopes = new HashSet<String>(Arrays.asList(SCOPE));

        PublicClientApplication app =
                PublicClientApplication
                        .builder(CLIENT_ID)
                        .authority(AUTHORITY)
                        .build();

        InteractiveRequestParameters interactiveRequestParameters = InteractiveRequestParameters
                .builder(new URI("http://localhost"))
                .scopes(scopes)
                .build();

        IAuthenticationResult result = app.acquireToken(interactiveRequestParameters).join();
        String accessToken = result.accessToken();

        return accessToken;
    }*/

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
