package com.windstrom5.tugasakhir.activity

import android.app.AlertDialog
import android.icu.text.SimpleDateFormat
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import com.bumptech.glide.Glide
import com.windstrom5.tugasakhir.R
import com.windstrom5.tugasakhir.databinding.ActivityProfileBinding
import com.windstrom5.tugasakhir.fragment.ScanAbsensiFragment
import com.windstrom5.tugasakhir.fragment.ShowQRCodeFragment
import com.windstrom5.tugasakhir.model.Admin
import com.windstrom5.tugasakhir.model.Pekerja
import com.windstrom5.tugasakhir.model.Perusahaan
import de.hdodenhof.circleimageview.CircleImageView
import java.util.Locale

class ProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProfileBinding
    private lateinit var profile: CircleImageView
    private lateinit var username: TextView
    private lateinit var back: TextView
    private lateinit var nama : TextView
    private lateinit var tanggalLahir: TextView
    private var admin : Admin? = null
    private var pekerja : Pekerja? = null
    private var bundle: Bundle? = null
    private var perusahaan : Perusahaan? = null
    private lateinit var delete:Button
    private lateinit var email : TextView
    private lateinit var promote:Button
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        username = binding.Name
        nama = binding.nama
        back = binding.back
        tanggalLahir = binding.birthday
        email = binding.email
        promote = binding.admin
        profile = binding.circleImageView
        delete = binding.delete
        getBundle()
        promote.setOnClickListener{
            val builder = AlertDialog.Builder(this@ProfileActivity)
            builder.setTitle("Promote Admin")
            builder.setMessage("Are you sure you want to promote this ${pekerja?.nama}?")
            builder.setPositiveButton("Yes") { dialog, which ->
                // Perform action when "Yes" is clicked
                // For example, you can call a function to handle the promotion
//                handlePromotion()
            }

            builder.setNegativeButton("No") { dialog, which ->
                // Perform action when "No" is clicked
                dialog.dismiss() // Dismiss the dialog
            }
            val dialog = builder.create()
            dialog.show()
        }
        delete.setOnClickListener{
            val builder = AlertDialog.Builder(this@ProfileActivity)
            builder.setTitle("Delete User")
            builder.setMessage("Are you sure you want to delete this ${pekerja?.nama}?")
            builder.setPositiveButton("Yes") { dialog, which ->
                // Perform action when "Yes" is clicked
                // For example, you can call a function to handle the promotion
//                handlePromotion()
            }

            builder.setNegativeButton("No") { dialog, which ->
                // Perform action when "No" is clicked
                dialog.dismiss() // Dismiss the dialog
            }
            val dialog = builder.create()
            dialog.show()
        }
    }
    private fun getBundle() {
        bundle = intent?.getBundleExtra("data")
        if (bundle != null) {
            bundle?.let {
                perusahaan = it.getParcelable("perusahaan")
                val role = it.getString("role")
                val jenis = it.getString("jenis")
                if(role == "Admin"){
                    if(jenis == "Pekerja"){
                        pekerja = it.getParcelable("user")
                        if (pekerja?.profile != "null") {
                            val imageUrl =
                                "http://192.168.1.6:8000/storage/${pekerja?.profile}" // Replace with your Laravel image URL
                            Glide.with(this)
                                .load(imageUrl)
                                .into(profile)
                        }else{
                            Glide.with(this)
                                .load(R.drawable.profile)
                                .into(profile)
                        }
                        nama.setText(pekerja?.nama)
                        username.setText(pekerja?.nama)
                        email.setText(pekerja?.email)
                        val dateString = pekerja?.tanggal_lahir
                        val inputDateFormat = SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.ENGLISH)
                        val outputDateFormat = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID"))
                        try {
                            val tanggalLahirDate = inputDateFormat.parse(dateString.toString())
                            val formattedDate = outputDateFormat.format(tanggalLahirDate)
                            tanggalLahir.setText(formattedDate)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        promote.visibility=View.VISIBLE
                        delete.visibility=View.VISIBLE
                    }else{
                        admin = it.getParcelable("user")
                        nama.setText(admin?.nama)
                        username.setText(admin?.nama)
                        email.setText(admin?.email)
                        val dateString = admin?.tanggal_lahir
                        val inputDateFormat = SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.ENGLISH)
                        val outputDateFormat = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID"))
                        try {
                            val tanggalLahirDate = inputDateFormat.parse(dateString.toString())
                            val formattedDate = outputDateFormat.format(tanggalLahirDate)
                            // Use formattedDate as needed
                            println(formattedDate) // Output: "06 Juni 2024"
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        if (admin?.profile != "null") {
                            val imageUrl =
                                "http://192.168.1.6:8000/storage/${admin?.profile}" // Replace with your Laravel image URL
                            Glide.with(this)
                                .load(imageUrl)
                                .into(profile)
                        }else{
                            Glide.with(this)
                                .load(R.drawable.profile)
                                .into(profile)
                        }
                        promote.visibility=View.GONE
                        delete.visibility=View.VISIBLE
                    }
                }else {
                    if(jenis == "Pekerja"){
                        pekerja = it.getParcelable("user")
                        if (pekerja?.profile != "null") {
                            val imageUrl =
                                "http://192.168.1.6:8000/storage/${pekerja?.profile}" // Replace with your Laravel image URL
                            Glide.with(this)
                                .load(imageUrl)
                                .into(profile)
                        }else{
                            Glide.with(this)
                                .load(R.drawable.profile)
                                .into(profile)
                        }
                        nama.setText(pekerja?.nama)
                        username.setText(pekerja?.nama)
                        email.setText(pekerja?.email)
                        val dateString = pekerja?.tanggal_lahir
                        val inputDateFormat = SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.ENGLISH)
                        val outputDateFormat = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID"))
                        try {
                            val tanggalLahirDate = inputDateFormat.parse(dateString.toString())
                            val formattedDate = outputDateFormat.format(tanggalLahirDate)
                            tanggalLahir.setText(formattedDate)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        promote.visibility=View.GONE
                        delete.visibility=View.GONE
                    }else{
                        admin = it.getParcelable("user")
                        nama.setText(admin?.nama)
                        username.setText(admin?.nama)
                        email.setText(admin?.email)
                        val dateString = admin?.tanggal_lahir
                        val inputDateFormat = SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.ENGLISH)
                        val outputDateFormat = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID"))
                        try {
                            val tanggalLahirDate = inputDateFormat.parse(dateString.toString())
                            val formattedDate = outputDateFormat.format(tanggalLahirDate)
                            tanggalLahir.setText(formattedDate)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        if (admin?.profile != "null") {
                            val imageUrl =
                                "http://192.168.1.6:8000/storage/${admin?.profile}" // Replace with your Laravel image URL
                            Glide.with(this)
                                .load(imageUrl)
                                .into(profile)
                        }else{
                            Glide.with(this)
                                .load(R.drawable.profile)
                                .into(profile)
                        }
                        promote.visibility=View.GONE
                        delete.visibility=View.GONE
                    }
                }
            }
        } else {
            Log.d("Error","Bundle Not Found")
        }
    }
}

//PopupDialog.getInstance(this@AdminActivity)
//.statusDialogBuilder()
//.createSuccessDialog()
//.setHeading("Well Done")
//.setDescription("You have successfully completed the task")
//.setActionButtonText("OK")
//.build(Dialog::dismiss)
//.show()