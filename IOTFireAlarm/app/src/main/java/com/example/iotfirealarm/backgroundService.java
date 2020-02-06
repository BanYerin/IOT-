/*
-작성자: 2017038023 반예린(백그라운드 서비스, 클라우드 서버로부터 데이터 수신, JSON 파싱, 화재여부 판단 기능), 2015023025 배나영(화재 푸시알림 기능)
-해당 소스파일 정보: 백그라운드 서비스, 클라우드 서버로 부터의 데이터 수신, JSON 파싱, 화재여부 판단, 화재 푸시알림에 대한 기능.
                    URL을 통해 클라우드 서버로부터 아두이노 센서 측정값 데이터를 JSON 형태로 가져와 파싱하여 필요한 데이터를 추출한 후
                    추출한 온도값 및 가스값을 이용하여 화재여부를 판단하고 화재여부가 true이면 화재 푸시알림이 작동하는 기능을
                    백그라운드 서비스에서 계속 유지하여 실행함.
-구현 완료된 기능: 백그라운드 서비스, 클라우드 서버로 부터의 데이터 수신, JSON 파싱, 화재여부 판단, 화재 푸시알림에 대한 기능.
-테스트 환경: SAMSUNG Galaxy S7(AVD), API 22
 */

package com.example.iotfirealarm;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
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

public class backgroundService extends Service {
    boolean mQuit;
    static RequestQueue requestQueue;
    String json = ""; //json문자열
    float temp; //온도값
    float gas; //가스값

    public void onCreate() {
        super.onCreate();

        if (requestQueue == null) {
            requestQueue = Volley.newRequestQueue(getApplicationContext());
        }
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
                makeRequest(); //URL로부터 JSON데이터 가져와 파싱
                if(fireDecision() == true){ //화재 판단 결과가 ture이면 화재알림 동작
                    fireAlarm();
                }

                Message msg = new Message();
                msg.what = 0;
                msg.obj = temp +" / "+ gas;
                mHandler.sendMessage(msg);
                try{Thread.sleep(5000);}catch(Exception e){;} //스레드를 5초에 한번씩 실행하도록 설정
            }
        }
    }

    Handler mHandler = new Handler(){
      public void handleMessage(Message msg){
          if(msg.what == 0){
              String data = (String)msg.obj;
              //Toast.makeText(backgroundService.this, data, Toast.LENGTH_SHORT).show();

              //URL을 통해 받아온 JSON을 파싱한 결과를 확인하기 위해 화면에 결과 띄워줌
              Toast.makeText(getApplicationContext(),"온도값 : "+temp+"\n가스값 : "+gas,Toast.LENGTH_LONG).show();
          }
      }
    };

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

    //파싱 후 파싱 결과값을 변수에 넣음
    public void dataParsing(String data){
        Gson gson = new Gson();
        SensingData gsonResult = gson.fromJson(data, SensingData.class);
        temp = gsonResult.feeds.get(0).field1; //파싱 결과 온도값을 가져와 temp에 넣음
        gas = gsonResult.feeds.get(0).field2; //파싱 결과 가스값을 가져와 gas에 넣음
        Log.d("Json Parsing", gsonResult.feeds.get(0).field1+" / "+ gsonResult.feeds.get(0).field2);

    }

    //화재여부 판단 메소드: 측정한 온도값과 가스값이 화재라고 판단되는 특정 경계값을 넘으면 화재여부는 true, 그렇지 않으면 화재여부는 false
    public boolean fireDecision(){ //현재 화재판단 기준 경계값은 테스트 간편화를 위해 임시값으로 작성하였음.
        if(temp >= 15 && gas >= 55){
            return true;
        }else{
            return false;
        }
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
