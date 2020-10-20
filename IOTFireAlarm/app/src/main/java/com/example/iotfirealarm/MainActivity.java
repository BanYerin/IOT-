/*
-작성자: 2017038023 반예린(DB, 백그라운드 서비스 관련 기능), 2015023025 배나영(UI, 화면 간 이동, 인터넷 연결 확인 기능)
-해당 소스파일 정보: 백그라운드 서비스 시작, 화면간 이동, 인터넷 연결 확인에 대한 기능.
                    백그라운드 서비스를 시작시키고 인터넷 연결을 확인함. 각 메뉴 버튼을 클릭하면 그 메뉴 기능에 해당하는 화면으로 이동.
                    사용자 정보가 필요한 기능의 경우에는 DB에 저장된 사용자 정보가 없으면 사용자 정보 설정을 요청하는 알림창을 띄우고
                    정보를 설정하기 전까지는 해당 기능을 사용하지 못하도록 막아 오동작을 예방.
-구현 완료된 기능: 백그라운드 서비스 시작, 화면간 이동, DB 연동에 대한 기능
-테스트 환경: Nexus 5X(AVD), API 29
 */

package com.example.iotfirealarm;

import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkRequest;
import android.net.wifi.WifiManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;


class Variables{
    public static int isNetworkConnected=0;
    public static boolean alert=true;
}

class CheckNetwork {

    Context context;
    WifiManager wifiManager;

    public CheckNetwork(Context context){
        this.context=context;
    }

    public void registerNetworkCallback(){

        try {
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkRequest.Builder builder = new NetworkRequest.Builder();

            connectivityManager.registerNetworkCallback(builder.build(), new ConnectivityManager.NetworkCallback(){
                @Override
                public void onAvailable(@NonNull Network network) {
                    Variables.isNetworkConnected=1;
                }

                @Override
                public void onLost(@NonNull Network network) {
                    Variables.isNetworkConnected=2;
                }
            });

        } catch(Exception e){
            e.printStackTrace();
            Variables.isNetworkConnected=3;
        }
    }

}

public class MainActivity extends AppCompatActivity {
    UserInfoDBHelper mHelper;
    String userAddrString;  // 사용자 주소 정보를 저장하는 변수
    String adminPhoneNumString; // 관리자 전화번호 정보를 저장하는 변수
    String ipString; // 현장사진을 가져오기 위한 아두이노 서버 IP주소 정보를 저장하는 변수

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        //네트워크 연결 테스트
        CheckNetwork network = new CheckNetwork(getApplicationContext());
        network.registerNetworkCallback();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.activity_main, null);

        networkMethod(view);

        //백그라운드 서비스를 시작시킴
        Intent intent;
        intent = new Intent(MainActivity.this, backgroundService.class);
        startService(intent);

        mHelper=new UserInfoDBHelper(this);
        getuserInfo(); //DB에서 사용자 정보를 가져와 각 변수에 넣음


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

    //사용자 정보 가져오는 메소드: DB에서 사용자 정보를 가져와 각 변수에 넣음
    void getuserInfo(){

        //DB관련 변수 선언
        SQLiteDatabase db;
        ContentValues row;
        Cursor cursor;

        db=mHelper.getReadableDatabase();
        cursor=db.rawQuery("select addr, tel, ip from UserInfo", null);

        //결과가 존재하는 동안 레코드를 읽어와 각 필드에 대입
        while(cursor.moveToNext()){
            userAddrString=cursor.getString(0);
            adminPhoneNumString=cursor.getString(1);
            ipString=cursor.getString(2);
        }
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
        if (id == R.id.homeButton) {
            Intent intent=new Intent(this, MainActivity.class);
        }

        return super.onOptionsItemSelected(item);
    }

    /*
    메소드 이름 : onClickCheckField
    메소드 기능 : 메인 페이지에서 현장을 확인하는 페이지로 이동한다.
     */
    public void onClickCheckField(View v) {
        getuserInfo(); //DB에서 사용자 정보를 가져와 각 변수에 넣음

        if(ipString.length()==0 || ipString==null){ //아두이노 서버 IP주소가 설정되어 있지 않은 경우 알림창 띄움
            new AlertDialog.Builder(this)
                    .setTitle("알림")
                    .setMessage("아두이노 서버 IP주소가 설정 되어있지 않아서 사용할 수 없습니다!\n정보 설정 후 다시 시도하세요!")
                    .setIcon(R.drawable.ic_launcher_foreground)
                    .setPositiveButton("닫기", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {}
                    }).show();
        }else{ //아두이노 서버 IP주소가 설정되어 있는 경우 현장확인 화면으로 이동
            Intent intent = new Intent(this, CheckFieldActivity.class);
            startActivity(intent);

        }

    }


    //메인 페이지에서 화재 기록 조회 페이지로 이동한다
    public void onClickShowRecord(View V) {
        Intent intent = new Intent(this, ShowRecordActivity.class);
        startActivity(intent);
    }

    /*
    메소드 이름 : onClickSetting
    메소드 기능 : 메인 페이지에서 사용자 정보설정 페이지로 이동한다
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
        getuserInfo(); //DB에서 사용자 정보를 가져와 각 변수에 넣음

        if(adminPhoneNumString.length()==0 || userAddrString.length()==0 || adminPhoneNumString==null || userAddrString==null){//관리자 연락처가 없거나 사용자 주소가 설정되어 있지 않은 경우 알림창 띄움
            new AlertDialog.Builder(this)
                    .setTitle("알림")
                    .setMessage("관리자 연락처 또는 사용자 주소가 설정 되어있지 않아서 사용할 수 없습니다!\n정보 설정 후 다시 시도하세요!")
                    .setIcon(R.drawable.ic_launcher_foreground)
                    .setPositiveButton("닫기", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {}
                    }).show();
        }else{//관리자 연락처가 없거나 사용자 주소가 설정되어 있는 경우 신고하기 화면으로 이동
            Intent intent = new Intent(this, ReportActivity.class);
            startActivity(intent);
        }

    }

    /*
    메소드 이름 : onClickRecord
    메소드 기능 : 어플을 종료한다
     */
    public void onClickClosed(View v) {
        ActivityCompat.finishAffinity(this);
    }


    /*
    메소드 이름 : networkMethod
    메소드 기능 : 인터넷 연결 검사
     */
    public void networkMethod(View v){

        Log.d("network state", Integer.toString(Variables.isNetworkConnected)); //변수에 들어간 값을 로그캣에 출력

        switch(Variables.isNetworkConnected){
            case 0:
                new AlertDialog.Builder(this)
                        .setTitle("알림")
                        .setMessage("인터넷이 연결되어 있지 않습니다.")
                        .setIcon(R.drawable.ic_launcher_foreground)
                        .setPositiveButton("닫기", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {}
                        }).show();
                break;

            case 1:
                Toast.makeText(getApplicationContext(),"연결되었습니다.", Toast.LENGTH_SHORT).show();
                break;
            case 2:
                Toast.makeText(getApplicationContext(),"인터넷 연결이 좋지 않습니다.", Toast.LENGTH_SHORT).show();
                break;
            case 3:
                Toast.makeText(getApplicationContext(),"try-catch 에러발생", Toast.LENGTH_SHORT).show();
                break;
        }
    }


}
