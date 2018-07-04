# recorder-mp3

[![](https://jitpack.io/v/xieyangxuejun/recorder-mp3.svg)](https://jitpack.io/#xieyangxuejun/recorder-mp3)

## Update
- (18/7/3)add record timer

## Introduction
this sample demonstrates create a recording by mp3lame

## Usage
import this
```
allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
}```

```
dependencies {
	        implementation 'com.github.xieyangxuejun:recorder-mp3:1.0.0'
}```

```
mRecorder = MP3Recorder(filePath)
mRecorder?.setOnRecordUpdateListener { decibel, millis ->
    Log.d("xy=====>", millis.toString())
    tv_time.setText(RecordUtils.format(millis))
}
mRecorder?.start()
```

## Build mp3lame
- [libmp3lame-android](https://github.com/xieyangxuejun/libmp3lame-android)

## Screenshots

![](./screenshot.png)