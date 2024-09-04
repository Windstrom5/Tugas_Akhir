package com.windstrom5.tugasakhir.feature

// News.kt
data class News(
    val link: String,
    val description: String,
    val title: String,
    val image: String,
    val posts: List<Post>
)

data class Post(
    val link: String,
    val title: String,
    val pubDate: String,
    val description: String,
    val thumbnail: String
)
