package com.windstrom5.tugasakhir.adapter

import android.content.Context
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.windstrom5.tugasakhir.R
import com.windstrom5.tugasakhir.feature.PreviewDialogFragment
import com.windstrom5.tugasakhir.model.LemburItem
import com.windstrom5.tugasakhir.model.historyLembur
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.itextpdf.html2pdf.ConverterProperties
import com.itextpdf.html2pdf.HtmlConverter
import com.windstrom5.tugasakhir.model.Perusahaan
import com.windstrom5.tugasakhir.model.session_lembur
import org.json.JSONException
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.concurrent.TimeUnit

class LemburAdapter(
    private val perusahaan: Perusahaan,
    private val context: Context,
    private var statusWithLemburList: List<historyLembur>,
    private val Role:String
) : BaseExpandableListAdapter() {
    private val rotationAngleExpanded = 180f
    private val rotationAngleCollapsed = 0f
    private var originalStatusWithLemburList: List<historyLembur> = statusWithLemburList.toList()
//    private var
    fun filterData(query: String) {
        val lowerCaseQuery = query.toLowerCase()
        statusWithLemburList = if (lowerCaseQuery.isEmpty()) {
            originalStatusWithLemburList
        } else {
            originalStatusWithLemburList.filter { historyDinas ->
                historyDinas.lemburList.any { dinas ->
                    dinas.id.toString().contains(lowerCaseQuery)
                }
            }
        }

        // Notify the adapter with the filtered data
        notifyDataSetChanged()
    }
    override fun getGroupCount(): Int {
        return statusWithLemburList.size
    }

    override fun getChildrenCount(groupPosition: Int): Int {
        return statusWithLemburList[groupPosition].lemburList.size
    }

    override fun getGroup(groupPosition: Int): Any {
        return statusWithLemburList[groupPosition].status
    }

    override fun getChild(groupPosition: Int, childPosition: Int): Any {
        return statusWithLemburList[groupPosition].lemburList[childPosition]
    }

    override fun getGroupId(groupPosition: Int): Long {
        return groupPosition.toLong()
    }

    override fun getChildId(groupPosition: Int, childPosition: Int): Long {
        return childPosition.toLong()
    }

    override fun hasStableIds(): Boolean {
        return true
    }

    override fun getGroupView(
        groupPosition: Int,
        isExpanded: Boolean,
        convertView: View?,
        parent: ViewGroup?
    ): View {
        val status = getGroup(groupPosition) as String
        val view =
            convertView ?: LayoutInflater.from(context).inflate(R.layout.list_group, parent, false)
        view.findViewById<TextView>(R.id.title).text = status
        val arrowLogo = view.findViewById<ImageView>(R.id.arrowLogo)
        if (isExpanded) {
            arrowLogo.rotation = rotationAngleExpanded
        } else {
            arrowLogo.rotation = rotationAngleCollapsed
        }
        return view
    }
    override fun isChildSelectable(groupPosition: Int, childPosition: Int): Boolean {
        return true
    }
    override fun getChildView(
        groupPosition: Int,
        childPosition: Int,
        isLastChild: Boolean,
        convertView: View?,
        parent: ViewGroup?
    ): View {
        val lembur = getChild(groupPosition, childPosition) as LemburItem
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.history_lembur, parent, false)
        val tanggal = view.findViewById<TextView>(R.id.tanggal)
        val jam = view.findViewById<TextView>(R.id.jam)
        val actionButton = view.findViewById<Button>(R.id.actionButton)
        val dateFormatter = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID"))
        val timeFormatter = SimpleDateFormat("HH:mm", Locale("id", "ID")) // Use "HH:mm" for 24-hour format
        val tanggalFormatted = dateFormatter.format(lembur.tanggal)
        val jammasuk = timeFormatter.format(lembur.waktu_masuk)
        val jamkeluar = timeFormatter.format(lembur.waktu_pulang)
        tanggal.text = tanggalFormatted
        val now = Calendar.getInstance().time
        val jamMasuk = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).parse(lembur.waktu_masuk.toString()) ?: Date()
        jam.text = "$jammasuk - $jamkeluar"
        when {
            Role == "Admin" && lembur.status == "Pending" -> {
                actionButton.visibility = View.VISIBLE
                actionButton.text = "Respond \nLembur"
            }
            Role == "Admin" && lembur.status == "Accept" -> {
                actionButton.visibility = View.VISIBLE
                actionButton.text = "View \nData"
            }
            Role == "Admin" && lembur.status == "On Going" -> {
                actionButton.visibility = View.VISIBLE
                actionButton.text = "View \nData"
            }
            Role != "Admin" && lembur.status == "Pending" -> {
                actionButton.visibility = View.VISIBLE
                actionButton.text = "Edit \nData"
            }
            Role != "Admin" && lembur.status == "On Going" -> {
                val formatter = DateTimeFormatter.ofPattern("HH:mm:ss", Locale.getDefault())
                val waktuMasuk = LocalTime.parse(lembur.waktu_masuk.toString(), formatter)
                val now = LocalTime.now()
                if (now.isBefore(waktuMasuk)) {
                    val diffInSeconds = java.time.Duration.between(now, waktuMasuk).seconds

                    // Initialize the countdown timer
                    object : CountDownTimer(diffInSeconds * 1000, 1000) {
                        override fun onTick(millisUntilFinished: Long) {
                            val diffMinutes = TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished)
                            val diffHours = TimeUnit.MILLISECONDS.toHours(millisUntilFinished)
                            val diffSeconds = TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) % 60
                            val cooldownText = String.format("%02d:%02d:%02d \nRemaining", diffHours, diffMinutes % 60, diffSeconds)

                            actionButton.visibility = View.VISIBLE
                            actionButton.text = cooldownText
                            actionButton.isEnabled = false
                        }

                        override fun onFinish() {
                            // When the countdown finishes, update the button
                            actionButton.visibility = View.VISIBLE
                            actionButton.text = "Upload \nProgress"
                            actionButton.isEnabled = true
                        }
                    }.start()
                } else {
                    // Current time is on or after jam_masuk
                    actionButton.visibility = View.VISIBLE
                    actionButton.text = "Upload \nProgress"
                }
            }
            Role != "Admin" && lembur.status == "Finished" -> {
                actionButton.visibility = View.VISIBLE
                actionButton.text = "Download \nReceipt"
            }
            else -> {
                actionButton.visibility = View.GONE
            }
        }
        actionButton.setOnClickListener {
            when (actionButton.text) {
                "Download \nReceipt" -> {
                    val htmlContent = getHtmlTemplate(lembur)
                    generatePdfFromHtml(htmlContent)
                }
                "Upload \nProgress" ->{
                    Log.d("ApiResponse","Clicked")
                    lembur.id?.let { it1 ->
                        fetchSessionLemburData(it1, onSuccess = { lemburList ->
                            val fragmentManager = (context as AppCompatActivity).supportFragmentManager
                            val previewDialogFragment = PreviewDialogFragment()
                            val bundle = Bundle()
                            if (lemburList.isNullOrEmpty()) {
                                Log.e("Lembur", "No data available for the given Lembur ID")
                                bundle.putParcelable("lembur", lembur) // Pass different object for Lembur
                                bundle.putString("layoutType", "session_lembur")
                                bundle.putString("category", "session_pekerja")
                            }else{
                                bundle.putParcelableArrayList("lemburList", ArrayList(lemburList))
                                bundle.putParcelable("lembur", lembur) // Pass different object for Lembur
                                bundle.putString("layoutType", "session_lembur")
                                bundle.putString("category", "session_pekerja")
                            }
                            previewDialogFragment.arguments = bundle
                            previewDialogFragment.setStyle(DialogFragment.STYLE_NORMAL, R.style.Dialog_FullWidth)
                            previewDialogFragment.show(fragmentManager, "preview_dialog")
                        }, onError = { error ->
                            Log.e("Lembur", "Error fetching data: ${error.message}")
                            // Handle the error here
                        })
                    }
                }
                "Confirm \nRequest" ->{

                }
                "Respond \nLembur" -> {
                    Log.d("Lembur", "clicked")
                    val fragmentManager = (context as AppCompatActivity).supportFragmentManager
                    val previewDialogFragment = PreviewDialogFragment()
                    val bundle = Bundle()
                    bundle.putParcelable("lembur", lembur) // Pass different object for Lembur
                    bundle.putString("layoutType", "lembur_layout") // Add layout type here
                    bundle.putString("category","Respond")
                    previewDialogFragment.arguments = bundle
                    previewDialogFragment.setStyle(DialogFragment.STYLE_NORMAL, R.style.Dialog_FullWidth)
                    previewDialogFragment.show(fragmentManager, "preview_dialog")
                }
                "Edit \nData" ->{
                    Log.d("Lembur", "clicked")
                    val fragmentManager = (context as AppCompatActivity).supportFragmentManager
                    val previewDialogFragment = PreviewDialogFragment()
                    val bundle = Bundle()
                    bundle.putParcelable("lembur", lembur) // Pass different object for Lembur
                    bundle.putString("layoutType", "lembur_layout") // Add layout type here
                    bundle.putString("category","Edit")
                    previewDialogFragment.arguments = bundle
                    previewDialogFragment.setStyle(DialogFragment.STYLE_NORMAL, R.style.Dialog_FullWidth)
                    previewDialogFragment.show(fragmentManager, "preview_dialog")
                }
                "View \nData"->{
                    if(lembur.status == "On Going"){
                        lembur.id?.let { it1 ->
                            fetchSessionLemburData(it1, onSuccess = { lemburList ->
                                val fragmentManager = (context as AppCompatActivity).supportFragmentManager
                                val previewDialogFragment = PreviewDialogFragment()
                                val bundle = Bundle()
                                bundle.putParcelable("lembur", lembur)
                                bundle.putParcelableArrayList("lemburList", ArrayList(lemburList))
                                bundle.putString("layoutType", "session_lembur")
                                bundle.putString("category", "session_admin")
                                previewDialogFragment.arguments = bundle
                                previewDialogFragment.setStyle(DialogFragment.STYLE_NORMAL, R.style.Dialog_FullWidth)
                                previewDialogFragment.show(fragmentManager, "preview_dialog")
                            }, onError = { error ->
                                Log.e("Lembur", "Error fetching data: ${error.message}")
                                // Handle the error here
                            })
                        }
                    }
                }
                // Handle other button actions if needed
            }
        }
        return view
    }
    private fun fetchSessionLemburData(lemburId: Int, onSuccess: (List<session_lembur>) -> Unit, onError: (VolleyError) -> Unit) {
        val url = "http://192.168.1.6:8000/api/Lembur/Session/GetSession/$lemburId" // Replace with your API endpoint
        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                try {
                    val lemburList = mutableListOf<session_lembur>()
                    val dataArray = response.optJSONArray("data") // Access the "data" field as an array
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    if (dataArray != null) {
                        for (i in 0 until dataArray.length()) {
                            val jsonObject = dataArray.getJSONObject(i)
                            val jamString = jsonObject.optString("jam") // Get timestamp as string
                            val jamDate = dateFormat.parse(jamString) // Parse string to Date
                            Log.d("LemburLogTimeStamp", Date(jsonObject.optLong("jam")).toString())
                            val sessionLembur = session_lembur(
                                id = jsonObject.optInt("id"),
                                id_lembur = jsonObject.optInt("id_lembur"),
                                jam = jamDate ?: Date(0),
                                keterangan = jsonObject.optString("keterangan"),
                                bukti = jsonObject.optString("bukti"),
                                status = jsonObject.optString("status")
                            )
                            lemburList.add(sessionLembur)
                        }
                        onSuccess(lemburList)
                    } else {
                        // Handle the case where "data" is not present in the response
                        onError(VolleyError("No data found in response"))
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                    onError(VolleyError("Parsing error"))
                }
            },
            { error ->
                onError(error)
            }
        )
        Volley.newRequestQueue(context).add(jsonObjectRequest)
    }



    private fun getHtmlTemplate(lembur: LemburItem): String {
        val dateFormatter = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID"))
        val timeFormatter = SimpleDateFormat("HH:mm", Locale("id", "ID")) // Use "HH:mm" for 24-hour format
        val tanggal = dateFormatter.format(lembur.tanggal)
        val jammasuk = timeFormatter.format(lembur.waktu_masuk)
        val jamkeluar = timeFormatter.format(lembur.waktu_pulang)
        val template = """
            <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Work Overtime Receipt for Employee</title>
                    <style>
                        body {
                            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                            margin: 20px;
                            text-align: center;
                        }
                        header {
                            background-color: #4CAF50;
                            padding: 10px;
                            color: #fff;
                        }
                        h1 {
                            margin-bottom: 0;
                        }
                        .logo {
                            max-width: 100px;
                            margin: 10px auto;
                        }
                        .receipt-details {
                            margin-top: 20px;
                            text-align: left;
                        }
                        .receipt-details p {
                            margin: 5px 0;
                        }
                        footer {
                            margin-top: 50px;
                            padding-top: 10px;
                            border-top: 1px solid #ccc;
                            color: #555;
                        }
                    </style>
                </head>
                <body>
                    <header>
                        <h1>Receipt for Pekerja</h1>
                    </header>
                    <img src="http://192.168.1.6:8000/storage/${perusahaan.logo}" alt="Perusahaan Logo" class="logo">
                    <div class="receipt-details">\
                        <p><strong>Date Printed:</strong> ${
                        dateFormatter.format(
                            Date()
                        )
                    }</p>
                    <p><strong>Company Name:</strong> ${perusahaan.nama}</p>
                    <p><strong>Worker Name:</strong> ${lembur.nama_pekerja}</p>
                    <p><strong>Date Worked:</strong> ${tanggal}</p>
                    <p><strong>Check-in Time:</strong> ${jammasuk}</p>
                    <p><strong>Check-out Time:</strong> ${jamkeluar}</p>
                    <p><strong>Work Description:</strong> ${lembur.pekerjaan}</p>
                    </div>
                    <footer>
                        <p>Powered by Workhubs</p>
                    </footer>
                </body>
                </html>

        """.trimIndent()

        // Replace placeholders with actual data
        return template
    }
    private fun generatePdfFromHtml(htmlContent: String) {
        try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val outputPdfFile = File(downloadsDir, "receipt.pdf")
            val outputStream = FileOutputStream(outputPdfFile)
            val converterProperties = ConverterProperties()
            HtmlConverter.convertToPdf(htmlContent, outputStream, converterProperties)
            outputStream.close()
            Toast.makeText(context, "Receipt downloaded at ${outputPdfFile.absolutePath}", Toast.LENGTH_SHORT).show()
            // PDF is generated, you can now save it or share it as needed
        } catch (e: Exception) {
            Log.e("PDFGeneration", "Error generating PDF: ${e.message}", e)
        }
    }
}
