const int fps = 10;
float counter = 0;
float counter2 = 0;
float randChoice = 0.0;
float randChoice2 = 0.0;

float velocity = 0;
float ampere = 0;
float volt = 0;
float temp = 0;

String jsonString = "";

void setLedOn() {
  digitalWrite(LED_BUILTIN, HIGH);
}

void setLedOff() {
  digitalWrite(LED_BUILTIN, LOW);
}

void noise() {
  randChoice = random(0, 1001)/10;
  int probability = 100 - counter;
  if( randChoice < probability ) {
    counter += 0.1;
  } else if ( randChoice > probability ) {
    counter -= 0.1;
  }
  
  randChoice2 = random(0, 501)/10;
  int probability2 = 50 - counter2;
  if( randChoice2 < probability2 ) {
    counter2 += 0.1;
  } else if ( randChoice2 > probability2 ) {
    counter2 -= 0.1;
  }
  
  velocity = counter;
  temp = counter2;
  ampere = 6 + (random( 1, 10 ) / 10.);
  volt = 20 + random( 1, 10 ) / 10.;
}

void setup() {
  Serial.begin(19200);
  randomSeed(analogRead(0));
  pinMode(LED_BUILTIN, OUTPUT);
}

void encodeJson() {
  jsonString = "";
  jsonString = "<{";
  jsonString += "\"vel\": ";
  jsonString += velocity;
  jsonString += ",";
  jsonString += "\"amp\": ";
  jsonString += ampere;
  jsonString += ",";
  jsonString += "\"volt\": ";
  jsonString += volt;
  jsonString += ",";
  jsonString += "\"temp\": ";
  jsonString += temp;
  jsonString += "}>";
}

void sendStopReading() {
  jsonString = "<STOPIT>";
}

void loop() {
  noise();
  encodeJson();
  Serial.println(jsonString);
  delay(1000/fps);
}
