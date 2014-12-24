package com.ljjqdc.app.c3.utils;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.UUID;

/**
 * Created by jingjing on 2014/10/22.
 */
public class BluetoothUtil {
    private String TAG = "ljjblue";

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

    public static final String ACTION_WIFI_SERVER_OPEN = "com.ljjqdc.app.c3.utils.BluetoothUtil.wifi_server_open";
    public static final String ACTION_WIFI_CLIENT_OPEN = "com.ljjqdc.app.c3.utils.BluetoothUtil.wifi_client_open";
    public static final String ACTION_WIFI_CLIENT_ERROR = "com.ljjqdc.app.c3.utils.BluetoothUtil.wifi_client_error";
    public static final String ACTION_BLUETOOTH_CLIENT_OPEN = "com.ljjqdc.app.c3.utils.BluetoothUtil.bluetooth_client_open";
    public static final String ACTION_BLUETOOTH_CLIENT_ERROR = "com.ljjqdc.app.c3.utils.BluetoothUtil.bluetooth_client_error";
    public static final String ACTION_RECEIVE_MESSAGE = "com.ljjqdc.app.c3.utils.BluetoothUtil.receive_message";

    private Context context;

    //Bluetooth
    private BluetoothAdapter bluetoothAdapter;
    //private Set<BluetoothDevice> bluetoothDevices;
    private BluetoothDevice bluetoothDevice;

    private ServerThread serverThread;
    private ClientWifiThread clientWifiThread;
    private ClientThread clientThread;
    private ReadThread readThread;

    private BluetoothSocket bluetoothSocket;
    //private BluetoothServerSocket bluetoothServerSocket;
    private ServerSocket serverSocket;
    private Socket socket;
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
     * 初始化wifi服务器端
     */
    public void startServer(){
        //设备可以被检测到
        /*Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION,3000);
        context.startActivity(intent);*/

        serverThread = new ServerThread();
        serverThread.start();
    }

    /**
     * 关闭wifi服务器端
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
        if(serverSocket!=null){
            try {
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            serverSocket = null;
        }
        if(socket!=null){
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            socket = null;
        }
        SERVER_OPEN = false;
    }

    /**
     * wifi客户端连接
     */
    public void connectWifiClient(){
        clientWifiThread = new ClientWifiThread();
        clientWifiThread.start();
    }

    /**
     * 关闭wifi客户端连接
     */
    public void finishWifiClient(){
        if(clientWifiThread!=null){
            clientWifiThread.interrupt();
            clientWifiThread = null;
        }
        if(readThread!=null){
            readThread.interrupt();
            readThread = null;
        }
        if(socket!=null){
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            socket = null;
        }
    }

    /**
     * 蓝牙客户端连接
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
     * 关闭蓝牙客户端连接
     */
    public void finishClient(){
        CLIENT_CONNECT = false;
        if(clientThread!=null){
            clientThread.interrupt();
            clientThread = null;
        }
    }

    private class ServerThread extends Thread{
        @Override
        public void run(){
            try {
                /*bluetoothServerSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord("ljjserver",MY_UUID);Log.i("ljjbluetooth","server open");
                Intent intent = new Intent();
                intent.setAction(ACTION_SERVER_OPEN);
                context.sendBroadcast(intent);

                bluetoothSocket = bluetoothServerSocket.accept();*/

                serverSocket = new ServerSocket(Integer.valueOf(DataUtil.getSpfString(context,DataUtil.SPF_WIFI_NAME,DataUtil.SPF_WIFI_PORT,"")));
                Intent intent = new Intent();
                intent.setAction(ACTION_WIFI_SERVER_OPEN);
                context.sendBroadcast(intent);

                socket = serverSocket.accept();

                SERVER_OPEN = true;
                intent = new Intent();
                intent.setAction(ACTION_WIFI_CLIENT_OPEN);
                context.sendBroadcast(intent);

                readThread = new ReadThread();
                readThread.start();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public class ClientWifiThread extends Thread{
        @Override
        public void run(){
            try {
                socket = new Socket(DataUtil.getSpfString(context,DataUtil.SPF_WIFI_NAME,DataUtil.SPF_WIFI_ADDRESS,""),
                        Integer.valueOf(DataUtil.getSpfString(context,DataUtil.SPF_WIFI_NAME,DataUtil.SPF_WIFI_PORT,"")));

                Intent intent = new Intent();
                intent.setAction(ACTION_WIFI_CLIENT_OPEN);
                context.sendBroadcast(intent);

                readThread = new ReadThread();
                readThread.start();
            } catch (IOException e) {
                e.printStackTrace();Log.i(TAG,e.getMessage());
                Intent intent = new Intent();
                intent.setAction(ACTION_WIFI_CLIENT_ERROR);
                context.sendBroadcast(intent);
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
                CLIENT_CONNECT = true;Log.i(TAG,"connect succeed");

                Intent intent = new Intent();
                intent.setAction(ACTION_BLUETOOTH_CLIENT_OPEN);
                context.sendBroadcast(intent);

            } catch (IOException e) {
                Log.i(TAG,"蓝牙连接失败!"+e.toString());
                Intent intent = new Intent();
                intent.setAction(ACTION_BLUETOOTH_CLIENT_ERROR);
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
     * 手机通过蓝牙发送消息给小车
     */
    public void sendMessageViaBluetooth(String outputMessage){
        if(bluetoothSocket==null){
            Log.i(TAG,"蓝牙设备未连接，发送失败");
            //logs = "蓝牙设备未连接，发送失败";
            return;
        }

        //利用 BluetoothSocket获取输出流进行输出
        try {
            OutputStream outputStream = bluetoothSocket.getOutputStream();
            outputStream.write(outputMessage.getBytes());
            outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 手机通过wifi发送消息给另外一台手机
     */
    public void sendMessageViaWifi(String outputMessage){
        if(socket==null){
            Log.i(TAG,"未连接另一台手机，发送失败");
            return;
        }

        try {
            OutputStream outputStream = socket.getOutputStream();
            outputStream.write(outputMessage.getBytes());
            Log.i(TAG, "发送成功：" + outputMessage);
            outputStream.flush();
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
                inputStream = socket.getInputStream();
            }catch (Exception e){
                e.printStackTrace();
                return;
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
