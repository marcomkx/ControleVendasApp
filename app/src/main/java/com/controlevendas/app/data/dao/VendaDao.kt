package com.controlevendas.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.controlevendas.app.data.model.Venda
import kotlinx.coroutines.flow.Flow

@Dao
interface VendaDao {
    @Query("SELECT * FROM vendas ORDER BY id DESC")
    fun getAllVendas(): Flow<List<Venda>>

    @Query("SELECT * FROM vendas WHERE mesReferencia = :mes ORDER BY id DESC")
    fun getVendasByMes(mes: String): Flow<List<Venda>>

    @Query("""
        SELECT DISTINCT mesReferencia FROM vendas 
        ORDER BY 
            CASE 
                WHEN mesReferencia LIKE 'DEZEMBRO%' THEN 1
                WHEN mesReferencia LIKE 'NOVEMBRO%' THEN 2
                WHEN mesReferencia LIKE 'OUTUBRO%' THEN 3
                WHEN mesReferencia LIKE 'SETEMBRO%' THEN 4
                WHEN mesReferencia LIKE 'AGOSTO%' THEN 5
                WHEN mesReferencia LIKE 'JULHO%' THEN 6
                WHEN mesReferencia LIKE 'JUNHO%' THEN 7
                WHEN mesReferencia LIKE 'MAIO%' THEN 8
                WHEN mesReferencia LIKE 'ABRIL%' THEN 9
                WHEN mesReferencia LIKE 'MARÇO%' THEN 10
                WHEN mesReferencia LIKE 'FEVEREIRO%' THEN 11
                WHEN mesReferencia LIKE 'JANEIRO%' THEN 12
                ELSE 13
            END ASC, id DESC
    """)
    fun getMesesDisponiveis(): Flow<List<String>>

    @Query("SELECT DISTINCT cliente FROM vendas ORDER BY cliente ASC")
    fun getClientes(): Flow<List<String>>

    @Query("SELECT DISTINCT produto FROM vendas ORDER BY produto ASC")
    fun getProdutos(): Flow<List<String>>

    @Query("SELECT * FROM vendas WHERE cliente LIKE '%' || :query || '%' OR produto LIKE '%' || :query || '%' ORDER BY id DESC")
    fun searchVendas(query: String): Flow<List<Venda>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(venda: Venda): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(vendas: List<Venda>)

    @Update
    suspend fun update(venda: Venda)

    @Delete
    suspend fun delete(venda: Venda)

    @Query("DELETE FROM vendas WHERE LOWER(TRIM(cliente)) = LOWER(TRIM(:nomeCliente))")
    suspend fun deleteVendasPorCliente(nomeCliente: String)

    @Query("UPDATE vendas SET cliente = :novoNome WHERE LOWER(TRIM(cliente)) = LOWER(TRIM(:antigoNome))")
    suspend fun atualizarNomeCliente(antigoNome: String, novoNome: String)

    @Query("UPDATE vendas SET produto = :novoNome WHERE LOWER(TRIM(produto)) = LOWER(TRIM(:antigoNome))")
    suspend fun atualizarNomeProduto(antigoNome: String, novoNome: String)

    @Query("DELETE FROM vendas WHERE LOWER(TRIM(produto)) = LOWER(TRIM(:nomeProduto))")
    suspend fun deleteVendasPorProduto(nomeProduto: String)

    @Query("UPDATE vendas SET cliente = :destino WHERE LOWER(TRIM(cliente)) = LOWER(TRIM(:origem))")
    suspend fun mesclarClientes(origem: String, destino: String)

    @Query("UPDATE vendas SET produto = :destino WHERE LOWER(TRIM(produto)) = LOWER(TRIM(:origem))")
    suspend fun mesclarProdutos(origem: String, destino: String)

    @Query("DELETE FROM vendas WHERE LOWER(TRIM(mesReferencia)) = LOWER(TRIM(:mes))")
    suspend fun deleteVendasPorMes(mes: String)

    @Query("DELETE FROM vendas")
    suspend fun deleteAll()
}
