/*
-작성자: 2017038023 반예린
-해당 소스파일 정보: 온도값 및 가스값을 측정하여 화재여부를 판단하고, 화재여부가 참이면 LED와 부저로 경보한 후 클라우드 서버에 화재발생 시각과 당시의 온도 및 가스 측정값을 전송하는 기능.
-구현 완료된 기능: 온도값 및 가스값 측정에 대한 기능.
-테스트 환경: Arduino Uno(보드), ESP8266 GPIO WIFI Shield(와이파이 쉴드)
            LM35DZ(온도센서), MQ-2(가스센서), 5R3HT-10(LED), FQ-030(피에조 부저)
 */

const int temperaturePin = A0; //lm35 온도센서 핀 설정
const int gasPin = A1; //MQ-2 가스센서 핀 설정

int temp; //온도값
int gas; //가스값
 
void setup() {
  Serial.begin(115200);  //시리얼 통신속도 설정

}
 
 
void loop() {
  temp = 5.0*analogRead(temperaturePin)*100.0/1024.0; //온도센서값 읽어온 뒤 섭씨온도로 변환(LM35 계산 공식에 의함)
  //gas = (float)analogRead(gasPin)/1024*5.0;//가스센서값 읽어온 뒤 백분율로 변환
  gas = analogRead(gasPin);//가스센서값 읽어옴
  
  Serial.print("온도: "); Serial.print(temp); Serial.println(" 도\n"); //시리얼 모니터에 온도값 출력
  Serial.print("가스: "); Serial.println(gas);
  Serial.println("\n");

  delay(1000); //측정 간격 설정 (1000 = 1초) 
}
