package com.crazyricky.androidsslstrip;

import android.util.Log;

public class Logging {
    
    private static final String TAG = "SSLStrip";
    private static boolean DEBUG = true;
    
    public static void updateDebug(String msg) {
        if (DEBUG) { 
            Log.d(TAG, msg);
        }
    }
    
    public static void updateLog(String newMessage) {
        Main.updateLog(newMessage);
    }
}
