package com.guminegor.example.downloadingpicture;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class MainActivity extends AppCompatActivity {
    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 300;
    private final Handler mHideHandler = new Handler();
    private View mContentView;
    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar

            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };
    private View mControlsView;
    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            // Delayed display of UI elements
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.show();
            }
            mControlsView.setVisibility(View.VISIBLE);
        }
    };
    private boolean mVisible;
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };
    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    private final View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            return false;
        }
    };


    private ImageView mImageView;
    private ProgressBar imageProgressBar;
    private int wasStoppedOnFile = -1;
    private TextView fullscreenText;
    private Button main_button;
    private enum MainButton { DELETE, DOWNLOAD };
    private MainButton mainButton;
    private String cachePath;
    private String downloadPath;
    private SharedPreferences prefs;
    SharedPreferences.Editor ed;
    private final int DEFAULT = 0;
    private final int NOT_LOADED = 1;
    private final int IN_CACHE = 2;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageProgressBar = (ProgressBar) findViewById(R.id.progressBar2);
        imageProgressBar.setVisibility(View.INVISIBLE);
        fullscreenText = (TextView) findViewById(R.id.fullscreen_content);
        mImageView = (ImageView) findViewById(R.id.testImage2);
        main_button = (Button) findViewById(R.id.main_button);

        mVisible = true;
        mControlsView = findViewById(R.id.fullscreen_content_controls);
        mContentView = findViewById(R.id.fullscreen_content);

        String [] images =  {"image0.bmp", "image1.jpg", "image2.png", "image3.bmp"};
        cachePath = Environment.getExternalStorageDirectory() + "/" + images[0];
        downloadPath = "http://guminegor.github.io/DownloadingPicture/images/" + images[0];

        prefs = getPreferences(MODE_PRIVATE);
        ed = prefs.edit();

        main_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mainButton == MainButton.DOWNLOAD) {
                    new DownloadImageTaskSafe().execute(downloadPath);
                    return;
                }

                if (mainButton == MainButton.DELETE) {
                    File cachedImage = new File(cachePath);
                    boolean deleted = cachedImage.delete();
                    if (deleted) {
                        mImageView.setVisibility(View.INVISIBLE);
                        fullscreenText.setText("Image was not downloaded yet");
                        mainButton = MainButton.DOWNLOAD;
                        main_button.setText("Download");
                        ed.putInt("downloadStatus", DEFAULT);
                        ed.commit();
                    } else {
                        Toast.makeText(MainActivity.this, "Error during deliting file", Toast.LENGTH_LONG).show();
                    }

                }


            }
        });
        int downloadStatus = prefs.getInt("downloadStatus", DEFAULT);

        if(downloadStatus == IN_CACHE){
            ifImageInCache();
        }
        else{
            new DownloadImageTaskSafe().execute(downloadPath);
        }




        // Set up the user interaction to manually show or hide the system UI.
        mContentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggle();
            }
        });

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        findViewById(R.id.main_button).setOnTouchListener(mDelayHideTouchListener);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }

    private void toggle() {
        if (mVisible) {
            hide();
        } else {
            show();
        }
    }

    private void hide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mControlsView.setVisibility(View.GONE);
        mVisible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    @SuppressLint("InlinedApi")
    private void show() {
        // Show the system bar
        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        mVisible = true;

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable);
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
    }

    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }

    private class DownloadImageTaskSafe extends AsyncTask<String, String, Boolean> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mControlsView.setBackgroundColor(getResources().getColor(R.color.transparent_overlay));
            main_button.setVisibility(View.GONE);
            imageProgressBar.setProgress(0);
            imageProgressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected Boolean doInBackground(String... params) {
            try {
                String path = cachePath;
                URL url = new URL(downloadPath);

                URLConnection connection = url.openConnection();
                File fileThatExists = new File(path);
                int downloadStatus = prefs.getInt("downloadStatus", DEFAULT);
                OutputStream output = downloadStatus == NOT_LOADED ?
                        new FileOutputStream(path, true) : new FileOutputStream(path);
                ed.putInt("downloadStatus", NOT_LOADED);
                ed.commit();
                long downloadFrom = fileThatExists.length();
                connection.setRequestProperty("Range", "bytes=" + downloadFrom + "-");

                connection.connect();

                int lengthOfFile = connection.getContentLength();

                int k = 0;

                InputStream input = new BufferedInputStream(connection.getInputStream());
                byte data[] = new byte[1024];

                long total = 0;
                int count = 0;
                while ((count = input.read(data)) != -1) {
                    total += count;
                    publishProgress("" + (total + downloadFrom)* 100 / (lengthOfFile + downloadFrom));
                    output.write(data, 0 , count);
                }
            } catch (Exception ex) {
                Log.d("Networking", ex.getLocalizedMessage());
                return false;
            }
            return true;
        }



        protected void onProgressUpdate(String... progress) {
            imageProgressBar.setProgress(Integer.parseInt(progress[0]));
        }

        protected void onPostExecute(Boolean downloaded) {
            if(downloaded){
                imageProgressBar.setVisibility(View.GONE);
                ed.putInt("downloadStatus", IN_CACHE);
                ed.commit();
                ifImageInCache();
            }
            else{
                ifDownloadError();
            }
        }
    }

    private void ifDownloadError(){
        Toast.makeText(MainActivity.this, "Network Error", Toast.LENGTH_SHORT).show();
        mControlsView.setBackgroundColor(getResources().getColor(R.color.black_overlay));
        main_button.setText("Resume");
        mainButton = MainButton.DOWNLOAD;
        imageProgressBar.setVisibility(View.INVISIBLE);

        main_button.setVisibility(View.VISIBLE);
        fullscreenText.setText("Loading is paused. Tap Resume to continue");
    }

    private void ifImageInCache(){
        mControlsView.setBackgroundColor(getResources().getColor(R.color.black_overlay));
        main_button.setText("Delete image");
        mainButton = MainButton.DELETE;

        main_button.setVisibility(View.VISIBLE);

        Bitmap bmp = BitmapFactory.decodeFile(cachePath);
        mImageView.setImageBitmap(bmp);
        fullscreenText.setText("");
        mImageView.setVisibility(View.VISIBLE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add("Settings");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case 1:
                // write your code here
                Toast msg = Toast.makeText(MainActivity.this, "Menu 1", Toast.LENGTH_LONG);
                msg.show();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
