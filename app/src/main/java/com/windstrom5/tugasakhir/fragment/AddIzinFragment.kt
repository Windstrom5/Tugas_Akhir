package com.windstrom5.tugasakhir.fragment

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import br.com.simplepass.loadingbutton.customViews.CircularProgressButton
import com.android.volley.Request
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.Volley
import com.bumptech.glide.Glide
import com.github.barteksc.pdfviewer.PDFView
import com.google.android.material.textfield.TextInputLayout
import com.saadahmedev.popupdialog.PopupDialog
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog
import com.windstrom5.tugasakhir.R
import com.windstrom5.tugasakhir.connection.ApiResponse
import com.windstrom5.tugasakhir.connection.ApiService
import com.windstrom5.tugasakhir.feature.EmailSender
import com.windstrom5.tugasakhir.model.Admin
import com.windstrom5.tugasakhir.model.Pekerja
import com.windstrom5.tugasakhir.model.Perusahaan
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.HttpException
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import www.sanju.motiontoast.MotionToast
import www.sanju.motiontoast.MotionToastStyle
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


class AddIzinFragment : Fragment() {
    private lateinit var TINama: TextInputLayout
    private lateinit var TITanggal: TextInputLayout
    private lateinit var acIzin: AutoCompleteTextView
    private lateinit var TIAlasan: TextInputLayout
    private lateinit var uploadfileButton: Button
    private lateinit var selectedFileName: TextView
    private lateinit var save : CircularProgressButton
    private var perusahaan : Perusahaan? = null
    private lateinit var imageView: ImageView
    private lateinit var pdfView: PDFView
    private var pekerja : Pekerja? = null
    private lateinit var selectedFile: File
    private val PDF_REQUEST_CODE = 123
    private val PICK_PDF_OR_IMAGE_REQUEST_CODE = 100
    private var holidaysMap: MutableMap<Calendar, String> = mutableMapOf()
    private var adminList: List<Admin>? = null
    private var izinKerjaOptions: List<String> =
        mutableListOf("Sakit", "Cuti", "Izin Khusus", "Pendidikan", "Liburan", "Keperluan Pribadi", "Kegiatan Keluarga", "Ibadah");
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_add_izin, container, false)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TINama = view.findViewById(R.id.nama)
        getBundle()
        perusahaan?.let { fetchDataFromApi(it.nama) }
        fetchHolidayData()
        save = view.findViewById(R.id.submitButton)
        TITanggal = view.findViewById(R.id.TITanggal)
        acIzin = view.findViewById(R.id.acizin)
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, izinKerjaOptions)
        acIzin.setAdapter(adapter)
        imageView = view.findViewById(R.id.imageView)
        pdfView = view.findViewById(R.id.pdfView)
        selectedFileName = view.findViewById(R.id.selectedFileName)
        TIAlasan = view.findViewById(R.id.alasan)
        uploadfileButton = view.findViewById(R.id.uploadfile)
        uploadfileButton.setOnClickListener{
            pickPdfFile()
        }
        TITanggal.setEndIconOnClickListener{
            TITanggal.editText?.let { it1 -> showDatePickerDialog(it1) }
        }
//        TINama.editText?.addTextChangedListener(watcher)
//        TITanggal.editText?.addTextChangedListener(watcher)
//        acIzin.addTextChangedListener(watcher)
//        TIAlasan.editText?.addTextChangedListener(watcher)
        save.setOnClickListener {
            Log.d("Testing",isAllFieldsFilled().toString())
            save.startAnimation()
            if(isAllFieldsFilled()){
                perusahaan?.let { it1 -> pekerja?.let { it2 -> saveDataIzin( it2, it1) } }
            }else{
                save.revertAnimation()
            }
        }
    }
    private fun fetchDataFromApi(namaPerusahaan: String) {
        val url = "https://selected-jaguar-presently.ngrok-free.app/api/"
        Log.d("FetchDataError", "Nama: $namaPerusahaan")

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
                            val adminArray = responseData.getJSONArray("admin")
                            // Parse admin and pekerja data
                            adminList = parseAdminList(adminArray)  // Assign parsed admin list
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
    private fun parseAdminList(adminArray: JSONArray): List<Admin> {
        val adminList = mutableListOf<Admin>()
        for (i in 0 until adminArray.length()) {
            val adminObject = adminArray.getJSONObject(i)
            adminList.add(
                Admin(
                    adminObject.getInt("id"),
                    adminObject.getInt("id_perusahaan"),
                    adminObject.getString("email"),
                    adminObject.getString("password"),
                    adminObject.getString("nama"),
                    parseDate(adminObject.getString("tanggal_lahir")),
                    adminObject.getString("profile")
                )
            )
        }
        return adminList
    }
    private fun parseDate(dateString: String): Date {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dateFormat.parse(dateString) ?: Date()
    }
    private fun isAllFieldsFilled(): Boolean {
        val missingFields = mutableListOf<String>()

        if (TINama.editText?.text.isNullOrEmpty()) {
            missingFields.add("Nama")
        }
        if (TITanggal.editText?.text.isNullOrEmpty()) {
            missingFields.add("Tanggal Izin")
        }
        if (acIzin.text.isNullOrEmpty()) {
            missingFields.add("Tanggal Berangkat")
        }
        if (TIAlasan.editText?.text.isNullOrEmpty()) {
            missingFields.add("Alasan Izin")
        }
        if (selectedFileName.text == "No file selected") {
            missingFields.add("Bukti Izin")
        }

        if (missingFields.isNotEmpty()) {
            val errorMessage = "The following fields are empty: ${missingFields.joinToString(", ")}"
            PopupDialog.getInstance(requireContext())
                .statusDialogBuilder()
                .createErrorDialog()
                .setHeading("Cannot Save")
                .setDescription(errorMessage)
                .build(Dialog::dismiss)
                .show()
            return false
        }
        return true
    }

    private fun setLoading(isLoading: Boolean) {
        val loadingLayout = activity?.findViewById<LinearLayout>(R.id.layout_loading)
        if (isLoading) {
            loadingLayout?.visibility = View.VISIBLE
        } else {
            loadingLayout?.visibility = View.INVISIBLE
        }
    }
    private fun Calendar.isSameDay(other: Calendar): Boolean {
        return this.get(Calendar.YEAR) == other.get(Calendar.YEAR) &&
                this.get(Calendar.MONTH) == other.get(Calendar.MONTH) &&
                this.get(Calendar.DAY_OF_MONTH) == other.get(Calendar.DAY_OF_MONTH)
    }
    private fun showDatePickerDialog(editText: EditText?) {
        // Prepare to fetch holiday data if "Nasional" is present
        if (perusahaan?.holiday?.contains("Nasional") == true) {
            fetchHolidayData()
        }

        // Extract weekdays to disable based on company holidays
        val disabledWeekdays = mutableSetOf<Int>()
        perusahaan?.holiday?.split(",\\s*".toRegex())?.forEach { day ->
            when (day.trim().lowercase()) {
                "senin" -> disabledWeekdays.add(Calendar.MONDAY)
                "selasa" -> disabledWeekdays.add(Calendar.TUESDAY)
                "rabu" -> disabledWeekdays.add(Calendar.WEDNESDAY)
                "kamis" -> disabledWeekdays.add(Calendar.THURSDAY)
                "jumat" -> disabledWeekdays.add(Calendar.FRIDAY)
                "sabtu" -> disabledWeekdays.add(Calendar.SATURDAY)
                "minggu" -> disabledWeekdays.add(Calendar.SUNDAY)
            }
        }

        val now = Calendar.getInstance()
        val disabledDates = mutableListOf<Calendar>()

        // Generate disabled dates for the entire year based on weekdays and holidays
        val startOfYear = Calendar.getInstance().apply { set(Calendar.DAY_OF_YEAR, 1) }
        val endOfYear = Calendar.getInstance().apply {
            add(Calendar.YEAR, 1)
            set(Calendar.DAY_OF_YEAR, 1)
            add(Calendar.DAY_OF_YEAR, -1)
        }

        var current = startOfYear.clone() as Calendar
        while (current.before(endOfYear)) {
            // Add disabled weekdays
            if (disabledWeekdays.contains(current.get(Calendar.DAY_OF_WEEK))) {
                disabledDates.add(current.clone() as Calendar)
            }

            // Add national holidays
            holidaysMap.keys.forEach { holidayCalendar ->
                if (current.isSameDay(holidayCalendar)) {
                    disabledDates.add(current.clone() as Calendar)
                }
            }

            current.add(Calendar.DAY_OF_MONTH, 1)
        }

        // Fetch `izin` and `dinas` dates and add them to the disabled dates
        fetchDisabledDatesForDinas(perusahaan?.nama.orEmpty(), pekerja?.nama.orEmpty()) { dinasDates ->
            fetchDisabledDatesForIzin(perusahaan?.nama.orEmpty(), pekerja?.nama.orEmpty()) { izinDates ->
                disabledDates.addAll(dinasDates)
                disabledDates.addAll(izinDates)

                // Set up the DatePickerDialog
                val dpd = DatePickerDialog.newInstance(
                    { _, year, monthOfYear, dayOfMonth ->
                        val selectedDate = Calendar.getInstance().apply {
                            set(year, monthOfYear, dayOfMonth)
                        }

                        // Check if the selected date is a holiday
                        holidaysMap.forEach { holidayCalendar, holidayName ->
                            if (holidayCalendar.isSameDay(selectedDate)) {
                                Toast.makeText(
                                    requireContext(),
                                    "Tanggal yang dipilih adalah hari libur: $holidayName. Silakan pilih tanggal lain.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }

                        // Format the date in Indonesian style (e.g., 20 Agustus 2024)
                        val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID"))
                        val formattedDate = dateFormat.format(selectedDate.time)
                        editText?.setText(formattedDate)
                    },
                    now.get(Calendar.YEAR),
                    now.get(Calendar.MONTH),
                    now.get(Calendar.DAY_OF_MONTH)
                )

                // Disable past days
                dpd.minDate = now

                // Disable specific weekdays and holidays
                dpd.setDisabledDays(disabledDates.toTypedArray())

                dpd.show(childFragmentManager, "DatePickerDialog")
            }
        }
    }

    private fun fetchDisabledDatesForDinas(
        namaPerusahaan: String,
        namaPekerja: String,
        callback: (Set<Calendar>) -> Unit
    ) {
        val url = "https://selected-jaguar-presently.ngrok-free.app/api/"
        val retrofit = Retrofit.Builder()
            .baseUrl(url)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val apiService = retrofit.create(ApiService::class.java)
        val call = apiService.getDataDinasPekerja(namaPerusahaan, namaPekerja)

        call.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    val disabledDates = mutableSetOf<Calendar>()
                    response.body()?.let { responseBody ->
                        try {
                            val jsonResponse = responseBody.string()
                            val responseData = JSONObject(jsonResponse)
                            val dataArray = responseData.getJSONArray("data")

                            for (i in 0 until dataArray.length()) {
                                val jsonObject = dataArray.getJSONObject(i)
                                val tanggalBerangkat = parseDate(jsonObject.getString("tanggal_berangkat"))
                                val tanggalPulang = parseDate(jsonObject.getString("tanggal_pulang"))
                                val status = jsonObject.getString("status")

                                if (status == "Accept") {
                                    val startCal = Calendar.getInstance().apply { time = tanggalBerangkat }
                                    val endCal = Calendar.getInstance().apply { time = tanggalPulang }

                                    // Add all dates in the range to disabled dates
                                    while (!startCal.after(endCal)) {
                                        disabledDates.add(Calendar.getInstance().apply { time = startCal.time })
                                        startCal.add(Calendar.DAY_OF_MONTH, 1)
                                    }
                                }
                            }
                        } catch (e: JSONException) {
                            Log.e("FetchDataError", "Error parsing JSON: ${e.message}")
                        }
                    }
                    callback(disabledDates)
                } else {
                    Log.e("FetchDataError", "Failed to fetch data: ${response.code()}")
                    callback(emptySet())
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Log.e("FetchDataError", "Failed to fetch data: ${t.message}")
                callback(emptySet())
            }
        })
    }
    private fun fetchDisabledDatesForIzin(
        namaPerusahaan: String,
        namaPekerja: String,
        callback: (Set<Calendar>) -> Unit
    ) {
        val url = "https://selected-jaguar-presently.ngrok-free.app/api/"
        val retrofit = Retrofit.Builder()
            .baseUrl(url)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val apiService = retrofit.create(ApiService::class.java)
        val call = apiService.getDataIzinPekerja(namaPerusahaan, namaPekerja)

        call.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    val disabledDates = mutableSetOf<Calendar>()
                    response.body()?.let { responseBody ->
                        try {
                            val jsonResponse = responseBody.string()
                            val responseData = JSONObject(jsonResponse)
                            val dataArray = responseData.getJSONArray("data")

                            for (i in 0 until dataArray.length()) {
                                val jsonObject = dataArray.getJSONObject(i)
                                val tanggal = parseDate(jsonObject.getString("tanggal"))
                                val status = jsonObject.getString("status")

                                if (status == "Accept") {
                                    disabledDates.add(Calendar.getInstance().apply { time = tanggal })
                                }
                            }
                        } catch (e: JSONException) {
                            Log.e("FetchDataError", "Error parsing JSON: ${e.message}")
                        }
                    }
                    callback(disabledDates)
                } else {
                    Log.e("FetchDataError", "Failed to fetch data: ${response.code()}")
                    callback(emptySet())
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Log.e("FetchDataError", "Failed to fetch data: ${t.message}")
                callback(emptySet())
            }
        })
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

        Volley.newRequestQueue(requireContext()).add(jsonArrayRequest)
    }
    private fun vectorToBitmap(vectorDrawable: Drawable): Bitmap {
        val bitmap = Bitmap.createBitmap(vectorDrawable.intrinsicWidth,
            vectorDrawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        vectorDrawable.setBounds(0, 0, canvas.width, canvas.height)
        vectorDrawable.draw(canvas)
        return bitmap
    }
    private fun saveDataIzin(pekerja: Pekerja,perusahaan: Perusahaan){
        val url = "https://selected-jaguar-presently.ngrok-free.app/api/"

        val retrofit = Retrofit.Builder()
            .baseUrl(url)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val apiService = retrofit.create(ApiService::class.java)
        val nama_Perusahaan = createPartFromString(perusahaan.nama)
        val nama = createPartFromString(pekerja.nama)
        val kategori = createPartFromString(acIzin.text.toString())
        val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID"))
        val dateInput = TITanggal.editText?.text.toString()
        val parsedDate = dateFormat.parse(dateInput)
        val sqlDate = Date(parsedDate?.time ?: 0)
        val tanggal = createPartFromString(sqlDate.toString()) // Convert back to SQL date format
        val alasan = createPartFromString(TIAlasan.editText?.text.toString())
        val buktifile = selectedFile
        val requestFile = RequestBody.create("pdf/*".toMediaTypeOrNull(), buktifile)
        val buktipart = MultipartBody.Part.createFormData("bukti", buktifile.name, requestFile)
        val call = apiService.uploadIzin(nama_Perusahaan,nama,tanggal, kategori,alasan, buktipart)
        call.enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                if (response.isSuccessful) {
                    val vectorDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.done_bitmap)
                    val bitmap = vectorDrawable?.let { vectorToBitmap(it) }
                    if (bitmap != null) {
                        save.doneLoadingAnimation(Color.parseColor("#AAFF00"), bitmap)
                    }
                    val apiResponse = response.body()
                    Log.d("ApiResponse", "Status: ${apiResponse?.status}, Message: ${apiResponse?.message}")
                    MotionToast.createToast(requireActivity(), "Add Izin Success",
                        "Data Izin Berhasil Ditambahkan",
                        MotionToastStyle.SUCCESS,
                        MotionToast.GRAVITY_BOTTOM,
                        MotionToast.LONG_DURATION,
                        ResourcesCompat.getFont(requireContext(), R.font.ralewaybold))
                    sendEmailToAllAdmins(
                        subject = "Leave Request Submitted",
                        message = """
                        Notification: Leave Request Submitted

                        Employee Name: ${pekerja.nama}
                        Company: ${perusahaan.nama}
                        Date of Leave: ${dateFormat.format(parsedDate)}
                        Leave Category: ${acIzin.text.toString()}
                        Reason: ${TIAlasan.editText?.text.toString()}
                        
                        Please review the leave request for approval.
                    """.trimIndent()
                    )

                } else {
                    save.revertAnimation()
                    val errorMessage = response.errorBody()?.string() ?: "Unknown error"
                    Log.e("ApiResponse", "Error: ${response.message()} - $errorMessage")                }
            }

            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                save.revertAnimation()
                when (t) {
                    is IOException -> {
                        // No internet connection on the device
                        Toast.makeText(
                            requireContext(),
                            "No internet connection. Please check your network and try again.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    is HttpException -> {
                        // Server is reachable, but there’s an issue on the server
                        val statusCode = t.code()
                        Toast.makeText(
                            requireContext(),
                            "Server error (code: $statusCode). Please try again later.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    else -> {
                        // General error
                        Toast.makeText(
                            requireContext(),
                            "Request failed: ${t.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        })
        setLoading(false)
    }
    private fun createPartFromString(value: String): RequestBody {
        return RequestBody.create("text/plain".toMediaTypeOrNull(), value)
    }
    private fun sendEmailToAllAdmins(subject: String, message: String) {
        adminList?.forEach { admin ->
            val receiverEmail = admin.email
            EmailSender.sendEmail(receiverEmail, subject, message)
        }
    }
//    private val watcher = object : TextWatcher {
//        override fun beforeTextChanged(charSequence: CharSequence?, start: Int, count: Int, after: Int) {
//            // Not needed for this example
//        }
//
//        override fun onTextChanged(charSequence: CharSequence?, start: Int, before: Int, count: Int) {
//            // Not needed for this example
//        }
//
//        override fun afterTextChanged(editable: Editable?) {
//            // Update the button state whenever a field is changed
//            save.isEnabled = isAllFieldsFilled()
//        }
//    }

    private fun pickPdfFile() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*" // Allow all file types
        intent.putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/pdf", "image/*")) // Specify PDF and image MIME types
        startActivityForResult(intent, PICK_PDF_OR_IMAGE_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_PDF_OR_IMAGE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            data?.data?.let { fileUri ->
                val mimeType = requireContext().contentResolver.getType(fileUri)
                if (mimeType != null) {
                    if (mimeType.startsWith("image/")) {
                        // Handle PDF file
                        imageView.visibility = View.VISIBLE
                        pdfView.visibility = View.GONE
                        Glide.with(this)
                            .load(fileUri)
                            .into(imageView)
                        val displayName = getRealFilePathFromUri(fileUri)
                        if (displayName != null) {
                            val file = File(requireContext().cacheDir, displayName)
                            try {
                                requireContext().contentResolver.openInputStream(fileUri)?.use { inputStream ->
                                    FileOutputStream(file).use { outputStream ->
                                        inputStream.copyTo(outputStream)
                                    }
                                }
                                selectedFileName.text = displayName
//                                selectedFileName.addTextChangedListener(watcher)
                                selectedFile = file
                            } catch (e: IOException) {
                                Log.e("MyFragment", "Failed to copy file: ${e.message}")
                            }
                        } else {
                            Log.e("MyFragment", "Failed to get display name from URI")
                        }
                    } else if (mimeType == "application/pdf") {
                        // Handle PDF file
                        imageView.visibility = View.GONE
                        pdfView.visibility = View.VISIBLE
                        displayPdf(fileUri)
                        val displayName = getRealFilePathFromUri(fileUri)
                        if (displayName != null) {
                            val file = File(requireContext().cacheDir, displayName)
                            try {
                                requireContext().contentResolver.openInputStream(fileUri)?.use { inputStream ->
                                    FileOutputStream(file).use { outputStream ->
                                        inputStream.copyTo(outputStream)
                                    }
                                }
                                selectedFileName.text = displayName
//                                selectedFileName.addTextChangedListener(watcher)
                                selectedFile = file
                            } catch (e: IOException) {
                                Log.e("MyFragment", "Failed to copy file: ${e.message}")
                            }
                        } else {
                            Log.e("MyFragment", "Failed to get display name from URI")
                        }
                    }else{
                        MotionToast.createToast(requireActivity(), "Failed",
                            "Jenis File tidak Didukung",
                            MotionToastStyle.ERROR,
                            MotionToast.GRAVITY_BOTTOM,
                            MotionToast.LONG_DURATION,
                            ResourcesCompat.getFont(requireContext(), R.font.ralewaybold))
                    }
                } else {
                    Log.e("MyFragment", "Failed to get MIME type")
                }
            }
        }
    }
    private fun displayPdf(uri: Uri) {
        Log.d("uri5",uri.toString())
        pdfView.fromUri(uri)
            .password(null) // If your PDF is password protected, provide the password here
            .defaultPage(0) // Specify which page to display by default
            .enableSwipe(true) // Enable or disable swipe to change pages
            .swipeHorizontal(false) // Set to true to enable horizontal swipe
            .enableDoubletap(true) // Enable double tap to zoom
            .onLoad { /* Called when PDF is loaded */ }
            .onPageChange { page, pageCount -> /* Called when page is changed */ }
            .onPageError { page, t -> /* Called when an error occurs while loading a page */ }
            .scrollHandle(null) // Specify a custom scroll handle if needed
            .enableAntialiasing(true) // Improve rendering a little bit on low-res screens
            .spacing(0) // Add spacing between pages in dp
            .load()
    }
    private fun getRealImagePathFromUri(uri: Uri): String? {
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        val cursor = requireActivity().contentResolver.query(uri, projection, null, null, null)
        cursor?.use {
            val columnIndex = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            it.moveToFirst()
            return it.getString(columnIndex)
        }
        return null
    }
    private fun getRealFilePathFromUri(uri: Uri): String? {
        val projection = arrayOf(MediaStore.Files.FileColumns.DISPLAY_NAME)
        val cursor = requireActivity().contentResolver.query(uri, projection, null, null, null)
        cursor?.use {
            val columnIndex = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
            it.moveToFirst()
            val displayName = it.getString(columnIndex)
            Log.d("MyFragment", "Display Name: $displayName")
            return displayName
        }
        Log.d("MyFragment", "Cursor is null")
        return null
    }
    private fun getBundle() {
        val arguments = arguments
        if (arguments != null) {
            perusahaan = arguments.getParcelable("perusahaan")
            pekerja = arguments.getParcelable("user")
            TINama.editText?.setText(pekerja?.nama)
            Log.d("namaPekerja", pekerja?.nama.toString())
            TINama.isEnabled=false
        } else {
            Log.d("Error","Bundle Not Found")
        }
    }
}