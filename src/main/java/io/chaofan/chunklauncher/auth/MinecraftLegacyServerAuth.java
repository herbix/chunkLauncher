package io.chaofan.chunklauncher.auth;

import java.util.HashMap;

import io.chaofan.chunklauncher.util.HttpFetcher;
import io.chaofan.chunklauncher.util.Lang;

public class MinecraftLegacyServerAuth extends ServerAuth {

    private static final int VERSION = 14;

    public MinecraftLegacyServerAuth(String name, String pass) {
        super(name, pass);
    }

    public void login(final AuthDoneCallback callback) {

        System.out.println(Lang.getString("msg.auth.connecting1") + "https://login.minecraft.net" + Lang.getString("msg.auth.connecting2"));

        HashMap<String, String> args = new HashMap<>();

        args.put("user", getName());
        args.put("password", getPass());
        args.put("version", String.valueOf(VERSION));

        String respond = HttpFetcher.fetchUsePostMethod("https://login.minecraft.net", args);

        if (respond == null) {
            callback.authDone(this, false);
            return;
        }

        String[] split = respond.split(":");

        if (split.length != 5) {
            System.out.println(respond);
            callback.authDone(this, false);
            return;
        }

        setAccessToken(split[3]);
        setUuid(split[4]);
        setPlayerName(split[2]);
        setUserType("legacy");
        callback.authDone(this, true);

    }

    public static String getAuthTypeName() {
        return Lang.getString("ui.auth.type.minecraft");
    }

    public static String getAlias() {
        return "minecraft";
    }

}
