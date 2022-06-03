package com.example.poc_nfc

import android.app.PendingIntent
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.NfcV
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import java.io.IOException
import java.util.Locale
import kotlin.experimental.and

class MainActivity : AppCompatActivity() {

    private var nfcAdapter: NfcAdapter? = null
    private var nfcIntent: PendingIntent? = null

    private lateinit var nfcText: TextView
    private lateinit var puckIdText: TextView
    private lateinit var clearBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        if (nfcAdapter == null) {
            Toast.makeText(this, "NFC not supported", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        nfcIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, this.javaClass)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0
        )

        nfcText = findViewById(R.id.nfc_reads_value)
        puckIdText = findViewById(R.id.puck_id_value)
        clearBtn = findViewById(R.id.clear)
        clearBtn.setOnClickListener {
            nfcText.text = ""
            puckIdText.text = ""
        }
    }

    override fun onResume() {
        super.onResume()

        nfcAdapter?.enableForegroundDispatch(this, nfcIntent, null, null)
    }

    override fun onPause() {
        super.onPause()

        nfcAdapter?.disableForegroundDispatch(this)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        if (NfcAdapter.ACTION_NDEF_DISCOVERED == intent?.action ||
            NfcAdapter.ACTION_TAG_DISCOVERED == intent?.action ||
            NfcAdapter.ACTION_TECH_DISCOVERED == intent?.action
        ) {
            try {
                intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)?.let { tag: Tag ->
                    val nfcV = NfcV.get(tag)
                    nfcV.connect()
                    val cmdInfo = byteArrayOf(
                        0x20.toByte(),
                        0x2B.toByte(),
                        0x00.toByte(),
                        0x00.toByte(),
                        0x00.toByte(),
                        0x00.toByte(),
                        0x00.toByte(),
                        0x00.toByte(),
                        0x00.toByte(),
                        0x00.toByte()
                    )
                    System.arraycopy(tag.id, 0, cmdInfo, 2, 8)
                    val answer = nfcV.transceive(cmdInfo)

                    val sb = StringBuffer()
                    for (i in answer.indices.reversed()) {
                        sb.append(((answer[i].toInt() and 0xff) + 0x100).toString(16).substring(1))
                    }

                    val read = sb.toString().uppercase()
                    nfcText.text = read

                    // a Puck Id starts with "E00" and has 16 characters of length
                    puckIdText.text = read.subSequence(read.indexOf("E00"), read.indexOf("E00") + 16)
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
}
