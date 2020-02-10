package com.example.gassie;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.Toast;
import android.content.Context;
import android.bluetooth.BluetoothDevice;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Collection;
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


public class MainActivity extends AppCompatActivity {

    private BluetoothManager bluetoothManager;
    private String mac;
    private SimpleBluetoothDeviceInterface deviceInterface;
    private Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = getApplicationContext();

        FirebaseStorage storage = FirebaseStorage.getInstance("gs://dynamic-heading-267810-files-source-1581331589");
        StorageReference storageRef = storage.getReference();
        StorageReference dataRef = storageRef.child("data3.json");

        String jsonData = "{\"id\":\"4\",\"first_name\":\"May\",\"last_name\":\"Doe\",\"dob\":\"1968-01-22\"," +
                "\"addresses\":[{\"status\":\"current\",\"address\":\"123 First Avenue\",\"city\":\"Seattle\",\"" +
                "state\":\"WA\",\"zip\":\"11111\",\"numberOfYears\":\"1\"},{\"status\":\"previous\",\"address\":\"" +
                "456 Main Street\",\"city\":\"Portland\",\"state\":\"OR\",\"zip\":\"22222\",\"numberOfYears\":\"5\"" +
                "}]}\n";

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
    public void onDestroy() {
        // Disconnect one device
        bluetoothManager.closeDevice(mac); // Close by mac
        bluetoothManager.close();
        super.onDestroy();

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

    private void onMessageReceived(String message) {
        // We received a message! Handle it here.
        Toast.makeText(context, "Received a message! Message was: " + message, Toast.LENGTH_LONG).show(); // Replace context with your context instance.


    }

    private void onError(Throwable error) {
        // Handle the error
    }


}
