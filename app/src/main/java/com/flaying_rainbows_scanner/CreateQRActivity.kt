package com.flaying_rainbows_scanner

import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
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
    private  lateinit var shareButton: Button

    companion object {
        private const val REQUEST_CODE_PERMISSION = 123
    }
    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        setContentView(R.layout.activity_create_qractivity)


        qrEditText = findViewById(R.id.text_iput_qr)
        previewImageView = findViewById(R.id.previewImageView)
        previewButton = findViewById(R.id.previewButton)
        saveButton = findViewById(R.id.saveButton)

        previewButton.setOnClickListener {
            val textToEncode = qrEditText.text.toString()
            if (textToEncode.isEmpty()) {
                Toast.makeText(this, "Input text must be filled first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val qrCodeBitmap = generateQRCodeBitmap(textToEncode, 600, 600)
            previewImageView.setImageBitmap(qrCodeBitmap)
            previewImageView.visibility = View.VISIBLE // Menampilkan preview gambar
        }

        saveButton.setOnClickListener {
            val textToEncode = qrEditText.text.toString()
            if (textToEncode.isEmpty()) {
                Toast.makeText(this, "Input text must be filled first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                // Permission already granted, proceed with saving the image
                val qrCodeBitmap = generateQRCodeBitmap(textToEncode, 600, 600)
                if (qrCodeBitmap != null) {
                    saveImageToGallery(qrCodeBitmap)
                    previewImageView.visibility = View.GONE // Hide preview after saving
                }
                qrEditText.text.clear() // Clear the content of qrEditText after saving
            } else {
                // Permission not granted, request permission from the user
                requestPermissions(arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_CODE_PERMISSION)
            }
        }



    }




    private fun generateQRCodeBitmap(data: String, width: Int, height: Int): Bitmap? {
        val qrCodeWriter = QRCodeWriter()
        try {
            val bitMatrix: BitMatrix = qrCodeWriter.encode(data, BarcodeFormat.QR_CODE, width, height)
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt())
                }
            }
            return bitmap
        } catch (e: WriterException) {
            e.printStackTrace()
            return null
        }
    }

    @RequiresApi(30)
    private fun saveImageToGallery(bitmap: Bitmap) {
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "QR_Code_${System.currentTimeMillis()}.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        }

        val resolver = contentResolver
        val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        val item = resolver.insert(collection, contentValues)

        if (item != null) {
            val outputStream: OutputStream? = resolver.openOutputStream(item)
            if (outputStream != null) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                outputStream.close()

                Toast.makeText(this, "Save Photo Successfully To Gallery", Toast.LENGTH_SHORT).show()
                Toast.makeText(this, "Make sure the internet connection is smooth when saving photos and wait a few moments ..", Toast.LENGTH_SHORT).show()

            }
        }
    }
}
