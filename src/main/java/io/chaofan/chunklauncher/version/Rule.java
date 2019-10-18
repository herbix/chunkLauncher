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

    public HashMap<String, Boolean> features = new HashMap<>();

    public Rule(JSONObject json) {
        action = json.getString("action").equals("allow") ? ALLOW : DISALLOW;
        if (json.has("os")) {
            JSONObject os = json.getJSONObject("os");
            if (os.has("name"))
                osName = os.getString("name");
            if (os.has("version"))
                osVersion = os.getString("version");
            if (os.has("arch"))
                osArch = os.getString("arch");
        }

        if (json.has("features")) {
            JSONObject obj = json.getJSONObject("features");
            for (String key : obj.keySet()) {
                features.put(key, obj.getBoolean(key));
            }
        }
    }

    public static boolean isAllowed(List<Rule> rules) {
        return isAllowed(rules, new HashSet<>());
    }

    public static boolean isAllowed(List<Rule> rules, Set<String> providedFeatures) {
        return rules == null ||
                rules.stream()
                        .filter(r -> r.isAllowed(providedFeatures))
                        .map(r -> r.action == ALLOW)
                        .reduce((a, b) -> b)
                        .orElse(false);
    }

    public boolean isAllowed(Set<String> providedFeatures) {
        boolean osCheck = osName == null || OS.matchOsNameAndVersion(osName, osVersion);
        boolean osArchCheck = osArch == null || OS.matchOsArch(osArch);
        boolean featureCheck = features.entrySet().stream()
                .allMatch(e -> e.getValue() == providedFeatures.contains(e.getKey()));

        return osCheck && osArchCheck && featureCheck;
    }
}
