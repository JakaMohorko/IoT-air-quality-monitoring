package com.example.gassie;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import android.content.Context;
import android.bluetooth.BluetoothDevice;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Collection;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.harrysoft.androidbluetoothserial.*;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import com.google.firebase.FirebaseApp;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.ListResult;
import com.google.firebase.storage.OnPausedListener;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageException;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import androidx.annotation.NonNull;

import java.util.Date;
import java.text.SimpleDateFormat;

public class MainActivity extends AppCompatActivity {

    private BluetoothManager bluetoothManager;
    private String mac;
    private SimpleBluetoothDeviceInterface deviceInterface;
    private Context context;
    private GoogleSignInOptions gso;
    private GoogleSignInClient mGoogleSignInClient;
    private static final int RC_SIGN_IN = 9001;
    private static final String TAG = "MainActivity";
    private FirebaseAuth mAuth;
    private Boolean authenticated = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
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

        Collection<BluetoothDevice> pairedDevices = bluetoothManager.getPairedDevicesList();
        for (BluetoothDevice device : pairedDevices) {
            Log.d("My Bluetooth App", "Device name: " + device.getName());
            Log.d("My Bluetooth App", "Device MAC Address: " + device.getAddress());

            if (device.getName().equals("HC-06")){
                mac = device.getAddress();
            }
        }

        if (mac.equals("")){
            Log.d("Error:", "Device not found");
            finish();
        }

        connectDevice(mac);

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

        //json schema
        String json_schema = "{\"CO\": %s,\"NO2\": %s,\"NH3\": %s,\"CH4\": %s,\"H2\": %s,\"ETHANOL\": %s,\"PROPANE\": %s,\"DUST\": %s,\"eCO2\": %s,\"TVOC\": %s,\"TIME\": %s, \"Location_tag\": %s}";

        // get timestamp
        String timeStamp = new SimpleDateFormat("yyyy-MM-DD HH:mm:ss").format(new Date());
        System.out.println("Time: " + timeStamp);

        // get location tag

        // get geolocation data

        String json_data = String.format(json_schema, values[1], values[3], values[5], values[7], values[9], values[11], values[13], values[15], values[17], values[19], "0001-01-01 00:00:00", "AT-1");
        if (authenticated){
            sendDataToBigQuery(json_data, timeStamp);
        }

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
        System.out.println("request code: " + requestCode);
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
                          //  Snackbar.make(findViewById(R.id.main_layout), "Authentication Failed.", Snackbar.LENGTH_SHORT).show();

                        }

                        // ...
                    }
                });
    }

    private void sendDataToBigQuery(String jsonData, String timestamp){
        FirebaseStorage storage = FirebaseStorage.getInstance("gs://gassie-files-source-1581353521/");
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
}
