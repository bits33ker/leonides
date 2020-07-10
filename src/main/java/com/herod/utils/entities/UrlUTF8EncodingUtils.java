package com.herod.utils.entities;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class UrlUTF8EncodingUtils {
    public UrlUTF8EncodingUtils() {
    }

    public static String urlEncodeUTF8(String s) {
        try {
            return s != null ? URLEncoder.encode(s, "UTF-8") : null;
        } catch (UnsupportedEncodingException var2) {
            throw new UnsupportedOperationException(var2);
        }
    }

    public static String urlEncodeUTF8(Map<String, String> map) {
        StringBuilder sb = new StringBuilder();

        Entry entry;
        for(Iterator var2 = map.entrySet().iterator(); var2.hasNext(); sb.append(String.format("%s=%s", urlEncodeUTF8((String)entry.getKey()), urlEncodeUTF8((String)entry.getValue())))) {
            entry = (Entry)var2.next();
            if (sb.length() > 0) {
                sb.append("&");
            }
        }

        return sb.toString();
    }
}
