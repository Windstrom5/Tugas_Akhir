package com.windstrom5.tugasakhir.fragment

import android.app.Activity
import android.app.Dialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.windstrom5.tugasakhir.R
import com.windstrom5.tugasakhir.model.Pekerja
import com.windstrom5.tugasakhir.model.Perusahaan
import java.util.Calendar
import java.util.Locale
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.net.Uri
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TimePicker
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isEmpty
import br.com.simplepass.loading_button_lib.customViews.CircularProgressButton
import com.android.volley.Request
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.Volley
import com.bumptech.glide.Glide
import com.saadahmedev.popupdialog.PopupDialog
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog
import com.windstrom5.tugasakhir.activity.RegisterActivity
import com.windstrom5.tugasakhir.connection.ApiResponse
import com.windstrom5.tugasakhir.connection.ApiService
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import www.sanju.motiontoast.MotionToast
import www.sanju.motiontoast.MotionToastStyle
import java.io.File
import java.io.InputStream
import java.sql.Time
import java.text.SimpleDateFormat
import java.util.*


class AddLemburFragment : Fragment() {
    private lateinit var TINama: TextInputLayout
    private lateinit var TITanggal: TextInputLayout
    private lateinit var TIMasuk: TextInputLayout
    private lateinit var TIPulang: TextInputLayout
    private lateinit var TIPekerjaan: TextInputLayout
    private lateinit var uploadfileButton: Button
    private lateinit var selectedFileName: TextView
    private lateinit var changeFileButton: Button
    private lateinit var imageView: ImageView
    private lateinit var save : CircularProgressButton
    private var perusahaan : Perusahaan? = null
    private var pekerja : Pekerja? = null
    private lateinit var selectedFile: File
    private val PICK_IMAGE_REQUEST_CODE = 123
    private var isTIMasukFilled = false
    private var holidaysMap: MutableMap<Calendar, String> = mutableMapOf()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_add_lembur, container, false)
        TINama = view.findViewById(R.id.nama)
        getBundle()
        fetchHolidayData()
        TITanggal = view.findViewById(R.id.TITanggal)
        TIMasuk = view.findViewById(R.id.TIMasuk)
        TIPulang = view.findViewById(R.id.TIPulang)
        TIPekerjaan = view.findViewById(R.id.pekerjaan)
        uploadfileButton = view.findViewById(R.id.uploadfile)
        imageView = view.findViewById(R.id.imageView)
        selectedFileName = view.findViewById(R.id.selectedFileName)
        changeFileButton = view.findViewById(R.id.changeFile)
        save = view.findViewById(R.id.submitButton)
        TIMasuk.setEndIconOnClickListener{
            perusahaan?.let { it1 -> showTimePickerDialog(TIMasuk, it1) }
        }
        TITanggal.setEndIconOnClickListener {
            showDatePickerDialog(TITanggal.editText)
        }
        TIPulang.setEndIconOnClickListener {
            if (!isTIMasukFilled) {
                MotionToast.createToast(requireActivity(), "Error",
                    "Masukkan Jam Masuk Terlebih Dahulu",
                    MotionToastStyle.ERROR,
                    MotionToast.GRAVITY_BOTTOM,
                    MotionToast.LONG_DURATION,
                    ResourcesCompat.getFont(requireActivity(), R.font.ralewaybold))
            }else{
                perusahaan?.let { it1 -> showTimePickerDialog(TIPulang, it1) }
            }
        }
//        TINama.editText?.addTextChangedListener(watcher)
//        TITanggal.editText?.addTextChangedListener(watcher)
//        TIMasuk.editText?.addTextChangedListener(watcher)
//        TIPulang.editText?.addTextChangedListener(watcher)
//        TIPekerjaan.editText?.addTextChangedListener(watcher)
        uploadfileButton.setOnClickListener {
            pickImageFromGallery()
        }

        // Set onClickListener for the save button
        save.setOnClickListener {
            Log.d("Amkam","Ming")
            save.startAnimation()
            if(isAllFieldsFilled()){
                pekerja?.let { it1 -> perusahaan?.let { it2 -> saveDataLembur(it1, it2) } }
            }else{
                save.revertAnimation()
                MotionToast.createToast(requireActivity(), "Error",
                    "Belum Semua Form Terisi",
                    MotionToastStyle.ERROR,
                    MotionToast.GRAVITY_BOTTOM,
                    MotionToast.LONG_DURATION,
                    ResourcesCompat.getFont(requireActivity(), R.font.ralewaybold))
            }
        }

        return view
    }

    private fun setLoading(isLoading: Boolean) {
        val loadingLayout = activity?.findViewById<LinearLayout>(R.id.layout_loading)
        if (isLoading) {
            loadingLayout?.visibility = View.VISIBLE
        } else {
            loadingLayout?.visibility = View.INVISIBLE
        }
    }
    private fun isAllFieldsFilled(): Boolean {
        val missingFields = mutableListOf<String>()

        if (TINama.editText?.text.isNullOrEmpty()) {
            missingFields.add("Nama")
        }
        if (TITanggal.editText?.text.isNullOrEmpty()) {
            missingFields.add("Tanggal Lembur")
        }
        if (TIMasuk.editText?.text.isNullOrEmpty()) {
            missingFields.add("Jam Masuk")
        }
        if (TIPulang.editText?.text.isNullOrEmpty()) {
            missingFields.add("Jam Pulang")
        }
        if (selectedFileName.text == "No file selected") {
            missingFields.add("Bukti Lembur")
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

    private fun showDatePickerDialog(editText: EditText?) {
        // Get the current date
        val now = Calendar.getInstance()

        // Set up the DatePickerDialog
        val dpd = DatePickerDialog.newInstance(
            { _, year, monthOfYear, dayOfMonth ->
                val selectedDate = Calendar.getInstance().apply {
                    set(year, monthOfYear, dayOfMonth)
                }

                // Check if the selected date is in the past
                if (selectedDate.before(now)) {
                    Toast.makeText(
                        requireContext(),
                        "Tanggal yang dipilih sudah lewat. Silakan pilih tanggal lain.",
                        Toast.LENGTH_LONG
                    ).show() }

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

        dpd.show(childFragmentManager, "DatePickerDialog")
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


    private fun checkTodayHoliday(holidays: JSONArray) {
        val today =
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().time)
        var isHoliday = false

        for (i in 0 until holidays.length()) {
            val holiday = holidays.getJSONObject(i)
            if (holiday.getString("tanggal") == today) {
                isHoliday = true
                break
            }
        }
    }
    private fun pickImageFromGallery() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        startActivityForResult(intent, PICK_IMAGE_REQUEST_CODE)
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            data?.data?.let { imageUri ->
                // Get the real path from the URI
                val realPath = getRealPathFromUri(imageUri)
                if (realPath != null) {
                    selectedFileName.text = File(realPath).name
                    selectedFile = File(realPath)
//                    selectedFileName.addTextChangedListener(watcher)
                    imageView.visibility = View.VISIBLE
                    Glide.with(this)
                        .load(imageUri)
                        .into(imageView)
                } else {
                    selectedFileName.text = "Failed to get real path"
                }
            }
        }
    }

    private fun getRealPathFromUri(uri: Uri): String? {
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        val cursor = requireActivity().contentResolver.query(uri, projection, null, null, null)
        cursor?.use {
            val columnIndex = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            it.moveToFirst()
            return it.getString(columnIndex)
        }
        return null
    }

    private fun showTimePickerDialog(textInputLayout: TextInputLayout,perusahaan: Perusahaan) {
        val calendar = Calendar.getInstance()
        val masuk = perusahaan.jam_masuk.toString()
        val keluar = perusahaan.jam_keluar.toString()
        val masukParts = masuk.split(":")
        val keluarParts = keluar.split(":")
        val masukHour = masukParts[0].toInt()
        val masukMinute = masukParts[1].toInt()
        if(textInputLayout.id == R.id.TIMasuk){
            val timePickerDialog = TimePickerDialog(
                requireContext(),
                TimePickerDialog.OnTimeSetListener { _, hourOfDay, minute ->
                    val selectedTime = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute)
                    textInputLayout.editText?.setText(selectedTime)
                    isTIMasukFilled = true
                },
                masukHour,
                masukMinute,
                true
            )
            // Set the time range for the TimePickerDialog
            timePickerDialog.updateTime(masukHour, masukMinute)
            timePickerDialog.setRange(perusahaan.jam_masuk, perusahaan.jam_keluar)
            timePickerDialog.show()
        }else{
            val masuk = view?.findViewById<TextInputLayout>(R.id.TIMasuk)?.editText?.text.toString()
            val masukParts = masuk.split(":")
            val masukHour = masukParts[0].toInt()
            val masukMinute = masukParts[1].toInt()
            val timePickerDialog = TimePickerDialog(
                requireContext(),
                TimePickerDialog.OnTimeSetListener { _, hourOfDay, minute ->
                    val selectedTime = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute)
                    textInputLayout.editText?.setText(selectedTime)
                },
                masukHour,
                masukMinute,
                true
            )

            // Set the time range for the TimePickerDialog
            timePickerDialog.updateTime(masukHour, masukMinute)
            timePickerDialog.setRange(perusahaan.jam_masuk, perusahaan.jam_keluar)

            timePickerDialog.show()
        }
    }

    private fun TimePickerDialog.setRange(minTime: Time, maxTime: Time) {
        try {
            val timePicker = this.findViewById<TimePicker>(
                Resources.getSystem().getIdentifier("timePicker", "id", "android")
            )

            val field = TimePicker::class.java.getDeclaredField("mDelegate")
            field.isAccessible = true
            val delegate = field.get(timePicker)
            val method = delegate.javaClass.getDeclaredMethod(
                "setHour", Int::class.javaPrimitiveType
            )
            method.isAccessible = true
            method.invoke(delegate, minTime.hours)

            val method2 = delegate.javaClass.getDeclaredMethod(
                "setMinute", Int::class.javaPrimitiveType
            )
            method2.isAccessible = true
            method2.invoke(delegate, minTime.minutes)

            val method3 = delegate.javaClass.getDeclaredMethod(
                "setIs24Hour", Boolean::class.javaPrimitiveType
            )
            method3.isAccessible = true
            method3.invoke(delegate, true)

            val method4 = delegate.javaClass.getDeclaredMethod(
                "setCurrentHour", Int::class.javaPrimitiveType
            )
            method4.isAccessible = true
            method4.invoke(delegate, maxTime.hours)

            val method5 = delegate.javaClass.getDeclaredMethod(
                "setCurrentMinute", Int::class.javaPrimitiveType
            )
            method5.isAccessible = true
            method5.invoke(delegate, maxTime.minutes)
        } catch (e: Exception) {
            e.printStackTrace()
        }
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
    private fun vectorToBitmap(vectorDrawable: Drawable): Bitmap {
        val bitmap = Bitmap.createBitmap(vectorDrawable.intrinsicWidth,
            vectorDrawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        vectorDrawable.setBounds(0, 0, canvas.width, canvas.height)
        vectorDrawable.draw(canvas)
        return bitmap
    }
    private fun saveDataLembur(pekerja: Pekerja,perusahaan: Perusahaan){
        val url = "https://selected-jaguar-presently.ngrok-free.app/api/"

        val retrofit = Retrofit.Builder()
            .baseUrl(url)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val apiService = retrofit.create(ApiService::class.java)
        val nama_Perusahaan = createPartFromString(perusahaan.nama)
        val nama = createPartFromString(pekerja.nama)
        val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID"))
        val dateInput = TITanggal.editText?.text.toString()
        val parsedDate = dateFormat.parse(dateInput)
        val sqlDate = Date(parsedDate?.time ?: 0)
        val tanggal = createPartFromString(sqlDate.toString()) // Convert back to SQL date format
        val waktu_masuk = createPartFromString(TIMasuk.editText?.text.toString())
        val waktu_pulang = createPartFromString(TIPulang.editText?.text.toString())
        val kegiatan = createPartFromString(TIPekerjaan.editText?.text.toString())

        val buktifile = selectedFile
        val requestFile = RequestBody.create(MediaType.parse("pdf/*"), buktifile)
        val buktipart = MultipartBody.Part.createFormData("bukti", buktifile.name, requestFile)
        val call = apiService.uploadLembur(nama_Perusahaan,nama,tanggal, waktu_masuk,waktu_pulang, kegiatan, buktipart)
        call.enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                if (response.isSuccessful) {
                    val vectorDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.done_bitmap)
                    val bitmap = vectorDrawable?.let { vectorToBitmap(it) }
                    save.doneLoadingAnimation(Color.parseColor("#AAFF00"), bitmap)
                    val apiResponse = response.body()
                    Log.d("ApiResponse", "Status: ${apiResponse?.status}, Message: ${apiResponse?.message}")
                    MotionToast.createToast(requireActivity(), "Add Lembur Success",
                        "Data Lembur Berhasil Ditambahkan",
                        MotionToastStyle.SUCCESS,
                        MotionToast.GRAVITY_BOTTOM,
                        MotionToast.LONG_DURATION,
                        ResourcesCompat.getFont(requireContext(), R.font.ralewaybold))
                } else {
                    save.revertAnimation()
                    val errorMessage = response.errorBody()?.string() ?: "Unknown error"
                    Log.e("ApiResponse", "Error: ${response.message()} - $errorMessage")
                }
            }

            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                save.revertAnimation()
                Log.e("ApiResponse", "Request failed: ${t.message}")
            }
        })
        setLoading(false)
    }
    private fun createPartFromString(value: String): RequestBody {
        return RequestBody.create(MediaType.parse("text/plain"), value)
    }
    private fun stringToSqlTime(timeString: String): Time {
        val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val date = dateFormat.parse(timeString)
        return Time(date.time)
    }
}
