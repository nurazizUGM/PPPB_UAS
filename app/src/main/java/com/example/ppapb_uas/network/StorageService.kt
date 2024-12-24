package com.example.ppapb_uas.network

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Configuration
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest
import java.io.File
import java.net.URI
import java.net.URLConnection
import java.time.Duration
import java.util.Date


object StorageService {
    private const val R2_ENDPOINT =
        "https://1a7da46ce1213e7471db17577aa342b2.r2.cloudflarestorage.com"
    private const val ACCESS_KEY = "2e1b59158ce60438d3dafa924b26a3c0"
    private const val SECRET_KEY =
        "6037b990b7e4a99311d9a96b69b90548a6576788c0e616de32a2846d25353f83"
    private const val BUCKET_NAME = "ppapb"
    private const val PUBLIC_URL = "https://s3.crazyz.biz.id"

    private val presigner: S3Presigner = buildS3Presigner()
    private fun buildS3Presigner(): S3Presigner {
        val credentials = AwsBasicCredentials.create(ACCESS_KEY, SECRET_KEY)

        return S3Presigner.builder().endpointOverride(URI.create(R2_ENDPOINT))
            .credentialsProvider(StaticCredentialsProvider.create(credentials))
            .region(Region.of("auto")).serviceConfiguration(
                S3Configuration.builder().pathStyleAccessEnabled(true).build()
            ).build()
    }

    private fun generatePresignedUploadUrl(
        objectKey: String?
    ): String {
        val presignRequest =
            PutObjectPresignRequest.builder().signatureDuration(Duration.ofMinutes(15))
                .putObjectRequest { builder: PutObjectRequest.Builder ->
                    builder.bucket(BUCKET_NAME).key(objectKey).build()
                }.build()

        val presignedRequest: PresignedPutObjectRequest = presigner.presignPutObject(presignRequest)
        return presignedRequest.url().toString()
    }

    suspend fun uploadFile(file: File, folder: String): String? = withContext(Dispatchers.IO) {
        try {
            val fileName = "${folder}/${Date().time}.${file.extension}"
            val uploadUrl: String = generatePresignedUploadUrl(fileName)

            // Create OkHttp client
            val client = OkHttpClient.Builder()
                .hostnameVerifier { _, _ -> true }
                .build()

            // Create the request body with the file
            val mimeType = getMimeType(file) ?: "application/octet-stream"
            val requestBody = file.asRequestBody(mimeType.toMediaTypeOrNull())

            // Build the request
            val request = Request.Builder()
                .url(uploadUrl)
                .put(requestBody)
                .build()

            // Execute the request
            val response = client.newCall(request).execute()

            // Check the response
            if (response.isSuccessful) {
                val publicUrl = "$PUBLIC_URL/$fileName"
                println("File uploaded successfully to $publicUrl")
                return@withContext publicUrl
            } else {
                println("Failed to upload file: HTTP response code ${response.code}")
                return@withContext null
            }
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

    suspend fun getMimeType(file: File): String? = withContext(Dispatchers.IO) {
        return@withContext URLConnection.guessContentTypeFromName(file.name)
    }
}