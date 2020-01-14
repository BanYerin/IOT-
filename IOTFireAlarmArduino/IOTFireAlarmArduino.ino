/*
-작성자: 2017038023 반예린
-해당 소스파일 정보: 온도값 및 가스값을 측정하여 화재여부를 판단하고, 화재여부가 참이면 LED와 부저로 경보한 후 클라우드 서버에 화재발생 시각과 당시의 온도 및 가스 측정값을 전송하는 기능.
-구현 완료된 기능: 온도값 및 가스값 측정, 화재판단, LED 및 부저를 통한 화재경보에 대한 기능.
-테스트 환경: Arduino Uno(보드), ESP8266 GPIO WIFI Shield(와이파이 쉴드)
            LM35DZ(온도센서), MQ-2(가스센서), 5R3HT-10(LED), FQ-030(피에조 부저)
 */

const int temperaturePin = A0; //lm35 온도센서 핀 설정
const int gasPin = A1; //MQ-2 가스센서 핀 설정
const int ledPin=13; //LED 핀 설정
const int buzzerPin=12; //부저 핀 설정

boolean checkFire; //화재여부

int temp; //온도값
int gas; //가스값
 
void setup() {
  Serial.begin(115200);  //시리얼 통신속도 설정

  pinMode(ledPin, OUTPUT); //LED를 출력으로 설정
  pinMode(buzzerPin, OUTPUT); //BUZZER를 출력으로 설정
}
 
 
void loop() {
  temp = 5.0*analogRead(temperaturePin)*100.0/1024.0; //온도센서값 읽어온 뒤 섭씨온도로 변환(LM35 계산 공식에 의함)
  //gas = (float)analogRead(gasPin)/1024*5.0;//가스센서값 읽어온 뒤 백분율로 변환
  gas = analogRead(gasPin);//가스센서값 읽어옴

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
    tone(buzzerPin, 800, 100); // 500 - 음의 높낮이, 100 - 부저 지속 시간
    delay(1000); //1초 대기
    digitalWrite(ledPin, LOW); //LED 불빛 끄기
    tone(buzzerPin, 800, 100); // 500 - 음의 높낮이, 100 - 부저 지속 시간
    delay(1000); //측정 간격 설정 (1000 = 1초)
  }else{
    delay(1000); //측정 간격 설정 (1000 = 1초)
  }
  
}
