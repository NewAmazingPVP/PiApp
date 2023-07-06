package com.newamazingpvp.piapp;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ToggleButton;

import androidx.appcompat.app.AppCompatActivity;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;

public class MainActivity extends AppCompatActivity {

    private static final String SERVER_IP_OFF = "192.168.1.167";
    private static final String SERVER_IP_ON = "192.168.1.129";
    private static final int SERVER_PORT_OFF = 10001;
    private static final int SERVER_PORT_ON = 10003;

    private ImageView frameImageView;
    private SocketClientTask socketClientTask;
    private ToggleButton objectDetectionToggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_main);

        frameImageView = findViewById(R.id.frameImageView);
        objectDetectionToggle = findViewById(R.id.objectDetectionToggle);

        // Start the socket client task
        socketClientTask = new SocketClientTask();
        socketClientTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        // Set the OnCheckedChangeListener for objectDetectionToggle
        objectDetectionToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    // Object Detection is turned On
                    // Perform actions for On state
                    if (socketClientTask != null) {
                        socketClientTask.cancel(true);
                    }
                    socketClientTask = new SocketClientTask(SERVER_IP_ON, SERVER_PORT_ON);
                    socketClientTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                } else {
                    // Object Detection is turned Off
                    // Perform actions for Off state
                    if (socketClientTask != null) {
                        socketClientTask.cancel(true);
                    }
                    socketClientTask = new SocketClientTask(SERVER_IP_OFF, SERVER_PORT_OFF);
                    socketClientTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up the socket client task
        if (socketClientTask != null) {
            socketClientTask.cancel(true);
        }
    }

    private class SocketClientTask extends AsyncTask<Void, Bitmap, Void> {

        private Socket socket;
        private DataInputStream dataInputStream;
        private String serverIp;
        private int serverPort;

        public SocketClientTask() {
            this.serverIp = SERVER_IP_OFF;
            this.serverPort = SERVER_PORT_OFF;
        }

        public SocketClientTask(String serverIp, int serverPort) {
            this.serverIp = serverIp;
            this.serverPort = serverPort;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                socket = new Socket(serverIp, serverPort);
                dataInputStream = new DataInputStream(socket.getInputStream());

                while (!isCancelled()) {
                    // Receive the frame size as a 4-byte integer
                    int frameSize = dataInputStream.readInt();

                    // Receive the frame data
                    byte[] frameData = new byte[frameSize];
                    dataInputStream.readFully(frameData);

                    // Decode the frame into a Bitmap
                    Bitmap frameBitmap = BitmapFactory.decodeByteArray(frameData, 0, frameData.length);

                    // Publish the progress for UI update
                    publishProgress(frameBitmap);
                }
            } catch (IOException e) {
                Log.e("SocketClientTask", "Error: " + e.getMessage());
            } finally {
                // Clean up the socket and input stream
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        Log.e("SocketClientTask", "Error closing socket: " + e.getMessage());
                    }
                }
                if (dataInputStream != null) {
                    try {
                        dataInputStream.close();
                    } catch (IOException e) {
                        Log.e("SocketClientTask", "Error closing data input stream: " + e.getMessage());
                    }
                }
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(Bitmap... bitmaps) {
            super.onProgressUpdate(bitmaps);

            // Display the received frame
            if (bitmaps.length > 0) {
                Bitmap frameBitmap = bitmaps[0];
                frameImageView.setImageBitmap(frameBitmap);
            }
        }
    }
}
