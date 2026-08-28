package com.moneyfamily.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.text.NumberFormat
import java.util.Locale

private val analyticsMoney = NumberFormat.getCurrencyInstance(Locale.ITALY)

@Composable
fun AnalyticsScreen(records: List<MoneyRecord>) {
    val months = records.groupBy { monthKey(it.date) }
        .map { (key, items) ->
            val income = items.filter { it.type == RecordType.INCOME }.sumOf { it.amount }
            val expense = items.filter { it.type == RecordType.EXPENSE }.sumOf { it.amount }
            Triple(key, income, expense)
        }.sortedByDescending { it.first }
    val expenses = records.filter { it.type == RecordType.EXPENSE }.groupBy { it.category }
        .mapValues { it.value.sumOf(MoneyRecord::amount) }.entries.sortedByDescending { it.value }
    val members = records.groupBy { it.member }.map { (member, items) ->
        val income = items.filter { it.type == RecordType.INCOME }.sumOf { it.amount }
        val expense = items.filter { it.type == RecordType.EXPENSE }.sumOf { it.amount }
        Triple(member, income, expense)
    }.sortedByDescending { it.third }
    val totalExpense = expenses.sumOf { it.value }

    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Analisi storica", style = MaterialTheme.typography.headlineSmall) }
        item { Text("Basata esclusivamente sui movimenti reali", style = MaterialTheme.typography.bodyMedium) }
        item { SectionTitle("Andamento mensile") }
        if (months.isEmpty()) item { Text("Inserisci alcuni movimenti per visualizzare lo storico.") }
        items(months.take(24)) { (month, income, expense) ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    Text(month, style = MaterialTheme.typography.titleMedium)
                    Text("Entrate: ${analyticsMoney.format(income)}")
                    Text("Uscite: ${analyticsMoney.format(expense)}")
                    Text("Saldo: ${analyticsMoney.format(income - expense)}")
                }
            }
        }
        item { SectionTitle("Spese per categoria") }
        items(expenses) { entry ->
            val pct = if (totalExpense > 0) entry.value * 100.0 / totalExpense else 0.0
            Column(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(entry.key); Text(analyticsMoney.format(entry.value))
                }
                LinearProgressIndicator(progress = { (pct / 100.0).toFloat().coerceIn(0f, 1f) }, Modifier.fillMaxWidth())
                Text("${String.format(Locale.ITALY, "%.1f", pct)}%", style = MaterialTheme.typography.bodySmall)
            }
        }
        item { SectionTitle("Per membro") }
        items(members) { (member, income, expense) ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    Text(member, style = MaterialTheme.typography.titleMedium)
                    Text("Entrate: ${analyticsMoney.format(income)}")
                    Text("Uscite: ${analyticsMoney.format(expense)}")
                    Text("Saldo: ${analyticsMoney.format(income - expense)}")
                }
            }
        }
    }
}

@Composable private fun SectionTitle(text: String) { Text(text, style = MaterialTheme.typography.titleLarge) }
private fun monthKey(date: String): String {
    val p = date.split("/")
    return if (p.size == 3) "${p[1]}/${p[2]}" else date
}
