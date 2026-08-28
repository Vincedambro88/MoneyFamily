package com.moneyfamily.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moneyfamily.app.data.Movement
import com.moneyfamily.app.data.categoryStats
import com.moneyfamily.app.data.memberStats
import com.moneyfamily.app.data.monthStats
import java.text.NumberFormat
import java.util.Locale

private val analysisMoney = NumberFormat.getCurrencyInstance(Locale.ITALY)

@Composable
fun AnalysisScreen(movements: List<Movement>) {
    val months = movements.monthStats()
    val categories = movements.categoryStats()
    val members = movements.memberStats()
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Text("Analisi", style = MaterialTheme.typography.headlineSmall) }
        item { Text("Storico reale di entrate e uscite", style = MaterialTheme.typography.bodyMedium) }
        item { Text("Andamento mensile", style = MaterialTheme.typography.titleLarge) }
        items(months) { m ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    Text("${m.month.toString().padStart(2, '0')}/${m.year}", style = MaterialTheme.typography.titleMedium)
                    Text("Entrate: ${analysisMoney.format(m.income)}")
                    Text("Uscite: ${analysisMoney.format(m.expense)}")
                    Text("Saldo: ${analysisMoney.format(m.balance)}")
                }
            }
        }
        item { Text("Spese per categoria", style = MaterialTheme.typography.titleLarge) }
        items(categories) { c ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(c.category)
                Text("${analysisMoney.format(c.amount)} (${String.format(Locale.ITALY, "%.1f", c.percentage)}%)")
            }
        }
        item { Text("Analisi per membro", style = MaterialTheme.typography.titleLarge) }
        items(members) { m ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    Text(m.member, style = MaterialTheme.typography.titleMedium)
                    Text("Entrate: ${analysisMoney.format(m.income)}")
                    Text("Uscite: ${analysisMoney.format(m.expense)}")
                    Text("Saldo: ${analysisMoney.format(m.balance)}")
                }
            }
        }
    }
}
