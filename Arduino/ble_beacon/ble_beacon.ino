#include "CurieBle.h"

static const char* bluetoothDeviceName = "MyBeacon"; // name the device

static const char* serviceUuid = "0000180c-0000-1000-8000-00805f9b34fb"; 
static const char* rssiCharacteristicUuid = "00002a56-0000-1000-8000-00805f9b34fa";
static const char* xCharacteristicUuid = "00002a56-0000-1000-8000-00805f9b34fb";
static const char* yCharacteristicUuid = "00002a56-0000-1000-8000-00805f9b34fc";

// critical beacon info
static const int referenceRssi = -57; // // Average 47-67
static const int xLocationInCentimeters = 185;
static const int yLocationInCentimeters = 59;

static const int characteristicTransmissionLength = sizeof(int);

BLEService service(serviceUuid);

BLEIntCharacteristic rssiCharacteristic(
  rssiCharacteristicUuid,
  BLERead
);
BLEIntCharacteristic xCharacteristic(
  xCharacteristicUuid,
  BLERead
);
BLEIntCharacteristic yCharacteristic(
  yCharacteristicUuid,
  BLERead
);

BLEPeripheral blePeripheral; // initialize bluetooth 



void setup() {
  Serial.begin(9600);
  while (!Serial) {;}
  Serial.print("Setting device name to: ");
  Serial.println(bluetoothDeviceName);
  blePeripheral.setLocalName(bluetoothDeviceName); // set the broadcast name
  
  blePeripheral.setAdvertisedServiceUuid(service.uuid());
  blePeripheral.addAttribute(service);
  blePeripheral.addAttribute(rssiCharacteristic);
  rssiCharacteristic.setValue(referenceRssi);
  blePeripheral.addAttribute(xCharacteristic);
  xCharacteristic.setValue(xLocationInCentimeters);
  blePeripheral.addAttribute(yCharacteristic);
  yCharacteristic.setValue(yLocationInCentimeters);

  Serial.println("Starting Bluetooth Broadcast");
  blePeripheral.begin(); // start broadcasting
  
}

void loop() {}
