package com.controlevendas.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.controlevendas.app.data.model.Venda
import java.text.NumberFormat
import java.util.Locale

@Composable
fun PagamentoDialog(
    venda: Venda,
    onDismiss: () -> Unit,
    onConfirmarPagamento: (Double) -> Unit,
    onQuitarTotal: () -> Unit
) {
    var valorStr by remember { mutableStateOf(venda.saldoDevedor.toString()) }
    val formatador = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Receber Pagamento", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Cliente: ${venda.cliente}", fontWeight = FontWeight.SemiBold)
                Text("Peça: ${venda.produto}")
                Text("Saldo Devedor Atual: ${formatador.format(venda.saldoDevedor)}", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)

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
                    Text("Quitar Total (${formatador.format(venda.saldoDevedor)})")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val v = valorStr.toDoubleOrNull()
                    if (v != null && v > 0) {
                        onConfirmarPagamento(v)
                    }
                }
            ) {
                Text("Confirmar Parcial")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
