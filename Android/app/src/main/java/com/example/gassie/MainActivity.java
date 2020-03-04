package com.example.gassie;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
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

    private float[] aqi_breakpoints_co = new float[]{-0.1f, 4.4f, 9.4f, 12.4f, 15.4f, 30.4f, 40.4f, 50.4f};
    private int[] aqi_breakpoints_no2 = new int[]{-1, 53, 100, 360, 649, 1249, 1649, 2049};
    private float[] aqi_breakpoints_dust = new float[]{-0.1f, 12.0f, 35.4f, 55.4f, 150.4f, 250.4f, 350.4f, 500.4f};
    private int[] aqi_values = new int[]{-1, 50, 100, 150, 200, 300, 400, 500};

    private LocationManager mLocationManager;
    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    private String longitude = "";
    private String latitude = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mClickButton = findViewById(R.id.button);
        spinner = findViewById(R.id.spinner);
        mTextView = findViewById(R.id.textInputLayout);

        context = getApplicationContext();

        gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("***REMOVED***")
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

        mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_REQUEST_LOCATION);
        }
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10 * 1000, 0, mLocationListener);


        mClickButton.setOnClickListener(this);
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

        if (sensor_counter == 12){
            sendToSensorReadings();
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

        co /= 12; co = Math.round(co * 100.0f) / 100.0f;
        no2 /= 12; no2 *= 1000; no2 = Math.round(no2 * 100.0f) / 100.0f;
        nh3 /= 12; nh3 = Math.round(nh3 * 100.0f) / 100.0f;
        ch4 /= 12; ch4 = Math.round(ch4 * 100.0f) / 100.0f;
        h2 /= 12; h2 = Math.round(h2 * 100.0f) / 100.0f;
        ethanol /= 12; ethanol = Math.round(ethanol * 100.0f) / 100.0f;
        propane /= 12; propane = Math.round(propane * 100.0f) / 100.0f;
        dust /= 12; dust = Math.round(dust * 100.0f) / 100.0f;
        eco2 /= 12;
        tvoc /= 12;

        int index = 7;

        for (int x = 1; x <= aqi_breakpoints_co.length; x++){
            if(co <= aqi_breakpoints_co[x]){
                index = x;
                break;
            }
        }

        float breakpointf_lo = aqi_breakpoints_co[index-1] + 0.1f;
        float breakpointf_hi = aqi_breakpoints_co[index];
        int aqi_lo = aqi_values[index-1] + 1;
        int aqi_hi = aqi_values[index];

        aqico += Math.round((aqi_hi - aqi_lo) / (breakpointf_hi-breakpointf_lo) * ((Math.round(co*10.0f) / 10.0f) - breakpointf_lo) + aqi_lo);

        index = 7;

        for (int x = 1; x <= aqi_breakpoints_dust.length; x++){
            if(co <= aqi_breakpoints_dust[x]){
                index = x;
                break;
            }
        }

        breakpointf_lo = aqi_breakpoints_co[index-1] + 0.1f;
        breakpointf_hi = aqi_breakpoints_co[index];
        aqi_lo = aqi_values[index-1] + 1;
        aqi_hi = aqi_values[index];

        aqidust += Math.round((aqi_hi - aqi_lo) / (breakpointf_hi-breakpointf_lo) * ((Math.round(co*10.0f) / 10.0f) - breakpointf_lo) + aqi_lo);

        index = 7;
        for (int x = 1; x <= aqi_breakpoints_no2.length; x++){
            if(co <= aqi_breakpoints_no2[x]){
                index = x;
                break;
            }
        }

        int breakpoint_lo = aqi_breakpoints_no2[index-1] + 1;
        int breakpoint_hi = aqi_breakpoints_no2[index];
        aqi_lo = aqi_values[index-1] + 1;
        aqi_hi = aqi_values[index];

        aqino2 += Math.round((aqi_hi - aqi_lo) / (breakpoint_hi-breakpoint_lo) * ((Math.round(co*10.0f) / 10.0f) - breakpoint_lo) + aqi_lo);

        sensor_counter = 0;

        String json_data = String.format(json_schema, co, no2, nh3, ch4, h2, ethanol, propane, dust, eco2, tvoc, timeStamp, location_tag);
        System.out.println("Schema: " + json_data);
        if (authenticated){
            sendDataToBigQuery(json_data, timeStamp, "gs://gassie-files-source-1581353521/");
        }

        aqi_counter++;
        sendToAQIReadings();


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
        String json_schema = "{\"AQI\": %s,\"Location_tag\": \"%s\", \"AQIco\": %s,\"AQIno2\": %s,\"AQIdust\": %s,\"TIME\": \"%s\", \"longitude\": %s,\"latitude\": %s}";

        // get timestamp
        String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        System.out.println("Time: " + timeStamp);

        // get location tag
        String location_tag = spinner.getSelectedItem().toString();
        location_tag = location_tag.replaceAll(" ", "-");

        aqico /= 5;
        aqidust /= 5;
        aqino2 /= 5;

        int aqi = Integer.max(aqico, aqidust);
        aqi = Integer.max(aqi, aqino2);

        String json_data = String.format(json_schema, aqi, location_tag, aqico, aqino2, aqidust, timeStamp, longitude, latitude);
        System.out.println("Schema: " + json_data);
        if (authenticated){
            sendDataToBigQuery(json_data, timeStamp, "gs://gassie-files-source2");
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

    private void sendDataToBigQuery(String jsonData, String timestamp, String bucket){
        FirebaseStorage storage = FirebaseStorage.getInstance(bucket);
        StorageReference storageRef = storage.getReference();

        // may fail due to spaces in file name
        StorageReference dataRef = storageRef.child("data" + timestamp + ".json");


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
        String loc = mTextView.getEditText().getText().toString();

        drop.add(loc);

        ArrayAdapter<String> newAdapter = new ArrayAdapter<String> (this, android.R.layout.simple_spinner_item, drop);

        newAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(newAdapter);

        mTextView.getEditText().setText("");
    }

    private final LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(final Location location) {
            longitude = Double.toString(location.getLongitude());
            latitude = Double.toString(location.getLatitude());
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onProviderDisabled(String provider) {
        }
    };


}
