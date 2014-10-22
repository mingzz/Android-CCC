package com.ljjqdc.app.c3.utils;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

/**
 * Created by jingjing on 2014/10/22.
 */
public class BluetoothUtil {
    private Context context;

    //Bluetooth
    private BluetoothAdapter bluetoothAdapter;
    //private Set<BluetoothDevice> bluetoothDevices;
    private BluetoothDevice bluetoothDevice;

    private BluetoothSocket bluetoothSocket;
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private String logs = "";

    public BluetoothUtil(Context con){
        context = con;

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(bluetoothAdapter==null){
            //这台机器上没有蓝牙设备
            logs = "找不到蓝牙设备";
            return;
        }
        if(!bluetoothAdapter.isEnabled()){
            //蓝牙设备没有开启
            logs = "等待用户开启蓝牙设备";
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            new AlertDialog.Builder(con).setTitle("是否开启蓝牙？")
                    .setNegativeButton("取消",new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                        }
                    })
                    .setPositiveButton("开启",new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            bluetoothAdapter.enable();
                            if(bluetoothAdapter.isEnabled()){
                                logs = "蓝牙已打开";
                            }
                            dialogInterface.dismiss();
                        }
                    }).create().show();
        }

        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION,3000);
        con.startActivity(intent);

        //检测到可连接的蓝牙设备
        //bluetoothDevices = bluetoothAdapter.getBondedDevices();
        bluetoothAdapter.startDiscovery();
    }

    public String getLogs(){
        return logs;
    }

    public void connectBluetoothDevice(BluetoothDevice device){
        bluetoothDevice = device;
        if(bluetoothDevice == null){
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
            logs = "蓝牙连接成功!";
        } catch (IOException e) {
            logs = "蓝牙连接失败!";Log.i("ljjbluetooth",e.toString());
            e.printStackTrace();
            try {
                bluetoothSocket.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }

    public void sendMessage(String outputMessage){//outputMessage = "你想发送的东西";
        if(!bluetoothSocket.isConnected()){
            logs = "蓝牙设备未连接，发送失败";
            return;
        }

        //利用 BluetoothSocket获取输出流进行输出
        try {
            OutputStream outputStream = bluetoothSocket.getOutputStream();
            outputStream.write(outputMessage.getBytes());
            logs = "发送成功："+outputMessage;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
