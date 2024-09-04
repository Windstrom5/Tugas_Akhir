package com.windstrom5.tugasakhir.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.window.OnBackInvokedDispatcher
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.windstrom5.tugasakhir.R
import com.windstrom5.tugasakhir.databinding.ActivityAbsensiBinding
import com.windstrom5.tugasakhir.fragment.HistoryAbsenFragment
import com.windstrom5.tugasakhir.fragment.ScanAbsensiFragment
import com.windstrom5.tugasakhir.fragment.ShowQRCodeFragment
import com.windstrom5.tugasakhir.fragment.TrackingFragment
import com.windstrom5.tugasakhir.model.Admin
import com.windstrom5.tugasakhir.model.Pekerja
import com.windstrom5.tugasakhir.model.Perusahaan

class AbsensiActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAbsensiBinding
    private var bundle: Bundle? = null
    private var perusahaan : Perusahaan? = null
    private var admin : Admin? = null
    private var pekerja : Pekerja? = null
    private lateinit var fragment : FragmentContainerView
    private var isFirstLaunch = true
    private lateinit var navigation : BottomNavigationView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAbsensiBinding.inflate(layoutInflater)
        setContentView(binding.root)
        fragment = binding.content
        navigation = binding.navigation
        getBundle()
        navigation.setOnNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.qrabsen -> {
                    replaceFragment(ShowQRCodeFragment())
                    true
                }
                R.id.trackPegawai -> {
                    replaceFragment(TrackingFragment())
                    true
                }
                R.id.absenuser -> {
                    replaceFragment(ScanAbsensiFragment())
                    true
                }
                R.id.historyabsen -> {
                    replaceFragment(HistoryAbsenFragment())
                    true
                }
                else -> false
            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        if(pekerja != null){
            val userBundle = Bundle()
            val intent = Intent(this, UserActivity::class.java)
            userBundle.putParcelable("user", pekerja)
            userBundle.putParcelable("perusahaan", perusahaan)
            intent.putExtra("data", userBundle)
            startActivity(intent)
        }else{
            val userBundle = Bundle()
            val intent = Intent(this, AdminActivity::class.java)
            userBundle.putParcelable("user", admin)
            userBundle.putParcelable("perusahaan", perusahaan)
            intent.putExtra("data", userBundle)
            startActivity(intent)
        }
    }
    override fun onDestroy() {
        if(admin != null){
            TrackingFragment().stopFetchRunnable()
        }
        super.onDestroy()
    }
    private fun replaceFragment(fragment: Fragment) {
        val transaction = supportFragmentManager.beginTransaction()
        val args = Bundle()
        Log.d("pekerja",pekerja.toString())
        if (admin != null) {
            args.putParcelable("user", admin)
        } else if (pekerja != null) {
            Log.d("pekerja",pekerja.toString())
            args.putParcelable("user", pekerja)
        }
        args.putParcelable("perusahaan",perusahaan)
        fragment.arguments = args
        transaction.replace(R.id.content, fragment)
        transaction.addToBackStack(null)
        transaction.commit()
    }
    private fun getBundle() {
        bundle = intent?.getBundleExtra("data")
        if (bundle != null) {
            bundle?.let {
                perusahaan = it.getParcelable("perusahaan")
                val role = it.getString("role")
                if(role == "Admin"){
                    admin = it.getParcelable("user")
                    navigation.inflateMenu(R.menu.absenadmin)
                    if (isFirstLaunch) {
                        replaceFragment(ShowQRCodeFragment())
                        isFirstLaunch = false
                    }
                }else{
                    pekerja = it.getParcelable("user")
                    navigation.inflateMenu(R.menu.absenuser)
                    if (isFirstLaunch) {
                        replaceFragment(ScanAbsensiFragment())
                        isFirstLaunch = false
                    }
                }
            }
        } else {
            Log.d("Error","Bundle Not Found")
        }
    }
}