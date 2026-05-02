package by.kotlin.salesAppartment

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File

object ImgBBManager {
    private const val API_KEY = "44267d1172a7523c0ad2c9e3a7c34b09"

    private val client = OkHttpClient()

    suspend fun uploadImage(file: File, fileName: String): String? {
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("image", fileName, file.asRequestBody("image/*".toMediaTypeOrNull()))
            .build()

        val request = Request.Builder()
            .url("https://api.imgbb.com/1/upload?key=$API_KEY")
            .post(requestBody)
            .build()

        try {
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()
            val jsonObject = responseBody?.let { JSONObject(it) }
            return jsonObject?.getJSONObject("data")?.getString("url")
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}