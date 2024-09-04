package com.windstrom5.tugasakhir.activity

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.text.InputType
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.res.ResourcesCompat
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.input.input
import com.bumptech.glide.Glide
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.windstrom5.tugasakhir.R
import com.windstrom5.tugasakhir.connection.ApiResponse
import com.windstrom5.tugasakhir.connection.ApiService
import com.windstrom5.tugasakhir.connection.ReverseGeocoder
import com.windstrom5.tugasakhir.databinding.ActivityEditCompanyBinding
import com.windstrom5.tugasakhir.model.Admin
import com.windstrom5.tugasakhir.model.Pekerja
import com.windstrom5.tugasakhir.model.Perusahaan
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
        val url = "http://192.168.1.6:8000/api/"

        val retrofit = Retrofit.Builder()
            .baseUrl(url)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val apiService = retrofit.create(ApiService::class.java)
        val nama = createPartFromString(textNama.toString())
        val jammasuk = createPartFromString(textJamMasuk.toString())
        val jamkeluar = createPartFromString(textJamKeluar.toString())
        val latitude = createPartFromString(latitude.toString())
        val longitude = createPartFromString(longitude.toString())

        val call: Call<ApiResponse>

        if (selectedFile != null) {
            val requestFile = RequestBody.create(MediaType.parse("image/*"), selectedFile)
            val logoPart = MultipartBody.Part.createFormData("logo", selectedFile!!.name, requestFile)
            call = apiService.updatePerusahaan(Id, nama, jammasuk, jamkeluar, latitude, longitude, logoPart)
        } else {
            call = apiService.updatePerusahaanNoFile(Id, nama, jammasuk, jamkeluar, latitude, longitude)
        }
        call.enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    Log.d("ApiResponse", "Status: ${apiResponse?.status}, Message: ${apiResponse?.message}")
                    MotionToast.createToast(
                        this@EditCompany,
                        "Update User Success",
                        "Data User Berhasil Diperbarui",
                        MotionToastStyle.SUCCESS,
                        MotionToast.GRAVITY_BOTTOM,
                        MotionToast.LONG_DURATION,
                        ResourcesCompat.getFont(this@EditCompany, R.font.ralewaybold)
                    )
                } else {
                    Log.e("ApiResponse", "Error: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                Log.e("ApiResponse", "Request failed: ${t.message}")
            }
        })
        setLoading(false)
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
        MaterialDialog(this).show {
            title(text = "Enter New Company Name")
            input(hint = "Nama Perusahaan", prefill = textNama.text.toString()) { dialog, text ->
                // Handle the text input when the user clicks OK
                val enteredText = text.toString()
                textNama.setText(enteredText)
                // Do something with the entered text
            }
            positiveButton(text = "OK")
            negativeButton(text = "Cancel") { dialog ->
                // Handle the cancellation if needed
                dialog.dismiss()
            }
        }
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
                if(logo == null){
                    val imageUrl =
                        "http://192.168.1.6:8000/storage/${perusahaan?.logo}"
                    Glide.with(this)
                        .load(imageUrl)
                        .into(profile)
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
                        "http://192.168.1.6:8000/storage/${perusahaan?.logo}" // Replace with your Laravel image URL
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