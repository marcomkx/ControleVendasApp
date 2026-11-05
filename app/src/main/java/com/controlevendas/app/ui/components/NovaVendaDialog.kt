package com.controlevendas.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.controlevendas.app.data.model.Venda
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NovaVendaDialog(
    vendaEditando: Venda? = null,
    mesAtual: String = "",
    sugestoesClientes: List<String> = emptyList(),
    sugestoesProdutos: List<String> = emptyList(),
    sugestoesMeses: List<String> = emptyList(),
    onDismiss: () -> Unit,
    onSalvar: (Venda) -> Unit
) {
    // Calcula automaticamente o mês e ano atual do celular (ex: "NOVEMBRO/2026")
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
    var produto by remember { mutableStateOf(vendaEditando?.produto ?: "") }
    var valorPecaStr by remember { mutableStateOf(vendaEditando?.valorPeca?.toString() ?: "") }
    var valorPagoStr by remember { mutableStateOf(vendaEditando?.valorPago?.toString() ?: "") }
    var margemStr by remember { mutableStateOf(vendaEditando?.margemLucroPercentual?.toString() ?: "30") }
    var mesReferencia by remember { mutableStateOf(mesInicial) }

    // Estados para controle de exibição do autocomplete
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

    val sugestoesProdutosFiltradas = remember(produto, sugestoesProdutos) {
        if (produto.isBlank()) {
            sugestoesProdutos.take(6)
        } else {
            sugestoesProdutos.filter { it.contains(produto, ignoreCase = true) && !it.equals(produto, ignoreCase = true) }.take(6)
        }
    }

    // Lista de sugestões de mês: inclui o mês atual do celular + meses que já existem no banco
    val listaSugestoesMeses = remember(sugestoesMeses, mesAtualCelular) {
        val set = linkedSetOf(mesAtualCelular)
        set.addAll(sugestoesMeses)
        set.toList()
    }

    val valorPeca = valorPecaStr.toDoubleOrNull() ?: 0.0
    val valorPago = valorPagoStr.toDoubleOrNull() ?: 0.0
    val margem = margemStr.toDoubleOrNull() ?: 30.0

    val lucro = valorPeca * (margem / 100.0)
    val fornecedor = (valorPeca - lucro).coerceAtLeast(0.0)
    val saldoDevedor = (valorPeca - valorPago).coerceAtLeast(0.0)

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

                // Campo Produto com Auto-completar
                ExposedDropdownMenuBox(
                    expanded = expandedProduto && sugestoesProdutosFiltradas.isNotEmpty(),
                    onExpandedChange = { expandedProduto = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = produto,
                        onValueChange = {
                            produto = it
                            expandedProduto = true
                        },
                        label = { Text("Produto/Peça * (ex: sutiã, calça)") },
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
                                    produto = sugestao
                                    expandedProduto = false
                                }
                            )
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = valorPecaStr,
                        onValueChange = { valorPecaStr = it },
                        label = { Text("Valor Total (R$) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = valorPagoStr,
                        onValueChange = { valorPagoStr = it },
                        label = { Text("Valor Já Pago (R$)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = margemStr,
                        onValueChange = { margemStr = it },
                        label = { Text("Lucro %") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(0.7f),
                        singleLine = true
                    )

                    // Campo Mês com Auto-completar e detecção automática
                    ExposedDropdownMenuBox(
                        expanded = expandedMes,
                        onExpandedChange = { expandedMes = it },
                        modifier = Modifier.weight(1.3f)
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

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Resumo Calculado:", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                        Text("💰 Seu Lucro (${margem.toInt()}%): ${formatador.format(lucro)}")
                        Text("🏭 Pagar ao Fornecedor: ${formatador.format(fornecedor)}")
                        Text(
                            text = if (saldoDevedor <= 0.001) "✅ Quitado (Sem saldo devedor)" else "⏳ Saldo Devedor: ${formatador.format(saldoDevedor)}",
                            fontWeight = FontWeight.Bold,
                            color = if (saldoDevedor <= 0.001) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (cliente.isNotBlank() && produto.isNotBlank() && valorPeca > 0) {
                        val venda = (vendaEditando ?: Venda(
                            cliente = cliente.trim(),
                            produto = produto.trim(),
                            valorPeca = valorPeca
                        )).copy(
                            cliente = cliente.trim(),
                            produto = produto.trim(),
                            valorPeca = valorPeca,
                            valorPago = valorPago,
                            saldoDevedor = saldoDevedor,
                            margemLucroPercentual = margem,
                            lucro = lucro,
                            mesReferencia = mesReferencia.trim().uppercase(),
                            status = if (saldoDevedor <= 0.001) "Quitado" else "Pendente"
                        )
                        onSalvar(venda)
                    }
                },
                enabled = cliente.isNotBlank() && produto.isNotBlank() && valorPeca > 0
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
