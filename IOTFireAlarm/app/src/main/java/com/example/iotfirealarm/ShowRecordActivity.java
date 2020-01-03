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

public class ShowRecordActivity extends AppCompatActivity {

//    recordInfoDBHelper rHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_record);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


//        rHelper=new recordInfoDBHelper(this);


    }

}

// 화재 기록 DB 관리를 위한 도우미 클래스
/*
class recordInfoDBHelper extends SQLiteOpenHelper {

    public recordInfoDBHelper(@Nullable Context context) {
        super(context, "UserInfoDB",null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table FireRecordInfo(senNum integer, time text, temperature real, flame real);"); // datetime형이 없는데 어떻게 할 것인지 이야기해보기
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

}*/
