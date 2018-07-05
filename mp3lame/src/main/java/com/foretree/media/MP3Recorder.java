package com.foretree.media;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * create by silen
 */
public class MP3Recorder {
    private int mSampleRate = 44100;
    private int mOutChannel = 2;
    private int mOutBitRate = 32;
    private int mVolume;

    private boolean mIsRecording = false;
    private boolean mIsPause = false;
    private ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private Runnable mRunnable;
    private String mSaveFilePath = null;
    private long mTimingMillis;

    //listener
    private OnRecordUpdateListener mUpdateListener;

    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_REC_STARTED: {
                    mTimingMillis = 0L;
                    sendRecordMessage(MSG_REC_UPDATE, TIME_INTERVAL_MILLISECOND);
                    break;
                }
                case MSG_REC_STOPPED: {
                    if (mUpdateListener != null) mUpdateListener.onRecordFinish(mSaveFilePath);
                    removeMessages(MSG_REC_UPDATE);
                    break;
                }
                case MSG_REC_UPDATE: {
                    mTimingMillis += TIME_INTERVAL_MILLISECOND;
                    if (mUpdateListener != null)
                        mUpdateListener.onRecordUpdate(mVolume, mTimingMillis);
                    sendRecordMessage(MSG_REC_UPDATE, TIME_INTERVAL_MILLISECOND);
                    break;
                }
                case MSG_ERROR_GET_MIN_BUFFERSIZE:
                case MSG_ERROR_CREATE_FILE:
                case MSG_ERROR_AUDIO_RECORD:
                case MSG_ERROR_AUDIO_ENCODE:
                case MSG_ERROR_WRITE_FILE:
                case MSG_ERROR_CLOSE_FILE: {
                    removeMessages(MSG_REC_UPDATE);
                    break;
                }
            }
        }
    };

    //根据资料假定的最大值。 实测时有时超过此值。
    private static final int MAX_VOLUME = 2000;

    /**
     * 开始录音
     */
    public static final int MSG_REC_STARTED = 1;

    /**
     * 结束录音
     */
    public static final int MSG_REC_STOPPED = 2;

    /**
     * 暂停录音
     */
    public static final int MSG_REC_PAUSE = 3;

    /**
     * 继续录音
     */
    public static final int MSG_REC_RESTORE = 4;

    /**
     * 计时
     */
    public static final int MSG_REC_UPDATE = 5;
    public static final int TIME_INTERVAL_MILLISECOND = 1000;

    /**
     * 缓冲区挂了,采样率手机不支持
     */
    public static final int MSG_ERROR_GET_MIN_BUFFERSIZE = -1;

    /**
     * 创建文件时扑街了
     */
    public static final int MSG_ERROR_CREATE_FILE = -2;

    /**
     * 初始化录音器时扑街了
     */
    public static final int MSG_ERROR_REC_START = -3;

    /**
     * 录音的时候出错
     */
    public static final int MSG_ERROR_AUDIO_RECORD = -4;

    /**
     * 编码时挂了
     */
    public static final int MSG_ERROR_AUDIO_ENCODE = -5;

    /**
     * 写文件时挂了
     */
    public static final int MSG_ERROR_WRITE_FILE = -6;

    /**
     * 没法关闭文件流
     */
    public static final int MSG_ERROR_CLOSE_FILE = -7;

    public MP3Recorder(String filePath) {
        this.mSaveFilePath = filePath;
        this.mSampleRate = 44100;
    }


    public void setOnRecordUpdateListener(OnRecordUpdateListener listener) {
        this.mUpdateListener = listener;
    }

    /**
     * 开片
     */
    public void start() {
        if (mIsRecording) return;
        if (mRunnable == null) {
            mRunnable = new Runnable() {
                @Override
                public void run() {
                    android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
                    // 根据定义好的几个配置，来获取合适的缓冲大小
                    final int minBufferSize = AudioRecord.getMinBufferSize(
                            mSampleRate, AudioFormat.CHANNEL_IN_MONO,
                            AudioFormat.ENCODING_PCM_16BIT);
                    if (minBufferSize < 0) {
                        sendRecordMessage(MSG_ERROR_GET_MIN_BUFFERSIZE);
                        return;
                    }
                    AudioRecord audioRecord = new AudioRecord(
                            MediaRecorder.AudioSource.MIC, mSampleRate,
                            AudioFormat.CHANNEL_IN_MONO,
                            AudioFormat.ENCODING_PCM_16BIT, minBufferSize * 2);

                    // 5秒的缓冲
                    short[] buffer = new short[mSampleRate * (16 / 8) * 5];
                    byte[] mp3buffer = new byte[(int) (7200 + buffer.length * 2 * 1.25)];

                    FileOutputStream output = null;
                    try {
                        output = new FileOutputStream(new File(mSaveFilePath));
                    } catch (FileNotFoundException e) {
                        sendRecordMessage(MSG_ERROR_CREATE_FILE);
                        return;
                    }
                    MP3Recorder.init(mSampleRate, mOutChannel, mSampleRate, mOutBitRate);
                    mIsRecording = true; // 录音状态
                    mIsPause = false; // 录音状态
                    try {
                        try {
                            audioRecord.startRecording(); // 开启录音获取音频数据
                        } catch (IllegalStateException e) {
                            // 不给录音...
                            sendRecordMessage(MSG_ERROR_REC_START);
                            return;
                        }

                        try {
                            // 开始录音
                            sendRecordMessage(MSG_REC_STARTED);

                            int readSize = 0;
                            boolean pause = false;
                            while (mIsRecording) {
                                /*--暂停--*/
                                if (mIsPause) {
                                    if (!pause) {
                                        sendRecordMessage(MSG_REC_PAUSE);
                                        pause = true;
                                    }
                                    continue;
                                }
                                if (pause) {
                                    sendRecordMessage(MSG_REC_RESTORE);
                                    pause = false;
                                }
                                /*--End--*/
                                /*--实时录音写数据--*/
                                readSize = audioRecord.read(buffer, 0,
                                        minBufferSize);
                                if (readSize < 0) {
                                    sendRecordMessage(MSG_ERROR_AUDIO_RECORD);
                                    break;
                                } else if (readSize == 0) {
                                    ;
                                } else {
                                    //计算vo
                                    calculateRealVolume(buffer, readSize);
                                    int encResult = MP3Recorder.encode(buffer,
                                            buffer, readSize, mp3buffer);
                                    if (encResult < 0) {
                                        sendRecordMessage(MSG_ERROR_AUDIO_ENCODE);
                                        break;
                                    }
                                    if (encResult != 0) {
                                        try {
                                            output.write(mp3buffer, 0, encResult);
                                        } catch (IOException e) {
                                            sendRecordMessage(MSG_ERROR_WRITE_FILE);
                                            break;
                                        }
                                    }
                                }
                                /*--End--*/
                            }
                            /*--录音完--*/
                            int flushResult = MP3Recorder.flush(mp3buffer);
                            if (flushResult < 0) {
                                sendRecordMessage(MSG_ERROR_AUDIO_ENCODE);
                            }
                            if (flushResult != 0) {
                                try {
                                    output.write(mp3buffer, 0, flushResult);
                                } catch (IOException e) {
                                    sendRecordMessage(MSG_ERROR_WRITE_FILE);
                                }
                            }
                            try {
                                output.close();
                            } catch (IOException e) {
                                sendRecordMessage(MSG_ERROR_CLOSE_FILE);
                            }
                            /*--End--*/
                        } finally {
                            audioRecord.stop();
                            audioRecord.release();
                        }
                    } finally {
                        MP3Recorder.close();
                        mIsRecording = false;
                    }
                    sendRecordMessage(MSG_REC_STOPPED);
                }
            };
        }
        mExecutor.execute(mRunnable);
    }

    //计算音量大小
    private void calculateRealVolume(short[] buffer, int readSize) {
        double sum = 0;
        for (int i = 0; i < readSize; i++) {
            sum += buffer[i] * buffer[i];
        }
        if (readSize > 0) {
            double amplitude = sum / readSize;
            mVolume = (int) Math.sqrt(amplitude);
        }
    }

    public int getVolume() {
        if (mVolume >= MAX_VOLUME) {
            return MAX_VOLUME;
        }
        return mVolume;
    }

    public int getMaxVolume() {
        return MAX_VOLUME;
    }

    private void sendRecordMessage(int msg) {
        if (mHandler != null) {
            Message message = new Message();
            message.what = msg;
            mHandler.sendMessageDelayed(message, 0);
        }
    }

    private void sendRecordMessage(int msg, long delayMillis) {
        if (mHandler != null) {
            Message message = new Message();
            message.what = msg;
            mHandler.sendMessageDelayed(message, delayMillis);
        }
    }

    public void stop() {
        mIsRecording = false;
    }

    public void pause() {
        mIsPause = true;
    }

    public void restore() {
        mIsPause = false;
    }

    public boolean isRecording() {
        return mIsRecording;
    }

    public boolean isPaus() {
        if (!mIsRecording) {
            return false;
        }
        return mIsPause;
    }

    public String getFilePath() {
        return mSaveFilePath;
    }

    /**
     * 录音状态管理
     *
     * @see RecMicToMp3#MSG_REC_STARTED
     * @see RecMicToMp3#MSG_REC_STOPPED
     * @see RecMicToMp3#MSG_REC_PAUSE
     * @see RecMicToMp3#MSG_REC_RESTORE
     * @see RecMicToMp3#MSG_ERROR_GET_MIN_BUFFERSIZE
     * @see RecMicToMp3#MSG_ERROR_CREATE_FILE
     * @see RecMicToMp3#MSG_ERROR_REC_START
     * @see RecMicToMp3#MSG_ERROR_AUDIO_RECORD
     * @see RecMicToMp3#MSG_ERROR_AUDIO_ENCODE
     * @see RecMicToMp3#MSG_ERROR_WRITE_FILE
     * @see RecMicToMp3#MSG_ERROR_CLOSE_FILE
     */
    public void setHandle(Handler handler) {
        this.mHandler = handler;
    }

    /*--以下为Native部分--*/
    static {
        System.loadLibrary("recodermp3");
    }

    /**
     * 初始化录制参数
     */
    public static void init(int inSamplerate, int outChannel,
                            int outSamplerate, int outBitrate) {
        init(inSamplerate, outChannel, outSamplerate, outBitrate, 7);
    }

    /**
     * 初始化录制参数 quality:0=很好很慢 9=很差很快
     */
    public native static void init(int inSamplerate, int outChannel,
                                   int outSamplerate, int outBitrate, int quality);

    /**
     * 音频数据编码(PCM左进,PCM右进,MP3输出)
     */
    public native static int encode(short[] buffer_l, short[] buffer_r,
                                    int samples, byte[] mp3buf);

    /**
     * 刷干净缓冲区
     */
    public native static int flush(byte[] mp3buf);

    /**
     * 结束编码
     */
    public native static void close();

}
