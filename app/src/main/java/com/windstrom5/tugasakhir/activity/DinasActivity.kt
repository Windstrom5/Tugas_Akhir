package com.windstrom5.tugasakhir.activity

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.windstrom5.tugasakhir.R
import com.windstrom5.tugasakhir.databinding.ActivityDinasBinding
import com.windstrom5.tugasakhir.fragment.AddDinasFragment
import com.windstrom5.tugasakhir.fragment.HistoryAbsenFragment
import com.windstrom5.tugasakhir.fragment.HistoryDinasFragment
import com.windstrom5.tugasakhir.fragment.ScanAbsensiFragment
import com.windstrom5.tugasakhir.fragment.ShowQRCodeFragment
import com.windstrom5.tugasakhir.fragment.TrackingFragment
import com.windstrom5.tugasakhir.model.Admin
import com.windstrom5.tugasakhir.model.Pekerja
import com.windstrom5.tugasakhir.model.Perusahaan

class DinasActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDinasBinding
    private var bundle: Bundle? = null
    private var perusahaan : Perusahaan? = null
    private var admin : Admin? = null
    private var pekerja : Pekerja? = null
    private lateinit var fragment : FragmentContainerView
    private var isFirstLaunch = true
    private lateinit var navigation : BottomNavigationView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDinasBinding.inflate(layoutInflater)
        setContentView(binding.root)
        fragment = binding.content
        navigation = binding.navigation
        getBundle()
        navigation.setOnNavigationItemSelectedListener { menuItem ->
            if (!isFirstLaunch) {
                when (menuItem.itemId) {
                    R.id.historydinas -> {
                        replaceFragment(HistoryDinasFragment())
                        true
                    }
                    R.id.adddinas -> {
                        replaceFragment(AddDinasFragment())
                        true
                    }
                    else -> false
                }
            } else {
                // It's the first launch, do nothing or perform any setup needed
                isFirstLaunch = false
                true
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
    private fun replaceFragment(fragment: Fragment) {
        val transaction = supportFragmentManager.beginTransaction()
        val args = Bundle()

        if (admin != null) {
            args.putParcelable("user", admin)
            args.putString("role","Admin")
        } else if (pekerja != null) {
            args.putParcelable("user", pekerja)
            args.putString("role","Pekerja")
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
                if(role.toString() == "Admin"){
                    admin = it.getParcelable("user")
                    replaceFragment(HistoryDinasFragment())
                    navigation.visibility = View.GONE
                }else{
                    pekerja = it.getParcelable("user")
                    navigation.visibility = View.VISIBLE
                    navigation.inflateMenu(R.menu.dinasuser)
                    if (isFirstLaunch) {
                        replaceFragment(AddDinasFragment())
                        isFirstLaunch = false
                    }
                }
            }
        } else {
            Log.d("Error","Bundle Not Found")
        }
    }
}