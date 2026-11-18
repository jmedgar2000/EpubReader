package com.example.epubreader

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import java.io.ByteArrayInputStream
import java.io.File

class MainActivity : Activity() {

    private companion object {
        const val HOST = "epub.local"
        const val REQ_OPEN = 1
    }

    private lateinit var web: WebView
    private lateinit var label: TextView
    private var book: EpubBook? = null
    private var index = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        web = findViewById(R.id.web)
        label = findViewById(R.id.label)

        setupWebView()

        findViewById<Button>(R.id.btnOpen).setOnClickListener {
            startActivityForResult(
                Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "*/*"
                }, REQ_OPEN
            )
        }
        findViewById<Button>(R.id.btnSample).setOnClickListener {
            openStream { assets.open("ejemplo.epub") }
        }
        findViewById<Button>(R.id.btnPrev).setOnClickListener { go(index - 1) }
        findViewById<Button>(R.id.btnNext).setOnClickListener { go(index + 1) }

        // Abrir un .epub desde el administrador de archivos ("Abrir con...")
        intent?.data?.let { uri -> openStream { contentResolver.openInputStream(uri)!! } }
            ?: openStream { assets.open("ejemplo.epub") }
    }

    private fun setupWebView() {
        web.settings.apply {
            javaScriptEnabled = true            // JavaScript del libro
            domStorageEnabled = true
            allowFileAccess = false             // el libro no puede leer archivos del teléfono
            allowContentAccess = false
            mediaPlaybackRequiresUserGesture = true
            cacheMode = WebSettings.LOAD_NO_CACHE
            builtInZoomControls = false
        }

        web.webViewClient = object : WebViewClient() {
            // Sirve cada archivo del EPUB como si fuera una web: https://epub.local/<ruta>
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                val url = request.url
                val b = book
                if (url.host != HOST || b == null) {
                    // Bloquea cualquier recurso externo (red)
                    return WebResourceResponse("text/plain", "utf-8", ByteArrayInputStream(ByteArray(0)))
                }
                val path = (url.path ?: "").trimStart('/')
                val stream = b.open(path)
                    ?: return WebResourceResponse("text/plain", "utf-8", 404, "Not Found",
                        emptyMap(), ByteArrayInputStream(ByteArray(0)))
                val mime = b.mime(path)
                val enc = if (mime.startsWith("text/") || mime.contains("xml") ||
                    mime.contains("javascript") || mime.contains("svg")) "utf-8" else null
                return WebResourceResponse(mime, enc, stream)
            }

            // Enlaces internos del libro: permitidos. Externos: bloqueados.
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
                request.url.host != HOST

            @Suppress("OVERRIDE_DEPRECATION")
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean =
                Uri.parse(url).host != HOST

            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                val path = Uri.parse(url).path?.trimStart('/') ?: return
                val i = book?.spine?.indexOf(path) ?: -1
                if (i >= 0) { index = i; updateLabel() }
            }
        }
    }

    private fun openStream(source: () -> java.io.InputStream) {
        Thread {
            try {
                val f = File(cacheDir, "libro_${System.currentTimeMillis()}.epub")
                source().use { input -> f.outputStream().use { input.copyTo(it) } }
                val nuevo = EpubBook(f)
                runOnUiThread {
                    val anterior = book
                    book = nuevo
                    index = 0
                    anterior?.close()
                    cacheDir.listFiles()?.filter { it != f }?.forEach { it.delete() }
                    go(0)
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "No se pudo abrir: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    private fun go(i: Int) {
        val b = book ?: return
        if (i !in b.spine.indices) return
        index = i
        web.loadUrl("https://$HOST/" + Uri.encode(b.spine[i], "/"))
        updateLabel()
    }

    private fun updateLabel() {
        val b = book ?: return
        label.text = "${b.title}  (${index + 1}/${b.spine.size})"
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_OPEN && resultCode == RESULT_OK) {
            data?.data?.let { uri -> openStream { contentResolver.openInputStream(uri)!! } }
        }
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onBackPressed() {
        if (web.canGoBack()) web.goBack() else super.onBackPressed()
    }

    override fun onDestroy() {
        web.destroy()
        book?.close()
        super.onDestroy()
    }
}
