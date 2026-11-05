package com.controlevendas.app.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.controlevendas.app.data.model.Venda
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter

object ExcelUtils {

    private const val HEADER = "Mês Referência;Cliente;Produto;Valor Peça;Valor Pago;Saldo Devedor;Margem Lucro (%);Lucro (R$);Status"

    fun exportarVendasParaUri(context: Context, vendas: List<Venda>, uri: Uri): Boolean {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream, Charsets.UTF_8).use { writer ->
                    // Adiciona BOM UTF-8 para o Excel reconhecer acentos automaticamente
                    writer.write("\uFEFF")
                    writer.write(HEADER)
                    writer.write("\n")

                    vendas.forEach { v ->
                        val linha = listOf(
                            v.mesReferencia,
                            v.cliente,
                            v.produto,
                            String.format("%.2f", v.valorPeca).replace(".", ","),
                            String.format("%.2f", v.valorPago).replace(".", ","),
                            String.format("%.2f", v.saldoDevedor).replace(".", ","),
                            String.format("%.1f", v.margemLucroPercentual).replace(".", ","),
                            String.format("%.2f", v.lucro).replace(".", ","),
                            v.status
                        ).joinToString(";")
                        writer.write(linha)
                        writer.write("\n")
                    }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun abrirNoGooglePlanilhasOuCompartilhar(context: Context, vendas: List<Venda>) {
        try {
            val exportDir = File(context.cacheDir, "export")
            if (!exportDir.exists()) exportDir.mkdirs()

            val file = File(exportDir, "Controle_de_Vendas.csv")
            FileOutputStream(file).use { fos ->
                OutputStreamWriter(fos, Charsets.UTF_8).use { writer ->
                    writer.write("\uFEFF")
                    writer.write(HEADER)
                    writer.write("\n")

                    vendas.forEach { v ->
                        val linha = listOf(
                            v.mesReferencia,
                            v.cliente,
                            v.produto,
                            String.format("%.2f", v.valorPeca).replace(".", ","),
                            String.format("%.2f", v.valorPago).replace(".", ","),
                            String.format("%.2f", v.saldoDevedor).replace(".", ","),
                            String.format("%.1f", v.margemLucroPercentual).replace(".", ","),
                            String.format("%.2f", v.lucro).replace(".", ","),
                            v.status
                        ).joinToString(";")
                        writer.write(linha)
                        writer.write("\n")
                    }
                }
            }

            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(intent, "Abrir no Google Planilhas ou Compartilhar")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Erro ao preparar arquivo para exportação", Toast.LENGTH_SHORT).show()
        }
    }

    fun lerVendasDoUri(context: Context, uri: Uri): List<Venda> {
        val lista = mutableListOf<Venda>()
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                    var linha: String? = reader.readLine()

                    // Se a primeira linha tiver BOM UTF-8, removemos
                    if (linha != null && linha.startsWith("\uFEFF")) {
                        linha = linha.substring(1)
                    }

                    // Pula cabeçalho se houver
                    if (linha != null && (linha.contains("Mês", ignoreCase = true) || linha.contains("Cliente", ignoreCase = true))) {
                        linha = reader.readLine()
                    }

                    while (linha != null) {
                        val l = linha!!
                        val partes = if (l.contains(";")) {
                            l.split(";")
                        } else {
                            l.split(",")
                        }

                        if (partes.size >= 3) {
                            val mes = partes.getOrNull(0)?.trim().orEmpty()
                            val cliente = partes.getOrNull(1)?.trim().orEmpty()
                            val produto = partes.getOrNull(2)?.trim().orEmpty()

                            if (mes.isNotBlank() && cliente.isNotBlank() && produto.isNotBlank()) {
                                val valorPeca = parseDouble(partes.getOrNull(3))
                                val valorPago = parseDouble(partes.getOrNull(4))
                                val saldo = (valorPeca - valorPago).coerceAtLeast(0.0)
                                val status = if (saldo <= 0.001) "Quitado" else "Pendente"

                                lista.add(
                                    Venda(
                                        mesReferencia = mes,
                                        cliente = cliente,
                                        produto = produto,
                                        valorPeca = valorPeca,
                                        valorPago = valorPago,
                                        saldoDevedor = saldo,
                                        margemLucroPercentual = 30.0,
                                        lucro = valorPeca * 0.30,
                                        status = status
                                    )
                                )
                            }
                        }
                        linha = reader.readLine()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return lista
    }

    private fun parseDouble(valorStr: String?): Double {
        if (valorStr.isNullOrBlank()) return 0.0
        val limpo = valorStr.replace("R$", "").replace(" ", "").trim()
            .replace(".", "")
            .replace(",", ".")
        return limpo.toDoubleOrNull() ?: 0.0
    }
}
