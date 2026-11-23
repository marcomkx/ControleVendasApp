package com.controlevendas.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.controlevendas.app.ui.viewmodel.ClienteResumo
import java.text.NumberFormat
import java.util.Locale

@Composable
fun ClientePagamentoDialog(
    cliente: ClienteResumo,
    onDismiss: () -> Unit,
    onConfirmarPagamento: (Double) -> Unit,
    onQuitarTotal: () -> Unit
) {
    var valorStr by remember {
        mutableStateOf(String.format(Locale("pt", "BR"), "%.2f", cliente.saldoDevedor).replace(".", ","))
    }
    val formatador = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Baixa por Cliente", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Cliente: ${cliente.nome}", fontWeight = FontWeight.SemiBold)
                Text("Valor Pendente: ${formatador.format(cliente.saldoDevedor)}", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                Text("Total Já Pago: ${formatador.format(cliente.totalPago)}", color = Color(0xFF2E7D32), fontWeight = FontWeight.SemiBold)

                OutlinedTextField(
                    value = valorStr,
                    onValueChange = { valorStr = it },
                    label = { Text("Valor Recebido Agora (R$)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Button(
                    onClick = onQuitarTotal,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text("Quitar Tudo (${formatador.format(cliente.saldoDevedor)})")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val v = valorStr.replace(",", ".").toDoubleOrNull()
                    if (v != null && v > 0) {
                        onConfirmarPagamento(v)
                    }
                }
            ) {
                Text("Confirmar Baixa")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
