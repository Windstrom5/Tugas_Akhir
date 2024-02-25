package com.example.tugasakhir

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.res.ResourcesCompat
import br.com.simplepass.loading_button_lib.customViews.CircularProgressEditText
import com.example.tugasakhir.connection.ApiService
import com.example.tugasakhir.connection.RetrofitClient
import com.example.tugasakhir.databinding.ActivityRegisterAdminBinding
import com.example.tugasakhir.model.Admin
import com.example.tugasakhir.model.Perusahaan
import com.google.android.material.textfield.TextInputLayout
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import www.sanju.motiontoast.MotionToast
import www.sanju.motiontoast.MotionToastStyle
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class RegisterAdminActivity : AppCompatActivity() {
    private lateinit var TINama: TextInputLayout
    private lateinit var edNama: EditText
    private lateinit var TIEmail: TextInputLayout
    private lateinit var edEmail: EditText
    private lateinit var TIPassword: TextInputLayout
    private lateinit var edPassword: EditText
    private lateinit var TITanggal: TextInputLayout
    private lateinit var edTanggal: EditText
    private lateinit var save : CircularProgressEditText
    private lateinit var admin: Admin
    private lateinit var bundle: Bundle
    private lateinit var binding: ActivityRegisterAdminBinding
    private lateinit var circleImageView: CircleImageView
    private lateinit var selectedImage: ByteArray
    var selectedDateSqlFormat: String? = null
    private lateinit var selectImage: ImageView
    companion object {
        private const val PICK_IMAGE_REQUEST_CODE = 1
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)
        circleImageView = binding.circleImageView
        selectImage = binding.selectImage
        TIEmail = binding.textInputEmail
        TIPassword = binding.textInputPassword
        TITanggal = binding.textInputTanggal
        TITanggal.editText?.apply {
            inputType = InputType.TYPE_NULL
            isFocusable = false
            isClickable = true
        }
        TITanggal.setOnClickListener{
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
                    val dateFormatSql = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    selectedDateSqlFormat = dateFormatSql.format(selectedDate.time)
                    TITanggal.editText?.setText(formattedDate)
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
        save = binding.cirsaveButton
        selectImage.setOnClickListener{
            openFilePicker()
        }
        save.setOnClickListener{
            val apiService = RetrofitClient.getClient("https://your-base-url.com/").create(ApiService::class.java)
            val emailRequestBody = RequestBody.create(MediaType.parse("text/plain"), TIEmail.editText?.text.toString())
            val passwordRequestBody = RequestBody.create(MediaType.parse("text/plain"), TIPassword.editText?.text.toString())
            val tanggalRequestBody = RequestBody.create(MediaType.parse("text/plain"), selectedDateSqlFormat)
            val imageRequestBody = RequestBody.create(MediaType.parse("image/*"), selectedImage)
            val imagePart = MultipartBody.Part.createFormData("image", "image.jpg", imageRequestBody)
            val call = apiService.uploadData(emailRequestBody, passwordRequestBody, tanggalRequestBody, imagePart)
            call.enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    if (response.isSuccessful) {
                        MotionToast.createToast(this@RegisterAdminActivity, "Success",
                            "Berhasil Menyimpan Data",
                            MotionToastStyle.SUCCESS,
                            MotionToast.GRAVITY_BOTTOM,
                            MotionToast.LONG_DURATION,
                            ResourcesCompat.getFont(this@RegisterAdminActivity,R.font.ralewaybold))
                    } else {
                        MotionToast.createToast(this@RegisterAdminActivity, "Error",
                            "Tidak Dapat Menyimpan Data",
                            MotionToastStyle.ERROR,
                            MotionToast.GRAVITY_BOTTOM,
                            MotionToast.LONG_DURATION,
                            ResourcesCompat.getFont(this@RegisterAdminActivity,R.font.ralewaybold))
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    MotionToast.createToast(this@RegisterAdminActivity,
                        "Failure",
                        "Something went wrong",
                        MotionToastStyle.ERROR,
                        MotionToast.GRAVITY_BOTTOM,
                        MotionToast.LONG_DURATION,
                        ResourcesCompat.getFont(this@RegisterAdminActivity,R.font.ralewaybold))
                }
            })
        }
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        startActivityForResult(intent, PICK_IMAGE_REQUEST_CODE)
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST_CODE && resultCode == RESULT_OK) {
            data?.data?.let { imageUri: Uri ->
                val inputStream = contentResolver.openInputStream(imageUri)
                selectedImage = inputStream?.readBytes() ?: ByteArray(0)
                circleImageView.setImageURI(imageUri)
            }
        }
    }
}