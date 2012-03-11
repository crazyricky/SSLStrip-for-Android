package com.crazyricky.androidsslstrip;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.format.Formatter;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class ConfigMenu extends Activity {
    
    private final static String CONFIG_FILE = "config";
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.menu);
        
        final Button understandBtn = (Button) this.findViewById(R.id.understand_btn);
        final EditText wifiEditText = (EditText) findViewById(R.id.wifi_edittext);
        understandBtn.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                String str = wifiEditText.getText().toString();
                
                SharedPreferences sp = ConfigMenu.this.getSharedPreferences(CONFIG_FILE, Context.MODE_PRIVATE);
                sp.edit().putString("wifi", str).commit();
                sp.edit().putBoolean("understand", true).commit();
                ConfigMenu.this.setResult(Activity.RESULT_OK);
                finish();
            }
        });
        
        SharedPreferences sp = ConfigMenu.this.getSharedPreferences(CONFIG_FILE, Context.MODE_PRIVATE);
        String wifi = sp.getString("wifi", "");
        wifiEditText.setText(wifi);
        
        Button detectBtn = (Button) this.findViewById(R.id.auto_btn);
        detectBtn.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                autoSetWifi();
            }
        });
        
        Button licenseBtn = (Button) this.findViewById(R.id.license_btn);
        licenseBtn.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(ConfigMenu.this);
                builder.setMessage(R.string.license)
                       .setCancelable(false)
                       .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                           public void onClick(DialogInterface dialog, int id) {
                               dialog.dismiss();
                           }
                       });
                AlertDialog alert = builder.create();
                alert.show();
            }
        });
    }
    
    private void autoSetWifi() {
        WifiManager wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        String ip = Formatter.formatIpAddress(wifi.getDhcpInfo().ipAddress);
        
        Process process;
        String targetStr = "";
        try {
            process = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            os.writeBytes("netcfg\n");
            os.writeBytes("exit\n");
            os.flush();
            BufferedReader mReader = new BufferedReader(new InputStreamReader(process.getInputStream()),1024);
            
            String line;
            while ((line = mReader.readLine()) != null) {
                if (line.length() == 0) {
                    continue;
                }
                if (line.contains(ip)) {
                    targetStr = line;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        targetStr = targetStr.split(" ")[0];
        targetStr = targetStr.split("\t")[0];
        Logging.updateDebug(targetStr);
        EditText et = (EditText) findViewById(R.id.wifi_edittext);
        et.setText(targetStr);
        
    }
}
