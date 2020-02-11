/*
-작성자: 2017038023 반예린
-해당 소스파일 정보: 센서를 통해 온도값 및 가스값을 측정하고, 측정값을 클라우드 서비스인 ThingSpeak에 전송하고, 화재여부가 true이면 LED와 부저로 경보함.
                  또한 아두이노 서버에 접속하려는 모바일 폰 클라이언트가 감지되면 카메라 모듈로 사진을 촬영하여 SD카드에 저장 후 아두이노 서버 웹페이지에 해당 사진을 출력함.
-구현 완료된 기능: 온도값 및 가스값 측정, 화재판단, LED 및 부저를 통한 화재경보, WIFI연결 및 클라우드 서버로의 데이터 전송, 아두이노 서버에 접속하려는 클라이언트 감지,
                카메라 모듈을 통한 사진 촬영, 촬영한 사진을 SD카드에 저장, 아두이노 서버의 웹페이지에 촬영사진 출력에 대한 기능.
-테스트 환경: Arduino Uno WIFI Rev2(보드)
            LM35DZ(온도센서), MQ-2(가스센서), 5R3HT-10(LED), FQ-030(피에조 부저), OV2640_MINI_2MP_PLUS(카메라 모듈), MH-SD Card Module(SD카드 모듈)
 */

/*
 * <참고사항>
 * 카메라 및 SD카드 모듈은 보드 오른편 가장자리에 있는 ICSP 핀에 다음과 같이 연결해야 함(그림 참조)
 * CS - 0, 1번을 제외한 아무 디지털 핀(해당 코드에서는 카메라 7번, SD카드 모듈 4번)
 * MISO - 위쪽 왼쪽
 * MOSI - 가운데 오른쪽
 * SCK - 가운데 왼쪽
 * SD카드 모듈은 3.3V 핀도 있으나 3.3V 전원으로 연결하면 SD카드 인식을 제대로 하지 못함. 5V를 사용할 것.
 * 
 * 아두캠 라이브러리 다운로드 후, ArduCAM.cpp 파일을 다음과 같이 수정할 것(수정하지 않으면 컴파일러 오류 발생)
 * #include <HardwareSerial.h>  ->   #include <api/HardwareSerial.h>
*/


#include <WiFiClient.h>
#include <ThingSpeak.h>
#include <stdlib.h>
#define DEBUG true

#include <WiFiNINA.h>
#include <Wire.h>
#include <ArduCAM.h>
#include <SPI.h>
#include <SD.h>
#include "memorysaver.h"

//헤더 파일 설정 확인 전처리기
#if !(defined (OV2640_MINI_2MP_PLUS)||(defined (ARDUCAM_SHIELD_V2) && defined (OV2640_CAM)))
#error Please select the hardware platform and camera module in the ../libraries/ArduCAM/memorysaver.h file
#endif

const int temperaturePin = A0; //lm35 온도센서 핀 설정
const int gasPin = A1; //MQ-2 가스센서 핀 설정
const int ledPin=13; //LED 핀 설정
const int buzzerPin=12; //부저 핀 설정

boolean checkFire; //화재여부

float temp; //온도값
float gas; //가스값

bool is_header = false;          //헤더 여부를 저장하는 변수
int total_time = 0;              //소요 시간을 저장하는 변수
const int CS = 7;                //아두캠 포트 번호(연결 부위에 맞게 변경해야 함)
const int SD_CS = 4;             //SD카드 리더기 포트 번호(연결 부위에 맞게 변경해야 함)
char picture[16]="";

//wifi 설정
WiFiClient client1;
char ssid[] = "bigeyes0";         //Wi-Fi SSID
char password[] = "75857585";  //Wi-Fi 비밀번호
int status = WL_IDLE_STATUS;

//thingSpeak 설정
unsigned long ChannelID = 953092;
const char* WriteAPIKey = "9299T94BKG1RGE1I";

unsigned long lastCheck; //센서값 측정 간격을 위한 현재시간 저장변수

ArduCAM myCAM(OV2640, CS);  //아두캠 객체
WiFiServer server(80);      //서버 객체. 매개변수는 포트 번호로 기본적으로 80 고정

//카메라 구동 함수
void camCapture(WiFiClient client){
  myCAM.flush_fifo();
  myCAM.clear_fifo_flag();
  myCAM.start_capture();
  Serial.println(F("촬영을 시작합니다."));
  total_time = millis();
  while ( !myCAM.get_bit(ARDUCHIP_TRIG, CAP_DONE_MASK));
  Serial.println(F("촬영이 끝났습니다."));
  total_time = millis() - total_time;
  Serial.print(F("소모 시간(ms):"));
  Serial.println(total_time, DEC);
  total_time = millis();
  
  read_fifo_burst(myCAM);
  
  total_time = millis() - total_time;
  Serial.print(F("저장에 걸린 시간(ms):"));
  Serial.println(total_time, DEC);
  //Clear the capture done flag
  myCAM.clear_fifo_flag();
  delay(5000);
}

uint8_t read_fifo_burst(ArduCAM myCAM)
{
  uint8_t temp = 0, temp_last = 0;
  uint32_t length = 0;
  static int i = 0;
  static int k = 0;
  char str[16];
  File outFile;
  byte buf[256];
  length = myCAM.read_fifo_length();
  Serial.print(F("FIFO 버퍼 길이:"));
  Serial.println(length, DEC);
  if (length >= MAX_FIFO_SIZE) //8M
  {
    Serial.println("Over size.");
    return 0;
  }
  if (length == 0 ) //0 kb
  {
    Serial.println(F("Size is 0."));
    return 0;
  }
  myCAM.CS_LOW();
  myCAM.set_fifo_burst();//Set fifo burst mode
  i = 0;
  while ( length-- )
  {
    temp_last = temp;
    temp =  SPI.transfer(0x00);
    //Read JPEG data from FIFO
    if ( (temp == 0xD9) && (temp_last == 0xFF) ) //If find the end ,break while,
    {
      buf[i++] = temp;  //save the last  0XD9
      //Write the remain bytes in the buffer
      myCAM.CS_HIGH();
      outFile.write(buf, i);
      //Close the file
      outFile.close();
      Serial.println(F("OK"));
      is_header = false;
      myCAM.CS_LOW();
      myCAM.set_fifo_burst();
      i = 0;
    }
    if (is_header == true)
    {
      //Write image data to buffer if not full
      if (i < 256)
        buf[i++] = temp;
      else
      {
        //Write 256 bytes image data to file
        myCAM.CS_HIGH();
        outFile.write(buf, 256);
        i = 0;
        buf[i++] = temp;
        myCAM.CS_LOW();
        myCAM.set_fifo_burst();
      }
    }
    else if ((temp == 0xD8) & (temp_last == 0xFF))
    {
      is_header = true;
      myCAM.CS_HIGH();
      //Create a avi file
      k = k + 1;
      itoa(k, str, 10);
      strcat(str, ".jpg");
      strcpy(picture, str);
      Serial.print("저장된 파일명: ");
      Serial.println(picture);
      
      //Open the new file
      outFile = SD.open(str, O_WRITE | O_CREAT | O_TRUNC);
      if (! outFile)
      {
        Serial.println(F("파일 열기에 실패했습니다."));
        //while (1);
      }
      myCAM.CS_LOW();
      myCAM.set_fifo_burst();
      buf[i++] = temp_last;
      buf[i++] = temp;
    }
  }
  myCAM.CS_HIGH();
  return 1;
}

void setup() {

uint8_t vid, pid;
  uint8_t temp;
  
#if defined(__SAM3X8E__)
  Wire1.begin();
#else
  Wire.begin();
#endif
  Serial.begin(9600);
  Serial.println(F("아두캠을 가동합니다."));

  //핀 설정
  pinMode(ledPin, OUTPUT); //LED를 output으로 설정
  pinMode(buzzerPin, OUTPUT); //BUZZER를 output으로 설정
  pinMode(CS, OUTPUT); //카메라를 output으로 설정

  // SPI 초기화
  SPI.begin();
  myCAM.write_reg(0x07, 0x80);
  delay(100);
  myCAM.write_reg(0x07, 0x00);
  delay(100);
  
  //아두캠 SPI 통신 확인
  myCAM.write_reg(ARDUCHIP_TEST1, 0x55);
  temp = myCAM.read_reg(ARDUCHIP_TEST1);
  if (temp != 0x55){
    Serial.println(F("SPI 인터페이스 오류입니다."));
    while(true);
  }

  //카메라 모델이 OV2640인지 확인
  myCAM.wrSensorReg8_8(0xff, 0x01);
  myCAM.rdSensorReg8_8(OV2640_CHIPID_HIGH, &vid);
  myCAM.rdSensorReg8_8(OV2640_CHIPID_LOW, &pid);
  if ((vid != 0x26 ) && (( pid != 0x41 ) || ( pid != 0x42 )))
    Serial.println(F("OV2640 카메라 모듈이 연결되지 않았습니다."));
  else
    Serial.println(F("OV2640 카메라 모듈이 정상 감지됐습니다."));
 

  //저장 포맷 및 크기 설정
  myCAM.set_format(JPEG);
  myCAM.InitCAM();
  myCAM.OV2640_set_JPEG_size(OV2640_320x240);
  myCAM.clear_fifo_flag();

  //SD카드 인식
  if (!SD.begin(SD_CS)) {
      Serial.println(F("SD카드가 연결되지 않았습니다."));
      return;
  }
  Serial.println(F("SD카드가 정상 감지됐습니다."));
  
  //index.htm 파일 인식
  if (!SD.exists("index.htm")) {
    Serial.println(F("index.htm 파일을 찾을 수 없습니다."));
    return;
  }
  Serial.println(F("index.htm 파일이 정상 감지됐습니다."));

  //Wi-Fi 모듈 확인
  if (WiFi.status() == WL_NO_MODULE) {
    Serial.println(F("Wi-Fi 연결에 실패했습니다."));
    //모듈이 작동하지 않으면 코드 진행 정지
    while (true);
  }
  
  //Wi-Fi 접속
  while (status != WL_CONNECTED) {
    Serial.print(F("다음 SSID로 접속 중: "));
    Serial.println(ssid);
    status = WiFi.begin(ssid, password);

    //접속을 위한 대기
    delay(5000);
  }
  server.begin();
  
  //연결이 끝났으면 시리얼 모니터에 SSID 및 IP 표시
  Serial.print(F("SSID: "));
  Serial.println(WiFi.SSID());

  IPAddress ip = WiFi.localIP();
  Serial.print(F("IP 주소: "));
  Serial.println(ip);

  ThingSpeak.begin(client1);
  lastCheck = 0; //현재시간을 0으로 초기화

}

void loop() {
  tkPicture(); //아두이노 서버에 접속하려는 모바일 폰 클라이언트가 감지되면 아두캠으로 사진찍은 후 아두이노 서버 웹페이지에 사진을 출력
  sensingAndUpload(); //센서를 통해 온도값 및 가스값을 측정하고 측정값을 클라우드 서비스인 ThingSpeak에 전송하고, 화재판단 후 화재 여부가 true이면 LED와 부저로 경보
  
}


//센서를 통해 온도값 및 가스값을 측정하고 측정값을 클라우드 서비스인 ThingSpeak에 전송하고,
//화재판단 후 화재 여부가 true이면 LED와 부저로 경보하는 메소드
void sensingAndUpload(){
  float c_time = 20000 * 0.1; //측정간격. 2초
  
  //현재시간 - 센서값 측정 후 시간 > 측정간격 이면 실행.
  //즉, 현재 시간이 마지막 측정시간으로 부터 2초를 초과했으면 실행.
  if(millis() - lastCheck > c_time) {
    static boolean data_state = false;
    
    temp = (float)5.0*analogRead(temperaturePin)*100.0/1024.0; //온도센서값 읽어온 뒤 섭씨온도로 변환(LM35 계산 공식에 의함)
    //gas = (float)analogRead(gasPin)/1024*5.0; //가스센서값 읽어온 뒤 백분율로 변환
    gas = (float)analogRead(gasPin); //가스센서값 읽어옴

    //측정한 온도값과 가스값이 화재라고 판단되는 특정 경계값을 넘으면 화재여부는 true, 그렇지 않으면 화재여부는 false. 현재 화재판단 기준 경계값은 테스트 간편화를 위해 임시값으로 작성하였음.
    if(temp >= 40 && gas >= 200){
      checkFire = true;
    }else{
      checkFire = false;
    }
  
   //시리얼 모니터에 온도값 및 가스값 출력
   Serial.print("온도: "); Serial.print(temp); Serial.println(" 도\n");
   Serial.print("가스: "); Serial.println(gas);
   Serial.println("\n");

   if(checkFire == true){//화재판단 결과 화재여부가 참인 경우
      digitalWrite(ledPin, HIGH); //LED 불빛 켜기
      tone(buzzerPin, 800); // 800 - 음의 높낮이, 100 - 부저 지속 시간
      //delay(1000); //1초 대기
      //digitalWrite(ledPin, LOW); //LED 불빛 끄기
      //tone(buzzerPin, 800, 100); // 800 - 음의 높낮이, 100 - 부저 지속 시간
      //delay(1000); //측정 간격 설정 (1000 = 1초)
    }else{
      digitalWrite(ledPin, LOW); //LED 불빛 끄기
      noTone(buzzerPin); //부저 끄기
      //delay(1000); //측정 간격 설정 (1000 = 1초)
    }


  ThingSpeak.setField( 1, (float) temp);
  ThingSpeak.setField( 2, (float) gas);
  ThingSpeak.writeFields(ChannelID, WriteAPIKey);

  lastCheck = millis();
    
  }
  
  delay(100);
  
}


void tkPicture(){
  WiFiClient client = server.available();   //아두이노 서버에 접속하려는 클라이언트 감지
  
  if (client) { //클라이언트가 감지되면
    Serial.println(F("클라이언트가 감지됐습니다."));

    camCapture(client); //카메라 캡처 실행
    
    boolean currentLineIsBlank = true; //http 요청은 마지막 줄이 빈 줄로 끝나야 함. 이를 위한 변수
    while (client.connected()) {
      if (client.available()) {
        char c = client.read();       //클라이언트 정보를 읽어온 후
        Serial.write(c);              //이를 시리얼 모니터에 표시
        
        //읽어온 문자가 줄바꿈 문자이고 마지막 줄이 빈 줄이면 http 요청이 끝난 것. 이때부터 클라이언트에게 화면 출력 시작
        if (c == '\n' && currentLineIsBlank) {         
          client.println("HTTP/1.1 200 OK");           //표준 http 응답 헤더 보내기
          //client.println("Content-Type: text/html");   //html 텍스트임을 명시
          client.println("Connection: close");         //이 연결은 응답이 끝난 직후 종료될 것(그러나 무한 반복되고 있음)
          client.println();

//          client.println(F("<!DOCTYPE html>"));
//          client.println(F("<html>"));
//          client.println(F("<head>"));
//          client.println(F("<meta charset=\"utf-8\">"));
//          client.println(F("<title>현장확인</title>"));
//          client.println(F("</head>"));
//          client.println(F("<bod>"));
//          //client.println(F("<img class=\"mb-3 mb-lg-0 \" style=\"width:70%\" alt=\"\" src=\"1.jpg\" />"));
//          client.println(F("<p>테스트</p>"));
//          client.println(F("</body>"));
//          client.println(F("</html>"));

     
          //File webFile = SD.open("index.htm");  //화면 html 파일 열기
          File webFile = SD.open(picture);  //jpg파일 열기

            if (webFile) {                     //파일이 열렸으면
              while(webFile.available())
                client.write(webFile.read());  //정보를 읽어와 클라이언트에게 출력
              
              webFile.close();                 //파일 닫기
            }else{// SD 카드가 없거나 파일이 없을 경우, 메시지를 직접 보냄
              client.println(F("HTTP/1.1 200 OK"));
            client.println(F("Content-Type: text/html"));
            client.println(F("Connection: close"));  // the connection will be closed after completion of the response
            client.println(F("Refresh: 10"));  // refresh the page automatically every 10 sec
            client.println();
            client.println(F("<!DOCTYPE HTML>"));
            client.println(F("<html>"));
            client.println(F("  <head>"));
            client.println(F("    <meta http-equiv=\"Content-Type\" content=\"text/html;charset=UTF-8\">"));
            client.println(F("  </head>"));
            client.println(F("  <body>"));
            client.println(("    <p>아두이노 웹 서버<br>"));
            client.println(("       ================</p>"));
            client.println(("SD 카드가 장착되어 있지 않습니다.<br>"));
            client.println(("확인한 후에 다시 시도하여 주세요!<br>"));
            client.println(F("  </body>"));
            client.println(F("</html>"));
            }
          break;
        }
        if (c == '\n') {
          // you're starting a new line
          currentLineIsBlank = true;
        } else if (c != '\r') {
          // you've gotten a character on the current line
          currentLineIsBlank = false;
        }
      }
      //delay(10000);     //10초마다 갱신
    }
    // give the web browser time to receive the data
    delay(1);

    client.stop();  //클라이언트 연결 해제
    Serial.println(F("클라이언트 연결이 해제됐습니다."));
  }
}
