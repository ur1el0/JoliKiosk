package com.example.webdev2

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class MenuItem(
    val id: Int,
    val name: String,
    val priceInPesos: Int,
    val category: String,
    val description: String
)

data class SubmittedOrder(val id: Int, val queueNumber: Int, val status: String)

class KioskApi(private val baseUrl: String = BuildConfig.KIOSK_API_URL) {
    suspend fun menu(): List<MenuItem> = withContext(Dispatchers.IO) {
        val response = request("GET", "/api/menu-items/")
        val items = JSONObject(response).getJSONArray("items")
        List(items.length()) { index ->
            val item = items.getJSONObject(index)
            MenuItem(
                id = item.getInt("id"),
                name = item.getString("name"),
                priceInPesos = item.getInt("price"),
                category = item.getString("category"),
                description = item.getString("description")
            )
        }
    }

    suspend fun submitOrder(lines: Map<Int, Int>): SubmittedOrder = withContext(Dispatchers.IO) {
        val items = JSONArray()
        lines.filterValues { it > 0 }.forEach { (id, quantity) ->
            items.put(JSONObject().put("menuItemId", id).put("quantity", quantity))
        }
        val response = request("POST", "/api/orders/", JSONObject().put("items", items).toString())
        val order = JSONObject(response)
        SubmittedOrder(order.getInt("id"), order.getInt("queueNumber"), order.getString("status"))
    }

    private fun request(method: String, path: String, body: String? = null): String {
        val connection = (URL(baseUrl + path).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 10_000
            readTimeout = 10_000
            setRequestProperty("Accept", "application/json")
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                outputStream.bufferedWriter().use { it.write(body) }
            }
        }
        val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
        val response = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        if (connection.responseCode !in 200..299) {
            val message = runCatching { JSONObject(response).optString("error") }.getOrDefault("")
            throw IllegalStateException(message.ifBlank { "The kiosk service is unavailable." })
        }
        return response
    }
}
