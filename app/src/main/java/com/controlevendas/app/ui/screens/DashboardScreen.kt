package com.controlevendas.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.controlevendas.app.ui.viewmodel.ResumoMes
import com.controlevendas.app.ui.viewmodel.VendasViewModel
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.min

@Composable
fun DashboardScreen(viewModel: VendasViewModel) {
    val metricas by viewModel.metricas.collectAsState()
    val mesesDisponiveis by viewModel.mesesDisponiveis.collectAsState()
    val filtroMes by viewModel.filtroMes.collectAsState()
    val vendasPorMes by viewModel.vendasPorMes.collectAsState()
    val formatador = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Resumo Geral de Valores",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            // Seletor de Meses
            val scrollState = rememberScrollState()
            val chipColors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primary,
                selectedLabelColor = Color.White
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .horizontalScroll(scrollState),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                mesesDisponiveis.forEach { mes ->
                    FilterChip(
                        selected = filtroMes == mes,
                        onClick = { viewModel.setFiltroMes(mes) },
                        label = { Text(mes) },
                        colors = chipColors
                    )
                }
                FilterChip(
                    selected = filtroMes == "Todos",
                    onClick = { viewModel.setFiltroMes("Todos") },
                    label = { Text("📅 Todos os Meses") },
                    colors = chipColors
                )
            }
        }

        // Cards de métricas principais
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricaCard(
                        titulo = "💰 Lucro Total",
                        valor = formatador.format(metricas.lucroTotal),
                        icone = Icons.Default.AttachMoney,
                        containerColor = Color(0xFFFCE4EC),
                        contentColor = Color(0xFFC2185B),
                        modifier = Modifier.weight(1f)
                    )
                    MetricaCard(
                        titulo = "⏳ Total Vendido",
                        valor = formatador.format(metricas.totalVendido),
                        icone = Icons.Default.Inventory2,
                        containerColor = Color(0xFFF8BBD0),
                        contentColor = Color(0xFF880E4F),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricaCard(
                        titulo = "🏭 p/ Fornecedor",
                        valor = formatador.format(metricas.totalFornecedor),
                        icone = Icons.Default.LocalShipping,
                        containerColor = Color(0xFFEDE7F6),
                        contentColor = Color(0xFF4A148C),
                        modifier = Modifier.weight(1f)
                    )
                    MetricaCard(
                        titulo = "⚠️ A Receber (Pendente)",
                        valor = formatador.format(metricas.totalPendente),
                        icone = Icons.Default.HourglassTop,
                        containerColor = Color(0xFFFFF3E0),
                        contentColor = Color(0xFFE65100),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricaCard(
                        titulo = "📋 Total de Peças",
                        valor = "${metricas.totalPecas} un.",
                        icone = Icons.Default.Inventory2,
                        containerColor = Color(0xFFF5F5F5),
                        contentColor = Color(0xFF333333),
                        modifier = Modifier.weight(1f)
                    )
                    MetricaCard(
                        titulo = "✅ Total Já Pago",
                        valor = formatador.format(metricas.totalPago),
                        icone = Icons.Default.CheckCircle,
                        containerColor = Color(0xFFE0F2F1),
                        contentColor = Color(0xFF004D40),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Gráfico 1: Donut - Vendido vs Recebido
        item {
            val totalVendido = metricas.totalVendido
            val totalPago = metricas.totalPago
            val totalPendente = metricas.totalPendente

            if (totalVendido > 0) {
                GraficoDonutVendidoRecebido(
                    totalVendido = totalVendido,
                    totalPago = totalPago,
                    totalPendente = totalPendente,
                    formatador = formatador
                )
            }
        }

        // Gráfico 2: Comparação Mês Passado vs Mês Atual
        if (vendasPorMes.isNotEmpty()) {
            item {
                GraficoComparacaoMeses(
                    vendasPorMes = vendasPorMes,
                    formatador = formatador
                )
            }
        }

        // Dica de Gestão
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("💡 Dica de Gestão:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(
                        "Para consultar ou registrar recebimentos individuais, acerte os pagamentos na aba 'Vendas' ou verifique o saldo consolidado devedor na aba 'Clientes'.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

// ─── Gráfico 1: Barra horizontal do último mês ───────────────────────────────

// ─── Gráfico 1: Comparação Mês Passado vs Mês Atual ───────────────────────────

@Composable
fun GraficoComparacaoMeses(
    vendasPorMes: List<ResumoMes>,
    formatador: NumberFormat
) {
    if (vendasPorMes.isEmpty()) return

    val mesAtual = vendasPorMes.getOrNull(0)
    val mesPassado = vendasPorMes.getOrNull(1)

    val surfaceColor = MaterialTheme.colorScheme.surfaceVariant
    val onSurface = MaterialTheme.colorScheme.onSurface
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(vendasPorMes) {
        animProgress.snapTo(0f)
        animProgress.animateTo(1f, animationSpec = tween(durationMillis = 1000))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceColor),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "📊 Comparação: Mês Passado vs Mês Atual",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = onSurface
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (mesPassado == null) {
                Text(
                    text = "Cadastre vendas em mais de um mês para visualizar a comparação entre o mês passado e o mês atual.",
                    fontSize = 12.sp,
                    color = onSurface.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (mesAtual != null) {
                    val nomeAtual = mesAtual.mes.substringBefore("/").trim().replaceFirstChar { it.uppercaseChar() }
                    val anoAtual = mesAtual.mes.substringAfter("/").trim()
                    Text(
                        text = "$nomeAtual/$anoAtual (Atual): ${formatador.format(mesAtual.totalVendido)}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = primaryColor
                    )
                }
            } else {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp)
                ) {
                    val barH = 30.dp.toPx()
                    val cornerR = 8.dp.toPx()
                    val labelPx = 95.dp.toPx()
                    val barArea = size.width - labelPx - 8.dp.toPx()
                    
                    val maxV = maxOf(mesAtual?.totalVendido ?: 0.0, mesPassado.totalVendido, 1.0)

                    val nomePassado = mesPassado.mes.substringBefore("/").trim().replaceFirstChar { it.uppercaseChar() } + "/" + mesPassado.mes.substringAfter("/").trim()
                    val nomeAtual = (mesAtual?.mes?.substringBefore("/")?.trim()?.replaceFirstChar { it.uppercaseChar() } ?: "") + "/" + (mesAtual?.mes?.substringAfter("/")?.trim() ?: "")

                    fun drawBarValue(
                        canvas: android.graphics.Canvas,
                        valor: Double,
                        w: Float,
                        y: Float,
                        barColor: Color
                    ) {
                        val textVal = formatador.format(valor)
                        val basePaint = android.graphics.Paint().apply {
                            textSize = 10.sp.toPx()
                            isAntiAlias = true
                            typeface = android.graphics.Typeface.DEFAULT_BOLD
                        }
                        val textWidth = basePaint.measureText(textVal)
                        val spaceRight = labelPx + barArea - (labelPx + w)

                        if (w > textWidth + 24.dp.toPx() && spaceRight > -textWidth) {
                            // Desenha dentro da barra em branco
                            canvas.drawText(
                                textVal,
                                labelPx + w - 8.dp.toPx(),
                                y + barH / 2f + 5.dp.toPx(),
                                android.graphics.Paint(basePaint).apply {
                                    color = android.graphics.Color.WHITE
                                    textAlign = android.graphics.Paint.Align.RIGHT
                                }
                            )
                        } else {
                            // Desenha fora se houver espaço, ou dentro se estiver muito no limite
                            if (spaceRight < textWidth + 8.dp.toPx()) {
                                canvas.drawText(
                                    textVal,
                                    labelPx + w - 8.dp.toPx(),
                                    y + barH / 2f + 5.dp.toPx(),
                                    android.graphics.Paint(basePaint).apply {
                                        color = android.graphics.Color.WHITE
                                        textAlign = android.graphics.Paint.Align.RIGHT
                                    }
                                )
                            } else {
                                canvas.drawText(
                                    textVal,
                                    labelPx + w + 6.dp.toPx(),
                                    y + barH / 2f + 5.dp.toPx(),
                                    android.graphics.Paint(basePaint).apply {
                                        color = barColor.toArgb()
                                        textAlign = android.graphics.Paint.Align.LEFT
                                    }
                                )
                            }
                        }
                    }

                    // Linha 1: Mês Passado
                    val y1 = 0f
                    drawContext.canvas.nativeCanvas.drawText(
                        nomePassado,
                        0f,
                        y1 + barH / 2f + 5.dp.toPx(),
                        android.graphics.Paint().apply {
                            color = onSurface.copy(alpha = 0.75f).toArgb()
                            textSize = 11.sp.toPx()
                            isAntiAlias = true
                            typeface = android.graphics.Typeface.DEFAULT_BOLD
                        }
                    )
                    drawRoundRect(
                        color = secondaryColor.copy(alpha = 0.12f),
                        topLeft = Offset(labelPx, y1),
                        size = Size(barArea, barH),
                        cornerRadius = CornerRadius(cornerR)
                    )
                    val ratio1 = (mesPassado.totalVendido / maxV).coerceIn(0.0, 1.0)
                    val w1 = (barArea * ratio1 * animProgress.value).toFloat()
                    if (w1 > cornerR) {
                        drawRoundRect(
                            color = secondaryColor.copy(alpha = 0.85f),
                            topLeft = Offset(labelPx, y1),
                            size = Size(w1, barH),
                            cornerRadius = CornerRadius(cornerR)
                        )
                    }
                    drawBarValue(drawContext.canvas.nativeCanvas, mesPassado.totalVendido, w1, y1, secondaryColor)

                    // Linha 2: Mês Atual
                    val y2 = barH + 20.dp.toPx()
                    val ratio2 = if (mesAtual != null) (mesAtual.totalVendido / maxV).coerceIn(0.0, 1.0) else 0.0

                    drawContext.canvas.nativeCanvas.drawText(
                        nomeAtual,
                        0f,
                        y2 + barH / 2f + 5.dp.toPx(),
                        android.graphics.Paint().apply {
                            color = onSurface.copy(alpha = 0.75f).toArgb()
                            textSize = 11.sp.toPx()
                            isAntiAlias = true
                            typeface = android.graphics.Typeface.DEFAULT_BOLD
                        }
                    )
                    drawRoundRect(
                        color = primaryColor.copy(alpha = 0.12f),
                        topLeft = Offset(labelPx, y2),
                        size = Size(barArea, barH),
                        cornerRadius = CornerRadius(cornerR)
                    )
                    val w2 = (barArea * ratio2 * animProgress.value).toFloat()
                    if (w2 > cornerR) {
                        drawRoundRect(
                            color = primaryColor.copy(alpha = 0.85f),
                            topLeft = Offset(labelPx, y2),
                            size = Size(w2, barH),
                            cornerRadius = CornerRadius(cornerR)
                        )
                    }
                    drawBarValue(drawContext.canvas.nativeCanvas, mesAtual?.totalVendido ?: 0.0, w2, y2, primaryColor)
                }
            }
        }
    }
}

// ─── Gráfico 2: Donut — Vendido vs Recebido ──────────────────────────────────

@Composable
fun GraficoDonutVendidoRecebido(
    totalVendido: Double,
    totalPago: Double,
    totalPendente: Double,
    formatador: NumberFormat
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surfaceVariant
    val onSurface = MaterialTheme.colorScheme.onSurface

    val colorPago = Color(0xFF00897B)
    val colorPendente = Color(0xFFE65100)
    val colorTrack = primaryColor.copy(alpha = 0.1f)

    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(totalVendido, totalPago) {
        animProgress.snapTo(0f)
        animProgress.animateTo(1f, animationSpec = tween(durationMillis = 1200))
    }

    val percPago = if (totalVendido > 0) (totalPago / totalVendido).coerceIn(0.0, 1.0) else 0.0
    val percPendente = 1.0 - percPago

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceColor),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "🍩 Vendas vs Recebimentos",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = onSurface
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Donut Canvas
                Box(
                    modifier = Modifier.size(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeW = 28.dp.toPx()
                        val radius = (min(size.width, size.height) - strokeW) / 2f
                        val topLeft = Offset(
                            (size.width - radius * 2) / 2f,
                            (size.height - radius * 2) / 2f
                        )
                        val arcSize = Size(radius * 2, radius * 2)

                        // Track (fundo)
                        drawArc(
                            color = colorTrack,
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeW, cap = StrokeCap.Round)
                        )

                        // Arco: Recebido (verde)
                        val sweepPago = (360f * percPago * animProgress.value).toFloat()
                        if (sweepPago > 0.5f) {
                            drawArc(
                                color = colorPago,
                                startAngle = -90f,
                                sweepAngle = sweepPago,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = strokeW, cap = StrokeCap.Round)
                            )
                        }

                        // Arco: Pendente (laranja) — começa onde o verde termina
                        val sweepPend = (360f * percPendente * animProgress.value).toFloat()
                        if (sweepPend > 0.5f) {
                            drawArc(
                                color = colorPendente,
                                startAngle = -90f + sweepPago,
                                sweepAngle = sweepPend,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = strokeW, cap = StrokeCap.Round)
                            )
                        }
                    }

                    // Percentual no centro
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${(percPago * 100).toInt()}%",
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = colorPago
                        )
                        Text(
                            text = "recebido",
                            fontSize = 11.sp,
                            color = onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                // Legenda
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    LegendaItem(
                        cor = colorPago,
                        label = "✅ Recebido",
                        valor = formatador.format(totalPago),
                        percentual = "${(percPago * 100).toInt()}%"
                    )
                    LegendaItem(
                        cor = colorPendente,
                        label = "⚠️ Pendente",
                        valor = formatador.format(totalPendente),
                        percentual = "${(percPendente * 100).toInt()}%"
                    )
                    HorizontalDivider(color = onSurface.copy(alpha = 0.1f))
                    LegendaItem(
                        cor = primaryColor,
                        label = "🛍️ Total Vendido",
                        valor = formatador.format(totalVendido),
                        percentual = "100%"
                    )
                }
            }
        }
    }
}

@Composable
fun LegendaItem(
    cor: Color,
    label: String,
    valor: String,
    percentual: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Canvas(modifier = Modifier.size(12.dp)) {
            drawCircle(color = cor)
        }
        Column {
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(valor, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = cor)
                Text("($percentual)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
        }
    }
}

// ─── Card de Métrica ─────────────────────────────────────────────────────────

@Composable
fun MetricaCard(
    titulo: String,
    valor: String,
    icone: ImageVector,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(titulo, fontSize = 12.sp, color = contentColor, fontWeight = FontWeight.Medium)
                Icon(icone, contentDescription = null, tint = contentColor, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(valor, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = contentColor)
        }
    }
}
