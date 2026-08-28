package com.moneyfamily.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moneyfamily.app.data.Movement
import com.moneyfamily.app.data.MovementType
import java.text.NumberFormat
import java.util.Locale

private val analyticsMoney = NumberFormat.getCurrencyInstance(Locale.ITALY)

@Composable
fun AnalyticsScreen(movements: List<Movement>) {
    val months = movements.monthStats()
    val categories = movements.categoryStats()
    val members = movements.memberStats()
    LazyColumn(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Analisi", style = MaterialTheme.typography.headlineSmall) }
        item { Text("Andamento storico", style = MaterialTheme.typography.titleLarge) }
        items(months) { m ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(String.format(Locale.ITALIAN, "%02d/%d", m.month, m.year), style = MaterialTheme.typography.titleMedium)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Entrate"); Text(analyticsMoney.format(m.income)) }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Uscite"); Text(analyticsMoney.format(m.expense)) }
                    HorizontalDivider()
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Saldo", style = MaterialTheme.typography.titleMedium); Text(analyticsMoney.format(m.balance), style = MaterialTheme.typography.titleMedium) }
                }
            }
        }
        item { Spacer(Modifier.height(8.dp)); Text("Spese per categoria", style = MaterialTheme.typography.titleLarge) }
        items(categories) { s -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(s.category); Text("${analyticsMoney.format(s.amount)} (${String.format(Locale.ITALIAN, "%.1f", s.percentage)}%)") } }
        item { Spacer(Modifier.height(8.dp)); Text("Analisi per membro", style = MaterialTheme.typography.titleLarge) }
        items(members) { s -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(s.member); Text("+${analyticsMoney.format(s.income)} / -${analyticsMoney.format(s.expense)}") } }
    }
}
