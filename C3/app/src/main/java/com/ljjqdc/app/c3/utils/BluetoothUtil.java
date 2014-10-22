package com.ljjqdc.app.c3.utils;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
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
    public static boolean HAS_BLUETOOTH = false;//"找不到蓝牙设备"
    public static boolean BLUETOOTH_OPEN = false;//"等待用户开启蓝牙设备"
    public static boolean SERVER_OPEN = false;//服务器端开启
    public static boolean CLIENT_CONNECT = false;//客户端连接成功

    private Context context;

    //Bluetooth
    private BluetoothAdapter bluetoothAdapter;
    //private Set<BluetoothDevice> bluetoothDevices;
    private BluetoothDevice bluetoothDevice;

    private ServerThread serverThread;
    private ClientThread clientThread;

    private BluetoothSocket bluetoothSocket;
    private BluetoothServerSocket bluetoothServerSocket;
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    /**
     * 初始化,做一次就好
     */
    public BluetoothUtil(Context con){
        context = con;

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(bluetoothAdapter==null){
            //这台机器上没有蓝牙设备
            return;
        }
        HAS_BLUETOOTH = true;

        if(!bluetoothAdapter.isEnabled()){
            //蓝牙设备没有开启
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
                                BLUETOOTH_OPEN = true;
                            }
                            dialogInterface.dismiss();
                        }
                    }).create().show();
        }

        //设备可以被检测到
        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION,3000);
        con.startActivity(intent);

    }

    /**
     * 检测到可连接的蓝牙设备
     */
    public void startSearch(){
        //bluetoothDevices = bluetoothAdapter.getBondedDevices();//得到最近配对的设备
        bluetoothAdapter.startDiscovery();
    }

    /**
     * 初始化服务器端
     */
    public void startServer(){
        serverThread = new ServerThread();
        serverThread.start();
    }

    /**
     * 关闭服务器端
     */
    public void finishServer(){
        if(serverThread!=null){
            serverThread.interrupt();
            serverThread = null;
        }
        if(bluetoothServerSocket!=null){
            try {
                bluetoothServerSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            bluetoothServerSocket = null;
        }
        if(bluetoothSocket!=null){
            try {
                bluetoothSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            bluetoothSocket = null;
        }
        SERVER_OPEN = false;
    }

    /**
     * 客户端连接
     */
    public void connectBluetoothDevice(BluetoothDevice device){
        bluetoothDevice = device;
        if(bluetoothDevice == null){
            return;
        }

        clientThread = new ClientThread();
        clientThread.start();
    }

    /**
     * 关闭客户端连接
     */
    public void finishClient(){
        if(clientThread!=null){
            clientThread.interrupt();
            clientThread = null;
        }
        if(bluetoothSocket!=null){
            try {
                bluetoothSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            bluetoothSocket = null;
        }
        CLIENT_CONNECT = false;
    }

    private class ServerThread extends Thread{
        @Override
        public void run(){
            try {
                bluetoothServerSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord("ljjserver",MY_UUID);
                bluetoothSocket = bluetoothServerSocket.accept();
                SERVER_OPEN = true;Log.i("ljjbluetooth","server open");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class ClientThread extends Thread{
        @Override
        public void run(){
            try {
                bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                e.printStackTrace();
            }
            bluetoothAdapter.cancelDiscovery();
            try {
                bluetoothSocket.connect();
                CLIENT_CONNECT = true;Log.i("ljjbluetooth","connect succeed");
            } catch (IOException e) {
                Log.i("ljjbluetooth","蓝牙连接失败!"+e.toString());
                e.printStackTrace();
                try {
                    bluetoothSocket.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }

    /**
     * 发送信息
     */
    public void sendMessage(String outputMessage){//outputMessage = "你想发送的东西";
        if(!bluetoothSocket.isConnected()){
            //logs = "蓝牙设备未连接，发送失败";
            return;
        }

        //利用 BluetoothSocket获取输出流进行输出
        try {
            OutputStream outputStream = bluetoothSocket.getOutputStream();
            outputStream.write(outputMessage.getBytes());
            //logs = "发送成功："+outputMessage;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
