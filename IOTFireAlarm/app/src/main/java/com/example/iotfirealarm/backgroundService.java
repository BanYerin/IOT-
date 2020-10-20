/*
-작성자: 2017038023 반예린(백그라운드 서비스, 클라우드 서버로부터 데이터 수신, JSON 파싱, 화재여부 판단, 화재감지 기록 저장 기능),
        2015023025 배나영(화재 푸시알림 기능)
-해당 소스파일 정보: 백그라운드 서비스, 클라우드 서버로 부터의 데이터 수신, JSON 파싱, 화재여부 판단, 화재감지 기록 저장, 화재 푸시알림에 대한 기능.
                    URL을 통해 클라우드 서버로부터 아두이노 센서 측정값 데이터를 JSON 형태로 가져와 파싱하여 필요한 데이터를 추출한 후
                    추출한 온도값 및 가스값을 이용하여 화재여부를 판단하고 화재여부가 true이면 화재감지 기록 저장 기능과 화재 푸시알림 기능이
                    작동하도록 백그라운드 서비스에서 계속 유지하여 실행함.
-구현 완료된 기능: 백그라운드 서비스, 클라우드 서버로 부터의 데이터 수신, JSON 파싱, 화재여부 판단, 화재감지 기록 저장, 화재 푸시알림에 대한 기능.
-테스트 환경: Nexus 5X(AVD), API 29
 */

package com.example.iotfirealarm;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;

import java.util.HashMap;
import java.util.Map;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;


public class backgroundService extends Service {
    boolean mQuit;
    static RequestQueue requestQueue;
    String json = ""; //json문자열
    float temp=0; //온도값
    float gas=0; //가스값
    int priorEntryId=0; //이전 데이터 엔트리id
    int posteriorEntryId=0; //최근 데이터 엔트리id
    String detectTime; //화재가 감지된 시각
    DataSensingDBHelper mHelper;

    public void onCreate() {
        super.onCreate();

        if (requestQueue == null) {
            requestQueue = Volley.newRequestQueue(getApplicationContext());
        }

        mHelper=new DataSensingDBHelper(this);
    }

    public void onDestroy(){
        super.onDestroy();

        Toast.makeText(this, "Service End", Toast.LENGTH_SHORT).show();
        mQuit = true;
    }

    public int onStartCommand(Intent intent, int flags, int startId){
        super.onStartCommand(intent, flags, startId);

        mQuit = false;
        ParsingThread thread  = new ParsingThread(this, mHandler);
        thread.start();
        return START_STICKY;
    }


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    class ParsingThread extends Thread{
        backgroundService mParent;
        Handler mHandler;

        public ParsingThread(backgroundService parent, Handler handler){
            mParent = parent;
            mHandler = handler;
        }

        public void run(){
            while(true){
                makeRequest(); //URL로부터 JSON데이터를 가져와 파싱

                //마지막 데이터 엔트리id와 이전 데이터 엔트리id가 다르면 온도값 및 가스값 데이터가 새로운 값으로 변경된 것이므로 화재판단을 수행하고,
                //수행 결과 화재여부가 true이면 화재알림 및 화재감지 기록 저장을 수행함
                if(priorEntryId != posteriorEntryId){
                    if(fireDecision() == true){ //화재 판단 결과가 ture이면
                        fireAlarm(); //화재 푸시알림 동작
                        recordFire(); //화재감지 기록 저장
                        //ShowRecordActivity showRecordActivity = new ShowRecordActivity();
                        //showRecordActivity.printFireRecord(); //DB에 존재하는 화재감지 기록 데이터를 가져와 화재기록 조회 화면의 출력물을 업데이트함
                    }
                }


                priorEntryId = posteriorEntryId; //이전 데이터 엔트리id 값을 최근 데이터 엔트리id 값과 같도록 업데이트


                Message msg = new Message();
                msg.what = 0;
                msg.obj = temp +" / "+ gas;
                mHandler.sendMessage(msg);
                try{Thread.sleep(10000);}catch(Exception e){;} //스레드를 5초에 한번씩 실행하도록 설정
            }
        }
    }

    Handler mHandler = new Handler(){
      public void handleMessage(Message msg){
          if(msg.what == 0){
              String data = (String)msg.obj;
              //Toast.makeText(backgroundService.this, data, Toast.LENGTH_SHORT).show();

              //URL을 통해 받아온 JSON을 파싱한 결과를 확인하기 위해 화면에 결과 띄워줌(테스트용 코드)
              //Toast.makeText(getApplicationContext(),"온도값 : "+temp+"\n가스값 : "+gas,Toast.LENGTH_LONG).show();
          }
      }
    };

    //URL로부터 JSON데이터를 가져와 파싱하는 메소드
    public void makeRequest() {
        //아두이노 센서 측정값 데이터 중 가장 최근에 기록된 데이터 1개에 대한 정보를 JSON 형태로 보여주는 URL
        String url = "https://api.thingspeak.com/channels/953092/feeds.json?api_key=D9OG8OSWJEMRHFJY&results=1";

        StringRequest request = new StringRequest(
                Request.Method.GET,
                url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        json = response; //URL요청 결과 데이터를 json 변수에 넣음
                        Log.d("URL RS", json); //json변수에 들어간 값을 로그캣에 출력
                        dataParsing(json); //가져온 JSON데이터 파싱

                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        //println("에러 -> " + error.getMessage());
                    }
                }
        ) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String,String> params = new HashMap<String,String>();

                return params;
            }
        };

        request.setShouldCache(false);
        requestQueue.add(request);
    }

    //JSON 데이터 파싱 메소드: JSON 데이터 파싱 후 파싱 결과값을 변수에 넣음
    public void dataParsing(String data){
        Gson gson = new Gson();
        SensingData gsonResult = gson.fromJson(data, SensingData.class);


        //파싱 결과 마지막 데이터 엔트리id와 이전 데이터 엔트리id가 다르면 클라우드 서버에 새로운 데이터가 추가된 것이므로 파싱 결과값을 변수에 넣어 온도값 및 가스값 데이터를 업데이트함
        posteriorEntryId = gsonResult.channel.last_entry_id; //파싱 결과 엔트리id를 가져와 posteriorEntryId에 넣음
        if(priorEntryId != gsonResult.channel.last_entry_id){
            temp = gsonResult.feeds.get(0).field1; //파싱 결과 온도값을 가져와 temp에 넣음
            gas = gsonResult.feeds.get(0).field2; //파싱 결과 가스값을 가져와 gas에 넣음
            detectTime = gsonResult.feeds.get(0).created_at; //파싱 결과 데이터 생성 시각을 가져와 detectTime에 넣음
        }

        Log.d("Json Parsing", gsonResult.channel.last_entry_id+ " / " +gsonResult.feeds.get(0).field1+" / "+ gsonResult.feeds.get(0).field2);

    }

    //화재여부 판단 메소드: 측정한 온도값과 가스값이 화재라고 판단되는 특정 경계값을 넘으면 화재여부는 true, 그렇지 않으면 화재여부는 false
    public boolean fireDecision(){ //현재 화재판단 기준 경계값은 테스트 간편화를 위해 임시값으로 작성하였음.
        if(temp >= 50 && gas >= 200){
            return true;
        }else{
            return false;
        }
    }

    //DB관리를 위한 도우미 클래스
    class DataSensingDBHelper extends SQLiteOpenHelper{
        public DataSensingDBHelper(Context context){
            super(context, "DataSensing.db", null, 1);
        }

        public void onCreate(SQLiteDatabase db){
            //화재감지 기록 DB 생성
            db.execSQL("create table DataSensing(senNum integer primary key autoincrement, time text, temper real, gas real);");
        }

        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion){
            db.execSQL("drop table if exists DataSensing");
            onCreate(db);
        }
    }

    //화재감지 기록 저장 메소드: 화재 발생 당시의 날짜 및 시각, 온도값, 가스값 데이터를 DB에 저장함
    public void recordFire(){
        //DB관련 변수 선언
        SQLiteDatabase db;
        ContentValues row;
        Cursor cursor;

        db=mHelper.getWritableDatabase(); //쓰기 가능한 SQLite DB 인스턴스 생성
        cursor=db.rawQuery("select time, temper, gas from DataSensing", null);

        //화재 발생 당시 기준으로 현재 날짜 및 시각, 온도값, 가스값 데이터를 DB에 저장
        db.execSQL("insert into DataSensing values(null, datetime('now','localtime'), "+temp+", "+gas+");");

        mHelper.close();
    }


    /*
     * 메소드 이름 : fireAlarm
     * 메소드 기능 : 화재여부가 true일 때 푸시알림을 보여준다.
     * */
    public void fireAlarm() {
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, new Intent(getApplicationContext(), MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "default")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("우리집 화재 알리미")
                .setContentText("화재가 발생했습니다!!!")
                .setColor(Color.RED)
                .setAutoCancel(true)
                .setDefaults(Notification.DEFAULT_ALL) // 소리, 진동, 불빛 모두 발생
                .setContentIntent(pendingIntent)
                .setTicker("화재가 발생했습니다.");

        // 알림 표시
        NotificationManager notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(new NotificationChannel("default", "기본채널", NotificationManager.IMPORTANCE_HIGH));
        }

        notificationManager.notify(1, builder.build());
    }

}
