package com.guminegor.example.downloadingpicture;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

public class StackOverflowActivity extends Activity {
    private ImageView mImageView;
    private ProgressBar imageProgressBar;
    private int wasStoppedOnFile = -1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        imageProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mImageView = (ImageView) findViewById(R.id.test_image);
        new DownloadImageTaskSafe().execute("http://guminegor.github.io/DownloadingPicture/images/image3.bmp");
    }

    private class DownloadImageTaskSafe extends AsyncTask<String, String, Bitmap> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            imageProgressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected Bitmap doInBackground(String... params) {
            Bitmap bitmap = null;
            try {
                String path = Environment.getExternalStorageDirectory() + "/image.bmp";
                URL url = new URL("http://guminegor.github.io/DownloadingPicture/images/image3.bmp");

                URLConnection connection = url.openConnection();
                File fileThatExists = new File(path);
                OutputStream output = wasStoppedOnFile == -1 ? new FileOutputStream(path) :new FileOutputStream(path, true);
                long downloadFrom = wasStoppedOnFile == -1 ? 0 : fileThatExists.length();
                connection.setRequestProperty("Range", "bytes=" + downloadFrom + "-");

                connection.connect();

                int lengthOfFile = connection.getContentLength();

                if (lengthOfFile == downloadFrom){
                    return null;
                }

                InputStream input = new BufferedInputStream(connection.getInputStream());
                byte data[] = new byte[1024];

                long total = 0;
                int count = 0;
                while ((count = input.read(data)) != -1) {
                    total += count;
                    publishProgress("" + (total + downloadFrom)* 100 / (lengthOfFile + downloadFrom));
                    output.write(data, 0 , count);
                }
            } catch (java.net.SocketException ex) {
                Log.d("Networking", ex.getLocalizedMessage());
                wasStoppedOnFile = 0;
            } catch (java.net.UnknownHostException e){
                Log.d("Networking", e.getLocalizedMessage());
                wasStoppedOnFile = 0;
            }
            catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }



        protected void onProgressUpdate(String... progress) {
            imageProgressBar.setProgress(Integer.parseInt(progress[0]));
        }

        protected void onPostExecute(Bitmap bm) {

            imageProgressBar.setVisibility(View.GONE);

            if (bm != null) {
                mImageView.setImageBitmap(bm);
            }
        }
    }

}