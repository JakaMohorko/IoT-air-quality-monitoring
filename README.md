# IoT Air Quality Monitoring solution

University project IoT Air Quality Monitoring solution, utilising particle and gas sensors connected to a FRDM-K64F board. The sensor data is transferred to an Android device running our app via bluetooth, uploaded to Google BigQuery through Firebase and visualized in Google DataStudio and on Google Maps. A subscription service designed to notify users of bad air quality is available.

## Sensors

We make use of the Grove Multichannel Gas Sensor, the Adafruit SGP30 Gas Sensor and the Waveshare Dust Sensor. These are polled for data on gasses relevant to AQI calculations. Mbed libraries are used to interface with the sensors through a FRDM-K64F board.

## Android App

The Android App serves as a bridge between the Google Cloud and the development board. Data from the board is sent via Bluetooth, using the android-bluetooth-serial library by harry1453. The app calculates the AQI values, displays a graph of the current readings and sends the time- and location-tagged data to Google Cloud through Firebase, requiring user authentication.

## Visualization

Data in Google Cloud is stored in BigQuery and visualized in DataStudio. We use Google Maps to visualize the data on an interactive map, which currently does not allow non-developer access due to Google privacy restrictions.

## Notification System

To alert users we have created a email-notification system allowingusers to subscribe to the email list to be notified when the AQIseverity level of our developed air quality monitoring system ex-ceeds a chosen level

## Project Report

The full description and report on the project is available in the report.pdf file.
