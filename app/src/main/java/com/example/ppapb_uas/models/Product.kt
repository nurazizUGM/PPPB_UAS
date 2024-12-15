package com.example.ppapb_uas.models

import android.support.annotation.NonNull
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.ppapb_uas.utils.Utils
import com.google.gson.annotations.SerializedName

@Entity(tableName = "products")
data class Product(
    val name: String,
    val price: Int,
    val description: String,
    val image: String,
    @SerializedName("user_id")
    val userId: String,
    @SerializedName("_id")
    @PrimaryKey(autoGenerate = false)
    @NonNull
    val id: String = Utils.randomString(12),
    @SerializedName("created_at")
    @ColumnInfo(name = "created_at")
    val createdAt: Int? = (System.currentTimeMillis() / 1000).toInt()
)
