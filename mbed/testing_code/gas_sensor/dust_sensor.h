#ifndef DUST_GUARD
#define DUST_GUARD

#include "mbed.h"

#define        COV_RATIO                       0.2            //ug/mmm / mv
#define        NO_DUST_VOLTAGE                 500            //mv   

/*
variable
*/
float density, voltage;

void setup(void);
// int _filter(int); 

// == setup pins ==
DigitalOut myled(LED1), iled_m(D7);
AnalogIn vout_m(A0);
//

void dust_setup(void){
//   pinMode(iled, OUTPUT);
//   digitalWrite(iled, LOW);                                     //iled default closed  
  iled_m = 0;
}

static int _filter(int m)
{
  static int flag_first = 0, _buff[10], sum;
  const int _buff_max = 10;
  int i;
  
  if(flag_first == 0)
  {
    flag_first = 1;

    for(i = 0, sum = 0; i < _buff_max; i++)
    {
      _buff[i] = m;
      sum += _buff[i];
    }
    return m;
  }
  else
  {
    sum -= _buff[0];
    for(i = 0; i < (_buff_max - 1); i++)
    {
      _buff[i] = _buff[i + 1];
    }
    _buff[9] = m;
    sum += _buff[9];
    
    i = sum / 10.0;
    return i;
  }
} 

float read_dust_sensor(void)
{
   iled_m = 1;
        wait_us(280);
		voltage = vout_m.read() * 3300 * 11;
		iled_m = 0;
		
		voltage = _filter(voltage);
		
		if(voltage >= NO_DUST_VOLTAGE)
		{
			voltage -= NO_DUST_VOLTAGE;
			
			density = voltage * COV_RATIO;
		}
		else
			density = 0;
	
    myled = !myled;
		wait(1);
    return density;
}


#endif 