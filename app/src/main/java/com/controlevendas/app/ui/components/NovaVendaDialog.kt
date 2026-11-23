package com.controlevendas.app.ui.components

import androidx.compose.foundation.clickable
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
    mesAtual: String = "",
    sugestoesClientes: List<String> = emptyList(),
    sugestoesProdutos: List<String> = emptyList(),
    sugestoesMeses: List<String> = emptyList(),
    onDismiss: () -> Unit,
    onSalvar: (List<Venda>) -> Unit
) {
    val mesAtualCelular = remember {
        val sdf = SimpleDateFormat("MMMM/yyyy", Locale("pt", "BR"))
        sdf.format(Date()).uppercase()
    }

    val mesInicial = when {
        vendaEditando != null -> vendaEditando.mesReferencia
        mesAtual.isNotBlank() && mesAtual != "Todos" -> mesAtual
        else -> mesAtualCelular
    }

    var cliente by remember { mutableStateOf(vendaEditando?.cliente ?: "") }
    var mesReferencia by remember { mutableStateOf(mesInicial) }
    var valorPagoStr by remember {
        mutableStateOf(
            vendaEditando?.valorPago?.let { if (it > 0) String.format(Locale("pt", "BR"), "%.2f", it).replace(".", ",") else "" } ?: ""
        )
    }

    // Current item inputs
    var produtoAtual by remember { mutableStateOf(vendaEditando?.produto ?: "") }
    var quantidadeAtual by remember { mutableIntStateOf(1) }
    var valorUnitarioStr by remember {
        mutableStateOf(
            vendaEditando?.valorPeca?.let { if (it > 0) String.format(Locale("pt", "BR"), "%.2f", it).replace(".", ",") else "" } ?: ""
        )
    }

    // Cart of items
    var itensCarrinho by remember {
        mutableStateOf(
            if (vendaEditando != null) listOf(ItemVendaTemp(vendaEditando.produto, 1, vendaEditando.valorPeca))
            else emptyList()
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

    val valorTotalGeral = itensCarrinho.sumOf { it.valorTotal }
    val valorPago = valorPagoStr.replace(",", ".").toDoubleOrNull() ?: 0.0
    val margem = 30.0



    val formatador = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (vendaEditando == null) "Nova Venda" else "Editar Venda",
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

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Text("Adicionar Produtos:", fontWeight = FontWeight.Bold, fontSize = 14.sp)

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
                        label = { Text("Produto * (ex: sutiã, calça)") },
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
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Quantidade:", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { if (quantidadeAtual > 1) quantidadeAtual-- }) {
                            Icon(Icons.Default.Remove, contentDescription = "Menos")
                        }
                        Text(
                            text = "$quantidadeAtual",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        IconButton(onClick = { quantidadeAtual++ }) {
                            Icon(Icons.Default.Add, contentDescription = "Mais")
                        }
                    }
                }

                OutlinedTextField(
                    value = valorUnitarioStr,
                    onValueChange = { valorUnitarioStr = it },
                    label = { Text("Valor Unitário (R$) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Button(
                    onClick = {
                        val valUnit = valorUnitarioStr.replace(",", ".").toDoubleOrNull() ?: 0.0
                        if (produtoAtual.isNotBlank() && valUnit > 0 && quantidadeAtual > 0) {
                            val nomeProd = if (quantidadeAtual > 1) "${produtoAtual.trim()} (x$quantidadeAtual)" else produtoAtual.trim()
                            itensCarrinho = itensCarrinho + ItemVendaTemp(nomeProd, quantidadeAtual, valUnit)
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
                            (vendaEditando ?: Venda(
                                cliente = cliente.trim(),
                                produto = item.produto,
                                valorPeca = vTotal
                            )).copy(
                                cliente = cliente.trim(),
                                produto = item.produto,
                                valorPeca = vTotal,
                                valorPago = vPagoItem,
                                saldoDevedor = sDevedor,
                                margemLucroPercentual = margem,
                                lucro = vTotal * (margem / 100.0),
                                mesReferencia = mesReferencia.trim().uppercase(),
                                status = if (sDevedor <= 0.001) "Quitado" else "Pendente"
                            )
                        }
                        onSalvar(vendasASalvar)
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
