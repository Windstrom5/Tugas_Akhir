package com.windstrom5.tugasakhir.adapter

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView.OnItemClickListener
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.toolbox.ImageRequest
import com.android.volley.toolbox.Volley
import com.bumptech.glide.Glide
import com.saadahmedev.popupdialog.PopupDialog
import com.windstrom5.tugasakhir.R
import com.windstrom5.tugasakhir.activity.ProfileActivity
import com.windstrom5.tugasakhir.model.Admin
import com.windstrom5.tugasakhir.model.Pekerja
import de.hdodenhof.circleimageview.CircleImageView
import com.windstrom5.tugasakhir.databinding.ItemAnggotaBinding
import com.windstrom5.tugasakhir.model.Perusahaan

class ListAnggotaAdapter(
    private var pekerjaList: MutableList<Pekerja>,
    private var adminList: MutableList<Admin>,
    private var perusahaan: Perusahaan,
    private var role:String,
    private val context: Context
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    inner class PekerjaViewHolder(private val binding: ItemAnggotaBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val clickedItem = if (position < adminList.size) {
                        adminList[position] // For AdminViewHolder
                    } else {
                        pekerjaList[position - adminList.size] // For PekerjaViewHolder
                    }
                    navigateToNextActivity(clickedItem)
                }
            }
        }

        fun bind(currentPekerja: Pekerja) {
            // Set data to views for Pekerja
            if (currentPekerja.profile != "null") {
                val url = "https://selected-jaguar-presently.ngrok-free.app/api/Pekerja/decryptProfile/${currentPekerja.id}"

                // Create an ImageRequest to fetch the profile image
                val imageRequest = ImageRequest(
                    url,
                    { response ->
                        // Set the Bitmap to the ImageView if the request is successful
                        binding.profileImageView.setImageBitmap(response)
                    },
                    0, 0, ImageView.ScaleType.CENTER_CROP, Bitmap.Config.RGB_565,
                    { error ->
                        // Log or handle the error if the image request fails
                        error.printStackTrace()
                        // Load a default image if there is an error
                        Glide.with(binding.profileImageView.context)
                            .load(R.drawable.profile)
                            .into(binding.profileImageView)
                    }
                )

                // Add the request to the Volley request queue
                val requestQueue = Volley.newRequestQueue(binding.profileImageView.context)
                requestQueue.add(imageRequest)
            }else{
                Glide.with(binding.profileImageView.context)
                    .load(R.drawable.profile)
                    .into(binding.profileImageView)
            }
            binding.nameTextView.text = currentPekerja.nama
            binding.roleTextView.text = "Pekerja"
        }
    }

    inner class AdminViewHolder(private val binding: ItemAnggotaBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val clickedItem = if (position < adminList.size) {
                        adminList[position] // For AdminViewHolder
                    } else {
                        pekerjaList[position - adminList.size] // For PekerjaViewHolder
                    }
                    navigateToNextActivity(clickedItem)
                }
            }
        }

        fun bind(currentAdmin: Admin) {
            // Set data to views for Admin
            if (currentAdmin.profile != "null") {
                val url = "https://selected-jaguar-presently.ngrok-free.app/api/Admin/decryptProfile/${currentAdmin.id}"

                // Create an ImageRequest to fetch the profile image
                val imageRequest = ImageRequest(
                    url,
                    { response ->
                        // Set the Bitmap to the ImageView if the request is successful
                        binding.profileImageView.setImageBitmap(response)
                    },
                    0, 0, ImageView.ScaleType.CENTER_CROP, Bitmap.Config.RGB_565,
                    { error ->
                        // Log or handle the error if the image request fails
                        error.printStackTrace()
                        // Load a default image if there is an error
                        Glide.with(binding.profileImageView.context)
                            .load(R.drawable.profile)
                            .into(binding.profileImageView)
                    }
                )

                // Add the request to the Volley request queue
                val requestQueue = Volley.newRequestQueue(binding.profileImageView.context)
                requestQueue.add(imageRequest)
            } else {
                Glide.with(binding.profileImageView.context)
                    .load(R.drawable.profile)
                    .into(binding.profileImageView)
            }
            binding.nameTextView.text = currentAdmin.nama
            binding.roleTextView.text = "Admin"
            Log.d("AdminAdapter", "Admin name: ${currentAdmin.nama}")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemAnggotaBinding.inflate(inflater, parent, false)
        return when (viewType) {
            VIEW_TYPE_PEKERJA -> PekerjaViewHolder(binding)
            VIEW_TYPE_ADMIN -> AdminViewHolder(binding)
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is PekerjaViewHolder -> {
                Log.d("AdminAdapter", "Binding PekerjaViewHolder at position: $position")
                val currentPekerja = if (position < adminList.size) {
                    null // Handle case where the position is not within the pekerjaList range
                } else {
                    pekerjaList[position - adminList.size]
                }
                currentPekerja?.let { holder.bind(it) }
            }
            is AdminViewHolder -> {
                Log.d("AdminAdapter", "Binding AdminViewHolder at position: $position")
                val currentAdmin = if (position < adminList.size) {
                    adminList[position]
                } else {
                    null // Handle case where the position is not within the adminList range
                }
                currentAdmin?.let { holder.bind(it) }
            }
        }
    }

    override fun getItemCount(): Int {
        return pekerjaList.size + adminList.size
    }

    override fun getItemViewType(position: Int): Int {
        return if (position < adminList.size) {
            VIEW_TYPE_ADMIN
        } else {
            VIEW_TYPE_PEKERJA
        }
    }

    fun updateData(newPekerjaList: MutableList<Pekerja>, newAdminList: MutableList<Admin>) {
        pekerjaList = newPekerjaList
        adminList = newAdminList
        notifyDataSetChanged()
    }
    fun updateOrAddData(newPekerjaList: MutableList<Pekerja>, newAdminList: MutableList<Admin>) {
        // Update existing Pekerja data
        newPekerjaList.forEach { newPekerja ->
            val existingPekerjaIndex = pekerjaList.indexOfFirst { it.id == newPekerja.id }
            if (existingPekerjaIndex != -1) {
                // If existing Pekerja found, update it
                pekerjaList[existingPekerjaIndex] = newPekerja
            } else {
                // If not found, add it to the list
                pekerjaList.add(newPekerja)
            }
        }

        // Update existing Admin data
        newAdminList.forEach { newAdmin ->
            val existingAdminIndex = adminList.indexOfFirst { it.id == newAdmin.id }
            if (existingAdminIndex != -1) {
                // If existing Admin found, update it
                adminList[existingAdminIndex] = newAdmin
            } else {
                // If not found, add it to the list
                adminList.add(newAdmin)
            }
        }

        notifyDataSetChanged()
    }

    fun addPekerja(pekerja: Pekerja) {
        pekerjaList.add(pekerja)
        notifyItemInserted(pekerjaList.size - 1) // Notify adapter that item is inserted at the end
    }

    fun addAdmin(admin: Admin) {
        adminList.add(admin)
        notifyItemInserted(adminList.size - 1) // Notify adapter that item is inserted at the end
    }

    companion object {
        private const val VIEW_TYPE_PEKERJA = 0
        private const val VIEW_TYPE_ADMIN = 1
    }

    interface OnItemClickListener {
        fun onPekerjaItemClick(pekerja: Pekerja)
        fun onAdminItemClick(admin: Admin)
    }

    private var itemClickListener: OnItemClickListener? = null

    fun setOnItemClickListener(listener: OnItemClickListener) {
        itemClickListener = listener
    }

    private fun navigateToNextActivity(clickedItem: Any) {
        val intent = Intent(context, ProfileActivity::class.java)
        val userBundle = Bundle()
        when (clickedItem) {
            is Pekerja -> {
                userBundle.putParcelable("user", clickedItem)
                userBundle.putParcelable("perusahaan", perusahaan)
                userBundle.putString("jenis","Pekerja")
                userBundle.putString("role",role)
            }
            is Admin -> {
                userBundle.putParcelable("user", clickedItem)
                userBundle.putParcelable("perusahaan", perusahaan)
                userBundle.putString("jenis","Admin")
                userBundle.putString("role",role)
            }
            else -> {
                // Handle unexpected item type
            }
        }
        intent.putExtra("data", userBundle)
        context.startActivity(intent)
    }
}