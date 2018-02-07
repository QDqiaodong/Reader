package com.github.reader.utils;

import android.content.Context;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class to help handle basic type computation and conversion. And dynamic using of Classes.
 *
 * @author Bo Hu
 */
public class LangUtils {

    private LangUtils() {
    }


    /**
     * @param text
     * @return
     */
    public static boolean parseBoolean(Object text) {
        if (text instanceof Integer) {
            return (Integer) text > 0;
        } else if (text instanceof Long) {
            return (Long) text > 0;
        } else if (text instanceof String) {
            if (isEmpty((String) text)) {
                return false;
            }
            return text.equals("true");
        } else if (text instanceof Boolean) {
            return (Boolean) text;
        }
        return false;
    }

    /**
     * Parse int from a String object.
     *
     * @param text
     * @return int
     */
    public static int parseInt(String text) {
        return parseInt(text, -1);
    }

    /**
     * Parse int from a String object based on a default value.
     *
     * @param text
     * @param defaultValue
     * @return
     */
    public static int parseInt(String text, int defaultValue) {
        if (text == null)
            return defaultValue;
        try {
            return Integer.parseInt(text);
        } catch (Exception e) {
            return defaultValue;
        }
    }
    public static String parseString(Object o) {
        return parseString(o, "");
    }

    public static String parseString(Object o, String defaultValue) {
        String ret = String.valueOf(o);
        if (ret == null || "null".equalsIgnoreCase(ret)) {
            ret = defaultValue;
            // LogUtils.d("parseString ret = null o = %s",o);
        }
        return ret;
    }

    /**
     * Check if a CharSequence is empty.
     *
     * @param s
     * @return boolean
     */
    public static boolean isEmpty(CharSequence s) {
        return s == null || s.length() == 0;
    }

    /**
     * Check if a byte[] is not empty.
     *
     * @param s
     * @return boolean
     */
    public static boolean isNotEmpty(byte[] s) {
        return s != null && s.length > 0;
    }

    /**
     * Check if a CharSequence is not empty.
     *
     * @param s
     * @return boolean
     */
    public static boolean isNotEmpty(CharSequence s) {
        return s != null && s.length() > 0;
    }

    /**
     * @param <E>
     * @param list
     * @return
     */
    public static <E> boolean isEmpty(Collection<E> list) {
        return list == null || list.size() == 0;
    }

    /**
     * @param list
     * @return
     */
    public static <E> boolean isNotEmpty(Collection<E> list) {
        return list != null && list.size() > 0;
    }

    /**
     * 判断数组是否为空
     *
     * @param array 数组
     * @param <E>   泛型，数组的数据类型
     * @return 数组是否为空
     */
    public static <E> boolean isEmpty(E[] array) {
        return array == null || array.length == 0;
    }


    /**
     * /**
     *
     * @param list
     * @return the first obj or null
     */
    public static <E> E getFirstObj(List<E> list) {
        return isNotEmpty(list) ? list.get(0) : null;
    }

    /**
     * @param list
     * @return the last obj or null
     */
    public static <E> E getLastObj(ArrayList<E> list) {
        return isNotEmpty(list) ? list.get(list.size() - 1) : null;
    }

    /***
     * @param date
     * @return
     */
    public static int getYear(Date date) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        return c.get(Calendar.YEAR);
    }

    /***
     * @param date
     * @return 1-12
     */
    public static int getMonth(Date date) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        return c.get(Calendar.MONTH) + 1;
    }

    /***
     * Get day of month 1-30 or 31 29
     *
     * @param date
     * @return
     */
    public static int getMonthDay(Date date) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        return c.get(Calendar.DAY_OF_MONTH);
    }

    /**
     * Get day of week 1-7
     *
     * @param date
     * @return
     */
    public static int getWeekDay(Date date) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        return c.get(Calendar.DAY_OF_WEEK);
    }

    public static boolean isSameDay(Date date1, Date date2) {
        if (date1 == null || date2 == null) {
//            w(" date is invalid date1 = %s date2 = %s", date1, date2);
            return false;
        }
        Calendar calendar1 = Calendar.getInstance(), calendar2 = Calendar.getInstance();
        calendar1.setTime(date1);
        calendar2.setTime(date2);
        if (calendar1.get(Calendar.YEAR) == calendar2.get(Calendar.YEAR)
                && calendar1.get(Calendar.MONTH) == calendar2.get(Calendar.MONTH)
                && calendar1.get(Calendar.DAY_OF_MONTH) == calendar2.get(Calendar.DAY_OF_MONTH))
            return true;
        return false;
    }

    public static boolean isSameMonth(Date date1, Date date2) {
        if (date1 == null || date2 == null) {
//            w(" date is invalid date1 = %s date2 = %s", date1, date2);
            return false;
        }
        Calendar calendar1 = Calendar.getInstance(), calendar2 = Calendar.getInstance();
        calendar1.setTime(date1);
        calendar2.setTime(date2);
        if (calendar1.get(Calendar.YEAR) == calendar2.get(Calendar.YEAR)
                && calendar1.get(Calendar.MONTH) == calendar2.get(Calendar.MONTH))
            return true;
        return false;
    }

    public static boolean isSameWeek(Date date1, Date date2) {
        if (date1 == null || date2 == null) {
//            w(" date is invalid date1 = %s date2 = %s", date1, date2);
            return false;
        }
        Calendar calendar1 = Calendar.getInstance(), calendar2 = Calendar.getInstance();
        calendar1.setTime(date1);
        calendar2.setTime(date2);
        if (calendar1.get(Calendar.YEAR) == calendar2.get(Calendar.YEAR)
                && calendar1.get(Calendar.WEEK_OF_YEAR) == calendar2.get(Calendar.WEEK_OF_YEAR))
            return true;
        return false;
    }

    public static boolean isSameYear(Date date1, Date date2) {
        if (date1 == null || date2 == null) {
//            w(" date is invalid date1 = %s date2 = %s", date1, date2);
            return false;
        }
        Calendar calendar1 = Calendar.getInstance(), calendar2 = Calendar.getInstance();
        calendar1.setTime(date1);
        calendar2.setTime(date2);
        if (calendar1.get(Calendar.YEAR) == calendar2.get(Calendar.YEAR))
            return true;
        return false;
    }


    /**
     * @param str 需要过滤的字符串
     * @return
     * @Description:过滤数字以外的字符
     */
    public static String filterUnNumber(String str) {
        // 只允数字
        String regEx = "[^0-9]";
        Pattern p = Pattern.compile(regEx);
        Matcher m = p.matcher(str);
        //替换与模式匹配的所有字符（即非数字的字符将被""替换）
        return m.replaceAll("").trim();

    }



    /**
     * Convert dip to pixels.
     *
     * @param dip
     * @return int
     */
    public static int rp(int dip) {
        return rp(AppUtils.getAppContext(), dip);
    }

    /**
     * Convert Dip to pixels.
     *
     * @param c
     * @param dip
     * @return int
     */
    public static int rp(Context c, int dip) {
        // return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
        // dip, c.getResources().getDisplayMetrics());
        if (c == null)
            c = AppUtils.getAppContext();
        if (c == null) {
//            e("context is null for rp");
            return dip;
        }
        return dip > 0 ? (int) (c.getResources().getDisplayMetrics().density * dip + 0.5f) : dip;
    }

    /**
     * Convert an Object to a String object.
     *
     * @param o
     * @return String
     */
    public static String toString(Object o) {
        return o == null ? "" : o.toString();
    }
}

