package com.controlevendas.app.utils

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class GitHubRelease(
    val versionName: String,
    val releaseUrl: String,
    val apkUrl: String?
)

object UpdateChecker {
    // ⚠️ ATENÇÃO: Substitua pelo seu usuário e nome do repositório no GitHub
    // Exemplo: "seu-usuario/ControleVendasApp"
    private const val GITHUB_REPO = "seu-usuario/ControleVendasApp"

    suspend fun checkForUpdate(context: Context): GitHubRelease? = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://api.github.com/repos/$GITHUB_REPO/releases/latest")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github+json")
                connectTimeout = 5000
                readTimeout = 5000
            }

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val responseString = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(responseString)
                val tagName = json.optString("tag_name", "").trim().removePrefix("v")
                val htmlUrl = json.optString("html_url", "https://github.com/$GITHUB_REPO/releases")

                // Procurar por asset .apk se houver
                var apkUrl: String? = null
                val assets = json.optJSONArray("assets")
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        val name = asset.optString("name", "")
                        if (name.endsWith(".apk", ignoreCase = true)) {
                            apkUrl = asset.optString("browser_download_url")
                            break
                        }
                    }
                }

                val currentVersionName = try {
                    val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                    pInfo.versionName ?: "1.0"
                } catch (e: Exception) {
                    "1.0"
                }

                if (isVersionNewer(currentVersionName, tagName)) {
                    return@withContext GitHubRelease(
                        versionName = tagName,
                        releaseUrl = htmlUrl,
                        apkUrl = apkUrl ?: htmlUrl
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext null
    }

    private fun isVersionNewer(current: String, latest: String): Boolean {
        try {
            val currentParts = current.split(".").map { it.toIntOrNull() ?: 0 }
            val latestParts = latest.split(".").map { it.toIntOrNull() ?: 0 }
            
            for (i in 0 until maxOf(currentParts.size, latestParts.size)) {
                val curr = currentParts.getOrElse(i) { 0 }
                val lat = latestParts.getOrElse(i) { 0 }
                if (lat > curr) return true
                if (lat < curr) return false
            }
        } catch (e: Exception) {
            // fallback
        }
        return false
    }
}
