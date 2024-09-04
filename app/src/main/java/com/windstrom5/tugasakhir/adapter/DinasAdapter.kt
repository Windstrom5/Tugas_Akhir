package com.windstrom5.tugasakhir.adapter

import android.content.Context
import android.os.Bundle
import android.os.Environment
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
import com.itextpdf.html2pdf.ConverterProperties
import com.itextpdf.html2pdf.HtmlConverter
import com.windstrom5.tugasakhir.R
import com.windstrom5.tugasakhir.feature.PreviewDialogFragment
import com.windstrom5.tugasakhir.model.DinasItem
import com.windstrom5.tugasakhir.model.Perusahaan
import com.windstrom5.tugasakhir.model.historyDinas
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DinasAdapter(
    private val perusahaan: Perusahaan,
    private val context: Context,
    private var statusWithDinasList: List<historyDinas>,
    private val Role: String
) : BaseExpandableListAdapter() {
    private val rotationAngleExpanded = 180f
    private val rotationAngleCollapsed = 0f
    private var originalStatusWithDinasList: List<historyDinas> = statusWithDinasList.toList()

    fun filterData(query: String) {
        val lowerCaseQuery = query.toLowerCase()

        statusWithDinasList = if (lowerCaseQuery.isEmpty()) {
            originalStatusWithDinasList
        } else {
            originalStatusWithDinasList.filter { historyDinas ->
                historyDinas.dinasList.any { dinas ->
                    dinas.id.toString().contains(lowerCaseQuery)
                }
            }
        }

        // Notify the adapter with the filtered data
        notifyDataSetChanged()
    }

    override fun getGroupCount(): Int {
        return statusWithDinasList.size
    }

    override fun getChildrenCount(groupPosition: Int): Int {
        return statusWithDinasList[groupPosition].dinasList.size
    }

    override fun getGroup(groupPosition: Int): Any {
        return statusWithDinasList[groupPosition].status
    }

    override fun getChild(groupPosition: Int, childPosition: Int): Any {
        return statusWithDinasList[groupPosition].dinasList[childPosition]
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

    override fun getChildView(
        groupPosition: Int,
        childPosition: Int,
        isLastChild: Boolean,
        convertView: View?,
        parent: ViewGroup?
    ): View {
        val dinas = getChild(groupPosition, childPosition) as DinasItem
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.history_dinas, parent, false)
        val tanggal = view.findViewById<TextView>(R.id.tanggal)
        val tujuan = view.findViewById<TextView>(R.id.tujuan)
        val actionButton = view.findViewById<Button>(R.id.actionButton)
        val dateFormatter = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID"))
        val tanggalBerangkatFormatted = dateFormatter.format(dinas.tanggal_berangkat)
        val tanggalPulangFormatted = dateFormatter.format(dinas.tanggal_pulang)
        tanggal.text = "$tanggalBerangkatFormatted - $tanggalPulangFormatted"
        tujuan.text = dinas.tujuan
        Role.let { Log.d("Role3", it) }
        if (Role == "Admin" && dinas.status == "Pending") {
            actionButton.visibility = View.VISIBLE
            actionButton.text = "Respond \nDinas"
        } else if (Role == "Admin" && dinas.status == "Accept") {
            actionButton.visibility = View.VISIBLE
            actionButton.text = "View \nData"
        } else if (Role != "Admin" && dinas.status == "Pending") {
            actionButton.visibility = View.VISIBLE
            actionButton.text = "Edit \nData"
        } else if (Role != "Admin" && dinas.status == "Accept") {
            actionButton.visibility = View.VISIBLE
            actionButton.text = "Download \nReceipt"
        } else {
            actionButton.visibility = View.GONE
            actionButton.text = "View \nData"
        }
        actionButton.setOnClickListener {
            when (actionButton.text) {
                "Download \nReceipt" -> {
                    Log.d("izin", "clicked")
                    val htmlContent = getHtmlTemplate(dinas)
                    generatePdfFromHtml(htmlContent)
                }
                "Respond \nDinas" -> {
                    val fragmentManager = (context as AppCompatActivity).supportFragmentManager
                    val previewDialogFragment = PreviewDialogFragment()
                    val bundle = Bundle()
                    bundle.putParcelable("dinas", dinas) // Pass different object for izin
                    bundle.putString("layoutType", "dinas_layout") // Add layout type here
                    bundle.putString("category", "Respond")
                    previewDialogFragment.arguments = bundle
                    previewDialogFragment.setStyle(DialogFragment.STYLE_NORMAL, R.style.Dialog_FullWidth)
                    previewDialogFragment.show(fragmentManager, "preview_dialog")
                }

                "Edit \nData" -> {
                    Log.d("izin", "clicked")
                    val fragmentManager = (context as AppCompatActivity).supportFragmentManager
                    val previewDialogFragment = PreviewDialogFragment()
                    val bundle = Bundle()
                    bundle.putParcelable("dinas", dinas) // Pass different object for izin
                    bundle.putString("layoutType", "dinas_layout") // Add layout type here
                    bundle.putString("category", "Edit")
                    previewDialogFragment.arguments = bundle
                    previewDialogFragment.setStyle(DialogFragment.STYLE_NORMAL, R.style.Dialog_FullWidth)
                    previewDialogFragment.show(fragmentManager, "preview_dialog")
                }
                // Handle other button actions if needed
            }
        }
        return view
    }

    override fun isChildSelectable(groupPosition: Int, childPosition: Int): Boolean {
        return true
    }

    private fun getHtmlTemplate(dinas: DinasItem): String {
        val dateFormatter = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID"))
        val timeFormatter = SimpleDateFormat("HH:mm", Locale("id", "ID")) // Use "HH:mm" for 24-hour format
        val template = """
            <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Work Assignment Receipt for Employee</title>
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
                <p><strong>Worker Name:</strong> ${dinas.nama_pekerja}</p>
                <p><strong>Date of Assignment:</strong></p>
                <p><strong>Departure Time:</strong> ${dateFormatter.format(dinas.tanggal_berangkat)}</p>
                <p><strong>Return Time:</strong> ${dateFormatter.format(dinas.tanggal_pulang)}</p>
                <p><strong>Assigned Activity:</strong> ${dinas.kegiatan}</p>
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