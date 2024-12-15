package com.example.ppapb_uas.network

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files
import io.minio.MinioClient
import io.minio.PutObjectArgs
import java.util.Date


object StorageService {
    private const val R2_ENDPOINT =
        "https://1a7da46ce1213e7471db17577aa342b2.r2.cloudflarestorage.com"
    private const val ACCESS_KEY = "2e1b59158ce60438d3dafa924b26a3c0"
    private const val SECRET_KEY =
        "6037b990b7e4a99311d9a96b69b90548a6576788c0e616de32a2846d25353f83"
    private const val BUCKET_NAME = "ppapb"
    private const val PUBLIC_URL = "https://pub-3c3c12eab1c246319f8888b0e7f2cc3e.r2.dev"

    private val minioClient = MinioClient.builder()
        .endpoint(R2_ENDPOINT)
        .credentials(ACCESS_KEY, SECRET_KEY)
        .build()

    suspend fun uploadFile(file: File, folder: String): String? = withContext(Dispatchers.IO) {
        try {
            // Prepare the file and its metadata
            val contentType =
                Files.probeContentType(file.toPath()) ?: "application/octet-stream"
            val fileSize = file.length()
            val fileName = "${folder}/${Date().time}.${file.extension}"

            // Upload the file to R2 bucket
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(BUCKET_NAME) // R2 bucket name
                    .`object`(fileName)  // Path in the bucket (e.g., "uploads/image.png")
                    .stream(file.inputStream(), fileSize, -1)
                    .contentType(contentType)
                    .build()
            )

            return@withContext "$PUBLIC_URL/$fileName"
        } catch (e: Exception) {
            println("Failed to upload image: ${e.message}")
            e.printStackTrace()
            return@withContext null
        }
    }

    fun getRealPathFromUri(context: Context, uri: Uri): String? {
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        val cursor = context.contentResolver.query(uri, projection, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val columnIndex = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                return it.getString(columnIndex)
            }
        }
        return null
    }
}