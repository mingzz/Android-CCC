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
import android.os.Message;
import android.util.Log;
import android.widget.EditText;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

/**
 * Created by jingjing on 2014/10/22.
 */
public class BluetoothUtil {
    private static BluetoothUtil bluetoothUtil;
    public static void init(Context con){
        bluetoothUtil = new BluetoothUtil(con);
    }
    public static BluetoothUtil getInstance(){
        return bluetoothUtil;
    }

    public static boolean HAS_BLUETOOTH = false;//"找不到蓝牙设备"
    public static boolean BLUETOOTH_OPEN = false;//"等待用户开启蓝牙设备"
    public static boolean SERVER_OPEN = false;//服务器端开启
    public static boolean CLIENT_CONNECT = false;//客户端连接成功

    public static final String ACTION_SERVER_OPEN = "com.ljjqdc.app.c3.utils.BluetoothUtil.server_open";
    public static final String ACTION_CLIENT_OPEN = "com.ljjqdc.app.c3.utils.BluetoothUtil.client_open";
    public static final String ACTION_CLIENT_ERROR = "com.ljjqdc.app.c3.utils.BluetoothUtil.client_error";
    public static final String ACTION_RECEIVE_MESSAGE = "com.ljjqdc.app.c3.utils.BluetoothUtil.receive_message";

    private Context context;

    //Bluetooth
    private BluetoothAdapter bluetoothAdapter;
    //private Set<BluetoothDevice> bluetoothDevices;
    private BluetoothDevice bluetoothDevice;

    private ServerThread serverThread;
    private ClientThread clientThread;
    private ReadThread readThread;

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
        //设备可以被检测到
        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION,3000);
        context.startActivity(intent);

        serverThread = new ServerThread();
        serverThread.start();
    }

    /**
     * 关闭服务器端
     */
    public void finishServer(){
        SERVER_OPEN = false;
        if(serverThread!=null){
            serverThread.interrupt();
            serverThread = null;
        }
        if(readThread!=null){
            readThread.interrupt();
            readThread = null;
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
        CLIENT_CONNECT = false;
        if(clientThread!=null){
            clientThread.interrupt();
            clientThread = null;
        }
        if(readThread!=null){
            readThread.interrupt();
            readThread = null;
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
                bluetoothServerSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord("ljjserver",MY_UUID);Log.i("ljjbluetooth","server open");
                Intent intent = new Intent();
                intent.setAction(ACTION_SERVER_OPEN);
                context.sendBroadcast(intent);

                bluetoothSocket = bluetoothServerSocket.accept();

                SERVER_OPEN = true;
                intent = new Intent();
                intent.setAction(ACTION_CLIENT_OPEN);
                context.sendBroadcast(intent);

                readThread = new ReadThread();
                readThread.start();
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

                Intent intent = new Intent();
                intent.setAction(ACTION_CLIENT_OPEN);
                context.sendBroadcast(intent);

                readThread = new ReadThread();
                readThread.start();
            } catch (IOException e) {
                Log.i("ljjbluetooth","蓝牙连接失败!"+e.toString());
                Intent intent = new Intent();
                intent.setAction(ACTION_CLIENT_ERROR);
                context.sendBroadcast(intent);

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
    public void sendMessage(String outputMessage){
        if(bluetoothSocket==null){Log.i("ljj","蓝牙设备未连接，发送失败");
            //logs = "蓝牙设备未连接，发送失败";
            //return;
        }

        //利用 BluetoothSocket获取输出流进行输出
        try {
            OutputStream outputStream = bluetoothSocket.getOutputStream();
            outputStream.write(outputMessage.getBytes());Log.i("ljj","发送成功："+outputMessage);
            //logs = "发送成功："+outputMessage;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 读取数据
     */
    public class ReadThread extends Thread{
        @Override
        public void run(){
            byte[] buffer = new byte[1024];
            int bytes;
            InputStream inputStream = null;

            try{
                inputStream = bluetoothSocket.getInputStream();
            }catch (IOException e){
                e.printStackTrace();
            }

            while (true){
                try {
                    if((bytes=inputStream.read(buffer))>0){
                        byte[] buffer_data = new byte[bytes+1];
                        for(int i=0;i<bytes;++i){
                            buffer_data[i]=buffer[i];
                        }
                        String s = new String(buffer_data);
                        //输出s
                        Intent intent = new Intent();
                        intent.setAction(ACTION_RECEIVE_MESSAGE);
                        intent.putExtra("receiveMsg",s);
                        context.sendBroadcast(intent);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
