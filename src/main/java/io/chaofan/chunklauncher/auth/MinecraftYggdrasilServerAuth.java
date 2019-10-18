package io.chaofan.chunklauncher.auth;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.swing.JOptionPane;

import io.chaofan.chunklauncher.util.EasyFileAccess;
import io.chaofan.chunklauncher.util.HttpFetcher;
import io.chaofan.chunklauncher.util.Lang;
import org.json.JSONArray;
import org.json.JSONObject;
import io.chaofan.chunklauncher.Config;

public class MinecraftYggdrasilServerAuth extends ServerAuth {

    private LauncherProfiles profiles = new LauncherProfiles(Config.gamePath + "/launcher_profiles.json");

    public MinecraftYggdrasilServerAuth(String name, String pass) {
        super(name, pass);
    }

    public void login(final AuthDoneCallback callback) {

        System.out.println(Lang.getString("msg.auth.connecting1") + "https://authserver.mojang.com/authenticate" + Lang.getString("msg.auth.connecting2"));

        String token = "ChunkLauncher:" + this.profiles.getClientToken();

        JSONObject obj = new JSONObject();
        obj.put("agent", "Minecraft");
        obj.put("username", getName());
        obj.put("password", getPass());
        obj.put("clientToken", token);
        obj.put("requestUser", true);

        String result = HttpFetcher.fetchUsePostMethod("https://authserver.mojang.com/authenticate", obj);

        if (result == null) {
            List<AuthDBItem> stored = this.profiles.findUsernameFromDB(getName());

            if (stored.isEmpty() || JOptionPane.YES_OPTION !=
                    JOptionPane.showConfirmDialog(null, Lang.getString("msg.yggdrasil.saved"), "ChunkLauncher", JOptionPane.YES_NO_OPTION)) {
                callback.authDone(this, false);
                return;
            } else {
                offlineLogin(callback, stored);
                return;
            }
        }

        JSONObject resultObj = new JSONObject(result);

        if (resultObj.has("clientToken") && !resultObj.getString("clientToken").equals(token)) {
            callback.authDone(this, false);
            return;
        }

        JSONObject profile = null;

        if (resultObj.getJSONArray("availableProfiles").length() > 0) {
            JSONArray profiles = resultObj.getJSONArray("availableProfiles");

            List<String> list = new ArrayList<>();
            Map<String, JSONObject> map = new HashMap<>();

            for (int i = 0; i < profiles.length(); i++) {
                JSONObject item = profiles.getJSONObject(i);
                String name = item.getString("name");
                list.add(name);
                map.put(name, item);
            }

            profile = map.get(selectFrom(list));
        }

        if (profile == null) {
            this.profiles.save();
            callback.authDone(this, false);
            return;
        }

        setPlayerName(profile.getString("name"));
        setUuid(profile.getString("id"));

        setAccessToken(resultObj.getString("accessToken"));

        setUserType("mojang");

        if (resultObj.has("user")) {
            Map<String, Collection<Object>> properties = getUserProperties();
            JSONObject user = resultObj.getJSONObject("user");
            JSONArray arr = null;

            if (user.has("properties")) {
                arr = user.getJSONArray("properties");
                makeUserproperties(properties, arr);
            }

            String userid = user.has("id") ? user.getString("id") : null;
            this.profiles.putData(new AuthDBItem(getUuid(), getName(), getAccessToken(),
                    userid, getPlayerName(), arr));

        }

        this.profiles.save();
        callback.authDone(this, true);
    }

    private void offlineLogin(AuthDoneCallback callback, List<AuthDBItem> stored) {
        AuthDBItem selected = selectFrom(stored);

        setPlayerName(selected.displayName);
        setUuid(selected.uuid);

        setAccessToken(selected.accessToken);

        setUserType("mojang");

        if (selected.userProperties != null) {
            makeUserproperties(getUserProperties(), selected.userProperties);
        }
        callback.authDone(this, true);
    }

    private void makeUserproperties(Map<String, Collection<Object>> properties, JSONArray arr) {

        for (int i = 0; i < arr.length(); i++) {
            JSONObject item = arr.getJSONObject(i);
            String name = item.getString("name");
            Object value = item.get("value");

            Collection<Object> list = properties.computeIfAbsent(name, k -> new ArrayList<>());

            list.add(value);
        }

    }

    public static String getAuthTypeName() {
        return Lang.getString("ui.auth.type.yggdrasil");
    }

    public static String getAlias() {
        return "yggdrasil";
    }

    static class LauncherProfiles {

        private JSONObject store;
        private String path;

        public LauncherProfiles(String path) {
            this.path = path;
            String json = EasyFileAccess.loadFile(path);
            try {
                store = json == null ? new JSONObject() : new JSONObject(json);
            } catch (Exception e) {
                store = new JSONObject();
                store.put("profiles", Collections.EMPTY_MAP);
            }
        }

        public void save() {
            EasyFileAccess.saveFile(path, store.toString(2));
        }

        public String getClientToken() {
            if (store.has("clientToken")) {
                return store.getString("clientToken");
            } else {
                String value = UUID.randomUUID().toString();
                store.put("clientToken", value);
                return value;
            }
        }

        private JSONObject getAuthDB() {
            if (!store.has("authenticationDatabase")) {
                store.put("authenticationDatabase", Collections.EMPTY_MAP);
            }
            return store.getJSONObject("authenticationDatabase");
        }

        public List<AuthDBItem> findUsernameFromDB(String username) {
            try {
                List<AuthDBItem> result = new ArrayList<>();
                JSONObject db = getAuthDB();
                Iterator<?> i = db.keys();
                while (i.hasNext()) {
                    JSONObject item = db.getJSONObject((String) i.next());
                    AuthDBItem dbitem = new AuthDBItem(item);
                    if (dbitem.username != null && dbitem.username.equals(username)) {
                        result.add(dbitem);
                    }
                }
                return result;
            } catch (Exception e) {
                return Collections.emptyList();
            }
        }

        public void putData(AuthDBItem item) {
            JSONObject db = getAuthDB();
            JSONObject data = item.toJson();
            db.put(item.uuid.replace("-", ""), data);
        }

    }

    static class AuthDBItem {

        public AuthDBItem(String playerUuid, String username,
                          String accessToken, String userid, String displayName,
                          JSONArray userProperties) {
            this.uuid = playerUuid.replaceAll("([a-fA-F0-9]{8})([a-fA-F0-9]{4})" +
                    "([a-fA-F0-9]{4})([a-fA-F0-9]{4})([a-fA-F0-9]{12})", "$1-$2-$3-$4-$5");
            this.username = username;
            this.accessToken = accessToken;
            this.userid = userid;
            this.displayName = displayName;
            this.userProperties = userProperties;
        }

        public AuthDBItem(JSONObject json) {
            this.uuid = getString(json, "uuid");
            this.username = getString(json, "username");
            this.accessToken = getString(json, "accessToken");
            this.userid = getString(json, "userid");
            this.displayName = getString(json, "displayName");
            this.userProperties = getJSONArray(json, "userProperties");
        }

        public JSONObject toJson() {
            JSONObject data = new JSONObject();
            if (username != null)
                data.put("username", username);
            if (accessToken != null)
                data.put("accessToken", accessToken);
            if (userid != null)
                data.put("userid", userid);
            if (uuid != null)
                data.put("uuid", uuid);
            if (displayName != null)
                data.put("displayName", displayName);
            if (userProperties != null)
                data.put("userProperties", userProperties);
            return data;
        }

        private String getString(JSONObject json, String key) {
            return json.has(key) ? json.getString(key) : null;
        }

        private JSONArray getJSONArray(JSONObject json, String key) {
            return json.has(key) ? json.getJSONArray(key) : null;
        }

        @Override
        public String toString() {
            return displayName;
        }

        String uuid;
        String username;
        String accessToken;
        String userid;
        String displayName;
        JSONArray userProperties;
    }

}
