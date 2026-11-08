package com.controlevendas.app.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.controlevendas.app.data.model.Venda
import com.controlevendas.app.ui.viewmodel.VendasViewModel
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendasListScreen(
    viewModel: VendasViewModel,
    onEditarVenda: (Venda) -> Unit,
    onReceberPagamento: (Venda) -> Unit
) {
    val vendas by viewModel.vendasFiltradas.collectAsState()
    val pesquisa by viewModel.pesquisa.collectAsState()
    val filtroStatus by viewModel.filtroStatus.collectAsState()
    val filtroMes by viewModel.filtroMes.collectAsState()
    val mesesDisponiveis by viewModel.mesesDisponiveis.collectAsState()
    val context = LocalContext.current
    val formatador = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

    var vendaParaExcluir by remember { mutableStateOf<Venda?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Barra de busca
        OutlinedTextField(
            value = pesquisa,
            onValueChange = { viewModel.setPesquisa(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text("Buscar por cliente ou peça...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Pesquisar") },
            trailingIcon = {
                if (pesquisa.isNotEmpty()) {
                    IconButton(onClick = { viewModel.setPesquisa("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Limpar")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        // Filtro de Mês e Filtro de Status
        val scrollState = rememberScrollState()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val chipColors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primary,
                selectedLabelColor = Color.White
            )

            FilterChip(
                selected = filtroStatus == "Todos",
                onClick = { viewModel.setFiltroStatus("Todos") },
                label = { Text("Todos") },
                colors = chipColors
            )
            FilterChip(
                selected = filtroStatus == "Pendente",
                onClick = { viewModel.setFiltroStatus("Pendente") },
                label = { Text("⏳ Pendentes") },
                colors = chipColors
            )
            FilterChip(
                selected = filtroStatus == "Quitado",
                onClick = { viewModel.setFiltroStatus("Quitado") },
                label = { Text("✅ Quitados") },
                colors = chipColors
            )

            VerticalDivider(modifier = Modifier.height(32.dp))

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

        // Contador
        Text(
            text = "${vendas.size} lançamentos encontrados",
            fontSize = 12.sp,
            color = Color.Gray,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        // Lista
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(vendas, key = { it.id }) { venda ->
                VendaCard(
                    venda = venda,
                    formatador = formatador,
                    onReceberPagamento = { onReceberPagamento(venda) },
                    onQuitar = { viewModel.quitarVenda(venda) },
                    onEditar = { onEditarVenda(venda) },
                    onExcluir = { vendaParaExcluir = venda },
                    onCobrar = {
                        viewModel.cobrarWhatsApp(
                            context = context,
                            cliente = venda.cliente,
                            valorPendente = venda.saldoDevedor,
                            itens = listOf(venda.produto)
                        )
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    if (vendaParaExcluir != null) {
        AlertDialog(
            onDismissRequest = { vendaParaExcluir = null },
            title = { Text("Excluir Lançamento") },
            text = { Text("Deseja realmente remover o registro de ${vendaParaExcluir?.produto} de ${vendaParaExcluir?.cliente}?") },
            confirmButton = {
                Button(
                    onClick = {
                        vendaParaExcluir?.let { viewModel.excluirVenda(it) }
                        vendaParaExcluir = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Excluir")
                }
            },
            dismissButton = {
                TextButton(onClick = { vendaParaExcluir = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun VendaCard(
    venda: Venda,
    formatador: NumberFormat,
    onReceberPagamento: () -> Unit,
    onQuitar: () -> Unit,
    onEditar: () -> Unit,
    onExcluir: () -> Unit,
    onCobrar: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(venda.cliente, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("${venda.produto} • ${venda.mesReferencia}", color = Color.Gray, fontSize = 13.sp)
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (venda.isQuitado) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
                ) {
                    Text(
                        text = if (venda.isQuitado) "✅ Quitado" else "⏳ Pendente",
                        color = if (venda.isQuitado) Color(0xFF2E7D32) else Color(0xFFE65100),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            HorizontalDivider()

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Valor Peça", fontSize = 11.sp, color = Color.Gray)
                    Text(formatador.format(venda.valorPeca), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
                Column {
                    Text("Já Pago", fontSize = 11.sp, color = Color.Gray)
                    Text(formatador.format(venda.valorPago), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
                Column {
                    Text("Saldo Devedor", fontSize = 11.sp, color = Color.Gray)
                    Text(
                        formatador.format(venda.saldoDevedor),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (venda.isQuitado) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                    )
                }
                Column {
                    Text("Seu Lucro (30%)", fontSize = 11.sp, color = Color.Gray)
                    Text(formatador.format(venda.lucro), fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color(0xFF1B5E20))
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onExcluir) {
                    Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = Color.Gray)
                }
                IconButton(onClick = onEditar) {
                    Icon(Icons.Default.Edit, contentDescription = "Editar", tint = MaterialTheme.colorScheme.primary)
                }

                if (!venda.isQuitado) {
                    IconButton(onClick = onCobrar) {
                        Icon(Icons.Default.Share, contentDescription = "Cobrar WhatsApp", tint = Color(0xFF25D366))
                    }
                    FilledTonalButton(onClick = onReceberPagamento) {
                        Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Baixa")
                    }
                }
            }
        }
    }
}
