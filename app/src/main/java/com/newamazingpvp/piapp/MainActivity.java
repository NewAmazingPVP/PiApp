package com.newamazingpvp.piapp;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;

public class MainActivity extends AppCompatActivity {

    private static final String SERVER_HOST = "192.168.1.157";
    private static final int SERVER_PORT = 10001;

    private ImageView frameImageView;
    private SocketClientTask socketClientTask;

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

        // Start the socket client task
        socketClientTask = new SocketClientTask();
        socketClientTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
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

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                socket = new Socket(SERVER_HOST, SERVER_PORT);
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
