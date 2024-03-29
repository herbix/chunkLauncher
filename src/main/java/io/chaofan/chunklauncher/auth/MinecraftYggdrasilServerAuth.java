package io.chaofan.chunklauncher.auth;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JOptionPane;

import io.chaofan.chunklauncher.util.HttpFetcher;
import io.chaofan.chunklauncher.util.Lang;
import org.json.JSONArray;
import org.json.JSONObject;

public class MinecraftYggdrasilServerAuth extends ServerAuth {

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
            List<LauncherProfiles.AuthDBItem> stored = this.profiles.findUsernameFromDB(getName());

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

        if (!resultObj.getJSONArray("availableProfiles").isEmpty()) {
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
            this.profiles.putData(new LauncherProfiles.AuthDBItem(getUuid(), getName(), getAccessToken(),
                    userid, getPlayerName(), arr));

        }

        this.profiles.save();
        callback.authDone(this, true);
    }

    private void offlineLogin(AuthDoneCallback callback, List<LauncherProfiles.AuthDBItem> stored) {
        LauncherProfiles.AuthDBItem selected = selectFrom(stored);

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

}
