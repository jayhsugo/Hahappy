package com.gimmatek.hahappy

import android.content.Intent
import android.graphics.PointF
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import com.dlazaro66.qrcodereaderview.QRCodeReaderView

class ScanQRCodeActivity : AppCompatActivity(), QRCodeReaderView.OnQRCodeReadListener {

    private val qrCodeReaderView: QRCodeReaderView by lazy { findViewById<QRCodeReaderView>(R.id.qrCodeView) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ac_qrcode_view)
        qrCodeReaderView.apply {
            setOnQRCodeReadListener(this@ScanQRCodeActivity)
            setQRDecodingEnabled(true)
            setAutofocusInterval(2000L)
            setBackCamera()
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            qrCodeReaderView.startCamera()
        } catch (ex: RuntimeException) {
            AlertDialog.Builder(this)
                    .setMessage(ex.message)
                    .setPositiveButton(R.string.alert_ok) { _, _ -> this@ScanQRCodeActivity.finish() }
                    .create()
                    .show()
        }

    }

    override fun onPause() {
        super.onPause()
        qrCodeReaderView.stopCamera()
    }

    override fun onDestroy() {
        super.onDestroy()
        qrCodeReaderView.stopCamera()
    }

    override fun onQRCodeRead(text: String, points: Array<PointF>) {
        val intent = Intent()
        intent.putExtra(RESULT_DECODE, text)
        setResult(RESULT_OK, intent)
        finish()
    }

    companion object {
        const val RESULT_DECODE = "uuid"
    }
}
