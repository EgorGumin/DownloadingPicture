package com.guminegor.example.downloadingpicture;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.Toast;

import java.io.File;

public class Settings extends AppCompatActivity implements View.OnClickListener{
    private View.OnClickListener radioListener;
    private int selectedImage;
    private SharedPreferences prefs;
    private SharedPreferences.Editor ed;
    private int currentImage;
    private int newImage;
    private String [] images = {"image0.bmp", "image1.jpg", "image2.png"};
    private String cachePath;
    private File cachedImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        RadioButton rb0 = (RadioButton)findViewById(R.id.radio0);
        rb0.setOnClickListener(this);

        RadioButton rb1 = (RadioButton)findViewById(R.id.radio1);
        rb1.setOnClickListener(this);

        RadioButton rb2 = (RadioButton)findViewById(R.id.radio2);
        rb2.setOnClickListener(this);

        Button save = (Button) findViewById(R.id.settings_btn_save);
        save.setOnClickListener(this);

        Button clear = (Button) findViewById(R.id.settings_btn_clear);
        clear.setOnClickListener(this);

        prefs = getSharedPreferences("settings", MODE_PRIVATE);
        ed = prefs.edit();

        currentImage = prefs.getInt("currentImage", 0);

        switch (currentImage){
            case 0:
                rb0.setChecked(true);
                break;
            case 1:
                rb1.setChecked(true);
                break;
            case 2:
                rb2.setChecked(true);
                break;
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.radio0:
                Toast.makeText(Settings.this, R.string.warning_big_image, Toast.LENGTH_LONG).show();
                newImage = 0;
                break;

            case R.id.radio1:
                newImage = 1;
                break;

            case R.id.radio2:
                newImage = 2;
                break;

            case R.id.settings_btn_save:
                cachePath = Environment.getExternalStorageDirectory() + "/" + images[currentImage];
                cachedImage = new File(cachePath);
                boolean deleted = cachedImage.delete();
                ed.putInt("currentImage", newImage);
                ed.putInt("downloadStatus", 0);
                ed.commit();

//                if (prefs.getInt("downloadStatus", 0) != 1){
//
//                }
//                else{
//                    Toast.makeText(Settings.this, R.string.warning_file_downloading, Toast.LENGTH_LONG).show();
//                }

                break;

            case R.id.settings_btn_clear:
                Toast.makeText(Settings.this, R.string.warning_file_downloading, Toast.LENGTH_LONG).show();
                cachePath = Environment.getExternalStorageDirectory() + "/" + images[currentImage];
                cachedImage = new File(cachePath);
                boolean del = cachedImage.delete();
                ed.putInt("downloadStatus", 0);
                ed.commit();

//                if (prefs.getInt("downloadStatus", 0) != 1){
//
//                }
//                else{
//                    Toast.makeText(Settings.this, R.string.warning_file_downloading, Toast.LENGTH_LONG).show();
//                }
               break;

            default:
                break;
        }
    }

}
