package com.example.epubreader

import android.net.Uri
import org.w3c.dom.Element
import java.io.Closeable
import java.io.File
import java.io.InputStream
import java.util.zip.ZipFile
import javax.xml.parsers.DocumentBuilderFactory

/** Lee un EPUB (ZIP): localiza el OPF, el orden de lectura (spine) y sirve archivos internos. */
class EpubBook(file: File) : Closeable {
    private val zip = ZipFile(file)
    private val mimeByPath = HashMap<String, String>()
    val spine = ArrayList<String>()   // rutas dentro del ZIP, en orden de lectura
    var title: String = "EPUB"
        private set

    init {
        val containerDoc = parse("META-INF/container.xml")
        val opfPath = (containerDoc.getElementsByTagNameNS("*", "rootfile").item(0) as Element)
            .getAttribute("full-path")
        val opfDir = if (opfPath.contains("/")) opfPath.substringBeforeLast("/") + "/" else ""
        val opf = parse(opfPath)

        opf.getElementsByTagNameNS("*", "title").item(0)?.textContent?.trim()
            ?.takeIf { it.isNotEmpty() }?.let { title = it }

        val hrefById = HashMap<String, String>()
        val items = opf.getElementsByTagNameNS("*", "item")
        for (i in 0 until items.length) {
            val e = items.item(i) as Element
            val path = normalize(opfDir + Uri.decode(e.getAttribute("href")))
            hrefById[e.getAttribute("id")] = path
            mimeByPath[path] = e.getAttribute("media-type")
        }
        val refs = opf.getElementsByTagNameNS("*", "itemref")
        for (i in 0 until refs.length) {
            hrefById[(refs.item(i) as Element).getAttribute("idref")]?.let { spine.add(it) }
        }
        require(spine.isNotEmpty()) { "El EPUB no tiene páginas" }
    }

    private fun parse(path: String): org.w3c.dom.Document {
        val entry = zip.getEntry(path) ?: throw IllegalArgumentException("Falta $path")
        val f = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
        return zip.getInputStream(entry).use { f.newDocumentBuilder().parse(it) }
    }

    private fun normalize(p: String): String {
        val out = ArrayList<String>()
        for (s in p.split("/")) when (s) {
            "", "." -> {}
            ".." -> if (out.isNotEmpty()) out.removeAt(out.size - 1)
            else -> out.add(s)
        }
        return out.joinToString("/")
    }

    fun open(path: String): InputStream? = zip.getEntry(path)?.let { zip.getInputStream(it) }

    fun mime(path: String): String {
        mimeByPath[path]?.takeIf { it.isNotEmpty() }?.let { return it }
        return when (path.substringAfterLast('.', "").lowercase()) {
            "xhtml", "xht" -> "application/xhtml+xml"
            "html", "htm" -> "text/html"
            "css" -> "text/css"
            "js" -> "text/javascript"
            "svg" -> "image/svg+xml"
            "png" -> "image/png"
            "jpg", "jpeg" -> "image/jpeg"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            "woff" -> "font/woff"
            "woff2" -> "font/woff2"
            "ttf" -> "font/ttf"
            "otf" -> "font/otf"
            "mp3" -> "audio/mpeg"
            "mp4" -> "video/mp4"
            else -> "application/octet-stream"
        }
    }

    override fun close() = zip.close()
}
