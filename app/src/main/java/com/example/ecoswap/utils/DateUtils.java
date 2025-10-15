package com.example.ecoswap.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class DateUtils {
    
    private static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final String DISPLAY_DATE_FORMAT = "MMM dd, yyyy";
    private static final String DISPLAY_TIME_FORMAT = "hh:mm a";
    
    // Get current timestamp
    public static String getCurrentTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat(DEFAULT_DATE_FORMAT, Locale.getDefault());
        return sdf.format(new Date());
    }
    
    // Format date for display
    public static String formatDateForDisplay(String dateString) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat(DEFAULT_DATE_FORMAT, Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat(DISPLAY_DATE_FORMAT, Locale.getDefault());
            Date date = inputFormat.parse(dateString);
            return date != null ? outputFormat.format(date) : dateString;
        } catch (ParseException e) {
            e.printStackTrace();
            return dateString;
        }
    }
    
    // Format time for display
    public static String formatTimeForDisplay(String dateString) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat(DEFAULT_DATE_FORMAT, Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat(DISPLAY_TIME_FORMAT, Locale.getDefault());
            Date date = inputFormat.parse(dateString);
            return date != null ? outputFormat.format(date) : dateString;
        } catch (ParseException e) {
            e.printStackTrace();
            return dateString;
        }
    }
    
    // Get time ago string (e.g., "2 hours ago")
    public static String getTimeAgo(String dateString) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(DEFAULT_DATE_FORMAT, Locale.getDefault());
            Date date = sdf.parse(dateString);
            if (date == null) return dateString;
            
            long time = date.getTime();
            long now = System.currentTimeMillis();
            
            long diff = now - time;
            
            if (diff < TimeUnit.MINUTES.toMillis(1)) {
                return "Just now";
            } else if (diff < TimeUnit.HOURS.toMillis(1)) {
                long minutes = TimeUnit.MILLISECONDS.toMinutes(diff);
                return minutes + " minute" + (minutes > 1 ? "s" : "") + " ago";
            } else if (diff < TimeUnit.DAYS.toMillis(1)) {
                long hours = TimeUnit.MILLISECONDS.toHours(diff);
                return hours + " hour" + (hours > 1 ? "s" : "") + " ago";
            } else if (diff < TimeUnit.DAYS.toMillis(7)) {
                long days = TimeUnit.MILLISECONDS.toDays(diff);
                return days + " day" + (days > 1 ? "s" : "") + " ago";
            } else {
                return formatDateForDisplay(dateString);
            }
        } catch (ParseException e) {
            e.printStackTrace();
            return dateString;
        }
    }
    
    // Parse date string to Date object
    public static Date parseDate(String dateString) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(DEFAULT_DATE_FORMAT, Locale.getDefault());
            return sdf.parse(dateString);
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    // Check if date is in the past
    public static boolean isPastDate(String dateString) {
        Date date = parseDate(dateString);
        if (date == null) return false;
        return date.before(new Date());
    }
    
    // Check if date is in the future
    public static boolean isFutureDate(String dateString) {
        Date date = parseDate(dateString);
        if (date == null) return false;
        return date.after(new Date());
    }
}
