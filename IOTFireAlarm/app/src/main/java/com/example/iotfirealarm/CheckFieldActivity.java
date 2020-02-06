package com.example.iotfirealarm;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

public class CheckFieldActivity extends AppCompatActivity {

    String imageURL=null;
    ImageView imageView=null;
    Bitmap bitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_check_field);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // 비트맵 받아와서 화면에 보여주는 코드
        // imageURL 링크에 있는 비트맵을 받아와서 보여준다
        imageURL="https://i.ytimg.com/vi/WCXM4DnwT1g/maxresdefault.jpg";
        imageView=(ImageView)findViewById(R.id.fieldImage);

        LoadImage loadImage = new LoadImage(imageURL);
        bitmap = loadImage.getBitmap(); // 비트맵
        imageView.setImageBitmap(bitmap);


    }

    public void onStop(){
        super.onStop();
        // getActivity().finish();
    }

    public void onClickReport(View v){
        Intent intent=new Intent(this, ReportActivity.class);
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.checkfield_toolbar, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){

        if(item.getItemId()==R.id.homeButton){
            Intent intent=new Intent(this, MainActivity.class);
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }



    // 비트맵을 받아오는 클래스
    public class LoadImage {

        private String imgPath;
        private Bitmap bitmap;

        public LoadImage(String imgPath){
            this.imgPath = imgPath;
        }

        public Bitmap getBitmap(){
            Thread imgThread = new Thread(){
                @Override
                public void run(){
                    try {
                        URL url = new URL(imgPath);
                        HttpURLConnection conn = (HttpURLConnection)url.openConnection();
                        conn.setDoInput(true);
                        conn.connect();
                        InputStream is = conn.getInputStream();
                        bitmap = BitmapFactory.decodeStream(is);
                    }catch (IOException e){
                    }
                }
            };
            imgThread.start();
            try{
                imgThread.join();
            }catch (InterruptedException e){
                e.printStackTrace();
            }finally {
                return bitmap;
            }
        }

    }



}
