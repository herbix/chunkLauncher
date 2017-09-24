package io.chaofan.chunklauncher.version;

import java.util.List;

import io.chaofan.chunklauncher.util.OS;
import org.json.JSONObject;

public class Rule {
    public static final int ALLOW = 0;
    public static final int DISALLOW = 1;

    public int action;

    public String osName;
    public String osVersion;

    public Rule(JSONObject json) {
        action = json.getString("action").equals("allow") ? ALLOW : DISALLOW;
        if(json.has("os")) {
            JSONObject os = json.getJSONObject("os");
            osName = os.getString("name");
            if(os.has("version"))
                osVersion = os.getString("version");
            else
                osVersion = null;
        }
    }

    public static boolean isAllowed(List<Rule> rules) {
        boolean allowed = true;

        if(rules != null) {
            allowed = false;

            for(int i=0; i<rules.size(); i++) {
                Rule r = rules.get(i);
                if(r.osName == null) {
                    allowed = (r.action == ALLOW);
                } else {
                    if(OS.matchOsNameAndVersion(r.osName, r.osVersion)) {
                        allowed = (r.action == ALLOW);
                    }
                }
            }
        }

        return allowed;
    }
}
