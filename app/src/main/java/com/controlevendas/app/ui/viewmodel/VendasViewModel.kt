package com.controlevendas.app.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.controlevendas.app.data.model.Venda
import com.controlevendas.app.data.repository.VendaRepository
import com.controlevendas.app.ui.theme.TemaModo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.text.NumberFormat
import java.util.Locale

data class ResumoMes(
    val mes: String,
    val totalVendido: Double,
    val totalPago: Double,
    val totalPendente: Double
)

data class ResumoMetricas(
    val lucroTotal: Double = 0.0,
    val totalVendido: Double = 0.0,
    val totalFornecedor: Double = 0.0,
    val totalPendente: Double = 0.0,
    val totalPago: Double = 0.0,
    val totalPecas: Int = 0
)

data class ClienteResumo(
    val nome: String,
    val totalComprado: Double,
    val totalPago: Double,
    val saldoDevedor: Double,
    val totalPecas: Int,
    val vendas: List<Venda>
)

class VendasViewModel(private val repository: VendaRepository) : ViewModel() {

    private val _novaVersao = MutableStateFlow<com.controlevendas.app.utils.GitHubRelease?>(null)
    val novaVersao: StateFlow<com.controlevendas.app.utils.GitHubRelease?> = _novaVersao

    fun verificarAtualizacao(context: Context, manual: Boolean = false) {
        viewModelScope.launch {
            val release = com.controlevendas.app.utils.UpdateChecker.checkForUpdate(context)
            if (release != null) {
                _novaVersao.value = release
            } else if (manual) {
                Toast.makeText(context, "Seu aplicativo já está na versão mais recente!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private var selecionouMesManualmente = false
    private var selecionouClienteMesManualmente = false

    private val _filtroMes = MutableStateFlow<String?>(null)
    val filtroMes: StateFlow<String?> = _filtroMes

    private val _pesquisa = MutableStateFlow("")
    val pesquisa: StateFlow<String> = _pesquisa

    private val _filtroStatus = MutableStateFlow("Todos") // Todos, Pendente, Quitado
    val filtroStatus: StateFlow<String> = _filtroStatus

    private val _modoTema = MutableStateFlow(TemaModo.SISTEMA)
    val modoTema: StateFlow<TemaModo> = _modoTema

    val mesesDisponiveis: StateFlow<List<String>> = repository.mesesDisponiveis
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            mesesDisponiveis.collect { meses ->
                if (meses.isNotEmpty()) {
                    if (!selecionouMesManualmente) {
                        _filtroMes.value = meses.first()
                    }
                    if (!selecionouClienteMesManualmente) {
                        _filtroClientesMes.value = meses.first()
                    }
                }
            }
        }
    }

    val clientesSugestoes: StateFlow<List<String>> = repository.clientes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val produtosSugestoes: StateFlow<List<String>> = repository.produtos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todasVendas: StateFlow<List<Venda>> = repository.allVendas
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val vendasFiltradas: StateFlow<List<Venda>> = combine(
        todasVendas,
        _filtroMes,
        _pesquisa,
        _filtroStatus
    ) { vendas, mes, query, status ->
        vendas.filter { venda ->
            if (venda.cliente == "— MÊS EM BRANCO —") return@filter false
            val matchMes = mes == null || mes == "Todos" || venda.mesReferencia.equals(mes, ignoreCase = true)
            val matchQuery = query.isBlank() ||
                    venda.cliente.contains(query, ignoreCase = true) ||
                    venda.produto.contains(query, ignoreCase = true)
            val matchStatus = when (status) {
                "Pendente" -> !venda.isQuitado
                "Quitado" -> venda.isQuitado
                else -> true
            }
            matchMes && matchQuery && matchStatus
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val metricas: StateFlow<ResumoMetricas> = combine(
        todasVendas,
        _filtroMes
    ) { vendas, mes ->
        val vendasDoMes = (if (mes == null || mes == "Todos") {
            vendas
        } else {
            vendas.filter { it.mesReferencia.equals(mes, ignoreCase = true) }
        }).filter { it.cliente != "— MÊS EM BRANCO —" }

        var lucro = 0.0
        var vendido = 0.0
        var fornecedor = 0.0
        var pendente = 0.0
        var pago = 0.0
        val pecas = vendasDoMes.size

        vendasDoMes.forEach {
            lucro += it.lucro
            vendido += it.valorPeca
            fornecedor += it.valorFornecedor
            pendente += it.saldoDevedor
            pago += it.valorPago
        }

        ResumoMetricas(
            lucroTotal = lucro,
            totalVendido = vendido,
            totalFornecedor = fornecedor,
            totalPendente = pendente,
            totalPago = pago,
            totalPecas = pecas
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ResumoMetricas())

    private val _filtroClientesApenasPendentes = MutableStateFlow(false)
    val filtroClientesApenasPendentes: StateFlow<Boolean> = _filtroClientesApenasPendentes

    private val _filtroClientesMes = MutableStateFlow<String?>(null)
    val filtroClientesMes: StateFlow<String?> = _filtroClientesMes

    val clientesAgrupados: StateFlow<List<ClienteResumo>> = combine(
        todasVendas,
        _pesquisa,
        _filtroClientesApenasPendentes,
        _filtroClientesMes
    ) { vendas, query, apenasPendentes, mesFiltro ->
        val vendasFiltradasPorMes = if (mesFiltro == null || mesFiltro == "Todos") {
            vendas
        } else {
            vendas.filter { it.mesReferencia.equals(mesFiltro, ignoreCase = true) }
        }

        vendasFiltradasPorMes.groupBy { it.cliente.trim().lowercase() }
            .map { (_, vendasCliente) ->
                val nomePadrao = vendasCliente.first().cliente.trim()
                ClienteResumo(
                    nome = nomePadrao,
                    totalComprado = vendasCliente.sumOf { it.valorPeca },
                    totalPago = vendasCliente.sumOf { it.valorPago },
                    saldoDevedor = vendasCliente.sumOf { it.saldoDevedor },
                    totalPecas = vendasCliente.size,
                    vendas = vendasCliente
                )
            }
            .filter { resumo ->
                val matchQuery = query.isBlank() || resumo.nome.contains(query, ignoreCase = true)
                val matchPendencia = !apenasPendentes || resumo.saldoDevedor > 0.001
                matchQuery && matchPendencia
            }
            .sortedByDescending { it.saldoDevedor }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleFiltroClientesApenasPendentes() {
        _filtroClientesApenasPendentes.value = !_filtroClientesApenasPendentes.value
    }

    fun setFiltroClientesMes(mes: String?) {
        selecionouClienteMesManualmente = true
        _filtroClientesMes.value = mes
    }

    fun setFiltroMes(mes: String?) {
        selecionouMesManualmente = true
        _filtroMes.value = mes
    }

    fun setPesquisa(q: String) {
        _pesquisa.value = q
    }

    fun setFiltroStatus(st: String) {
        _filtroStatus.value = st
    }

    fun inicializarTema(context: Context) {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val salvos = prefs.getString("modo_tema", TemaModo.SISTEMA.name)
        _modoTema.value = try {
            TemaModo.valueOf(salvos ?: TemaModo.SISTEMA.name)
        } catch (_: Exception) {
            TemaModo.SISTEMA
        }
    }

    fun setModoTema(context: Context, novoModo: TemaModo) {
        _modoTema.value = novoModo
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("modo_tema", novoModo.name).apply()
    }

    fun salvarVenda(venda: Venda) {
        viewModelScope.launch {
            if (venda.id == 0L) {
                repository.insert(venda)
            } else {
                repository.update(venda)
            }
        }
    }

    fun registrarPagamento(venda: Venda, valorRecebido: Double) {
        viewModelScope.launch {
            val novoPago = venda.valorPago + valorRecebido
            val novoSaldo = (venda.valorPeca - novoPago).coerceAtLeast(0.0)
            val st = if (novoSaldo <= 0.001) "Quitado" else "Pendente"
            val atualizada = venda.copy(
                valorPago = novoPago,
                saldoDevedor = novoSaldo,
                status = st
            )
            repository.update(atualizada)
        }
    }

    fun quitarVenda(venda: Venda) {
        viewModelScope.launch {
            val atualizada = venda.copy(
                valorPago = venda.valorPeca,
                saldoDevedor = 0.0,
                status = "Quitado"
            )
            repository.update(atualizada)
        }
    }

    fun excluirVenda(venda: Venda) {
        viewModelScope.launch {
            repository.delete(venda)
        }
    }

    fun excluirCliente(nomeCliente: String) {
        viewModelScope.launch {
            repository.deleteVendasPorCliente(nomeCliente)
        }
    }

    fun criarMesEmBranco(mes: String) {
        viewModelScope.launch {
            val vendaPlaceholder = Venda(
                cliente = "— MÊS EM BRANCO —",
                produto = "—",
                valorPeca = 0.0,
                valorPago = 0.0,
                margemLucroPercentual = 30.0,
                lucro = 0.0,
                mesReferencia = mes.trim().uppercase(),
                status = "Quitado"
            )
            repository.insert(vendaPlaceholder)
            _filtroMes.value = mes.trim().uppercase()
        }
    }

    fun renomearCliente(antigo: String, novo: String) {
        viewModelScope.launch { repository.atualizarNomeCliente(antigo, novo) }
    }

    fun excluirClienteCompleto(nome: String) {
        viewModelScope.launch { repository.deleteVendasPorCliente(nome) }
    }

    fun mesclarClientes(origem: String, destino: String) {
        viewModelScope.launch { repository.mesclarClientes(origem, destino) }
    }

    fun renomearProduto(antigo: String, novo: String) {
        viewModelScope.launch { repository.atualizarNomeProduto(antigo, novo) }
    }

    fun excluirProdutoCompleto(nome: String) {
        viewModelScope.launch { repository.deleteVendasPorProduto(nome) }
    }

    fun mesclarProdutos(origem: String, destino: String) {
        viewModelScope.launch { repository.mesclarProdutos(origem, destino) }
    }

    fun excluirMes(mes: String) {
        viewModelScope.launch {
            repository.deleteVendasPorMes(mes)
        }
    }

    // Dados para o gráfico: resumo por mês (todos os meses, sem filtro) — mais recente primeiro
    val vendasPorMes: StateFlow<List<ResumoMes>> = todasVendas
        .map { vendas ->
            vendas.groupBy { it.mesReferencia }
                .map { (mes, lista) ->
                    ResumoMes(
                        mes = mes,
                        totalVendido = lista.sumOf { it.valorPeca },
                        totalPago = lista.sumOf { it.valorPago },
                        totalPendente = lista.sumOf { it.saldoDevedor }
                    )
                }
                .sortedByDescending { resumo ->
                    val ordemMes = listOf(
                        "JANEIRO", "FEVEREIRO", "MARÇO", "ABRIL", "MAIO", "JUNHO",
                        "JULHO", "AGOSTO", "SETEMBRO", "OUTUBRO", "NOVEMBRO", "DEZEMBRO"
                    )
                    val nomeMes = resumo.mes.substringBefore("/").trim().uppercase()
                    val anoStr = resumo.mes.substringAfter("/").trim()
                    val ano = anoStr.toIntOrNull() ?: 0
                    val idxMes = ordemMes.indexOfFirst { nomeMes.startsWith(it) }.takeIf { it >= 0 } ?: 0
                    ano * 12 + idxMes
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun cobrarWhatsApp(context: Context, cliente: String, valorPendente: Double, itens: List<String> = emptyList()) {
        val formatador = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
        val valorFormatado = formatador.format(valorPendente)

        val detalheItens = if (itens.isNotEmpty()) {
            "\nReferente às peças: " + itens.joinToString(", ")
        } else ""

        val mensagem = """
            Olá $cliente, tudo bem? 😊
            Passando para lembrar do seu saldo pendente de $valorFormatado$detalheItens.
            Se já fez o pagamento, por favor desconsidere! Qualquer dúvida me avise. Obrigado(a)! ❤️
        """.trimIndent()

        try {
            val url = "https://api.whatsapp.com/send?text=" + URLEncoder.encode(mensagem, "UTF-8")
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(url)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "WhatsApp não instalado ou erro ao abrir", Toast.LENGTH_SHORT).show()
        }
    }
}

class VendasViewModelFactory(private val repository: VendaRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(VendasViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return VendasViewModel(repository) as T
        }
        throw IllegalArgumentException("Classe ViewModel desconhecida")
    }
}
