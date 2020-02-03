/*
-작성자: 2017038023 반예린(DB), 2015023025 배나영(UI)
-해당 소스파일 정보: 사용자 정보 등록 및 수정에 대한 기능.
                    사용자 주소와 신고 연락처를 입력한 뒤 등록 버튼을 클릭하면 DB에 존재하는 기존 정보를 삭제하고 새로 입력한 정보를 삽입함.
-구현 완료된 기능: 사용자 정보 등록 및 수정, 등록되어 있는 기존 사용자 정보 출력, DB 연동에 대한 기능.
-테스트 환경: SAMSUNG Galaxy S7(AVD), API 22
 */

package com.example.iotfirealarm;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class SettingActivity extends AppCompatActivity {

    UserInfoDBHelper mHelper;     // UserInfoDBHelper : DB 관리를 위한 도우미 클래스
    String userAddrString;  // 사용자 주소 정보를 저장하는 변수
    String adminPhoneNumString; // 관리자 전화번호 정보를 저장하는 변수

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mHelper=new UserInfoDBHelper(this);
        printUserInfo(); //기존에 등록되어 있는 사용자 정보를 화면에 출력

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.setting_toolbar, menu);
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

    //DB관리를 위한 도우미 클래스
    class UserInfoDBHelper extends SQLiteOpenHelper{
        public UserInfoDBHelper(Context context){
            super(context, "UserInfo.db", null, 1);
        }

        public void onCreate(SQLiteDatabase db){
            db.execSQL("create table UserInfo(infoNum integer primary key autoincrement, addr text, tel text);");
        }

        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion){
            db.execSQL("drop table if exists UserInfo");
            onCreate(db);
        }
    }

    //사용자 정보 출력 메소드: DB에 존재하는 사용자 정보 데이터를 가져와 화면에 출력
    public void printUserInfo(){
        //사용자 주소 정보와 관리자 전화번호를 EditText형으로 받아옴
        EditText userAddrEditText=findViewById(R.id.userAddress);
        EditText adminNumEditText=findViewById(R.id.adminPhoneNumber);

        //사용자 주소 정보와 관리자 전화번호를 TextView형으로 받아옴
        TextView existUserInfo=(TextView)findViewById(R.id.existUserInfo);

        //DB관련 변수 선언
        SQLiteDatabase db;
        ContentValues row;
        Cursor cursor;

        db=mHelper.getReadableDatabase();
        cursor=db.rawQuery("select addr, tel from UserInfo", null);

        //결과가 존재하는 동안 레코드를 읽어와 각 필드에 대입
        while(cursor.moveToNext()){
            userAddrString=cursor.getString(0);
            adminPhoneNumString=cursor.getString(1);
        }

        //현재 사용자 정보 DB에 존재하는 사용자 주소와 관리자 연락처를 각 에디트텍스트와 텍스트뷰에 출력
        userAddrEditText.setText(userAddrString);
        adminNumEditText.setText(adminPhoneNumString);
        existUserInfo.setText("<현재 등록되어 있는 정보>"+"\n-사용자 주소: "+userAddrString+"\n-신고 연락처: "+adminPhoneNumString);

    }

    //사용자 정보 등록 및 수정 메소드
    public void OnclickRegister(View v){
        //DB관련 변수 선언
        SQLiteDatabase db;
        ContentValues row;
        db=mHelper.getWritableDatabase();


        // 사용자 주소 정보와 관리자 전화번호를 EditText형으로 받아옴
        EditText userAddrEditText=findViewById(R.id.userAddress);
        EditText adminNumEditText=findViewById(R.id.adminPhoneNumber);

        // 사용자 주소 정보, 관리자 전화번호 String으로 형변환하기
        userAddrString=userAddrEditText.getText().toString();
        adminPhoneNumString=adminNumEditText.getText().toString();


        //UserInfo 테이블에 존재하는 기존의 사용자 정보를 삭제하고 새로 입력받은 정보를 삽입함으로써 사용자 정보를 업데이트함
        //UserInfo 테이블에 존재하는 모든 레코드 삭제
        db.execSQL(("delete from UserInfo;"));
        //사용자로 부터 입력받은 사용자 주소와 관리자 연락처를 UserInfo 테이블에 삽입
        db.execSQL("insert into UserInfo values(null, '"+userAddrString+"', '"+adminPhoneNumString+"');");

        printUserInfo(); //변경된 사용자 정보를 화면에 출력

        // 받아온 사용자 주소와 관리자 전화번호 정보를 화면에 띄워줌
        Toast.makeText(getApplicationContext(),"사용자주소 : "+userAddrString+"\n관리자 연락처 : "+adminPhoneNumString+"\n등록 완료",Toast.LENGTH_LONG).show();

        mHelper.close();
    }

}
