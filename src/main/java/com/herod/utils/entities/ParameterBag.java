package com.herod.utils.entities;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.time.Instant;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.stream.Collectors;

public class ParameterBag extends LinkedCaseInsensitiveMap<Object> {
    private static final long serialVersionUID = 8051282733202756626L;
    private static final String MIT_ACD_NULL_DATE = "30/12/1899";
    private static final String DEFAULT_ENCODING = "utf-8";

    public ParameterBag(Map<? extends String, ?> m) {
        m.keySet().forEach((k) -> {
            this.put(k, m.get(k));
        });
    }

    public ParameterBag() {
    }

    public static ParameterBag parse(String s, String encoding) throws UnsupportedEncodingException {
        if (StringUtils.isNullOrEmpty(s)) {
            return new ParameterBag();
        } else {
            LinkedHashMap parameters = new LinkedHashMap();
            LinkedList<String> lines = (LinkedList)Arrays.asList(s.split("&")).stream().filter((x) -> {
                return !StringUtils.isNullOrEmpty(x);
            }).collect(Collectors.toCollection(LinkedList::new));
            Iterator var4 = lines.iterator();

            while(var4.hasNext()) {
                String line = (String)var4.next();
                int index = line.indexOf("=");
                if (index != -1) {
                    String substringKey = line.substring(0, index);
                    String substringValue = line.substring(index + 1, line.length());
                    parameters.put(URLDecoder.decode(substringKey, encoding), URLDecoder.decode(substringValue, encoding));
                }
            }

            return new ParameterBag(parameters);
        }
    }

    public static ParameterBag parse(String s) throws UnsupportedEncodingException {
        return parse(s, "utf-8");
    }

    public String getString(String key) {
        Object object = this.get(key);
        if (object == null) {
            return null;
        } else {
            String value = object instanceof Boolean ? ((Boolean)object ? "1" : "0") : object.toString();
            return value;
        }
    }

    public int getInt(String key, int defaultValue) {
        int value;
        try {
            value = Integer.parseInt(this.getString(key));
        } catch (Exception var5) {
            value = defaultValue;
        }

        return value;
    }

    public Integer getInteger(String key) {
        String value = this.getString(key);
        return value != null && !value.isEmpty() ? Integer.parseInt(value) : null;
    }

    public Short getShort(String key) {
        String value = this.getString(key);
        return value != null && !value.isEmpty() ? Short.parseShort(value) : null;
    }

    public Short getShort(String key, Short defaultValue) {
        String value = this.getString(key);
        return value != null && !value.isEmpty() ? Short.parseShort(value) : defaultValue;
    }

    public Long getLong(String key) {
        String value = this.getString(key);
        return value != null && !value.isEmpty() ? Long.parseLong(value) : null;
    }

    public Long getLong(String key, long defaultValue) {
        Long value = this.getLong(key);
        return value == null ? defaultValue : value;
    }

    public Boolean getBoolean(String key) {
        String value = this.getString(key);
        return BooleanUtils.parse(value);
    }

    public Boolean getBoolean(String key, Boolean defaultValue) {
        String value = this.getString(key);
        return BooleanUtils.valueOfOrDefault(value, defaultValue);
    }

    public Double getDouble(String key) {
        return Double.valueOf(this.getString(key));
    }

    public Instant getDateTime(String key) {
        String date = this.getString(key);
        if (date == null) {
            return null;
        } else if (date.startsWith("30/12/1899")) {
            return null;
        } else {
            String dateFormat = date.split(" ").length > 0 && date.indexOf(".") > 0 ? "dd/MM/yyyy HH:mm:ss.SSS" : (date.split(" ").length > 1 ? "dd/MM/yyyy HH:mm:ss" : "dd/MM/yyyy");
            return DateTimeUtils.getInstantFromString(date, dateFormat == null ? "dd/MM/yyyy HH:mm:ss.SSS" : dateFormat);
        }
    }

    public Instant getDateTime(String key, String dateFormat) {
        String date = this.getString(key);
        if (date == null) {
            return null;
        } else {
            return date.startsWith("30/12/1899") ? null : DateTimeUtils.getInstantFromString(date, dateFormat);
        }
    }

    public Object getOrDefault(Object key, Object defaultValue) {
        Object value = this.get(key);
        return value == null ? defaultValue : value;
    }

    public String toString() {
        return (String)this.keySet().stream().filter((x) -> {
            return this.get(x) != null;
        }).map((x) -> {
            return String.format("%s=%s&", x, this.get(x));
        }).collect(Collectors.joining());
    }

    public void putAll(Map<? extends String, ?> map) {
        if (map != null) {
            super.putAll(map);
        }

    }

    public String toStringUrlUtf8Escaped() {
        return (String)this.keySet().stream().filter((x) -> {
            return this.get(x) != null;
        }).map((x) -> {
            return String.format("%s=%s&", x, UrlUTF8EncodingUtils.urlEncodeUTF8(this.get(x).toString()));
        }).collect(Collectors.joining());
    }
}
