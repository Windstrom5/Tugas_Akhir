package com.windstrom5.tugasakhir.activity

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.windstrom5.tugasakhir.R
import com.windstrom5.tugasakhir.databinding.ActivityCustomerServiceBinding
import com.windstrom5.tugasakhir.feature.DiscordOAuth2
import com.windstrom5.tugasakhir.fragment.AddLemburFragment
import com.windstrom5.tugasakhir.fragment.HistoryLemburFragment
import com.windstrom5.tugasakhir.model.Admin
import com.windstrom5.tugasakhir.model.Pekerja
import com.windstrom5.tugasakhir.model.Perusahaan

class CustomerServiceActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCustomerServiceBinding
    private lateinit var steam : ImageView
    private var bundle: Bundle? = null
    private var admin : Admin? = null
    private lateinit var discordOAuth2: DiscordOAuth2
    private var pekerja : Pekerja? = null
    private var perusahaan : Perusahaan? = null
    private lateinit var discord : ImageView
    private lateinit var github : ImageView
    private val steamlink = "https://steamcommunity.com/profiles/76561198881808539"
    private val githublink = "https://github.com/Windstrom5"
    private val discordlink = "https://discordapp.com/users/411135817449340929"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCustomerServiceBinding.inflate(layoutInflater)
        setContentView(binding.root)
        getBundle()
        steam = binding.steam
        discord = binding.discord
        github = binding.github
        discordOAuth2 = DiscordOAuth2()
        // Start the OAuth2 flow
//        authenticate()
        steam.setOnClickListener {
            openLink(steamlink)
        }

        discord.setOnClickListener {
            openLink(discordlink)
        }

        github.setOnClickListener {
            openLink(githublink)
        }
    }
//    private fun authenticate() {
//        discordOAuth2.authenticate() { user, error ->
//            if (user != null) {
//                val bannerImageView = findViewById<ImageView>(R.id.bannerImageView)
//                val avatarImageView = findViewById<ImageView>(R.id.avatarImageView)
//                val globalNameTextView = findViewById<TextView>(R.id.globalNameTextView)
//                val avatarUrl2 = "https://cdn.discordapp.com/banners/${user.id}/${user.banner}.png?size=4096"
//                discordOAuth2.fetchAvatar(avatarUrl2) { bitmap, error2 ->
//                    if (bitmap != null) {
//                        runOnUiThread {// Set the bitmap to your ImageView or wherever you want to display the avatar
//                            bannerImageView.setImageBitmap(bitmap)
//                        }
//                    } else {
//                        Log.e("AvatarError", "Failed to fetch avatar: $error")
//                    }
//                }
//                val avatarUrl = "https://cdn.discordapp.com/avatars/${user.id}/${user.avatar}.png?size=4096"
//                discordOAuth2.fetchAvatar(avatarUrl) { bitmap, error ->
//                    if (bitmap != null) {
//                        runOnUiThread {// Set the bitmap to your ImageView or wherever you want to display the avatar
//                            avatarImageView.setImageBitmap(bitmap)
//                        }
//                    } else {
//                        Log.e("AvatarError", "Failed to fetch avatar: $error")
//                    }
//                }
                //                val isGif = avatarUrl.endsWith(".gif", ignoreCase = true)
//                if (isGif) {
//                    // Load GIF image using Glide
//                    runOnUiThread {
//                        Glide.with(this)
//                            .asGif()
//                            .load(avatarUrl)
//                            .into(avatarImageView)
//                    }
//                } else {
//                    // Load PNG image using Glide
//                    runOnUiThread {
//                        Glide.with(this)
//                            .load(avatarUrl)
//                            .into(avatarImageView)
//                    }
//                }
//                runOnUiThread {
//                    globalNameTextView.text = user.globalName
//                }
//                // Authentication successful, handle user data
//                Log.d("DiscordUser", "Username: ${user.globalName}, Nick: ${user.avatar}")
//            } else {
//                // Authentication failed, handle error
//                Log.e("DiscordAuth", "Authentication error: $error")
//            }
//        }
//    }
    private fun openLink(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    }
    private fun getBundle() {
        bundle = intent?.getBundleExtra("data")
        if (bundle != null) {
            bundle?.let {
                perusahaan = it.getParcelable("perusahaan")
                val role = it.getString("role")
                Log.d("Role",role.toString())
                if(role.toString() == "Admin"){
                    admin = it.getParcelable("user")
                }else{
                    pekerja = it.getParcelable("user")
                }
            }
        } else {
            Log.d("Error","Bundle Not Found")
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
}