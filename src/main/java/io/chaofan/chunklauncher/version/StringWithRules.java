package io.chaofan.chunklauncher.version;

import io.chaofan.chunklauncher.util.StreamUtil;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class StringWithRules {

    private String[] values;
    private List<Rule> rules;

    public StringWithRules(Object objectFromJson) {
        if (objectFromJson instanceof String) {
            values = new String[]{(String) objectFromJson};

        } else if (objectFromJson instanceof JSONObject) {
            JSONArray rls = ((JSONObject) objectFromJson).getJSONArray("rules");
            rules = Arrays.asList(
                    StreamUtil.toStream(rls)
                            .map(obj -> new Rule((JSONObject) obj))
                            .toArray(Rule[]::new)
            );

            Object value = ((JSONObject) objectFromJson).get("value");
            if (value instanceof String) {
                values = new String[]{(String) value};
            } else if (value instanceof JSONArray) {
                values = StreamUtil.toStream((JSONArray) value)
                        .map(Object::toString)
                        .toArray(String[]::new);
            }
        }
    }

    public String[] getValue(Set<String> providedFeatures) {
        if (Rule.isAllowed(rules, providedFeatures)) {
            return values;
        } else {
            return new String[0];
        }
    }
}
