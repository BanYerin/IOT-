/*
-작성자: 2017038023 반예린
-해당 소스파일 정보: 온도값 및 가스값을 측정하여 화재여부를 판단하고, 화재여부가 참이면 LED와 부저로 경보한 후 클라우드 서버에 화재발생 시각과 당시의 온도 및 가스 측정값을 전송하는 기능.
-구현 완료된 기능: 온도값 및 가스값 측정, 화재판단, LED 및 부저를 통한 화재경보, WIFI연결 및 클라우드 서버로의 데이터 전송에 대한 기능.
-테스트 환경: Arduino Uno WIFI Rev2(보드)
            LM35DZ(온도센서), MQ-2(가스센서), 5R3HT-10(LED), FQ-030(피에조 부저)
 */

#include <WiFiClient.h>
#include <ThingSpeak.h>
#include <stdlib.h>
#define DEBUG true

#include <SPI.h>
#include <WiFiNINA.h>


const int temperaturePin = A0; //lm35 온도센서 핀 설정
const int gasPin = A1; //MQ-2 가스센서 핀 설정
const int ledPin=13; //LED 핀 설정
const int buzzerPin=12; //부저 핀 설정

boolean checkFire; //화재여부

float temp; //온도값
float gas; //가스값

//wifi 설정
WiFiClient client;
char ssid[] = "iPhone";        // your network SSID (name)
char pass[] = "zzzzzzzz";    // your network password (use for WPA, or use as key for WEP)
int status = WL_IDLE_STATUS;     // the Wifi radio's status

//thingSpeak 설정
unsigned long ChannelID = 953092;
const char* WriteAPIKey = "9299T94BKG1RGE1I";

unsigned long lastCheck;

void setup() {
  pinMode(ledPin, OUTPUT); //LED를 출력으로 설정
  pinMode(buzzerPin, OUTPUT); //BUZZER를 출력으로 설정
  
  Serial.begin(9600);  //시리얼 통신속도 설정
  while (!Serial) {
    ; // wait for serial port to connect. Needed for native USB port only
  }

  // check for the WiFi module:
  if (WiFi.status() == WL_NO_MODULE) {
    Serial.println("Communication with WiFi module failed!");
    // don't continue
    while (true);
  }

  // attempt to connect to Wifi network:
  while (status != WL_CONNECTED) {
    Serial.print("Attempting to connect to WPA SSID: ");
    Serial.println(ssid);
    // Connect to WPA/WPA2 network:
    status = WiFi.begin(ssid, pass);

    // wait 10 seconds for connection:
    delay(10000);
  }

  // you're connected now, so print out the data:
  Serial.print("You're connected to the network");
  //printCurrentNet();
  printWifiData();


  ThingSpeak.begin(client);

  lastCheck = 0;
}

void loop() {
  float c_time = 60000 * 0.1;
  if(millis() - lastCheck > c_time) {
    static boolean data_state = false;
    
    temp = (float)5.0*analogRead(temperaturePin)*100.0/1024.0; //온도센서값 읽어온 뒤 섭씨온도로 변환(LM35 계산 공식에 의함)
    //gas = (float)analogRead(gasPin)/1024*5.0;//가스센서값 읽어온 뒤 백분율로 변환
    gas = (float)analogRead(gasPin);//가스센서값 읽어옴

    //측정한 온도값과 가스값이 화재라고 판단되는 특정 경계값을 넘으면 화재여부는 true, 그렇지 않으면 화재여부는 false. 현재 화재판단 기준 경계값은 테스트 간편화를 위해 임시값으로 작성하였음.
    if(temp >= 28 && gas >= 55){
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
      tone(buzzerPin, 800, 100); // 800 - 음의 높낮이, 100 - 부저 지속 시간
      delay(1000); //1초 대기
      digitalWrite(ledPin, LOW); //LED 불빛 끄기
      tone(buzzerPin, 800, 100); // 800 - 음의 높낮이, 100 - 부저 지속 시간
      delay(1000); //측정 간격 설정 (1000 = 1초)
    }else{
      delay(1000); //측정 간격 설정 (1000 = 1초)
    }


  ThingSpeak.setField( 1, (float) temp);
  ThingSpeak.setField( 2, (float) gas);
  ThingSpeak.writeFields(ChannelID, WriteAPIKey);

  lastCheck = millis();
    
  }
  
  delay(100);
}

void printWifiData() {
  // print your board's IP address:
  IPAddress ip = WiFi.localIP();
  Serial.print("IP Address: ");
  Serial.println(ip);
  Serial.println(ip);

  // print your MAC address:
  byte mac[6];
  WiFi.macAddress(mac);
  Serial.print("MAC address: ");
  printMacAddress(mac);
}

void printMacAddress(byte mac[]) {
  for (int i = 5; i >= 0; i--) {
    if (mac[i] < 16) {
      Serial.print("0");
    }
    Serial.print(mac[i], HEX);
    if (i > 0) {
      Serial.print(":");
    }
  }
  Serial.println();
}
