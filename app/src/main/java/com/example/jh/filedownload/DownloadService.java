package com.example.jh.filedownload;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import java.io.File;

public class DownloadService extends Service {

    private DownloadTask downloadtask;

    private DownloadListener listener = new DownloadListener() {
        @Override
        public void onProgress(int progress) {
            // 显示progress 进度
            getNotificationManager().notify(1, getNotification("下载中...", progress));
//            ToastUtil.showShort(String.valueOf(progress) + "%");  // 百分比
            Intent intent = new Intent();
            intent.putExtra("progress", progress);
            intent.setAction("aa");
            sendBroadcast(intent);
        }

        @Override
        public void onSuccess() {
            // 下载成功后移除异步任务，并关闭前台通知；
            downloadtask = null;
            stopForeground(true);   //下载成功之后将前台服务通知关闭，并创建一个下载成功的通知。

            getNotificationManager().notify(1, getNotification("下载完成", -1));

            Toast.makeText(DownloadService.this, "下载成功", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onFailed() {
            downloadtask = null;
            // 下载失败时将前台任务关闭，并创建一个下载失败的通知；
            stopForeground(true);

            getNotificationManager().notify(1, getNotification("下载失败", -1));
            Toast.makeText(DownloadService.this, "下载失败", Toast.LENGTH_SHORT).show();

            Log.e("----onFailed------", "下载失败了~~~~");
            stopForeground(true);
        }

        @Override
        public void onPaused() {
            downloadtask = null;
            Toast.makeText(DownloadService.this, "暂停下载", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onCanceled() {
            downloadtask = null;
            stopForeground(true);   // 取消下载
            Toast.makeText(DownloadService.this, "取消下载", Toast.LENGTH_SHORT).show();
            Log.e("~~~~~~~~~~··", "onCanceled");
        }
    };
    private String downloadUrl;
    private DownloadBinder mBinder = new DownloadBinder();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public class DownloadBinder extends Binder {
        /**
         * 开始下载
         */
        public void startDownload(String url) {
            if (downloadtask == null) {
                downloadUrl = url;
                downloadtask = new DownloadTask(listener);
                downloadtask.execute(downloadUrl);  //执行异步加载

                startForeground(1, getNotification("Download", 0));   //开启前台服务；
//                Toast.makeText(DownloadService.this, "开始下载啦", Toast.LENGTH_SHORT).show();
            }
        }

        /**
         * 暂停下载
         */
        public void pauseDownload() {
            if (downloadtask != null) {
                downloadtask.pauseDownload();
            }
        }

        /**
         * 取消下载
         */
        public void cancelDownload() {
            if (downloadtask != null) {
                downloadtask.cancelDownload();
            } else {
                if (downloadUrl != null) {
                    // 取消下载时，将文件删除并关闭通知
                    String fileName = downloadUrl.substring(downloadUrl.lastIndexOf("/"));
                    String directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
                    File file = new File(directory + fileName);
                    if (file.exists()) {
                        file.delete();
                    }

                    getNotificationManager().cancel(1);   //关闭通知
                }
            }

            Log.e("---点击取消下载-", "取消~~~~");

        }
    }

    private NotificationManager getNotificationManager() {
        return (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    }


    // 显示对话框；
    private Notification getNotification(String title, int progress) {
        Intent intent = new Intent(DownloadService.this, DownloadActivity.class);
        PendingIntent pi = PendingIntent.getActivity(DownloadService.this, 0, intent, 0);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(DownloadService.this);
        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setContentTitle(title);
        if (progress > 0) {
            builder.setContentText(progress + "%");
            builder.setProgress(100, progress, false);
        }
        builder.setContentIntent(pi);
        return builder.build();
    }

}