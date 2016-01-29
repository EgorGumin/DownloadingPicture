package com.guminegor.example.downloadingpicture;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

public class StackOverflowActivity extends Activity {

    //
    private ImageView mImageView;
    private ProgressBar imageProgressBar;
    private int wasStoppedOnFile = -1;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        imageProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        //imageProgressBar.show

        //Find the reference to the ImageView
        mImageView = (ImageView) findViewById(R.id.test_image);

        // You can set a temporary background here
        //image.setImageResource(null);

        // Start the DownloadImage task with the given url
        //http://i.imgur.com/hK8u0GU.jpg
        //http://i.imgur.com/0pAdPiM.jpg

        //new DownloadImage().execute("http://guminegor.github.io/DownloadingPicture/images/image0.bmp");

        new DownloadImageTaskSafe().execute("http://guminegor.github.io/DownloadingPicture/images/image0.bmp");

        int a = 0;
        int b = 1;
    }


    @Override
    protected void onStart(){
        super.onStart();
        new DownloadImageTaskSafe().execute("http://guminegor.github.io/DownloadingPicture/images/image0.bmp");
    }


    /**
     * Simple functin to set a Drawable to the image View
     * @param drawable
     */
    private void setImage(Drawable drawable)
    {
        mImageView.setBackgroundDrawable(drawable);
    }

    public class DownloadImage extends AsyncTask<String, Integer, Drawable> {

        @Override
        protected Drawable doInBackground(String... arg0) {
            // This is done in a background thread
            return downloadImage(arg0[0]);
        }

        protected void onProgressUpdate(String... progress) {
            imageProgressBar.setProgress(Integer.parseInt(progress[0]));
        }

        /**
         * Called after the image has been downloaded
         * -> this calls a function on the main thread again
         */
        protected void onPostExecute(Drawable image)
        {
            setImage(image);
        }


        /**
         * Actually download the Image from the _url
         * @param _url
         * @return
         */
        private Drawable downloadImage(String _url)
        {
            //Prepare to download image
            URL url;
            BufferedOutputStream out;
            InputStream in;
            BufferedInputStream buf;

            try {
                url = new URL(_url);
                in = url.openStream();



            /*
             * THIS IS NOT NEEDED
             *
             * YOU TRY TO CREATE AN ACTUAL IMAGE HERE, BY WRITING
             * TO A NEW FILE
             * YOU ONLY NEED TO READ THE INPUTSTREAM
             * AND CONVERT THAT TO A BITMAP
            out = new BufferedOutputStream(new FileOutputStream("testImage.jpg"));
            int i;

             while ((i = in.read()) != -1) {
                 out.write(i);
             }
             out.close();
             in.close();
             */

                // Read the inputstream
                buf = new BufferedInputStream(in);



                // Convert the BufferedInputStream to a Bitmap
                Bitmap bMap = BitmapFactory.decodeStream(buf);
                if (in != null) {
                    in.close();
                }
                if (buf != null) {
                    buf.close();
                }

                return new BitmapDrawable(bMap);

            } catch (Exception e) {
                Log.e("Error reading file", e.toString());
            }

            return null;
        }

    }

    private class DownloadImageTask extends AsyncTask<String, String, Bitmap> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            imageProgressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected Bitmap doInBackground(String... params) {
            Bitmap bitmap = null;
            try {
                int increment;
                byte[] data;
                InputStream in = null;
                int response;
                URL url = new URL(params[0]);
                URLConnection conn = url.openConnection();
                if (!(conn instanceof HttpURLConnection))
                    throw new IOException("Not an HTTP connection");
                try {
                    HttpURLConnection httpConn = (HttpURLConnection) conn;

                    httpConn.setInstanceFollowRedirects(true);
                    httpConn.setRequestMethod("GET");
                    httpConn.connect();

                    response = httpConn.getResponseCode();
                    if (response == HttpURLConnection.HTTP_OK) {
                        in = httpConn.getInputStream();
                    }
                    int length = httpConn.getContentLength();

                    data = new byte[length];
                    increment = length / 100;
                    ByteArrayOutputStream outStream = new ByteArrayOutputStream();
                    int count = -1;
                    int progress = 0;

                    while ((count = in.read(data, 0, increment)) != -1) {
                        progress += count;
                        publishProgress("" + (int) ((progress * 100) / length));
                        outStream.write(data, 0, count);
                    }
                    bitmap = BitmapFactory.decodeByteArray(
                            outStream.toByteArray(), 0, data.length);
                    in.close();
                    outStream.close();

                } catch (Exception ex) {
                    Log.d("Networking", ex.getLocalizedMessage());
                    wasStoppedOnFile = 0;
                    throw new IOException("Error connecting");
                }

            } catch (Exception e) {
                Log.e("Error: ", e.getMessage());
            }
            return bitmap;
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
                URL url = new URL("http://guminegor.github.io/DownloadingPicture/images/image0.bmp");

                URLConnection connection = url.openConnection();
                File fileThatExists = new File(path);
                OutputStream output = wasStoppedOnFile == -1 ? new FileOutputStream(path) :new FileOutputStream(path, true);
                long downloadFrom = wasStoppedOnFile == -1 ? 0 : fileThatExists.length();
                connection.setRequestProperty("Range", "bytes=" + downloadFrom + "-");

                connection.connect();

                int lenghtOfFile = connection.getContentLength();

                InputStream input = new BufferedInputStream(connection.getInputStream());
                byte data[] = new byte[1024];

                long total = 0;
                int count = 0;
                while ((count = input.read(data)) != -1) {
                    total += count;
                    publishProgress("" + (total + downloadFrom)* 100 / lenghtOfFile);
                    output.write(data, 0 , count);
                }





                } catch (MalformedURLException e1) {
                e1.printStackTrace();
            } catch (FileNotFoundException e1) {
                e1.printStackTrace();
            } catch (IOException e1) {
                e1.printStackTrace();
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