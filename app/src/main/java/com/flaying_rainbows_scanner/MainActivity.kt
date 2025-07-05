package com.flaying_rainbows_scanner

import android.Manifest
import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.budiyev.android.codescanner.CodeScanner
import com.budiyev.android.codescanner.CodeScannerView
import com.budiyev.android.codescanner.DecodeCallback
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

import android.provider.MediaStore
import android.graphics.BitmapFactory
import androidx.activity.result.contract.ActivityResultContracts
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage


class MainActivity : AppCompatActivity() {

    companion object {
        private const val CAMERA_PERMISSION_REQUEST_CODE = 101
    }

    private lateinit var codeScanner: CodeScanner
    private var mInterstitialAd: InterstitialAd? = null
    private lateinit var mAdView: AdView

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        requestCameraPermission()
        setupAds()
        setupMenuPopup()

        // Tambahkan listener untuk ikon upload dari galeri
        findViewById<ImageView>(R.id.menuUploadQR)?.setOnClickListener {
            requestGalleryPermission()
        }

    }

    // Cek & minta izin untuk akses galeri
    private fun requestGalleryPermission() {
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_MEDIA_IMAGES),
                    102
                )
            } else {
                openGallery()
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    102
                )
            } else {
                openGallery()
            }
        }
    }

    // Buka galeri untuk memilih gambar
    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryLauncher.launch(intent)
    }

    // Launcher untuk memilih gambar dari galeri
    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val imageUri: Uri? = result.data?.data
            imageUri?.let {
                processImageFromUri(it)
            }
        }
    }
    // Proses gambar dari galeri menggunakan ML Kit
    private fun processImageFromUri(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            val image = InputImage.fromBitmap(bitmap, 0)

            val options = BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                .build()

            val scanner = BarcodeScanning.getClient(options)
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    if (barcodes.isNotEmpty()) {
                        val result = barcodes.first().rawValue ?: "Cannot read QR"
                        handleScanResult(result)
                    } else {
                        Toast.makeText(this, "QR Code not found in image", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to process image", Toast.LENGTH_SHORT).show()
                }

        } catch (e: Exception) {
            Toast.makeText(this, "An error occurred while reading the image", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }



    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        if (!hasCameraPermission()) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_REQUEST_CODE
            )
        } else {
            setupScanner()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Camera permission granted", Toast.LENGTH_SHORT).show()
                setupScanner()
            } else {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
            }

        } else if (requestCode == 102) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openGallery()
            } else {
                Toast.makeText(this, "Gallery permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun setupScanner() {
        val scannerView: CodeScannerView = findViewById(R.id.scannerView)
        codeScanner = CodeScanner(this, scannerView)

        codeScanner.decodeCallback = DecodeCallback { result ->
            runOnUiThread {
                handleScanResult(result.text)
            }
        }

        scannerView.setOnClickListener {
            codeScanner.startPreview()
        }

        codeScanner.startPreview()
    }

    override fun onResume() {
        super.onResume()
        if (hasCameraPermission()) {
            val scannerView: CodeScannerView = findViewById(R.id.scannerView)
            scannerView.postDelayed({
                if (::codeScanner.isInitialized) {
                    findViewById<TextView>(R.id.scanResultTextView).text = "" // Clear result
                    codeScanner.startPreview()
                }
            }, 500) // Delay untuk pastikan view siap
        }
    }

    override fun onPause() {
        if (::codeScanner.isInitialized) {
            codeScanner.releaseResources()
        }
        super.onPause()
    }

    @SuppressLint("SetTextI18n")
    private fun handleScanResult(scanResult: String) {
        Log.d("SCAN_RESULT", "Hasil scan: $scanResult")

        val scanResultTextView: TextView = findViewById(R.id.scanResultTextView)
        scanResultTextView.text = "Scan Result : $scanResult"

        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Scan Result", scanResult)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "Copied to clipboard: $scanResult", Toast.LENGTH_SHORT).show()

        when {
            isGoogleMapsLink(scanResult) -> openGoogleMaps(scanResult)
            isWebsiteLink(scanResult) -> openWebsite(scanResult)
        }
    }

    private fun isGoogleMapsLink(text: String): Boolean {
        return text.startsWith("https://maps.google.com") || text.startsWith("geo:")
    }

    private fun isWebsiteLink(text: String): Boolean {
        return text.startsWith("http://") || text.startsWith("https://")
    }

    private fun openGoogleMaps(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            Toast.makeText(this, "Google Maps is not installed.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openWebsite(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            Toast.makeText(this, "No application can open this link.", Toast.LENGTH_SHORT).show()
        }
    }










    private fun setupAds() {
        MobileAds.initialize(this) {}
        mAdView = findViewById(R.id.adview)
        val adRequest = AdRequest.Builder().build()
        mAdView.loadAd(adRequest)
    }

    private fun showInterstitialAd() {
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(this, "ca-app-pub-8604728728100888/8831455986", adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    mInterstitialAd = interstitialAd
                    interstitialAd.show(this@MainActivity)
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.d("ADS", "Failed to load ad: ${adError.message}")
                    mInterstitialAd = null
                }
            })
    }

    private fun setupMenuPopup() {
        val menuIcon: ImageView = findViewById(R.id.menuIcon)

        menuIcon.setOnClickListener {
            val popup = PopupMenu(this, menuIcon)
            popup.menuInflater.inflate(R.menu.main_menu, popup.menu)
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.menu_rescan -> {
                        showInterstitialAd()
                        if (::codeScanner.isInitialized) {
                            findViewById<TextView>(R.id.scanResultTextView).text = ""
                            codeScanner.startPreview()
                        }
                        Toast.makeText(this, "Rescan", Toast.LENGTH_SHORT).show()
                        true
                    }

                    R.id.menu_copy -> {
                        val result = findViewById<TextView>(R.id.scanResultTextView).text
                        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("QR Result", result)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(this, "Copied: $result", Toast.LENGTH_SHORT).show()
                        true
                    }

                    R.id.menu_create_qr -> {
                        startActivity(Intent(this, CreateQRActivity::class.java))
                        true
                    }

                    else -> false
                }
            }
            popup.show()
        }
    }
}
