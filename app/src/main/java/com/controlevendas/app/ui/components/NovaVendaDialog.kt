package com.controlevendas.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.controlevendas.app.data.model.Venda
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ItemVendaTemp(
    val produto: String,
    val quantidade: Int,
    val valorUnitario: Double
) {
    val valorTotal: Double get() = quantidade * valorUnitario
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NovaVendaDialog(
    vendaEditando: Venda? = null,
    vendasParaEditar: List<Venda> = emptyList(),
    mesAtual: String = "",
    sugestoesClientes: List<String> = emptyList(),
    sugestoesProdutos: List<String> = emptyList(),
    sugestoesMeses: List<String> = emptyList(),
    onDismiss: () -> Unit,
    onSalvar: (List<Venda>, List<Venda>) -> Unit
) {
    val loteInicial = when {
        vendasParaEditar.isNotEmpty() -> vendasParaEditar
        vendaEditando != null -> listOf(vendaEditando)
        else -> emptyList()
    }

    val mesAtualCelular = remember {
        val sdf = SimpleDateFormat("MMMM/yyyy", Locale("pt", "BR"))
        sdf.format(Date()).uppercase()
    }

    val mesInicial = when {
        loteInicial.isNotEmpty() -> loteInicial.first().mesReferencia
        mesAtual.isNotBlank() && mesAtual != "Todos" -> mesAtual
        else -> mesAtualCelular
    }

    var cliente by remember { mutableStateOf(loteInicial.firstOrNull()?.cliente ?: "") }
    var mesReferencia by remember { mutableStateOf(mesInicial) }

    val totalPagoInicial = loteInicial.sumOf { it.valorPago }
    var valorPagoStr by remember {
        mutableStateOf(if (totalPagoInicial > 0) String.format(Locale("pt", "BR"), "%.2f", totalPagoInicial).replace(".", ",") else "")
    }

    // Current item inputs
    var produtoAtual by remember { mutableStateOf("") }
    var quantidadeAtual by remember { mutableIntStateOf(1) }
    var valorUnitarioStr by remember { mutableStateOf("") }

    // Cart of items
    var itensCarrinho by remember {
        mutableStateOf<List<ItemVendaTemp>>(
            loteInicial.map { ItemVendaTemp(it.produto, 1, it.valorPeca) }
        )
    }

    var expandedCliente by remember { mutableStateOf(false) }
    var expandedProduto by remember { mutableStateOf(false) }
    var expandedMes by remember { mutableStateOf(false) }

    val sugestoesClientesFiltradas = remember(cliente, sugestoesClientes) {
        if (cliente.isBlank()) {
            sugestoesClientes.take(6)
        } else {
            sugestoesClientes.filter { it.contains(cliente, ignoreCase = true) && !it.equals(cliente, ignoreCase = true) }.take(6)
        }
    }

    val sugestoesProdutosFiltradas = remember(produtoAtual, sugestoesProdutos) {
        if (produtoAtual.isBlank()) {
            sugestoesProdutos.take(6)
        } else {
            sugestoesProdutos.filter { it.contains(produtoAtual, ignoreCase = true) && !it.equals(produtoAtual, ignoreCase = true) }.take(6)
        }
    }

    val listaSugestoesMeses = remember(sugestoesMeses, mesAtualCelular) {
        val set = linkedSetOf(mesAtualCelular)
        set.addAll(sugestoesMeses)
        set.toList()
    }

    val valorPago = valorPagoStr.replace(",", ".").toDoubleOrNull() ?: 0.0
    val margem = 30.0

    val formatador = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (loteInicial.isEmpty()) "Nova Venda" else "Editar Venda",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Campo Cliente com Auto-completar
                ExposedDropdownMenuBox(
                    expanded = expandedCliente && sugestoesClientesFiltradas.isNotEmpty(),
                    onExpandedChange = { expandedCliente = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = cliente,
                        onValueChange = {
                            cliente = it
                            expandedCliente = true
                        },
                        label = { Text("Nome do Cliente *") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        singleLine = true,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCliente && sugestoesClientesFiltradas.isNotEmpty())
                        }
                    )

                    ExposedDropdownMenu(
                        expanded = expandedCliente && sugestoesClientesFiltradas.isNotEmpty(),
                        onDismissRequest = { expandedCliente = false }
                    ) {
                        sugestoesClientesFiltradas.forEach { sugestao ->
                            DropdownMenuItem(
                                text = { Text(sugestao) },
                                onClick = {
                                    cliente = sugestao
                                    expandedCliente = false
                                }
                            )
                        }
                    }
                }

                // Campo Produto com Auto-completar
                ExposedDropdownMenuBox(
                    expanded = expandedProduto && sugestoesProdutosFiltradas.isNotEmpty(),
                    onExpandedChange = { expandedProduto = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = produtoAtual,
                        onValueChange = {
                            produtoAtual = it
                            expandedProduto = true
                        },
                        label = { Text("Produto * (ex: sutiã)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        singleLine = true,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedProduto && sugestoesProdutosFiltradas.isNotEmpty())
                        }
                    )

                    ExposedDropdownMenu(
                        expanded = expandedProduto && sugestoesProdutosFiltradas.isNotEmpty(),
                        onDismissRequest = { expandedProduto = false }
                    ) {
                        sugestoesProdutosFiltradas.forEach { sugestao ->
                            DropdownMenuItem(
                                text = { Text(sugestao) },
                                onClick = {
                                    produtoAtual = sugestao
                                    expandedProduto = false
                                }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = valorUnitarioStr,
                        onValueChange = { valorUnitarioStr = it },
                        label = { Text("Valor R$ *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1.1f),
                        singleLine = true
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(0.9f)
                    ) {
                        Text("Qtd:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            IconButton(
                                onClick = { if (quantidadeAtual > 1) quantidadeAtual-- },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Menos", modifier = Modifier.size(16.dp))
                            }
                            Text(
                                text = "$quantidadeAtual",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                            IconButton(
                                onClick = { quantidadeAtual++ },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Mais", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                Button(
                    onClick = {
                        val valUnit = valorUnitarioStr.replace(",", ".").toDoubleOrNull() ?: 0.0
                        if (produtoAtual.isNotBlank() && valUnit > 0 && quantidadeAtual > 0) {
                            val novosItens = (1..quantidadeAtual).map {
                                ItemVendaTemp(produtoAtual.trim(), 1, valUnit)
                            }
                            itensCarrinho = itensCarrinho + novosItens
                            produtoAtual = ""
                            quantidadeAtual = 1
                            valorUnitarioStr = ""
                        }
                    },
                    enabled = produtoAtual.isNotBlank() && (valorUnitarioStr.replace(",", ".").toDoubleOrNull() ?: 0.0) > 0,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Adicionar Produto à Lista")
                }

                if (itensCarrinho.isNotEmpty()) {
                    Text("Produtos na Venda (${itensCarrinho.size}):", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        itensCarrinho.forEachIndexed { index, item ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.produto, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                        Text("Total: ${formatador.format(item.valorTotal)}", fontSize = 12.sp, color = Color.Gray)
                                    }
                                    IconButton(
                                        onClick = {
                                            itensCarrinho = itensCarrinho.filterIndexed { i, _ -> i != index }
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Remover", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                OutlinedTextField(
                    value = valorPagoStr,
                    onValueChange = { valorPagoStr = it },
                    label = { Text("Valor Pago (R$)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Campo Mês com Auto-completar
                ExposedDropdownMenuBox(
                    expanded = expandedMes,
                    onExpandedChange = { expandedMes = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = mesReferencia,
                        onValueChange = {
                            mesReferencia = it
                            expandedMes = true
                        },
                        label = { Text("Mês/Ano") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        singleLine = true,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedMes)
                        }
                    )

                    ExposedDropdownMenu(
                        expanded = expandedMes,
                        onDismissRequest = { expandedMes = false }
                    ) {
                        listaSugestoesMeses.forEach { mesSugestao ->
                            DropdownMenuItem(
                                text = {
                                    Text(if (mesSugestao == mesAtualCelular) "$mesSugestao (Atual 📅)" else mesSugestao)
                                },
                                onClick = {
                                    mesReferencia = mesSugestao
                                    expandedMes = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (cliente.isNotBlank() && itensCarrinho.isNotEmpty()) {
                        var pagoRestante = valorPago
                        val vendasASalvar = itensCarrinho.map { item ->
                            val vTotal = item.valorTotal
                            val vPagoItem = if (pagoRestante >= vTotal) {
                                pagoRestante -= vTotal
                                vTotal
                            } else {
                                val p = pagoRestante
                                pagoRestante = 0.0
                                p
                            }
                            val sDevedor = (vTotal - vPagoItem).coerceAtLeast(0.0)
                            Venda(
                                id = 0L,
                                cliente = cliente.trim(),
                                produto = item.produto,
                                valorPeca = vTotal,
                                valorPago = vPagoItem,
                                saldoDevedor = sDevedor,
                                margemLucroPercentual = margem,
                                lucro = vTotal * (margem / 100.0),
                                mesReferencia = mesReferencia.trim().uppercase(),
                                status = if (sDevedor <= 0.001) "Quitado" else "Pendente",
                                data = loteInicial.firstOrNull()?.data?.ifBlank { "" } ?: ""
                            )
                        }
                        onSalvar(loteInicial, vendasASalvar)
                    }
                },
                enabled = cliente.isNotBlank() && itensCarrinho.isNotEmpty()
            ) {
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
