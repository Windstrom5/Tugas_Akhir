package com.windstrom5.tugasakhir.fragment

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import br.com.simplepass.loadingbutton.customViews.CircularProgressButton
import com.google.android.material.textfield.TextInputLayout
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.saadahmedev.popupdialog.PopupDialog
import com.windstrom5.tugasakhir.R
import com.windstrom5.tugasakhir.activity.RegisterAdminActivity
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
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import www.sanju.motiontoast.MotionToast
import www.sanju.motiontoast.MotionToastStyle
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


class AddDinasFragment : Fragment() {
    private lateinit var nama : TextInputLayout
    private val READ_WRITE_PERMISSION_CODE = 123
    private val CAMERA_PERMISSION_CODE = 101
    private val PDF_REQUEST_CODE = 123
    private lateinit var pdfView: WebView
    private lateinit var selectedFile: File
    private lateinit var save : CircularProgressButton
    private lateinit var textviewtujuan : AutoCompleteTextView
    private lateinit var tilberangkat: TextInputLayout
    private lateinit var tilpulang: TextInputLayout
    private lateinit var keterangan: TextInputLayout
    private lateinit var selectedFileName: TextView
    private var perusahaan : Perusahaan? = null
    private var pekerja : Pekerja? = null
    private var adminList: List<Admin>? = null
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
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_add_dinas, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        nama = view.findViewById(R.id.nama)
        getBundle()
        val kotaDataList = readAndParseKotaJson()
        val provinsiDataList = readAndParseProvinsiJson()
        val combinedDataList = combineAndFormatData(kotaDataList, provinsiDataList)
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            combinedDataList
        )
        perusahaan?.let { fetchDataFromApi(it.nama) }
        save = view.findViewById(R.id.submitButton)
        keterangan = view.findViewById(R.id.keterangan)
        textviewtujuan = view.findViewById(R.id.actujuan)
        selectedFileName = view.findViewById(R.id.selectedFileName)
        textviewtujuan.setAdapter(adapter)
        tilberangkat = view.findViewById(R.id.TITanggalberangkat)
        tilpulang = view.findViewById(R.id.TITanggalpulang)
        tilberangkat.setEndIconOnClickListener {
            tilberangkat.editText?.let { it1 -> showDatePickerDialog(it1,) }
        }
        tilpulang.setEndIconOnClickListener {
            tilpulang.editText?.let { it1 -> showDatePickerDialog(it1) }
        }
        val uploadButton = view.findViewById<Button>(R.id.uploadfile)
        uploadButton.setOnClickListener {
            pickPdfFile()
        }
        save.setOnClickListener {
            Log.d("clicked","click")
            save.startAnimation()
            if(isAllFieldsFilled()){
//                if (isInternetAvailable()) {
                    perusahaan?.let { it1 -> pekerja?.let { it2 -> saveDataDinas(it2, it1) } }
//                } else {
//                    // Show Toast if no internet connection
//                    Toast.makeText(requireContext(), "No internet connection. Please check your connection.", Toast.LENGTH_LONG).show()
//                    save.revertAnimation()
//                }
            }else{
                save.revertAnimation()
            }
        }
    }
    private fun setLoading(isLoading: Boolean) {
        val loadingLayout = activity?.findViewById<LinearLayout>(R.id.layout_loading)
        if (isLoading) {
            loadingLayout?.visibility = View.VISIBLE
        } else {
            loadingLayout?.visibility = View.INVISIBLE
        }
    }
    private fun saveDataDinas(pekerja: Pekerja,perusahaan: Perusahaan){
        val url = "https://selected-jaguar-presently.ngrok-free.app/api/"

        val retrofit = Retrofit.Builder()
            .baseUrl(url)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val apiService = retrofit.create(ApiService::class.java)
        val nama_Perusahaan = createPartFromString(perusahaan.nama)
        val nama = createPartFromString(pekerja.nama)
        val tujuan = createPartFromString(textviewtujuan.text.toString())
        val tanggal_berangkat = createPartFromString(tilberangkat.editText?.text.toString())
        val tanggal_pulang = createPartFromString(tilpulang.editText?.text.toString())
        val kegiatan = createPartFromString(keterangan.editText?.text.toString())

        val buktifile = selectedFile
        val requestFile = RequestBody.create("pdf/*".toMediaTypeOrNull(), buktifile)
        val buktipart = MultipartBody.Part.createFormData("bukti", buktifile.name, requestFile)
        val call = apiService.uploadDinas(nama_Perusahaan,nama,tujuan, tanggal_berangkat,tanggal_pulang, kegiatan, buktipart)
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
                    MotionToast.createToast(requireActivity(), "Add Dinas Success",
                        "Data Dinas Berhasil Ditambahkan",
                        MotionToastStyle.SUCCESS,
                        MotionToast.GRAVITY_BOTTOM,
                        MotionToast.LONG_DURATION,
                        ResourcesCompat.getFont(requireContext(), R.font.ralewaybold))
                    val notificationMessage = """
                        Notification: Dinas Session Requested
                        
                        Employee Name: ${pekerja.nama}
                        Destination: ${textviewtujuan.text.toString()}
                        Departure Date: ${tilberangkat.editText?.text.toString()}
                        Return Date: ${tilpulang.editText?.text.toString()}
                        Activity: ${keterangan.editText?.text.toString()}
                        
                        Please review the dinas submission for approval.
                """.trimIndent()
                    sendEmailToAllAdmins("Dinas Requested", notificationMessage)
                } else {
                    save.revertAnimation()
                    Log.e("ApiResponse", "Error: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                save.revertAnimation()
                Log.e("ApiResponse", "Request failed: ${t.message}")
            }
        })
        setLoading(false)
    }
    private fun sendEmailToAllAdmins(subject: String, message: String) {
        adminList?.forEach { admin ->
            val receiverEmail = admin.email
            EmailSender.sendEmail(receiverEmail, subject, message)
        }
    }
    private fun vectorToBitmap(vectorDrawable: Drawable): Bitmap {
        val bitmap = Bitmap.createBitmap(vectorDrawable.intrinsicWidth,
            vectorDrawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        vectorDrawable.setBounds(0, 0, canvas.width, canvas.height)
        vectorDrawable.draw(canvas)
        return bitmap
    }
    private fun createPartFromString(value: String): RequestBody {
        return RequestBody.create("text/plain".toMediaTypeOrNull(), value)
    }
    private fun getBundle() {
        val arguments = arguments
        if (arguments != null) {
            perusahaan = arguments.getParcelable("perusahaan")
            pekerja = arguments.getParcelable("user")
            nama.editText?.setText(pekerja?.nama)
            Log.d("namaPekerja", pekerja?.nama.toString())
            nama.isEnabled=false
        } else {
            Log.d("Error","Bundle Not Found")
        }
    }

    // Check if all fields are filled
    private fun isAllFieldsFilled(): Boolean {
        val missingFields = mutableListOf<String>()

        if (nama.editText?.text.isNullOrEmpty()) {
            missingFields.add("Nama")
        }
        if (textviewtujuan.text.isEmpty()) {
            missingFields.add("Tujuan Dinas")
        }
        if (tilberangkat.editText?.text.isNullOrEmpty()) {
            missingFields.add("Tanggal Berangkat")
        }
        if (tilpulang.editText?.text.isNullOrEmpty()) {
            missingFields.add("Tanggal Pulang")
        }
        if (keterangan.editText?.text.isNullOrEmpty()) {
            missingFields.add("Keterangan Dinas")
        }
        if (selectedFileName.text == "No file selected") {
            missingFields.add("Bukti Dinas")
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

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == READ_WRITE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
                && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                pickPdfFile()
            } else {
                // Permissions denied, handle accordingly (e.g., show a message to the user).
                // You can inform the user why the permissions are required.
            }
        }
    }

    private fun pickPdfFile() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "application/pdf"
        startActivityForResult(intent, PDF_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PDF_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            data?.data?.let { fileUri ->
                val displayName = getRealPathFromUri(fileUri)
                if (displayName != null) {
                    val file = File(requireContext().cacheDir, displayName)
                    try {
                        requireContext().contentResolver.openInputStream(fileUri)?.use { inputStream ->
                            FileOutputStream(file).use { outputStream ->
                                inputStream.copyTo(outputStream)
                            }
                        }
                        selectedFileName.text = displayName
//                        selectedFileName.addTextChangedListener(watcher)
                        selectedFile = file
                        Log.d("MyFragment", "Selected File: $selectedFile")
                    } catch (e: IOException) {
                        Log.e("MyFragment", "Failed to copy file: ${e.message}")
                    }
                } else {
                    Log.e("MyFragment", "Failed to get display name from URI")
                }
            }
        }
    }
    private fun getRealPathFromUri(uri: Uri): String? {
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

    private fun readAndParseKotaJson(): List<JSONObject> {
        val kotaDataList = mutableListOf<JSONObject>()
        try {
            val inputStream: InputStream = resources.openRawResource(R.raw.kota)
            val size: Int = inputStream.available()
            val buffer = ByteArray(size)
            inputStream.read(buffer)
            inputStream.close()

            val json = String(buffer, Charsets.UTF_8)

            val jsonArray = JSONArray(json)
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                kotaDataList.add(jsonObject)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        Log.d("KotaList",kotaDataList.toString())
        return kotaDataList
    }

    private fun readAndParseProvinsiJson(): List<JSONObject> {
        val provinsiDataList = mutableListOf<JSONObject>()
        try {
            val inputStream: InputStream = resources.openRawResource(R.raw.provinsi)
            val size: Int = inputStream.available()
            val buffer = ByteArray(size)
            inputStream.read(buffer)
            inputStream.close()

            val json = String(buffer, Charsets.UTF_8)

            val jsonArray = JSONArray(json)
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                provinsiDataList.add(jsonObject)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        Log.d("ProvinsiList",provinsiDataList.toString())
        return provinsiDataList
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
//private suspend fun isInternetAvailable(): Boolean {
//    return withContext(Dispatchers.IO) {
//        try {
//            val url = URL("https://selected-jaguar-presently.ngrok-free.app") // Replace with your actual endpoint
//            val connection = url.openConnection() as HttpURLConnection
//            connection.requestMethod = "HEAD" // Use HEAD to only get headers
//            connection.connectTimeout = 3000 // Timeout in milliseconds
//            connection.connect()
//            val responseCode = connection.responseCode
//            responseCode in 200..399 // Check if the response code indicates success (200-399)
//        } catch (e: Exception) {
//            false // If an exception occurs, consider it as no internet
//        }
//    }
//}
    private fun combineAndFormatData(kotaList: List<JSONObject>, provinsiList: List<JSONObject>): List<String> {
        val combinedDataList = mutableListOf<String>()
        for (kotaObject in kotaList) {
            val kotaId = kotaObject.optInt("ID_Kota")
            for (provinsiObject in provinsiList) {
                val provinsiId = provinsiObject.optInt("Id")
                if (kotaId == provinsiId) {
                    val kotaName = kotaObject.optString("Nama_Daerah")
                    val provinsiName = provinsiObject.optString("Kota")
                    val combinedData = "$kotaName, $provinsiName"
                    combinedDataList.add(combinedData)
                }
            }
        }
        return combinedDataList
    }

    private fun showDatePickerDialog(editText: EditText) {
        val holidaysMap = loadHolidaysFromJson(requireContext())
        val disabledDates = holidaysMap.keys.toSet()

        val disabledDatesList = mutableSetOf<Calendar>()
        disabledDatesList.addAll(disabledDates)

        perusahaan?.let {
            pekerja?.let { it1 ->
                fetchDisabledDatesForDinas(it.nama, it1.nama) { dinasDates ->
                    fetchDisabledDatesForIzin(it.nama, it1.nama) { izinDates ->
                        disabledDatesList.addAll(dinasDates)
                        disabledDatesList.addAll(izinDates)

                        val now = Calendar.getInstance()

                        val dpd = DatePickerDialog.newInstance(
                            { _, year, monthOfYear, dayOfMonth ->
                                val selectedDate = Calendar.getInstance().apply {
                                    set(year, monthOfYear, dayOfMonth)
                                }

                                // Check if the selected date is disabled
                                if (disabledDatesList.any { it.isSameDay(selectedDate) }) {
                                    Toast.makeText(
                                        requireContext(),
                                        "Selected date is not available. Please choose another date.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                } else {
                                    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                    val formattedDate = dateFormat.format(selectedDate.time)
                                    editText.setText(formattedDate)
                                }
                            },
                            now.get(Calendar.YEAR),
                            now.get(Calendar.MONTH),
                            now.get(Calendar.DAY_OF_MONTH)
                        )

                        dpd.setDisabledDays(disabledDatesList.toTypedArray())
                        dpd.show(childFragmentManager, "DatePickerDialog")
                    }
                }
            }
        }
    }
    private fun Calendar.isSameDay(other: Calendar): Boolean {
        return this.get(Calendar.YEAR) == other.get(Calendar.YEAR) &&
                this.get(Calendar.MONTH) == other.get(Calendar.MONTH) &&
                this.get(Calendar.DAY_OF_MONTH) == other.get(Calendar.DAY_OF_MONTH)
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

    private fun loadHolidaysFromJson(context: Context): Map<Calendar, String> {
        val holidaysMap = mutableMapOf<Calendar, String>()

        try {
            val inputStream: InputStream = context.resources.openRawResource(R.raw.holidays)
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            val jsonObject = JSONObject(jsonString)

            // Iterate through the keys (dates) in the JSON object
            for (key in jsonObject.keys()) {
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val date = Calendar.getInstance().apply {
                    time = dateFormat.parse(key) ?: Date()
                }
                val dateObject = jsonObject.getJSONObject(key)
                val summary = dateObject.getString("summary")

                // Add the date and summary to the map
                holidaysMap[date] = summary
                Log.d("A", "Holiday: Date = $key, Summary = $summary")
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }

        return holidaysMap
    }
}