package com.herod.utils.entities;

public class BooleanUtils {
    public BooleanUtils() {
    }

    public static Boolean parse(String value) {
        return StringUtils.isNullOrEmpty(value) ? null : valueOf(value);
    }

    public static boolean valueOfOrDefault(String value, boolean defaultVaule) {
        if (StringUtils.isNullOrEmpty(value)) {
            return defaultVaule;
        } else if (StringUtils.isNumeric(value)) {
            int iValue = Integer.valueOf(value);
            return iValue != 0;
        } else {
            return value.equalsIgnoreCase("true") || value.equalsIgnoreCase("t");
        }
    }

    public static boolean valueOf(String value) {
        if (StringUtils.isNumeric(value)) {
            int iValue = Integer.valueOf(value);
            return iValue != 0;
        } else {
            return value.equalsIgnoreCase("true") || value.equalsIgnoreCase("t");
        }
    }
}
