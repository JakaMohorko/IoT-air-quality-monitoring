#include "mbed.h"
#include "MiCS6814_GasSensor.h"
#include "MMA8652.h"
#include "Adafruit_SGP30.h"
#include "dust_sensor.h"

Serial pc(USBTX, USBRX);

MiCS6814_GasSensor sensor(I2C_SDA, I2C_SCL);
 
MMA8652 accel(I2C_SDA, I2C_SCL);

//  Total Volatile Organic Compound (TVOC)
// reading and an equivalent carbon dioxide reading (eCO2) 
Adafruit_SGP30 mox(I2C_SDA, I2C_SCL);

int main(){
    
    // mox setup
    mox.begin();
    mox.IAQinit();

    accel.MMA8652_config();

    pc.printf("test start\n");
 
    float accel_data[3];

    // setup
    dust_setup();

    while(1) {
        pc.printf("Gas sensor\r\n");
        pc.printf("NH3: %.2f ppm, CO: %.2f ppm, NO2: %.2f ppm, C3H8: %.2f ppm \r\n", sensor.getGas(NH3), sensor.getGas(CO), sensor.getGas(NO2), sensor.getGas(C3H8));
        pc.printf("C4H10: %.2f ppm, CH4: %.2f ppm, H2: %.2f ppm, C2H5OH: %.2f ppm \r\n", sensor.getGas(C4H10), sensor.getGas(CH4), sensor.getGas(H2), sensor.getGas(C2H5OH));

        pc.printf("Accel\r\n");
        accel.acquire_MMA8652_data_g(&accel_data[0]);
        pc.printf("X:%f, Y:%f, Z:%f\r\n", accel_data[0], accel_data[1], accel_data[2]);

        pc.printf("Mox sensor\r\n");
        mox.IAQmeasure();
        pc.printf("TVOC: %d, eCO2: %d\r\n", mox.TVOC, mox.eCO2);

        pc.printf("Dust sensor\r\n");
        float dust_val = read_dust_sensor();
        pc.printf("dust concentration: %4.1f ug/m3z\r\n", dust_val);
        pc.printf("\r\n");

        ThisThread::sleep_for(1000);
    }
}