package io.chaofan.chunklauncher.version;

import org.json.JSONObject;

public class Version {
    public String id;
    public String time;
    public String releaseTime;
    public String type;

    public Version(JSONObject arrelem) {
        this.id = arrelem.getString("id");
        this.time = arrelem.getString("time");
        this.releaseTime = arrelem.getString("releaseTime");
        this.type = arrelem.getString("type");
    }

    public Version() {

    }
}