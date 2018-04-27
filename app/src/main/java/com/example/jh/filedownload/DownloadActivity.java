package com.example.jh.filedownload;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.example.jh.filedownload.view.RoundlProgresWithNum;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

/**
 * 文件下载 https://www.jianshu.com/p/927a524f3b82
 * <p>
 * <文件下载>
 * 分析：
 * 异步下载AsyncTask
 * 后台服务开启下载
 * 使用Activity显示界面
 */
public class DownloadActivity extends AppCompatActivity implements View.OnClickListener {

    private String tag = "DownloadActivity";
    private DownloadService.DownloadBinder binder;

    MyBroadcast myBroadcast;
    private RoundlProgresWithNum mRoundlProgresWithNum32;
    private Timer timer32;

    private ServiceConnection connection = new ServiceConnection() {

        @Override       // 服务连接
        public void onServiceConnected(ComponentName name, IBinder service) {
            binder = (DownloadService.DownloadBinder) service;
        }

        @Override       //服务断开连接
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initService();
        initView();

        myBroadcast = new MyBroadcast();
        IntentFilter intentFilter = new IntentFilter("aa");
        registerReceiver(myBroadcast, intentFilter);

        //Circle progress no num
        mRoundlProgresWithNum32 = findViewById(R.id.mRoundlProgresWithNum32);
        mRoundlProgresWithNum32.setProgress(0);
        mRoundlProgresWithNum32.setMax(100);

//        timer32 = new Timer();
//        timer32.schedule(new TimerTask() {
//            @Override
//            public void run() {
//                //实时更新进度
//                if (mRoundlProgresWithNum32.getProgress() >= mRoundlProgresWithNum32.getMax()) {//指定时间取消
//                    timer32.cancel();
//                }
//                mRoundlProgresWithNum32.setProgress(mRoundlProgresWithNum32.getProgress() + 1);
//
//            }
//        }, 30, 30);

    }

    private void initService() {
        Intent intent = new Intent(DownloadActivity.this, DownloadService.class);
        startService(intent);       // 开启服务
        bindService(intent, connection, BIND_AUTO_CREATE);    // 绑定服务

        // 请求权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "拒绝权限无法使用程序", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
        }
    }

    private void initView() {
        findViewById(R.id.button_start).setOnClickListener(this);
        findViewById(R.id.button_stop).setOnClickListener(this);
        findViewById(R.id.button_canceled).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (binder == null) {
            Log.e(tag, "------binder==null-------");
            return;
        }
        switch (v.getId()) {
            case R.id.button_start:
                String url = "https://lcadream.oss-cn-shanghai.aliyuncs.com/book/1880fa21d2d7ddb1.epub";
                // 如果文件存在就点击读取epub文件，如果文件不存在就下载文件
                String fileName =
                        url.substring(url.lastIndexOf("/")); //获取文件的名称
                String pathName = Environment.getExternalStorageDirectory().getAbsolutePath() + fileName;
                Log.e(tag, "pathName = " + pathName); // pathName = /storage/emulated/0/1880fa21d2d7ddb1.epub
                File file = new File(pathName);
                if (file.exists()) {    // 如果文件已经存在,获取文件已经下载的进度
                    // 如果文件存在就点击读取epub文件，如果文件不存在就下载文件
//                    readEpub(pathName);
                    ToastUtil.showShort("文件已存在");
                }else {
//                    String url = "https://raw.githubusercontent.com/goulindev/eclipse/master/eclipse-inst-win64.exe";
                    binder.startDownload(url);
                    Log.e(tag, "------点击开始下载-------");
                }
                break;
            case R.id.button_stop:
                binder.pauseDownload();
                Log.e(tag, "------点击停止下载-------");
                break;
            case R.id.button_canceled:
                binder.cancelDownload();
                break;
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(myBroadcast);
        unbindService(connection);
        stopService(new Intent(this, DownloadService.class));
    }

    private class MyBroadcast extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            int progress = intent.getIntExtra("progress", 0);
            Log.e(tag, "广播progress = " + progress);
            ToastUtil.showShort(progress + "%");
            //实时更新进度
            mRoundlProgresWithNum32.setProgress(progress);
            if (mRoundlProgresWithNum32.getProgress() >= mRoundlProgresWithNum32.getMax()) { //指定时间取消
//                mRoundlProgresWithNum32.setVisibility(View.INVISIBLE);
            }
        }
    }
}

