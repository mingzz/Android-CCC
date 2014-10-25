package com.ljjqdc.app.c3.main;

import android.app.Activity;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.media.Image;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bairuitech.anychat.AnyChatBaseEvent;
import com.bairuitech.anychat.AnyChatCoreSDK;
import com.bairuitech.anychat.AnyChatDefine;
import com.ljjqdc.app.c3.R;
import com.ljjqdc.app.c3.setting.ConfigEntity;
import com.ljjqdc.app.c3.utils.BluetoothUtil;
import com.ljjqdc.app.c3.utils.DataUtil;
import com.ljjqdc.app.c3.utils.DemoPath;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;


public class MainActivity extends Activity implements AnyChatBaseEvent {

    //蓝牙
    private BluetoothUtil bluetoothUtil;

    //基础控件
    //private RelativeLayout layout1;//基础控件层
    private CheckBox checkBoxButton;
    private CheckBox checkBoxGesture;
    private CheckBox checkBoxVideo;
    private TextView textViewLogs;

    //AnyChat
    private AnyChatCoreSDK anyChatSDK;
    private ConfigEntity configEntity;
    private FrameLayout layout3;//视频层
    private SurfaceView surfaceViewMe;
    private SurfaceView surfaceViewOther;
    private ImageButton buttonSwitchCamera;
    private boolean isAnyChatOnline = false;
    private boolean isAnyChatChatting = false;
    private List<RoleInfo> roleInfoList;
    private int otherUserId;//对面的人的user id

    //voice recognizer
    private ImageButton buttonVoiceRecognizer;

    //按钮控制
    private RelativeLayout layout2;//按钮层
    private ImageButton buttonUp;
    private ImageButton buttonDown;
    private ImageButton buttonLeft;
    private ImageButton buttonRight;

    //划屏控制
    private FrameLayout layout4;
    private DemoPath demoPathGesture;
    private ImageButton buttonRubbish;
    private boolean useGesture = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();

        initBlueTooth();

        initAnyChatView();
        initAnyChatSDK();

        initButtonController();
        initGestureController();

        initVoiceRecognizer();

        textViewLogs.setText("初始化完毕~");
    }

    private void initBlueTooth(){
        bluetoothUtil = BluetoothUtil.getInstance();

        IntentFilter intentFilter = new IntentFilter(BluetoothUtil.ACTION_RECEIVE_MESSAGE);
        registerReceiver(inputStreamReceiver,intentFilter);
    }

    private BroadcastReceiver inputStreamReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String s = intent.getStringExtra("receiveMsg");
            textViewLogs.setText(s);
            Toast.makeText(MainActivity.this,s,Toast.LENGTH_SHORT).show();
        }
    };

    /**
     * 初始化界面控件显示
     */
    private void initView(){

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
                    textViewLogs.setText("可以点击按钮控制哦~");
                }else {
                    layout2.setVisibility(View.GONE);
                    textViewLogs.setText("按钮控制取消");
                }
            }
        });
        checkBoxGesture.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                useGesture = b;
                if(b){
                    textViewLogs.setText("可以划屏控制哦~");
                    layout4.setVisibility(View.VISIBLE);
                }else{
                    textViewLogs.setText("划屏控制取消");
                    demoPathGesture.clearAll();
                    layout4.setVisibility(View.GONE);
                }
            }
        });
        checkBoxVideo.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(b){
                    if(isAnyChatOnline){
                        layout3.setVisibility(View.VISIBLE);
                        textViewLogs.setText("开启视频");
                        openVideoChat();
                    }else{
                        textViewLogs.setText("尚未登录，无法视频");
                    }

                }else {
                    closeVideoChat();
                    layout3.setVisibility(View.GONE);
                    if(surfaceViewMe!=null){
                        surfaceViewMe.setVisibility(View.GONE);
                    }
                    textViewLogs.setText("视频关闭啦~");
                }
            }
        });
    }

    /**
     * 语音识别
     */
    private void initVoiceRecognizer(){
        buttonVoiceRecognizer = (ImageButton)findViewById(R.id.btn_voice_recognizer);

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
            textViewLogs.setText("语音识别："+resultString);
        }
    }

    /**
     * 按钮控制
     */
    private void initButtonController(){
        layout2 = (RelativeLayout)findViewById(R.id.layout2);
        buttonUp = (ImageButton)findViewById(R.id.buttonUp);
        buttonDown = (ImageButton)findViewById(R.id.buttonDown);
        buttonLeft = (ImageButton)findViewById(R.id.buttonLeft);
        buttonRight = (ImageButton)findViewById(R.id.buttonRight);

        layout2.setVisibility(View.GONE);
        buttonUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                textViewLogs.setText("上");
            }
        });
        buttonDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                textViewLogs.setText("下");
            }
        });
        buttonLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                textViewLogs.setText("左");
            }
        });
        buttonRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                textViewLogs.setText("右");
            }
        });
    }

    /**
     * 手势划屏控制
     */
    private void initGestureController(){
        layout4 = (FrameLayout)findViewById(R.id.layout4);
        demoPathGesture = (DemoPath) findViewById(R.id.demoPathGesture);
        buttonRubbish = (ImageButton)findViewById(R.id.buttonRubbish);

        layout4.setVisibility(View.GONE);

        buttonRubbish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                demoPathGesture.clearAll();
            }
        });
    }

    private GestureDetector gestureDetector = new GestureDetector(new GestureDetector.SimpleOnGestureListener(){
        @Override
        public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent2, float v, float v2) {
            if(useGesture){
                if (motionEvent.getX() - motionEvent2.getX() > 100 && Math.abs(v) > 50) {
                    textViewLogs.setText("左");bluetoothUtil.sendMessage("0");
                }else if(motionEvent2.getX() - motionEvent.getX() > 100 && Math.abs(v) > 50){
                    textViewLogs.setText("右");
                }else if(motionEvent2.getY() - motionEvent.getY() > 100 && Math.abs(v) > 50){
                    textViewLogs.setText("下");
                }else if(motionEvent.getY() - motionEvent2.getY() > 100 && Math.abs(v) > 50){
                    textViewLogs.setText("上");
                }
            }
            return super.onFling(motionEvent,motionEvent2,v,v2);
        }
    });

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        gestureDetector.onTouchEvent(ev);
        return super.dispatchTouchEvent(ev);
    }

    /**
     * 视频聊天
     */
    private void initAnyChatView(){
        layout3 = (FrameLayout)findViewById(R.id.layout3);
        buttonSwitchCamera = (ImageButton)findViewById(R.id.ImgSwichVideo);

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

    /**
     * 开启视频聊天
     */
    private void openVideoChat(){
        //获取当前在线人的列表
        final Dialog dialog = new Dialog(this);
        dialog.setTitle("选择视频对象");
        ListView listView = new ListView(this);
        dialog.setContentView(listView);

        roleInfoList = new ArrayList<RoleInfo>();
        int[] userIdList = anyChatSDK.GetOnlineUser();
        RoleInfo info = null;
        for(int i=0;i<userIdList.length;++i){
            info = new RoleInfo();
            info.setName(anyChatSDK.GetUserName(userIdList[i]));
            info.setUserID(String.valueOf(userIdList[i]));
            roleInfoList.add(info);
        }
        RoleListAdapter roleListAdapter = new RoleListAdapter(this,roleInfoList);
        listView.setAdapter(roleListAdapter);

        dialog.show();

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                videoChat(roleInfoList.get(i));
                dialog.dismiss();
            }
        });

    }

    //建立视频聊天
    private void videoChat(RoleInfo roleInfo){
        surfaceViewMe = (SurfaceView)findViewById(R.id.surface_local);
        surfaceViewOther = (SurfaceView)findViewById(R.id.surface_remote);
        surfaceViewMe.setVisibility(View.VISIBLE);
        surfaceViewOther.setVisibility(View.VISIBLE);
        surfaceViewMe.setZOrderOnTop(true);

        isAnyChatChatting = true;
        otherUserId = Integer.parseInt(roleInfo.getUserID());
        textViewLogs.setText("正在和"+roleInfo.getName()+"视频聊天");
        //初始化
        anyChatSDK.mSensorHelper.InitSensor(this);
        AnyChatCoreSDK.mCameraHelper.SetContext(this);

        buttonSwitchCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 如果是采用Java视频采集，则在Java层进行摄像头切换
                if (AnyChatCoreSDK.GetSDKOptionInt(AnyChatDefine.BRAC_SO_LOCALVIDEO_CAPDRIVER) == AnyChatDefine.VIDEOCAP_DRIVER_JAVA) {
                    AnyChatCoreSDK.mCameraHelper.SwitchCamera();
                    return;
                }

                String strVideoCaptures[] = anyChatSDK.EnumVideoCapture();
                String temp = anyChatSDK.GetCurVideoCapture();
                for (int i = 0; i < strVideoCaptures.length; i++) {
                    if (!temp.equals(strVideoCaptures[i])) {
                        anyChatSDK.UserCameraControl(-1, 0);
                        anyChatSDK.SelectVideoCapture(strVideoCaptures[i]);
                        anyChatSDK.UserCameraControl(-1, 1);
                        break;
                    }
                }
            }
        });

        // 如果是采用Java视频采集，则需要设置Surface的CallBack
        if (AnyChatCoreSDK.GetSDKOptionInt(AnyChatDefine.BRAC_SO_LOCALVIDEO_CAPDRIVER) == AnyChatDefine.VIDEOCAP_DRIVER_JAVA) {
            surfaceViewMe.getHolder().addCallback(AnyChatCoreSDK.mCameraHelper);
        }

        // 如果是采用Java视频显示，则需要设置Surface的CallBack
        if (AnyChatCoreSDK.GetSDKOptionInt(AnyChatDefine.BRAC_SO_VIDEOSHOW_DRIVERCTRL) == AnyChatDefine.VIDEOSHOW_DRIVER_JAVA) {
            int index = anyChatSDK.mVideoHelper.bindVideo(surfaceViewOther.getHolder());
            anyChatSDK.mVideoHelper.SetVideoUser(index, otherUserId);
        }

        SurfaceHolder holder = surfaceViewOther.getHolder();
        holder.setKeepScreenOn(true);

        anyChatSDK.UserCameraControl(otherUserId, 1);
        anyChatSDK.UserSpeakControl(otherUserId, 1);

        // 判断是否显示本地摄像头切换图标
        if (AnyChatCoreSDK.GetSDKOptionInt(AnyChatDefine.BRAC_SO_LOCALVIDEO_CAPDRIVER) == AnyChatDefine.VIDEOCAP_DRIVER_JAVA) {
            if (AnyChatCoreSDK.mCameraHelper.GetCameraNumber() > 1) {
                // 默认打开前置摄像头
                AnyChatCoreSDK.mCameraHelper.SelectVideoCapture(AnyChatCoreSDK.mCameraHelper.CAMERA_FACING_FRONT);
            }
        } else {
            String[] strVideoCaptures = anyChatSDK.EnumVideoCapture();
            if (strVideoCaptures != null && strVideoCaptures.length > 1) {
                // 默认打开前置摄像头
                for (int i = 0; i < strVideoCaptures.length; i++) {
                    String strDevices = strVideoCaptures[i];
                    if (strDevices.indexOf("Front") >= 0) {
                        anyChatSDK.SelectVideoCapture(strDevices);
                        break;
                    }
                }
            }
        }

        anyChatSDK.UserCameraControl(-1, 1);//-1表示对本地视频进行控制，打开本地视频
        anyChatSDK.UserSpeakControl(-1, 1);//-1表示对本地音频进行控制，打开本地音频

        // 根据屏幕方向改变本地surfaceview的宽高比
        if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            adjustLocalVideo(true);
        } else if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            adjustLocalVideo(false);
        }

    }

    private void adjustLocalVideo(boolean bLandScape){
        float width;
        float height = 0;
        DisplayMetrics dMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dMetrics);
        width = (float) dMetrics.widthPixels / 4;
        LinearLayout layoutLocal = (LinearLayout) this
                .findViewById(R.id.frame_local_area);
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) layoutLocal
                .getLayoutParams();
        if (bLandScape) {

            if (AnyChatCoreSDK
                    .GetSDKOptionInt(AnyChatDefine.BRAC_SO_LOCALVIDEO_WIDTHCTRL) != 0)
                height = width
                        * AnyChatCoreSDK
                        .GetSDKOptionInt(AnyChatDefine.BRAC_SO_LOCALVIDEO_HEIGHTCTRL)
                        / AnyChatCoreSDK
                        .GetSDKOptionInt(AnyChatDefine.BRAC_SO_LOCALVIDEO_WIDTHCTRL)
                        + 5;
            else
                height = (float) 3 / 4 * width + 5;
        } else {

            if (AnyChatCoreSDK
                    .GetSDKOptionInt(AnyChatDefine.BRAC_SO_LOCALVIDEO_HEIGHTCTRL) != 0)
                height = width
                        * AnyChatCoreSDK
                        .GetSDKOptionInt(AnyChatDefine.BRAC_SO_LOCALVIDEO_WIDTHCTRL)
                        / AnyChatCoreSDK
                        .GetSDKOptionInt(AnyChatDefine.BRAC_SO_LOCALVIDEO_HEIGHTCTRL)
                        + 5;
            else
                height = (float) 4 / 3 * width + 5;
        }
        layoutParams.width = (int) width;
        layoutParams.height = (int) height;
        layoutLocal.setLayoutParams(layoutParams);
    }

    private void refreshAV() {
        anyChatSDK.UserCameraControl(otherUserId, 1);
        anyChatSDK.UserSpeakControl(otherUserId, 1);
        anyChatSDK.UserCameraControl(-1, 1);
        anyChatSDK.UserSpeakControl(-1, 1);
    }

    /**
     * 关闭视频聊天
     */
    private void closeVideoChat(){
        if(isAnyChatChatting){
            anyChatSDK.mSensorHelper.DestroySensor();
            isAnyChatChatting = false;
            anyChatSDK.UserCameraControl(otherUserId, 0);
            anyChatSDK.UserSpeakControl(otherUserId, 0);
            anyChatSDK.UserCameraControl(-1, 0);
            anyChatSDK.UserSpeakControl(-1, 0);
        }

    }

    @Override
    public void onDestroy(){
        unregisterReceiver(inputStreamReceiver);

        anyChatSDK.Logout();
        anyChatSDK.Release();
        isAnyChatOnline = false;
        isAnyChatChatting = false;
        super.onDestroy();
    }

    @Override
    public void OnAnyChatConnectMessage(boolean bSuccess) {
        if (!bSuccess) {
            textViewLogs.setText("连接视频通信服务器失败，自动重连，请稍后...");
            Log.i("ljj","连接视频通信服务器失败，自动重连，请稍后...");
            isAnyChatOnline = false;
            isAnyChatChatting = false;
        }else{
            isAnyChatOnline = true;
        }
    }

    @Override
    public void OnAnyChatLoginMessage(int dwUserId, int dwErrorCode) {
        if (dwErrorCode == 0) {
            textViewLogs.setText("登录成功！");
            Log.i("ljj","登录成功！");
            anyChatSDK.EnterRoom(DataUtil.roomID, "");
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
        refreshAV();
    }

    @Override
    public void OnAnyChatUserAtRoomMessage(int dwUserId, boolean bEnter) {
        /*if(bEnter){
            //有人上线
            RoleInfo info = new RoleInfo();
            info.setUserID(String.valueOf(dwUserId));
            info.setName(anyChatSDK.GetUserName(dwUserId));
            roleInfoList.add(info);

        }else{
            //有人下线
            for(int i=0;i<roleInfoList.size();++i){
                if(roleInfoList.get(i).getUserID().equals(""+dwUserId)){
                    roleInfoList.remove(i);
                }
            }
        }*/
    }

    @Override
    public void OnAnyChatLinkCloseMessage(int dwErrorCode) {
        textViewLogs.setText("连接关闭，error：" + dwErrorCode);
        isAnyChatOnline = false;
        isAnyChatChatting = false;

        // 网络连接断开之后，上层需要主动关闭已经打开的音视频设备
        anyChatSDK.UserCameraControl(otherUserId, 0);
        anyChatSDK.UserSpeakControl(otherUserId, 0);
        anyChatSDK.UserCameraControl(-1, 0);
        anyChatSDK.UserSpeakControl(-1, 0);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            adjustLocalVideo(true);
            AnyChatCoreSDK.mCameraHelper.setCameraDisplayOrientation();
        } else {
            adjustLocalVideo(false);
            AnyChatCoreSDK.mCameraHelper.setCameraDisplayOrientation();
        }

    }


}
