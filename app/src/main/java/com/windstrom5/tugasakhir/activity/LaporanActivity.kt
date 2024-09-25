package com.windstrom5.tugasakhir.activity

import android.app.Dialog
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.ExpandableListView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import br.com.simplepass.loading_button_lib.customViews.CircularProgressButton
import com.android.volley.Request
import com.android.volley.toolbox.ImageRequest
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.Volley
import com.github.aachartmodel.aainfographics.aachartcreator.AAChartModel
import com.github.aachartmodel.aainfographics.aachartcreator.AAChartType
import com.github.aachartmodel.aainfographics.aachartcreator.AAChartView
import com.github.aachartmodel.aainfographics.aachartcreator.AASeriesElement
import com.github.mikephil.charting.data.Entry
import com.saadahmedev.popupdialog.PopupDialog
import com.windstrom5.tugasakhir.R
import com.windstrom5.tugasakhir.adapter.AbsenAdapter
import com.windstrom5.tugasakhir.adapter.DinasAdapter
import com.windstrom5.tugasakhir.adapter.IzinAdapter
import com.windstrom5.tugasakhir.adapter.LemburAdapter
import com.windstrom5.tugasakhir.connection.ApiService
import com.windstrom5.tugasakhir.connection.ReverseGeocoder
import com.windstrom5.tugasakhir.databinding.ActivityLaporanBinding
import com.windstrom5.tugasakhir.model.AbsenItem
import com.windstrom5.tugasakhir.model.Admin
import com.windstrom5.tugasakhir.model.DinasItem
import com.windstrom5.tugasakhir.model.IzinItem
import com.windstrom5.tugasakhir.model.LemburItem
import com.windstrom5.tugasakhir.model.Pekerja
import com.windstrom5.tugasakhir.model.Perusahaan
import com.windstrom5.tugasakhir.model.historyAbsen
import com.windstrom5.tugasakhir.model.historyDinas
import com.windstrom5.tugasakhir.model.historyIzin
import com.windstrom5.tugasakhir.model.historyLembur
import com.windstrom5.tugasakhir.model.session_lembur
import de.hdodenhof.circleimageview.CircleImageView
import okhttp3.ResponseBody
import org.apache.commons.lang3.time.DateUtils.parseDate
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.osmdroid.util.GeoPoint
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import www.sanju.motiontoast.MotionToast
import www.sanju.motiontoast.MotionToastStyle
import java.sql.Date
import java.sql.Time
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class LaporanActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLaporanBinding
    private lateinit var logo: CircleImageView
    private lateinit var name: TextView
    private lateinit var address: TextView
    private lateinit var jenis: AutoCompleteTextView
    private lateinit var data: AutoCompleteTextView
    private lateinit var bulan: AutoCompleteTextView
    private lateinit var tahun: AutoCompleteTextView
    private lateinit var pegawai: AutoCompleteTextView
    private lateinit var chartList: AutoCompleteTextView
    private lateinit var generate:CircularProgressButton
    private lateinit var chart: AAChartView
    private var bundle: Bundle? = null
    private var admin: Admin? = null
    private var izinItemList: MutableList<IzinItem> = mutableListOf()
    private var lemburItemList: MutableList<LemburItem> = mutableListOf()
    private var sesilemburItemList: MutableList<session_lembur> = mutableListOf()
    private var dinasItemList: MutableList<DinasItem> = mutableListOf()
    private var presensiItemList: MutableList<AbsenItem> = mutableListOf()
    private var perusahaan: Perusahaan? = null
    private var pekerjaList: MutableList<Pekerja> = mutableListOf()
    private lateinit var suggestionsCardView: CardView
    private lateinit var suggestionsText: TextView
    private lateinit var expand: TextView
    private lateinit var expandableLayout:LinearLayout
    private lateinit var btnToggleExpand:TextView
    private var isExpanded = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLaporanBinding.inflate(layoutInflater)
        setContentView(binding.root)
        name = binding.tvName
        logo = binding.circleImageView
        address = binding.tvAddress
        getBundle()
        jenis = binding.ACJenis
        data = binding.ACData
        expandableLayout = binding.expandableLayout
        btnToggleExpand = binding.expandCollapseButton
        bulan = binding.ACBulan
        tahun = binding.ACTahun
        pegawai = binding.ACPegawai
        chartList = binding.ACChart
        chart = binding.barChart
        generate = binding.create
        suggestionsCardView = findViewById(R.id.suggestionsCardView)
        suggestionsText = findViewById(R.id.suggestionsText)
        setupAutoCompleteTextViews()
        getData()
        btnToggleExpand.setOnClickListener {
            // Toggle the expand/collapse state
            isExpanded = !isExpanded

            if (isExpanded) {
                // Show the expandable layout
                expandableLayout.visibility = View.VISIBLE
                // Change the button text to "Collapse"
                btnToggleExpand.text = "Collapse"
            } else {
                // Hide the expandable layout
                expandableLayout.visibility = View.GONE
                // Change the button text to "Expand"
                btnToggleExpand.text = "Expand"
            }
        }
        generate.setOnClickListener {
            val selectedJenis = jenis.text.toString()
            val selectedDataShort = data.text.toString()
            val selectedData = getFullDescription(selectedJenis, selectedDataShort)
            val selectedBulan = bulan.text.toString()  // Retrieve selected month, if any
            val selectedTahun = tahun.text.toString()
            if (selectedJenis.isEmpty() || selectedData.isEmpty() || selectedBulan.isEmpty() || selectedTahun.isEmpty()) {
                showErrorDialog(selectedJenis.isEmpty(), selectedData.isEmpty(),selectedBulan.isEmpty(),selectedTahun.isEmpty())
            } else {
                generate.startAnimation()
                generateChart(selectedJenis, selectedData,selectedBulan,selectedTahun)
            }
        }

    }

    private fun processDataForChart(selectedJenis: String, selectedData: String,selectedBulan: String,selectedTahun: String): List<Entry> {
        return when (selectedJenis) {
            "lembur" -> processLemburData(selectedData,selectedBulan,selectedTahun)
//            "dinas"-> processDinasData(selectedData)
//            "izin"-> processIzinData(selectedData)
//            "presensi"-> processPresensiData(selectedData)
            else -> emptyList()
        }
    }
    private fun buildErrorMessage(isJenisEmpty: Boolean, isDataEmpty: Boolean, isBulanEmpty: Boolean, isTahunEmpty: Boolean): String {
        val missingFields = mutableListOf<String>()

        if (isJenisEmpty) {
            missingFields.add("Jenis")
        }
        if (isDataEmpty) {
            missingFields.add("Data")
        }
        if (isBulanEmpty) {
            missingFields.add("Bulan")
        }
        if (isTahunEmpty) {
            missingFields.add("Tahun")
        }

        return if (missingFields.isNotEmpty()) {
            "Please fill in the following fields: ${missingFields.joinToString(", ")}"
        } else {
            ""
        }
    }
    private fun showErrorDialog(isJenisEmpty: Boolean, isDataEmpty: Boolean, isBulanEmpty: Boolean, isTahunEmpty: Boolean) {
        val errorMessage = buildErrorMessage(isJenisEmpty, isDataEmpty, isBulanEmpty, isTahunEmpty)

        if (errorMessage.isNotEmpty()) {
            MotionToast.createColorToast(
                this, // Context
                "Error", // Title
                errorMessage, // Message
                MotionToastStyle.ERROR, // Error style
                MotionToast.GRAVITY_BOTTOM, // Position
                MotionToast.SHORT_DURATION, // Duration
                ResourcesCompat.getFont(this, R.font.ralewaybold) // Font
            )
        }
    }
    private fun processLemburData(selectedData: String, selectedBulan: String, selectedTahun: String): List<Entry> {
        // Create a mapping from month names to their corresponding numeric values
        Log.d("TestChart",selectedData+selectedBulan+selectedTahun)
        val monthMapping = mapOf(
            "January" to 1,
            "February" to 2,
            "March" to 3,
            "April" to 4,
            "May" to 5,
            "June" to 6,
            "July" to 7,
            "August" to 8,
            "September" to 9,
            "October" to 10,
            "November" to 11,
            "December" to 12
        )

        // Get the month number from the selected month name
        val selectedMonth = monthMapping[selectedBulan]

        // Convert selected year to Int (if provided)
        val selectedYear = selectedTahun.toIntOrNull()

        // Filter lembur items based on the selected month and year
        val filteredLemburItems = lemburItemList.filter { lembur ->
            val calendar = Calendar.getInstance()
            calendar.time = lembur.tanggal  // Assuming lembur.tanggal is a Date object

            val lemburMonth = calendar.get(Calendar.MONTH) + 1  // Calendar.MONTH is 0-based
            val lemburYear = calendar.get(Calendar.YEAR)

            // Filter by month and year (if provided)
            (selectedMonth == null || selectedMonth == lemburMonth) &&
                    (selectedYear == null || selectedYear == lemburYear)
        }

        return when (selectedData) {
            "total lembur" -> {
                // Group by employee and calculate total lembur duration
                filteredLemburItems.groupBy { it.id_pekerja }
                    .map { (id, lemburItems) ->
                        Entry(
                            id?.toFloat() ?: 0f,
                            lemburItems.sumOf { it.getLemburDuration().toDouble() }.toFloat()
                        )
                    }
            }
            "rata-rata lembur" -> {
                // Group by employee and calculate average lembur duration
                filteredLemburItems.groupBy { it.id_pekerja }
                    .map { (id, lemburItems) ->
                        Entry(
                            id?.toFloat() ?: 0f,
                            lemburItems.map { it.getLemburDuration() }.average().toFloat()
                        )
                    }
            }
            "distribusi lembur" -> {
                // Map each lembur item to a chart entry based on the index (or another field)
                filteredLemburItems.mapIndexed { index, lembur ->
                    Entry(index.toFloat(), lembur.getLemburDuration().toFloat())
                }
            }
            else -> emptyList()
        }
    }

    private fun generateSuggestions(
        entries: List<Entry>,
        selectedData: String,
        selectedBulan: String,
        selectedTahun: String
    ): String {
        // Use the selected month and year in suggestions
        val monthYearText = "untuk bulan $selectedBulan tahun $selectedTahun"

        return when (selectedData) {
            "total lembur" -> {
                val totalLembur = entries.map { it.y }.sum()
                val averageLembur = entries.map { it.y }.average()

                val excessiveLemburThreshold = 40 // Example threshold for excessive overtime
                val employeesWithExcessiveLembur = entries.filter { it.y > excessiveLemburThreshold }

                val excessiveMessage = if (employeesWithExcessiveLembur.isNotEmpty()) {
                    "Perhatian: ${employeesWithExcessiveLembur.size} karyawan bekerja lembur lebih dari $excessiveLemburThreshold jam $monthYearText. Pertimbangkan untuk mengevaluasi beban kerja atau menambah staf."
                } else {
                    "Tidak ada karyawan yang melebihi lembur $excessiveLemburThreshold jam $monthYearText. Pemakaian lembur masih dalam batas normal."
                }

                "Total lembur keseluruhan $monthYearText adalah $totalLembur jam.\nRata-rata lembur per karyawan adalah ${"%.2f".format(averageLembur)} jam.\n$excessiveMessage"
            }
            "rata-rata lembur" -> {
                val averageLembur = entries.map { it.y }.average()
                val belowOptimalThreshold = 5 // Example threshold for under-utilization
                val employeesWithLowLembur = entries.filter { it.y < belowOptimalThreshold }

                val underUtilizationMessage = if (employeesWithLowLembur.isNotEmpty()) {
                    "Beberapa karyawan memiliki lembur di bawah rata-rata $belowOptimalThreshold jam $monthYearText, pertimbangkan apakah tugas mereka bisa dialihkan untuk meningkatkan produktivitas."
                } else {
                    "Penggunaan lembur sudah merata di antara karyawan $monthYearText."
                }

                "Rata-rata lembur keseluruhan $monthYearText adalah ${"%.2f".format(averageLembur)} jam.\n$underUtilizationMessage"
            }
            "distribusi lembur" -> {
                "Distribusi lembur $monthYearText menunjukkan pola lembur karyawan pada berbagai waktu. Analisis ini dapat membantu untuk merencanakan lembur di masa depan dengan lebih efisien."
            }
            else -> {
                "Tidak ada data yang relevan untuk ditampilkan $monthYearText."
            }
        }
    }


    private fun LemburItem.getLemburDuration(): Int {
        val durationInMillis = waktu_pulang.time - waktu_masuk.time
        return (durationInMillis / (1000 * 60 * 60)).toInt() // Return duration in hours
    }
    private fun getEmployeeNameById(employeeId: Int?): String {
        return pekerjaList.find { it.id == employeeId }?.nama?: "Unknown"
    }
//    private fun processDinasData(selectedData: String): List<Entry> {
//        val entries = mutableListOf<Entry>()
//        when (selectedData) {
//            "total dinas" -> {
//                val totalDinasByEmployee = filteredDinasList.groupBy { it.id_pekerja }
//                totalDinasByEmployee.forEach { (id_pekerja, dinasItems) ->
//                    entries.add(Entry(id_pekerja?.toFloat() ?: 0f, dinasItems.size.toFloat()))
//                }
//            }
//            "rata-rata durasi dinas" -> {
//                val durasiByEmployee = filteredDinasList.groupBy { it.id_pekerja }
//                durasiByEmployee.forEach { (id_pekerja, dinasItems) ->
//                    val averageDuration = dinasItems.map {
//                        (it.tanggal_pulang.time - it.tanggal_berangkat.time) / (1000 * 60 * 60)
//                    }.average()
//                    entries.add(Entry(id_pekerja?.toFloat() ?: 0f, averageDuration.toFloat()))
//                }
//            }
//            "distribusi dinas" -> {
//                filteredDinasList.forEach { dinas ->
//                    val xValue = dinas.tanggal_berangkat.time.toFloat() // Use timestamp for x-axis
//                    val durationInHours = (dinas.tanggal_pulang.time - dinas.tanggal_berangkat.time) / (1000 * 60 * 60)
//                    val yValue = durationInHours.toFloat()
//                    entries.add(Entry(xValue, yValue))
//                }
//            }
//        }
//        return entries
//    }
    private fun generateChart(selectedJenis: String, selectedData: String, selectedBulan: String, selectedTahun: String) {
        val selectedPegawai = pegawai.text.toString()  // Retrieve selected employee or check if empty
        val selectedChartType = chartList.text.toString()  // Retrieve selected chart type

        val entries = processLemburData(selectedData, selectedBulan, selectedTahun)  // Fetch relevant entries
        Log.d("TestChart",selectedData+selectedBulan+selectedTahun)
        val suggestions = generateSuggestions(entries, selectedData, selectedBulan, selectedTahun)
        suggestionsCardView = findViewById(R.id.suggestionsCardView)
        suggestionsText = findViewById(R.id.suggestionsText)
        suggestionsText.text = suggestions
        suggestionsCardView.visibility = if (suggestions.isNotEmpty()) View.VISIBLE else View.GONE

            // Determine the chart type, defaulting to Bar if none selected
        val chartType = when (selectedChartType) {
            "Bar" -> AAChartType.Bar
            "Line" -> AAChartType.Line
            "Pie" -> AAChartType.Pie
            "Scatter" -> AAChartType.Scatter
            "Polygon" -> AAChartType.Polygon  // Since Heat Map isn't directly available
            else -> AAChartType.Bar  // Default to Bar if none selected
        }


    // Convert x-values (which are employee IDs) to employee names for categories
        val categories = entries.map { entry ->
            getEmployeeNameById(entry.x.toInt())  // Convert employee ID (x) to name
        }.toTypedArray()

        // Map the y-values for the chart data
        val data = entries.map { it.y }.toTypedArray()

        // Construct the chart title dynamically based on selected options
        val chartTitle = buildChartTitle(selectedJenis, selectedData, selectedBulan, selectedTahun, selectedPegawai)

        // Create and configure the AAChartModel with the processed data
        val aaChartModel = AAChartModel()
            .chartType(chartType)
            .title(chartTitle)  // Use the dynamic title
            .categories(categories)  // Use names as categories on the x-axis
            .series(arrayOf(
                AASeriesElement()
                    .name(selectedData)
                    .data(data as Array<Any>)  // Set y-values as chart data
            ))

        // Draw the chart using the configured model
        chart.aa_drawChartWithChartModel(aaChartModel)
        generate.revertAnimation()
    }


    private fun getYAxisTitle(selectedJenis: String, selectedData: String): String {
        return when (selectedJenis) {
            "lembur" -> when (selectedData) {
                "total lembur" -> "Total Jam Lembur"
                "rata-rata lembur" -> "Rata-rata Jam Lembur"
                "distribusi lembur" -> "Jam Lembur"
                else -> "Nilai"
            }
            // Add cases for other jenis when implemented
            else -> "Nilai"
        }
    }

    private fun buildChartTitle(
        jenis: String,
        data: String,
        bulan: String,
        tahun: String,
        pegawai: String
    ): String {
        val jenisTitle = when (jenis) {
            "lembur" -> "Lembur"
            "dinas" -> "Dinas"
            "izin" -> "Izin"
            "presensi" -> "Presensi"
            "all" -> "Keseluruhan"
            else -> "Data"
        }

        val dataTitle = when (data) {
            "total lembur" -> "Total Lembur"
            "rata-rata lembur" -> "Rata-rata Lembur"
            "distribusi lembur" -> "Distribusi Lembur"
            "total dinas" -> "Total Dinas"
            "rata-rata durasi dinas" -> "Rata-rata Durasi Dinas"
            "distribusi dinas" -> "Distribusi Dinas"
            "total izin" -> "Total Izin"
            "rata-rata izin" -> "Rata-rata Izin"
            "jenis izin" -> "Jenis Izin"
            "tingkat kehadiran" -> "Tingkat Kehadiran"
            "jumlah ketidakhadiran" -> "Jumlah Ketidakhadiran"
            "pola kehadiran" -> "Pola Kehadiran"
            "produktivitas keseluruhan" -> "Produktivitas Keseluruhan"
            else -> "Data"
        }

        val timePeriod = when {
            bulan.isNotEmpty() && tahun.isNotEmpty() -> "Bulan $bulan Tahun $tahun"
            tahun.isNotEmpty() -> "Tahun $tahun"
            else -> "Semua Waktu"
        }

        val pegawaiTitle = if (pegawai.isNotEmpty()) "Pegawai: $pegawai" else "Semua Pegawai"

        // Combine all parts to form the full title
        return "$dataTitle - $jenisTitle ($timePeriod, $pegawaiTitle)"
    }

    private fun getEmployeeNameById(employeeId: Int): String {
        return pekerjaList.find { it.id == employeeId }?.nama ?: "Unknown"
    }
    private fun setupAutoCompleteTextViews() {
        val jenisOptions = arrayOf("lembur", "dinas", "izin", "presensi", "all")
        val chartOptions = arrayOf("Bar", "Line", "Pie", "Scatter", "Polygon")


        val jenisAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, jenisOptions)
        val chartAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, chartOptions)

        jenis.setAdapter(jenisAdapter)

        jenis.setOnItemClickListener { parent, view, position, id ->
            val selectedJenis = parent.getItemAtPosition(position).toString()
            val dataOptions = getDataOptions(selectedJenis)
            val dataAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, dataOptions)
            data.setAdapter(dataAdapter)
            // Update month and year adapters dynamically based on selectedJenis
            updatePegawaiAdapter(selectedJenis)
            updateMonthAndYearAdapters(selectedJenis)
        }

        chartList.setAdapter(chartAdapter)
    }

    private fun updatePegawaiAdapter(selectedJenis: String) {
        val pegawaiNamesWithIds = when (selectedJenis) {
            "lembur" -> lemburItemList.map { Pair(it.id_pekerja, it.nama_pekerja) }
            "dinas" -> dinasItemList.map { Pair(it.id_pekerja, it.nama_pekerja) }
            "izin" -> izinItemList.map { Pair(it.id_pekerja, it.nama_pekerja) }
            "presensi" -> presensiItemList.map { Pair(it.id_pekerja, it.nama_pekerja) }
            else -> emptyList()
        }.distinctBy { it.first }

        val pegawaiNames = pegawaiNamesWithIds.map { it.second }
        val pegawaiAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, pegawaiNames)
        pegawai.setAdapter(pegawaiAdapter)

        // Store the mapping of names to IDs for later use
        val nameToIdMap = pegawaiNamesWithIds.associate { it.second to it.first }
        pegawai.setOnItemClickListener { _, _, position, _ ->
            val selectedName = pegawaiNames[position]
            val selectedId = nameToIdMap[selectedName]
            // You can use selectedId when you need the ID for database queries
        }
    }
    private val lemburOptionsMap = mapOf(
        "Total" to "total lembur",
        "Rata-rata" to "rata-rata lembur",
        "Distribusi" to "distribusi lembur"
    )

    private val dinasOptionsMap = mapOf(
        "Total" to "total dinas",
        "Rata-rata" to "rata-rata durasi dinas",
        "Distribusi" to "distribusi dinas"
    )

    private val izinOptionsMap = mapOf(
        "Total" to "total izin",
        "Rata-rata" to "rata-rata izin",
        "Jenis" to "jenis izin"
    )

    private val presensiOptionsMap = mapOf(
        "Kehadiran" to "tingkat kehadiran",
        "Ketidakhadiran" to "jumlah ketidakhadiran",
        "Pola" to "pola kehadiran"
    )

    private val allOptionsMap = mapOf(
        "Produktivitas" to "produktivitas keseluruhan"
    )


    private fun getDataOptions(selectedJenis: String): Array<String> {
        return when (selectedJenis) {
            "lembur" -> lemburOptionsMap.keys.toTypedArray()  // Show short labels
            "dinas" -> dinasOptionsMap.keys.toTypedArray()
            "izin" -> izinOptionsMap.keys.toTypedArray()
            "presensi" -> presensiOptionsMap.keys.toTypedArray()
            "all" -> allOptionsMap.keys.toTypedArray()
            else -> arrayOf()
        }
    }
    private fun getFullDescription(selectedJenis: String, selectedDataShort: String): String {
        return when (selectedJenis) {
            "lembur" -> lemburOptionsMap[selectedDataShort] ?: ""
            "dinas" -> dinasOptionsMap[selectedDataShort] ?: ""
            "izin" -> izinOptionsMap[selectedDataShort] ?: ""
            "presensi" -> presensiOptionsMap[selectedDataShort] ?: ""
            "all" -> allOptionsMap[selectedDataShort] ?: ""
            else -> ""
        }
    }

    private fun generateDinasChart(
        selectedJenis: String,
        selectedData: String
    ) {
        val selectedBulan = bulan.text.toString()
        val selectedTahun = tahun.text.toString()
        val selectedPegawai = pegawai.text.toString()
        val selectedChartType = chartList.text.toString()

        val entries = processDataForChart(selectedJenis, selectedData,selectedBulan,selectedTahun)

        // Prepare categories and data arrays
        val categories = entries.map { entry ->
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = entry.x.toLong()
            calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault())
        }.toTypedArray()

        val data = entries.map { it.y }.toTypedArray()

        val aaChartModel = AAChartModel()
            .chartType(AAChartType.valueOf(selectedChartType.replace(" ", "")))
            .title(generateChartTitle(selectedJenis, selectedData, selectedBulan, selectedTahun))
            .categories(categories)
            .series(arrayOf(
                AASeriesElement()
                    .name(selectedData)
                    .data(arrayOf(data)) // Wrap data in arrayOf
            ))

        chart.aa_drawChartWithChartModel(aaChartModel)
    }

    private fun generateChartTitle(
        selectedJenis: String,
        selectedData: String,
        selectedBulan: String,
        selectedTahun: String
    ): String {
        return when (selectedJenis) {
            "lembur" -> "Lembur Data for ${formatMonthYear(selectedBulan, selectedTahun)}"
            "dinas" -> "Dinas Data for ${formatMonthYear(selectedBulan, selectedTahun)}"
            "izin" -> "Izin Data for ${formatMonthYear(selectedBulan, selectedTahun)}"
            "presensi" -> "Presensi Data for ${formatMonthYear(selectedBulan, selectedTahun)}"
            "all" -> "Overall Data for ${formatMonthYear(selectedBulan, selectedTahun)}"
            else -> "Chart"
        }
    }

    private fun formatMonthYear(selectedBulan: String, selectedTahun: String): String {
        return if (selectedBulan.isNotEmpty() && selectedTahun.isNotEmpty()) {
            "${getMonthName(selectedBulan)} ${selectedTahun}"
        } else if (selectedTahun.isNotEmpty()) {
            selectedTahun
        } else if (selectedBulan.isNotEmpty()) {
            getMonthName(selectedBulan)
        } else {
            "All Time"
        }
    }

    private fun getMonthName(monthIndex: String): String {
        return Calendar.getInstance().apply { set(Calendar.MONTH, monthIndex.toInt()) }
            .getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault()) ?: ""
    }

    private fun updateDinasChartTitle(selectedData: String): String {
        return when (selectedData) {
            "total dinas" -> "Total Dinas per Karyawan"
            "rata-rata durasi dinas" -> "Rata-Rata Durasi Dinas per Karyawan"
            "distribusi dinas" -> "Distribusi Dinas"
            else -> "Data Dinas"
        }
    }

    private fun getData() {
        perusahaan?.let {
            fetchDataDinasPerusahaan(it.nama)
            fetchDataLemburPerusahaan(it.nama)
            it.id?.let { it1 -> fetchDataSesiPerusahaan(it1) }
            fetchDataIzinPerusahaan(it.nama)
            fetchDataPresensiPerusahaan(it.nama)
            fetchDataPekerja(it.nama)
        }
    }

    private fun fetchDataDinasPerusahaan(namaPerusahaan: String) {
        val url = "http://192.168.1.5:8000/api/"
        val retrofit = Retrofit.Builder()
            .baseUrl(url)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val apiService = retrofit.create(ApiService::class.java)

        val call = apiService.getDataDinasPerusahaan(namaPerusahaan)
        call.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    response.body()?.let { responseBody ->
                        try {
                            val jsonResponse = responseBody.string()
                            val responseData = JSONObject(jsonResponse)
                            val dataArray = responseData.getJSONArray("data")
                            dinasItemList.clear()
                            for (i in 0 until dataArray.length()) {
                                val jsonObject = dataArray.getJSONObject(i)
                                val dinasItem = DinasItem(
                                    jsonObject.getInt("id"),
                                    jsonObject.getInt("id_perusahaan"),
                                    jsonObject.getInt("id_pekerja"),
                                    jsonObject.getString("nama_pekerja"),
                                    jsonObject.getString("nama_perusahaan"),
                                    jsonObject.getString("tujuan"),
                                    parseDate(jsonObject.getString("tanggal_berangkat")),
                                    parseDate(jsonObject.getString("tanggal_pulang")),
                                    jsonObject.getString("kegiatan"),
                                    jsonObject.getString("bukti"),
                                    jsonObject.getString("status")
                                )
                                dinasItemList.add(dinasItem)
                            }
                            updateMonthAndYearAdapters("dinas")
                        } catch (e: JSONException) {
                            Log.e("FetchDataError", "Error parsing JSON: ${e.message}")
                        }
                    }
                } else {
                    Log.e("FetchDataError", "Failed to fetch data: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Log.e("FetchDataError", "Failed to fetch data: ${t.message}")
            }
        })
    }
    private fun fetchDataPekerja(namaPerusahaan: String) {
        val url = "http://192.168.1.5:8000/api/"
        Log.d("FetchDataError", "Nama: ${namaPerusahaan}")
        val retrofit = Retrofit.Builder()
            .baseUrl(url)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val apiService = retrofit.create(ApiService::class.java)
        val call = apiService.getDataPekerja(namaPerusahaan)
        call.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    response.body()?.let { responseBody ->
                        try {
                            val responseData = JSONObject(responseBody.string())
                            val pekerjaArray = responseData.getJSONArray("pekerja")
                            pekerjaList = parsePekerjaList(pekerjaArray) // Update pekerjaList with the parsed data
                            updatePegawaiAdapter() // Update AutoCompleteTextView with the new data
                        } catch (e: JSONException) {
                            Log.e("FetchDataError", "Error parsing JSON: ${e.message}")
                        }
                    }
                } else {
                    // Handle unsuccessful response
                    Log.e("FetchDataError", "Failed to fetch data: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                // Handle network failures
                Log.e("FetchDataError", "Failed to fetch data: ${t.message}")
            }
        })
    }
    private fun updatePegawaiAdapter() {
        val pegawaiNames = pekerjaList.map { it.nama }
        val pegawaiAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, pegawaiNames)
        pegawai.setAdapter(pegawaiAdapter)
    }

    private fun parsePekerjaList(pekerjaArray: JSONArray): MutableList<Pekerja> {
        val pekerjaList = mutableListOf<Pekerja>()
        for (i in 0 until pekerjaArray.length()) {
            val pekerjaObject = pekerjaArray.getJSONObject(i)
            pekerjaList.add(
                Pekerja(
                    pekerjaObject.getInt("id"),
                    pekerjaObject.getInt("id_perusahaan"),
                    pekerjaObject.getString("email"),
                    pekerjaObject.getString("password"),
                    pekerjaObject.getString("nama"),
                    parseDate(pekerjaObject.getString("tanggal_lahir")),
                    pekerjaObject.getString("profile")
                )
            )
        }
        return pekerjaList
    }

    private fun fetchDataLemburPerusahaan(namaPerusahaan: String) {
        val url = "http://192.168.1.5:8000/api/"
        val retrofit = Retrofit.Builder()
            .baseUrl(url)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val apiService = retrofit.create(ApiService::class.java)

        val call = apiService.getDataLemburPerusahaan(namaPerusahaan)
        call.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    response.body()?.let { responseBody ->
                        try {
                            val jsonResponse = responseBody.string()
                            val responseData = JSONObject(jsonResponse)
                            val dataArray = responseData.getJSONArray("data")
                            lemburItemList.clear()
                            for (i in 0 until dataArray.length()) {
                                val jsonObject = dataArray.getJSONObject(i)
                                val lemburItem = LemburItem(
                                    jsonObject.getInt("id"),
                                    jsonObject.getInt("id_perusahaan"),
                                    jsonObject.getInt("id_pekerja"),
                                    jsonObject.getString("nama_pekerja"),
                                    jsonObject.getString("nama_perusahaan"),
                                    parseDate(jsonObject.getString("tanggal")),
                                    Time.valueOf(jsonObject.getString("waktu_masuk")),
                                    Time.valueOf(jsonObject.getString("waktu_pulang")),
                                    jsonObject.getString("pekerjaan"),
                                    jsonObject.getString("bukti"),
                                    jsonObject.getString("status")
                                )
                                lemburItemList.add(lemburItem)
                            }
                            updateMonthAndYearAdapters("lembur")
                        } catch (e: JSONException) {
                            Log.e("FetchDataError", "Error parsing JSON: ${e.message}")
                        }
                    }
                } else {
                    Log.e("FetchDataError", "Failed to fetch data: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Log.e("FetchDataError", "Failed to fetch data: ${t.message}")
            }
        })
    }
    private fun fetchDataSesiPerusahaan(perusahaanId: Int) {
        val url = "http://192.168.1.5:8000/api/"
        val retrofit = Retrofit.Builder()
            .baseUrl(url)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val apiService = retrofit.create(ApiService::class.java)

        val call = apiService.getDataSesiPerusahaan(perusahaanId)
        call.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    response.body()?.let { responseBody ->
                        try {
                            val jsonResponse = responseBody.string()
                            val responseData = JSONObject(jsonResponse)
                            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                            val dataArray = responseData.getJSONArray("data")
                            sesilemburItemList.clear()
                            for (i in 0 until dataArray.length()) {
                                val jsonObject = dataArray.getJSONObject(i)
                                val jamString = jsonObject.optString("jam") // Get timestamp as string
                                val jamDate = dateFormat.parse(jamString) // Parse string to Date
                                val sesiItem = session_lembur(
                                    id = jsonObject.optInt("id"),
                                    id_lembur = jsonObject.optInt("id_lembur"),
                                    jam = jamDate ?: java.util.Date(0),
                                    keterangan = jsonObject.optString("keterangan"),
                                    bukti = jsonObject.optString("bukti"),
                                    status = jsonObject.optString("status")
                                )
                                sesilemburItemList.add(sesiItem)
                            }
                            updateMonthAndYearAdapters("lembur")
                        } catch (e: JSONException) {
                            Log.e("FetchDataError", "Error parsing JSON: ${e.message}")
                        }
                    }
                } else {
                    Log.e("FetchDataError", "Failed to fetch data: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Log.e("FetchDataError", "Failed to fetch data: ${t.message}")
            }
        })
    }
    private fun fetchDataIzinPerusahaan(namaPerusahaan: String) {
        val url = "http://192.168.1.5:8000/api/"
        val retrofit = Retrofit.Builder()
            .baseUrl(url)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val apiService = retrofit.create(ApiService::class.java)

        val call = apiService.getDataIzinPerusahaan(namaPerusahaan)
        call.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    response.body()?.let { responseBody ->
                        try {
                            val jsonResponse = responseBody.string()
                            val responseData = JSONObject(jsonResponse)
                            val dataArray = responseData.getJSONArray("data")
                            izinItemList.clear()
                            for (i in 0 until dataArray.length()) {
                                val jsonObject = dataArray.getJSONObject(i)
                                val izinItem = IzinItem(
                                    jsonObject.getInt("id"),
                                    jsonObject.getInt("id_perusahaan"),
                                    jsonObject.getInt("id_pekerja"),
                                    jsonObject.getString("nama_pekerja"),
                                    jsonObject.getString("nama_perusahaan"),
                                    parseDate(jsonObject.getString("tanggal")),
                                    jsonObject.getString("kategori"),
                                    jsonObject.getString("alasan"),
                                    jsonObject.getString("bukti"),
                                    jsonObject.getString("status")
                                )
                                izinItemList.add(izinItem)
                            }
                            updateMonthAndYearAdapters("izin")
                        } catch (e: JSONException) {
                            Log.e("FetchDataError", "Error parsing JSON: ${e.message}")
                        }
                    }
                } else {
                    Log.e("FetchDataError", "Failed to fetch data: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Log.e("FetchDataError", "Failed to fetch data: ${t.message}")
            }
        })
    }

    private fun fetchDataPresensiPerusahaan(namaPerusahaan: String) {
        val url = "http://192.168.1.5:8000/api/"
        val retrofit = Retrofit.Builder()
            .baseUrl(url)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val apiService = retrofit.create(ApiService::class.java)

        val call = apiService.getDataAbsenPerusahaan(namaPerusahaan)
        call.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    response.body()?.let { responseBody ->
                        try {
                            val jsonResponse = responseBody.string()
                            val responseData = JSONObject(jsonResponse)
                            val dataArray = responseData.getJSONArray("data")
                            presensiItemList.clear()
                            for (i in 0 until dataArray.length()) {
                                val jsonObject = dataArray.getJSONObject(i)
                                val absenItem = AbsenItem(
                                    jsonObject.getInt("id"),
                                    jsonObject.getInt("id_perusahaan"),
                                    jsonObject.getInt("id_pekerja"),
                                    jsonObject.getString("nama_pekerja"),
                                    jsonObject.getString("nama_perusahaan"),
                                    parseDate(jsonObject.getString("tanggal")),
                                    Time.valueOf(jsonObject.getString("waktu_masuk")),
                                    Time.valueOf(jsonObject.getString("waktu_pulang")),
                                )
                                presensiItemList.add(absenItem)
                            }
                            updateMonthAndYearAdapters("presensi")
                        } catch (e: JSONException) {
                            Log.e("FetchDataError", "Error parsing JSON: ${e.message}")
                        }
                    }
                } else {
                    Log.e("FetchDataError", "Failed to fetch data: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Log.e("FetchDataError", "Failed to fetch data: ${t.message}")
            }
        })
    }

    private fun parseDate(dateString: String): Date {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val utilDate = format.parse(dateString)
        return Date(utilDate.time)
    }

    private fun updateMonthAndYearAdapters(jenis: String) {
        val months = mutableSetOf<String>()
        val years = mutableSetOf<String>()

        val combinedList: List<Any> = when (jenis) {
            "lembur" -> lemburItemList
            "dinas" -> dinasItemList
            "izin" -> izinItemList
            "presensi" -> presensiItemList
            else -> lemburItemList + dinasItemList + izinItemList + presensiItemList
        }

        combinedList.forEach { item ->
            val calendar = Calendar.getInstance()
            val dates = when (item) {
                is LemburItem -> listOf(item.tanggal)
                is DinasItem -> listOf(item.tanggal_berangkat, item.tanggal_pulang)
                is IzinItem -> listOf(item.tanggal)
                is AbsenItem -> listOf(item.tanggal)
                else -> listOf()
            }
            dates.forEach { date ->
                calendar.time = date
                val month = calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault())
                val year = calendar.get(Calendar.YEAR).toString()
                months.add(month!!)
                years.add(year)
            }
        }

        val monthAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, months.toList())
        val yearAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, years.toList())

        bulan.setAdapter(monthAdapter)
        tahun.setAdapter(yearAdapter)
    }
    private fun getBundle() {
        bundle = intent?.getBundleExtra("data")
        if (bundle != null) {
            bundle?.let {
                perusahaan = it.getParcelable("perusahaan")
                admin = it.getParcelable("user")
            }
            val url = "http://192.168.1.5/getDecryptedLogo/${perusahaan?.id}" // Replace with your actual URL
//            name.setText(perusahaan.nama)
            val imageRequest = ImageRequest(
                url,
                { response ->
                    // Set the Bitmap to an ImageView or handle it as needed
                    logo.setImageBitmap(response)
                },
                0, 0, ImageView.ScaleType.CENTER_CROP, Bitmap.Config.RGB_565,
                { error ->
                    error.printStackTrace()
                    Toast.makeText(this, "Failed to fetch profile image", Toast.LENGTH_SHORT).show()
                }
            )

            val requestQueue = Volley.newRequestQueue(this)
            requestQueue.add(imageRequest)
        } else {
            Log.d("Error", "Bundle Not Found")
        }
    }
}