package com.mp.karental.payment.util;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class DateUtils {
    protected static final SimpleDateFormat ISO_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    // Use Asia/Ho_Chi_Minh timezone for Vietnam (GMT+7)
    private static final TimeZone VIETNAM_TIMEZONE = TimeZone.getTimeZone("Asia/Ho_Chi_Minh");
    
    public static Date parseISO(String date) {
        try {
            return ISO_DATE_FORMAT.parse(date);
        } catch (Exception e) {
            return null;
        }
    }

    public static long getDiffInDays(LocalDate date1, LocalDate date2) {
        return ChronoUnit.DAYS.between(date1, date2);
    }

    public static LocalDate parse(String date) {
        return LocalDate.parse(date);
    }

    public static String getVnTime() {
        Calendar calendar = Calendar.getInstance(VIETNAM_TIMEZONE);
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        formatter.setTimeZone(VIETNAM_TIMEZONE);
        return formatter.format(calendar.getTime());
    }

    public static String formatVnTime(Calendar calendar) {
        // Ensure the calendar timezone is set correctly
        if (calendar.getTimeZone().getID().equals(VIETNAM_TIMEZONE.getID())) {
            SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
            formatter.setTimeZone(VIETNAM_TIMEZONE);
            return formatter.format(calendar.getTime());
        } else {
            // If calendar is in different timezone, convert it
            Calendar vnCalendar = Calendar.getInstance(VIETNAM_TIMEZONE);
            vnCalendar.setTimeInMillis(calendar.getTimeInMillis());
            SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
            formatter.setTimeZone(VIETNAM_TIMEZONE);
            return formatter.format(vnCalendar.getTime());
        }
    }

//    public static void main(String[] agrs) {
//        LocalDate date1 = LocalDate.parse("2022-06-12");
//        LocalDate date2 = LocalDate.parse("2022-06-15");
//        System.out.println(getDiffInDays(date1, date2));
//    }
}
