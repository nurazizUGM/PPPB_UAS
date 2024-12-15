package com.example.ppapb_uas.models

import com.example.ppapb_uas.utils.Utils
import com.google.gson.annotations.SerializedName

data class User(
    @SerializedName("full_name")
    var fullName: String,
    var username: String,
    var email: String,
    var password: String,
    var products: List<String>? = listOf(),
    var phone: String? = null,
    @SerializedName("profile_picture")
    var profilePicture: String? = null,
    @SerializedName("_id")
    var id: String? = Utils.randomString(12),
    @SerializedName("created_at")
    var createdAt: Int? = (System.currentTimeMillis() / 1000).toInt()
)
