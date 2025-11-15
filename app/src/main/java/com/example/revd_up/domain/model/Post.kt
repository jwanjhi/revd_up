package com.example.revd_up.domain.model

import com.example.revd_up.data.api.Comment
import kotlinx.serialization.Serializable

@Serializable
data class Post(
    val id: String? = null,
    val userId: String? = null,
    val caption: String,
    val tags: List<String>? = null,
    val mediaUrl: String? = null,
    val mediaType: String? = null,
    val likes: Int = 0,
    val comments: List<Comment>? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
)