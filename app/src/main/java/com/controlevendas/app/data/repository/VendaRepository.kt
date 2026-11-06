package com.controlevendas.app.data.repository

import com.controlevendas.app.data.dao.VendaDao
import com.controlevendas.app.data.model.Venda
import kotlinx.coroutines.flow.Flow

class VendaRepository(private val vendaDao: VendaDao) {
    val allVendas: Flow<List<Venda>> = vendaDao.getAllVendas()
    val mesesDisponiveis: Flow<List<String>> = vendaDao.getMesesDisponiveis()
    val clientes: Flow<List<String>> = vendaDao.getClientes()
    val produtos: Flow<List<String>> = vendaDao.getProdutos()

    fun getVendasByMes(mes: String): Flow<List<Venda>> = vendaDao.getVendasByMes(mes)

    fun searchVendas(query: String): Flow<List<Venda>> = vendaDao.searchVendas(query)

    suspend fun insert(venda: Venda): Long = vendaDao.insert(venda)

    suspend fun insertAll(vendas: List<Venda>) = vendaDao.insertAll(vendas)

    suspend fun update(venda: Venda) = vendaDao.update(venda)

    suspend fun delete(venda: Venda) = vendaDao.delete(venda)

    suspend fun deleteVendasPorCliente(nomeCliente: String) = vendaDao.deleteVendasPorCliente(nomeCliente)

    suspend fun atualizarNomeCliente(antigo: String, novo: String) = vendaDao.atualizarNomeCliente(antigo, novo)

    suspend fun atualizarNomeProduto(antigo: String, novo: String) = vendaDao.atualizarNomeProduto(antigo, novo)

    suspend fun deleteVendasPorProduto(produto: String) = vendaDao.deleteVendasPorProduto(produto)

    suspend fun mesclarClientes(origem: String, destino: String) = vendaDao.mesclarClientes(origem, destino)

    suspend fun mesclarProdutos(origem: String, destino: String) = vendaDao.mesclarProdutos(origem, destino)

    suspend fun deleteVendasPorMes(mes: String) = vendaDao.deleteVendasPorMes(mes)

    suspend fun deleteAll() = vendaDao.deleteAll()
}
