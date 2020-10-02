package com.example.gassie;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;

import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Context;
import android.bluetooth.BluetoothDevice;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.lang.Math;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.harrysoft.androidbluetoothserial.*;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import androidx.annotation.NonNull;

import java.util.Date;
import java.text.SimpleDateFormat;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;


public class MainActivity extends AppCompatActivity
        implements AdapterView.OnClickListener {

    private BluetoothManager bluetoothManager;
    private String mac;
    private SimpleBluetoothDeviceInterface deviceInterface;
    private Context context;
    private GoogleSignInOptions gso;
    private GoogleSignInClient mGoogleSignInClient;
    private static final int RC_SIGN_IN = 9001;
    private static final String TAG = "MainActivity";
    private FirebaseAuth mAuth;
    private ArrayList<String> drop = new ArrayList<>();
    private Button mClickButton;
    private Spinner spinner;
    private Spinner spinner1;
    private Spinner spinner2;
    private TextInputLayout mTextView;
    private Boolean authenticated = false;

    private float co = 0;
    private float no2 = 0;
    private float nh3 = 0;
    private float ch4 = 0;
    private float h2 = 0;
    private float ethanol = 0;
    private float propane = 0;
    private float dust = 0;
    private int eco2 = 0;
    private int tvoc = 0;

    private int aqico = 0;
    private int aqino2 = 0;
    private int aqidust = 0;

    private int sensor_counter = 0;
    private int aqi_counter = 0;
    private int frequency = 18;

    private float[] aqi_breakpoints_co = new float[]{-0.1f, 4.4f, 9.4f, 12.4f, 15.4f, 30.4f, 40.4f, 50.4f};
    private int[] aqi_breakpoints_no2 = new int[]{-1, 53, 100, 360, 649, 1249, 1649, 2049};
    private float[] aqi_breakpoints_dust = new float[]{-0.1f, 12.0f, 35.4f, 55.4f, 150.4f, 250.4f, 350.4f, 500.4f};
    private int[] aqi_values = new int[]{-1, 50, 100, 150, 200, 300, 400, 500};
    private FusedLocationProviderClient fusedLocationClient;


    private LocationCallback locationCallback;
    private LocationRequest locationRequest;
    private boolean requestingLocationUpdates = false;
    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    private float longitude = 0.0f;
    private float latitude = 0.0f;

    private LineChart mChart;
    private String description = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mClickButton = findViewById(R.id.button);
        spinner = findViewById(R.id.spinner);
        spinner1 = findViewById(R.id.spinner3);
        spinner2 = findViewById(R.id.spinner2);
        mTextView = findViewById(R.id.textInputLayout);

        context = getApplicationContext();

        gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("")
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        mAuth = FirebaseAuth.getInstance();
        signIn();

        bluetoothManager = BluetoothManager.getInstance();
        if (bluetoothManager == null) {
            // Bluetooth unavailable on this device :( tell the user
            Toast.makeText(context, "Bluetooth not available.", Toast.LENGTH_LONG).show(); // Replace context with your context instance.
            finish();
        }

        boolean found = false;
        Collection<BluetoothDevice> pairedDevices = bluetoothManager.getPairedDevicesList();
        for (BluetoothDevice device : pairedDevices) {
            Log.d("My Bluetooth App", "Device name: " + device.getName());
            Log.d("My Bluetooth App", "Device MAC Address: " + device.getAddress());

            if (device.getName().equals("HC-06")) {
                mac = device.getAddress();
                found = true;
            }
        }

        if (!found) {
            Log.d("Error:", "Device not found");
            finish();
        }

        connectDevice(mac);


        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.appleton_floors, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        spinner.setAdapter(adapter);

        for (int x = 0; x < adapter.getCount(); x++) {
            String z = (String) adapter.getItem(x);
            drop.add(z);
        }

        spinner1.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                if (spinner1.getSelectedItem().toString().equals("Stationary mode")){
                    frequency = 18;
                    System.out.println("Frequency set to 18");
                }
                else{
                    frequency = 3;
                    System.out.println("Frequency set to 3");
                }
                sensor_counter = 0;
                aqi_counter = 0;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                frequency = 18;
            }

        });

        spinner2.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                description = spinner2.getSelectedItem().toString();
                createGraph();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_REQUEST_LOCATION);
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        // Got last known location. In some rare situations this can be null.
                        if (location != null) {
                            latitude = (float) location.getLatitude();
                            longitude = (float) location.getLongitude();
                        }
                    }
                });

        createLocationRequest();


        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                Location location = locationResult.getLastLocation();
                latitude = (float)location.getLatitude();
                longitude = (float)location.getLongitude();
            };
        };


        mClickButton.setOnClickListener(this);

        mChart = findViewById(R.id.chart1);

        // enable description text
        mChart.getDescription().setEnabled(true);
        mChart.getDescription().setText("Not tracking");

        // enable touch gestures
        mChart.setTouchEnabled(true);

        // enable scaling and dragging
        mChart.setDragEnabled(false);
        mChart.setScaleEnabled(false);
        mChart.setDrawGridBackground(false);

        // if disabled, scaling can be done on x- and y-axis separately
        mChart.setPinchZoom(false);

        // set an alternative background color
        mChart.setBackgroundColor(Color.WHITE);

        LineData data = new LineData();
        data.setValueTextColor(Color.WHITE);

        // add empty data
        mChart.setData(data);


        // get the legend (only possible after setting data)
        Legend l = mChart.getLegend();

        // modify the legend ...
        l.setForm(Legend.LegendForm.LINE);
        l.setTextColor(Color.BLACK);

        XAxis xl = mChart.getXAxis();
        xl.setTextColor(Color.WHITE);
        xl.setDrawGridLines(true);
        xl.setAvoidFirstLastClipping(true);
        xl.setEnabled(true);
        xl.setPosition(XAxis.XAxisPosition.BOTTOM);

        YAxis leftAxis = mChart.getAxisLeft();
        leftAxis.setTextColor(Color.BLACK);
        leftAxis.setDrawGridLines(false);
        leftAxis.setAxisMaximum(50f);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setDrawGridLines(true);

        YAxis rightAxis = mChart.getAxisRight();
        rightAxis.setEnabled(false);

        mChart.getAxisLeft().setDrawGridLines(false);
        mChart.getXAxis().setDrawGridLines(false);
        mChart.setDrawBorders(false);

        mChart.invalidate();

    }
    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
    }


    @Override
    public void onDestroy() {
        // Disconnect one device
        bluetoothManager.closeDevice(mac); // Close by mac
        bluetoothManager.close();
        super.onDestroy();
        FirebaseAuth.getInstance().signOut();

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (requestingLocationUpdates) {
            startLocationUpdates();
        }
    }

    private void startLocationUpdates() {
        fusedLocationClient.requestLocationUpdates(locationRequest,
                locationCallback,
                Looper.getMainLooper());
    }

    private void connectDevice(String mac) {
        bluetoothManager.openSerialDevice(mac)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onConnected, this::onError);
    }

    private void onConnected(BluetoothSerialDevice connectedDevice) {
        // You are now connected to this device!
        // Here you may want to retain an instance to your device:
        Log.d("Note:", "Connected to device.");
        deviceInterface = connectedDevice.toSimpleDeviceInterface();

        // Listen to bluetooth events
        deviceInterface.setListeners(this::onMessageReceived, this::onMessageSent, this::onError);

        // Let's send a message:
        //deviceInterface.sendMessage("Hello world!");
    }

    private void onMessageSent(String message) {
        // We sent a message! Handle it here.
        Toast.makeText(context, "Sent a message! Message was: " + message, Toast.LENGTH_LONG).show(); // Replace context with your context instance.
    }




    private void printSentData(String[] values){
         // DEBUG
         for(int i = 0; i < values.length; i+=2){
            System.out.format("%s %s\n", values[i], values[i+1]);
         }
    }

    private void onMessageReceived(String message) {
        // We received a message! Handle it here.
        Toast.makeText(context, "Received a message! Message was: " + message, Toast.LENGTH_LONG).show(); // Replace context with your context instance.1

        // split data
        String[] values = message.split(" ");

        printSentData(values);

        co += Float.parseFloat(values[1]);
        no2 += Float.parseFloat(values[3]);
        nh3 += Float.parseFloat(values[5]);
        ch4 += Float.parseFloat(values[7]);
        h2 += Float.parseFloat(values[9]);
        ethanol += Float.parseFloat(values[11]);
        propane += Float.parseFloat(values[13]);
        dust +=  Float.parseFloat(values[15]);
        eco2 += Float.parseFloat(values[17]);
        tvoc += Float.parseFloat(values[19]);

        sensor_counter++;

        if (sensor_counter == frequency){
            sensor_counter = 0;
            sendToSensorReadings();
        }
        switch (description){
            case "CO": addEntry(Float.parseFloat(values[1])); break;
            case "NO2": addEntry(Float.parseFloat(values[3])); break;
            case "NH3": addEntry(Float.parseFloat(values[5])); break;
            case "CH4": addEntry(Float.parseFloat(values[7])); break;
            case "H2": addEntry(Float.parseFloat(values[9])); break;
            case "Ethanol": addEntry(Float.parseFloat(values[11])); break;
            case "Propane": addEntry(Float.parseFloat(values[13])); break;
            case "Dust": addEntry(Float.parseFloat(values[15])); break;
            case "eCO2": addEntry(Float.parseFloat(values[17])); break;
            case "TVOC": addEntry(Float.parseFloat(values[19])); break;
            default:
        }


    }

    private void sendToSensorReadings(){
        //json schema
        String json_schema = "{\"CO\": %s,\"NO2\": %s,\"NH3\": %s,\"CH4\": %s,\"H2\": %s,\"ETHANOL\": %s,\"PROPANE\": %s,\"DUST\": %s,\"eCO2\": %s,\"TVOC\": %s,\"TIME\": \"%s\", \"Location_tag\": \"%s\"}";

        // get timestamp
        String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        System.out.println("Time: " + timeStamp);

        // get location tag
        String location_tag = spinner.getSelectedItem().toString();
        location_tag = location_tag.replaceAll(" ", "-");

        co /= frequency; co = Math.round(co * 100.0f) / 100.0f;
        no2 /= frequency; no2 *= 1000; no2 = Math.round(no2 * 100.0f) / 100.0f;
        nh3 /= frequency; nh3 = Math.round(nh3 * 100.0f) / 100.0f;
        ch4 /= frequency; ch4 = Math.round(ch4 * 100.0f) / 100.0f;
        h2 /= frequency; h2 = Math.round(h2 * 100.0f) / 100.0f;
        ethanol /= frequency; ethanol = Math.round(ethanol * 100.0f) / 100.0f;
        propane /= frequency; propane = Math.round(propane * 100.0f) / 100.0f;
        dust /= frequency; dust = Math.round(dust * 100.0f) / 100.0f;
        eco2 /= frequency;
        tvoc /= frequency;

        int index = 7;

        for (int x = 1; x < aqi_breakpoints_co.length; x++){
            if(co <= aqi_breakpoints_co[x]){
                index = x;
                break;
            }
        }

        int tempaqi = 0;
        float breakpointf_lo = aqi_breakpoints_co[index-1] + 0.1f;
        float breakpointf_hi = aqi_breakpoints_co[index];
        int aqi_lo = aqi_values[index-1] + 1;
        int aqi_hi = aqi_values[index];
        //System.out.println("AQI co values: " + breakpointf_lo + " " + breakpointf_hi + " " + aqi_lo + " " + aqi_hi + " " + co);
        tempaqi = Math.round((aqi_hi - aqi_lo) / (breakpointf_hi-breakpointf_lo) * ((Math.round(co*10.0f) / 10.0f) - breakpointf_lo) + aqi_lo);
        if(description.equals("AQI: CO")) addEntry(tempaqi);
        aqico += tempaqi;
        //System.out.println("AQI co: " + aqico);
        index = 7;

        for (int x = 1; x < aqi_breakpoints_dust.length; x++){
            if(dust <= aqi_breakpoints_dust[x]){
                index = x;
                break;
            }
        }

        breakpointf_lo = aqi_breakpoints_dust[index-1] + 0.1f;
        breakpointf_hi = aqi_breakpoints_dust[index];
        aqi_lo = aqi_values[index-1] + 1;
        aqi_hi = aqi_values[index];
        //System.out.println("AQI dust values: " + breakpointf_lo + " " + breakpointf_hi + " " + aqi_lo + " " + aqi_hi + " " + dust);

        tempaqi = Math.round((aqi_hi - aqi_lo) / (breakpointf_hi-breakpointf_lo) * ((Math.round(dust*10.0f) / 10.0f) - breakpointf_lo) + aqi_lo);
        if(description.equals("AQI: Dust")) addEntry(tempaqi);
        aqidust += tempaqi;
        //System.out.println("AQI dust: " + aqidust);

        index = 7;
        for (int x = 1; x < aqi_breakpoints_no2.length; x++){
            if(no2 <= aqi_breakpoints_no2[x]){
                index = x;
                break;
            }
        }

        breakpointf_lo = (float)(aqi_breakpoints_no2[index-1] + 1);
        breakpointf_hi = (float)(aqi_breakpoints_no2[index]);
        aqi_lo = aqi_values[index-1] + 1;
        aqi_hi = aqi_values[index];
        //System.out.println("AQI no2 values: " + breakpointf_lo + " " + breakpointf_hi + " " + aqi_lo + " " + aqi_hi + " " + no2);

        tempaqi = Math.round((aqi_hi - aqi_lo) / (breakpointf_hi-breakpointf_lo) * (Math.round(no2) - breakpointf_lo) + aqi_lo);
        if(description.equals("AQI: NO2")) addEntry(tempaqi);
        aqino2 += tempaqi;
        //System.out.println("AQI no2: " + aqino2);


        String json_data = String.format(json_schema, co, no2, nh3, ch4, h2, ethanol, propane, dust, eco2, tvoc, timeStamp, location_tag);
        System.out.println("Schema Readings: " + json_data);
        if (authenticated){
            sendDataToBigQuery(json_data, timeStamp, "gs://gassie-files-source-1581353521/", "sensor");
        }

        aqi_counter++;

        if (aqi_counter == 3){
            aqi_counter = 0;
            sendToAQIReadings();
        }



        co = 0;
        no2 = 0;
        nh3 = 0;
        ch4 = 0;
        h2 = 0;
        ethanol = 0;
        propane = 0;
        dust = 0;
        eco2 = 0;
        tvoc = 0;

    }

    private void sendToAQIReadings(){
        // Set json schema
        String json_schema = "{\"AQI\": %s,\"Location_tag\": \"%s\", \"AQIco\": %s,\"AQIno2\": %s,\"AQIdust\": %s,\"TIME\": \"%s\", \"longitude\": %s,\"latitude\": %s, \"AQIcategory\": \"%s\"}";

        // get timestamp
        String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        System.out.println("Time: " + timeStamp);

        // get location tag
        String location_tag = spinner.getSelectedItem().toString();
        location_tag = location_tag.replaceAll(" ", "-");

        aqico /= 3;
        aqidust /= 3;
        aqino2 /= 3;

        int aqi = Integer.max(aqico, aqidust);
        aqi = Integer.max(aqi, aqino2);
        String AQIcategory = "";
        if (aqi <= 50) AQIcategory = "Good";
        else if (aqi <= 100) AQIcategory = "Moderate";
        else if (aqi <= 150) AQIcategory = "Unhealthy for Sensitive Groups";
        else if (aqi <= 200) AQIcategory = "Unhealthy";
        else if (aqi <= 300) AQIcategory = "Very Unhealthy";
        else AQIcategory = "Hazardous";

        String json_data = String.format(json_schema, aqi, location_tag, aqico, aqino2, aqidust, timeStamp, longitude, latitude, AQIcategory);
        System.out.println("Schema AQI: " + json_data);
        if (authenticated){
            sendDataToBigQuery(json_data, timeStamp, "gs://gassie-files-source2", "AQI");
        }

        aqi_counter = 0;

        aqico = 0;
        aqidust = 0;
        aqino2 = 0;
    }

    private void onError(Throwable error) {
        // Handle the error
    }

    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        Log.w(TAG, "request code:" + requestCode);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                // Google Sign In failed, update UI appropriately
                Log.w(TAG, "Google sign in failed", e);
            }
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        Log.d(TAG, "firebaseAuthWithGoogle:" + acct.getId());

        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithCredential:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            authenticated = true;
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                        }
                    }
                });
    }

    private void sendDataToBigQuery(String jsonData, String timestamp, String bucket, String type){
        FirebaseStorage storage = FirebaseStorage.getInstance(bucket);
        StorageReference storageRef = storage.getReference();

        // may fail due to spaces in file name, so remove spaces
        StorageReference dataRef = storageRef.child("data-" + timestamp + "-" + type + ".json");


        InputStream stream = new ByteArrayInputStream(jsonData.getBytes());

        UploadTask uploadTask = dataRef.putStream(stream);

        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {

            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

            }
        });
    }


    public void onClick(View v) {

        switch (v.getId()){

            case R.id.button:
                String loc = mTextView.getEditText().getText().toString();
                mTextView.clearFocus();
                drop.add(loc);

                ArrayAdapter<String> newAdapter = new ArrayAdapter<String> (this, android.R.layout.simple_spinner_item, drop);

                newAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinner.setAdapter(newAdapter);

                mTextView.getEditText().setText("");
                break;

            default:
                break;
        }

    }


    protected void createLocationRequest() {
        locationRequest = LocationRequest.create();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        requestingLocationUpdates = true;
    }

    private void addEntry(float mValue) {

        LineData data = mChart.getData();

        if (data != null) {

            ILineDataSet set = data.getDataSetByIndex(0);
            // set.addEntry(...); // can be called as well

            if (set == null) {
                set = createSet();
                data.addDataSet(set);
            }

            data.addEntry(new Entry(set.getEntryCount(), mValue), 0);
            data.notifyDataChanged();

            // let the chart know it's data has changed
            mChart.notifyDataSetChanged();

            // limit the number of visible entries
            mChart.setVisibleXRangeMaximum(10);
            // mChart.setVisibleYRange(30, AxisDependency.LEFT);

            // move to the latest entry
            mChart.moveViewToX(data.getEntryCount());

        }
    }

    private LineDataSet createSet() {

        String unit = "";
        String[] isAQI = description.split(" ");

        if (description.equals("TVOC")) unit = " (ppb)";
        else if (description.equals("Dust")) unit = " (μg/m3)";
        else if (isAQI.equals("AQI:")) unit = "";
        else unit = " (ppm)";

        LineDataSet set = new LineDataSet(null, description + unit);
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setLineWidth(3f);
        set.setColor(Color.MAGENTA);
        set.setHighlightEnabled(false);
        set.setDrawValues(true);
        set.setDrawCircles(false);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setCubicIntensity(0.2f);
        return set;
    }

    private void createGraph() {
        if (description.equals("None")) mChart.getDescription().setText("Not tracking");
        else mChart.getDescription().setText(description + " Values");
        LineData data = new LineData();
        data.setValueTextColor(Color.WHITE);

        // add empty data
        mChart.setData(data);

        if (!description.equals("None")){
            YAxis leftAxis = mChart.getAxisLeft();
            String[] isAqi = description.split(" ");

            if (description.equals("Propane")) leftAxis.setAxisMaximum(5000f);
            else if (description.equals("eCO2")) leftAxis.setAxisMaximum(800f);
            else if (description.equals("Dust")) leftAxis.setAxisMaximum(300f);
            else if (isAqi[0].equals("AQI:")) leftAxis.setAxisMaximum(600f);
            else {
                leftAxis.setAxisMaximum(5f);
            }
        }



        mChart.invalidate();
    }


}
