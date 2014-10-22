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
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
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
        initButtonClickListener();

        initBluetooth();
        scanBluetoothDevices();
    }

    private void initView(){
        textViewStatus = (TextView)findViewById(R.id.textViewStatus);
        listViewBluetoothDevices = (ListView)findViewById(R.id.listViewBluetoothDevices);
        buttonReScanDevices = (Button)findViewById(R.id.buttonReScanDevices);

        layoutLogin = (LinearLayout)findViewById(R.id.layoutLogin);
        editTextUserName = (EditText)findViewById(R.id.editTextUserName);
        editTextPassword = (EditText)findViewById(R.id.editTextPassword);
        buttonLogin = (Button)findViewById(R.id.buttonLogin);
        buttonTour = (Button)findViewById(R.id.buttonTour);

        layoutLogin.setVisibility(View.GONE);
    }

    private void initButtonClickListener(){
        buttonReScanDevices.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                scanBluetoothDevices();
            }
        });
        buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String username = editTextUserName.getText().toString();
                String password = editTextPassword.getText().toString();

                if(username==""||password==""){
                    Toast.makeText(LoginActivity.this,"用户名或密码输入不完全",Toast.LENGTH_SHORT).show();
                    return;
                }

                DataUtil.username = username;
                DataUtil.password = password;

                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });
        buttonTour.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    /**
     * 蓝牙扫描
     */
    private class BluetoothBroadcastReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {

            if(intent.getAction().equals(BluetoothDevice.ACTION_FOUND)){
                BluetoothDevice deviceTmp = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                deviceMap.put(deviceTmp.getName(),deviceTmp);
                deviceNames.add(deviceTmp.getName());
                //arrayAdapterDevices.notifyDataSetChanged();
                arrayAdapterDevices = new ArrayAdapter<String>(LoginActivity.this,android.R.layout.simple_list_item_1,deviceNames);
                listViewBluetoothDevices.setAdapter(arrayAdapterDevices);
                Log.i("ljjbluetooth","receive");
            }else if(intent.getAction().equals(BluetoothAdapter.ACTION_DISCOVERY_STARTED)){
                Log.i("ljjbluetooth","start");
            }else if(intent.getAction().equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)){
                Log.i("ljjbluetooth","finish");
            }

        }
    }

    private void initBluetooth(){
        arrayAdapterDevices = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,deviceNames);
        listViewBluetoothDevices.setAdapter(arrayAdapterDevices);

        BluetoothBroadcastReceiver bluetoothReceiver = new BluetoothBroadcastReceiver();
        IntentFilter intentFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(bluetoothReceiver,intentFilter);

        //点击列表项建立连接
        listViewBluetoothDevices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                bluetoothUtil.connectBluetoothDevice(deviceMap.get(deviceNames.get(i)));
                textViewStatus.setText(bluetoothUtil.getLogs());
                if(bluetoothUtil.getLogs().equals("蓝牙连接成功!")){
                    layoutLogin.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void scanBluetoothDevices(){
        deviceMap = new HashMap<String, BluetoothDevice>();
        deviceNames = new ArrayList<String>();
        arrayAdapterDevices.notifyDataSetChanged();
        bluetoothUtil = new BluetoothUtil(this);
    }

}
