package com.example.jh.filedownload;

public interface DownloadListener {

    /**下载进度*/
    void onProgress(int progress);

    /**下载成功*/
    void onSuccess();

    /**下载失败 */
    void onFailed();

    /**下载暂停 */
    void onPaused();

    /**下载取消 */
    void onCanceled();


}
