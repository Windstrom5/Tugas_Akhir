package com.windstrom5.tugasakhir.feature

import android.app.Activity
import android.app.Dialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.res.Resources
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
import android.widget.TextView
import android.widget.TimePicker
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.DialogFragment
import br.com.simplepass.loading_button_lib.customViews.CircularProgressButton
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
import com.windstrom5.tugasakhir.model.Dinas
import com.windstrom5.tugasakhir.model.DinasItem
import com.windstrom5.tugasakhir.model.Izin
import com.windstrom5.tugasakhir.model.IzinItem
import com.windstrom5.tugasakhir.model.Lembur
import com.windstrom5.tugasakhir.model.LemburItem
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
    private lateinit var selectedFile: File
    private var lembur: LemburItem? = null
    private var dinas: DinasItem? = null
    private var izin: IzinItem? = null
    private var perusahaan:Perusahaan? = null
    private var category: String? = null
    private var isTIMasukFilled = false
    private var sessionList: List<session_lembur>? = null
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
    private fun calculateSessions(waktuMasuk: Time, waktuPulang: Time): Pair<List<String>, Int> {
        val sessions = mutableListOf<String>()

        // Calculate the time difference in minutes
        val differenceInMillis = waktuPulang.time - waktuMasuk.time
        val differenceInMinutes = differenceInMillis / (1000 * 60)

        var closestSessionIndex = 0

        if (differenceInMinutes > 30) {
            val hours = differenceInMinutes / 60
            val currentTime = Time(System.currentTimeMillis())
            var closestTime = waktuMasuk

            for (i in 1..hours) {
                sessions.add("Sesi $i")

                // Update closest session if current time falls in this session
                val sessionStartTime = Time(closestTime.time)
                closestTime = Time(closestTime.time + 3600000) // Add 1 hour to the start time

                if (currentTime.before(closestTime) && currentTime.after(sessionStartTime)) {
                    closestSessionIndex = (i - 1).toInt() // Zero-based index
                }
            }
        }

        return Pair(sessions, closestSessionIndex)
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
                val option = view.findViewById<LinearLayout>(R.id.option)
                option.visibility= View.GONE
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

                val pdfUrl = "http://192.168.1.6:8000/storage/${dinas?.bukti}"
                val pdfView = view.findViewById<PDFView>(R.id.pdfView)
                pdfView.visibility = View.VISIBLE
                val retrievePdfTask = RetrievePDFfromUrl(pdfView)
                retrievePdfTask.execute(pdfUrl)
                view.findViewById<Button>(R.id.acceptButton).setOnClickListener {
                    updateStatus("Accept","Dinas")
                }

                view.findViewById<Button>(R.id.rejectButton).setOnClickListener {
                    updateStatus("Reject","Dinas")
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
                val option = view.findViewById<LinearLayout>(R.id.option)
                option.visibility= View.VISIBLE
                val delete = view.findViewById<CircularProgressButton>(R.id.deleteButton)
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
                val pdfUrl = "http://192.168.1.6:8000/storage/${dinas?.bukti}"
                val pdfView = view.findViewById<PDFView>(R.id.pdfView)
                pdfView.visibility = View.VISIBLE
                val retrievePdfTask = RetrievePDFfromUrl(pdfView)
                retrievePdfTask.execute(pdfUrl)
                val acceptButton = view.findViewById<Button>(R.id.acceptButton)
                acceptButton.setText("Save")
                val cancelButton = view.findViewById<Button>(R.id.rejectButton)
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
                    setLoading(true)
                    dinas!!.id?.let { it1 -> updateDataDinas(it1, berangkatInputLayout,pulangInputLayout,tujuanInput,kegiatanInputLayout) }
                }
            }

        } else if (lembur != null) {
            perusahaan = arguments?.getParcelable("perusahaan")
            if(category == "Respond") {
                view.findViewById<TextInputLayout>(R.id.namaInputLayout).editText?.setText(lembur?.nama_pekerja)
                view.findViewById<TextInputLayout>(R.id.tanggalInputLayout).editText?.setText(lembur?.tanggal.toString())
                view.findViewById<TextInputLayout>(R.id.masukInputLayout).editText?.setText(lembur?.waktu_masuk.toString())
                view.findViewById<TextInputLayout>(R.id.pulangInputLayout).editText?.setText(lembur?.waktu_pulang.toString())
                view.findViewById<TextInputLayout>(R.id.kegiatanEditText).editText?.setText(lembur?.pekerjaan)
                val attachmentUrl = "http://192.168.1.6:8000/storage/${lembur?.bukti}"
                val imageView = view.findViewById<ImageView>(R.id.imageView)
                imageView.visibility = View.VISIBLE
                Glide.with(requireContext())
                    .load(attachmentUrl)
                    .into(imageView)
                view.findViewById<Button>(R.id.acceptButton).setOnClickListener {
                    updateStatus("Accept", "Lembur")
                }

                view.findViewById<Button>(R.id.rejectButton).setOnClickListener {
                    updateStatus("Reject", "Lembur")
                }
            }else if(category == "Edit"){

            }else if(category == "session_pekerja"){
                val (sessions, closestSessionIndex) = calculateSessions(lembur!!.waktu_masuk, lembur!!.waktu_pulang)

                val adapter = ArrayAdapter(view.context, android.R.layout.simple_dropdown_item_1line, sessions)
                val acSesi = view.findViewById<AutoCompleteTextView>(R.id.ACsesi)
                acSesi.setAdapter(adapter)

                // Automatically select the closest session
                if (sessions.isNotEmpty()) {
                    acSesi.setText(sessions[closestSessionIndex], false)
                }

                // Set the initial values for the TextViews
                view.findViewById<TextInputLayout>(R.id.namaInputLayout).editText?.setText(lembur?.nama_pekerja)
                view.findViewById<TextInputLayout>(R.id.tanggalInputLayout).editText?.setText(lembur?.tanggal.toString())

                // Function to update session details based on selected session
                fun updateSessionDetails(selectedSession: String) {
                    val selectedSessionIndex = sessions.indexOf(selectedSession)
                    if (selectedSessionIndex != -1 && sessionList != null) {
                        val correspondingSession = sessionList?.get(selectedSessionIndex)
                        view.findViewById<TextInputLayout>(R.id.keteranganInputLayout).editText?.setText(correspondingSession?.keterangan)
                    }
                }

                // Update session details when a session is selected from the dropdown
                acSesi.setOnItemClickListener { _, _, position, _ ->
                    updateSessionDetails(sessions[position])
                }

                // Update session details based on initially selected session
                if (sessions.isNotEmpty()) {
                    updateSessionDetails(sessions[closestSessionIndex])
                }
            } else if(category == "session_admin") {
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
                val url = "http://192.168.1.6:8000/storage/${lembur?.bukti}"
                val imageView = view.findViewById<ImageView>(R.id.imageView)
                imageView.visibility = View.VISIBLE
                Glide.with(requireContext())
                    .load(url)
                    .into(imageView)
                val acceptButton = view.findViewById<Button>(R.id.acceptButton)
                acceptButton.setText("Save")
                val cancelButton = view.findViewById<Button>(R.id.rejectButton)
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
                    setLoading(true)
                    lembur!!.id?.let { it1 -> updateDataLembur(it1, tanggal,TiMasuk,TiPulang,kegiatanInputLayout) }
                }
            }
        }else if (izin != null){
            if(category == "Respond"){
                view.findViewById<TextInputLayout>(R.id.namaInputLayout).editText?.setText(izin?.nama_pekerja)
                view.findViewById<TextInputLayout>(R.id.tanggalInputLayout).editText?.setText(izin?.tanggal.toString())
                view.findViewById<TextInputLayout>(R.id.kategoriInputLayout).editText?.setText(izin?.kategori)
                view.findViewById<TextInputLayout>(R.id.kegiatanInputLayout).editText?.setText(izin?.alasan)
                val attachmentUrl = "http://192.168.1.6:8000/storage/${izin?.bukti}"
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
                view.findViewById<Button>(R.id.acceptButton).setOnClickListener {
                    updateStatus("Accept","Izin")
                }

                view.findViewById<Button>(R.id.rejectButton).setOnClickListener {
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

                val attachmentUrl = "http://192.168.1.6:8000/storage/${izin?.bukti}"
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
                val acceptButton = view.findViewById<Button>(R.id.acceptButton)
                acceptButton.setText("Save")
                val cancelButton = view.findViewById<Button>(R.id.rejectButton)
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
                    setLoading(true)
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
    private fun updateDataDinas(dinasId: Int,TIBerangkat : TextInputLayout,TIPulang : TextInputLayout,acTujuan:AutoCompleteTextView,TIkegiatan:TextInputLayout) {
        val url = "http://192.168.1.6:8000/api/"

        val retrofit = Retrofit.Builder()
            .baseUrl(url)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val apiService = retrofit.create(ApiService::class.java)
        val berangkat = createPartFromString(TIBerangkat.editText?.text.toString())
        val pulang = createPartFromString(TIPulang.editText?.text.toString())
        val tujuan = createPartFromString(acTujuan.text.toString())
        val kegiatan = createPartFromString(TIkegiatan.editText?.text.toString())
        val buktiFile = selectedFile
        val requestFile = RequestBody.create(MediaType.parse("pdf/*"), buktiFile)
        val buktiPart = MultipartBody.Part.createFormData("bukti", buktiFile.name, requestFile)
        val call = apiService.updateDinas(dinasId, berangkat,pulang,tujuan,kegiatan, buktiPart)
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
                    Log.e("ApiResponse", "Error: ${response.message()}")
                }
            }

            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                Log.e("ApiResponse", "Request failed: ${t.message}")
            }
        })
        setLoading(false)
        dismiss()
    }
    private fun updateDataLembur(lemburId: Int,TiTanggal : TextInputLayout,TIMasuk : TextInputLayout,TiKeluar:TextInputLayout,TIkegiatan:TextInputLayout) {
        val url = "http://192.168.1.6:8000/api/"

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
        val requestFile = RequestBody.create(MediaType.parse("pdf/*"), buktiFile)
        val buktiPart = MultipartBody.Part.createFormData("bukti", buktiFile.name, requestFile)
        val call = apiService.updateDinas(lemburId, tanggal,masuk,pulang,pekerjaan, buktiPart)
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
        setLoading(false)
        dismiss()
    }
    private fun updateDataIzin(izinId: Int,TITanggal:TextInputLayout,acIzin:AutoCompleteTextView,TIAlasan:TextInputLayout) {
        val url = "http://192.168.1.6:8000/api/"

        val retrofit = Retrofit.Builder()
            .baseUrl(url)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val apiService = retrofit.create(ApiService::class.java)
        val tanggal = createPartFromString(TITanggal.editText?.text.toString())
        val kategori = createPartFromString(acIzin.text.toString())
        val alasan = createPartFromString(TIAlasan.editText?.text.toString())
        val buktiFile = selectedFile
        val requestFile = RequestBody.create(MediaType.parse("pdf/*"), buktiFile)
        val buktiPart = MultipartBody.Part.createFormData("bukti", buktiFile.name, requestFile)
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
        setLoading(false)
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
                view?.findViewById<Button>(R.id.acceptButton)?.isEnabled = isAllFieldsIzinFilled()
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
        } else {
            id = dinas?.id!!
        }// Assuming you have an ID associated with the item
        // Call your API to update the status
        val url = "http://192.168.1.6:8000/api/"
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
        } else {
            call = apiService.updatestatusDinas(id, status)
        }
        call.enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                if (response.isSuccessful) {
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

                    dismiss() // Dismiss the dialog even if the status update fails
                }
            }

            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                // Handle network failures
                Log.e("UpdateStatusError", "Failed to update status: ${t.message}")
                dismiss()
            }
        })
    }
}