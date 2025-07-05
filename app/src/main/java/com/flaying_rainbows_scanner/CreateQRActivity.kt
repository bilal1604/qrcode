package com.flaying_rainbows_scanner

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.zxing.BarcodeFormat
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import java.io.OutputStream

class CreateQRActivity : AppCompatActivity() {

    private lateinit var qrEditText: EditText
    private lateinit var previewImageView: ImageView
    private lateinit var previewButton: Button
    private lateinit var saveButton: Button
    private lateinit var adView: AdView
    private lateinit var adView1: AdView

    companion object {
        private const val REQUEST_CODE_PERMISSION = 123
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        setContentView(R.layout.activity_create_qractivity)

        qrEditText = findViewById(R.id.text_iput_qr)
        previewImageView = findViewById(R.id.previewImageView)
        previewButton = findViewById(R.id.previewButton)
        saveButton = findViewById(R.id.saveButton)

        // Inisialisasi AdMob
        MobileAds.initialize(this) {}

        adView = findViewById(R.id.adviewcreate)
        adView1 = findViewById(R.id.adviewcreate1)
        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)
        adView1.loadAd(adRequest)

        previewButton.setOnClickListener {
            val textToEncode = qrEditText.text.toString()
            if (textToEncode.isEmpty()) {
                Toast.makeText(this, "Input text must be filled first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val qrCodeBitmap = generateQRCodeBitmap(textToEncode)
            previewImageView.setImageBitmap(qrCodeBitmap)
            previewImageView.visibility = View.VISIBLE
        }

        saveButton.setOnClickListener {
            val textToEncode = qrEditText.text.toString()
            if (textToEncode.isEmpty()) {
                Toast.makeText(this, "Input text must be filled first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Android 9 (API 28) butuh permission WRITE_EXTERNAL_STORAGE
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                if (ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                        REQUEST_CODE_PERMISSION
                    )
                    return@setOnClickListener
                }
            }

            val qrCodeBitmap = generateQRCodeBitmap(textToEncode)
            saveImageToGallery(qrCodeBitmap)
            previewImageView.visibility = View.GONE
            qrEditText.text.clear()
        }
    }

    @SuppressLint("UseKtx")
    private fun generateQRCodeBitmap(data: String): Bitmap {
        val width = 600
        val height = 600
        val qrCodeWriter = QRCodeWriter()
        return try {
            val bitMatrix: BitMatrix =
                qrCodeWriter.encode(data, BarcodeFormat.QR_CODE, width, height)
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(
                        x,
                        y,
                        if (bitMatrix[x, y]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
                    )
                }
            }
            bitmap
        } catch (e: WriterException) {
            Toast.makeText(this, "Failed to generate QR Code", Toast.LENGTH_SHORT).show()
            Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        }
    }

    private fun saveImageToGallery(bitmap: Bitmap) {
        val filename = "QR_Code_${System.currentTimeMillis()}.png"
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/QRGenerator")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val resolver = contentResolver
        val imageUri = resolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        )

        if (imageUri != null) {
            val outputStream: OutputStream? = resolver.openOutputStream(imageUri)
            outputStream?.use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(imageUri, contentValues, null, null)
            }

            Toast.makeText(this, "Saved to Gallery!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Failed to save image", Toast.LENGTH_SHORT).show()
        }
    }

    // Optional: handle permission result
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSION) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                saveButton.performClick() // Retry save action
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
