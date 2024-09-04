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
import br.com.simplepass.loading_button_lib.customViews.CircularProgressButton
import com.bumptech.glide.Glide
import com.github.barteksc.pdfviewer.PDFView
import com.google.android.material.textfield.TextInputLayout
import com.saadahmedev.popupdialog.PopupDialog
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog
import com.windstrom5.tugasakhir.R
import com.windstrom5.tugasakhir.connection.ApiResponse
import com.windstrom5.tugasakhir.connection.ApiService
import com.windstrom5.tugasakhir.model.Pekerja
import com.windstrom5.tugasakhir.model.Perusahaan
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
            save.startAnimation()
            if(isAllFieldsFilled()){
                perusahaan?.let { it1 -> pekerja?.let { it2 -> saveDataIzin( it2, it1) } }
            }else{
                save.revertAnimation()
            }
        }
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
    private fun vectorToBitmap(vectorDrawable: Drawable): Bitmap {
        val bitmap = Bitmap.createBitmap(vectorDrawable.intrinsicWidth,
            vectorDrawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        vectorDrawable.setBounds(0, 0, canvas.width, canvas.height)
        vectorDrawable.draw(canvas)
        return bitmap
    }
    private fun saveDataIzin(pekerja: Pekerja,perusahaan: Perusahaan){
        val url = "http://192.168.1.6:8000/api/"

        val retrofit = Retrofit.Builder()
            .baseUrl(url)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val apiService = retrofit.create(ApiService::class.java)
        val nama_Perusahaan = createPartFromString(perusahaan.nama)
        val nama = createPartFromString(pekerja.nama)
        val kategori = createPartFromString(acIzin.text.toString())
        val tanggal = createPartFromString(TITanggal.editText?.text.toString())
        val alasan = createPartFromString(TIAlasan.editText?.text.toString())
        val buktifile = selectedFile
        val requestFile = RequestBody.create(MediaType.parse("pdf/*"), buktifile)
        val buktipart = MultipartBody.Part.createFormData("bukti", buktifile.name, requestFile)
        val call = apiService.uploadIzin(nama_Perusahaan,nama,tanggal, kategori,alasan, buktipart)
        call.enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                if (response.isSuccessful) {
                    val vectorDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.done_bitmap)
                    val bitmap = vectorDrawable?.let { vectorToBitmap(it) }
                    save.doneLoadingAnimation(Color.parseColor("#AAFF00"), bitmap)
                    val apiResponse = response.body()
                    Log.d("ApiResponse", "Status: ${apiResponse?.status}, Message: ${apiResponse?.message}")
                    MotionToast.createToast(requireActivity(), "Add Izin Success",
                        "Data Dinas Berhasil Ditambahkan",
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