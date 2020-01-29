/*
-작성자: 2017038023 반예린(UI, DB), 2015023025 배나영(UI)
-해당 소스파일 정보: 화재감지 기록 조회에 대한 기능.
                    DB에 존재하는 화재감지 기록을 가져와 화면에 출력함.
-구현 완료된 기능: DB에 존재하는 화재감지 기록 출력, DB 연동에 대한 기능.
-테스트 환경: SAMSUNG Galaxy S7(AVD), API 22
 */

package com.example.iotfirealarm;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;


public class ShowRecordActivity extends AppCompatActivity {
    float temper, gas; //온도, 가스 값
    String detectTime; //화재가 감지된 시각
    DataSensingDBHelper mHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_record);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mHelper=new DataSensingDBHelper(this);
        printFireRecord(); //DB에 저장되어 있는 화재감지 기록을 화면에 출력

    }

    //DB관리를 위한 도우미 클래스
    class DataSensingDBHelper extends SQLiteOpenHelper{
        public DataSensingDBHelper(Context context){
            super(context, "DataSensing.db", null, 1);
        }

        public void onCreate(SQLiteDatabase db){
            db.execSQL("create table DataSensing(senNum integer primary key autoincrement, time text, temper real, gas real);");

            //출력 형식을 확인하기 위해 테스트용으로 삽입한 레코드
            db.execSQL("insert into DataSensing values(null, '2019-12-28 19:42:05', 101.34, 80.21);");
            db.execSQL("insert into DataSensing values(null, '2019-12-29 21:54:25', 107.12, 78.24);");
            db.execSQL("insert into DataSensing values(null, '2019-12-29 05:21:57', 104.23, 92.32);");
            db.execSQL("insert into DataSensing values(null, '2019-12-30 15:36:12', 102.35, 88.46);");
            db.execSQL("insert into DataSensing values(null, '2019-12-31 19:42:05', 114.52, 82.34);");
            db.execSQL("insert into DataSensing values(null, '2020-01-02 21:54:25', 116.14, 71.21);");
            db.execSQL("insert into DataSensing values(null, '2020-01-02 05:21:57', 117.26, 95.64);");
            db.execSQL("insert into DataSensing values(null, '2020-01-03 15:36:12', 116.36, 82.78);");
            db.execSQL("insert into DataSensing values(null, '2020-01-04 19:42:05', 121.76, 87.36);");
            db.execSQL("insert into DataSensing values(null, '2020-01-05 21:54:25', 124.34, 71.74);");
            db.execSQL("insert into DataSensing values(null, '2020-01-05 05:21:57', 123.57, 99.35);");
            db.execSQL("insert into DataSensing values(null, '2020-01-05 15:36:12', 126.65, 81.29);");
        }

        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion){
            db.execSQL("drop table if exists DataSensing");
            onCreate(db);
        }
    }

    //화재감지 기록 출력 메소드: DB에 존재하는 화재감지 기록 데이터를 가져와 화면에 출력
    public void printFireRecord(){
        //화재감지 기록 텍스트뷰의 부모 레이아웃
        LinearLayout fireRecordLayout = (LinearLayout) findViewById(R.id.fireRecordLayout);

        //DB관련 변수 선언
        SQLiteDatabase db;
        ContentValues row;
        Cursor cursor;

        db=mHelper.getReadableDatabase();
        cursor=db.rawQuery("select time, temper, gas from DataSensing", null);

        //결과가 존재하는 동안 레코드를 읽어와 각 필드에 대입하고 각 레코드에 대한 화재감지 기록 출력
        while(cursor.moveToNext()){
            //결과가 존재하는 동안 레코드를 읽어와 각 필드에 대입
            detectTime=cursor.getString(0);
            temper=cursor.getFloat(1);
            gas=cursor.getFloat(2);


            //현재 화재감지 기록 DB에 존재하는 화재감지 시각, 온도값, 가스값을 스크롤 텍스트뷰에 출력
            //화재감지 기록 텍스트뷰를 동적 생성
            TextView DinamicView=new TextView(this);
            DinamicView.setText(detectTime+"             "+temper+"        "+gas+"\n");

            //동적생성된 화재감지 기록 텍스트뷰를 부모 레이아웃에 추가
            fireRecordLayout.addView(DinamicView);


        }

        mHelper.close();
    }

}


