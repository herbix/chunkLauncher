package io.chaofan.chunklauncher.util;

import org.json.JSONArray;
import org.json.JSONObject;

public class JsonUtil {
    public static JSONObject mergeJson(JSONObject patchedJson, JSONObject originalJson, boolean reverseArray) {
        JSONObject output = new JSONObject(originalJson.toString());
        for (String key : patchedJson.keySet()) {
            Object patchedObject = patchedJson.get(key);
            if (!originalJson.has(key)) {
                output.put(key, patchedObject);
                continue;
            }

            Object originalObject = originalJson.get(key);
            if (originalObject instanceof JSONArray && patchedObject instanceof JSONArray) {
                output.put(key, mergeJson((JSONArray) patchedObject, (JSONArray) originalObject, reverseArray));
            } else if (originalObject instanceof JSONObject && patchedObject instanceof JSONObject) {
                output.put(key, mergeJson((JSONObject) patchedObject, (JSONObject) originalObject, reverseArray));
            } else {
                output.put(key, patchedObject);
            }
        }
        return output;
    }

    public static JSONArray mergeJson(JSONArray patchedJson, JSONArray originalJson, boolean reverseArray) {
        if (reverseArray) {
            JSONArray t = patchedJson;
            patchedJson = originalJson;
            originalJson = t;
        }
        JSONArray result = new JSONArray(originalJson.toString());
        for (Object item : patchedJson) {
            result.put(item);
        }
        return result;
    }
}
