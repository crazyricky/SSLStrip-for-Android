package com.crazyricky.androidsslstrip;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.util.Log;

public class Logging {
    
    private static final String TAG = "SSLStrip";
    private static boolean DEBUG = true;
    
    private static BufferedWriter sBufWriter = null;
    private static Object sWriterLock = new Object();
    
    public static void updateDebug(String msg) {
        if (DEBUG) { 
            Log.d(TAG, msg);
        }
    }
    
    private static String now() {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-dd-mm-hh:mm:ss");
        return dateFormat.format(cal.getTime());
    }
    
    public static void startLogging(Context context) {
        
        SharedPreferences sp = context.getSharedPreferences("config", Context.MODE_PRIVATE);        
        boolean saveLogs = sp.getBoolean("save_logs", false);
        if (!saveLogs) {
            // do not init writer when we don't wanna save logs
            return;
        }
        
        synchronized(sWriterLock) {
            if (sBufWriter != null) {
                try {
                    sBufWriter.close();
                    sBufWriter = null;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            try {
                File dir = new File(Environment.getExternalStorageDirectory(), "sslstrip");
                dir.mkdir();
                File logFile = new File(dir, now() + ".txt");
                FileWriter fWriter = new FileWriter(logFile);
                sBufWriter = new BufferedWriter(fWriter);
            } catch (Exception e) {
                e.printStackTrace();
            }
            
        }
    }
    
    public static void stopLogging() {
        synchronized(sWriterLock) {
            if (sBufWriter != null) {
                try {
                    sBufWriter.close();
                    sBufWriter = null;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    private static void writeLog(final String msg) {
        new Thread() {
            public void run() {
                synchronized(sWriterLock) {
                    try {
                        if (sBufWriter != null) {
                            sBufWriter.write(msg);
                            sBufWriter.newLine();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }.start();
    }
    
    public static void updateLog(String newMessage) {
        Main.updateLog(newMessage);
        writeLog(newMessage);
    }
}
