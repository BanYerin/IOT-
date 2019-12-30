/*
-작성자: 2017038023 반예린
-해당 소스파일 정보: 화재 신고하기 화면 및 화재 신고에 대한 기능.
                    신고 버튼을 클릭하면 DB에 저장된 신고 연락처와 사용자 위치정보를 이용하여 화재 신고 메시지를 자동으로 전송.
-구현 완료된 기능: 관리자에게 신고, 소방서로 신고, 메시지 전송, 홈 화면으로 이동, DB 연동에 대한 기능.
-테스트 환경: SAMSUNG Galaxy S7(AVD), API 22
 */

package com.example.iotfirealarm;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.View;

public class ReportActivity extends AppCompatActivity {
    UserInfoDBHelper mHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);

        mHelper=new UserInfoDBHelper(this);
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

    //관리자 신고 메소드
    public void onClickReportM(View v){
        String tel="010-7455-3507"; //메시지를 전송할 임시 관리자 번호. 나중에 DB에서 받아와야함
        String addr="충북 청주시 서원구 개신동 A번지 101호"; //임시 사용자 주소. 나중에 DB에서 받아와야함
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

        sendSMS(tel, msg); //화재 신고 메시지 전송

    }

    //소방서 신고 메소드
    public void onClickReportFS(View v) {
        String tel="119"; //메시지를 전송 소방서 번호
        String addr="충북 청주시 서원구 개신동 A번지 101호"; //임시 사용자 주소. 나중에 DB에서 받아와야함
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

        sendSMS(tel, msg); //화재 신고 메시지 전송

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

//    /**
//     * 사용자가 권한을 허용했는지 거부했는지 체크
//     * reference : http://ande226.tistory.com/136
//     * @param requestCode 1000번
//     * @param permissions 개발자가 요청한 권한들
//     * @param grantResults 권한에 대한 응답들
//     * permissions와 grantResults는 인덱스 별로 매칭된다.
//     */
//    @Override
//    public void onRequestPermissionsResult(int requestCode,
//                                           @NonNull String[] permissions,
//                                           @NonNull int[] grantResults) {
//        if (requestCode == 1000) {
//
//    /* 요청한 권한을 사용자가 "허용"했다면 인텐트를 띄워라
//    내가 요청한 게 하나밖에 없기 때문에. 원래 같으면 for문을 돈다.*/
//            if ( grantResults.length > 0 &&
//                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                if (ActivityCompat.checkSelfPermission( this, Manifest.permission.CALL_PHONE) !=
//                        PackageManager.PERMISSION_GRANTED) {
//                    Uri n = Uri.parse("tel: " + etNumber.getText());
//                    startActivity(new Intent(Intent.ACTION_CALL, n));
//                }
//            } else {
//                Toast.makeText(MainActivity.this, "권한 요청을 거부했습니다.", Toast.LENGTH_SHORT).show();
//            }
//
}
