package com.controlevendas.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
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
import com.controlevendas.app.ui.components.SeletorMesesBar
import com.controlevendas.app.ui.viewmodel.ClienteResumo
import com.controlevendas.app.ui.viewmodel.VendasViewModel
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientesScreen(viewModel: VendasViewModel) {
    val clientes by viewModel.clientesAgrupados.collectAsState()
    val pesquisa by viewModel.pesquisa.collectAsState()
    val apenasPendentes by viewModel.filtroClientesApenasPendentes.collectAsState()
    val filtroMesCliente by viewModel.filtroClientesMes.collectAsState()
    val mesesDisponiveis by viewModel.mesesDisponiveis.collectAsState()

    var clienteParaExcluir by remember { mutableStateOf<ClienteResumo?>(null) }

    val context = LocalContext.current
    val formatador = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

    val totalDevedorGeral = clientes.sumOf { it.saldoDevedor }
    val clientesComDivida = clientes.count { it.saldoDevedor > 0.001 }

    Column(modifier = Modifier.fillMaxSize()) {
        // Barra de busca por cliente
        OutlinedTextField(
            value = pesquisa,
            onValueChange = { viewModel.setPesquisa(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            placeholder = { Text("Buscar cliente pelo nome...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Buscar") },
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

        // Seletor de Meses para Clientes
        SeletorMesesBar(
            meses = mesesDisponiveis,
            mesSelecionado = filtroMesCliente,
            onSelectMes = { viewModel.setFiltroClientesMes(it) }
        )

        // Filtro rápido de Pendentes / Todos
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = !apenasPendentes,
                onClick = { if (apenasPendentes) viewModel.toggleFiltroClientesApenasPendentes() },
                label = { Text("Todos (${clientes.size})") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = Color.White
                )
            )
            FilterChip(
                selected = apenasPendentes,
                onClick = { if (!apenasPendentes) viewModel.toggleFiltroClientesApenasPendentes() },
                label = { Text("⚠️ Só com Débito ($clientesComDivida)") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFFC2185B),
                    selectedLabelColor = Color.White
                )
            )
        }

        // Card resumo de saldo pendente
        if (totalDevedorGeral > 0.001) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFCE4EC)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Total a receber destes clientes:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF880E4F)
                    )
                    Text(
                        text = formatador.format(totalDevedorGeral),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFC2185B)
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (clientes.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (apenasPendentes) "Nenhum cliente com débito pendente! 🎉" else "Nenhum cliente encontrado.",
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else {
                items(clientes, key = { it.nome }) { clienteResumo ->
                    ClienteCard(
                        cliente = clienteResumo,
                        formatador = formatador,
                        onCobrarTudo = {
                            val pecas = clienteResumo.vendas.filter { !it.isQuitado }.map { "${it.produto} (${formatador.format(it.saldoDevedor)})" }
                            viewModel.cobrarWhatsApp(
                                context = context,
                                cliente = clienteResumo.nome,
                                valorPendente = clienteResumo.saldoDevedor,
                                itens = pecas
                            )
                        },
                        onExcluir = { clienteParaExcluir = clienteResumo }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    if (clienteParaExcluir != null) {
        AlertDialog(
            onDismissRequest = { clienteParaExcluir = null },
            title = { Text("Excluir Cliente") },
            text = {
                Text("Deseja realmente excluir o cliente \"${clienteParaExcluir!!.nome}\"?\n\nTodas as ${clienteParaExcluir!!.vendas.size} venda(s) associadas a este cliente serão removidas permanentemente.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.excluirCliente(clienteParaExcluir!!.nome)
                        clienteParaExcluir = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Excluir", color = MaterialTheme.colorScheme.onError)
                }
            },
            dismissButton = {
                TextButton(onClick = { clienteParaExcluir = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun ClienteCard(
    cliente: ClienteResumo,
    formatador: NumberFormat,
    onCobrarTudo: () -> Unit,
    onExcluir: () -> Unit
) {
    var expandido by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expandido = !expandido },
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
                    Text(cliente.nome, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Text("${cliente.totalPecas} peças • Total comprado: ${formatador.format(cliente.totalComprado)}", color = Color.Gray, fontSize = 13.sp)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (cliente.saldoDevedor > 0.001) {
                        Button(
                            onClick = onCobrarTudo,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Cobrar", color = Color.White, fontSize = 12.sp)
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFFE8F5E9)
                        ) {
                            Text(
                                text = "Sem débitos",
                                color = Color(0xFF2E7D32),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onExcluir,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = "Excluir Cliente",
                            tint = Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = if (cliente.saldoDevedor > 0.001) "Deve: ${formatador.format(cliente.saldoDevedor)}" else "100% Quitado",
                    color = if (cliente.saldoDevedor > 0.001) MaterialTheme.colorScheme.error else Color(0xFF2E7D32),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (expandido) "Ocultar detalhes" else "Ver detalhes (${cliente.vendas.size})", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    Icon(
                        imageVector = if (expandido) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (expandido) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    cliente.vendas.forEach { venda ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("• ${venda.produto} (${venda.mesReferencia})", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                            Text(
                                text = if (venda.isQuitado) "Quitado" else "Falta ${formatador.format(venda.saldoDevedor)}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (venda.isQuitado) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}
