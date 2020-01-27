#include "mbed.h"

int main(){
    
   RawSerial pc(USBTX, USBRX, 9600);

   pc.printf("Hello world!");

}
