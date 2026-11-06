package com.controlevendas.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.ReceiptLong
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
import com.controlevendas.app.data.database.AppDatabase
import com.controlevendas.app.data.model.Venda
import com.controlevendas.app.data.repository.VendaRepository
import com.controlevendas.app.ui.components.GerenciarClientesProdutosDialog
import com.controlevendas.app.ui.components.NovaVendaDialog
import com.controlevendas.app.ui.components.PagamentoDialog
import com.controlevendas.app.ui.screens.ClientesScreen
import com.controlevendas.app.ui.screens.DashboardScreen
import com.controlevendas.app.ui.screens.VendasListScreen
import com.controlevendas.app.ui.theme.ControleVendasTheme
import com.controlevendas.app.ui.theme.TemaModo
import com.controlevendas.app.ui.viewmodel.VendasViewModel
import com.controlevendas.app.ui.viewmodel.VendasViewModelFactory
import com.controlevendas.app.utils.ExcelUtils

class MainActivity : ComponentActivity() {

    private val database by lazy { AppDatabase.getDatabase(this) }
    private val repository by lazy { VendaRepository(database.vendaDao()) }
    private val viewModel: VendasViewModel by viewModels {
        VendasViewModelFactory(repository)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val context = LocalContext.current
            val modoTema by viewModel.modoTema.collectAsState()

            LaunchedEffect(Unit) {
                viewModel.inicializarTema(context)
                viewModel.verificarAtualizacao(context)
            }

            ControleVendasTheme(modoTema = modoTema) {
                var abaSelecionada by remember { mutableIntStateOf(0) } // 0: Vendas, 1: Clientes, 2: Resumo
                var showNovaVendaDialog by remember { mutableStateOf(false) }
                var vendaEditando by remember { mutableStateOf<Venda?>(null) }
                var vendaParaPagamento by remember { mutableStateOf<Venda?>(null) }

                // Estados de Menu e Diálogos
                var menuExpanded by remember { mutableStateOf(false) }
                var showTemaDialog by remember { mutableStateOf(false) }
                var showGerenciarMesesDialog by remember { mutableStateOf(false) }
                var showCriarMesDialog by remember { mutableStateOf(false) }
                var novoMesInput by remember { mutableStateOf("") }
                var showGerenciarClientesProdutosDialog by remember { mutableStateOf(false) }
                var mesParaExcluir by remember { mutableStateOf<String?>(null) }

                var vendasImportadasDoArquivo by remember { mutableStateOf<List<Venda>>(emptyList()) }
                var showConfirmImportacaoArquivo by remember { mutableStateOf(false) }

                val novaVersao by viewModel.novaVersao.collectAsState()
                var ignorarAtualizacao by remember { mutableStateOf(false) }

                val clientesSugestoes by viewModel.clientesSugestoes.collectAsState()
                val produtosSugestoes by viewModel.produtosSugestoes.collectAsState()
                val filtroMes by viewModel.filtroMes.collectAsState()
                val todasVendas by viewModel.todasVendas.collectAsState()
                val mesesDisponiveis by viewModel.mesesDisponiveis.collectAsState()

                // Launchers de Arquivos (Storage Access Framework)
                val filePickerLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.GetContent()
                ) { uri ->
                    if (uri != null) {
                        val lidas = ExcelUtils.lerVendasDoUri(context, uri)
                        if (lidas.isNotEmpty()) {
                            vendasImportadasDoArquivo = lidas
                            showConfirmImportacaoArquivo = true
                        } else {
                            Toast.makeText(context, "Nenhuma venda válida encontrada no arquivo selecionado.", Toast.LENGTH_LONG).show()
                        }
                    }
                }

                val exportLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                ) { uri ->
                    if (uri != null) {
                        val ok = ExcelUtils.exportarVendasParaUri(context, todasVendas, uri)
                        if (ok) {
                            Toast.makeText(context, "Planilha Excel (.xlsx) salva com sucesso!", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "Erro ao exportar planilha.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    text = when (abaSelecionada) {
                                        0 -> "Vendas Realizadas"
                                        1 -> "Controle de Clientes"
                                        else -> "Resumo Financeiro"
                                    },
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            actions = {
                                IconButton(onClick = { menuExpanded = true }) {
                                    Icon(
                                        Icons.Default.Menu,
                                        contentDescription = "Menu de Opções",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }

                                DropdownMenu(
                                    expanded = menuExpanded,
                                    onDismissRequest = { menuExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("📤 Exportar para Google Planilhas") },
                                        leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                                        onClick = {
                                            menuExpanded = false
                                            ExcelUtils.abrirNoGooglePlanilhasOuCompartilhar(context, todasVendas)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("💾 Salvar Planilha (.xlsx)") },
                                        leadingIcon = { Icon(Icons.Default.FileDownload, contentDescription = null) },
                                        onClick = {
                                            menuExpanded = false
                                            exportLauncher.launch("Controle_de_Vendas.xlsx")
                                        }
                                    )
                                    HorizontalDivider()
                                    DropdownMenuItem(
                                        text = { Text("📥 Importar Planilha do Dispositivo") },
                                        leadingIcon = { Icon(Icons.Default.FileUpload, contentDescription = null) },
                                        onClick = {
                                            menuExpanded = false
                                            filePickerLauncher.launch("*/*")
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("🗓️ Gerenciar Meses") },
                                        leadingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
                                        onClick = {
                                            menuExpanded = false
                                            showGerenciarMesesDialog = true
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("👥 Gerenciar Clientes e Peças") },
                                        leadingIcon = { Icon(Icons.Default.Group, contentDescription = null) },
                                        onClick = {
                                            menuExpanded = false
                                            showGerenciarClientesProdutosDialog = true
                                        }
                                    )
                                    HorizontalDivider()
                                    DropdownMenuItem(
                                        text = { Text("🎨 Tema (Claro / Escuro)") },
                                        leadingIcon = { Icon(Icons.Default.Palette, contentDescription = null) },
                                        onClick = {
                                            menuExpanded = false
                                            showTemaDialog = true
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("🔄 Verificar Atualizações") },
                                        leadingIcon = { Icon(Icons.Default.Insights, contentDescription = null) },
                                        onClick = {
                                            menuExpanded = false
                                            viewModel.verificarAtualizacao(context, manual = true)
                                        }
                                    )

                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    },
                    bottomBar = {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 8.dp
                        ) {
                            NavigationBarItem(
                                selected = abaSelecionada == 0,
                                onClick = { abaSelecionada = 0 },
                                icon = { Icon(Icons.Default.ReceiptLong, contentDescription = "Vendas") },
                                label = { Text("Vendas", fontWeight = if (abaSelecionada == 0) FontWeight.Bold else FontWeight.Normal) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = Color(0xFFFCE4EC)
                                )
                            )
                            NavigationBarItem(
                                selected = abaSelecionada == 1,
                                onClick = { abaSelecionada = 1 },
                                icon = { Icon(Icons.Default.Group, contentDescription = "Clientes") },
                                label = { Text("Clientes", fontWeight = if (abaSelecionada == 1) FontWeight.Bold else FontWeight.Normal) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = Color(0xFFFCE4EC)
                                )
                            )
                            NavigationBarItem(
                                selected = abaSelecionada == 2,
                                onClick = { abaSelecionada = 2 },
                                icon = { Icon(Icons.Default.Insights, contentDescription = "Resumo") },
                                label = { Text("Resumo", fontWeight = if (abaSelecionada == 2) FontWeight.Bold else FontWeight.Normal) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = Color(0xFFFCE4EC)
                                )
                            )
                        }
                    },
                    floatingActionButton = {
                        FloatingActionButton(
                            onClick = {
                                vendaEditando = null
                                showNovaVendaDialog = true
                            },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Nova Venda")
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        when (abaSelecionada) {
                            0 -> VendasListScreen(
                                viewModel = viewModel,
                                onEditarVenda = {
                                    vendaEditando = it
                                    showNovaVendaDialog = true
                                },
                                onReceberPagamento = { vendaParaPagamento = it }
                            )
                            1 -> ClientesScreen(viewModel = viewModel)
                            2 -> DashboardScreen(viewModel = viewModel)
                        }
                    }

                    if (showNovaVendaDialog) {
                        NovaVendaDialog(
                            vendaEditando = vendaEditando,
                            mesAtual = if (filtroMes != null && filtroMes != "Todos") filtroMes!! else "",
                            sugestoesClientes = clientesSugestoes,
                            sugestoesProdutos = produtosSugestoes,
                            sugestoesMeses = mesesDisponiveis,
                            onDismiss = {
                                showNovaVendaDialog = false
                                vendaEditando = null
                            },
                            onSalvar = { venda ->
                                viewModel.salvarVenda(venda)
                                showNovaVendaDialog = false
                                vendaEditando = null
                            }
                        )
                    }

                    if (vendaParaPagamento != null) {
                        PagamentoDialog(
                            venda = vendaParaPagamento!!,
                            onDismiss = { vendaParaPagamento = null },
                            onConfirmarPagamento = { valor ->
                                viewModel.registrarPagamento(vendaParaPagamento!!, valor)
                                vendaParaPagamento = null
                            },
                            onQuitarTotal = {
                                viewModel.quitarVenda(vendaParaPagamento!!)
                                vendaParaPagamento = null
                            }
                        )
                    }

                    // Diálogo de Seleção de Tema
                    if (showTemaDialog) {
                        AlertDialog(
                            onDismissRequest = { showTemaDialog = false },
                            title = { Text("🎨 Selecionar Tema") },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    TemaModo.entries.forEach { modo ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    viewModel.setModoTema(context, modo)
                                                    showTemaDialog = false
                                                }
                                                .padding(vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(
                                                selected = modoTema == modo,
                                                onClick = {
                                                    viewModel.setModoTema(context, modo)
                                                    showTemaDialog = false
                                                }
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = when (modo) {
                                                    TemaModo.SISTEMA -> "Padrão do Sistema"
                                                    TemaModo.CLARO -> "Modo Claro"
                                                    TemaModo.ESCURO -> "Modo Escuro"
                                                },
                                                fontSize = 16.sp
                                            )
                                        }
                                    }
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = { showTemaDialog = false }) {
                                    Text("Fechar")
                                }
                            }
                        )
                    }

                    // Diálogo de Gerenciar Meses
                    if (showGerenciarMesesDialog) {
                        AlertDialog(
                            onDismissRequest = { showGerenciarMesesDialog = false },
                            title = { Text("🗓️ Gerenciar Meses") },
                            text = {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Button(
                                        onClick = { showCriarMesDialog = true },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 12.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Criar Mês em Branco")
                                    }

                                    if (mesesDisponiveis.isEmpty()) {
                                        Text("Nenhum mês cadastrado no momento.", color = Color.Gray)
                                    } else {
                                        LazyColumn(
                                            modifier = Modifier.heightIn(max = 240.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            items(mesesDisponiveis) { mes ->
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(mes, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                                                    IconButton(onClick = { mesParaExcluir = mes }) {
                                                        Icon(
                                                            Icons.Default.DeleteOutline,
                                                            contentDescription = "Apagar $mes",
                                                            tint = MaterialTheme.colorScheme.error
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = { showGerenciarMesesDialog = false }) {
                                    Text("Fechar")
                                }
                            }
                        )
                    }

                    // Diálogo para Criar Mês em Branco
                    if (showCriarMesDialog) {
                        AlertDialog(
                            onDismissRequest = { showCriarMesDialog = false },
                            title = { Text("Criar Mês em Branco") },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Digite a referência do novo mês (ex: JANEIRO/2027):")
                                    OutlinedTextField(
                                        value = novoMesInput,
                                        onValueChange = { novoMesInput = it },
                                        singleLine = true,
                                        label = { Text("Mês/Ano") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        if (novoMesInput.isNotBlank()) {
                                            viewModel.criarMesEmBranco(novoMesInput.trim().uppercase())
                                            novoMesInput = ""
                                            showCriarMesDialog = false
                                            Toast.makeText(context, "Mês em branco criado com sucesso!", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    enabled = novoMesInput.isNotBlank()
                                ) {
                                    Text("Criar")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showCriarMesDialog = false }) {
                                    Text("Cancelar")
                                }
                            }
                        )
                    }

                    // Confirmação de Exclusão de Mês
                    if (mesParaExcluir != null) {
                        AlertDialog(
                            onDismissRequest = { mesParaExcluir = null },
                            title = { Text("Apagar Mês") },
                            text = {
                                Text("Deseja realmente APAGAR todas as vendas do mês \"$mesParaExcluir\"?\n\nEsta ação não poderá ser desfeita.")
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        viewModel.excluirMes(mesParaExcluir!!)
                                        mesParaExcluir = null
                                        Toast.makeText(context, "Mês apagado com sucesso!", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Text("Sim, Apagar", color = MaterialTheme.colorScheme.onError)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { mesParaExcluir = null }) {
                                    Text("Cancelar")
                                }
                            }
                        )
                    }

                    // Confirmação de Importação de Arquivo Selecionado
                    if (showConfirmImportacaoArquivo) {
                        AlertDialog(
                            onDismissRequest = { showConfirmImportacaoArquivo = false },
                            title = { Text("Importar Planilha") },
                            text = {
                                Text("Foram encontradas ${vendasImportadasDoArquivo.size} venda(s) no arquivo selecionado.\n\nDeseja importar e adicionar estas vendas ao aplicativo?")
                            },
                            confirmButton = {
                                Button(onClick = {
                                    vendasImportadasDoArquivo.forEach { v ->
                                        viewModel.salvarVenda(v)
                                    }
                                    showConfirmImportacaoArquivo = false
                                    Toast.makeText(context, "${vendasImportadasDoArquivo.size} vendas importadas com sucesso!", Toast.LENGTH_LONG).show()
                                }) {
                                    Text("Importar")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showConfirmImportacaoArquivo = false }) {
                                    Text("Cancelar")
                                }
                            }
                        )
                    }



                    // Gerenciamento de Clientes e Peças
                    if (showGerenciarClientesProdutosDialog) {
                        GerenciarClientesProdutosDialog(
                            clientes = clientesSugestoes,
                            produtos = produtosSugestoes,
                            onRenomearCliente = { antigo, novo -> viewModel.renomearCliente(antigo, novo) },
                            onExcluirCliente = { nome -> viewModel.excluirClienteCompleto(nome) },
                            onMesclarClientes = { origem, destino -> viewModel.mesclarClientes(origem, destino) },
                            onRenomearProduto = { antigo, novo -> viewModel.renomearProduto(antigo, novo) },
                            onExcluirProduto = { nome -> viewModel.excluirProdutoCompleto(nome) },
                            onMesclarProdutos = { origem, destino -> viewModel.mesclarProdutos(origem, destino) },
                            onDismiss = { showGerenciarClientesProdutosDialog = false }
                        )
                    }

                    // Notificação de Nova Versão no GitHub
                    if (novaVersao != null && !ignorarAtualizacao) {
                        AlertDialog(
                            onDismissRequest = { ignorarAtualizacao = true },
                            title = { Text("🚀 Nova Atualização Disponível!") },
                            text = {
                                Text("Uma nova versão (v${novaVersao!!.versionName}) do aplicativo está disponível no GitHub.\n\nDeseja baixar e atualizar agora?")
                            },
                            confirmButton = {
                                Button(onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(novaVersao!!.apkUrl ?: novaVersao!!.releaseUrl)).apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                    context.startActivity(intent)
                                    ignorarAtualizacao = true
                                }) {
                                    Text("Baixar Atualização")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { ignorarAtualizacao = true }) {
                                    Text("Depois")
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
