# TTN Tester
IoT project to test proper function of TTN (the things network) gateway.
Node acts as TTN device, sends an uplink message as PING. 
Backend acts as TTN application, replies to uplink node PING with downlink PONG.
A completed PING - PONG sequence represents a full test of both transmission directions of the TTN gateway.

## test scenario
The PING - PONG test works only if the node uplink transmissions are ONLY received by the TTN gateway under test.
By design LoraWAN is intended to work with multiple gateways receiving the same uplink transmission.
The problem stems from the fact that the downlink transmission is executed by just one specifically selected gateway. 
The selection of this gateway is conducted by a computed gateway score based on factors like RSSI of the received node transmission and 
gateway load. If the node uplink transmission (PING) is received by multiple gateways, there are multiple gateway candidates
for downlink selection. The more candidates the smaller the probability that our gateway under test will 
be selected for the PONG downlink transmission i.e. the probability to complete our test.
By downgrading the node antenna we ensure that just our gateway under test will receive the transmission thus
there is no other candidate to compete with the downlink gateway selection.   

## evaluating test results
Backend and node both write log messages (node through the serial port with 115200 baud rate).
The effective test result is displayed also on the node LCD as short state description followed by a date time stamp
e.g. 'SUCC 20201221 12:44'

a '-' output indicates that there is no last test success date time stamp.

Please note that a test run is only executed at a pace of 2h and 15min due to the ISM (industrial science medical) 868Mhz frequency band limitations imposed 
by the BAK. The limitation is described below in more details.
There is also a chance that a test cannot be executed properly because of the downlink gateway selection problem described below in more
details. If a downlink gateway is selected by TTN different from our gateway under test, the test procedure is aborted till the next
test interval starts. 
 
## software architecture
### backend
Java SE / gradle application based on java TTN github library. The latter uses the MQTT client library Eclipse Paho MQTT to 
interact with the TTN MQTT data API. Business logic is implemented by providing lambda functions attached to certain 
MQTT topic onMessage handlers i.e. the lambda function is executes once a new message arrives in the MQTT topic.
The MQTT topics from the TTN data API represent events like arrival of an uplink message, scheduling of a downlink message,
node ACK for downlink and downlink message sent.
The backend contains a state engine (see documentation in ./etc) and performs state transition based on MQTT topic handler callbacks.
TTN limitations concerning downlink messages is handled with a scheduled, recurring timer thread/executor that resets the
state to an inital state. A PING-PONG interval of 2h and 15min ensures that the 10 downlink messages per day (based on a sliding window)
are respected and no messages are disappearing/being ignored.      

### node
Arduino C++ project based on the Arduino adapted IBM LMIC LoraWAN library.
The Visual Studio Code PlatformIO project is setup to run on a TTGO v.1.6 Esp32 device.
PlatformIO takes care of the dependencies, it eases the struggle of finding and integrating the correct LMIC
library and configure it for the board.
There is basically no business logic running on the node except that the initial PING is emitted as uplink message
to the TTN application. The node implements all necessary LMIC events (join, uplink, downlink etc.).
The backend implements the test business logic and forwards the results to the node, which in turn 
displays the result on the LCD.
The displayed test result is basically a short state name with a time stamp e.g. 'SUCC 20201212 12:39'.
The last successful PING-PONG time stamp remains visible on the LCD.
The test interval of 2h 15min is enforced by the backend. The node continues to emit PINGs but
the backend throttles the test by only replying with a downlink PONG once every 2h 15min.
 
## test setup
The node needs to be placed near the TTN gateway. If possible ensure that the LoraWAN antenna's performance
is decreased sufficiently to lower the chance of signal reception by other gateways.
Register a TTN device and application and generate the needed access credentials (applicationID / access key).
The gateway EUI from the gateway under test is also needed.

## starting the backend - TTN application
### backend configuration
The java application takes input from the following environment variables:
#### appId
the TTN application ID obtained from the TTN console

#### region
e.g. 'eu' for europe

#### accessKey
the application access key obtained from the TTN console

#### gatewayEUI
the EUI of the TTN gateway under test e.g. 'eui-b827ebfffe4ad9d9'

#### enableConfirmation
this environment variable is optional, if set to 'true' the downlink messages are sent 
with the confirmation flag set to true thus the node has to confirm downlink reception;
this will result in MQTT messages in the 'down/acks' topic; if this flag is active a state 
transistion is only executed if the PONG was sent and an ACK received from the node

### compilation
The gradle target 'fatJar' is used to create a shadowed single JAR file.
e.g. 
'./gradlew fatJar'
creates a JAR file: ./build/libs/ttn-echo-backend-1.0-SNAPSHOT-all.jar

### execution
The created JAR can be executed in shell with prepended system environment variables
fish shell exampe:
'env enableConfirmation="true" env appId="deadbeefdeadbeef" env accessKey="ttn-account-v2.****" env gatewayEUI="eui-b827ebfffe4ad9d9" env region="eu" java -jar ./build/libs/ttn-echo-backend-1.0-SNAPSHOT-all.jar'
bash example:
'enableConfirmation="true" appId="deadbeefdeadbeef" accessKey="ttn-account-v2.****" gatewayEUI="eui-b827ebfffe4ad9d9"t region="eu" java -jar ./build/libs/ttn-echo-backend-1.0-SNAPSHOT-all.jar'

## starting the node - TTN device
The configuration of SPI pinmap is done in source, the same is true for TTN credentials.
If you need to change credentials, you need to change the following variables in main.cpp source file:
static const u1_t PROGMEM APPEUI[8] = {0x**, 0x**, ...};
static const u1_t PROGMEM DEVEUI[8] = {0x**, 0x**, ...};
static const u1_t PROGMEM APPKEY[16] = {0x**, 0x**, ...};

Note: the keys are in litte endian hex format (LSB first), there is a switch in the TTN console to convert
the ASCII keys accordingly.

This is the SPI pin mapping in the main.cpp:
// Pin mapping
const lmic_pinmap lmic_pins = {
    .nss = 18,
    .rxtx = LMIC_UNUSED_PIN,
    .rst = 23,
    .dio = {26, 33, 32} // Pins for the Heltec ESP32 Lora board/ TTGO Lora32 with 3D metal antenna
};

No further configuration is necessary.

The platformIO project can be compiled and uploaded to the device through PlatformIO ide or through the
command line tools.
e.g. 
upload compiled code: 'pio run -t upload'
monitor serial: 'pio device monitor -p /dev/cu.SLAB_USBtoUART -b 115200'
