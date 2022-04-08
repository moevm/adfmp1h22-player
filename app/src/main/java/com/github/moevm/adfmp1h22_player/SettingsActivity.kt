package com.github.moevm.adfmp1h22_player

import android.net.Uri
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.appcompat.widget.Toolbar
import kotlinx.android.synthetic.main.activity_settings.*

class SettingsActivity : AppCompatActivity() {

    companion object {
        const val TAG = "SettingsActivity"
    }

    fun isStoragePermissionGranted(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){
                return true
            } else {
                Log.d(TAG, "Permission is revoked")
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
                return false
            }
        } else return true
    }

    lateinit var pref: SharedPreferences

    private fun updateSaveFolderButtonName(url: Uri?) {
        var name: String? = null

        if (url != null) {
            val path = url.path
            if (path != null) {
                val spl = path.split(":")
                if (spl.size >= 2) {
                    name = spl[1]
                }
            }

            if (name == null) {
                name = url.toString()
            }
        } else {
            name = resources.getString(R.string.save_folder_not_set)
        }

        button.setText(name)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        pref = getSharedPreferences("Table", Context.MODE_PRIVATE)
        val countTracks = pref.getInt("progress", 10)
        val saveurl = pref.getString("folder", null)

        val folder = if (saveurl != null) {
            Uri.parse(saveurl)
        } else {
            null
        }

        updateSaveFolderButtonName(folder)

        seekBar.progress = countTracks
        progressSeekBar.text = seekBar.progress.toString()
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                progressSeekBar.text = p0?.progress.toString()
                pref.edit().putInt("progress", p0?.progress.toString().toInt())?.apply()
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {
            }

            override fun onStopTrackingTouch(p0: SeekBar?) {
            }

        })

        button.setOnClickListener {
            if(isStoragePermissionGranted()){
                Log.d(TAG, "OPEN FILES MANAGER")
                var intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                startActivityForResult(intent, 1)
            }
        }

        intent = Intent(this, SaveTracksActivity::class.java)
        intent.putExtra("progress", countTracks)

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when(requestCode){
            1 -> {
                if(grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    button.callOnClick()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when(requestCode){
            1 -> {
                val url = data?.getData()
                if (url != null) {
                    Log.d(TAG, "url = $url")
                    pref.edit().putString("folder", url.toString()).apply()
                    updateSaveFolderButtonName(url)
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
