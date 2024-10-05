package com.windstrom5.tugasakhir.feature

import android.app.Activity
import android.app.Dialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.InputType
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
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.TimePicker
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.DialogFragment
import br.com.simplepass.loading_button_lib.customViews.CircularProgressButton
import com.android.volley.toolbox.ImageRequest
import com.android.volley.toolbox.Volley
import com.bumptech.glide.Glide
import com.github.barteksc.pdfviewer.PDFView
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle
import com.google.android.material.textfield.TextInputLayout
import com.itextpdf.kernel.geom.Line
import com.saadahmedev.popupdialog.PopupDialog
import com.saadahmedev.popupdialog.listener.StandardDialogActionListener
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog
import com.windstrom5.tugasakhir.BuildConfig
import com.windstrom5.tugasakhir.R
import com.windstrom5.tugasakhir.activity.LoginActivity
import com.windstrom5.tugasakhir.connection.ApiResponse
import com.windstrom5.tugasakhir.connection.ApiService
import com.windstrom5.tugasakhir.connection.RetrievePDFfromUrl
import com.windstrom5.tugasakhir.connection.SharedPreferencesManager
import com.windstrom5.tugasakhir.model.Admin
import com.windstrom5.tugasakhir.model.Dinas
import com.windstrom5.tugasakhir.model.DinasItem
import com.windstrom5.tugasakhir.model.Izin
import com.windstrom5.tugasakhir.model.IzinItem
import com.windstrom5.tugasakhir.model.Lembur
import com.windstrom5.tugasakhir.model.LemburItem
import com.windstrom5.tugasakhir.model.Pekerja
import com.windstrom5.tugasakhir.model.Perusahaan
import com.windstrom5.tugasakhir.model.session_lembur
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import okio.Buffer
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
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.URL
import java.sql.Time
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


class PreviewDialogFragment: DialogFragment() {
    private val PICK_PDF_OR_IMAGE_REQUEST_CODE = 100
    private val PICK_IMAGE_REQUEST_CODE = 123
    private var selectedFile: File? = null
    private var lembur: LemburItem? = null
    private var dinas: DinasItem? = null
    private var izin: IzinItem? = null
    private var perusahaan:Perusahaan? = null
    private var category: String? = null
    private var isTIMasukFilled = false
    private var sesilembur: Int? = null
    private var islast:Boolean?= null
    private var sessionList: List<session_lembur>? = null
    private var adminList: List<Admin>? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout based on the layout type passed in arguments
        val layoutType = arguments?.getString("layoutType")
        val view = when (layoutType) {
            "dinas_layout" -> inflater.inflate(R.layout.preview_dinas, container, false)
            "lembur_layout" -> inflater.inflate(R.layout.preview_lembur, container, false)
            "session_lembur" -> inflater.inflate(R.layout.lembursession,container,false)
            else -> inflater.inflate(R.layout.preview_izin, container, false)
        }
        return view
    }
    private fun pickImageFromGallery() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        startActivityForResult(intent, PICK_IMAGE_REQUEST_CODE)
    }

    private fun calculateSessionsAdmin(
        waktuMasuk: Time,
        waktuPulang: Time,
        sessionLemburList: List<session_lembur>?
    ): Pair<List<Pair<String, Pair<String, String>>>, Int> {
        val sessions = mutableListOf<Pair<String, Pair<String, String>>>()
        val currentTime = Time(System.currentTimeMillis())
        val currentTimeMinutes = (currentTime.hours * 60) + currentTime.minutes
        var closestSessionIndex = -1

        val waktuMasukMinutes = (waktuMasuk.hours * 60) + waktuMasuk.minutes
        val waktuPulangMinutes = (waktuPulang.hours * 60) + waktuPulang.minutes

        val totalMinutesDifference = waktuPulangMinutes - waktuMasukMinutes
        val sessionCount = totalMinutesDifference / 60

        val timeFormatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

        for (i in 1..sessionCount) {
            val sessionStart = Time(waktuMasuk.time + (i - 1) * 3600000)
            val sessionEnd = if (i == sessionCount) {
                Time(waktuMasuk.time + totalMinutesDifference * 60000)
            } else {
                Time(waktuMasuk.time + i * 3600000)
            }

            val sessionStartString = timeFormatter.format(sessionStart)
            val sessionEndString = timeFormatter.format(sessionEnd)

            // Find matching lembur session
            val lemburSession = sessionLemburList?.find { session ->
                val lemburTime = Time(session.jam.time) // Convert session.jam to Time object

                Log.d("lemburAdminLog", "Formatted Lembur Time: ${timeFormatter.format(lemburTime)}")
                Log.d("lemburAdminLog", "Session Start: $sessionStartString")
                Log.d("lemburAdminLog", "Session End: $sessionEndString")

                // Break down lemburTime, sessionStart, and sessionEnd into hours and minutes
                val lemburMinutes = lemburTime.hours * 60 + lemburTime.minutes
                val sessionStartMinutes = sessionStart.hours * 60 + sessionStart.minutes
                val sessionEndMinutes = sessionEnd.hours * 60 + sessionEnd.minutes

                // Check if lemburTime is between sessionStart and sessionEnd
                lemburMinutes in sessionStartMinutes..sessionEndMinutes
            }

            Log.d("lemburAdminLog", "Waktu Masuk: ${timeFormatter.format(waktuMasuk)}")
            Log.d("lemburAdminLog", "Waktu Pulang: ${timeFormatter.format(waktuPulang)}")
            Log.d("lemburAdminLog", "Session List: $sessionLemburList")
            Log.d("lemburAdminLog", "Matched Session: $lemburSession")

            if (lemburSession != null) {
                sessions.add("Sesi $i" to (sessionStartString to sessionEndString))

                // Update the closest session index to the latest matching session
                closestSessionIndex = i - 1
            }
        }

        // Handle last partial session
        val remainingMinutes = totalMinutesDifference % 60
        if (remainingMinutes > 0 && remainingMinutes >= 30) {
            val lastSessionStart = Time(waktuMasuk.time + sessionCount * 3600000)
            val lastSessionEnd = waktuPulang

            val lastSessionStartString = timeFormatter.format(lastSessionStart)
            val lastSessionEndString = timeFormatter.format(lastSessionEnd)

            val lemburSession = sessionLemburList?.find { session ->
                val lemburTime = Time(session.jam.time)
                lemburTime.hours == lastSessionStart.hours && lemburTime.minutes == lastSessionStart.minutes
            }

            if (lemburSession != null) {
                sessions.add("Sesi ${sessionCount + 1}" to (lastSessionStartString to lastSessionEndString))

                // Update the closest session to the last partial session if found
                closestSessionIndex = sessionCount
            }
        }

        // Default closest session to last found matched session if none is closer
        if (closestSessionIndex == -1 && sessions.isNotEmpty()) {
            closestSessionIndex = sessions.size - 1
        }

        return Pair(sessions, closestSessionIndex)
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
            // Start of the session
            val sessionStart = Time(waktuMasuk.time + (i - 1) * 3600000)

            // End of the session
            val sessionEnd = if (i == sessionCount) {
                Time(waktuMasuk.time + totalMinutesDifference * 60000) // Exact waktuPulang for the last session
            } else {
                Time(waktuMasuk.time + i * 3600000) // Full-hour sessions
            }

            val sessionStartString = timeFormatter.format(sessionStart)
            val sessionEndString = timeFormatter.format(sessionEnd)

            // Add session to the list
            sessions.add("Sesi $i" to (sessionStartString to sessionEndString))

            // Calculate session start and end in minutes
            val sessionStartMinutes = (sessionStart.hours * 60) + sessionStart.minutes
            val sessionEndMinutes = (sessionEnd.hours * 60) + sessionEnd.minutes

            // Determine the closest session to the current time
            if (currentTimeMinutes > sessionStartMinutes && currentTimeMinutes <= sessionEndMinutes) {
                closestSessionIndex = i - 1
            }
        }

        // Handle the last session if it's less than a full hour
        val remainingMinutes = totalMinutesDifference % 60
        if (remainingMinutes > 0 && remainingMinutes >= 30) {
            // Add the last partial session if it is more than or equal to 30 minutes
            val lastSessionStart = Time(waktuMasuk.time + sessionCount * 3600000)
            val lastSessionEnd = waktuPulang

            val lastSessionStartString = timeFormatter.format(lastSessionStart)
            val lastSessionEndString = timeFormatter.format(lastSessionEnd)

            sessions.add("Sesi ${sessionCount + 1}" to (lastSessionStartString to lastSessionEndString))

            if (currentTimeMinutes > lastSessionStart.hours * 60 + lastSessionStart.minutes &&
                currentTimeMinutes <= lastSessionEnd.hours * 60 + lastSessionEnd.minutes) {
                closestSessionIndex = sessionCount // The last partial session
            }
        }

        // Ensure there's a valid closest session index
        if (closestSessionIndex == -1) {
            closestSessionIndex = sessionCount - 1
        }

        return Pair(sessions, closestSessionIndex)
    }


    private fun updateButtonsAndLayout(startTime: String, endTime: String) {
        val acceptButton = view?.findViewById<Button>(R.id.acceptButton)
        val rejectButton = view?.findViewById<Button>(R.id.rejectButton)
        val pekerjaan = view?.findViewById<TextInputLayout>(R.id.keteranganInputLayout)
        val change = view?.findViewById<Button>(R.id.changeFile)
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val currentTime = sdf.format(Date())
        val text = view?.findViewById<TextView>(R.id.text)
        val textfile = view?.findViewById<TextView>(R.id.textfile)
        val selectedFileName = view?.findViewById<TextView>(R.id.selectedFileName)
        val start = sdf.parse(startTime)!!
        val end = sdf.parse(endTime)!!
        val current = sdf.parse(currentTime)!!
        // Check if current time is within the selected session's time range
        if (current >= start && current <= end) {
            // Show 'Save' button and hide 'Cancel' button
            acceptButton?.text = "Save"
            rejectButton?.text = "Cancel"
            rejectButton?.visibility = View.VISIBLE
            pekerjaan?.isEnabled = true
            change?.isEnabled = true
            text?.visibility = View.VISIBLE
            textfile?.visibility = View.VISIBLE
            selectedFileName?.visibility = View.VISIBLE
            change?.visibility = View.VISIBLE
            // Make option layout visible
        } else if (current > end){
            acceptButton?.text = "Sesi Expired"
//            rejectButton?.isEnabled = false
            rejectButton?.text = "Cancel"
            rejectButton?.visibility = View.GONE
            pekerjaan?.isEnabled = false
            change?.isEnabled = false
            change?.visibility = View.GONE
            rejectButton?.visibility = View.GONE
            text?.visibility = View.GONE
            textfile?.visibility = View.GONE
            selectedFileName?.visibility = View.GONE
            // Make option layout visible
        } else {
            // Set 'Save' button to countdown or appropriate text
            val diffInMillis = start.time - current.time
            val diffInMinutes = (diffInMillis / (1000 * 60)).toInt()
            pekerjaan?.isEnabled = false
            change?.isEnabled = false
            change?.visibility = View.GONE
            text?.visibility = View.GONE
            textfile?.visibility = View.GONE
            selectedFileName?.visibility = View.GONE
            if (diffInMinutes > 0) {
                acceptButton?.text = "Waiting\n$diffInMinutes minutes"
            } else {
                acceptButton?.text = "Save"
            }
            rejectButton?.visibility = View.GONE

            // Hide option layout
        }
    }
    private fun updateSessionAdminDetails(selectedSession: Pair<String, Pair<String, String>>) {
        val startTime = selectedSession.second.first
        val endTime = selectedSession.second.second

        // Check for matching sessions
        if (sessionList != null) {
            val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

            // Parse the startTime and endTime from the selected session
            val startTimeDate = timeFormat.parse(startTime)
            val endTimeDate = timeFormat.parse(endTime)

            // Find the matching session based on the selected start and end time
            val matchingSessions = sessionList?.filter { session ->
                // Extract the time part from session.jam
                val sessionTime = timeFormat.format(session.jam)
                val sessionTimeDate = timeFormat.parse(sessionTime)

                Log.d("LemburLog", "Session time: $sessionTime")
                Log.d("LemburLog", "Start time: ${timeFormat.format(startTimeDate)}, End time: ${timeFormat.format(endTimeDate)}")

                // Compare the session time to the selected start and end times
                sessionTimeDate.after(startTimeDate) && sessionTimeDate.before(endTimeDate)
            }

            Log.d("LemburLog", "Matching sessions found: ${matchingSessions?.toString()}")
            val acceptButton = view?.findViewById<Button>(R.id.acceptButton)
            val rejectButton = view?.findViewById<Button>(R.id.rejectButton)
            val option = view?.findViewById<LinearLayout>(R.id.option)
            option?.visibility = View.GONE
            if (matchingSessions != null && matchingSessions.isNotEmpty()) {
                val firstMatchingSession = matchingSessions.first()
                sesilembur = firstMatchingSession.id
                // Update the keterangan field
                view?.findViewById<TextInputLayout>(R.id.keteranganInputLayout)?.editText?.setText(firstMatchingSession.keterangan)
                if(firstMatchingSession.status == "Pending"){
                    acceptButton?.setText("Confirm")
                    rejectButton?.setText("Reject")
                    rejectButton?.visibility=View.VISIBLE
                    acceptButton?.isEnabled=true
                }else{
                    acceptButton?.setText("Data Has Been " + firstMatchingSession.status)
                    rejectButton?.visibility=View.GONE
                    acceptButton?.isEnabled=false
                }
                // Load image based on matching session
                val imageView = view?.findViewById<ImageView>(R.id.imageView)
                val url = "http://192.168.1.5:8000/api/Lembur/Session/decryptBukti/${firstMatchingSession.id}"

                val imageRequest = ImageRequest(
                    url,
                    { response ->
                        imageView?.setImageBitmap(response)
                    },
                    0, 0, ImageView.ScaleType.CENTER_CROP, Bitmap.Config.RGB_565,
                    { error ->
                        error.printStackTrace()
                    }
                )
                imageView?.visibility = View.VISIBLE
                val requestQueue = Volley.newRequestQueue(requireContext())
                requestQueue.add(imageRequest)
            } else {
                Log.d("LemburLog", "No matching sessions found.")
                Log.d("LemburLog", "$startTimeDate - $endTimeDate")
                val imageView = view?.findViewById<ImageView>(R.id.imageView)
                imageView?.visibility = View.GONE
                view?.findViewById<TextInputLayout>(R.id.keteranganInputLayout)?.editText?.setText("")
                val change = view?.findViewById<Button>(R.id.changeFile)
                change?.setOnClickListener {
                    pickImageFromGallery()
                }
            }
        }
    }
    private fun sendEmailToAllAdmins(subject: String, message: String) {
        adminList?.forEach { admin ->
            val receiverEmail = admin.email
            EmailSender.sendEmail(receiverEmail, subject, message)
        }
    }
    private fun updateSessionDetails(selectedSession: Pair<String, Pair<String, String>>) {
        val startTime = selectedSession.second.first
        val endTime = selectedSession.second.second

        // Check for matching sessions
        if (sessionList != null) {
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

// Parse the startTime and endTime from the selected session
            val startTimeDate = timeFormat.parse(startTime)
            val endTimeDate = timeFormat.parse(endTime)

            val matchingSessions = sessionList?.filter { session ->
                // Extract the time part from session.jam
                val sessionTime = timeFormat.format(session.jam)
                val sessionTimeDate = timeFormat.parse(sessionTime)

                Log.d("LemburLog", "Session time: $sessionTime")
                Log.d("LemburLog", "Start time: ${timeFormat.format(startTimeDate)}, End time: ${timeFormat.format(endTimeDate)}")

                // Compare only the time portion (ignoring the date)
                sessionTimeDate.after(startTimeDate) && sessionTimeDate.before(endTimeDate)
            }
            Log.d("LemburLog", "Matching sessions found: ${matchingSessions?.toString()}")
            if (matchingSessions != null && matchingSessions.isNotEmpty()) {
                val firstMatchingSession = matchingSessions.first()

                // Update the keterangan field
                view?.findViewById<TextInputLayout>(R.id.keteranganInputLayout)?.editText?.setText(firstMatchingSession.keterangan)

                val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
//                val tanggalFormatted = dateFormatter.format(firstMatchingSession.jam)
//                view?.findViewById<TextInputLayout>(R.id.tanggalInputLayout)?.editText?.setText(tanggalFormatted)
                // Load image based on matching session
                val imageView = view?.findViewById<ImageView>(R.id.imageView)
                val url = "http://192.168.1.5:8000/api/Lembur/Session/decryptBukti/${firstMatchingSession.id}"

                val imageRequest = ImageRequest(
                    url,
                    { response ->
                        imageView?.setImageBitmap(response)
                    },
                    0, 0, ImageView.ScaleType.CENTER_CROP, Bitmap.Config.RGB_565,
                    { error ->
                        error.printStackTrace()
                    }
                )
                imageView?.visibility =View.VISIBLE
                val requestQueue = Volley.newRequestQueue(requireContext())
                requestQueue.add(imageRequest)
                setupButtonsForExistingSession(firstMatchingSession)
            } else {
                Log.d("LemburLog", "No matching sessions found.")
                Log.d("LemburLog", startTimeDate.toString() + " - " + endTimeDate.toString())
                val imageView = view?.findViewById<ImageView>(R.id.imageView)
                imageView?.visibility =View.GONE
                view?.findViewById<TextInputLayout>(R.id.keteranganInputLayout)?.editText?.setText("")
                val change = view?.findViewById<Button>(R.id.changeFile)
                change?.setOnClickListener{
                    pickImageFromGallery()
                }
                // Set up button actions for adding a new session
                setupButtonsForNewSession()
            }
        }

        updateButtonsAndLayout(startTime, endTime)
    }

    private fun setupButtonsForExistingSession(matchingSession: session_lembur) {
        val acceptButton = view?.findViewById<CircularProgressButton>(R.id.acceptButton)
        val rejectButton = view?.findViewById<CircularProgressButton>(R.id.rejectButton)
        if(category == "session_admin"){
            acceptButton?.setOnClickListener {
                acceptButton.startAnimation()
                updateDataSesi()
            }

            rejectButton?.setOnClickListener {
                rejectButton.startAnimation()
                // Handle cancel or rejection of session
                dismiss()
            }
        }else{
            acceptButton?.setOnClickListener {
                acceptButton.startAnimation()
                updateDataSesi()
            }

            rejectButton?.setOnClickListener {
                rejectButton.startAnimation()
                // Handle cancel or rejection of session
                dismiss()
            }
        }


    }

    private fun setupButtonsForNewSession() {
        val acceptButton = view?.findViewById<Button>(R.id.acceptButton)
        val rejectButton = view?.findViewById<Button>(R.id.rejectButton)
        acceptButton?.setOnClickListener {
            // Add new session data here
//            addNewSessionData()
            saveDataSesi()
            Toast.makeText(requireContext(), "New session added successfully!", Toast.LENGTH_SHORT).show()
            dismiss()
        }

        rejectButton?.setOnClickListener {
            // Handle cancel or rejection of session
            dismiss()
        }
    }
    private fun updateDataSesi(){
        val url = "http://192.168.1.5:8000/api/"

        val retrofit = Retrofit.Builder()
            .baseUrl(url)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val pekerjaan = view?.findViewById<TextInputLayout>(R.id.keteranganInputLayout)
        val apiService = retrofit.create(ApiService::class.java)
        val currentTimestamp = System.currentTimeMillis()
        val currentTime = Date(currentTimestamp)

        // Optionally format the timestamp (for logging or sending)
        val timeFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val formattedTimestamp = timeFormatter.format(currentTime)
        val jam = createPartFromString(formattedTimestamp.toString())
        val keterangan = createPartFromString(pekerjaan?.editText?.text.toString())
        val buktifile = selectedFile
        val buktiPart = if (buktifile != null) {
            val requestFile = RequestBody.create(MediaType.parse("image/*"), buktifile)
            MultipartBody.Part.createFormData("bukti", buktifile.name, requestFile)
        } else {
            null
        }
        val call = apiService.UpdateSessionLembur(lembur?.id,jam,keterangan,buktiPart)
        call.enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                if (response.isSuccessful) {
                    val vectorDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.done_bitmap)
                    val bitmap = vectorDrawable?.let { vectorToBitmap(it) }
                    view?.findViewById<CircularProgressButton>(R.id.acceptButton)?.doneLoadingAnimation(Color.parseColor("#AAFF00"), bitmap)
                    val apiResponse = response.body()
                    Log.d("ApiResponse", "Status: ${apiResponse?.status}, Message: ${apiResponse?.message}")
                    MotionToast.createToast(requireActivity(), "Add Lembur Success",
                        "Session Berhasil Ditambahkan",
                        MotionToastStyle.SUCCESS,
                        MotionToast.GRAVITY_BOTTOM,
                        MotionToast.LONG_DURATION,
                        ResourcesCompat.getFont(requireContext(), R.font.ralewaybold))
                    dismiss()
                } else {
                    val errorMessage = response.errorBody()?.string() ?: "Unknown error"
                    Log.e("ApiResponse", "Error: ${response.message()} - $errorMessage")
                }
            }

            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                Log.e("ApiResponse", "Request failed: ${t.message}")
            }
        })
//        setLoading(false)
    }
    private fun saveDataSesi(){
        val url = "http://192.168.1.5:8000/api/"

        val retrofit = Retrofit.Builder()
            .baseUrl(url)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val pekerjaan = view?.findViewById<TextInputLayout>(R.id.keteranganInputLayout)
        val apiService = retrofit.create(ApiService::class.java)
        val currentTimestamp = System.currentTimeMillis()
        val currentTime = Date(currentTimestamp)

        // Optionally format the timestamp (for logging or sending)
        val timeFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val formattedTimestamp = timeFormatter.format(currentTime)
        val jam = createPartFromString(formattedTimestamp.toString())
        val id_lembur = createPartFromString(lembur?.id.toString())
        val keterangan = createPartFromString(pekerjaan?.editText?.text.toString())
        val buktifile = selectedFile
        val requestFile = RequestBody.create(MediaType.parse("image/*"), buktifile)
        val buktipart = MultipartBody.Part.createFormData("bukti", buktifile?.name, requestFile)
        val call = apiService.AddSessionLembur(id_lembur,jam,keterangan,buktipart)
        call.enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                if (response.isSuccessful) {
                    val vectorDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.done_bitmap)
                    val bitmap = vectorDrawable?.let { vectorToBitmap(it) }
                    view?.findViewById<CircularProgressButton>(R.id.acceptButton)?.doneLoadingAnimation(Color.parseColor("#AAFF00"), bitmap)
                    val apiResponse = response.body()
                    Log.d("ApiResponse", "Status: ${apiResponse?.status}, Message: ${apiResponse?.message}")
                    MotionToast.createToast(requireActivity(), "Add Lembur Success",
                        "Session Berhasil Ditambahkan",
                        MotionToastStyle.SUCCESS,
                        MotionToast.GRAVITY_BOTTOM,
                        MotionToast.LONG_DURATION,
                        ResourcesCompat.getFont(requireContext(), R.font.ralewaybold))
                    dismiss()
                } else {
                    val errorMessage = response.errorBody()?.string() ?: "Unknown error"
                    Log.e("ApiResponse", "Error: ${response.message()} - $errorMessage")
                }
            }

            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                Log.e("ApiResponse", "Request failed: ${t.message}")
            }
        })
//        setLoading(false)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dinas = arguments?.getParcelable("dinas")
        lembur = arguments?.getParcelable("lembur")
        izin = arguments?.getParcelable("izin")
        category = arguments?.getString("category")
        sessionList = arguments?.getParcelableArrayList("lemburList")
        val encryption = BuildConfig.openssl_key
        if (dinas != null) {
            if(category == "Respond"){
                val namaInputLayout = view.findViewById<TextInputLayout>(R.id.namaInputLayout)
                namaInputLayout.isEnabled = false
                namaInputLayout.editText?.setText(dinas?.nama_pekerja)
                val dateFormatter = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID"))
                val tanggalBerangkatFormatted = dateFormatter.format(dinas?.tanggal_berangkat)
                val tanggalPulangFormatted = dateFormatter.format(dinas?.tanggal_pulang)
                val berangkatInputLayout = view.findViewById<TextInputLayout>(R.id.berangkatInputLayout)
                berangkatInputLayout.editText?.setText(tanggalBerangkatFormatted)
                val pulangInputLayout = view.findViewById<TextInputLayout>(R.id.pulangInputLayout)
                pulangInputLayout.editText?.setText(tanggalPulangFormatted)
                view.findViewById<TextInputLayout>(R.id.tujuanInputLayout2).visibility = View.GONE
                val tujuanInputLayout = view.findViewById<TextInputLayout>(R.id.tujuanInputLayout)
                tujuanInputLayout.visibility = View.VISIBLE
                tujuanInputLayout.isEnabled = false
                tujuanInputLayout.editText?.setText(dinas?.tujuan)

                val kegiatanInputLayout = view.findViewById<TextInputLayout>(R.id.kegiatanInputLayout)
                kegiatanInputLayout.isEnabled = false
                kegiatanInputLayout.editText?.setText(dinas?.kegiatan)

                val pdfUrl = "http://192.168.1.5:8000/api/Dinas/decryptBukti/${dinas?.id}"
                val pdfView = view.findViewById<PDFView>(R.id.pdfView)
                pdfView.visibility = View.VISIBLE
                val retrievePdfTask = RetrievePDFfromUrl(pdfView)
                retrievePdfTask.execute(pdfUrl)
                view.findViewById<CircularProgressButton>(R.id.acceptButton).setOnClickListener {
                    view.findViewById<CircularProgressButton>(R.id.acceptButton).startAnimation()
                    updateStatus("Accept","Dinas")
                    sendEmailToAllAdmins(
                        "Dinas Accepted",
                        "Your Dinas request has been accepted. Details: \n" +
                                "Nama: ${dinas?.nama_pekerja}\n" +
                                "Tanggal Berangkat: $tanggalBerangkatFormatted\n" +
                                "Tanggal Pulang: $tanggalPulangFormatted\n" +
                                "Tujuan: ${dinas?.tujuan}\n" +
                                "Kegiatan: ${dinas?.kegiatan}"
                    )
                    dismiss()
                }

                view.findViewById<CircularProgressButton>(R.id.rejectButton).setOnClickListener {
                    view.findViewById<CircularProgressButton>(R.id.acceptButton).startAnimation()
                    updateStatus("Reject","Dinas")
                    sendEmailToAllAdmins(
                        "Dinas Accepted",
                        "Your Dinas request has been rejected. Please contact the HR department for further details."
                    )
                    dismiss()
                }
            }else{
                val namaInputLayout = view.findViewById<TextInputLayout>(R.id.namaInputLayout)
                namaInputLayout.isEnabled = true
                namaInputLayout.editText?.setText(dinas?.nama_pekerja)
                val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val endIconDrawable: Drawable? = ContextCompat.getDrawable(requireContext(), R.drawable.baseline_calendar_month_24)
                endIconDrawable?.setBounds(0, 0, endIconDrawable.intrinsicWidth, endIconDrawable.intrinsicHeight)
                val tanggalBerangkatFormatted = dateFormatter.format(dinas?.tanggal_berangkat)
                val tanggalPulangFormatted = dateFormatter.format(dinas?.tanggal_pulang)
                val berangkatInputLayout = view.findViewById<TextInputLayout>(R.id.berangkatInputLayout)
                berangkatInputLayout.editText?.setText(tanggalBerangkatFormatted)
                val pulangInputLayout = view.findViewById<TextInputLayout>(R.id.pulangInputLayout)
                pulangInputLayout.editText?.setText(tanggalPulangFormatted)
                berangkatInputLayout.endIconMode = TextInputLayout.END_ICON_CUSTOM
                berangkatInputLayout.endIconDrawable = endIconDrawable
                pulangInputLayout.endIconMode = TextInputLayout.END_ICON_CUSTOM
                val delete = view.findViewById<CircularProgressButton>(R.id.rejectButton)
                delete.setOnClickListener{
                    PopupDialog.getInstance(requireContext())
                        .standardDialogBuilder()
                        .createIOSDialog()
                        .setHeading("Delete Data")
                        .setDescription(
                            "Are you sure you want to delete this data?" +
                                    " This action cannot be undone"
                        )
                        .build(object : StandardDialogActionListener {
                            override fun onPositiveButtonClicked(dialog: Dialog) {
                                dialog.dismiss()
                            }

                            override fun onNegativeButtonClicked(dialog: Dialog) {
                                dialog.dismiss()
                            }
                        })
                        .show()
                }
                pulangInputLayout.endIconDrawable = endIconDrawable
                berangkatInputLayout.setEndIconOnClickListener{
                    berangkatInputLayout.editText?.let { it1 -> showDatePickerDialog(it1) }
                }
                pulangInputLayout.setEndIconOnClickListener{
                    berangkatInputLayout.editText?.let { it1 -> showDatePickerDialog(it1) }
                }
                view.findViewById<TextInputLayout>(R.id.tujuanInputLayout).visibility = View.GONE
                val kotaDataList = readAndParseKotaJson()
                val provinsiDataList = readAndParseProvinsiJson()
                val combinedDataList = combineAndFormatData(kotaDataList, provinsiDataList)
                val adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    combinedDataList
                )
                val tujuan = dinas?.tujuan
                val tujuanInput = view.findViewById<AutoCompleteTextView>(R.id.actujuan)
                val tujuanLayout = view.findViewById<TextInputLayout>(R.id.tujuanInputLayout2)
                tujuanLayout.visibility = View.VISIBLE
                tujuanInput.setAdapter(adapter)
                if (combinedDataList.contains(tujuan)) {
                    val position = combinedDataList.indexOf(tujuan)
                    tujuanInput.setText(combinedDataList[position], false)
                }
                val kegiatanInputLayout = view.findViewById<TextInputLayout>(R.id.kegiatanInputLayout)
                kegiatanInputLayout.editText?.inputType = InputType.TYPE_TEXT_FLAG_MULTI_LINE
                kegiatanInputLayout.editText?.isFocusable = true
                kegiatanInputLayout.editText?.isFocusableInTouchMode = true
                kegiatanInputLayout.editText?.setText(dinas?.kegiatan)
                val pdfUrl = "http://192.168.1.5:8000/api/Dinas/decryptBukti/${dinas?.id}"
                val pdfView = view.findViewById<PDFView>(R.id.pdfView)
                pdfView.visibility = View.VISIBLE
                val retrievePdfTask = RetrievePDFfromUrl(pdfView)
                retrievePdfTask.execute(pdfUrl)
                val acceptButton = view.findViewById<CircularProgressButton>(R.id.acceptButton)
                acceptButton.setText("Save")
                val cancelButton = view.findViewById<CircularProgressButton>(R.id.rejectButton)
                cancelButton.setText("Cancel")
                val fileName = pdfUrl.substringAfterLast("/")
                view.findViewById<TextView>(R.id.text).visibility = View.VISIBLE
                view.findViewById<LinearLayout>(R.id.layout).visibility = View.VISIBLE
                view.findViewById<TextView>(R.id.selectedFileName).setText(fileName)
                view.findViewById<Button>(R.id.changeFile).setOnClickListener{
                    pickPdf()
                }
                cancelButton.setOnClickListener{
                    dismiss()
                }
                acceptButton.setOnClickListener{
                    view.findViewById<CircularProgressButton>(R.id.acceptButton).startAnimation()
//                    setLoading(true)
                    dinas!!.id?.let { it1 -> updateDataDinas(it1, berangkatInputLayout,pulangInputLayout,tujuanInput,kegiatanInputLayout) }
                    dismiss()
                }
            }

        } else if (lembur != null) {
            perusahaan = arguments?.getParcelable("perusahaan")
            if(category == "Respond") {
                view.findViewById<TextInputLayout>(R.id.namaInputLayout).editText?.setText(lembur?.nama_pekerja)
                view.findViewById<TextInputLayout>(R.id.tanggalInputLayout).editText?.setText(lembur?.tanggal.toString())
                view.findViewById<TextInputLayout>(R.id.masukInputLayout).editText?.setText(lembur?.waktu_masuk.toString())
                view.findViewById<TextInputLayout>(R.id.keluarInputLayout).editText?.setText(lembur?.waktu_pulang.toString())
                view.findViewById<TextInputLayout>(R.id.kegiatanInputLayout).editText?.setText(lembur?.pekerjaan)
                val imageView = view.findViewById<ImageView>(R.id.imageView)
                imageView.visibility = View.VISIBLE
                val url =
                    "http://192.168.1.5:8000/api/Lembur/decryptBukti/${lembur?.id}" // Replace with your actual URL
                val imageRequest = ImageRequest(
                    url,
                    { response ->
                        // Set the Bitmap to an ImageView or handle it as needed
                        imageView.setImageBitmap(response)
                    },
                    0, 0, ImageView.ScaleType.CENTER_CROP, Bitmap.Config.RGB_565,
                    { error ->
                        error.printStackTrace()
                        Toast.makeText(requireContext(), "Failed to fetch bukti image", Toast.LENGTH_SHORT).show()
                    }
                )
                val requestQueue = Volley.newRequestQueue(requireContext())
                requestQueue.add(imageRequest)
                view.findViewById<CircularProgressButton>(R.id.acceptButton).setOnClickListener {
                    view.findViewById<CircularProgressButton>(R.id.acceptButton).startAnimation()
                    updateStatus("Accept", "Lembur")
                    val subject = "Lembur Accepted"
                    val message = "Lembur for ${lembur?.nama_pekerja} on ${lembur?.tanggal} has been accepted."
                    sendEmailToAllAdmins(subject, message)
                    dismiss()
                }

                view.findViewById<CircularProgressButton>(R.id.rejectButton).setOnClickListener {
                    view.findViewById<CircularProgressButton>(R.id.acceptButton).startAnimation()
                    updateStatus("Reject", "Lembur")
                    val subject = "Lembur Rejected"
                    val message = "Lembur for ${lembur?.nama_pekerja} on ${lembur?.tanggal} has been rejected."
                    sendEmailToAllAdmins(subject, message)
                    dismiss()
                }
            }else if (category == "session_pekerja") {
                val acSesi = view.findViewById<AutoCompleteTextView>(R.id.ACsesi)

                // Calculate sessions and find the closest session index
                val (sessions, closestSessionIndex) = calculateSessions(lembur!!.waktu_masuk, lembur!!.waktu_pulang)

                // Ensure acSesi is not null and sessions are available
                if (acSesi != null && sessions.isNotEmpty()) {
                    // Extract session names for the dropdown
                    val sessionNames = sessions.map { it.first }
                    val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, sessionNames)
                    acSesi.setAdapter(adapter)

                    // Ensure closestSessionIndex is within bounds
                    val safeIndex = closestSessionIndex.coerceIn(0, sessions.size - 1)

                    // Set the text to the closest session
                    acSesi.setText(sessionNames[safeIndex], false)

                    // Enable the dropdown
                    acSesi.showDropDown()

                    Log.d("LemburLog", "Setting acSesi to: ${sessionNames[safeIndex]}")
                    Log.d("LemburLog", "All sessions: $sessionNames")
                    Log.d("LemburLog", "Closest session index: $safeIndex")
                }
                val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val tanggalFormatted = dateFormatter.format(lembur?.tanggal)
                view.findViewById<TextInputLayout>(R.id.tanggalInputLayout)?.editText?.setText(tanggalFormatted)
                view.findViewById<TextInputLayout>(R.id.namaInputLayout).editText?.setText(lembur?.nama_pekerja)
                // Automatically select the closest session
                if (sessions.isNotEmpty()) {
                    acSesi.setText(sessions[closestSessionIndex].first, false)
                    updateSessionDetails(sessions[closestSessionIndex])
                    val optionLayout = view?.findViewById<LinearLayout>(R.id.option)
                    if (closestSessionIndex == sessions.size - 1) {
                        optionLayout?.visibility = View.VISIBLE
                        islast = true
                    } else {
                        optionLayout?.visibility = View.GONE
                        islast = false
                    }
                }

                // Update session details when a session is selected from the dropdown
                acSesi.setOnItemClickListener { _, _, position, _ ->
                    val selectedSession = sessions[position]
                    val optionLayout = view?.findViewById<LinearLayout>(R.id.option)
                    if (position == sessions.size - 1) {
                        optionLayout?.visibility = View.VISIBLE
                        islast = true
                    } else {
                        optionLayout?.visibility = View.GONE
                        islast = false
                    }
                    updateSessionDetails(selectedSession)
                }
            } else if(category == "session_admin") {
                val textview = view.findViewById<TextView>(R.id.not_found)
                val scrollView = view.findViewById<ScrollView>(R.id.scrollview)
                if(sessionList == null){
                    textview?.visibility = View.VISIBLE
                    scrollView?.visibility = View.GONE
                }else{
                    textview?.visibility = View.GONE
                    scrollView?.visibility = View.VISIBLE
                    val acSesi = view.findViewById<AutoCompleteTextView>(R.id.ACsesi)
                    Log.d("ApiResponse",sessionList.toString())
                    val (sessions, closestSessionIndex) = calculateSessionsAdmin(lembur!!.waktu_masuk, lembur!!.waktu_pulang,sessionList)
                    if (acSesi != null && sessions.isNotEmpty()) {
                        // Extract session names for the dropdown
                        val sessionNames = sessions.map { it.first }
                        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, sessionNames)
                        acSesi.setAdapter(adapter)

                        // Ensure closestSessionIndex is within bounds
                        val safeIndex = closestSessionIndex.coerceIn(0, sessions.size - 1)

                        // Set the text to the closest session
                        acSesi.setText(sessionNames[safeIndex], false)

                        // Enable the dropdown
                        acSesi.showDropDown()

                        Log.d("LemburLog", "Setting acSesi to: ${sessionNames[safeIndex]}")
                        Log.d("LemburLog", "All sessions: $sessionNames")
                        Log.d("LemburLog", "Closest session index: $safeIndex")
                    }
                    val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val tanggalFormatted = dateFormatter.format(lembur?.tanggal)
                    view.findViewById<TextInputLayout>(R.id.tanggalInputLayout)?.editText?.setText(tanggalFormatted)
                    view.findViewById<TextInputLayout>(R.id.namaInputLayout).editText?.setText(lembur?.nama_pekerja)
                    // Update session details when a session is selected from the dropdown
                    if (sessions.isNotEmpty()) {
                        acSesi.setText(sessions[closestSessionIndex].first, false)
                        updateSessionAdminDetails(sessions[closestSessionIndex])
                        if (closestSessionIndex == sessions.size - 1) {
                            islast = true
                        } else {
                            islast = false
                        }
                    }
                    acSesi.setOnItemClickListener { _, _, position, _ ->
                        val selectedSession = sessions[position]
                        val optionLayout = view?.findViewById<LinearLayout>(R.id.option)
                        if (position == sessions.size - 1) {
                            islast = true
                        } else {
                            islast = false
                        }
                        updateSessionAdminDetails(selectedSession)
                    }
                    view.findViewById<CircularProgressButton>(R.id.acceptButton).setOnClickListener {
                        view.findViewById<CircularProgressButton>(R.id.acceptButton).startAnimation()
                        updateStatus("Accept", "Sesi")
                        if(islast == true){
                            updateStatus("Finished", "Lembur")
                        }
                    }

                    view.findViewById<CircularProgressButton>(R.id.rejectButton).setOnClickListener {
                        view.findViewById<CircularProgressButton>(R.id.acceptButton).startAnimation()
                        updateStatus("Reject", "Sesi")
                        if(islast == true){
                            updateStatus("Finished", "Lembur")
                        }
                    }
                }
            }else{
                val namaInputLayout = view.findViewById<TextInputLayout>(R.id.namaInputLayout)
                namaInputLayout.isEnabled = true
                namaInputLayout.editText?.setText(lembur?.nama_pekerja)
                val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val endIconDrawable: Drawable? = ContextCompat.getDrawable(requireContext(), R.drawable.baseline_calendar_month_24)
                endIconDrawable?.setBounds(0, 0, endIconDrawable.intrinsicWidth, endIconDrawable.intrinsicHeight)
                val tanggalFormatted = dateFormatter.format(lembur?.tanggal)
                val tanggal = view.findViewById<TextInputLayout>(R.id.tanggalInputLayout)
                tanggal.editText?.setText(tanggalFormatted)
                tanggal.endIconMode = TextInputLayout.END_ICON_CUSTOM
                tanggal.endIconDrawable = endIconDrawable
                val TiMasuk = view.findViewById<TextInputLayout>(R.id.masukInputLayout)
                val TiPulang = view.findViewById<TextInputLayout>(R.id.keluarInputLayout)
                val endIconDrawable2: Drawable? = ContextCompat.getDrawable(requireContext(), R.drawable.baseline_access_time_24)
                TiMasuk.endIconMode = TextInputLayout.END_ICON_CUSTOM
                TiPulang.endIconMode = TextInputLayout.END_ICON_CUSTOM
                TiMasuk.endIconDrawable = endIconDrawable2
                TiPulang.endIconDrawable = endIconDrawable2
                tanggal.endIconDrawable = endIconDrawable
                TiMasuk.editText?.setText(lembur?.waktu_masuk.toString())
                TiPulang.editText?.setText(lembur?.waktu_pulang.toString())
                tanggal.setEndIconOnClickListener{
                    tanggal.editText?.let { it1 -> showDatePickerDialog(it1) }
                }
                TiMasuk.setEndIconOnClickListener{
                    perusahaan?.let { it1 -> showTimePickerDialog(TiMasuk, it1) }
                }
                val kegiatanInputLayout = view.findViewById<TextInputLayout>(R.id.kegiatanInputLayout)
                kegiatanInputLayout.editText?.inputType = InputType.TYPE_TEXT_FLAG_MULTI_LINE
                kegiatanInputLayout.editText?.isFocusable = true
                kegiatanInputLayout.editText?.isFocusableInTouchMode = true
                kegiatanInputLayout.editText?.setText(lembur?.pekerjaan)
                val imageView = view.findViewById<ImageView>(R.id.imageView)
                imageView.visibility = View.VISIBLE
                val url =
                    "http://192.168.1.5:8000/api/Lembur/decryptBukti/${lembur?.id}" // Replace with your actual URL
                val imageRequest = ImageRequest(
                    url,
                    { response ->
                        // Set the Bitmap to an ImageView or handle it as needed
                        imageView.setImageBitmap(response)
                    },
                    0, 0, ImageView.ScaleType.CENTER_CROP, Bitmap.Config.RGB_565,
                    { error ->
                        error.printStackTrace()
                        Toast.makeText(requireContext(), "Failed to fetch bukti image", Toast.LENGTH_SHORT).show()
                    }
                )
                val requestQueue = Volley.newRequestQueue(requireContext())
                requestQueue.add(imageRequest)
                val acceptButton = view.findViewById<CircularProgressButton>(R.id.acceptButton)
                acceptButton.setText("Save")
                val cancelButton = view.findViewById<CircularProgressButton>(R.id.rejectButton)
                cancelButton.setText("Cancel")
                val fileName = url.substringAfterLast("/")
                view.findViewById<TextView>(R.id.text).visibility = View.VISIBLE
                view.findViewById<LinearLayout>(R.id.layout).visibility = View.VISIBLE
                view.findViewById<TextView>(R.id.selectedFileName).setText(fileName)
                view.findViewById<Button>(R.id.changeFile).setOnClickListener{
                    pickImage()
                }
                cancelButton.setOnClickListener{
                    dismiss()
                }
                acceptButton.setOnClickListener{
                    view.findViewById<CircularProgressButton>(R.id.acceptButton).startAnimation()
//                    setLoading(true)
                    lembur!!.id?.let { it1 -> updateDataLembur(it1, tanggal,TiMasuk,TiPulang,kegiatanInputLayout) }
                }
            }
        }else if (izin != null){
            if(category == "Respond"){
                view.findViewById<TextInputLayout>(R.id.namaInputLayout).editText?.setText(izin?.nama_pekerja)
                view.findViewById<TextInputLayout>(R.id.tanggalInputLayout).editText?.setText(izin?.tanggal.toString())
                view.findViewById<TextInputLayout>(R.id.kategoriInputLayout).editText?.setText(izin?.kategori)
                view.findViewById<TextInputLayout>(R.id.kegiatanInputLayout).editText?.setText(izin?.alasan)
                val attachmentUrl = "http://192.168.1.5:8000/storage/${izin?.bukti}"
                val isPdf = attachmentUrl.endsWith(".pdf")
                if (isPdf) {
                    val pdfView = view.findViewById<PDFView>(R.id.pdfView)
                    pdfView.visibility = View.VISIBLE
                    val retrievePdfTask = RetrievePDFfromUrl(pdfView)
                    retrievePdfTask.execute(attachmentUrl)
                } else {
                    // Load image using Glide or Picasso
                    val imageView = view.findViewById<ImageView>(R.id.imageView)
                    imageView.visibility = View.VISIBLE
                    Glide.with(requireContext())
                        .load(attachmentUrl)
                        .into(imageView)
                }
                view.findViewById<CircularProgressButton>(R.id.acceptButton).setOnClickListener {
                    view.findViewById<CircularProgressButton>(R.id.acceptButton).startAnimation()
                    updateStatus("Accept","Izin")
                }

                view.findViewById<CircularProgressButton>(R.id.rejectButton).setOnClickListener {
                    view.findViewById<CircularProgressButton>(R.id.acceptButton).startAnimation()
                    updateStatus("Reject","Izin")
                }
            }else{
                val izinKerjaOptions: List<String> =
                    mutableListOf("Sakit", "Cuti", "Izin Khusus", "Pendidikan", "Liburan", "Keperluan Pribadi", "Kegiatan Keluarga", "Ibadah");
                val acIzin = view.findViewById<AutoCompleteTextView>(R.id.acizin)
                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, izinKerjaOptions)
                acIzin.setAdapter(adapter)
                val nama = view.findViewById<TextInputLayout>(R.id.namaInputLayout)
                val tanggal = view.findViewById<TextInputLayout>(R.id.tanggalInputLayout)
                val kegiatan = view.findViewById<TextInputLayout>(R.id.kegiatanInputLayout)
                nama.editText?.setText(izin?.nama_pekerja)
                val dateString = izin?.tanggal
                if (dateString != null) {
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val formattedDate = dateFormat.format(dateString.time)
                    tanggal.editText?.setText(formattedDate)
                }
                val kategori = izin?.kategori
                if (izinKerjaOptions.contains(kategori)) {
                    val position = izinKerjaOptions.indexOf(kategori)
                    acIzin.setText(izinKerjaOptions[position], false)
                }
                val endIconDrawable: Drawable? = ContextCompat.getDrawable(requireContext(), R.drawable.baseline_calendar_month_24)
                endIconDrawable?.setBounds(0, 0, endIconDrawable.intrinsicWidth, endIconDrawable.intrinsicHeight)
                tanggal.endIconMode = TextInputLayout.END_ICON_CUSTOM
                tanggal.endIconDrawable = endIconDrawable
                tanggal.setEndIconOnClickListener{
                    tanggal.editText?.let { it1 -> showDatePickerDialog(it1) }
                }
                kegiatan.editText?.setText(izin?.alasan)
                kegiatan.editText?.inputType = InputType.TYPE_TEXT_FLAG_MULTI_LINE
                kegiatan.editText?.isFocusable = true
                kegiatan.editText?.isFocusableInTouchMode = true

                val attachmentUrl = "http://192.168.1.5:8000/storage/${izin?.bukti}"
                val isPdf = attachmentUrl.endsWith(".pdf")
                if (isPdf) {
                    // Create an instance of RetrievePDFfromUrl passing the PDFView
                    val pdfView = view.findViewById<PDFView>(R.id.pdfView)
                    pdfView.visibility = View.VISIBLE
                    val retrievePdfTask = RetrievePDFfromUrl(pdfView)

                    // Execute the task with the attachment URL
                    retrievePdfTask.execute(attachmentUrl)
                } else {
                    // Load image using Glide or Picasso
                    val imageView = view.findViewById<ImageView>(R.id.imageView)
                    imageView.visibility = View.VISIBLE
                    Glide.with(requireContext())
                        .load(attachmentUrl)
                        .into(imageView)
                }
                val fileName = attachmentUrl.substringAfterLast("/")
                val acceptButton = view.findViewById<CircularProgressButton>(R.id.acceptButton)
                acceptButton.setText("Save")
                val cancelButton = view.findViewById<CircularProgressButton>(R.id.rejectButton)
                cancelButton.setText("Cancel")
                view.findViewById<TextView>(R.id.text).visibility = View.VISIBLE
                view.findViewById<LinearLayout>(R.id.layout).visibility = View.VISIBLE
                view.findViewById<TextView>(R.id.selectedFileName).setText(fileName)
                view.findViewById<Button>(R.id.changeFile).setOnClickListener{
                    pickFile()
                }
                cancelButton.setOnClickListener{
                    dismiss()
                }
                acceptButton.setOnClickListener{
                    view.findViewById<CircularProgressButton>(R.id.acceptButton).startAnimation()
//                    setLoading(true)
                    izin!!.id?.let { it1 -> updateDataIzin(it1, tanggal,acIzin,kegiatan) }
                }
            }
        }else{
            Toast.makeText(requireContext(),"Failed Open The Dialog",Toast.LENGTH_LONG).show()
        }
    }
    private fun showTimePickerDialog(textInputLayout: TextInputLayout,perusahaan: Perusahaan) {
        val calendar = Calendar.getInstance()
        val masuk = perusahaan.jam_masuk.toString()
        val keluar = perusahaan.jam_keluar.toString()
        val masukParts = masuk.split(":")
        val keluarParts = keluar.split(":")
        val masukHour = masukParts[0].toInt()
        val masukMinute = masukParts[1].toInt()
        if(textInputLayout.id == R.id.masukInputLayout){
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
    private fun updateDataDinas(
        dinasId: Int,
        TIBerangkat: TextInputLayout,
        TIPulang: TextInputLayout,
        acTujuan: AutoCompleteTextView,
        TIkegiatan: TextInputLayout
    ) {
        val url = "http://192.168.1.5:8000/api/"

        val retrofit = Retrofit.Builder()
            .baseUrl(url)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val apiService = retrofit.create(ApiService::class.java)

        // Log input values
        val berangkat = createPartFromString(TIBerangkat.editText?.text.toString())
        val pulang = createPartFromString(TIPulang.editText?.text.toString())
        val tujuan = createPartFromString(acTujuan.text.toString())
        val kegiatan = createPartFromString(TIkegiatan.editText?.text.toString())

        Log.d("UpdateDinas", "berangkat: ${berangkat}, pulang: ${pulang}, tujuan: ${tujuan}, kegiatan: ${kegiatan}")

        // Log file upload status
        val buktiFile = selectedFile
        val buktiPart = if (buktiFile != null) {
            val requestFile = RequestBody.create(MediaType.parse("pdf/*"), buktiFile)
            MultipartBody.Part.createFormData("bukti", buktiFile.name, requestFile)
        } else {
            Log.d("UpdateDinas", "No file selected")
            null
        }

        // Log Retrofit call details
        Log.d("UpdateDinas", "Calling updateDinas API with dinasId: $dinasId")
        val call = apiService.updateDinas(dinasId, berangkat, pulang, tujuan, kegiatan, buktiPart)
        call.enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                if (response.isSuccessful) {
                    val vectorDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.done_bitmap)
                    val bitmap = vectorDrawable?.let { vectorToBitmap(it) }
                    view?.findViewById<CircularProgressButton>(R.id.acceptButton)?.doneLoadingAnimation(Color.parseColor("#AAFF00"), bitmap)
                    val apiResponse = response.body()
                    Log.d("ApiResponse", "Status: ${apiResponse?.status}, Message: ${apiResponse?.message}")
                    activity?.let { motionToastActivity ->
                        MotionToast.createToast(
                            motionToastActivity,
                            "Update Izin Success",
                            "Data Izin Berhasil Diperbarui",
                            MotionToastStyle.SUCCESS,
                            MotionToast.GRAVITY_BOTTOM,
                            MotionToast.LONG_DURATION,
                            ResourcesCompat.getFont(requireContext(), R.font.ralewaybold)
                        )
                    }
                } else {
                    // Log the error details from the response
                    Log.e("ApiResponse", "Error: ${response.message()} | Code: ${response.code()} | Body: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                Log.e("ApiResponse", "Request failed: ${t.message}")
            }
        })

//        setLoading(false)
        dismiss()
    }

    private fun updateDataLembur(lemburId: Int,TiTanggal : TextInputLayout,TIMasuk : TextInputLayout,TiKeluar:TextInputLayout,TIkegiatan:TextInputLayout) {
        val url = "http://192.168.1.5:8000/api/"

        val retrofit = Retrofit.Builder()
            .baseUrl(url)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val apiService = retrofit.create(ApiService::class.java)
        val tanggal = createPartFromString(TiTanggal.editText?.text.toString())
        val masuk = createPartFromString(TIMasuk.editText?.text.toString())
        val pulang = createPartFromString(TiKeluar.editText?.text.toString())
        val pekerjaan = createPartFromString(TIkegiatan.editText?.text.toString())
        val buktiFile = selectedFile
        val bukti = if (buktiFile != null) {
            val requestFile = RequestBody.create(MediaType.parse("image/*"), buktiFile)
            MultipartBody.Part.createFormData("bukti", buktiFile.name, requestFile)
        } else {
            null
        }
        Log.d("ApiResponse",bukti.toString())
        val call = apiService.updateLembur(lemburId, tanggal,masuk,pulang,pekerjaan, bukti)
        call.enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    Log.d("ApiResponse", "Status: ${apiResponse?.status}, Message: ${apiResponse?.message}")
                    activity?.let { motionToastActivity ->
                        MotionToast.createToast(
                            motionToastActivity,
                            "Update Izin Success",
                            "Data Izin Berhasil Diperbarui",
                            MotionToastStyle.SUCCESS,
                            MotionToast.GRAVITY_BOTTOM,
                            MotionToast.LONG_DURATION,
                            ResourcesCompat.getFont(requireContext(), R.font.ralewaybold)
                        )
                    }
                } else {
                    Log.e("ApiResponse", "Error: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                Log.e("ApiResponse", "Request failed: ${t.message}")
            }
        })
//        setLoading(false)
        dismiss()
    }
    private fun updateDataIzin(izinId: Int,TITanggal:TextInputLayout,acIzin:AutoCompleteTextView,TIAlasan:TextInputLayout) {
        val url = "http://192.168.1.5:8000/api/"

        val retrofit = Retrofit.Builder()
            .baseUrl(url)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val apiService = retrofit.create(ApiService::class.java)
        val tanggal = createPartFromString(TITanggal.editText?.text.toString())
        val kategori = createPartFromString(acIzin.text.toString())
        val alasan = createPartFromString(TIAlasan.editText?.text.toString())
        val buktiFile = selectedFile
        val buktiPart = if (buktiFile != null) {
            val requestFile = RequestBody.create(MediaType.parse("image/*"), buktiFile)
            MultipartBody.Part.createFormData("bukti", buktiFile.name, requestFile)
        } else {
            null
        }
        val call = apiService.updateIzin(izinId, tanggal, kategori, alasan, buktiPart)
        call.enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    Log.d("ApiResponse", "Status: ${apiResponse?.status}, Message: ${apiResponse?.message}")
                    activity?.let { motionToastActivity ->
                        MotionToast.createToast(
                            motionToastActivity,
                            "Update Izin Success",
                            "Data Izin Berhasil Diperbarui",
                            MotionToastStyle.SUCCESS,
                            MotionToast.GRAVITY_BOTTOM,
                            MotionToast.LONG_DURATION,
                            ResourcesCompat.getFont(requireContext(), R.font.ralewaybold)
                        )
                    }
                } else {
                    Log.e("ApiResponse", "Error: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                Log.e("ApiResponse", "Request failed: ${t.message}")
            }
        })
//        setLoading(false)
        dismiss()
    }
    private fun createPartFromString(value: String): RequestBody {
        return RequestBody.create(MediaType.parse("text/plain"), value)
    }
    private fun setLoading(isLoading: Boolean) {
        val loadingLayout = activity?.findViewById<LinearLayout>(R.id.layout_loading)
        if (isLoading) {
            loadingLayout?.visibility = View.VISIBLE
        } else {
            loadingLayout?.visibility = View.INVISIBLE
        }
    }
    private fun pickFile() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*" // Allow all file types
        intent.putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/pdf", "image/*")) // Specify PDF and image MIME types
        startActivityForResult(intent, PICK_PDF_OR_IMAGE_REQUEST_CODE)
    }
    private fun pickImage() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*" // Allow all file types
        intent.putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/*")) // Specify PDF and image MIME types
        startActivityForResult(intent, PICK_PDF_OR_IMAGE_REQUEST_CODE)
    }
    private fun pickPdf() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "application/pdf"
        startActivityForResult(intent, PICK_PDF_OR_IMAGE_REQUEST_CODE)
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_PDF_OR_IMAGE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            data?.data?.let { fileUri ->
                val mimeType = requireContext().contentResolver.getType(fileUri)
                if (mimeType != null) {
                    if (mimeType.startsWith("image/")) {
                        view?.findViewById<ImageView>(R.id.imageView)?.visibility = View.VISIBLE
                        view?.findViewById<PDFView>(R.id.pdfView)?.visibility = View.GONE
                        val displayName = getRealFilePathFromUri(fileUri)
                        if (displayName != null) {
                            val file = File(requireContext().cacheDir, displayName)
                            try {
                                requireContext().contentResolver.openInputStream(fileUri)?.use { inputStream ->
                                    FileOutputStream(file).use { outputStream ->
                                        inputStream.copyTo(outputStream)
                                    }
                                }
                                val selectedFileName = view?.findViewById<TextView>(R.id.selectedFileName)
                                selectedFileName?.text = displayName
                                selectedFileName?.addTextChangedListener(watcher)
                                selectedFile = file
                                view?.findViewById<ImageView>(R.id.imageView)?.let {
                                    Glide.with(this)
                                        .load(fileUri) // Load the image using the URI
                                        .into(it)
                                } // Set it into
                            } catch (e: IOException) {
                                Log.e("MyFragment", "Failed to copy file: ${e.message}")
                            }
                        } else {
                            Log.e("MyFragment", "Failed to get display name from URI")
                        }
                    } else if (mimeType == "application/pdf") {
                        view?.findViewById<ImageView>(R.id.imageView)?.visibility = View.GONE
                        view?.findViewById<PDFView>(R.id.pdfView)?.visibility = View.VISIBLE
                        view?.findViewById<PDFView>(R.id.pdfView)?.fromUri(fileUri)
                            ?.password(null) // If your PDF is password protected, provide the password here
                            ?.defaultPage(0) // Specify which page to display by default
                            ?.enableSwipe(true) // Enable or disable swipe to change pages
                            ?.swipeHorizontal(false) // Set to true to enable horizontal swipe
                            ?.enableDoubletap(true) // Enable double tap to zoom
                            ?.onLoad { /* Called when PDF is loaded */ }
                            ?.onPageChange { page, pageCount -> /* Called when page is changed */ }
                            ?.onPageError { page, t -> /* Called when an error occurs while loading a page */ }
                            ?.scrollHandle(null) // Specify a custom scroll handle if needed
                            ?.enableAntialiasing(true) // Improve rendering a little bit on low-res screens
                            ?.spacing(0) // Add spacing between pages in dp
                            ?.load()
                        val displayName = getRealFilePathFromUri(fileUri)
                        if (displayName != null) {
                            val file = File(requireContext().cacheDir, displayName)
                            try {
                                requireContext().contentResolver.openInputStream(fileUri)?.use { inputStream ->
                                    FileOutputStream(file).use { outputStream ->
                                        inputStream.copyTo(outputStream)
                                    }
                                }
                                val selectedFileName = view?.findViewById<TextView>(R.id.selectedFileName)
                                selectedFileName?.text = displayName
                                selectedFileName?.addTextChangedListener(watcher)
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
    private val watcher = object : TextWatcher {
        override fun beforeTextChanged(charSequence: CharSequence?, start: Int, count: Int, after: Int) {
            // Not needed for this example
        }

        override fun onTextChanged(charSequence: CharSequence?, start: Int, before: Int, count: Int) {
            // Not needed for this example
        }

        override fun afterTextChanged(editable: Editable?) {
            // Update the button state whenever a field is changed
            if (dinas != null) {

            }else if (lembur  != null) {

            }else{
                view?.findViewById<CircularProgressButton>(R.id.acceptButton)?.isEnabled = isAllFieldsIzinFilled()
            }
        }
    }
    private fun isAllFieldsIzinFilled(): Boolean {
        val TINama = view?.findViewById<TextInputLayout>(R.id.namaInputLayout)
        val TITanggal = view?.findViewById<TextInputLayout>(R.id.tanggalInputLayout)
        val acIzin = view?.findViewById<AutoCompleteTextView>(R.id.acizin)
        val TIAlasan = view?.findViewById<TextInputLayout>(R.id.kegiatanInputLayout)
        val selectedFileName = view?.findViewById<TextView>(R.id.selectedFileName)

        return TINama?.editText?.text?.isNotEmpty() ?: false &&
                TITanggal?.editText?.text?.isNotEmpty() ?: false &&
                acIzin?.text?.isNotEmpty() ?: false &&
                TIAlasan?.editText?.text?.isNotEmpty() ?: false &&
                selectedFileName?.text != "No file selected"
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

    private fun updateStatus(status: String,category:String) {
        val id: Int?
        if (category == "Izin") {
            id = izin?.id!!
        } else if (category == "Lembur") {
            id = lembur?.id!!
        } else if(category == "Dinas"){
            id = dinas?.id!!
        }else{
            id = sesilembur!!
        }
        // Assuming you have an ID associated with the item
        // Call your API to update the status
        val url = "http://192.168.1.5:8000/api/"
        // Make a network request using Retrofit
        val retrofit = Retrofit.Builder()
            .baseUrl(url)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val apiService = retrofit.create(ApiService::class.java)
        val call: Call<ApiResponse> // Declare the call variable outside the if-else statement
        if (category == "Izin") {
            call = apiService.updatestatusIzin(id, status)
        } else if (category == "Lembur") {
            call = apiService.updatestatusLembur(id, status)
        } else if (category == "Dinas") {
            call = apiService.updatestatusDinas(id, status)
        } else{
            call = apiService.updatestatusSesi(id,status)
        }
        call.enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                if (response.isSuccessful) {
                    val vectorDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.done_bitmap)
                    val bitmap = vectorDrawable?.let { vectorToBitmap(it) }
                    view?.findViewById<CircularProgressButton>(R.id.acceptButton)?.doneLoadingAnimation(Color.parseColor("#AAFF00"), bitmap)
                    if (category == "Izin") {
                        MotionToast.createToast(
                            requireActivity(),
                            "Update Izin Success",
                            "Silahkan Refresh Halaman",
                            MotionToastStyle.SUCCESS,
                            MotionToast.GRAVITY_BOTTOM,
                            MotionToast.LONG_DURATION,
                            ResourcesCompat.getFont(requireContext(), R.font.ralewaybold)
                        )
                    } else if (category == "Lembur") {
                        MotionToast.createToast(
                            requireActivity(),
                            "Update Lembur Success",
                            "Silahkan Refresh Halaman",
                            MotionToastStyle.SUCCESS,
                            MotionToast.GRAVITY_BOTTOM,
                            MotionToast.LONG_DURATION,
                            ResourcesCompat.getFont(requireContext(), R.font.ralewaybold)
                        )
                    } else {
                        MotionToast.createToast(
                            requireActivity(),
                            "Update Dinas Success",
                            "Silahkan Refresh Halaman",
                            MotionToastStyle.SUCCESS,
                            MotionToast.GRAVITY_BOTTOM,
                            MotionToast.LONG_DURATION,
                            ResourcesCompat.getFont(requireContext(), R.font.ralewaybold)
                        )
                    }
                    dismiss() // Dismiss the dialog after updating the status
                } else {
                    // Handle unsuccessful response
                    val errorMessage = response.errorBody()?.string() ?: "Unknown error"
                    Log.e("UpdateStatusError", "Failed to update status: $errorMessage")
                    Log.e("UpdateStatusError", "Id: $id")
                    view?.findViewById<CircularProgressButton>(R.id.acceptButton)?.revertAnimation()
                    dismiss() // Dismiss the dialog even if the status update fails
                }
            }

            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                view?.findViewById<CircularProgressButton>(R.id.acceptButton)?.revertAnimation()
                Log.e("UpdateStatusError", "Failed to update status: ${t.message}")
                dismiss()
            }
        })
    }
    private fun vectorToBitmap(vectorDrawable: Drawable): Bitmap {
        val bitmap = Bitmap.createBitmap(vectorDrawable.intrinsicWidth,
            vectorDrawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        vectorDrawable.setBounds(0, 0, canvas.width, canvas.height)
        vectorDrawable.draw(canvas)
        return bitmap
    }
}