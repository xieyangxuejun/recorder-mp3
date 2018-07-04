package com.foretree.media;

/**
 * Created by silen on 04/07/2018
 */
public interface OnRecordListener {
    void onRecordStart();
    void onRecordStop(String saveFilePath);
    void onRecordError();
}
