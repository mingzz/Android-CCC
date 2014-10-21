package com.ljjqdc.app.c3;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.helloanychat.ChatMainActivity;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;


public class MainActivity extends Activity {

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private BluetoothDevice bluetoothDevice;

    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private String outputMessage = "你想发送的东西";

    //voice recognizer
    private Button buttonVoiceRecognizer;
    private TextView textViewVoiceRecognizer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //initBlueTooth();

        Button button = (Button)findViewById(R.id.btn_test);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, ChatMainActivity.class);
                startActivity(intent);
            }
        });

        initVoiceRecognizer();
    }

    private void initBlueTooth(){
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(bluetoothAdapter==null || !bluetoothAdapter.isEnabled()){
            finish();
            return;
        }

        Set<BluetoothDevice> devices = bluetoothAdapter.getBondedDevices();
        BluetoothDevice device = null;
        for(int i=0;i<devices.size();i++){
            device = (BluetoothDevice)devices.toArray()[i];
            Log.i("device_name",device.getName());//device_name﹕ Lenovo A600e
            if(device.getName().equals("Lenovo A600e")){
                bluetoothDevice = device;
                break;
            }
        }

        if(bluetoothDevice == null){
            finish();
            return;
        }

        try {
            bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(MY_UUID);
        } catch (IOException e) {
            e.printStackTrace();
        }
        bluetoothAdapter.cancelDiscovery();
        try {
            bluetoothSocket.connect();
        } catch (IOException e) {
            e.printStackTrace();
            try {
                bluetoothSocket.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }

        //利用 BluetoothSocket获取输出流进行输出
        try {
            OutputStream outputStream = bluetoothSocket.getOutputStream();
            outputStream.write(outputMessage.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void initVoiceRecognizer(){
        buttonVoiceRecognizer = (Button)findViewById(R.id.btn_voice_recognizer);
        textViewVoiceRecognizer = (TextView)findViewById(R.id.tv_voice_recognizer);

        buttonVoiceRecognizer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //try{
                    Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                    intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                    intent.putExtra(RecognizerIntent.EXTRA_PROMPT,"kaishi");
                    startActivityForResult(intent,0);
                //}catch (Exception e){
                //    e.printStackTrace();
                //    Toast.makeText(MainActivity.this,"cuole",Toast.LENGTH_SHORT).show();
                //}
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        if(requestCode==0&&resultCode==RESULT_OK){
            ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            String resultString = "";
            for(int i=0;i<results.size();++i){
                resultString+=results.get(i);
            }
            textViewVoiceRecognizer.setText(resultString);
        }
    }
}
