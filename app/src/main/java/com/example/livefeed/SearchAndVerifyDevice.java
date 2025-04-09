package com.example.livefeed;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

public class SearchAndVerifyDevice extends AppCompatActivity {

    String remoteIp;
    int port;
    TextView connect;
    AppCompatButton btn;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_search_and_verify_device);

        TextView deviceName = findViewById(R.id.deviceName);
        TextView ipAdress = findViewById(R.id.IpAdress);
        TextInputEditText inputUrl = findViewById(R.id.inputUrl);
        btn = findViewById(R.id.btn);
        connect = findViewById(R.id.connect);

        inputUrl.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                btn.setText("Verify URL");
                connect.setText("");
                deviceName.setText("");
                ipAdress.setText("");
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(btn.getText().toString().equals("Verify URL")) {
                    String url = inputUrl.getText().toString().trim();
                    remoteIp = url;
                    for (int i = 0; i < url.length(); i++) {
                        if (url.charAt(i) == '/' && url.charAt(i + 1) == '/') {
                            for (int j = url.length() - 1; j >= i; j--) {
                                if (url.charAt(j) == ':') {
                                    try {
                                        port = Integer.parseInt(url.substring(j+1,j+5));
                                        remoteIp = url.substring(i + 2, j);
                                        break;
                                    }
                                    catch (Exception e){
                                        Toast.makeText(SearchAndVerifyDevice.this,"There is an Error in the url",Toast.LENGTH_SHORT).show();
                                        return;
                                    }
                                }
                            }
                            break;
                        }
                    }

                    Log.d("RemoteIp", remoteIp);

                    //Getting device details
                    new Thread(() -> {
                        try {
                            InetAddress inetAddress = InetAddress.getByName(remoteIp);
                            String hostname = inetAddress.getCanonicalHostName();
                            Log.d("cane", "yes");

                            runOnUiThread(() -> {
                                    deviceName.setText(hostname);
                                    ipAdress.setText(remoteIp);
                                    Log.d("HOSTNAME", "Resolved: " + hostname);
                                    checkRtspServer(remoteIp, port);
                            });
                        } catch (Exception e) {
                            e.printStackTrace();
                            runOnUiThread(() -> {
                                deviceName.setText("Device Unknown");
                                ipAdress.setText("Unknown Ip Adress");
                                Log.d("HOSTNAME", "Failed to resolve host");
                            });
                        }
                    }).start();
                }
                else{
                    Log.d("came", "but");

                    Intent i = new Intent(SearchAndVerifyDevice.this,VlcPlayerActivity.class);
                    i.putExtra("URL",inputUrl.getText().toString().trim());
                    startActivity(i);
                }
            }
        });


    }

    private void checkRtspServer(String ip, int port) {

        new Thread(() -> {
            try {
                Socket socket = new Socket();
                socket.connect(new InetSocketAddress(ip, port), 3000);

                socket.close();
                runOnUiThread(()->{

                connect.setText("● Server Available to Connect");
                btn.setText("Connect");

                connect.setTextColor(getResources().getColor(R.color.green));
                });
            } catch (IOException e) {

                runOnUiThread(() ->{
                    connect.setText("● Server Offline");
                    btn.setText("Verify URL");

                    connect.setTextColor(getResources().getColor(R.color.red));

                Toast.makeText(this, "RTSP server not reachable", Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

}