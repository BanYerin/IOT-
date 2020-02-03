/*
-작성자: 2017038023 반예린(백그라운드 서비스 관련 기능), 2015023025 배나영(UI, 화면 간 이동 기능)
-해당 소스파일 정보: 백그라운드 서비스 시작 및 화면간 이동에 대한 기능.
                    백그라운드 서비스를 시작시키고 각 메뉴 버튼을 클릭하면 그 메뉴 기능에 해당하는 화면으로 이동.
-구현 완료된 기능: 백그라운드 서비스 시작, 화면간 이동에 대한 기능
-테스트 환경: SAMSUNG Galaxy S7(AVD), API 22
 */

package com.example.iotfirealarm;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //백그라운드 서비스를 시작시킴
        Intent intent;
        intent = new Intent(MainActivity.this, backgroundService.class);
        startService(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent=new Intent(this, MainActivity.class);
            startActivity(intent);
        }

        return super.onOptionsItemSelected(item);
    }

    /*
    메소드 이름 : onClickCheckField
    메소드 기능 : 메인 페이지에서 현장을 확인하는 페이지로 이동한다.
     */
    public void onClickCheckField(View v) {
        Intent intent = new Intent(this, CheckFieldActivity.class);
        startActivity(intent);
    }


    //메인 페이지에서 화재 기록 조회 페이지로 이동한다
    public void onClickShowRecord(View V) {
        Intent intent = new Intent(this, ShowRecordActivity.class);
        startActivity(intent);
    }

    /*
    메소드 이름 : onClickSetting
    메소드 기능 : 메인 페이지에서 환경설정 페이지로 이동한다
     */
    public void onClickSetting(View v) {

        // 화면 이동
        Intent intent = new Intent(this, SettingActivity.class);
        startActivity(intent);

    }

    /*
    메소드 이름 : onClickRecord
    메소드 기능 : 메인 화면에서 신고 페이지로 이동한다
     */
    public void onClickReport(View v) {
        Intent intent = new Intent(this, ReportActivity.class);
        startActivity(intent);
    }

    /*
    메소드 이름 : onClickRecord
    메소드 기능 : 어플을 종료한다
     */
    public void onClickClosed(View v) {
        ActivityCompat.finishAffinity(this);
    }



}
