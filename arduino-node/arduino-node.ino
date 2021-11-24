#include <Servo.h>

const int BUFFER_SIZE = 100;
char buff[BUFFER_SIZE];

Servo servo;
const int servoPIN = 12;
const int servoMin = 100;
const int servoMax = 180;

const int publishTime = 20000;   // publish every 20s
unsigned long myTime  = millis();

enum Topic {
  TOPIC_LED,
  TOPIC_SERVO
};

void setup() {
  // setup built in led
  pinMode(LED_BUILTIN, OUTPUT);

  // setup servo
  servo.attach(servoPIN);
  servo.write(servoMin);
  
  // set baud rate
  Serial.begin(9600);

  // if analog input pin 0 is unconnected, random analog
  // noise will cause the call to randomSeed() to generate
  // different seed numbers each time the sketch runs.
  // randomSeed() will then shuffle the random function.
  randomSeed(analogRead(0));
}

void loop() {
  // put your main code here, to run repeatedly:
  if (Serial.available() > 0) {
    // Serial.readBytesUntil('#', buff, BUFFER_SIZE);
    Serial.readBytesUntil('#', buff, BUFFER_SIZE);  // !<TOPIC_ENUM>:<VALUE>#
    String payload = String(buff);
    if (payload.length() > 0) {
      int value = payload.substring(payload.indexOf(':') + 1, payload.indexOf('#')).toInt();
      int code = payload.substring(payload.indexOf('!') + 1, payload.indexOf(':')).toInt();
      switch(code) {
      case 0:
        digitalWrite(LED_BUILTIN, (value == 1) ? HIGH : LOW);
        break;
      case 1:
        servo.write(constrain(value, servoMin, servoMax));
        break;
      default:
        break;
      } // !switch
    } // !payload > 0
  } // !serial.available()
  
  if (millis() - myTime > publishTime) {
    myTime = millis();
    Serial.print("!1:temperature:" + String(random(0, 100)) + "#");
    Serial.print("!1:humidity:" + String(random(0, 100)) + "#");
  }
}
