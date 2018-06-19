package com.foretree.example

import android.Manifest
import android.os.Bundle
import android.os.Environment
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.foretree.media.MP3Recorder
import kotlinx.android.synthetic.main.activity_main.*
import pub.devrel.easypermissions.EasyPermissions
import pub.devrel.easypermissions.PermissionRequest
import java.io.File
import java.util.*

class MainActivity : AppCompatActivity() {
    private var mRecorder: MP3Recorder? = null
    private var mDir: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        EasyPermissions.requestPermissions(PermissionRequest.Builder(this,
                123,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.WRITE_EXTERNAL_STORAGE).build())
        mDir = Environment.getExternalStorageDirectory().absolutePath + "/DCIM/recordaudio"
        var file = File(mDir)
        if (!file.exists()) file.mkdirs()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }


    fun onStartRecorder(view: View) {
        val filePath = mDir + "/" + Calendar.getInstance().timeInMillis + ".mp3"
        tv.setText(filePath)
        mRecorder = MP3Recorder(filePath)
        mRecorder?.start()
    }

    fun onStopRecorder(view: View) {
        mRecorder?.stop()
        mRecorder = null
    }
}
