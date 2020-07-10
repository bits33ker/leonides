package com.herod.utils.entities;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.Base64;

public class StringUtils {
    public static final String SPACE = " ";
    public static final String TRUE = "true";
    public static final String FALSE = "false";
    public static String EMPTY = "";
    public static String DEFAULT_ENCODING = "utf-8";

    public StringUtils() {
    }

    public static boolean isNullOrEmpty(String value) {
        return value == null || value.length() == 0;
    }

    public static boolean areEqualsCaseInsensitive(String a, String b) {
        return a != null && b != null && a.equalsIgnoreCase(b) || a == null && b == null;
    }

    public static boolean isNumeric(String value) {
        return value != null && value.matches("[-+]?\\d*\\.?\\d+");
    }

    public static boolean equalsAny(String value, boolean ignoreCase, String... others) {
        if (value == null) {
            return false;
        } else if (others == null) {
            return false;
        } else {
            String[] var3 = others;
            int var4 = others.length;
            int var5 = 0;

            while(true) {
                if (var5 >= var4) {
                    return false;
                }

                String other = var3[var5];
                if (ignoreCase) {
                    if (value.equalsIgnoreCase(other)) {
                        break;
                    }
                } else if (value.equals(other)) {
                    break;
                }

                ++var5;
            }

            return true;
        }
    }

    public static boolean equalsAny(String value, String... others) {
        return equalsAny(value, true, others);
    }

    public static boolean startsWith(String value, boolean ignoreCase, String... others) {
        String[] var3 = others;
        int var4 = others.length;
        int var5 = 0;

        while(true) {
            if (var5 >= var4) {
                return false;
            }

            String other = var3[var5];
            if (ignoreCase) {
                if (value.toLowerCase().startsWith(other.toLowerCase())) {
                    break;
                }
            } else if (value.startsWith(other)) {
                break;
            }

            ++var5;
        }

        return true;
    }

    public static String getStringWindows1252(String value) {
        String windows1252Encoded;
        try {
            windows1252Encoded = new String(value.getBytes("UTF-8"), Charset.forName("Windows-1252"));
        } catch (UnsupportedEncodingException var3) {
            windows1252Encoded = value;
        }

        return windows1252Encoded;
    }

    public static String getStringUTF8(String value) {
        byte[] bytes = value.getBytes();

        String utf8Encoded;
        try {
            utf8Encoded = new String(bytes, "UTF-8");
        } catch (UnsupportedEncodingException var4) {
            utf8Encoded = value;
        }

        return utf8Encoded;
    }

    public static boolean startsWithIgnoreCase(String value, String... others) {
        return startsWith(value, true, others);
    }

    public static boolean contains(String value, boolean ignoreCase, String... others) {
        String[] var3 = others;
        int var4 = others.length;
        int var5 = 0;

        while(true) {
            if (var5 >= var4) {
                return false;
            }

            String other = var3[var5];
            if (ignoreCase) {
                if (value.toLowerCase().contains(other.toLowerCase())) {
                    break;
                }
            } else if (value.startsWith(other)) {
                break;
            }

            ++var5;
        }

        return true;
    }

    public static boolean containsIgnoreCase(String value, String... others) {
        return contains(value, true, others);
    }

    public static boolean containsIgnoreAccent(String str1, String str2) {
        boolean isContains = false;
        str1 = Normalizer.normalize(str1, Form.NFD);
        str1 = str1.replaceAll("[^\\p{ASCII}]", "");
        str1 = str1.replaceAll(" ", "");
        str2 = Normalizer.normalize(str2, Form.NFD);
        str2 = str2.replaceAll("[^\\p{ASCII}]", "");
        str2 = str2.replaceAll(" ", "");
        isContains = containsIgnoreCase(str1, str2);
        return isContains;
    }

    public static String urlDecode(String toDecode, String encoding) throws UnsupportedEncodingException {
        return URLDecoder.decode(toDecode, encoding);
    }

    public static String toMD5(String value) throws IOException {
        return new String(BytesUtils.md5(value.getBytes()));
    }

    public static String urlEncode(String toEncode, String encoding) throws UnsupportedEncodingException {
        return URLEncoder.encode(toEncode, encoding).replace("+", "%20");
    }

    public static String urlDecode(String toDecode) {
        try {
            return URLDecoder.decode(toDecode, DEFAULT_ENCODING);
        } catch (UnsupportedEncodingException var2) {
            return toDecode;
        }
    }

    public static String urlEncode(String toEncode) {
        try {
            return URLEncoder.encode(toEncode, DEFAULT_ENCODING);
        } catch (UnsupportedEncodingException var2) {
            return toEncode;
        }
    }

    public static String encodeBase64(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes());
    }

    public static String decodeBase64(String value) {
        return new String(Base64.getDecoder().decode(value));
    }

    public static String toCamelCase(String s) {
        String toParse = s.replace(" ", "_");
        String camelCased = toProperCase(toProperCase(s, "-"), "_");
        return String.format("%s%s", camelCased.substring(0, 1).toLowerCase(), camelCased.substring(1, camelCased.length()));
    }

    private static String toProperCase(String s, String separator) {
        String[] parts = s.split(separator);
        StringBuffer camelCaseStringBuffer = new StringBuffer();
        if (parts.length > 1) {
            String[] var4 = parts;
            int var5 = parts.length;

            for(int var6 = 0; var6 < var5; ++var6) {
                String part = var4[var6];
                camelCaseStringBuffer.append(toProperCase(part));
            }
        } else {
            camelCaseStringBuffer.append(s);
        }

        return camelCaseStringBuffer.toString();
    }

    static String toProperCase(String s) {
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }
}
