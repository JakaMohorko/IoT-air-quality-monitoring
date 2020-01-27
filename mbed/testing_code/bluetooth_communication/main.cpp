#include "mbed.h"
#include "MiCS6814_GasSensor.h"

#define MAIN_SLEEP_MS                       1000

DigitalOut myled(LED1);

RawSerial pc(USBTX, USBRX, 9600);
// Instantiate connection for bluetooth communication

// D1: TX 
// DO: RX 
RawSerial blue(D1, D0, 9600);


MiCS6814_GasSensor gas_sensor(I2C_SDA, I2C_SCL);

/*
 * recieves data from device and prints to screen
*/
void callback_ex() {
    // Note: you need to actually read from the serial to clear the RX interrupt
    pc.putc(blue.getc());
    myled = !myled;
}

int main(){

    blue.attach(&callback_ex);

    while(1) {
        ThisThread::sleep_for(MAIN_SLEEP_MS);

        // Send data to bluetooth module
        // pc.printf("to term\n\r");
    

        // readable does not work
        // if(blue.readable()){
        //     char c = blue.getc();
        //     pc.putc(c);
        // }else{
        //     blue.printf("No read\n");
        // }

		// Receive data from bluetooth module
        float co2_gas = gas_sensor.getGas(CO);
        blue.printf("%f\n",co2_gas);
    }
}
