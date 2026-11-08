package com.controlevendas.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GerenciarClientesProdutosDialog(
    clientes: List<String>,
    produtos: List<String>,
    onRenomearCliente: (String, String) -> Unit,
    onExcluirCliente: (String) -> Unit,
    onMesclarClientes: (String, String) -> Unit,
    onRenomearProduto: (String, String) -> Unit,
    onExcluirProduto: (String) -> Unit,
    onMesclarProdutos: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var abaSelecionada by remember { mutableIntStateOf(0) } // 0: Clientes, 1: Produtos
    var textoBusca by remember { mutableStateOf("") }

    // Estados de Diálogos Secundários
    var itemSelecionadoParaAcao by remember { mutableStateOf<String?>(null) }
    var acaoTipo by remember { mutableStateOf<AcaoTipo?>(null) } // EDITAR, MESCLAR, EXCLUIR
    var novoNomeInput by remember { mutableStateOf("") }
    var destinoSelecionado by remember { mutableStateOf("") }
    var showConfirmacaoMescla by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Gerenciar Clientes e Peças", fontWeight = FontWeight.Bold)
                TabRow(selectedTabIndex = abaSelecionada) {
                    Tab(
                        selected = abaSelecionada == 0,
                        onClick = {
                            abaSelecionada = 0
                            textoBusca = ""
                        },
                        text = { Text("👥 Clientes (${clientes.size})") }
                    )
                    Tab(
                        selected = abaSelecionada == 1,
                        onClick = {
                            abaSelecionada = 1
                            textoBusca = ""
                        },
                        text = { Text("👗 Peças (${produtos.size})") }
                    )
                }

                // Campo de Busca
                OutlinedTextField(
                    value = textoBusca,
                    onValueChange = { textoBusca = it },
                    placeholder = { Text(if (abaSelecionada == 0) "Buscar cliente..." else "Buscar peça...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Buscar") },
                    trailingIcon = {
                        if (textoBusca.isNotEmpty()) {
                            IconButton(onClick = { textoBusca = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Limpar")
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        text = {
            Box(modifier = Modifier.fillMaxWidth().height(350.dp)) {
                val listaBase = if (abaSelecionada == 0) clientes else produtos
                val listaAtual = remember(listaBase, textoBusca) {
                    if (textoBusca.isBlank()) {
                        listaBase
                    } else {
                        listaBase.filter { it.contains(textoBusca, ignoreCase = true) }
                    }
                }

                if (listaAtual.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (textoBusca.isBlank()) "Nenhum item cadastrado." else "Nenhum resultado encontrado.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(listaAtual) { item ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = item,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.weight(1f)
                                    )

                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        // Botão Renomear / Editar
                                        IconButton(onClick = {
                                            itemSelecionadoParaAcao = item
                                            novoNomeInput = item
                                            acaoTipo = AcaoTipo.EDITAR
                                        }) {
                                            Icon(Icons.Default.Edit, contentDescription = "Renomear", tint = MaterialTheme.colorScheme.primary)
                                        }

                                        // Botão Mesclar
                                        IconButton(onClick = {
                                            itemSelecionadoParaAcao = item
                                            destinoSelecionado = ""
                                            showConfirmacaoMescla = false
                                            acaoTipo = AcaoTipo.MESCLAR
                                        }) {
                                            Icon(Icons.Default.Merge, contentDescription = "Mesclar", tint = MaterialTheme.colorScheme.secondary)
                                        }

                                        // Botão Excluir
                                        IconButton(onClick = {
                                            itemSelecionadoParaAcao = item
                                            acaoTipo = AcaoTipo.EXCLUIR
                                        }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fechar")
            }
        }
    )

    // Diálogo de Ação (Editar, Mesclar ou Excluir)
    if (itemSelecionadoParaAcao != null && acaoTipo != null) {
        val tituloTipo = if (abaSelecionada == 0) "Cliente" else "Peça"
        val item = itemSelecionadoParaAcao!!

        when (acaoTipo) {
            AcaoTipo.EDITAR -> {
                AlertDialog(
                    onDismissRequest = { itemSelecionadoParaAcao = null },
                    title = { Text("Renomear $tituloTipo") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Digite o novo nome para \"$item\":")
                            OutlinedTextField(
                                value = novoNomeInput,
                                onValueChange = { novoNomeInput = it },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (novoNomeInput.isNotBlank() && novoNomeInput.trim() != item) {
                                    if (abaSelecionada == 0) {
                                        onRenomearCliente(item, novoNomeInput.trim())
                                    } else {
                                        onRenomearProduto(item, novoNomeInput.trim())
                                    }
                                }
                                itemSelecionadoParaAcao = null
                            },
                            enabled = novoNomeInput.isNotBlank()
                        ) {
                            Text("Salvar")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { itemSelecionadoParaAcao = null }) {
                            Text("Cancelar")
                        }
                    }
                )
            }
            AcaoTipo.MESCLAR -> {
                val opcoesDestino = if (abaSelecionada == 0) clientes else produtos

                if (showConfirmacaoMescla) {
                    AlertDialog(
                        onDismissRequest = { showConfirmacaoMescla = false },
                        title = { Text("Confirmar Mesclagem") },
                        text = {
                            Text("Tem certeza que deseja mesclar \"$item\" em \"$destinoSelecionado\"?\n\nTodas as vendas de \"$item\" passarão a pertencer a \"$destinoSelecionado\".")
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    if (abaSelecionada == 0) {
                                        onMesclarClientes(item, destinoSelecionado)
                                    } else {
                                        onMesclarProdutos(item, destinoSelecionado)
                                    }
                                    showConfirmacaoMescla = false
                                    itemSelecionadoParaAcao = null
                                }
                            ) {
                                Text("Sim, Mesclar")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showConfirmacaoMescla = false }) {
                                Text("Voltar")
                            }
                        }
                    )
                } else {
                    AlertDialog(
                        onDismissRequest = { itemSelecionadoParaAcao = null },
                        title = { Text("Mesclar $tituloTipo") },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Mesclar \"$item\" para qual $tituloTipo existente?")
                                if (opcoesDestino.isEmpty()) {
                                    Text("Não há outros itens para mesclar.", color = MaterialTheme.colorScheme.error)
                                } else {
                                    var expandedDest by remember { mutableStateOf(false) }
                                    ExposedDropdownMenuBox(
                                        expanded = expandedDest,
                                        onExpandedChange = { expandedDest = it },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        OutlinedTextField(
                                            value = destinoSelecionado,
                                            onValueChange = {},
                                            readOnly = true,
                                            label = { Text("Selecionar Destino") },
                                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDest) }
                                        )
                                        ExposedDropdownMenu(
                                            expanded = expandedDest,
                                            onDismissRequest = { expandedDest = false }
                                        ) {
                                            opcoesDestino.forEach { destino ->
                                                DropdownMenuItem(
                                                    text = { Text(destino) },
                                                    onClick = {
                                                        destinoSelecionado = destino
                                                        expandedDest = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    if (destinoSelecionado.isNotBlank()) {
                                        showConfirmacaoMescla = true
                                    }
                                },
                                enabled = destinoSelecionado.isNotBlank()
                            ) {
                                Text("Mesclar")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { itemSelecionadoParaAcao = null }) {
                                Text("Cancelar")
                            }
                        }
                    )
                }
            }
            AcaoTipo.EXCLUIR -> {
                AlertDialog(
                    onDismissRequest = { itemSelecionadoParaAcao = null },
                    title = { Text("Excluir $tituloTipo") },
                    text = {
                        Text("Tem certeza que deseja excluir \"$item\" e **todas as vendas** associadas a ele/ela?")
                    },
                    confirmButton = {
                        Button(
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            onClick = {
                                if (abaSelecionada == 0) {
                                    onExcluirCliente(item)
                                } else {
                                    onExcluirProduto(item)
                                }
                                itemSelecionadoParaAcao = null
                            }
                        ) {
                            Text("Excluir Tudo")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { itemSelecionadoParaAcao = null }) {
                            Text("Cancelar")
                        }
                    }
                )
            }
            else -> {}
        }
    }
}

private enum class AcaoTipo {
    EDITAR, MESCLAR, EXCLUIR
}
