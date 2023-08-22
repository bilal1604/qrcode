package com.flaying_rainbows_scanner


import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.budiyev.android.codescanner.CodeScanner
import com.budiyev.android.codescanner.CodeScannerView
import com.budiyev.android.codescanner.DecodeCallback
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

class MainActivity : AppCompatActivity() {

    companion object {
        private const val CAMERA_PERMISSION_REQUEST_CODE = 101
    }

    private lateinit var codeScanner: CodeScanner
    private var mInterstitialAd: InterstitialAd? = null
    private lateinit var mAdView : AdView


    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        startscan()
        iklan()
        rescanner()
        opencreate()
        copy()

    }

    fun startscan() {
        val scannerView: CodeScannerView = findViewById(R.id.scannerView)
        codeScanner = CodeScanner(this, scannerView)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_REQUEST_CODE
            )
        }

        codeScanner.decodeCallback = DecodeCallback { result ->
            runOnUiThread {
                handleScanResult(result.text)
            }
        }

        scannerView.setOnClickListener {
            codeScanner.startPreview()
        }
    }

    override fun onResume() {
        super.onResume()
        codeScanner.startPreview()
    }

    override fun onPause() {
        codeScanner.releaseResources()
        super.onPause()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Successfully Allowed", Toast.LENGTH_SHORT).show()
                codeScanner.startPreview()
            } else {
                Toast.makeText(this, "Not allowed", Toast.LENGTH_SHORT).show()
            }
        }
    }


    var rescanClickCount = 0

    fun rescanner() {
        val rescanButton: Button = findViewById(R.id.btn_rescan)
        rescanButton.setOnClickListener {
            rescanClickCount++
            if (rescanClickCount >= 3) {
                // Jika tombol rescanner diklik 3 kali atau lebih, tampilkan interstitial
                codeScanner.startPreview()
                val scanResultTextView: TextView = findViewById(R.id.scanResultTextView)
                scanResultTextView.text = ""
                interstisial()

                // Reset hitungan klik
                rescanClickCount = 0
            } else {
                // Jika tombol rescanner diklik kurang dari 3 kali, lanjutkan rescan
                codeScanner.startPreview()
                val scanResultTextView: TextView = findViewById(R.id.scanResultTextView)
                scanResultTextView.text = ""
            }
        }
    }

    fun copy() {
        val scanResultTextView: TextView = findViewById(R.id.scanResultTextView)
        val btncopy: Button = findViewById(R.id.btn_copy)
        btncopy.setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip: ClipData = ClipData.newPlainText("save text",scanResultTextView.text)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this,"${scanResultTextView.text}",Toast.LENGTH_SHORT).show()
            Toast.makeText(this,"Copy Successfully",Toast.LENGTH_SHORT).show()
        }
    }


    fun iklan(){

        MobileAds.initialize(this) {}

        mAdView = findViewById(R.id.adview)
        val adRequest = AdRequest.Builder().build()
        mAdView.loadAd(adRequest)
    }

    fun interstisial() {
        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(this,"ca-app-pub-8604728728100888/8831455986", adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                adError.toString().let { Log.d(ContentValues.TAG, it) }
                mInterstitialAd = null
            }

            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                Log.d(ContentValues.TAG, "Ad was loaded.")
                mInterstitialAd = interstitialAd
            }
        })
        if (mInterstitialAd != null) {
            mInterstitialAd?.show(this)
        } else {
            Log.d("TAG", "The interstitial ad wasn't ready yet.")
        }
    }

    private fun handleScanResult(scanResult: String) {
        val scanResultTextView: TextView = findViewById(R.id.scanResultTextView)
        scanResultTextView.text = scanResult

        if (isGoogleMapsLink(scanResult)) {
            openGoogleMaps(scanResult)
        } else if (isWebsiteLink(scanResult)) {
            openWebsite(scanResult)
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
            Toast.makeText(this, "Google Maps tidak terpasang.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openWebsite(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            Toast.makeText(this, "Tidak ada aplikasi yang dapat membuka tautan ini.", Toast.LENGTH_SHORT).show()
        }
    }

    fun opencreate(){
        val btnCreateQR: Button = findViewById(R.id.btn_create_qr)
        btnCreateQR.setOnClickListener {
            val intent = Intent(this, CreateQRActivity::class.java)
            startActivity(intent)
        }

    }
}
