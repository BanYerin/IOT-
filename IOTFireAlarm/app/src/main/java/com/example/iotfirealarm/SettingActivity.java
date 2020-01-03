package com.example.iotfirealarm;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.view.View;
import android.widget.EditText;
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
    }

    public void registerInfo(View v){

        mHelper=new UserInfoDBHelper(this);

        // 사용자 주소 정보와 관리자 전화번호를 EditText형으로 받아옴
        EditText userAddrEditText=findViewById(R.id.userAddress);
        EditText adminNumEditText=findViewById(R.id.adminPhoneNumber);

        // 사용자 주소 정보, 관리자 전화번호 String으로 형변환하기
        userAddrString=userAddrEditText.getText().toString();
        adminPhoneNumString=adminNumEditText.getText().toString();

        // 받아온 사용자 주소와 관리자 전화번호 정보를 화면에 띄워줌
        Toast.makeText(getApplicationContext(),"사용자주소 : "+userAddrString+"/ 관리자 전화번호 : "+adminPhoneNumString,Toast.LENGTH_LONG).show();

    }

    // DB 관리를 위한 도우미 클래스
    class UserInfoDBHelper extends SQLiteOpenHelper{

        public UserInfoDBHelper(@Nullable Context context) {
            super(context, "UserInfoDB",null, 1);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("create table UserInfo(infoNum integer primary key autoincrement, addr text, tel text);");
        }

        // 사용자 주소와 관리인 전화번호 정보를 어떻게 넘겨줄 것인지 - 매개변수 수정하니 오류 뜸
        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            // db.execSQL("drop table if exists UserInfo");
            // 주소와 관리자 정보 업데이트하기
            // 있으면 update로 데이터 값을 바꿔주고 없으면 insert로 삽입

            db.execSQL("insert into UserInfo values ("+userAddrString+","+adminPhoneNumString+" )"); // 사용자 주소와 관리인 전화번호 정보 삽입
        }
    }

}
