package com.fxlibs.bpomcheck

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Html
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.fxlibs.bpomcheck.databinding.ActivityMainBinding
import com.fxlibs.countdown.CountDownDialog
import com.google.android.gms.ads.*
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback
import com.google.zxing.integration.android.IntentIntegrator
import com.journeyapps.barcodescanner.CaptureActivity


class MainActivity : AppCompatActivity() {

    lateinit var viewModel: MainViewModel
    lateinit var dialog : Dialog
    lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this).get(MainViewModel::class.java)
        viewModel.resultData.observe(this, Observer {result ->
            dialog.dismiss()
            when (result.status) {
                MainViewModel.Status.SUCCESS -> {
                    result.plainHtml?.let { html ->
                        val timerDialog = CountDownDialog(
                            context = this,
                            title = "Memuat informasi",
                            description = "Anda dapat mempercepat waktu dengan menonton iklan",
                            textAction = "LIHAT IKLAN",
                            onAction = {
                                rewardedAd?.show(this){}
                            },
                            onTimeout = {
                                binding.webView.loadData(html, "text/html", "UTF-8")
                            }
                        )
                        timerDialog.setActionEnable(false)
                        timerDialog.show()
                        loadRewardAds(
                            onLoaded = {
                                timerDialog.setActionEnable(true)
                                rewardedAd?.fullScreenContentCallback = object :
                                    FullScreenContentCallback() {
                                    /** Called when the ad failed to show full screen content.  */
                                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                                        timerDialog.dismiss()
                                        binding.webView.loadData(html, "text/html", "UTF-8")
                                    }

                                    /** Called when ad showed the full screen content.  */
                                    override fun onAdShowedFullScreenContent() {
                                    }

                                    /** Called when full screen content is dismissed.  */
                                    override fun onAdDismissedFullScreenContent() {
                                        timerDialog.dismiss()
                                        binding.webView.loadData(html, "text/html", "UTF-8")
                                    }
                                }
                            },
                            onFailed = {
                                timerDialog.dismiss()
                                binding.webView.loadData(html, "text/html", "UTF-8")
                            }
                        )
                    }

                }
                MainViewModel.Status.ERROR_NOT_FOUND -> {
                    val html = "<p>Maaf sitem kami tidak berhasil menemukan informasi!, Silahkan cek melalui situs berikut ini </br><a href='https://cekbpom.pom.go.id/'><b>cekbpom.pom.go.id</b> </a></>"
                    binding.webView.loadData(html, "text/html", "UTF-8")
                }
                MainViewModel.Status.ERROR_DATA -> {
                    Toast.makeText(this, "Gagal Memuat Data, Coba lagi nanti", Toast.LENGTH_SHORT)
                        .show()
                }
                else -> {
                    Toast.makeText(
                        this,
                        "Gagal Memuat Data, Periksa Koneksi Anda",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        })

        binding.btnCheck.setOnClickListener {
            binding.webView.loadData("<html></html>", "text/html", "UTF-8")
            binding.edtBPOMID.text.toString().let {
                if (it.isBlank()) {
                    Toast.makeText(this, "Mohon ini nomor BPOM dengan benar", Toast.LENGTH_SHORT).show()
                }
                else {
                    dialog = setProgressDialog(this, "Menyiapkan data").apply {
                        show()
                    }
                    viewModel.getInfo(binding.edtBPOMID.text.toString())
                }
            }

        }

        binding.btnScan.setOnClickListener {
            val scanIntegrator = IntentIntegrator(this)
            scanIntegrator.setPrompt("Scan Kode QR")
            scanIntegrator.setBeepEnabled(true)
            scanIntegrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE_TYPES)
            scanIntegrator.captureActivity = CaptureActivity::class.java
            scanIntegrator.setOrientationLocked(false)
            scanIntegrator.setBarcodeImageEnabled(true)
            scanIntegrator.initiateScan()
        }

        MobileAds.initialize(this)
        binding.adView.loadAd(AdRequest.Builder().build())
        showDialogTerm()
    }

    fun setProgressDialog(context: Context, message: String):AlertDialog {
        val llPadding = 30
        val ll = LinearLayout(context)
        ll.orientation = LinearLayout.HORIZONTAL
        ll.setPadding(llPadding, llPadding, llPadding, llPadding)
        ll.gravity = Gravity.CENTER
        var llParam = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        llParam.gravity = Gravity.CENTER
        ll.layoutParams = llParam

        val progressBar = ProgressBar(context)
        progressBar.isIndeterminate = true
        progressBar.setPadding(0, 0, llPadding, 0)
        progressBar.layoutParams = llParam

        llParam = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        llParam.gravity = Gravity.CENTER_VERTICAL
        val tvText = TextView(context)
        tvText.text = message
        tvText.setTextColor(Color.parseColor("#000000"))
        tvText.textSize = 20.toFloat()
        tvText.layoutParams = llParam

        ll.addView(progressBar)
        ll.addView(tvText)

        val builder = AlertDialog.Builder(context)
        builder.setCancelable(true)
        builder.setView(ll)
        builder.setNegativeButton("BATAL", object : DialogInterface.OnClickListener {
            override fun onClick(dialog: DialogInterface?, which: Int) {
                dialog?.dismiss()
            }
        })

        val dialog = builder.create()
        dialog.setCanceledOnTouchOutside(false)
        val window = dialog.window
        if (window != null) {
            val layoutParams = WindowManager.LayoutParams()
            layoutParams.copyFrom(dialog.window?.attributes)
            layoutParams.width = LinearLayout.LayoutParams.WRAP_CONTENT
            layoutParams.height = LinearLayout.LayoutParams.WRAP_CONTENT
            dialog.window?.attributes = layoutParams
        }
        return dialog
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val scanningResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (scanningResult != null) {
            if (scanningResult.contents != null) {
                val scanContent = scanningResult.contents.toString()
                val idx  = scanContent.indexOf("(91)")
                val kode = scanContent.substring(4, idx).toUpperCase()

                binding.webView.loadData("<html></html>", "text/html", "UTF-8")
                binding.edtBPOMID.setText(stripFirstAlpha(kode))
                binding.btnCheck.callOnClick()

            }
        }
    }

    fun stripFirstAlpha(text:String) : String {
        val sb = StringBuilder()
        text.toCharArray().forEach {
            if (sb.isEmpty() && it.toString().matches(Regex("[A-Z]"))) {

            }
            else {
                sb.append(it)
            }
        }
        return sb.toString()
    }

    var rewardedAd: RewardedInterstitialAd? = null
    private fun loadRewardAds(onLoaded:(ad:RewardedInterstitialAd) -> Unit, onFailed:() -> Unit) {
        RewardedInterstitialAd.load(this@MainActivity,
            resources.getString(R.string.ads_unit_reward),
            AdRequest.Builder().build(),
            object : RewardedInterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedInterstitialAd) {
                    rewardedAd = ad
                    onLoaded(ad)
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError?) {
                    onFailed()
                }
            })
    }

    private fun showDialogTerm() {
        if (this.getSharedPreferences("SYS", Context.MODE_PRIVATE).getBoolean("TERM_AGREE", false)) {
            return
        }

        var builder = AlertDialog.Builder(this)
        builder.setOnCancelListener {
            finish()
        }
        var view = LayoutInflater.from(this).inflate(R.layout.term_of_service, null)
        view.findViewById<TextView>(R.id.txtTerm).text = Html.fromHtml(
            getTerms(getString(R.string.app_name)).replace(
                "\n",
                ""
            )
        )
        view.findViewById<CheckBox>(R.id.chkAgree).setOnCheckedChangeListener(
            CompoundButton.OnCheckedChangeListener { btn, isChecked ->
                view.findViewById<Button>(R.id.btnNext).isEnabled = isChecked
            })
        builder.setView(view)
        var dialog = builder.create()
        view.findViewById<Button>(R.id.btnNext).setOnClickListener {
            this.getSharedPreferences("SYS", Context.MODE_PRIVATE)
                .edit()
                .putBoolean("TERM_AGREE", true).commit()
            dialog.dismiss() }
        dialog.show()

    }

    fun getTerms(appName: String) : String {
        return "<h2> <b> Persyaratan dan Ketentuan </b> </h2>\n" +
                "<p> Selamat datang di $appName </p>\n" +
                "<p> Syarat dan ketentuan ini menguraikan aturan dan ketentuan penggunaan aplikasi $appName, yang terletak di google play store </p>\n" +
                "<p> Dengan mengakses aplikasi ini, kami menganggap Anda menerima syarat dan ketentuan ini. Jangan terus menggunakan aplikasi $appName jika Anda tidak setuju untuk mengikuti semua syarat dan ketentuan yang tercantum di halaman ini. </p>\n" +
                "<p> Terminologi berikut berlaku untuk Syarat dan Ketentuan, Pernyataan Privasi dan Pemberitahuan Sanggahan dan semua Perjanjian: \"Klien\", \"Anda\" dan \"Milik Anda\" mengacu pada Anda, orang yang menggunakan aplikasi ini dan mematuhi persyaratan Perusahaan dan kondisi. \"Perusahaan\", \"Diri Kami\", \"Kami\", \"Milik Kami\", dan \"Kami\", mengacu pada Perusahaan kami. \"Pihak\", \"Pihak\", atau \"Kami\", mengacu pada Klien dan diri kami sendiri. Semua istilah mengacu pada penawaran, penerimaan dan pertimbangan pembayaran yang diperlukan untuk melakukan proses bantuan kami kepada Klien dengan cara yang paling tepat untuk tujuan yang jelas untuk memenuhi kebutuhan Klien sehubungan dengan penyediaan layanan yang dinyatakan Perusahaan, sesuai dengan dan tunduk pada, hukum yang berlaku di Indonesia. Setiap penggunaan terminologi di atas atau kata lain dalam bentuk tunggal, jamak, huruf besar dan / atau dia, dianggap dapat dipertukarkan dan oleh karena itu merujuk pada yang sama.\n" +
                "\n" +
                "<h3><li><b>Paket Data Internet</b></i></h3>\n" +
                "<p> Kami menggunakan penggunaan paket data internet. Dengan mengakses $appName, Anda setuju untuk memperbolehkan kami menggunakan paket data internet sesuai dengan Kebijakan Privasi $appName. </p>\n" +
                "<p> Aplikasi ini membutuhkan koneksi internet untuk dapat menampilkan informasi dan iklan</p>\n" +
                "\n" +
                "<h3><li><b>Local Preference</b></i> </h3>\n" +
                "<p> Kami menggunakan penggunaan local preference. Dengan mengakses $appName, Anda setuju untuk memperbolehkan kami menggunakan lokal preference sesuai dengan Kebijakan Privasi $appName. </p>\n" +
                "<p> Lokal preference digunakan untuk mengingat input pengguna untuk setiap penggunaan layanan, dengan begitu pengguna tidak perlu repot untuk mengisi kembali saat akan digunakan pada sesi berikutnya. </p>\n" +
                "\n" +
                "<h3><li><b>License</b></i> </h3>\n" +
                "<p> $appName dan / atau pemberi lisensinya memiliki hak kekayaan intelektual untuk semua materi di $appName. Semua hak kekayaan intelektual dilindungi. Anda dapat mengakses ini dari $appName untuk penggunaan pribadi dan Anda dengan tunduk pada batasan yang ditetapkan dalam syarat dan ketentuan ini. </p>\n" +
                "\n" +
                "<p> <b>Anda tidak diperbolehkan untuk:</b> </p>\n" +
                "<ul>\n" +
                "    <li> Publikasikan ulang materi dari $appName </li>" +
                "    <li> Menjual, menyewakan atau mensublisensikan materi dari $appName </li>\n" +
                "    <li> Mereproduksi, menggandakan, atau menyalin materi dari $appName </li>\n" +
                "    <li> Mendistribusikan kembali konten dari $appName </li>\n" +
                "</ul>\n" +
                "\n" +
                "<p> <b>Perjanjian ini akan dimulai pada tanggal Perjanjian ini.</b> </p>\n" +
                "\n" +
                "<p> Bagian dari aplikasi ini menawarkan kesempatan bagi pengguna untuk mendapatkan informasi mengenai kekuatan sinyal dari modem secara realtime. Kami tidak akan bertanggung jawab atas Komentar atau kewajiban, kerusakan atau biaya yang disebabkan dan / atau diderita sebagai akibat dari penggunaan dan / atau tampilan dari aplikasi ini. </p>\n" +
                "\n" +
                "<p> <b>Anda menjamin dan menyatakan bahwa:</b> </p>\n" +
                "\n" +
                "<ul>\n" +
                "<li> Anda bersedia mengisi data ID BPOM Produk pada aplikasi kami, dan aplikasi berhak untuk menggunakan informasi tersebut sebagai syarat penggunaan layanan yang dibutuhkan </li>\n" +
                "<li> Isian data pengguna tidak melanggar hak kekayaan intelektual apa pun, termasuk tanpa batasan hak cipta, paten, atau merek dagang pihak ketiga mana pun </li>\n" +
                "<li> Memperbolehkan akses penggunaan paket data internet untuk penggunaan aplikasi </li>\n" +
                "<li> Memperbolehkan akses penggunaan informasi perangkat modem </li>\n" +
                "</ul>\n" +
                "\n" +
                "<p><b>Dengan ini Anda memberi $appName lisensi non-eksklusif untuk menggunakan, mereproduksi, mengedit, dan mengizinkan orang lain untuk menggunakan, mereproduksi, dan mengedit informasi yang anda masukan kedalam aplikasi dalam segala bentuk, format, atau media. </b></p>\n" +
                "\n"
    }
}