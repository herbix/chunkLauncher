package io.chaofan.chunklauncher.auth;

import io.chaofan.chunklauncher.util.Lang;

public class OfflineServerAuth extends ServerAuth {

    public OfflineServerAuth(String name, String pass) {
        super(name, pass);
    }

    public void login(AuthDoneCallback callback) {
        callback.authDone(this, true);
    }

    public static String getAuthTypeName() {
        return Lang.getString("ui.auth.type.offline");
    }

    public static String getAlias() {
        return "offline";
    }

}
