package com.guminegor.example.downloadingpicture;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

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
    private static final int AUTO_HIDE_DELAY_MILLIS = 5000;

    private static final int UI_ANIMATION_DELAY = 300;
    private final Handler mHideHandler = new Handler();
    private TextView fullscreenText;
    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar

            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            fullscreenText.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
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
    private Button main_button;
    private enum MainButton { DELETE, DOWNLOAD };
    private MainButton mainButton;
    private String cachePath;
    private SharedPreferences prefs;
    private SharedPreferences.Editor ed;
    private int currentImage;
    private final String [] images =  {"image0.bmp", "image1.jpg", "image2.png"};
    private final String serverUrl = "http://s3.eu-central-1.amazonaws.com/imagedownloader/";

    DownloadManager downloadManager;
    String downloadFileUrl;
    private long myDownloadReference;
    private BroadcastReceiver receiverDownloadComplete;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fullscreenText = (TextView) findViewById(R.id.fullscreen_content);
        mControlsView = findViewById(R.id.fullscreen_content_controls);
        mImageView = (ImageView) findViewById(R.id.loaded_image);
        main_button = (Button) findViewById(R.id.main_button);
        mVisible = true;
        // Set up the user interaction to manually show or hide the system UI.
        fullscreenText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggle();
            }
        });
        findViewById(R.id.main_button).setOnTouchListener(mDelayHideTouchListener);

        prefs = getSharedPreferences("settings", MODE_PRIVATE);
        ed = prefs.edit();
        currentImage = prefs.getInt("currentImage", 1);

        downloadFileUrl = serverUrl + images[currentImage];
        downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);

        main_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (v.getId() == R.id.main_button && mainButton == MainButton.DOWNLOAD) {
                    DownloadImage(downloadFileUrl);
                    return;
                }

                if (v.getId() == R.id.main_button && mainButton == MainButton.DELETE) {
                    File cachedImage = new File(cachePath);
                    boolean deleted = cachedImage.delete();
                    ed.putLong("dmReference", -1);
                    ed.commit();
                    ifNoImageDownloaded();
                }
            }
        });
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
        fullscreenText.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
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


    private void DownloadImage(String fileURL) {
        mControlsView.setBackgroundColor(getResources().getColor(R.color.transparent_overlay));
        ifLoading();

        ConnectivityManager conManager = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = conManager.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null
                && activeNetwork.isConnectedOrConnecting();

        if (!isConnected) {
            Toast.makeText(MainActivity.this, "No Connection. " +
                            "Picture will be download when it appears",
                    Toast.LENGTH_SHORT).show();
        }

        Uri uri = Uri.parse(fileURL);
        DownloadManager.Request request = new DownloadManager.Request(uri);

        request.setDescription("ImageDownloader")
                .setTitle(images[currentImage]);

        request.setDestinationInExternalFilesDir(MainActivity.this,
                Environment.DIRECTORY_DOWNLOADS, images[currentImage]);

        request.setVisibleInDownloadsUi(true);

        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI
                | DownloadManager.Request.NETWORK_MOBILE);

        myDownloadReference = downloadManager.enqueue(request);
        ed.putLong("dmReference", myDownloadReference);
        ed.commit();
    }

    @Override
    protected void onResume() {
        super.onResume();
        currentImage = prefs.getInt("currentImage", 1);
        downloadFileUrl = serverUrl + images[currentImage];
        long reference = prefs.getLong("dmReference", -1);

        if(reference == -1){
            ifNoImageDownloaded();
        }
        else{
            checkLastDownloadStatus(reference);
        }

        IntentFilter intentFilter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);

        receiverDownloadComplete = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                long reference = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                if (myDownloadReference == reference) {
                    checkLastDownloadStatus(reference);
                }
            }
        };
        registerReceiver(receiverDownloadComplete, intentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(receiverDownloadComplete);
    }


    private void checkLastDownloadStatus(long reference)
    {
        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterById(reference);
        Cursor cursor = downloadManager.query(query);

        cursor.moveToFirst();

        int columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
        int status = cursor.getInt(columnIndex);

        int fileNameIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_FILENAME);
        cachePath = cursor.getString(fileNameIndex);
        ed.putString("cachePath", cachePath);
        ed.commit();

        int columnReason = cursor.getColumnIndex(DownloadManager.COLUMN_REASON);
        int reason = cursor.getInt(columnReason);

        switch (status) {
            case DownloadManager.STATUS_SUCCESSFUL:
                ifImageInCache();
                break;

            case DownloadManager.STATUS_FAILED:
                Toast.makeText(MainActivity.this, "FAILED: " + reason,
                        Toast.LENGTH_LONG).show();
                ed.putLong("dmReference", -1);
                ed.commit();
                ifNoImageDownloaded();
                break;

            case DownloadManager.STATUS_PAUSED:
                ifDownloadError();
                break;

            case DownloadManager.STATUS_PENDING:

            case DownloadManager.STATUS_RUNNING:
                ifLoading();
                break;
        }
        cursor.close();
    }

    private void ifLoading(){
        main_button.setVisibility(View.GONE);
        fullscreenText.setText(getResources().getText(R.string.loading));
    }

    private void ifDownloadError(){
        Toast.makeText(MainActivity.this, getResources().getText(R.string.network_error), Toast.LENGTH_SHORT).show();
        mControlsView.setBackgroundColor(getResources().getColor(R.color.black_overlay));
        main_button.setText(getResources().getText(R.string.resume));
        fullscreenText.setText(getResources().getText(R.string.loading_paused));
    }

    private void ifImageInCache(){
        mControlsView.setBackgroundColor(getResources().getColor(R.color.black_overlay));
        main_button.setText(getResources().getText(R.string.btn_delete));
        mainButton = MainButton.DELETE;
        main_button.setVisibility(View.VISIBLE);

        Bitmap bmp = BitmapFactory.decodeFile(cachePath);
        mImageView.setImageBitmap(bmp);
        fullscreenText.setText("");
        mImageView.setVisibility(View.VISIBLE);
    }

    private void ifNoImageDownloaded(){
        mImageView.setVisibility(View.INVISIBLE);
        fullscreenText.setText(getResources().getText(R.string.no_image));
        mainButton = MainButton.DOWNLOAD;
        main_button.setText(getResources().getText(R.string.download));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(1, 1, 1, getResources().getText(R.string.title_activity_settings));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case 1:
                Intent intent = new Intent(MainActivity.this, Settings.class);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
