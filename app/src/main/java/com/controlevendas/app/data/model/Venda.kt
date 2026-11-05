package com.controlevendas.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vendas")
data class Venda(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val data: String = "",
    val mesReferencia: String = "MAIO/2026",
    val cliente: String,
    val produto: String,
    val valorPeca: Double,
    val valorPago: Double = 0.0,
    val saldoDevedor: Double = (valorPeca - valorPago).coerceAtLeast(0.0),
    val margemLucroPercentual: Double = 30.0,
    val lucro: Double = valorPeca * (margemLucroPercentual / 100.0),
    val status: String = if (saldoDevedor <= 0.001) "Quitado" else "Pendente",
    val observacao: String = ""
) {
    val valorFornecedor: Double
        get() = (valorPeca - lucro).coerceAtLeast(0.0)

    val isQuitado: Boolean
        get() = saldoDevedor <= 0.001
}
