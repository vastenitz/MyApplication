package com.google.myapplication;

import android.content.Context;
import android.os.Build;
import android.provider.Settings;
import android.telephony.TelephonyManager;

import com.prolificinteractive.materialcalendarview.CalendarDay;

import java.text.SimpleDateFormat;
import java.util.Date;

public class CommonUtils {
    public static String getDate(long timestamp) {
        Date mDate = new Date(timestamp);
        SimpleDateFormat format = new SimpleDateFormat("ddMMyyyy");
        String date = format.format(mDate).toString();
        return date;
    }

    public static String getTime(long timestamp) {
        Date mDate = new Date(timestamp);
        SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");
        String time = format.format(mDate).toString();
        return time;
    }

    public static String getDeviceId(Context context) {

        String deviceId;

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            deviceId = Settings.Secure.getString(
                    context.getContentResolver(),
                    Settings.Secure.ANDROID_ID);
        } else {
            final TelephonyManager mTelephony = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            if (mTelephony.getDeviceId() != null) {
                deviceId = mTelephony.getDeviceId();
            } else {
                deviceId = Settings.Secure.getString(
                        context.getContentResolver(),
                        Settings.Secure.ANDROID_ID);
            }
        }

        return deviceId;
    }

    public static String getDateFromCalendar(CalendarDay cldDay) {
        if (cldDay != null) {
            String searchDate =  "";
            searchDate += cldDay.getDay() > 9? cldDay.getDay() : "0" + cldDay.getDay();
            searchDate += cldDay.getMonth() > 9 ? cldDay.getMonth() : "0" + cldDay.getMonth();
            searchDate += cldDay.getYear();
            return searchDate;
        }
        return null;
    }
}
