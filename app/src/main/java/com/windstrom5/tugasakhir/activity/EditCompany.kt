package com.windstrom5.tugasakhir.activity

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.res.ResourcesCompat
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.setActionButtonEnabled
import com.afollestad.materialdialogs.input.getInputField
import com.afollestad.materialdialogs.input.input
import com.android.volley.toolbox.ImageRequest
import com.android.volley.toolbox.Volley
import com.bumptech.glide.Glide
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.windstrom5.tugasakhir.R
import com.windstrom5.tugasakhir.connection.ApiResponse
import com.windstrom5.tugasakhir.connection.ApiService
import com.windstrom5.tugasakhir.connection.ReverseGeocoder
import com.windstrom5.tugasakhir.connection.SharedPreferencesManager
import com.windstrom5.tugasakhir.databinding.ActivityEditCompanyBinding
import com.windstrom5.tugasakhir.model.Admin
import com.windstrom5.tugasakhir.model.Pekerja
import com.windstrom5.tugasakhir.model.Perusahaan
import com.windstrom5.tugasakhir.model.UpdatedPerusahaan
import de.hdodenhof.circleimageview.CircleImageView
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.osmdroid.util.GeoPoint
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import www.sanju.motiontoast.MotionToast
import www.sanju.motiontoast.MotionToastStyle
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.sql.Time
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class EditCompany : AppCompatActivity() {
    private lateinit var binding: ActivityEditCompanyBinding
    private lateinit var editNama : ImageView
    private lateinit var editJamMasuk: ImageView
    private lateinit var editJamKeluar: ImageView
    private lateinit var editLocation : ImageView
    private var perusahaan : Perusahaan? = null
    private var admin : Admin? = null
    private var pekerja : Pekerja? = null
    private var selectedFile: File?= null
    private var bundle: Bundle? = null
    private lateinit var enteredText: String
    private lateinit var role:String
    private lateinit var textNama : TextView
    private lateinit var textJamMasuk : TextView
    private lateinit var textJamKeluar : TextView
    private lateinit var textalamat : TextView
    private lateinit var changeProfile : ImageView
    private lateinit var edituang : ImageView
    private lateinit var profile:CircleImageView
    private var latitude:Double? = null
    private lateinit var id:TextView
    private var longitude:Double? = null
    companion object {
        private const val PICK_IMAGE_REQUEST_CODE = 123
    }
    private lateinit var save: Button
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditCompanyBinding.inflate(layoutInflater)
        setContentView(binding.root)
        textNama = binding.companyNameTextView
        id = binding.idperusahaan
        textJamKeluar = binding.JamKeluarText
        textJamMasuk = binding.jamMasukText
        textalamat = binding.alamat
        changeProfile = binding.selectImage
//        edituang = binding.edituanglembur
        profile = binding.logo
        getBundle()
        editJamMasuk = binding.editJamMasuk
        editNama = binding.editNamaPerusahaan
        editLocation = binding.editLocation
        editJamKeluar = binding.editJamKeluar
        editNama.setOnClickListener{
            showNamaTextDialog()
        }
        changeProfile.setOnClickListener{
            pickImageFromGallery()
        }
        editLocation.setOnClickListener{
            val intent = Intent(this, MapActivity::class.java)
            val bundle = Bundle()
            bundle.putParcelable("perusahaan",perusahaan)
            bundle.putString("namaPerusahaan", textNama.toString())
            bundle.putString("openhour", textJamMasuk.toString())
            bundle.putString("closehour", textJamKeluar.toString())
            bundle.putString("role",role)
            if(role == "Admin"){
                bundle.putParcelable("user", admin)
            }else{
                bundle.putParcelable("user", pekerja)
            }
            if(selectedFile != null){
                val byteArrayOutputStream = ByteArrayOutputStream()
                val objectOutputStream = ObjectOutputStream(byteArrayOutputStream)
                objectOutputStream.writeObject(selectedFile)
                objectOutputStream.flush()
                val byteArray = byteArrayOutputStream.toByteArray()
                bundle.putByteArray("selectedFile", byteArray)
            }
            bundle.putParcelable("perusahaan", perusahaan)
            bundle.putString("category","edit")
            intent.putExtra("data", bundle)
            startActivity(intent)
        }
        editJamKeluar.setOnClickListener{
            showTimePicker(textJamKeluar)
        }
        editJamMasuk.setOnClickListener{
            showTimePicker(textJamMasuk)
        }
//        edituang.setOnClickListener{
//            showMoneyInputDialog()
//        }
        save = binding.saveButton
        save.setOnClickListener{
//            val editedNama = if (textNama.text.toString() == perusahaan?.nama) null else textNama.text.toString()
//            val jammasukedited = if ( textJamMasuk.text.toString() == perusahaan?.jam_masuk.toString()) null else  textJamMasuk.text.toString()
//            val jamkeluaredited = if ( textJamKeluar.text.toString() == perusahaan?.jam_keluar.toString()) null else  textJamKeluar.text.toString()
//            val alamat = if (textalamat.text.toString() == ReverseGeocoder.getFullAddressFromLocation(this@EditCompany, GeoPoint(perusahaan!!.latitude, perusahaan!!.longitude))) null else textalamat.text.toString()
            perusahaan?.id?.let { it1 -> updateData(it1) }
        }
    }

//    private fun showMoneyInputDialog() {
//        MaterialDialog(this).show {
//            title(text = "Enter Amount")
//            input(
//                hint = "Amount",
//                prefill = "IDR 120.000",
//                inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
//            ) { dialog, text ->
//                val formattedAmount = formatCurrency(text.toString())
//                findViewById<TextView>(R.id.UangLemburText).text = formattedAmount
//            }
//            positiveButton(text = "OK")
//            negativeButton(text = "Cancel")
//        }
//    }

    // Format input as currency
    private fun formatCurrency(amount: String): String {
        val localeID = Locale("in", "ID")
        val numberFormat = NumberFormat.getCurrencyInstance(localeID)
        return numberFormat.format(amount.toDoubleOrNull() ?: 0.0)
    }
    private fun setLoading(isLoading: Boolean) {
        val loadingLayout = findViewById<LinearLayout>(R.id.layout_loading)
        if (isLoading) {
            loadingLayout?.visibility = View.VISIBLE
        } else {
            loadingLayout?.visibility = View.INVISIBLE
        }
    }

    private fun updateData(Id: Int) {
        val url = "https://selected-jaguar-presently.ngrok-free.app/api/"
        val sharedPreferencesManager = SharedPreferencesManager(this@EditCompany)
        val retrofit = Retrofit.Builder()
            .baseUrl(url)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val apiService = retrofit.create(ApiService::class.java)
        val nama = createPartFromString(textNama.text.toString())
        val jam_masuk = stringToSqlTime(textJamMasuk.text.toString())
        val jam_keluar = stringToSqlTime(textJamKeluar.text.toString())
        val jammasuk = createPartFromString(jam_masuk.toString())
        val jamkeluar = createPartFromString(jam_keluar.toString())
        val latitude = createPartFromString(latitude.toString())
        val longitude = createPartFromString(longitude.toString())
        val call: Call<ApiResponse>
//        if (selectedFile != null) {
        val logoPart = selectedFile?.let {
            val requestFile = RequestBody.create(MediaType.parse("image/jpeg"), it)
            MultipartBody.Part.createFormData("logo", it.name, requestFile)
        }
        Log.d("ApiResponse",selectedFile.toString())
        call = apiService.updatePerusahaan(Id, nama, jammasuk, jamkeluar, latitude, longitude, logoPart)

//        }else {
//            call = if (admin != null) {
//                apiService.updateAdminNoFile(Id, nama, tanggal, email)
//            } else {
//                apiService.updatePekerjaNoFile(Id, nama, tanggal, email)
//            }
//        }
        call.enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                if (response.isSuccessful) {
                    val apiResponse = response.body()

                    Log.d("ApiResponse", "Status: ${apiResponse?.status}, Message: ${apiResponse?.message}")
                    MotionToast.createToast(
                        this@EditCompany,
                        "Update Perusahaan Success",
                        "Data Perusahaan Berhasil Diperbarui",
                        MotionToastStyle.SUCCESS,
                        MotionToast.GRAVITY_BOTTOM,
                        MotionToast.LONG_DURATION,
                        ResourcesCompat.getFont(this@EditCompany, R.font.ralewaybold)
                    )

                    val updatedPerusahaan = apiResponse?.perusahaan as UpdatedPerusahaan
                    val dateParser = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val javaUtilDate = dateParser.parse(updatedPerusahaan.batas_aktif)
                    val sqlDate = java.sql.Date(javaUtilDate.time)
                    val perusahaanUpdated = Perusahaan(
                        updatedPerusahaan.id,
                        updatedPerusahaan.nama,
                        updatedPerusahaan.latitude,
                        updatedPerusahaan.longitude,
                        stringToSqlTime(updatedPerusahaan.jam_masuk),
                        stringToSqlTime(updatedPerusahaan.jam_keluar),
                        sqlDate,
                        updatedPerusahaan.logo,
                        updatedPerusahaan.secret_key,
                        updatedPerusahaan.holiday
                    )
                    Log.d("currecnt",updatedPerusahaan.toString())
                    sharedPreferencesManager.removePerusahaan()
                    sharedPreferencesManager.savePerusahaan(perusahaanUpdated)
                    val intent = Intent(this@EditCompany, CompanyActivity::class.java)
                    val userBundle = Bundle()
                    userBundle.putParcelable("user", if (admin != null) admin else pekerja)
                    userBundle.putParcelable("perusahaan", perusahaanUpdated)
                    userBundle.putString("role", if (admin != null) "Admin" else "Pekerja")
                    intent.putExtra("data", userBundle)
                    startActivity(intent)
                } else {
                    // Log the error body from the response
                    val errorBody = response.errorBody()?.string()
                    Log.e("ApiResponse", "Error: ${response.code()}, Error Body: $errorBody, nama = ${textNama.text}")

                    // You can also show a toast or a MotionToast with the error message
                    MotionToast.createToast(
                        this@EditCompany,
                        "Update Perusahaan Failed",
                        "Error: $errorBody",
                        MotionToastStyle.ERROR,
                        MotionToast.GRAVITY_BOTTOM,
                        MotionToast.LONG_DURATION,
                        ResourcesCompat.getFont(this@EditCompany, R.font.ralewaybold)
                    )
                }
                setLoading(false)
            }

            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                Log.e("ApiResponse", "Request failed: ${t.message}",)
                setLoading(false)
            }
        })
        setLoading(false)
    }

    private fun stringToSqlTime(timeString: String): Time {
        val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val date = dateFormat.parse(timeString)
        return Time(date.time)
    }
    private fun createPartFromString(value: String): RequestBody {
        return RequestBody.create(MediaType.parse("text/plain"), value)
    }
    private fun pickImageFromGallery() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        startActivityForResult(intent, PICK_IMAGE_REQUEST_CODE)
    }
    private fun showTimePicker(textView: TextView) {
        val calendar = Calendar.getInstance()

        val timePicker = MaterialTimePicker.Builder()
            .setHour(calendar.get(Calendar.HOUR_OF_DAY))
            .setMinute(calendar.get(Calendar.MINUTE))
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .build()

        timePicker.addOnPositiveButtonClickListener {
            calendar.set(Calendar.HOUR_OF_DAY, timePicker.hour)
            calendar.set(Calendar.MINUTE, timePicker.minute)

            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val formattedTime = timeFormat.format(calendar.time)

            textView.setText(formattedTime)
        }

        timePicker.show(supportFragmentManager, "timePicker")
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            data?.data?.let { imageUri ->
                // Get the real path from the URI
                val realPath = getRealPathFromUri(imageUri)
                if (realPath != null) {
                    selectedFile = File(realPath)
                    Glide.with(this)
                        .load(imageUri) // Load the image using the URI
                        .into(profile) // Set it into the ImageView
                }
            }
        }
    }
    private fun getRealPathFromUri(uri: Uri): String? {
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        val cursor = contentResolver.query(uri, projection, null, null, null)
        cursor?.use {
            val columnIndex = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            it.moveToFirst()
            return it.getString(columnIndex)
        }
        return null
    }
    private fun showNamaTextDialog() {
        val initialText = textNama.text.toString()
        val titleText = "Nama Perusahaan"

        val dialog = MaterialDialog(this).show {
            title(text = titleText)  // Set the dialog title
            input(
                hint = "Masukkan Nama",
                prefill = initialText,
                inputType = InputType.TYPE_CLASS_TEXT
            ) { dialog, input ->
                // Enable the positive button only if the input is not empty
                dialog.setActionButtonEnabled(WhichButton.POSITIVE, input.isNotBlank())
            }
            positiveButton(text = "OK") { dialog ->
                val enteredText = dialog.getInputField().text.toString()
                textNama.setText(enteredText)
            }
            negativeButton(text = "Cancel")
        }
        dialog.getInputField().addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val inputText = s.toString()
                val isValid = inputText.isNotBlank()
                dialog.setActionButtonEnabled(WhichButton.POSITIVE, isValid)

                // Show error if input is invalid
                dialog.getInputField().error = when {
                    inputText.isBlank() -> "Nama cannot be empty"
                    else -> null // Clear error if valid
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }


    private fun getBundle() {
        bundle = intent?.getBundleExtra("data")
        if (bundle != null) {
            bundle?.let {
                perusahaan = it.getParcelable("perusahaan")
                val logo = perusahaan?.logo
                role = it.getString("role").toString()
                if(role == "Admin"){
                    admin = it.getParcelable("user")
                }else{
                    pekerja = it.getParcelable("user")
                }
                if(logo != null){
                    val imageUrl =
                        "https://selected-jaguar-presently.ngrok-free.app/api/Perusahaan/decryptLogo/${perusahaan?.id}"
                    val imageRequest = ImageRequest(
                        imageUrl,
                        { response ->
                            // Set the Bitmap to an ImageView or handle it as needed
                            profile.setImageBitmap(response)
                        },
                        0, 0, ImageView.ScaleType.CENTER_CROP, Bitmap.Config.RGB_565,
                        { error ->
                            error.printStackTrace()
                            Toast.makeText(this, "Failed to fetch profile image", Toast.LENGTH_SHORT).show()
                        }
                    )
                    val requestQueue = Volley.newRequestQueue(this)
                    requestQueue.add(imageRequest)
                }else{
                    Glide.with(this)
                        .load(R.drawable.logo)
                        .into(profile)
                }
                id.setText("ID : ${perusahaan?.id}")
                textNama.setText(perusahaan?.nama)
                val inputFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                val outputFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                val jamMasukDate = inputFormat.parse(perusahaan?.jam_masuk.toString())
                val jamKeluarDate = inputFormat.parse(perusahaan?.jam_keluar.toString())

                val jamMasukFormatted = outputFormat.format(jamMasukDate)
                val jamKeluarFormatted = outputFormat.format(jamKeluarDate)
                textJamMasuk.setText(jamMasukFormatted.toString())
                textJamKeluar.setText(jamKeluarFormatted.toString())
                latitude = perusahaan!!.latitude
                longitude = perusahaan!!.longitude
                val addressInfo = ReverseGeocoder.getFullAddressFromLocation(this@EditCompany, GeoPoint(perusahaan!!.latitude, perusahaan!!.longitude))
                textalamat.text = addressInfo
            }
        } else {
            bundle = intent?.getBundleExtra("edit")
            if (bundle != null) {
                bundle?.let {
                    perusahaan = it.getParcelable("perusahaan")
                    role = it.getString("role").toString()
                    if(role == "Admin"){
                        admin = it.getParcelable("user")
                    }else{
                        pekerja = it.getParcelable("user")
                    }
                    val byteArray = intent.getByteArrayExtra("selectedFile")
                    if(byteArray!=null){
                        val objectInputStream = ObjectInputStream(ByteArrayInputStream(byteArray))
                        selectedFile = objectInputStream.readObject() as File
                    }
                    latitude = it.getDouble("latitude")
                    longitude = it.getDouble("longitude")
                    val imageUrl =
                        "https://selected-jaguar-presently.ngrok-free.app/api/Perusahaan/decryptLogo/${perusahaan?.id}" // Replace with your Laravel image URL
                    textNama.setText(it.getString("namaPerusahaan"))
                    textJamMasuk.setText(it.getString("openhour"))
                    textJamKeluar.setText(it.getString("closehour"))
                    val addressInfo = ReverseGeocoder.getFullAddressFromLocation(this@EditCompany, GeoPoint(
                        latitude!!, longitude!!
                    ))
                    textalamat.text = addressInfo
                    Glide.with(this)
                        .load(imageUrl)
                        .into(profile)
                }
            }else {
                Log.d("Error", "Bundle Not Found")
            }
        }
    }
}