package com.controlevendas.app.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.controlevendas.app.data.model.Venda
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader

object ExcelUtils {

    fun exportarVendasParaUri(context: Context, vendas: List<Venda>, uri: Uri): Boolean {
        return try {
            val workbook = XSSFWorkbook()
            val sheet = workbook.createSheet("Controle de Vendas")

            val headerRow = sheet.createRow(0)
            val headers = listOf("Mês Referência", "Cliente", "Produto", "Valor Peça", "Valor Pago", "Saldo Devedor", "Margem Lucro (%)", "Lucro (R$)", "Status")
            headers.forEachIndexed { index, title ->
                headerRow.createCell(index).setCellValue(title)
            }

            vendas.forEachIndexed { rowIndex, v ->
                val row = sheet.createRow(rowIndex + 1)
                row.createCell(0).setCellValue(v.mesReferencia)
                row.createCell(1).setCellValue(v.cliente)
                row.createCell(2).setCellValue(v.produto)
                row.createCell(3).setCellValue(v.valorPeca)
                row.createCell(4).setCellValue(v.valorPago)
                row.createCell(5).setCellValue(v.saldoDevedor)
                row.createCell(6).setCellValue(v.margemLucroPercentual)
                row.createCell(7).setCellValue(v.lucro)
                row.createCell(8).setCellValue(v.status)
            }

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                workbook.write(outputStream)
            }
            workbook.close()
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

            val file = File(exportDir, "Controle_de_Vendas.xlsx")
            FileOutputStream(file).use { fos ->
                val workbook = XSSFWorkbook()
                val sheet = workbook.createSheet("Controle de Vendas")

                val headerRow = sheet.createRow(0)
                val headers = listOf("Mês Referência", "Cliente", "Produto", "Valor Peça", "Valor Pago", "Saldo Devedor", "Margem Lucro (%)", "Lucro (R$)", "Status")
                headers.forEachIndexed { index, title ->
                    headerRow.createCell(index).setCellValue(title)
                }

                vendas.forEachIndexed { rowIndex, v ->
                    val row = sheet.createRow(rowIndex + 1)
                    row.createCell(0).setCellValue(v.mesReferencia)
                    row.createCell(1).setCellValue(v.cliente)
                    row.createCell(2).setCellValue(v.produto)
                    row.createCell(3).setCellValue(v.valorPeca)
                    row.createCell(4).setCellValue(v.valorPago)
                    row.createCell(5).setCellValue(v.saldoDevedor)
                    row.createCell(6).setCellValue(v.margemLucroPercentual)
                    row.createCell(7).setCellValue(v.lucro)
                    row.createCell(8).setCellValue(v.status)
                }

                workbook.write(fos)
                workbook.close()
            }

            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(intent, "Abrir no Excel ou Compartilhar")
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
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val workbook = XSSFWorkbook(inputStream)
                    val sheet = workbook.getSheetAt(0)
                    for (rowIndex in 1..sheet.lastRowNum) {
                        val row = sheet.getRow(rowIndex) ?: continue
                        val mes = row.getCell(0)?.stringCellValue?.trim().orEmpty()
                        val cliente = row.getCell(1)?.stringCellValue?.trim().orEmpty()
                        val produto = row.getCell(2)?.stringCellValue?.trim().orEmpty()

                        if (mes.isNotBlank() && cliente.isNotBlank() && produto.isNotBlank()) {
                            val valorPeca = getCellNumericValue(row.getCell(3))
                            val valorPago = getCellNumericValue(row.getCell(4))
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
                    workbook.close()
                    if (lista.isNotEmpty()) return lista
                }
            } catch (_: Exception) {
                // Fallback para CSV
            }

            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                    var linha: String? = reader.readLine()
                    if (linha != null && linha.startsWith("\uFEFF")) {
                        linha = linha.substring(1)
                    }
                    if (linha != null && (linha.contains("Mês", ignoreCase = true) || linha.contains("Cliente", ignoreCase = true))) {
                        linha = reader.readLine()
                    }

                    while (linha != null) {
                        val l = linha
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

    private fun getCellNumericValue(cell: org.apache.poi.ss.usermodel.Cell?): Double {
        if (cell == null) return 0.0
        return when (cell.cellType) {
            CellType.NUMERIC -> cell.numericCellValue
            CellType.STRING -> parseDouble(cell.stringCellValue)
            else -> 0.0
        }
    }

    private fun parseDouble(valorStr: String?): Double {
        if (valorStr.isNullOrBlank()) return 0.0
        val limpo = valorStr.replace("R$", "").replace(" ", "").trim()
            .replace(".", "")
            .replace(",", ".")
        return limpo.toDoubleOrNull() ?: 0.0
    }
}
