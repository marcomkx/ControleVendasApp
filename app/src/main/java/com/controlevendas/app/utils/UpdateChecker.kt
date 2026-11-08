package com.controlevendas.app.utils

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
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

    fun baixarEInstalarApk(context: Context, release: GitHubRelease) {
        val downloadUrl = release.apkUrl ?: release.releaseUrl
        if (!downloadUrl.endsWith(".apk", ignoreCase = true)) {
            // Se por algum motivo não for um link direto de APK, abre no navegador
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            return
        }

        try {
            val fileName = "ControleVendas_v${release.versionName}.apk"
            val destination = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            val file = File(destination, fileName)
            if (file.exists()) {
                file.delete()
            }

            val request = DownloadManager.Request(Uri.parse(downloadUrl)).apply {
                setTitle("Atualização v${release.versionName}")
                setDescription("Baixando atualização do Controle de Vendas...")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationUri(Uri.fromFile(file))
                setMimeType("application/vnd.android.package-archive")
            }

            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val downloadId = downloadManager.enqueue(request)

            Toast.makeText(context, "Baixando atualização v${release.versionName} em segundo plano...", Toast.LENGTH_LONG).show()

            val onComplete = object : BroadcastReceiver() {
                override fun onReceive(c: Context, intent: Intent) {
                    val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                    if (id == downloadId) {
                        try {
                            context.unregisterReceiver(this)
                        } catch (_: Exception) {}

                        instalarApk(context, file)
                    }
                }
            }

            ContextCompat.registerReceiver(
                context,
                onComplete,
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                ContextCompat.RECEIVER_EXPORTED
            )
        } catch (e: Exception) {
            Log.e(TAG, "Erro no DownloadManager, abrindo no navegador", e)
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }

    private fun instalarApk(context: Context, file: File) {
        try {
            if (!file.exists()) return

            val apkUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao iniciar instalador do APK", e)
            Toast.makeText(context, "Download concluído! Verifique a pasta Downloads para instalar.", Toast.LENGTH_LONG).show()
        }
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
