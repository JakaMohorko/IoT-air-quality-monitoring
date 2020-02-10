#include "mbed.h"
#include "MiCS6814_GasSensor.h"
// #include "MMA8652.h"
#include "Adafruit_SGP30.h"
#include "dust_sensor.h"

// ========== serial connection setups ===========
Serial pc(USBTX, USBRX);

// setup bluetooth
// D1: TX 
// DO: RX 
RawSerial blue(D1, D0, 9600);
// ===============================================


// =========== SENSOR SETUP ============ 

// initialise mix gas sensor
MiCS6814_GasSensor sensor(I2C_SDA, I2C_SCL);

// Total Volatile Organic Compound (TVOC)
// reading and an equivalent carbon dioxide reading (eCO2) 
Adafruit_SGP30 mox(I2C_SDA, I2C_SCL);
// ======================================

/*
 * recieves data from device and prints to screen
 * debugging
*/
void callback_ex() {
    // Note: you need to actually read from the serial to clear the RX interrupt
    pc.putc(blue.getc());
    myled = !myled;
}

int main(){

    // bluetooth setup
    blue.attach(&callback_ex);

    // mox setup
    mox.begin();
    mox.IAQinit();

    pc.printf("test start\n");

    float accel_data[3];

    // setup
    dust_setup();

    while(1) {// pc.printf("Gas sensor\r\n");
        // pc.printf("NH3: %.2f ppm, CO: %.2f ppm, NO2: %.2f ppm, C3H8: %.2f ppm \r\n", sensor.getGas(NH3), sensor.getGas(CO), sensor.getGas(NO2), sensor.getGas(C3H8));
        // pc.printf("C4H10: %.2f ppm, CH4: %.2f ppm, H2: %.2f ppm, C2H5OH: %.2f ppm \r\n", sensor.getGas(C4H10), sensor.getGas(CH4), sensor.getGas(H2), sensor.getGas(C2H5OH));
        // pc.printf("Mox sensor\r\n");
        // mox.IAQmeasure();
        // pc.printf("TVOC: %d, eCO2: %d\r\n", mox.TVOC, mox.eCO2);
        // pc.printf("Dust sensor\r\n");
        // float dust_val = read_dust_sensor();
        // pc.printf("dust concentration: %4.1f ug/m3z\r\n", dust_val);
        // pc.printf("\r\n");

        // === READ VALUES === 
        float dust_val = read_dust_sensor();
        // read mox
        mox.IAQmeasure();
        int tvoc    = mox.TVOC;
        int eco2    = mox.eCO2;
        // read gases from MiCS6814_GasSensor
        float nh3   = sensor.getGas(NH3);
        float co    = sensor.getGas(CO);
        float no2   = sensor.getGas(NO2);
        float methane = sensor.getGas(CH4);

        // debug
        pc.printf("\r\nDust: %f\r\n", dust_val);
        pc.printf("TVOC: %d, eCO2: %d\r\n", tvoc, eco2);
        pc.printf("NH3: %.2f ppm, CO: %.2f ppm, NO2: %.2f ppm, CH4: %2.f\r\n", nh3, co, no2, methane); // not transfering all data

        // send data over bluetooth serial
        blue.printf("\r\nDust: %f\r\n", dust_val);
        blue.printf("TVOC: %d, eCO2: %d\r\n", tvoc, eco2);
        blue.printf("NH3: %.2f ppm, CO: %.2f ppm, NO2: %.2f ppm, CH4: %2.f\r\n", nh3, co, no2, methane); // not transfering all data


        // how frequently to send data
        ThisThread::sleep_for(1000);
    }
}