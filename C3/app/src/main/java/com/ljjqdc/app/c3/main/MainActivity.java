package com.ljjqdc.app.c3.main;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bairuitech.anychat.AnyChatBaseEvent;
import com.bairuitech.anychat.AnyChatCoreSDK;
import com.bairuitech.anychat.AnyChatDefine;
import com.example.helloanychat.ChatMainActivity;
import com.ljjqdc.app.c3.R;
import com.ljjqdc.app.c3.setting.ConfigEntity;
import com.ljjqdc.app.c3.utils.DataUtil;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;


public class MainActivity extends Activity implements AnyChatBaseEvent {

    //基础控件
    //private RelativeLayout layout1;//基础控件层
    private CheckBox checkBoxButton;
    private CheckBox checkBoxGesture;
    private CheckBox checkBoxVideo;
    private TextView textViewLogs;

    //Bluetooth
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private BluetoothDevice bluetoothDevice;

    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private String outputMessage = "你想发送的东西";

    //AnyChat
    private AnyChatCoreSDK anyChatSDK;
    private ConfigEntity configEntity;
    private FrameLayout layout3;//视频层
    private boolean isAnyChatOnline = false;

    //voice recognizer
    private Button buttonVoiceRecognizer;

    //按钮控制
    private RelativeLayout layout2;//按钮层
    private Button buttonUp;
    private Button buttonDown;
    private Button buttonLeft;
    private Button buttonRight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();

        //initBlueTooth();

        initAnyChatView();
        initAnyChatSDK();

        initButtonController();

        initGestureController();

        initVoiceRecognizer();
    }

    /**
     * 初始化界面控件显示
     */
    private void initView(){
        //临时
        Button button = (Button)findViewById(R.id.btn_test);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, ChatMainActivity.class);
                startActivity(intent);
            }
        });

        checkBoxButton = (CheckBox)findViewById(R.id.checkBoxButton);
        checkBoxGesture = (CheckBox)findViewById(R.id.checkBoxGesture);
        checkBoxVideo = (CheckBox)findViewById(R.id.checkBoxVideo);

        textViewLogs = (TextView)findViewById(R.id.textViewLogs);

        /**
         * check状态改变，layout会不同
         */
        checkBoxButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(b){
                    layout2.setVisibility(View.VISIBLE);
                }else {
                    layout2.setVisibility(View.GONE);
                }
            }
        });
        checkBoxGesture.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {

            }
        });
        checkBoxVideo.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(b){
                    if(isAnyChatOnline){
                        layout3.setVisibility(View.VISIBLE);
                    }else{

                    }

                }else {
                    layout3.setVisibility(View.GONE);
                }
            }
        });
    }

    /**
     * 蓝牙控制小车
     */
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


    /**
     * 语音识别
     */
    private void initVoiceRecognizer(){
        buttonVoiceRecognizer = (Button)findViewById(R.id.btn_voice_recognizer);

        buttonVoiceRecognizer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try{
                    Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                    intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                    intent.putExtra(RecognizerIntent.EXTRA_PROMPT,"kaishi");
                    startActivityForResult(intent,0);
                }catch (Exception e){
                    e.printStackTrace();
                    Toast.makeText(MainActivity.this,"您的手机不支持语音识别",Toast.LENGTH_SHORT).show();
                }
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
            textViewLogs.setText(resultString);
        }
    }

    /**
     * 按钮控制
     */
    private void initButtonController(){
        layout2 = (RelativeLayout)findViewById(R.id.layout2);
        buttonUp = (Button)findViewById(R.id.buttonUp);
        buttonDown = (Button)findViewById(R.id.buttonDown);
        buttonLeft = (Button)findViewById(R.id.buttonLeft);
        buttonRight = (Button)findViewById(R.id.buttonRight);

        layout2.setVisibility(View.GONE);
        buttonUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });
        buttonDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });
        buttonLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });
        buttonRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });
    }

    /**
     * 手势划屏控制
     */
    private void initGestureController(){

    }

    /**
     * 视频聊天
     */
    private void initAnyChatView(){
        layout3 = (FrameLayout)findViewById(R.id.layout3);

        layout3.setVisibility(View.GONE);
    }

    private void initAnyChatSDK(){
        if (anyChatSDK == null) {
            anyChatSDK = new AnyChatCoreSDK();
            anyChatSDK.SetBaseEvent(this);
            anyChatSDK.InitSDK(android.os.Build.VERSION.SDK_INT, 0);

            configEntity = new ConfigEntity();
            // 视频采集驱动设置
            AnyChatCoreSDK.SetSDKOptionInt(
                    AnyChatDefine.BRAC_SO_LOCALVIDEO_CAPDRIVER, configEntity.videoCapDriver);
            // 视频显示驱动设置
            AnyChatCoreSDK.SetSDKOptionInt(
                    AnyChatDefine.BRAC_SO_VIDEOSHOW_DRIVERCTRL, configEntity.videoShowDriver);
            // 音频播放驱动设置
            AnyChatCoreSDK.SetSDKOptionInt(
                    AnyChatDefine.BRAC_SO_AUDIO_PLAYDRVCTRL, configEntity.audioPlayDriver);
            // 音频采集驱动设置
            AnyChatCoreSDK.SetSDKOptionInt(
                    AnyChatDefine.BRAC_SO_AUDIO_RECORDDRVCTRL, configEntity.audioRecordDriver);
        }

        //登陆AnyChat
        if(DataUtil.username!=null && DataUtil.password!=null){
            anyChatSDK.Connect(DataUtil.ip, DataUtil.port);
            anyChatSDK.Login(DataUtil.username, DataUtil.password);
        }

    }

    @Override
    public void OnAnyChatConnectMessage(boolean bSuccess) {
        if (!bSuccess) {
            textViewLogs.setText("连接视频通信服务器失败，自动重连，请稍后...");
            Log.i("ljj","连接视频通信服务器失败，自动重连，请稍后...");
            isAnyChatOnline = false;
        }else{
            isAnyChatOnline = true;
        }
    }

    @Override
    public void OnAnyChatLoginMessage(int dwUserId, int dwErrorCode) {
        if (dwErrorCode == 0) {
            textViewLogs.setText("登录成功！");
            Log.i("ljj","登录成功！");
            int sHourseID = 1;
            anyChatSDK.EnterRoom(sHourseID, "");
            isAnyChatOnline = true;
        } else {
            textViewLogs.setText("登录失败，错误代码：" + dwErrorCode);
            Log.i("ljj","登录失败，错误代码：" + dwErrorCode);
            isAnyChatOnline = false;
        }
    }

    @Override
    public void OnAnyChatEnterRoomMessage(int dwRoomId, int dwErrorCode) {

    }

    @Override
    public void OnAnyChatOnlineUserMessage(int dwUserNum, int dwRoomId) {

    }

    @Override
    public void OnAnyChatUserAtRoomMessage(int dwUserId, boolean bEnter) {

    }

    @Override
    public void OnAnyChatLinkCloseMessage(int dwErrorCode) {
        textViewLogs.setText("连接关闭，error：" + dwErrorCode);
        isAnyChatOnline = false;
    }
}
