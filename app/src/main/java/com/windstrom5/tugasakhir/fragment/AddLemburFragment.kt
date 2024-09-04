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
import com.bumptech.glide.Glide
import com.saadahmedev.popupdialog.PopupDialog
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog
import com.windstrom5.tugasakhir.activity.RegisterActivity
import com.windstrom5.tugasakhir.connection.ApiResponse
import com.windstrom5.tugasakhir.connection.ApiService
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
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
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_add_lembur, container, false)
        TINama = view.findViewById(R.id.nama)
        getBundle()
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
            save.startAnimation()
            pekerja?.let { it1 -> perusahaan?.let { it2 -> saveDataLembur(it1, it2) } }
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
    private fun showDatePickerDialog(editText: EditText) {
        // Load holidays from JSON
        val holidaysMap = loadHolidaysFromJson(requireContext())

        val disabledDays = holidaysMap.keys.toTypedArray()

        val now = Calendar.getInstance()

        val dpd = DatePickerDialog.newInstance(
            DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
                val selectedDate = Calendar.getInstance().apply {
                    set(year, monthOfYear, dayOfMonth)
                }

                // Check if the selected date is a holiday
                if (holidaysMap.containsKey(selectedDate)) {
                    // Show Toast
                    Toast.makeText(
                        requireContext(),
                        "Selected date is a holiday. Please choose another date.",
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

        dpd.setDisabledDays(disabledDays)
        dpd.show(childFragmentManager, "DatePickerDialog")
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
        val url = "http://192.168.1.6:8000/api/"

        val retrofit = Retrofit.Builder()
            .baseUrl(url)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val apiService = retrofit.create(ApiService::class.java)
        val nama_Perusahaan = createPartFromString(perusahaan.nama)
        val nama = createPartFromString(pekerja.nama)
        val tanggal = createPartFromString(TITanggal.editText?.text.toString())
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
    private fun createPartFromString(value: String): RequestBody {
        return RequestBody.create(MediaType.parse("text/plain"), value)
    }
}
