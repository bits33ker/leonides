package com.herod.utils.entities;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class DateTimeUtils {
    public static final String TWITTER_DATE_FORMAT = "EEE MMM dd HH:mm:ss ZZZZZ yyyy";
    public static final String FACEBOOK_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZ";
    public static final String MITROL_INTERACTION_DATE_FORMAT = "yyMMddhhmmssSSS";
    public static final String MITROL_DATE_HOUR_MILLIS_FORMAT = "dd/MM/yyyy HH:mm:ss.SSS";
    public static final String MITROL_HYPHEN_DATE_HOUR_MILLIS_FORMAT = "dd-MM-yyyy HH:mm:ss.SSS";
    public static final String MITROL_DATE_HOUR_FORMAT = "dd/MM/yyyy HH:mm:ss";
    public static final String MITROL_DATE_FORMAT = "dd/MM/yyyy";
    public static final String MITROL_NULL_DATE = "1899-12-30 00:00:00.0";
    private static final Map<String, String> DATE_FORMAT_REGEXPS = new HashMap<String, String>() {
        {
            this.put("^\\d{8}$", "yyyyMMdd");
            this.put("^\\d{1,2}-\\d{1,2}-\\d{4}$", "dd-MM-yyyy");
            this.put("^\\d{4}-\\d{1,2}-\\d{1,2}$", "yyyy-MM-dd");
            this.put("^\\d{1,2}/\\d{1,2}/\\d{4}$", "MM/dd/yyyy");
            this.put("^\\d{4}/\\d{1,2}/\\d{1,2}$", "yyyy/MM/dd");
            this.put("^\\d{1,2}\\s[a-z]{3}\\s\\d{4}$", "dd MMM yyyy");
            this.put("^\\d{1,2}\\s[a-z]{4,}\\s\\d{4}$", "dd MMMM yyyy");
            this.put("^\\d{12}$", "yyyyMMddHHmm");
            this.put("^\\d{8}\\s\\d{4}$", "yyyyMMdd HHmm");
            this.put("^\\d{1,2}-\\d{1,2}-\\d{4}\\s\\d{1,2}:\\d{2}$", "dd-MM-yyyy HH:mm");
            this.put("^\\d{4}-\\d{1,2}-\\d{1,2}\\s\\d{1,2}:\\d{2}$", "yyyy-MM-dd HH:mm");
            this.put("^\\d{1,2}/\\d{1,2}/\\d{4}\\s\\d{1,2}:\\d{2}$", "MM/dd/yyyy HH:mm");
            this.put("^\\d{4}/\\d{1,2}/\\d{1,2}\\s\\d{1,2}:\\d{2}$", "yyyy/MM/dd HH:mm");
            this.put("^\\d{1,2}\\s[a-z]{3}\\s\\d{4}\\s\\d{1,2}:\\d{2}$", "dd MMM yyyy HH:mm");
            this.put("^\\d{1,2}\\s[a-z]{4,}\\s\\d{4}\\s\\d{1,2}:\\d{2}$", "dd MMMM yyyy HH:mm");
            this.put("^\\d{14}$", "yyyyMMddHHmmss");
            this.put("^\\d{8}\\s\\d{6}$", "yyyyMMdd HHmmss");
            this.put("^\\d{1,2}-\\d{1,2}-\\d{4}\\s\\d{1,2}:\\d{2}:\\d{2}$", "dd-MM-yyyy HH:mm:ss");
            this.put("^\\d{4}-\\d{1,2}-\\d{1,2}\\s\\d{1,2}:\\d{2}:\\d{2}$", "yyyy-MM-dd HH:mm:ss");
            this.put("^\\d{1,2}/\\d{1,2}/\\d{4}\\s\\d{1,2}:\\d{2}:\\d{2}$", "MM/dd/yyyy HH:mm:ss");
            this.put("^\\d{4}/\\d{1,2}/\\d{1,2}\\s\\d{1,2}:\\d{2}:\\d{2}$", "yyyy/MM/dd HH:mm:ss");
            this.put("^\\d{1,2}\\s[a-z]{3}\\s\\d{4}\\s\\d{1,2}:\\d{2}:\\d{2}$", "dd MMM yyyy HH:mm:ss");
            this.put("^\\d{1,2}\\s[a-z]{4,}\\s\\d{4}\\s\\d{1,2}:\\d{2}:\\d{2}$", "dd MMMM yyyy HH:mm:ss");
            this.put("^\\w{3}\\s\\w{3}\\s\\d{2}\\s\\d{2}:\\d{2}:\\d{2}\\s\\+\\d{4}\\s\\d{4}$", "EEE MMM dd HH:mm:ss ZZZZZ yyyy");
            this.put("^\\d{2}-\\d{2}-\\d{4} \\d{2}:\\d{2}:\\d{2}.\\d{3}", "dd-MM-yyyy HH:mm:ss.SSS");
        }
    };

    public DateTimeUtils() {
    }

    public static boolean macthDateFormat(String dateString, String dateFormat) {
        Optional<String> optRegex = DATE_FORMAT_REGEXPS.keySet().stream().filter((x) -> {
            return ((String)DATE_FORMAT_REGEXPS.get(x)).equals(dateFormat);
        }).findFirst();
        if (optRegex.isPresent()) {
            String regexp = (String)optRegex.get();
            if (dateString != null && dateString.toLowerCase().matches(regexp)) {
                return true;
            }
        }

        return false;
    }

    public static String determineDateFormat(String dateString) {
        Iterator var1 = DATE_FORMAT_REGEXPS.keySet().iterator();

        String regexp;
        do {
            if (!var1.hasNext()) {
                return null;
            }

            regexp = (String)var1.next();
        } while(!dateString.toLowerCase().matches(regexp));

        return (String)DATE_FORMAT_REGEXPS.get(regexp);
    }

    public static SimpleDateFormat getDateFormat(String dateString) {
        Iterator var1 = DATE_FORMAT_REGEXPS.keySet().iterator();

        String regexp;
        do {
            if (!var1.hasNext()) {
                return null;
            }

            regexp = (String)var1.next();
        } while(!dateString.toLowerCase().matches(regexp));

        String dateFormat = (String)DATE_FORMAT_REGEXPS.get(regexp);
        SimpleDateFormat simpleDateFormat;
        if (dateFormat.equals("EEE MMM dd HH:mm:ss ZZZZZ yyyy")) {
            simpleDateFormat = new SimpleDateFormat(dateFormat, Locale.ENGLISH);
            simpleDateFormat.setLenient(true);
        } else {
            simpleDateFormat = new SimpleDateFormat(dateFormat);
        }

        return simpleDateFormat;
    }

    public static String getStringFromInstant(Instant instant, String format) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format).withZone(ZoneId.systemDefault());
        String output = formatter.format(instant);
        return output;
    }

    public static Instant getInstantFromString(String value, String format) {
        SimpleDateFormat formatter = new SimpleDateFormat(format);
        if (!StringUtils.isNullOrEmpty(value) && (!"1899-12-30 00:00:00.0".equals(value) || !"dd-MM-yyyy HH:mm:ss.SSS".equals(format))) {
            try {
                Date date = formatter.parse(value);
                return date.toInstant();
            } catch (ParseException var5) {
                return Instant.parse(value);
            }
        } else {
            return null;
        }
    }

    public static Instant getInstantFromString(String value, String format, Instant defaultValue) {
        try {
            return getInstantFromString(value, format);
        } catch (Exception var4) {
            return defaultValue;
        }
    }

    public static Instant getInstantFromString(String value, Instant defaultValue) {
        try {
            return getInstantFromString(value);
        } catch (Exception var3) {
            return defaultValue;
        }
    }

    public static Instant getInstantFromString(String value) {
        SimpleDateFormat formatter = getDateFormat(value);
        if (formatter == null) {
            return LocalDateTime.parse(value, DateTimeFormatter.ISO_DATE_TIME).toInstant(ZoneOffset.UTC);
        } else {
            try {
                Date date = formatter.parse(value);
                return date.toInstant();
            } catch (ParseException var4) {
                return Instant.parse(value);
            }
        }
    }

    public static long diffDuration(Temporal from, Temporal to, TimeUnit timeUnit) {
        switch(timeUnit) {
            case DAYS:
                return ChronoUnit.DAYS.between(from, to);
            case HOURS:
                return ChronoUnit.HOURS.between(from, to);
            case MINUTES:
                return ChronoUnit.MINUTES.between(from, to);
            case SECONDS:
                return ChronoUnit.SECONDS.between(from, to);
            case MILLISECONDS:
                return ChronoUnit.MILLIS.between(from, to);
            default:
                return ChronoUnit.SECONDS.between(from, to);
        }
    }

    public static String stringDiffDuration(Temporal from, Temporal to) {
        long seconds = ChronoUnit.SECONDS.between(from, to);
        long hours = seconds / 3600L;
        long minutes = seconds % 3600L / 60L;
        seconds %= 60L;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    public static boolean isSameDay(Instant instant1, Instant instant2) {
        LocalDateTime localDateTime1 = LocalDateTime.ofInstant(instant1, ZoneId.systemDefault());
        LocalDateTime localDateTime2 = LocalDateTime.ofInstant(instant2, ZoneId.systemDefault());
        boolean sameDay = false;
        int year1 = localDateTime1.getYear();
        int month1 = localDateTime1.getMonthValue();
        int day1 = localDateTime1.getDayOfMonth();
        int year2 = localDateTime2.getYear();
        int month2 = localDateTime2.getMonthValue();
        int day2 = localDateTime2.getDayOfMonth();
        if (year1 == year2 && month1 == month2 && day1 == day2) {
            sameDay = true;
        }

        return sameDay;
    }
}
