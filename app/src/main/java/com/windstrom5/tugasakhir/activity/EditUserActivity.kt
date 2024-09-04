package com.windstrom5.tugasakhir.activity

import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.res.ResourcesCompat
import com.android.volley.toolbox.ImageRequest
import com.android.volley.toolbox.Volley
import com.bumptech.glide.Glide
import com.google.android.material.textfield.TextInputLayout
import com.windstrom5.tugasakhir.R
import com.windstrom5.tugasakhir.connection.ApiResponse
import com.windstrom5.tugasakhir.connection.ApiService
import com.windstrom5.tugasakhir.databinding.ActivityEditUserBinding
import com.windstrom5.tugasakhir.fragment.AddLemburFragment
import com.windstrom5.tugasakhir.fragment.HistoryLemburFragment
import com.windstrom5.tugasakhir.model.Admin
import com.windstrom5.tugasakhir.model.Pekerja
import com.windstrom5.tugasakhir.model.Perusahaan
import de.hdodenhof.circleimageview.CircleImageView
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import www.sanju.motiontoast.MotionToast
import www.sanju.motiontoast.MotionToastStyle
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class EditUserActivity : AppCompatActivity() {
    private lateinit var binding:ActivityEditUserBinding
    private lateinit var nama:TextView
    private lateinit var email:TextView
    private lateinit var tanggal:TextView
    private lateinit var editNama: ImageView
    private lateinit var editEmail: ImageView
    private lateinit var editTanggal: ImageView
    private lateinit var editProfile: ImageView
    private lateinit var alertDialog: AlertDialog
    private var bundle: Bundle? = null
    private var perusahaan : Perusahaan? = null
    private val CAMERA_CAPTURE_REQUEST = 126
    private val PICK_IMAGE_REQUEST_CODE = 1
    private var admin : Admin? = null
    private var selectedFile: File? = null
    private var pekerja : Pekerja? = null
    private lateinit var save:Button
    private lateinit var profilePicture: CircleImageView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditUserBinding.inflate(layoutInflater)
        setContentView(binding.root)
        nama = binding.NameTextView
        email = binding.textviewemail
        tanggal = binding.textviewtanggal
        editTanggal = binding.editTanggal
        editNama = binding.editNama
        editProfile = binding.selectImage
        editEmail = binding.editLocation
        save = binding.saveButton
        profilePicture = binding.logo
        getBundle()
        editNama.setOnClickListener{
            showNamaTextDialog()
        }
        editEmail.setOnClickListener{
            showEmailTextDialog()
        }
        editTanggal.setOnClickListener{
            val calendar = Calendar.getInstance()

            val datePicker = DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    // Update your TextInputEditText with the selected date
                    val selectedDate = Calendar.getInstance()
                    selectedDate.set(year, month, dayOfMonth)

                    // Format the date as needed
                    val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID"))
                    val formattedDate = dateFormat.format(selectedDate.time)
                    tanggal.setText(formattedDate)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )

            // Set date picker restrictions if needed
            datePicker.datePicker.maxDate = System.currentTimeMillis()  // Optional: Set a max date
            // datePicker.datePicker.minDate = System.currentTimeMillis() - 1000  // Optional: Set a min date

            datePicker.show()
        }
        editProfile.setOnClickListener{
            showImagePickerDialog()
        }
        save.setOnClickListener{
            if(admin!= null){
                admin!!.id?.let { it1 -> updateDataUser(it1) }
            }else{
                pekerja!!.id?.let { it1 -> updateDataUser(it1) }
            }
        }
    }
    private fun createPartFromString(value: String): RequestBody {
        return RequestBody.create(MediaType.parse("text/plain"), value)
    }
    private fun updateDataUser(Id: Int) {
        val url = "http://192.168.1.6:8000/api/"

        val retrofit = Retrofit.Builder()
            .baseUrl(url)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val inputDateFormat = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID"))
        val outputDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val date = inputDateFormat.parse(tanggal.text.toString())
        val formattedDate = outputDateFormat.format(date)
        val apiService = retrofit.create(ApiService::class.java)
        val namaEdit = createPartFromString(nama.text.toString())
        val tanggalEdit = createPartFromString(formattedDate)
        val emailEdit = createPartFromString(email.text.toString())
        val call: Call<ApiResponse>
//        if (selectedFile != null) {
        val profilePath = selectedFile
            val profilePart = if (profilePath != null) {
                val requestFile = RequestBody.create(MediaType.parse("image/*"), profilePath)
                MultipartBody.Part.createFormData("profile", profilePath.name, requestFile)
            } else {
                null
            }
            call = if (admin != null) {
                apiService.updateAdmin(Id, emailEdit, namaEdit,tanggalEdit, profilePart)
            } else {
                apiService.updatePekerja(Id, emailEdit, namaEdit,tanggalEdit, profilePart)
            }
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
                        this@EditUserActivity,
                        "Update User Success",
                        "Data User Berhasil Diperbarui",
                        MotionToastStyle.SUCCESS,
                        MotionToast.GRAVITY_BOTTOM,
                        MotionToast.LONG_DURATION,
                        ResourcesCompat.getFont(this@EditUserActivity, R.font.ralewaybold)
                    )
                    val intent = Intent(this@EditUserActivity, CompanyActivity::class.java)
                    val userBundle = Bundle()
                    userBundle.putParcelable("user", admin)
                    userBundle.putParcelable("perusahaan", perusahaan)
                    userBundle.putString("role", "Admin")
                    intent.putExtra("data", userBundle)
                    startActivity(intent)
                } else {
                    // Log the error body from the response
                    val errorBody = response.errorBody()?.string()
                    Log.e("ApiResponse", "Error: ${response.code()}, Error Body: $errorBody, nama = ${nama.text.toString()}")

                    // You can also show a toast or a MotionToast with the error message
                    MotionToast.createToast(
                        this@EditUserActivity,
                        "Update User Failed",
                        "Error: $errorBody",
                        MotionToastStyle.ERROR,
                        MotionToast.GRAVITY_BOTTOM,
                        MotionToast.LONG_DURATION,
                        ResourcesCompat.getFont(this@EditUserActivity, R.font.ralewaybold)
                    )
                }
                setLoading(false)
            }

            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                Log.e("ApiResponse", "Request failed: ${t.message}")
                setLoading(false)
            }
        })
        setLoading(false)
    }
    private fun setLoading(isLoading: Boolean) {
        val loadingLayout = findViewById<LinearLayout>(R.id.layout_loading)
        if (isLoading) {
            loadingLayout?.visibility = View.VISIBLE
        } else {
            loadingLayout?.visibility = View.INVISIBLE
        }
    }
    private fun showImagePickerDialog() {
        val options = arrayOf("Capture from Camera", "Select from File")
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Choose an option")
        builder.setItems(options) { _, which ->
            when (which) {
                0 -> openCamera()
                1 -> openFilePicker()
            }
        }
        alertDialog = builder.create()
        alertDialog.show()
    }
    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        startActivityForResult(intent, PICK_IMAGE_REQUEST_CODE)
    }
    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(packageManager) != null) {
            startActivityForResult(intent, CAMERA_CAPTURE_REQUEST)
        } else {
            // Handle the case where the camera app is not available
            Toast.makeText(this, "Camera app not found", Toast.LENGTH_SHORT).show()
        }
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            CAMERA_CAPTURE_REQUEST -> {
                if (resultCode == RESULT_OK) {
                    val imageBitmap = data?.extras?.get("data") as? Bitmap
                    imageBitmap?.let {
                        // Set the captured image to the CircleImageView
                        profilePicture.setImageBitmap(it)
                        // Convert the Bitmap to File
                        selectedFile = convertBitmapToFile(it)
                    }
                }
            }
            PICK_IMAGE_REQUEST_CODE -> {
                if (resultCode == RESULT_OK) {
                    data?.data?.let { imageUri: Uri ->
                        val realPath = getRealPathFromUri(imageUri)
                        if (realPath != null) {
                            selectedFile = File(realPath)
                            profilePicture.setImageURI(imageUri)
                        }
                    }
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
    private fun convertBitmapToFile(bitmap: Bitmap): File {
        // Create a temporary file
        val tempFile = File.createTempFile("temp_image", ".jpg", cacheDir)

        // Write the bitmap data into the file
        val outputStream = FileOutputStream(tempFile)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        outputStream.close()

        return tempFile
    }
    private fun showNamaTextDialog() {
        val textInputLayout = TextInputLayout(this@EditUserActivity)
        val editText = EditText(this@EditUserActivity)
        textInputLayout.hint = "Nama User"
        if(admin!= null){
            editText.setText(admin?.nama)
        }else{
            editText.setText(pekerja?.nama)
        }
        textInputLayout.addView(editText)

        val dialogBuilder = AlertDialog.Builder(this@EditUserActivity)
            .setView(textInputLayout)
            .setTitle("Enter Text")
            .setPositiveButton("OK") { dialog, which ->
                // Handle the text input when the user clicks OK
                val enteredText = editText.text.toString()
                nama.setText(enteredText)
                // Do something with the entered text
            }
            .setNegativeButton("Cancel") { dialog, which ->
                // Handle the cancellation if needed
                dialog.dismiss()
            }

        val dialog = dialogBuilder.create()
        dialog.show()
    }
    private fun showEmailTextDialog() {
        val textInputLayout = TextInputLayout(this@EditUserActivity)
        val editText = EditText(this@EditUserActivity)
        textInputLayout.hint = "Nama Perusahaan"
        if(admin!= null){
            editText.setText(admin?.email)
        }else{
            editText.setText(pekerja?.email)
        }
        textInputLayout.addView(editText)

        val dialogBuilder = AlertDialog.Builder(this@EditUserActivity)
            .setView(textInputLayout)
            .setTitle("Enter Text")
            .setPositiveButton("OK") { dialog, which ->
                val enteredText = editText.text.toString()
                if(Patterns.EMAIL_ADDRESS.matcher(enteredText).matches()){
                    email.setText(enteredText)
                }else{
                    MotionToast.createToast(this@EditUserActivity, "Error",
                        "Email Tidak Valid",
                        MotionToastStyle.ERROR,
                        MotionToast.GRAVITY_BOTTOM,
                        MotionToast.LONG_DURATION,
                        ResourcesCompat.getFont(this@EditUserActivity, R.font.ralewaybold))
                }
                // Do something with the entered text
            }
            .setNegativeButton("Cancel") { dialog, which ->
                // Handle the cancellation if needed
                dialog.dismiss()
            }

        val dialog = dialogBuilder.create()
        dialog.show()
    }
    private fun getBundle() {
        bundle = intent?.getBundleExtra("data")
        if (bundle != null) {
            bundle?.let {
                perusahaan = it.getParcelable("perusahaan")
                val role = it.getString("role")
                val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID"))
                Log.d("Role",role.toString())
                if(role.toString() == "Admin"){
                    admin = it.getParcelable("user")
                    nama.setText(admin?.nama)
                    val formattedDate = admin?.tanggal_lahir?.let { it1 -> dateFormat.format(it1) }
                    tanggal.setText(formattedDate.toString())
                    email.setText(admin?.email)
                    if(admin?.profile != "null"){
                        val imageUrl =
                            "http://192.168.1.6:8000/api/Admin/decryptProfile/${admin?.id}"
                        val imageRequest = ImageRequest(
                            imageUrl,
                            { response ->
                                // Set the Bitmap to an ImageView or handle it as needed
                                profilePicture.setImageBitmap(response)
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
                            .load(R.drawable.profile)
                            .into(profilePicture)
                    }
                }else{
                    pekerja = it.getParcelable("user")
                    nama.setText(pekerja?.nama)
                    val formattedDate = pekerja?.tanggal_lahir?.let { it1 -> dateFormat.format(it1) }
                    tanggal.setText(formattedDate.toString())
                    email.setText(pekerja?.email)
                    if(pekerja?.profile != "null"){
                        val imageUrl =
                            "http://192.168.1.6:8000/api/Pekerja/decryptProfile/{${pekerja?.id}}"
                        val imageRequest = ImageRequest(
                            imageUrl,
                            { response ->
                                // Set the Bitmap to an ImageView or handle it as needed
                                profilePicture.setImageBitmap(response)
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
                            .load(R.drawable.profile)
                            .into(profilePicture)
                    }
                }
            }
        } else {
            Log.d("Error","Bundle Not Found")
        }
    }
}