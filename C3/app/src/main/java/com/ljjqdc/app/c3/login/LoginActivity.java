package com.ljjqdc.app.c3.login;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.bairuitech.anychat.AnyChatCoreSDK;
import com.bairuitech.anychat.AnyChatDefine;
import com.ljjqdc.app.c3.R;
import com.ljjqdc.app.c3.main.MainActivity;
import com.ljjqdc.app.c3.setting.ConfigEntity;
import com.ljjqdc.app.c3.utils.BluetoothUtil;
import com.ljjqdc.app.c3.utils.DataUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class LoginActivity extends Activity {

    //bluetooth
    private Switch switchServerOrClient;
    private LinearLayout layoutConnectBluetooth;
    private TextView textViewStatus;
    private ListView listViewBluetoothDevices;
    private Button buttonReScanDevices;
    private ArrayAdapter<String> arrayAdapterDevices;

    private BluetoothUtil bluetoothUtil;
    private Map<String,BluetoothDevice> deviceMap = new HashMap<String, BluetoothDevice>();
    private List<String> deviceNames = new ArrayList<String>();

    //login
    private LinearLayout layoutLogin;
    private EditText editTextUserName;
    private EditText editTextPassword;
    private Button buttonLogin;
    private Button buttonTour;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initView();
        initListener();

        initBluetooth();
        startServer();
    }

    private void initView(){
        switchServerOrClient = (Switch)findViewById(R.id.switchSeverOrClient);
        textViewStatus = (TextView)findViewById(R.id.textViewStatus);

        layoutConnectBluetooth = (LinearLayout)findViewById(R.id.layoutConnectBluetooth);
        listViewBluetoothDevices = (ListView)findViewById(R.id.listViewBluetoothDevices);
        buttonReScanDevices = (Button)findViewById(R.id.buttonReScanDevices);

        layoutLogin = (LinearLayout)findViewById(R.id.layoutLogin);
        editTextUserName = (EditText)findViewById(R.id.editTextUserName);
        editTextPassword = (EditText)findViewById(R.id.editTextPassword);
        buttonLogin = (Button)findViewById(R.id.buttonLogin);
        buttonTour = (Button)findViewById(R.id.buttonTour);

        layoutConnectBluetooth.setVisibility(View.GONE);
        editTextUserName.setText(BluetoothAdapter.getDefaultAdapter().getName());
    }

    private void initListener(){
        switchServerOrClient.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(b){
                    //作为客户端
                    layoutConnectBluetooth.setVisibility(View.VISIBLE);

                    finishSever();//关闭服务器
                    startClient();//开启客户端
                }else{
                    //作为服务器
                    layoutConnectBluetooth.setVisibility(View.GONE);

                    finishClient();//关闭客户端
                    startServer();//开启服务器
                }
            }
        });

        buttonReScanDevices.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startClient();
            }
        });
        buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String username = editTextUserName.getText().toString();
                String password = editTextPassword.getText().toString();

                if(username.equals("")||password.equals("")){
                    Toast.makeText(LoginActivity.this,"用户名或密码输入不完全",Toast.LENGTH_SHORT).show();
                    return;
                }

                if(bluetoothUtil.CLIENT_CONNECT || bluetoothUtil.SERVER_OPEN){
                    //作为客户端或者服务器连接上了
                    DataUtil.username = username;
                    DataUtil.password = password;

                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    startActivity(intent);
                }else{
                    Toast.makeText(LoginActivity.this,"蓝牙设备未连接",Toast.LENGTH_SHORT).show();
                }

            }
        });
        buttonTour.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(bluetoothUtil.CLIENT_CONNECT || bluetoothUtil.SERVER_OPEN){
                    //作为客户端或者服务器连接上了
                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    startActivity(intent);
                }else{
                    Toast.makeText(LoginActivity.this,"蓝牙设备未连接",Toast.LENGTH_SHORT).show();
                }

            }
        });
    }

    /**
     * 蓝牙扫描广播
     */
    private BroadcastReceiver bluetoothReceiver =new BroadcastReceiver(){

        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(BluetoothDevice.ACTION_FOUND)){
                BluetoothDevice deviceTmp = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                deviceMap.put(deviceTmp.getName(),deviceTmp);
                deviceNames.add(deviceTmp.getName());
                //arrayAdapterDevices.notifyDataSetChanged();
                arrayAdapterDevices = new ArrayAdapter<String>(LoginActivity.this,android.R.layout.simple_list_item_1,deviceNames);
                listViewBluetoothDevices.setAdapter(arrayAdapterDevices);
                textViewStatus.setText("找到以下设备，点击连接");
            }else if(intent.getAction().equals(BluetoothUtil.ACTION_CLIENT_OPEN)){
                //客户端成功连接一台设备
                textViewStatus.setText("蓝牙连接成功！");
            }else if(intent.getAction().equals(BluetoothUtil.ACTION_CLIENT_ERROR)){
                textViewStatus.setText("蓝牙连接失败，请重试");
            }else if(intent.getAction().equals(BluetoothUtil.ACTION_SERVER_OPEN)){
                //服务器打开成功
                textViewStatus.setText("蓝牙连接成功！");
            }

        }
    };

    @Override
    public void onDestroy(){
        unregisterReceiver(bluetoothReceiver);
        super.onDestroy();
    }

    private void initBluetooth(){
        BluetoothUtil.init(this);
        bluetoothUtil = BluetoothUtil.getInstance();
        arrayAdapterDevices = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,deviceNames);
        listViewBluetoothDevices.setAdapter(arrayAdapterDevices);

        IntentFilter intentFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        intentFilter.addAction(BluetoothUtil.ACTION_CLIENT_OPEN);
        intentFilter.addAction(BluetoothUtil.ACTION_CLIENT_ERROR);
        intentFilter.addAction(BluetoothUtil.ACTION_SERVER_OPEN);
        registerReceiver(bluetoothReceiver, intentFilter);

        //点击列表项建立连接
        listViewBluetoothDevices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                bluetoothUtil.connectBluetoothDevice(deviceMap.get(deviceNames.get(i)));
                DataUtil.connectDeviceName = deviceNames.get(i);
            }
        });
    }

    private void startServer(){
        textViewStatus.setText("正在等待客户端的连接。。。");
        bluetoothUtil.startServer();
    }

    private void finishSever(){
        bluetoothUtil.finishServer();
    }

    private void startClient(){
        textViewStatus.setText("正在搜寻服务端。。。");
        deviceMap = new HashMap<String, BluetoothDevice>();
        deviceNames = new ArrayList<String>();
        arrayAdapterDevices = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,deviceNames);
        listViewBluetoothDevices.setAdapter(arrayAdapterDevices);
        bluetoothUtil.startSearch();
    }

    private void finishClient(){
        bluetoothUtil.finishClient();
    }
}
