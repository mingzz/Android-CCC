package com.ljjqdc.app.c3.utils;

import android.content.Context;

/**
 * Created by ljj on 2014/10/22.
 */
public class DataUtil {
    public static String username;//BluetoothAdapter.EXTRA_LOCAL_NAME
    public static String password;
    public static String ip = "demo.anychat.cn";
    public static int port = 8906;
    public static int roomID = 0;

    public static String connectDeviceName = "";

    public final static String SPF_WIFI_NAME = "wifi";
    public final static String SPF_WIFI_ADDRESS = "address";
    public final static String SPF_WIFI_PORT = "port";

    public static String getSpfString(Context context, String name, String key, String defaultValue){
        return context.getSharedPreferences(name, Context.MODE_PRIVATE).getString(key,defaultValue);
    }

    public static void setSpfString(Context context, String name, String key, String value){
        context.getSharedPreferences(name, Context.MODE_PRIVATE).edit().putString(key,value).apply();
    }

}
