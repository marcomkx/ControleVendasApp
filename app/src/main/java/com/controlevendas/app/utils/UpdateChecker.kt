package com.controlevendas.app.utils

import android.content.Context
import android.util.Log
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
    private const val GITHUB_REPO = "marcomkx/ControleVendasApp"
    private const val TAG = "UpdateChecker"

    suspend fun checkForUpdate(context: Context): GitHubRelease? = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://api.github.com/repos/$GITHUB_REPO/releases/latest")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "ControleVendasApp")
                connectTimeout = 8000
                readTimeout = 8000
            }

            val responseCode = connection.responseCode
            Log.d(TAG, "Resposta da API do GitHub: $responseCode")

            if (responseCode == HttpURLConnection.HTTP_OK) {
                val responseString = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(responseString)
                val rawTag = json.optString("tag_name", "").trim()
                val tagName = rawTag.removePrefix("v").removePrefix("V").trim()
                val htmlUrl = json.optString("html_url", "https://github.com/$GITHUB_REPO/releases")

                Log.d(TAG, "Última versão encontrada no GitHub: $tagName (Tag original: $rawTag)")

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
                } catch (_: Exception) {
                    "1.0"
                }

                Log.d(TAG, "Versão atual do aplicativo instalado: $currentVersionName")

                if (isVersionNewer(currentVersionName, tagName)) {
                    Log.d(TAG, "Nova versão identificada! ($currentVersionName -> $tagName)")
                    return@withContext GitHubRelease(
                        versionName = tagName,
                        releaseUrl = htmlUrl,
                        apkUrl = apkUrl ?: htmlUrl
                    )
                } else {
                    Log.d(TAG, "Aplicativo já está na versão mais recente ($currentVersionName vs $tagName).")
                }
            } else {
                Log.e(TAG, "Erro HTTP ao verificar atualização: $responseCode")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exceção ao verificar atualizações", e)
        }
        return@withContext null
    }

    private fun isVersionNewer(current: String, latest: String): Boolean {
        try {
            val currentParts = current.split(".").map { part ->
                part.filter { it.isDigit() }.toIntOrNull() ?: 0
            }
            val latestParts = latest.split(".").map { part ->
                part.filter { it.isDigit() }.toIntOrNull() ?: 0
            }

            for (i in 0 until maxOf(currentParts.size, latestParts.size)) {
                val curr = currentParts.getOrElse(i) { 0 }
                val lat = latestParts.getOrElse(i) { 0 }
                if (lat > curr) return true
                if (lat < curr) return false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao comparar versões: current=$current, latest=$latest", e)
        }
        return false
    }
}
