package com.windstrom5.tugasakhir.connection

import android.os.AsyncTask
import com.github.barteksc.pdfviewer.PDFView
import java.io.BufferedInputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import android.widget.Toast
import java.io.IOException


class RetrievePDFfromUrl(private val pdfView: PDFView) : AsyncTask<String, Void, InputStream>() {

    override fun doInBackground(vararg strings: String): InputStream? {
        var inputStream: InputStream? = null
        try {
            val url = URL(strings[0])
            val urlConnection = url.openConnection() as HttpURLConnection
            if (urlConnection.responseCode == 200) {
                inputStream = BufferedInputStream(urlConnection.inputStream)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return inputStream
    }

    override fun onPostExecute(inputStream: InputStream?) {
        if (inputStream != null) {
            pdfView.fromStream(inputStream).load()
        } else {
            Toast.makeText(pdfView.context, "Failed to load PDF", Toast.LENGTH_SHORT).show()
        }
    }
}
