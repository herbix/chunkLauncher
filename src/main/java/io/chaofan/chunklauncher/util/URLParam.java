package io.chaofan.chunklauncher.util;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;
import java.util.Map.Entry;

public class URLParam {

    public static String mapToParamString(Map<String, String> map) {

        StringBuilder sb = new StringBuilder();

        for (Entry<String, String> entry : map.entrySet()) {

            try {
                sb.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
                sb.append('=');
                sb.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
                sb.append('&');
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

        }

        if (sb.length() > 0)
            sb.deleteCharAt(sb.length() - 1);

        return sb.toString();
    }
}
