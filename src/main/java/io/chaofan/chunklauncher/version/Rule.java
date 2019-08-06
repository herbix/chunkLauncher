package io.chaofan.chunklauncher.version;

import java.util.*;

import io.chaofan.chunklauncher.util.OS;
import org.json.JSONObject;

public class Rule {
    public static final int ALLOW = 0;
    public static final int DISALLOW = 1;

    public int action;

    public String osName;
    public String osVersion;
    public String osArch;

    public HashMap<String, Boolean> features = new HashMap<String, Boolean>();

    public Rule(JSONObject json) {
        action = json.getString("action").equals("allow") ? ALLOW : DISALLOW;
        if(json.has("os")) {
            JSONObject os = json.getJSONObject("os");
            if(os.has("name"))
                osName = os.getString("name");
            if(os.has("version"))
                osVersion = os.getString("version");
            if(os.has("arch"))
                osArch = os.getString("arch");
        }

        if(json.has("features")) {
            JSONObject obj = json.getJSONObject("features");
            for(String key : obj.keySet()) {
                features.put(key, obj.getBoolean(key));
            }
        }
    }

    public static boolean isAllowed(List<Rule> rules) {
        return isAllowed(rules, new HashSet<String>());
    }

    public static boolean isAllowed(List<Rule> rules, HashSet<String> providedFeatures) {
        boolean allowed = true;

        if(rules != null) {
            allowed = false;

            for(int i=0; i<rules.size(); i++) {
                Rule r = rules.get(i);
                if(checkRuleCondition(r, providedFeatures)) {
                    allowed = (r.action == ALLOW);
                }
            }
        }

        return allowed;
    }

    private static boolean checkRuleCondition(Rule r, HashSet<String> providedFeatures) {
        boolean osCheck = r.osName == null || OS.matchOsNameAndVersion(r.osName, r.osVersion);
        boolean osArchCheck = r.osArch == null || OS.matchOsArch(r.osArch);
        boolean featureCheck = true;

        for(Map.Entry<String, Boolean> featureEntry : r.features.entrySet()) {
            if(featureEntry.getValue() != providedFeatures.contains(featureEntry.getKey())) {
                featureCheck = false;
                break;
            }
        }

        return osCheck && osArchCheck && featureCheck;
    }
}
