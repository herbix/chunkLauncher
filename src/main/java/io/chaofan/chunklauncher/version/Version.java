package io.chaofan.chunklauncher.version;

import org.json.JSONObject;

public class Version {
    public String id;
    public String time;
    public String releaseTime;
    public String type;
    public String url;

    public Version(JSONObject json) {
        this.id = json.getString("id");
        this.time = json.getString("time");
        this.releaseTime = json.getString("releaseTime");
        this.type = json.getString("type");
        this.url = json.has("url") ? json.getString("url") : null;
    }

    public Version() {

    }
}