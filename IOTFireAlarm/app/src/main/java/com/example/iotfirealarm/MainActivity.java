/*
-작성자: 2017038023 반예린
-해당 소스파일 정보: 내가 구현한 기능에 대해 화면 이동 작동 확인을 위한 테스트용 임시 메인화면 및 메인메뉴 기능.
                    나중에 통합할 때는 이 소스파일은 제거하고 배나영 팀원이 작성한 메인화면을 사용해야함.
-구현 완료된 기능: 홈 화면으로 이동, 신고하기 화면으로 이동, 어플 종료에 대한 기능.
-테스트 환경: SAMSUNG Galaxy S7(AVD), API 22
 */

package com.example.iotfirealarm;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    //홈 화면으로 이동 메소드
    public void onClickHome(View v) {
        goHome();
    }

    //신고하기 화면으로 이동하는 메소드
    public void onClickReport(View v) {
        Intent reportIntent = new Intent(this, ReportActivity.class);
        startActivity(reportIntent);
    }

    //어플 종료 메소드
    public void onClickExit(View v) {
        finish(); //현재 액티비티를 닫아 어플 종료
    }

    void goHome(){
        Intent homeIntent = new Intent(this, MainActivity.class);
        startActivity(homeIntent);
    }


}
