package com.controlevendas.app.utils

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.text.TextPaint
import android.text.TextUtils
import android.widget.Toast
import androidx.core.content.FileProvider
import com.controlevendas.app.ui.viewmodel.ClienteResumo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ClienteCardImageUtils {

    /**
     * Gera o bitmap em alta definição representando o card/recibo visual do cliente.
     */
    fun gerarCardBitmap(cliente: ClienteResumo): Bitmap {
        val formatador = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
        val dataHoraFormatada = SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", Locale("pt", "BR")).format(Date())

        val cardWidth = 840
        val paddingX = 40f

        // Cálculo dinâmico da altura
        val headerHeight = 130f
        val clientInfoHeight = 85f
        val summaryBoxHeight = 115f
        val sectionTitleHeight = 55f
        val itemHeight = 48f
        val itemsTotalHeight = (cliente.vendas.size.coerceAtLeast(1) * itemHeight)
        val footerHeight = 110f
        val cardHeight = (headerHeight + clientInfoHeight + summaryBoxHeight + sectionTitleHeight + itemsTotalHeight + footerHeight).toInt()

        val bitmap = Bitmap.createBitmap(cardWidth, cardHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Fundo do Cartão com cantos arredondados
        val cardRect = RectF(0f, 0f, cardWidth.toFloat(), cardHeight.toFloat())
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
        }
        canvas.drawRoundRect(cardRect, 32f, 32f, bgPaint)

        // Borda externa sutil
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E0E0E0")
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        canvas.drawRoundRect(cardRect, 32f, 32f, borderPaint)

        // --- 1. BANNER DE CABEÇALHO ---
        val headerPath = Path()
        val cornerRadius = 32f
        headerPath.moveTo(0f, cornerRadius)
        headerPath.quadTo(0f, 0f, cornerRadius, 0f)
        headerPath.lineTo(cardWidth - cornerRadius, 0f)
        headerPath.quadTo(cardWidth.toFloat(), 0f, cardWidth.toFloat(), cornerRadius)
        headerPath.lineTo(cardWidth.toFloat(), headerHeight)
        headerPath.lineTo(0f, headerHeight)
        headerPath.close()

        val headerGradient = LinearGradient(
            0f, 0f, cardWidth.toFloat(), headerHeight,
            Color.parseColor("#4A148C"),
            Color.parseColor("#7B1FA2"),
            Shader.TileMode.CLAMP
        )
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = headerGradient
        }
        canvas.drawPath(headerPath, headerPaint)

        // Texto do cabeçalho
        val appTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E1BEE7")
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.15f
        }
        canvas.drawText("CONTROLE DE VENDAS", paddingX, 48f, appTitlePaint)

        val headerTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 38f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("Resumo do Cliente", paddingX, 98f, headerTitlePaint)

        var currentY = headerHeight + 35f

        // --- 2. INFORMAÇÕES DO CLIENTE ---
        val clientNamePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#212121")
            textSize = 36f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val nomeTruncado = TextUtils.ellipsize(
            cliente.nome,
            clientNamePaint,
            cardWidth - (paddingX * 2),
            TextUtils.TruncateAt.END
        ).toString()
        canvas.drawText(nomeTruncado, paddingX, currentY, clientNamePaint)

        currentY += 32f
        val clientSubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#757575")
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        canvas.drawText("${cliente.totalPecas} peças • Compras registradas", paddingX, currentY, clientSubPaint)

        currentY += 35f

        // --- 3. CARDS DE RESUMO FINANCEIRO (3 COLUNAS) ---
        val boxWidth = (cardWidth - (paddingX * 2) - 30f) / 3f
        val boxHeight = 95f
        val boxRadius = 18f

        // Card 1: Total Comprado
        desenharBoxResumo(
            canvas = canvas,
            left = paddingX,
            top = currentY,
            width = boxWidth,
            height = boxHeight,
            radius = boxRadius,
            bgColor = Color.parseColor("#F5F5F7"),
            borderColor = Color.parseColor("#E0E0E0"),
            label = "Total Comprado",
            labelColor = Color.parseColor("#616161"),
            value = formatador.format(cliente.totalComprado),
            valueColor = Color.parseColor("#212121")
        )

        // Card 2: Total Pago
        desenharBoxResumo(
            canvas = canvas,
            left = paddingX + boxWidth + 15f,
            top = currentY,
            width = boxWidth,
            height = boxHeight,
            radius = boxRadius,
            bgColor = Color.parseColor("#E8F5E9"),
            borderColor = Color.parseColor("#C8E6C9"),
            label = "Valor Pago",
            labelColor = Color.parseColor("#2E7D32"),
            value = formatador.format(cliente.totalPago),
            valueColor = Color.parseColor("#1B5E20")
        )

        // Card 3: Saldo Devedor / Status
        val temDivida = cliente.saldoDevedor > 0.001
        val devedorBg = if (temDivida) Color.parseColor("#FCE4EC") else Color.parseColor("#E8F5E9")
        val devedorBorder = if (temDivida) Color.parseColor("#F8BBD0") else Color.parseColor("#C8E6C9")
        val devedorLabel = if (temDivida) "Saldo Devedor" else "Status"
        val devedorLabelColor = if (temDivida) Color.parseColor("#C2185B") else Color.parseColor("#2E7D32")
        val devedorValue = if (temDivida) formatador.format(cliente.saldoDevedor) else "100% Quitado"
        val devedorValueColor = if (temDivida) Color.parseColor("#AD1457") else Color.parseColor("#1B5E20")

        desenharBoxResumo(
            canvas = canvas,
            left = paddingX + (boxWidth + 15f) * 2,
            top = currentY,
            width = boxWidth,
            height = boxHeight,
            radius = boxRadius,
            bgColor = devedorBg,
            borderColor = devedorBorder,
            label = devedorLabel,
            labelColor = devedorLabelColor,
            value = devedorValue,
            valueColor = devedorValueColor
        )

        currentY += boxHeight + 40f

        // --- 4. DETALHAMENTO DAS COMPRAS ---
        val sectionTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#37474F")
            textSize = 26f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("Detalhamento dos Itens", paddingX, currentY, sectionTitlePaint)

        currentY += 18f

        val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E0E0E0")
            strokeWidth = 2f
        }
        canvas.drawLine(paddingX, currentY, cardWidth - paddingX, currentY, dividerPaint)

        currentY += 20f

        // Lista de produtos
        val rowBgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val itemTitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#212121")
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        val itemValorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#212121")
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }

        if (cliente.vendas.isEmpty()) {
            val emptyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.GRAY
                textSize = 22f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            }
            canvas.drawText("Nenhuma compra registrada.", paddingX, currentY + 25f, emptyPaint)
            currentY += itemHeight
        } else {
            cliente.vendas.forEachIndexed { index, venda ->
                val rowTop = currentY
                val rowBottom = currentY + itemHeight - 6f

                // Fundo alternado suave
                if (index % 2 == 1) {
                    rowBgPaint.color = Color.parseColor("#FAFAFA")
                    canvas.drawRoundRect(RectF(paddingX - 10f, rowTop - 4f, cardWidth - paddingX + 10f, rowBottom + 2f), 10f, 10f, rowBgPaint)
                }

                val valorTexto = formatador.format(venda.valorPeca)
                val valorWidth = itemValorPaint.measureText(valorTexto)
                val maxProdutoWidth = cardWidth - (paddingX * 2) - valorWidth - 24f

                // Texto do produto com mês
                val descricaoCompleta = "• ${venda.produto} (${venda.mesReferencia})"
                val descricaoTruncada = TextUtils.ellipsize(
                    descricaoCompleta,
                    itemTitlePaint,
                    maxProdutoWidth,
                    TextUtils.TruncateAt.END
                ).toString()

                canvas.drawText(descricaoTruncada, paddingX, currentY + 26f, itemTitlePaint)
                canvas.drawText(valorTexto, cardWidth - paddingX, currentY + 26f, itemValorPaint)

                currentY += itemHeight
            }
        }

        // --- 5. RODAPÉ ---
        currentY += 10f
        canvas.drawLine(paddingX, currentY, cardWidth - paddingX, currentY, dividerPaint)

        currentY += 30f
        val footerDatePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#9E9E9E")
            textSize = 21f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        canvas.drawText("Emitido em $dataHoraFormatada", paddingX, currentY, footerDatePaint)

        val footerThanksPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#5E35B1")
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("Agradecemos pela preferência! ❤️", cardWidth - paddingX, currentY, footerThanksPaint)

        return bitmap
    }

    private fun desenharBoxResumo(
        canvas: Canvas,
        left: Float,
        top: Float,
        width: Float,
        height: Float,
        radius: Float,
        bgColor: Int,
        borderColor: Int,
        label: String,
        labelColor: Int,
        value: String,
        valueColor: Int
    ) {
        val rect = RectF(left, top, left + width, top + height)

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = bgColor
        }
        canvas.drawRoundRect(rect, radius, radius, bgPaint)

        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = borderColor
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawRoundRect(rect, radius, radius, strokePaint)

        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = labelColor
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        canvas.drawText(label, left + 14f, top + 34f, labelPaint)

        val valuePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = valueColor
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val valorTruncado = TextUtils.ellipsize(
            value,
            valuePaint,
            width - 24f,
            TextUtils.TruncateAt.END
        ).toString()
        canvas.drawText(valorTruncado, left + 14f, top + 74f, valuePaint)
    }

    /**
     * Salva o bitmap gerado no cache compartilhado e retorna o content URI seguro.
     */
    fun salvarBitmapNoCache(context: Context, bitmap: Bitmap, clienteNome: String): Uri {
        val exportDir = File(context.cacheDir, "export").apply { if (!exists()) mkdirs() }
        val safeName = clienteNome.replace(Regex("[^a-zA-Z0-9_]"), "_").lowercase(Locale.ROOT)
        val file = File(exportDir, "card_${safeName}.png")

        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }

        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    /**
     * Executa a geração em background e abre o compartilhamento no WhatsApp (ou seletor).
     */
    suspend fun gerarECompartilharCard(context: Context, cliente: ClienteResumo) {
        try {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Gerando imagem do card...", Toast.LENGTH_SHORT).show()
            }

            val imageUri = withContext(Dispatchers.Default) {
                val bitmap = gerarCardBitmap(cliente)
                salvarBitmapNoCache(context, bitmap, cliente.nome)
            }

            withContext(Dispatchers.Main) {
                dispararIntentCompartilhamento(context, imageUri)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Erro ao gerar imagem: ${e.localizedMessage ?: "desconhecido"}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun dispararIntentCompartilhamento(context: Context, imageUri: Uri) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, imageUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            `package` = "com.whatsapp"
        }

        try {
            if (shareIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(shareIntent)
            } else {
                // Tenta WhatsApp Business
                shareIntent.`package` = "com.whatsapp.w4b"
                if (shareIntent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(shareIntent)
                } else {
                    // Fallback para Seletor de Compartilhamento Padrão do Android
                    shareIntent.`package` = null
                    val chooser = Intent.createChooser(shareIntent, "Compartilhar card do cliente")
                    chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(chooser)
                }
            }
        } catch (_: Exception) {
            val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, imageUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(fallbackIntent, "Compartilhar card do cliente")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        }
    }
}
