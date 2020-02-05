/*
-작성자: 2017038023 반예린(백그라운드 서비스 관련 기능), 2015023025 배나영(UI, 화면 간 이동 기능)
-해당 소스파일 정보: 백그라운드 서비스 시작 및 화면간 이동에 대한 기능.
                    백그라운드 서비스를 시작시키고 각 메뉴 버튼을 클릭하면 그 메뉴 기능에 해당하는 화면으로 이동.
-구현 완료된 기능: 백그라운드 서비스 시작, 화면간 이동에 대한 기능
-테스트 환경: Nexus 5X(AVD), API 29
 */

package com.example.iotfirealarm;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
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

            connectivityManager.registerDefaultNetworkCallback(new ConnectivityManager.NetworkCallback(){
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


    /*
    메소드 이름 : networkMethod
    메소드 기능 : 인터넷 연결 검사
     */
    public void networkMethod(View v){

        // 인터넷 연결 검사
        CheckNetwork network = new CheckNetwork(getApplicationContext());
        network.registerNetworkCallback();

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
