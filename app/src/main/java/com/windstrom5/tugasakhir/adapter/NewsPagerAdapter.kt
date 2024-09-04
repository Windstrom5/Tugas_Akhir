package com.windstrom5.tugasakhir.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.windstrom5.tugasakhir.R
import com.windstrom5.tugasakhir.feature.News

class NewsPagerAdapter(private val newsList: List<News>) : RecyclerView.Adapter<NewsPagerAdapter.NewsViewHolder>() {

    inner class NewsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val newsImage: ImageView = itemView.findViewById(R.id.newsImage)
        val newsTitle: TextView = itemView.findViewById(R.id.newsTitle)
        val newsDescription: TextView = itemView.findViewById(R.id.newsDescription)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_news, parent, false)
        return NewsViewHolder(view)
    }

    override fun onBindViewHolder(holder: NewsViewHolder, position: Int) {
        val news = newsList[position]

        // Display the first post's image, title, and description
        Glide.with(holder.itemView.context)
            .load(news.posts[0].thumbnail)
            .into(holder.newsImage)

        holder.newsTitle.text = news.posts[0].title
        holder.newsDescription.text = news.posts[0].description
    }

    override fun getItemCount(): Int = newsList.size
}