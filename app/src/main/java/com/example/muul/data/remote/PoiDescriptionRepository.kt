package com.example.muul.data.remote

import android.util.Log
import com.example.muul.data.model.POI
import org.json.JSONArray
import org.json.JSONObject
import java.text.Normalizer
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

data class PoiDescriptionInfo(
    val text: String,
    val sourceTitle: String,
    val sourceUrl: String
)

class PoiDescriptionRepository {

    fun fetchDescription(poi: POI): PoiDescriptionInfo? {
        runCatching {
            searchNearbyWikipedia(poi)
        }.onFailure { error ->
            Log.w("MUUL_POI_DESC", "Error buscando por coordenadas: ${error.message}")
        }.getOrNull()?.let { return it }

        val queries = buildQueries(poi)

        for (query in queries) {
            val description = runCatching {
                searchWikipedia(query, poi)
            }.onFailure { error ->
                Log.w("MUUL_POI_DESC", "Error buscando '$query': ${error.message}")
            }.getOrNull()

            if (description != null) {
                return description
            }
        }

        return null
    }

    private fun searchNearbyWikipedia(poi: POI): PoiDescriptionInfo? {
        val url = URL(
            "https://es.wikipedia.org/w/api.php" +
                    "?action=query" +
                    "&format=json" +
                    "&list=geosearch" +
                    "&gscoord=${poi.latitud}%7C${poi.longitud}" +
                    "&gsradius=450" +
                    "&gslimit=10"
        )

        val body = requestJson(url)
        val results = JSONObject(body)
            .optJSONObject("query")
            ?.optJSONArray("geosearch")
            ?: return null

        val matchingPageId = results.asSequence()
            .firstOrNull { item ->
                val title = item.optString("title")
                val distance = item.optDouble("dist", Double.MAX_VALUE)
                distance <= 450.0 && isLikelySamePlace(title, poi)
            }
            ?.optInt("pageid")
            ?.takeIf { it > 0 }
            ?: return null

        return fetchPageExtract(matchingPageId, poi)
    }

    private fun searchWikipedia(query: String, poi: POI): PoiDescriptionInfo? {
        val encodedQuery = URLEncoder.encode(query, Charsets.UTF_8.name())
        val url = URL(
            "https://es.wikipedia.org/w/api.php" +
                    "?action=query" +
                    "&format=json" +
                    "&prop=extracts" +
                    "&exintro=1" +
                    "&explaintext=1" +
                    "&exsentences=2" +
                    "&exlimit=1" +
                    "&redirects=1" +
                    "&generator=search" +
                    "&gsrlimit=5" +
                    "&gsrsearch=$encodedQuery"
        )

        val body = requestJson(url)
        return parseWikipediaSearchResponse(body, poi)
    }

    private fun fetchPageExtract(pageId: Int, poi: POI): PoiDescriptionInfo? {
        val url = URL(
            "https://es.wikipedia.org/w/api.php" +
                    "?action=query" +
                    "&format=json" +
                    "&prop=extracts" +
                    "&exintro=1" +
                    "&explaintext=1" +
                    "&exsentences=2" +
                    "&pageids=$pageId"
        )

        val body = requestJson(url)
        return parseWikipediaPages(body, poi)
    }

    private fun requestJson(url: URL): String {
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 4_000
            readTimeout = 4_000
            setRequestProperty("Accept", "application/json")
            setRequestProperty(
                "User-Agent",
                "MuulAndroid/1.0 (https://example.com; contacto@muul.local)"
            )
        }

        return try {
            if (connection.responseCode !in 200..299) {
                Log.w(
                    "MUUL_POI_DESC",
                    "Wikipedia respondió ${connection.responseCode} para $url"
                )
                return ""
            }

            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private fun parseWikipediaSearchResponse(body: String, poi: POI): PoiDescriptionInfo? {
        if (body.isBlank()) return null
        return parseWikipediaPages(body, poi)
    }

    private fun parseWikipediaPages(body: String, poi: POI): PoiDescriptionInfo? {
        if (body.isBlank()) return null

        val pages = JSONObject(body)
            .optJSONObject("query")
            ?.optJSONObject("pages")
            ?: return null

        val matchingPages = pages.keys().asSequence()
            .mapNotNull { key -> pages.optJSONObject(key) }
            .filter { page ->
                isLikelySamePlace(page.optString("title"), poi)
            }
            .sortedBy { it.optInt("index", Int.MAX_VALUE) }
            .toList()

        val page = matchingPages.firstOrNull() ?: return null
        val title = page.optString("title").takeIf { it.isNotBlank() } ?: return null
        val extract = page.optString("extract")
            .replace(Regex("\\s+"), " ")
            .trim()
            .takeIf { it.length >= MIN_DESCRIPTION_LENGTH }
            ?: return null

        return PoiDescriptionInfo(
            text = extract,
            sourceTitle = title,
            sourceUrl = "https://es.wikipedia.org/wiki/${title.replace(" ", "_")}"
        )
    }

    private fun buildQueries(poi: POI): List<String> {
        val name = poi.nombre.trim()
        if (name.isBlank()) return emptyList()

        val normalizedAddress = poi.direccion
            ?.lowercase(Locale.getDefault())
            .orEmpty()

        val cityHint = when {
            "campeche" in normalizedAddress -> "Campeche"
            "méxico" in normalizedAddress || "mexico" in normalizedAddress -> "México"
            else -> ""
        }

        return listOf(
            "$name $cityHint",
            "$name ${poi.categoria}",
            name
        ).map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
    }

    private fun isLikelySamePlace(title: String, poi: POI): Boolean {
        val poiName = poi.nombre.trim()
        if (title.isBlank() || poiName.isBlank()) return false

        val normalizedTitle = normalize(title)
        val normalizedName = normalize(poiName)

        if (normalizedTitle == normalizedName) return true
        if (normalizedTitle.contains(normalizedName) || normalizedName.contains(normalizedTitle)) {
            return true
        }

        val titleTokens = significantTokens(title)
        val poiTokens = significantTokens(poiName)
        if (poiTokens.isEmpty() || titleTokens.isEmpty()) return false

        val overlap = poiTokens.count { it in titleTokens }
        val overlapRatio = overlap.toDouble() / poiTokens.size.toDouble()

        return overlap >= 2 && overlapRatio >= 0.67
    }

    private fun normalize(text: String): String {
        return Normalizer.normalize(text.lowercase(Locale.getDefault()), Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .replace(Regex("[^a-z0-9 ]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun significantTokens(text: String): Set<String> {
        return normalize(text)
            .split(" ")
            .filter { token ->
                token.length >= 3 && token !in STOP_WORDS
            }
            .toSet()
    }

    private fun JSONArray.asSequence(): Sequence<JSONObject> {
        return sequence {
            for (index in 0 until length()) {
                optJSONObject(index)?.let { yield(it) }
            }
        }
    }

    private companion object {
        private const val MIN_DESCRIPTION_LENGTH = 45
        private val STOP_WORDS = setOf(
            "del",
            "los",
            "las",
            "una",
            "uno",
            "san",
            "santa",
            "santo",
            "iglesia",
            "museo",
            "parque",
            "plaza",
            "campeche",
            "mexico"
        )
    }
}
