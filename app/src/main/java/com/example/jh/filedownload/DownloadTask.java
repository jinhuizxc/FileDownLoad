package com.example.jh.filedownload;

import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DownloadTask extends AsyncTask<String, Integer, Integer> {

    // 定义四个int类型值，来记录文件下载的状态。
    public static final int TYPE_SUCCESS = 0;       //成功
    public static final int TYPE_FAILED = 1;        //失败
    public static final int TYPE_PAUSED = 2;        //暂停
    public static final int TYPE_CANCELED = 3;      //取消
    public static final int TYPE_Exits = 4;      //取消
    private static final String TAG = "DownloadTask";


    private boolean isCanceled = false;
    private boolean isPaused = false;
    private int lastProgress;

    private DownloadListener downloadListener;

    int progress;

    boolean isFirst = true;

    public DownloadTask(DownloadListener mDownloadListener) {
        this.downloadListener = mDownloadListener;
    }


    /**
     * 任务开始执行该方法，可以做界面的初始化操作。
     */
    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    /**
     * 开始执行后台任务，该方法中的所有代码都在自线程中操作。
     */
    @Override
    protected Integer doInBackground(String... params) {
        InputStream is = null;      //从服务区读取数据
        RandomAccessFile savedFile = null;
        File file = null;
        try {
            long downloadedLength = 0;   // 记录已下载的文件长度
            String downloadUrl = params[0];
            String fileName =
                    downloadUrl.substring(downloadUrl.lastIndexOf("/")); //获取文件的名称
            Log.e(TAG, "fileName = " + fileName);
            // 通过SD卡，获取内部存储的路径
//            String directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
            String pathName = Environment.getExternalStorageDirectory().getAbsolutePath() + fileName;
            Log.e(TAG, "pathName = " + pathName); // pathName = /storage/emulated/0/1880fa21d2d7ddb1.epub
            file = new File(pathName);
            if (file.exists()) {    // 如果文件已经存在,获取文件已经下载的进度
                downloadedLength = file.length();
                Log.e(TAG, "downloadedLength = " + downloadedLength);
                // 如果文件存在就点击读取epub文件，如果文件不存在就下载文件
//                readEpub(pathName);
            }

            long contentLength = getContentLength(downloadUrl);
            if (contentLength == 0) {
                return TYPE_FAILED;
            } else if (contentLength == downloadedLength) {   // 如果下载的文件的大小 == 源文件的大小 那么说明下载成功；
                return TYPE_SUCCESS;
            }


//            if (isFirst){
//                Log.e(TAG, "contentLength = " + contentLength);  // E/DownloadTask: contentLength = 219380
//                if (contentLength == 0) {
//                    return TYPE_FAILED;
//                } else if (contentLength == downloadedLength) {   // 如果下载的文件的大小 == 源文件的大小 那么说明下载成功；
//                    return TYPE_SUCCESS;
//                }
//                isFirst = false;
//            }else {
//                Log.e(TAG, "contentLength = " + contentLength);
//                if (contentLength == 0) {
//                    return TYPE_FAILED;
//                } else if (contentLength == downloadedLength) {   // 如果下载的文件的大小 == 源文件的大小 那么说明下载成功；
//                    return TYPE_SUCCESS;
//                }
//            }


            // 以上排除成功和失败，那么还有两种情况，暂停和取消， 这就需要断点续传
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    //断点下载指定从哪个字节开始下载
                    .addHeader("RANGE", "bytes=" + downloadedLength + "-")
                    .url(downloadUrl)
                    .build();
            Response response = client.newCall(request).execute();
            if (response != null) {
                is = response.body().byteStream();
                savedFile = new RandomAccessFile(file, "rw");
                savedFile.seek(downloadedLength);   //跳过已经下载好的字节；
                byte[] b = new byte[1024];
                int total = 0;      // 全部的
                int len = 0;
                while ((len = is.read(b)) != -1) {
                    if (isCanceled) {
                        return TYPE_CANCELED;       //取消
                    } else if (isPaused) {
                        return TYPE_PAUSED;         //暂停
                    } else {
                        total += len;
                        savedFile.write(b, 0, len);
                        // 计算已经下载好的百分比
                        progress = (int) ((total + downloadedLength) * 100 / contentLength);
                        Log.e(TAG, "下载好的百分比 progress = " + progress);  // progress的值为0到100
                        publishProgress(progress);  //更新界面
                    }
                }
                response.body().close();
                return TYPE_SUCCESS;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
                if (savedFile != null) {
                    savedFile.close();
                }
                if (isCanceled && file != null) {
                    file.delete();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return TYPE_FAILED;
    }


    /**
     * 获取下载文件的长度
     */
    private long getContentLength(String downloadUrl) throws IOException {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(downloadUrl)
                .build();
        Response response = client.newCall(request).execute();
        if (response != null && response.isSuccessful()) {
            long contentLength = response.body().contentLength();
            response.close();
            return contentLength;
        }
        return 0;
    }

    /**
     * 任务执行过程中，要更新UI，通过调用publishProgress()方法执行该方法、
     */
    @Override
    protected void onProgressUpdate(Integer... values) {
        Log.e(TAG, "onProgressUpdate =" + values[0]);
        int progress = values[0];
        if (progress > lastProgress) {
            downloadListener.onProgress(progress);
            lastProgress = progress;
        }
    }

    /**
     * 任务执行完毕 执行该方法，可以做关闭进度条的操作。
     */
    @Override
    protected void onPostExecute(Integer integer) {
        switch (integer) {
            case TYPE_SUCCESS:
                downloadListener.onSuccess();
                break;
            case TYPE_FAILED:
                downloadListener.onFailed();
                break;
            case TYPE_CANCELED:
                downloadListener.onCanceled();
                break;
            case TYPE_PAUSED:
                downloadListener.onPaused();
                break;
        }
    }

    public void pauseDownload() {
        isPaused = true;
    }

    public void cancelDownload() {
        isCanceled = true;
    }

}



