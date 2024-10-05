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
import com.github.aachartmodel.aainfographics.aachartcreator.aa_toAAOptions
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
import java.text.ParseException
import java.text.SimpleDateFormat
import java.time.Duration
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
    private var holidaysMap: MutableMap<Calendar, String> = mutableMapOf()
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

                // Clear the selected employee (pegawai) and chart selection
                pegawai.text.clear()  // Clear the AutoCompleteTextView for pegawai
                chartList.text.clear() // Clear the AutoCompleteTextView for chart selection
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
    private fun processDinasData(selectedData: String, selectedBulan: String, selectedTahun: String): List<Entry> {
        val monthMapping = mapOf(
            "January" to 1, "February" to 2, "March" to 3, "April" to 4,
            "May" to 5, "June" to 6, "July" to 7, "August" to 8,
            "September" to 9, "October" to 10, "November" to 11, "December" to 12
        )

        val selectedMonth = monthMapping[selectedBulan]
        val selectedYear = selectedTahun.toIntOrNull()

        // Filter dinas items by selected month and year
        val filteredDinasItems = dinasItemList.filter { dinas ->
            val calendar = Calendar.getInstance().apply { time = dinas.tanggal_berangkat }
            val dinasMonth = calendar.get(Calendar.MONTH) + 1
            val dinasYear = calendar.get(Calendar.YEAR)

            (selectedMonth == null || selectedMonth == dinasMonth) &&
                    (selectedYear == null || selectedYear == dinasYear)
        }

        return when (selectedData) {
            "total dinas" -> {
                val totalEntries = filteredDinasItems.groupBy { it.id_pekerja }
                    .map { (id, dinasItems) ->
                        val totalDinas = dinasItems.size.toFloat()
                        Entry(id?.toFloat() ?: 0f, totalDinas)
                    }
                totalEntries
            }
            "rata-rata durasi dinas" -> {
                val avgEntries = filteredDinasItems.groupBy { it.id_pekerja }
                    .map { (id, dinasItems) ->
                        val totalDurasi = dinasItems.sumOf { dinas ->
                            // Calculate duration in hours as Double
                            val durasi = (dinas.tanggal_pulang.time - dinas.tanggal_berangkat.time) / (1000 * 60 * 60).toDouble()
                            durasi
                        }
                        val avgDurasi = if (dinasItems.isNotEmpty()) totalDurasi / dinasItems.size else 0.0
                        // Return Entry with Float values
                        Entry(id?.toFloat() ?: 0f, avgDurasi.toFloat())
                    }
                avgEntries
            }
            "distribusi dinas" -> {
                val distribusiEntries = filteredDinasItems.groupBy { it.tujuan }
                    .map { (tujuan, dinasItems) ->
                        val totalPerTujuan = dinasItems.size.toFloat()
                        Entry(tujuan.hashCode().toFloat(), totalPerTujuan)  // Using tujuan's hashcode for unique representation
                    }
                distribusiEntries
            }
            else -> emptyList()
        }
    }
    private fun processIzinData(selectedData: String, selectedBulan: String, selectedTahun: String): List<Entry> {
        val monthMapping = mapOf(
            "January" to 1, "February" to 2, "March" to 3, "April" to 4,
            "May" to 5, "June" to 6, "July" to 7, "August" to 8,
            "September" to 9, "October" to 10, "November" to 11, "December" to 12
        )

        val selectedMonth = monthMapping[selectedBulan]
        val selectedYear = selectedTahun.toIntOrNull()

        // Filter izin items by selected month and year
        val filteredIzinItems = izinItemList.filter { izin ->
            val calendar = Calendar.getInstance().apply { time = izin.tanggal }
            val izinMonth = calendar.get(Calendar.MONTH) + 1
            val izinYear = calendar.get(Calendar.YEAR)

            (selectedMonth == null || selectedMonth == izinMonth) &&
                    (selectedYear == null || selectedYear == izinYear)
        }

        return when (selectedData) {
            "total izin" -> {
                val totalEntries = filteredIzinItems.groupBy { it.id_pekerja }
                    .map { (id, izinItems) ->
                        val totalIzin = izinItems.size.toFloat()
                        Entry(id?.toFloat() ?: 0f, totalIzin)
                    }
                totalEntries
            }
            "rata-rata izin" -> {
                val avgEntries = filteredIzinItems.groupBy { it.id_pekerja }
                    .map { (id, izinItems) ->
                        val totalIzin = izinItems.size.toFloat()
                        val avgIzin = if (izinItems.isNotEmpty()) totalIzin / izinItems.size else 0f
                        Entry(id?.toFloat() ?: 0f, avgIzin)
                    }
                avgEntries
            }
            "distribusi izin" -> {
                val distribusiEntries = filteredIzinItems.groupBy { it.kategori }
                    .map { (kategori, izinItems) ->
                        val totalPerKategori = izinItems.size.toFloat()
                        Entry(kategori.hashCode().toFloat(), totalPerKategori)  // Use kategori's hashcode as unique identifier
                    }
                distribusiEntries
            }
            else -> emptyList()
        }
    }
    private fun fetchHolidayData() {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val url = "https://dayoffapi.vercel.app/api?year=$currentYear"

        val jsonArrayRequest = JsonArrayRequest(
            Request.Method.GET, url, null,
            { response ->
                holidaysMap.clear() // Clear previous data

                for (i in 0 until response.length()) {
                    val holidayObject = response.getJSONObject(i)
                    val date = holidayObject.getString("tanggal")
                    val description = holidayObject.getString("keterangan")

                    // Parse date string into Calendar object
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val holidayDate = Calendar.getInstance().apply {
                        time = dateFormat.parse(date)
                    }

                    holidaysMap[holidayDate] = description
                }
            },
            { error ->
                error.printStackTrace()
            }
        )

        Volley.newRequestQueue(this
        ).add(jsonArrayRequest)
    }

    private fun processPresensiData(selectedData: String, selectedBulan: String, selectedTahun: String, selectedPegawai: String?): List<Entry> {
        // Map month names to numbers
        val monthMapping = mapOf(
            "January" to 1, "February" to 2, "March" to 3, "April" to 4,
            "May" to 5, "June" to 6, "July" to 7, "August" to 8,
            "September" to 9, "October" to 10, "November" to 11, "December" to 12
        )

        val selectedMonth = monthMapping[selectedBulan]
        val selectedYear = selectedTahun.toIntOrNull()

        // Fetch holidays if needed
        if (perusahaan?.holiday?.contains("Nasional") == true) {
            fetchHolidayData()
        }

        // Calculate the total valid days in the selected month
        val totalDaysInMonth = getTotalValidDaysInMonth(selectedMonth, selectedYear)

        // Filter presensi items by selected month and year, excluding holidays
        val filteredPresensiItems = presensiItemList.filter { presensi ->
            val calendar = Calendar.getInstance().apply {
                time = presensi.tanggal
            }
            val presensiMonth = calendar.get(Calendar.MONTH) + 1
            val presensiYear = calendar.get(Calendar.YEAR)

            val isMonthMatch = selectedMonth == null || selectedMonth == presensiMonth
            val isYearMatch = selectedYear == null || selectedYear == presensiYear

            isMonthMatch && isYearMatch
        }

        // If a specific employee is selected, filter by employee ID
        val employeeId = if (selectedPegawai?.isNotEmpty() == true) {
            presensiItemList.find { it.nama_pekerja == selectedPegawai }?.id_pekerja
        } else {
            null // No specific employee selected
        }

        val presensiItemsToProcess = if (employeeId != null) {
            filteredPresensiItems.filter { it.id_pekerja == employeeId }
        } else {
            filteredPresensiItems // Process all if no specific employee is selected
        }

        // Group by id_pekerja
        val totalEntries = presensiItemsToProcess.groupBy { it.id_pekerja }

        // Prepare the results
        val resultEntries = mutableListOf<Entry>()

        totalEntries.forEach { (id, items) ->
            val presentCount = items.count { presensi -> presensi.keluar != null }
            val absentCount = totalDaysInMonth - presentCount

            Log.d("AttendanceLogger", "Employee ID: $id, Total Present: $presentCount, Total Absent: $absentCount")

            // Return Entry with present count or discipline score based on selected data
            when (selectedData) {
                "tingkat kehadiran" -> {
                    resultEntries.add(Entry(id?.toFloat() ?: 0f, presentCount.toFloat()))
                }
                "kedisiplinan karyawan" -> {
                    val disciplineScore = if (totalDaysInMonth > 0) {
                        (presentCount.toFloat() / totalDaysInMonth) * 100 // Discipline score as a percentage
                    } else {
                        0f // No data for this employee
                    }
                    resultEntries.add(Entry(id?.toFloat() ?: 0f, disciplineScore))
                }
            }
        }

        return resultEntries
    }

    // Helper function to get total valid days in the selected month excluding holidays and specified weekdays
    private fun getTotalValidDaysInMonth(selectedMonth: Int?, selectedYear: Int?): Int {
        val totalDaysInMonth = 31 // Change this if necessary; it will be adjusted based on actual month days
        val validDays = (1..totalDaysInMonth).count { day ->
            val calendar = Calendar.getInstance().apply {
                set(Calendar.YEAR, selectedYear ?: 0)
                set(Calendar.MONTH, (selectedMonth ?: 1) - 1) // Month is zero-based
                set(Calendar.DAY_OF_MONTH, day)
            }

            val isHoliday = holidaysMap.keys.any { holidayDate ->
                holidayDate.get(Calendar.YEAR) == selectedYear &&
                        holidayDate.get(Calendar.MONTH) + 1 == selectedMonth &&
                        holidayDate.get(Calendar.DAY_OF_MONTH) == day
            }

            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
            val isExcludedWeekday = perusahaan?.holiday?.split(",\\s*".toRegex())?.any { day ->
                when (day.trim().lowercase()) {
                    "senin" -> dayOfWeek == Calendar.MONDAY
                    "selasa" -> dayOfWeek == Calendar.TUESDAY
                    "rabu" -> dayOfWeek == Calendar.WEDNESDAY
                    "kamis" -> dayOfWeek == Calendar.THURSDAY
                    "jumat" -> dayOfWeek == Calendar.FRIDAY
                    "sabtu" -> dayOfWeek == Calendar.SATURDAY
                    "minggu" -> dayOfWeek == Calendar.SUNDAY
                    else -> false
                }
            } ?: false

            !isHoliday && !isExcludedWeekday
        }

        return validDays
    }


    private fun processDataForChart(selectedJenis: String, selectedData: String,selectedBulan: String,selectedTahun: String,selectedPegawai: String): List<Entry> {
        return when (selectedJenis) {
            "lembur" -> processLemburData(selectedData,selectedBulan,selectedTahun,selectedPegawai)
            "dinas"-> processDinasData(selectedData,selectedBulan,selectedTahun)
            "izin"-> processIzinData(selectedData,selectedBulan,selectedTahun)
            "presensi"-> processPresensiData(selectedData,selectedBulan,selectedTahun,selectedPegawai)
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

    private fun processLemburData(selectedData: String, selectedBulan: String, selectedTahun: String,selectedPegawai: String?): List<Entry> {
        val monthMapping = mapOf(
            "January" to 1, "February" to 2, "March" to 3, "April" to 4,
            "May" to 5, "June" to 6, "July" to 7, "August" to 8,
            "September" to 9, "October" to 10, "November" to 11, "December" to 12
        )
        val selectedMonth = monthMapping[selectedBulan]
        val selectedYear = selectedTahun.toIntOrNull()

        val filteredLemburItems = lemburItemList.filter { lembur ->
            val calendar = Calendar.getInstance().apply {
                time = lembur.tanggal
            }
            val lemburMonth = calendar.get(Calendar.MONTH) + 1
            val lemburYear = calendar.get(Calendar.YEAR)

            val isMonthMatch = selectedMonth == null || selectedMonth == lemburMonth
            val isYearMatch = selectedYear == null || selectedYear == lemburYear

            isMonthMatch && isYearMatch
        }
        val employeeId = if (selectedPegawai?.isNotEmpty() == true) {
            lemburItemList.find { it.nama_pekerja == selectedPegawai }?.id_pekerja
        } else {
            null // No specific employee selected
        }

        val lemburItemsToProcess = if (employeeId != null) {
            filteredLemburItems.filter { it.id_pekerja == employeeId }
        } else {
            filteredLemburItems // Process all if no specific employee is selected
        }
        // Filter accepted sessions
        val acceptedSessions = sesilemburItemList.filter { it.status == "Accepted" }
        return when (selectedData) {
            "total lembur" -> {
                val totalEntries = lemburItemsToProcess.groupBy { it.id_pekerja }
                    .map { (id, lemburItems) ->
                        // Calculate total duration explicitly
                        val totalDuration = lemburItems.sumOf { lembur ->
                            val filteredSessions = acceptedSessions.filter { it.id_lembur == lembur.id }

                            val sessionDurations = filteredSessions.map { sesi ->
                                val waktuMasuk = lembur.waktu_masuk
                                val waktuPulang = lembur.waktu_pulang

                                // Calculate sessions based on waktuMasuk and waktuPulang
                                val (sessions, _) = calculateSessions(waktuMasuk, waktuPulang)

                                val validSessions = sessions.filter { (_, sessionTimes) ->
                                    val sesiJam = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(sesi.jam)
                                    sesiJam >= sessionTimes.first && sesiJam <= sessionTimes.second
                                }

                                // Calculate total time for each valid session
                                val totalSessionTime = validSessions.sumOf { (_, sessionTimes) ->
                                    val sessionStart = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).parse(sessionTimes.first)
                                    val sessionEnd = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).parse(sessionTimes.second)

                                    val durationMillis = sessionEnd.time - sessionStart.time
                                    val durationHours = durationMillis.toDouble() / (1000 * 60) // Convert to hours
                                    durationHours
                                }

                                // Return total session duration in hours (as Double)
                                totalSessionTime
                            }

                            // Return the sum of the session durations
                            sessionDurations.sum()
                        }

                        // Ensure the Entry is created with a Float value
                        Entry(id?.toFloat() ?: 0f, totalDuration.toFloat())
                    }
                totalEntries
            }
            "rata-rata lembur" -> {
                val avgEntries = filteredLemburItems.groupBy { it.id_pekerja }
                    .map { (id, lemburItems) ->
                        val totalDuration = lemburItems.sumOf { lembur ->
                            val filteredSessions = acceptedSessions.filter { it.id_lembur == lembur.id }

                            val sessionDurations = filteredSessions.map { sesi ->
                                val waktuMasuk = lembur.waktu_masuk
                                val waktuPulang = lembur.waktu_pulang

                                // Calculate sessions based on waktuMasuk and waktuPulang
                                val (sessions, _) = calculateSessions(waktuMasuk, waktuPulang)

                                val validSessions = sessions.filter { (_, sessionTimes) ->
                                    val sesiJam = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(sesi.jam)
                                    sesiJam >= sessionTimes.first && sesiJam <= sessionTimes.second
                                }

                                validSessions.size.toDouble()
                            }

                            sessionDurations.sum()
                        }

                        // Calculate the average duration
                        val avgDuration = if (lemburItems.isNotEmpty()) totalDuration / lemburItems.size else 0.0
                        Entry(id?.toFloat() ?: 0f, avgDuration.toFloat())
                    }
                avgEntries
            }
            "distribusi lembur" -> {
                val distribusiEntries = filteredLemburItems.groupBy { it.id_pekerja }
                    .map { (id, lemburItems) ->
                        val totalDuration = lemburItems.sumOf { lembur ->
                            val filteredSessions = acceptedSessions.filter { it.id_lembur == lembur.id }

                            val sessionDurations = filteredSessions.map { sesi ->
                                val waktuMasuk = lembur.waktu_masuk
                                val waktuPulang = lembur.waktu_pulang

                                // Calculate sessions based on waktuMasuk and waktuPulang
                                val (sessions, _) = calculateSessions(waktuMasuk, waktuPulang)

                                val validSessions = sessions.filter { (_, sessionTimes) ->
                                    val sesiJam = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(sesi.jam)
                                    sesiJam >= sessionTimes.first && sesiJam <= sessionTimes.second
                                }

                                validSessions.size.toDouble()
                            }

                            sessionDurations.sum()
                        }

                        // Entry represents the distribution per employee
                        Entry(id?.toFloat() ?: 0f, totalDuration.toFloat())
                    }
                distribusiEntries
            }
            else -> {
                emptyList()
            }
        }
    }

    private fun formatDecimalToHourMinute(decimal: Double): String {
        val hours = decimal.toInt() // Extract the hour part
        val minutes = ((decimal - hours) * 60).toInt() // Convert the fractional part to minutes
        return String.format("%d:%02d", hours, minutes) // Format as "hours:minutes"
    }

    private fun calculateSessions(waktuMasuk: Time, waktuPulang: Time): Pair<List<Pair<String, Pair<String, String>>>, Int> {
        val sessions = mutableListOf<Pair<String, Pair<String, String>>>()
        val currentTime = Time(System.currentTimeMillis())
        val currentTimeMinutes = (currentTime.hours * 60) + currentTime.minutes
        var closestSessionIndex = -1

        val waktuMasukMinutes = (waktuMasuk.hours * 60) + waktuMasuk.minutes
        val waktuPulangMinutes = (waktuPulang.hours * 60) + waktuPulang.minutes

        // Calculate the total difference in minutes between waktuMasuk and waktuPulang
        val totalMinutesDifference = waktuPulangMinutes - waktuMasukMinutes
        val sessionCount = totalMinutesDifference / 60 // Calculate full-hour sessions

        val timeFormatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

        for (i in 1..sessionCount) {
            val sessionStart = Time(waktuMasuk.time + (i - 1) * 3600000)
            val sessionEnd = Time(waktuMasuk.time + i * 3600000)

            val sessionStartString = timeFormatter.format(sessionStart)
            val sessionEndString = timeFormatter.format(sessionEnd)

            sessions.add("Sesi $i" to (sessionStartString to sessionEndString))

            // Calculate session start and end in minutes
            val sessionStartMinutes = (sessionStart.hours * 60) + sessionStart.minutes
            val sessionEndMinutes = (sessionEnd.hours * 60) + sessionEnd.minutes

            // Determine the closest session to the current time
            if (currentTimeMinutes > sessionStartMinutes && currentTimeMinutes <= sessionEndMinutes) {
                closestSessionIndex = i - 1
            }
        }

        // Handle the remaining minutes (less than 30 minutes should not add a session)
        val remainingMinutes = totalMinutesDifference % 60
        if (remainingMinutes >= 30) {
            val lastSessionStart = Time(waktuMasuk.time + sessionCount * 3600000)
            val lastSessionEnd = waktuPulang

            val lastSessionStartString = timeFormatter.format(lastSessionStart)
            val lastSessionEndString = timeFormatter.format(lastSessionEnd)

            sessions.add("Sesi ${sessionCount + 1}" to (lastSessionStartString to lastSessionEndString))

            if (currentTimeMinutes > lastSessionStart.hours * 60 + lastSessionStart.minutes &&
                currentTimeMinutes <= lastSessionEnd.hours * 60 + lastSessionEnd.minutes) {
                closestSessionIndex = sessionCount // Last partial session
            }
        } else {
            // If remaining minutes < 30, update the last session to reflect waktuPulang instead
            if (sessions.isNotEmpty()) {
                // Update the last session to end at waktuPulang
                val lastSessionIndex = sessions.size - 1
                val lastSession = sessions[lastSessionIndex].first
                val lastSessionStart = sessions[lastSessionIndex].second.first
                val lastSessionEnd = timeFormatter.format(waktuPulang)
                sessions[lastSessionIndex] = lastSession to (lastSessionStart to lastSessionEnd)
            }
        }

        Log.d("Sesi", sessions.toString())

        // Ensure there's a valid closest session index
        if (closestSessionIndex == -1) {
            closestSessionIndex = sessionCount - 1
        }

        return Pair(sessions, closestSessionIndex)
    }


    private fun session_lembur.getLemburDuration(waktuMasuk: String, waktuPulang: String): Double {
        // Session time is already a Date object (we only care about the time part)
        val sessionTime = this.jam

        try {
            // Define the format for parsing time (HH:mm:ss for time components)
            val timeFormatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

            // Parse waktu_masuk and waktu_pulang into Date objects (time only)
            val startTime = timeFormatter.parse(waktuMasuk)
            val endTime = timeFormatter.parse(waktuPulang)

            // Extract only the time part from sessionTime for comparison
            val sessionTimeFormatted = timeFormatter.format(sessionTime)
            val sessionOnlyTime = timeFormatter.parse(sessionTimeFormatted)

            Log.d("getLemburDuration", "Session Time: $sessionOnlyTime, Start Time: $startTime, End Time: $endTime")

            // Check if the session time (time only) is within the start and end time range
            return if (sessionOnlyTime.after(startTime) && sessionOnlyTime.before(endTime)) {
                // Calculate duration from session time to end time in milliseconds
                val durationMs = endTime.time - sessionOnlyTime.time
                // Convert the duration to hours by dividing milliseconds by (1000 * 60 * 60)
                val durationHours = durationMs / (1000.0 * 60.0 * 60.0)
                Log.d("getLemburDuration", "Calculated Duration: $durationHours hours")
                durationHours
            } else {
                Log.d("getLemburDuration", "Session time is not within the range.")
                0.0 // Not within range
            }
        } catch (e: ParseException) {
            Log.e("getLemburDuration", "Error parsing time: ${e.message}")
            return 0.0 // Return 0 if there is a parsing error
        }
    }


    private fun generateSuggestions(
        entries: List<Entry>,
        selectedData: String,
        selectedBulan: String,
        selectedTahun: String,
        selectedPegawai: String // Add selectedPegawai as a parameter
    ): String {
        // Use the selected month and year in suggestions
        val monthYearText = "untuk bulan $selectedBulan tahun $selectedTahun"

        // Check if there are multiple employees
        val isMultipleEmployees = entries.size > 1
        val employeeText = if (isMultipleEmployees) "karyawan" else "karyawan tersebut"

        // Check if a specific employee is selected
        val specificEmployeeMessage = if (selectedPegawai.isNotEmpty()) {
            " untuk $selectedPegawai"
        } else {
            ""
        }

        return when (selectedData) {
            "total lembur" -> {
                // Convert total lembur to hours and minutes
                val totalLemburInMinutes = entries.map { it.y }.sum().toInt()
                val totalHours = totalLemburInMinutes / 60
                val totalMinutes = totalLemburInMinutes % 60

                // Convert average lembur to hours and minutes
                val averageLemburInMinutes = entries.map { it.y }.average().toInt()
                val averageHours = averageLemburInMinutes / 60
                val averageMinutes = averageLemburInMinutes % 60

                val excessiveLemburThresholdInMinutes = 40 * 60 // 40 hours in minutes
                val employeesWithExcessiveLembur = entries.filter { it.y > excessiveLemburThresholdInMinutes }

                val excessiveMessage = if (employeesWithExcessiveLembur.isNotEmpty()) {
                    "Perhatian: ${employeesWithExcessiveLembur.size} $employeeText bekerja lembur lebih dari 40 jam$monthYearText. Pertimbangkan untuk mengevaluasi beban kerja atau menambah staf."
                } else {
                    "Tidak ada $employeeText yang melebihi lembur 40 jam$monthYearText. Pemakaian lembur masih dalam batas normal."
                }

                // Return the final message with hours and minutes
                val totalLemburFormatted = "$totalHours jam $totalMinutes menit"
                val averageLemburFormatted = "$averageHours jam $averageMinutes menit"

                "Total lembur keseluruhan $totalLemburFormatted$specificEmployeeMessage $monthYearText.\n$excessiveMessage"
            }
            "rata-rata lembur" -> {
                val averageLembur = entries.map { it.y }.average()
                val belowOptimalThreshold = 5 // Example threshold for under-utilization
                val employeesWithLowLembur = entries.filter { it.y < belowOptimalThreshold }

                val underUtilizationMessage = if (employeesWithLowLembur.isNotEmpty()) {
                    "Beberapa $employeeText memiliki lembur di bawah rata-rata $belowOptimalThreshold jam$monthYearText, pertimbangkan apakah tugas mereka bisa dialihkan untuk meningkatkan produktivitas."
                } else {
                    "Penggunaan lembur sudah merata di antara $employeeText$specificEmployeeMessage$monthYearText."
                }

                "Rata-rata lembur keseluruhan$specificEmployeeMessage $monthYearText adalah ${"%.2f".format(averageLembur)} jam.\n$underUtilizationMessage"
            }
            "distribusi lembur" -> {
                "Distribusi lembur$monthYearText menunjukkan pola lembur $employeeText$specificEmployeeMessage pada berbagai waktu. Analisis ini dapat membantu untuk merencanakan lembur di masa depan dengan lebih efisien."
            }
            "total dinas" -> {
                val totalDinas = entries.map { it.y }.sum()
                val averageDinas = entries.map { it.y }.average()

                val dinasThreshold = 5 // Example threshold for excessive dinas trips
                val employeesWithExcessiveDinas = entries.filter { it.y > dinasThreshold }

                val excessiveDinasMessage = if (employeesWithExcessiveDinas.isNotEmpty()) {
                    "Perhatian: ${employeesWithExcessiveDinas.size} $employeeText melakukan dinas lebih dari $dinasThreshold kali$monthYearText."
                } else {
                    "Jumlah dinas masih dalam batas normal$monthYearText."
                }

                "Total dinas keseluruhan$specificEmployeeMessage $monthYearText adalah $totalDinas kali.\nRata-rata dinas per karyawan adalah ${"%.2f".format(averageDinas)} kali.\n$excessiveDinasMessage"
            }
            "rata-rata durasi dinas" -> {
                val averageDuration = entries.map { it.y }.average()
                val shortTripThreshold = 48 // Example threshold for short trips (hours)

                val shortTrips = entries.filter { it.y < shortTripThreshold }

                val shortTripsMessage = if (shortTrips.isNotEmpty()) {
                    "Beberapa $employeeText melakukan dinas dengan durasi kurang dari $shortTripThreshold jam$monthYearText."
                } else {
                    "Durasi dinas per $employeeText cukup merata$specificEmployeeMessage $monthYearText."
                }

                "Rata-rata durasi dinas$specificEmployeeMessage $monthYearText adalah ${"%.2f".format(averageDuration)} jam.\n$shortTripsMessage"
            }
            "distribusi dinas" -> {
                "Distribusi dinas$monthYearText menunjukkan pola waktu perjalanan dinas $employeeText$specificEmployeeMessage. Analisis ini bisa berguna untuk merencanakan dinas yang lebih efisien ke depannya."
            }
            "total izin" -> {
                val totalIzin = entries.map { it.y }.sum()
                val averageIzin = entries.map { it.y }.average()

                val excessiveIzinThreshold = 5 // Example threshold for excessive izin
                val employeesWithExcessiveIzin = entries.filter { it.y > excessiveIzinThreshold }

                val excessiveMessage = if (employeesWithExcessiveIzin.isNotEmpty()) {
                    "Perhatian: ${employeesWithExcessiveIzin.size} $employeeText memiliki lebih dari $excessiveIzinThreshold izin$monthYearText. Pertimbangkan untuk mengevaluasi alasan izin dan mendiskusikan kembali kebijakan izin di perusahaan."
                } else {
                    "Tidak ada $employeeText yang melebihi izin $excessiveIzinThreshold kali$monthYearText. Penggunaan izin masih dalam batas normal."
                }

                "Total izin keseluruhan$specificEmployeeMessage $monthYearText adalah $totalIzin izin.\nRata-rata izin per karyawan adalah ${"%.2f".format(averageIzin)} izin.\n$excessiveMessage"
            }
            "rata-rata durasi izin" -> {
                val averageIzin = entries.map { it.y }.average()
                val lowIzinThreshold = 1 // Example threshold for low izin usage
                val employeesWithLowIzin = entries.filter { it.y < lowIzinThreshold }

                val underUsageMessage = if (employeesWithLowIzin.isNotEmpty()) {
                    "Beberapa $employeeText memiliki izin di bawah rata-rata $lowIzinThreshold kali$monthYearText, pertimbangkan apakah ada perubahan kebijakan yang diperlukan."
                } else {
                    "Penggunaan izin sudah merata di antara $employeeText$specificEmployeeMessage $monthYearText."
                }

                "Rata-rata izin keseluruhan$specificEmployeeMessage $monthYearText adalah ${"%.2f".format(averageIzin)} izin.\n$underUsageMessage"
            }
            "distribusi izin" -> {
                "Distribusi izin$monthYearText menunjukkan pola izin berdasarkan kategori seperti sakit, cuti, dan lainnya. Analisis ini dapat membantu untuk mengevaluasi kebijakan izin dan kategori yang paling sering digunakan."
            }
            "tingkat kehadiran" -> {
                val monthMapping = mapOf(
                    "January" to 1, "February" to 2, "March" to 3, "April" to 4,
                    "May" to 5, "June" to 6, "July" to 7, "August" to 8,
                    "September" to 9, "October" to 10, "November" to 11, "December" to 12
                )
                val selectedMonth = monthMapping[selectedBulan]
                val selectedYear = selectedTahun.toIntOrNull()
                if (perusahaan?.holiday?.contains("Nasional") == true) {
                    fetchHolidayData()
                }

                // Calculate the total valid days in the selected month
                val totalDaysInMonth = getTotalValidDaysInMonth(selectedMonth, selectedYear)
                val suggestions = StringBuilder()

                // Process each entry to create suggestions
                entries.forEach { entry ->
                    val id = entry.x.toInt() // Employee ID
                    val presentCount = entry.y.toInt() // Total present days
                    val absentCount = totalDaysInMonth - presentCount // Calculate absent days
                    val employeeName = getEmployeeNameById(id)

                    // Attendance message based on present and absent counts
                    val attendanceRate = (presentCount.toDouble() / totalDaysInMonth) * 100
                    val attendanceMessage = when {
                        attendanceRate < 50.0 -> {
                            "Tingkat kehadiran keseluruhan sangat rendah, hanya $presentCount hari hadir dari $totalDaysInMonth hari (absen $absentCount hari). Pertimbangkan untuk menyelidiki penyebabnya."
                        }
                        attendanceRate < 75.0 -> {
                            "Tingkat kehadiran keseluruhan $presentCount hari hadir dari $totalDaysInMonth hari (absen $absentCount hari). Meskipun masih dalam batas, pertimbangkan untuk meningkatkan kehadiran."
                        }
                        else -> {
                            "Tingkat kehadiran keseluruhan sangat baik dengan $presentCount hari hadir dari $totalDaysInMonth hari (absen $absentCount hari)."
                        }
                    }

                    // Log the attendance details
                    Log.d("AttendanceLogger", "Employee ID: $id, Total Present: $presentCount, Total Absent: $absentCount")
                    suggestions.append("Karyawan: $employeeName - $attendanceMessage\n")

                }

                suggestions.toString()
            }

            "jumlah ketidakhadiran" -> {
                val totalAbsences = entries.map { it.y }.sum() // Assuming it.y holds absence data
                val averageAbsences = entries.map { it.y }.average()

                val excessiveAbsenceThreshold = 5 // Example threshold for excessive absences
                val employeesWithExcessiveAbsences = entries.filter { it.y > excessiveAbsenceThreshold }

                val absenceMessage = if (employeesWithExcessiveAbsences.isNotEmpty()) {
                    "Perhatian: ${employeesWithExcessiveAbsences.size} karyawan memiliki ketidakhadiran lebih dari $excessiveAbsenceThreshold kali. Pertimbangkan untuk mendiskusikan kebijakan kehadiran."
                } else {
                    "Jumlah ketidakhadiran keseluruhan masih dalam batas normal."
                }

                "Total ketidakhadiran keseluruhan $totalAbsences kali.\nRata-rata ketidakhadiran per karyawan adalah ${"%.2f".format(averageAbsences)} kali.\n$absenceMessage"
            }

            else -> {
                "Tidak ada data yang relevan untuk ditampilkan$monthYearText."
            }
        }
    }


    private fun getLemburDurationFromSessions(idLembur: Int, waktuMasuk: Date, waktuPulang: Date): Int {
        // Filter sesi_lembur items based on id_lembur and status "Accepted"
        val relevantSessions = sesilemburItemList.filter {
            it.id_lembur == idLembur && it.status == "Accepted"
        }

        val timeFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

        // Parse the waktuMasuk and waktuPulang into Calendar objects
        val calendarMasuk = Calendar.getInstance().apply { time = waktuMasuk }
        val calendarPulang = Calendar.getInstance().apply { time = waktuPulang }

        var totalHours = 0

        relevantSessions.forEach { sesi ->
            val sesiJam = timeFormatter.parse(sesi.jam.toString()) ?: return@forEach

            val sesiCalendar = Calendar.getInstance().apply { time = sesiJam }

            // Check if the sesi timestamp is within waktuMasuk and waktuPulang
            if (!sesiCalendar.before(calendarMasuk) && !sesiCalendar.after(calendarPulang)) {
                // Increment the duration for each valid session
                totalHours++
            }
        }

        return totalHours
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

    private fun generateChart(
        selectedJenis: String,
        selectedData: String,
        selectedBulan: String,
        selectedTahun: String
    ) {
        val selectedPegawai = pegawai.text.toString()  // Retrieve selected employee or check if empty
        val selectedChartType = chartList.text.toString()  // Retrieve selected chart type

        val entries = processDataForChart(selectedJenis, selectedData, selectedBulan, selectedTahun, selectedPegawai)
        Log.d("EntriesData", "Entries: $entries")

        val suggestions = generateSuggestions(entries, selectedData, selectedBulan, selectedTahun, selectedPegawai)
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
        Log.d("ChartGeneration", "Chart type selected: $chartType")

        // Convert y-values (duration in hours) to formatted HH:mm strings for the X-axis categories
        val formattedXCategories = entries.map { entry ->
            val formattedTime = convertToHoursAndMinutes(entry.y)
            Log.d("ChartGeneration", "Original Y-value: ${entry.y}, Formatted: $formattedTime")
            formattedTime  // Use the formatted HH:mm time as a category
        }.toTypedArray()

        Log.d("ChartGeneration", "X-axis categories: ${formattedXCategories.contentToString()}")

        // Map the original y-values (decimal) to the chart data
        val data = entries.map { it.y.toFloat() }.toTypedArray()
        val categories = entries.map { entry ->
            val employeeId = entry.x.toInt()
            val employeeName = getEmployeeNameById(employeeId)  // Convert employee ID (x) to name
            Log.d("EmployeeNameLookup", "Looking up name for employee ID: $employeeId, Result: $employeeName")
            employeeName
        }.toTypedArray()

        // Construct the chart title dynamically based on selected options
        val chartTitle = buildChartTitle(selectedJenis, selectedData, selectedBulan, selectedTahun, selectedPegawai)
        Log.d("ChartGeneration", "Chart title: $chartTitle")

        // Create and configure the AAChartModel with the processed data
        val aaChartModel = AAChartModel()
            .chartType(chartType)
            .title(chartTitle)  // Use the dynamic title
            .categories(categories)  // Use employee names as categories on the x-axis
            .series(arrayOf(
                AASeriesElement()
                    .name(selectedData)
                    .data(data as Array<Any>)  // Set y-values as chart data
            ))

        Log.d("ChartGeneration", "Chart model generated with data")

        // Ensure X-axis categories are set properly
        aaChartModel.xAxisLabelsEnabled(true)  // Enable X-axis labels

        // Draw the chart using the configured model
        chart.aa_drawChartWithChartModel(aaChartModel)

        // Revert any loading animations
        generate.revertAnimation()
    }

    // Helper function to convert decimal hours to HH:mm format
    private fun convertToHoursAndMinutes(decimalHours: Float): String {
        val totalMinutes = (decimalHours * 60).toInt()  // Convert decimal hours to minutes
        val hours = totalMinutes / 60  // Get the hours part
        val minutes = totalMinutes % 60  // Get the remaining minutes

        Log.d("TimeConversion", "Decimal hours: $decimalHours, Total minutes: $totalMinutes, Hours: $hours, Minutes: $minutes")

        // Return the formatted HH:mm string
        return String.format("%d:%02d", hours, minutes)
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
//            "pola kehadiran" -> "Pola Kehadiran"
//            "produktivitas keseluruhan" -> "Produktivitas Keseluruhan"
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
        Log.d("List Pekerja",pekerjaList.toString())
        return pekerjaList.find { it.id == employeeId }?.nama ?: "Unknown"
    }
    private fun setupAutoCompleteTextViews() {
        val jenisOptions = arrayOf("lembur", "dinas", "izin", "presensi", "all")
        val chartOptions = arrayOf("Bar", "Line", "Pie", "Scatter", "Polygon")


        val jenisAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, jenisOptions)
        val chartAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, chartOptions)

        jenis.setAdapter(jenisAdapter)

        jenis.setOnItemClickListener { parent, view, position, id ->
            data.setText("") // Clear data field
            bulan.setText("") // Clear bulan field
            tahun.setText("") // Clear tahun field
            pegawai.setText("") // Clear pegawai field
            chartList.setText("")
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
//        "Disiplin" to "kedisiplinan karyawan",
//        "Pola" to "pola kehadiran"
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

        val entries = processDataForChart(selectedJenis, selectedData,selectedBulan,selectedTahun,selectedPegawai)

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
                            Log.e("FetchDataError", "Error parsing JSON Dinas: ${e.message}")
                        }
                    }
                } else {
                    Log.e("FetchDataError", "Failed to fetch data Dinas: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Log.e("FetchDataError", "Failed to fetch data Dinas: ${t.message}")
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
                            Log.d("List Pekerja",responseData.toString())
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
        Log.d("List Pekerja",pekerjaList.toString())
        val pegawaiAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, pegawaiNames)
        pegawai.setAdapter(pegawaiAdapter)
    }

    private fun parsePekerjaList(pekerjaArray: JSONArray): MutableList<Pekerja> {
        val pekerjaList = mutableListOf<Pekerja>()
        Log.d("List Pekerja",pekerjaArray.toString())

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
                            Log.e("FetchDataError", "Error parsing JSON Izin: ${e.message}")
                        }
                    }
                } else {
                    Log.e("FetchDataError", "Failed to fetch data Izin: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Log.e("FetchDataError", "Failed to fetch data Izin: ${t.message}")
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
                            val outerArray = JSONArray(jsonResponse) // Get the outer array
                            val dataArray = outerArray.getJSONArray(0) // Get the inner array
                            Log.d("Presensi1", dataArray.toString())
                            Log.d("Presensi14", dataArray.length().toString())

                            presensiItemList.clear()

                            for (i in 0 until dataArray.length()) {
                                val jsonObject = dataArray.getJSONObject(i)
                                val jamKeluarString = jsonObject.optString("jam_keluar")
                                Log.d("Presensi",jamKeluarString)
                                // Only create AbsenItem if jam_keluar is not null
                                if (jamKeluarString != "null") {
                                    val absenItem = AbsenItem(
                                        jsonObject.getInt("id"),
                                        jsonObject.getInt("id_perusahaan"),
                                        jsonObject.getInt("id_pekerja"),
                                        jsonObject.getString("nama"),
                                        namaPerusahaan,
                                        parseDate(jsonObject.getString("tanggal")),
                                        Time.valueOf(jsonObject.getString("jam_masuk")),
                                        Time.valueOf(jamKeluarString) // Since jam_keluar is not null, it's safe to use
                                    )
                                    Log.d("Presensi2", absenItem.toString())
                                    presensiItemList.add(absenItem)
                                }
                            }
                            Log.d("Presensi", presensiItemList.toString())
                            updateMonthAndYearAdapters("presensi")
                        } catch (e: JSONException) {
                            Log.e("FetchDataError", "Error parsing JSON Presensi: ${e.message}")
                        } catch (e: IllegalArgumentException) {
                            Log.e("FetchDataError", "Invalid time format: ${e.message}")
                        }
                    }
                } else {
                    Log.e("FetchDataError", "Failed to fetch data Presensi: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Log.e("FetchDataError", "Failed to fetch data Presensi: ${t.message}")
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

        // Map to convert month names to their numerical equivalents
        val monthMapping = mapOf(
            "January" to 1, "February" to 2, "March" to 3, "April" to 4,
            "May" to 5, "June" to 6, "July" to 7, "August" to 8,
            "September" to 9, "October" to 10, "November" to 11, "December" to 12
        )

        // Sort months by their mapped values
        val sortedMonths = months.sortedBy { monthMapping[it] }

        // Sort years numerically
        val sortedYears = years.sorted()

        val monthAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, sortedMonths)
        val yearAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, sortedYears)

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