/*
-작성자: 2017038023 반예린
-해당 소스파일 정보: 화재 신고하기 화면 및 화재 신고, 위험 권한 부여 요청에 대한 기능.
                    신고 버튼을 클릭하면 DB에 저장된 신고 연락처와 사용자 주소 정보를 이용하여 화재 신고 메시지를 자동으로 전송.
-구현 완료된 기능: 관리자에게 신고, 소방서로 신고, 메시지 전송, 홈 화면으로 이동, DB 연동, 위험 권한 부여 요청에 대한 기능.
-테스트 환경: Nexus 5X(AVD), API 29
 */

package com.example.iotfirealarm;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.View;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;

import com.pedro.library.AutoPermissions;
import com.pedro.library.AutoPermissionsListener;

import java.util.ArrayList;

public class ReportActivity extends AppCompatActivity implements AutoPermissionsListener {
    UserInfoDBHelper mHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);

        mHelper=new UserInfoDBHelper(this);

        //위험 권한 자동부여 요청
        AutoPermissions.Companion.loadAllPermissions(this,101);
    }

    @Override
    public void onDenied(int i, String[] strings) {
        Toast.makeText(this,"permissions denied : "+strings.length,Toast.LENGTH_LONG).show();
    }

    @Override
    public void onGranted(int i, String[] strings) {
        Toast.makeText(this,"permissions granted : "+strings.length,Toast.LENGTH_LONG).show();
    }

    //위험 권한 부여에 대한 응답 처리
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        AutoPermissions.Companion.parsePermissions(this,requestCode,permissions,this);

    }





    //DB관리를 위한 도우미 클래스
    class UserInfoDBHelper extends SQLiteOpenHelper{
        public UserInfoDBHelper(Context context){
            super(context, "UserInfo.db", null, 1);
        }

        public void onCreate(SQLiteDatabase db){
            db.execSQL("create table UserInfo(infoNum integer primary key autoincrement, addr text, tel text);");
            //db.execSQL("insert into UserInfo values(null, '충북 청주시 서원구 충대로1 충북대학교', '010-1111-1111');"); //테스트용으로 삽입한 레코드
        }

        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion){
            db.execSQL("drop table if exists UserInfo");
            onCreate(db);
        }
    }

    //관리자 신고 메소드: DB에 존재하는 사용자 정보 데이터를 이용하여 관리자에게 신고 메시지 전송
    public void onClickReportM(View v){
        String tel=""; //메시지를 전송할 임시 관리자 번호. 임시 값으로 초기화
        String addr=""; //사용자 주소. 임시 값으로 초기화.
        String msg="화재 발생!! 도와주세요!\n 화재 발생지:"+addr; //화재 신고 메시지 내용

        //DB관련 변수 선언
        SQLiteDatabase db;
        ContentValues row;
        Cursor cursor;

        db=mHelper.getReadableDatabase();
        cursor=db.rawQuery("select addr, tel from UserInfo", null);

        //결과가 존재하는 동안 레코드를 읽어와 각 필드에 대입
        while(cursor.moveToNext()){
            addr=cursor.getString(0);
            tel=cursor.getString(1);
            msg="화재 발생!! 도와주세요!\n 화재 발생지:"+addr; //화재 신고 메시지 내용
        }

        mHelper.close();

        if(tel.length()==0 || addr.length()==0){//관리자 연락처가 없거나 사용자 주소가 설정되어 있지 않은 경우 알림창 띄움
            new AlertDialog.Builder(this)
                    .setTitle("알림")
                    .setMessage("관리자 연락처 또는 사용자 주소가 설정 되어있지 않음!\n정보 설정 후 다시 시도하세요!")
                    .setIcon(R.drawable.ic_launcher_foreground)
                    .setPositiveButton("닫기", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {}
                    }).show();
        }else{//관리자 연락처가 없거나 사용자 주소가 설정되어 있는 경우 해당 정보로 신고 메시지 전송
            sendSMS(tel, msg); //화재 신고 메시지 전송
        }

    }

    //소방서 신고 메소드: DB에 존재하는 사용자 정보 데이터를 이용하여 소방서에 신고 메시지 전송
    public void onClickReportFS(View v) {
        String tel="119"; //메시지를 전송 소방서 번호
        String addr=""; //임시 사용자 주소. 나중에 DB에서 받아와야함
        String msg="화재 발생!! 도와주세요!\n 화재 발생지:"+addr; //화재 신고 메시지 내용

        //DB관련 변수 선언
        SQLiteDatabase db;
        ContentValues row;
        Cursor cursor;

        db=mHelper.getReadableDatabase();
        cursor=db.rawQuery("select addr, tel from UserInfo", null);

        //결과가 존재하는 동안 레코드를 읽어와 각 필드에 대입
        while(cursor.moveToNext()){
            addr=cursor.getString(0);
            msg="화재 발생!! 도와주세요!\n 화재 발생지:"+addr; //화재 신고 메시지 내용
        }

        mHelper.close();

        if(tel.length()==0 || addr.length()==0){//사용자 주소가 설정되어 있지 않은 경우 알림창 띄움
            new AlertDialog.Builder(this)
                    .setTitle("알림")
                    .setMessage("사용자 주소가 설정 되어있지 않음!\n정보 설정 후 다시 시도하세요!")
                    .setIcon(R.drawable.ic_launcher_foreground)
                    .setPositiveButton("닫기", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {}
                    }).show();
        }else{//사용자 주소가 설정되어 있는 경우 해당 정보로 신고 메시지 전송
            sendSMS(tel, msg); //화재 신고 메시지 전송

        }

    }

    //메시지 전송 메소드
    void sendSMS(String tel, String msg){
        try{
            Uri smsUri = Uri.parse("sms:"+tel);
            Intent sendIntent = new Intent(Intent.ACTION_SENDTO, smsUri);
            //sendIntent.putExtra("sms_body", msg); //메시지 입력란에 메시지 내용만 자동 입력되고 전송은 사용자가 직접 누르도록 하는 경우 사용
            SmsManager sms = SmsManager.getDefault();
            sms.sendTextMessage(tel,null,msg,null,null); //사용자가 전송버튼을 누르지 않아도 메시지 내용을 자동 전송하도록 하는 경우 사용
            startActivity(sendIntent);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    //홈 화면으로 이동 메소드
    public void onClickHome(View v) {
        goHome(); //홈 화면 액티비티로 이동
    }

    void goHome(){
        Intent homeIntent = new Intent(this, MainActivity.class);
        startActivity(homeIntent);
    }

}
