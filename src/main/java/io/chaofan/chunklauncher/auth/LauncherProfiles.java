package io.chaofan.chunklauncher.auth;

import io.chaofan.chunklauncher.util.EasyFileAccess;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;

public class LauncherProfiles {

    private JSONObject store;
    private final String path;

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
