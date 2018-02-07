package com.github.reader.utils;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;

/**
 * Created by ZhanTao on 6/9/17.
 */

public class SystemPropertyUtils {
    /**
     * Equals: SystemProperties.set(name, value);
     * @param context
     * @param name
     * @param value
     */
    public static void setSystemProperty(Context context, String name, String value){
        if(name == null || name.equals("") || value == null || value.equals(""))
            return;
        String AUTHORITY = "com.okay.property.provider";
        String PROPERTY_PATH = "property";
        Uri PROPERTY_URI = Uri.parse("content://" + AUTHORITY + "/" + PROPERTY_PATH);
        String TABLE_COLUMN_PROPERTY_NAME = "name";
        String TABLE_COLUMN_PROPERTY_VALUE = "value";

        ContentValues values = new ContentValues();
        values.put(TABLE_COLUMN_PROPERTY_NAME, name);
        values.put(TABLE_COLUMN_PROPERTY_VALUE, value);

        try {
            Uri uri = context.getContentResolver().insert(PROPERTY_URI, values);
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
