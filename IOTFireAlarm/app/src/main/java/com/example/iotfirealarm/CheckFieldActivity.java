/*
-작성자: 2017038023 반예린
-해당 소스파일 정보: 현장확인에 대한 기능.
                    DB에 저장된 아두이노 서버 IP주소 정보를 이용하여 아두이노 웹서버 페이지에 접속한 뒤
                    해당 페이지에 출력된 아두이노 카메라가 촬영한 현장사진을 웹뷰 형태로 가져와 화면에 출력함
-구현 완료된 기능: 아두이노 웹서버 페이지의 현장사진을 웹뷰로 가져와 출력, DB 연동에 대한 기능.
-테스트 환경: Nexus 5X(AVD), API 29
 */

package com.example.iotfirealarm;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.TextView;

public class CheckFieldActivity extends AppCompatActivity {

    String ipString; // 현장사진을 가져오기 위한 아두이노 서버 IP주소 정보를 저장하는 변수
    UserInfoDBHelper mHelper;
    private WebView mWebView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_check_field);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mHelper=new UserInfoDBHelper(this);
        getIp(); //DB에서 아두이노 서버 IP주소를 가져와 ipString변수에 넣음

        //현장사진 로딩 안내문구를 TextView형으로 받아옴
        TextView loadingMsg=findViewById(R.id.loadingMsg);

        //아두이노 카메라가 촬영한 현장사진을 WebView 형태로 받아옴
        mWebView = (WebView) findViewById(R.id.webview);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.loadUrl("http://" + ipString); // 접속 URL
        mWebView.setWebViewClient(new WebViewClient());

        //loadingMsg.setText("현장 촬영 이미지 로딩 완료!");


    }

    //DB관리를 위한 도우미 클래스
    class UserInfoDBHelper extends SQLiteOpenHelper {
        public UserInfoDBHelper(Context context){
            super(context, "UserInfo.db", null, 1);
        }

        public void onCreate(SQLiteDatabase db){
            db.execSQL("create table UserInfo(infoNum integer primary key autoincrement, addr text, tel text, ip text);");
        }

        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion){
            db.execSQL("drop table if exists UserInfo");
            onCreate(db);
        }
    }

    //IP주소 가져오는 메소드: DB에서 아두이노 서버 IP주소를 가져와 ipString변수에 넣음
    void getIp(){

        //DB관련 변수 선언
        SQLiteDatabase db;
        ContentValues row;
        Cursor cursor;

        db=mHelper.getReadableDatabase();
        cursor=db.rawQuery("select addr, tel, ip from UserInfo", null);

        //결과가 존재하는 동안 레코드를 읽어와 각 필드에 대입
        while(cursor.moveToNext()){
            ipString=cursor.getString(2);
        }
    }




    //신고하기 화면으로 이동하는 메소드
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



}
