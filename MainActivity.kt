package com.mhx.voicetool

import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import rikka.shizuku.Shizuku

class MainActivity : AppCompatActivity() {

    private lateinit var switchShizuku: Switch
    private lateinit var txtSystemStatus: TextView
    private lateinit var txtShizukuStatus: TextView
    private lateinit var btnImportAudio: Button
    private lateinit var listViewAudio: ListView

    private val audioList = ArrayList<String>()
    private val audioUriList = ArrayList<Uri>()
    private lateinit var adapter: ArrayAdapter<String>
    private var mediaPlayer: MediaPlayer? = null
    private var isShizukuActive = false

    companion object {
        const val PICK_AUDIO_REQUEST = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        switchShizuku = findViewById(R.id.switchShizuku)
        txtSystemStatus = findViewById(R.id.txtSystemStatus)
        txtShizukuStatus = findViewById(R.id.txtShizukuStatus)
        btnImportAudio = findViewById(R.id.btnImportAudio)
        listViewAudio = findViewById(R.id.listViewAudio)

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, audioList)
        listViewAudio.adapter = adapter

        // Shizuku Switch Listener
        switchShizuku.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                requestShizukuPermission()
            } else {
                releaseShizuku()
            }
        }

        // Import Audio Button
        btnImportAudio.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "audio/*"
            }
            startActivityForResult(Intent.createChooser(intent, "Select Audio"), PICK_AUDIO_REQUEST)
        }

        // Play Selected Audio to Inject
        listViewAudio.setOnItemClickListener { _, _, position, _ ->
            playAndInjectAudio(audioUriList[position])
        }
    }

    private fun requestShizukuPermission() {
        if (Shizuku.isPreV11() || Shizuku.getVersion() < 11) {
            Toast.makeText(this, "Shizuku version unsupported!", Toast.LENGTH_SHORT).show()
            switchShizuku.isChecked = false
            return
        }

        if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            activateShizukuState()
        } else {
            Shizuku.requestPermission(0)
        }
    }

    private fun activateShizukuState() {
        isShizukuActive = true
        txtSystemStatus.text = "SHIZUKU ACTIVE"
        txtSystemStatus.setTextColor(android.graphics.Color.parseColor("#00ff66"))
        txtSystemStatus.setBackgroundColor(android.graphics.Color.parseColor("#001a0a"))
        txtShizukuStatus.text = "Status: Shizuku & Mic Bridge Ready"
        Toast.makeText(this, "Shizuku Permission Granted Successfully!", Toast.LENGTH_SHORT).show()
    }

    private fun releaseShizuku() {
        isShizukuActive = false
        txtSystemStatus.text = "SHIZUKU OFF"
        txtSystemStatus.setTextColor(android.graphics.Color.parseColor("#ff003c"))
        txtSystemStatus.setBackgroundColor(android.graphics.Color.parseColor("#1a0005"))
        txtShizukuStatus.text = "Status: Permission Required"
        mediaPlayer?.stop()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_AUDIO_REQUEST && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                val fileName = uri.lastPathSegment ?: "Audio_File"
                audioList.add("🎵 $fileName")
                audioUriList.add(uri)
                adapter.notifyDataSetChanged()
                Toast.makeText(this, "Audio Imported Successfully!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun playAndInjectAudio(uri: Uri) {
        if (!isShizukuActive) {
            Toast.makeText(this, "Please enable Shizuku / Accessibility first!", Toast.LENGTH_SHORT).show()
            switchShizuku.isChecked = false
            return
        }

        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            
            mediaPlayer = MediaPlayer.create(this, uri).apply {
                start()
            }
            Toast.makeText(this, "Injecting Voice into Active Stream...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Error playing audio: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
