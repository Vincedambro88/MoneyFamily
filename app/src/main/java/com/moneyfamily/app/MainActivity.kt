package com.moneyfamily.app

import android.app.DatePickerDialog
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val money = NumberFormat.getCurrencyInstance(Locale.ITALY)
private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.ITALY)
private val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.ITALIAN)
private val expenseCategories = listOf("Alimentari", "Casa", "Auto", "Bollette", "Salute", "Figli", "Istruzione", "Abbigliamento", "Tempo libero", "Vacanze", "Sport", "Altro")
private val incomeCategories = listOf("Stipendio", "Bonus", "Rimborso", "Altro")
private val paymentMethods = listOf("Contanti", "Carta", "Bonifico", "Addebito", "Altro")
private const val PREFS = "moneyfamily_data"
private const val RECORDS = "records"
private const val MEMBERS = "members"

enum class RecordType { EXPENSE, INCOME }
data class MoneyRecord(val id: Long, val type: RecordType, val amount: Double, val category: String, val description: String, val date: String, val member: String, val paymentMethod: String)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MoneyFamilyApp() }
    }
}

@Composable
private fun MoneyFamilyApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }
    var records by remember { mutableStateOf(loadRecords(prefs)) }
    var members by remember { mutableStateOf(loadMembers(prefs).ifEmpty { listOf("Famiglia", "Papà", "Mamma", "Figlio 1", "Figlio 2") }) }
    var tab by remember { mutableStateOf(0) }
    var selectedMonth by remember { mutableStateOf(Calendar.getInstance()) }
    var editing by remember { mutableStateOf<MoneyRecord?>(null) }
    var adding by remember { mutableStateOf(false) }

    fun saveRecord(record: MoneyRecord) {
        records = if (records.any { it.id == record.id }) records.map { if (it.id == record.id) record else it } else listOf(record) + records
        saveRecords(prefs, records)
    }
    fun deleteRecord(record: MoneyRecord) {
        records = records.filterNot { it.id == record.id }
        saveRecords(prefs, records)
    }

    MaterialTheme {
        Scaffold(bottomBar = {
            NavigationBar {
                listOf("Home", "Movimenti", "Analisi", "Impostazioni").forEachIndexed { index, title ->
                    NavigationBarItem(selected = tab == index, onClick = { tab = index }, icon = { Text(title.take(1)) }, label = { Text(title) })
                }
            }
        }) { padding ->
            Column(Modifier.fillMaxSize().padding(padding)) {
                Text("MoneyFamily", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(16.dp))
                when (tab) {
                    0 -> Dashboard(records, members, selectedMonth, { selectedMonth = shiftMonth(selectedMonth, -1) }, { selectedMonth = shiftMonth(selectedMonth, 1) }) { adding = true }
                    1 -> MovementList(records.filter { sameMonth(it.date, selectedMonth) }, selectedMonth, { selectedMonth = shiftMonth(selectedMonth, -1) }, { selectedMonth = shiftMonth(selectedMonth, 1) }, { editing = it }, { deleteRecord(it) })
                    2 -> Analytics(records)
                    else -> Settings(members) { name -> if (name.isNotBlank() && name.trim() !in members) { members = members + name.trim(); saveMembers(prefs, members) } }
                }
            }
        }
        if (adding || editing != null) RecordEditor(editing, members, { adding = false; editing = null }) { saveRecord(it); adding = false; editing = null }
    }
}

@Composable private fun Dashboard(records: List<MoneyRecord>, members: List<String>, month: Calendar, previous: () -> Unit, next: () -> Unit, add: () -> Unit) {
    val current = records.filter { sameMonth(it.date, month) }
    val income = current.filter { it.type == RecordType.INCOME }.sumOf { it.amount }
    val expense = current.filter { it.type == RecordType.EXPENSE }.sumOf { it.amount }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { MonthSelector(monthFormat.format(month.time), previous, next) }
        item { SummaryCard(income, expense, income - expense) }
        item { MonthlyChart(records) }
        item { Text("Spese per categoria", style = MaterialTheme.typography.titleLarge) }
        items(current.filter { it.type == RecordType.EXPENSE }.groupBy { it.category }.entries.sortedByDescending { it.value.sumOf(MoneyRecord::amount) }) { entry -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(entry.key); Text(money.format(entry.value.sumOf(MoneyRecord::amount))) } }
        item { Text("Per membro", style = MaterialTheme.typography.titleLarge) }
        items(members) { member ->
            val out = current.filter { it.type == RecordType.EXPENSE && it.member == member }.sumOf { it.amount }
            val inc = current.filter { it.type == RecordType.INCOME && it.member == member }.sumOf { it.amount }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(member); Text("+${money.format(inc)} / -${money.format(out)}") }
        }
        item { Button(add, Modifier.fillMaxWidth()) { Text("+ Inserisci movimento") } }
    }
}

@Composable private fun SummaryCard(income: Double, expense: Double, balance: Double) {
    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(18.dp)) { Text("Riepilogo reale", style = MaterialTheme.typography.titleMedium); Text("Entrate  ${money.format(income)}"); Text("Uscite  ${money.format(expense)}"); HorizontalDivider(Modifier.padding(vertical = 8.dp)); Text("Saldo  ${money.format(balance)}", style = MaterialTheme.typography.headlineSmall) } }
}

@Composable private fun MonthlyChart(records: List<MoneyRecord>) {
    val groups = records.groupBy { it.date.substring(3) }.toList().sortedBy { it.first }.takeLast(6)
    if (groups.isEmpty()) return
    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) {
        Text("Andamento ultimi mesi", style = MaterialTheme.typography.titleMedium)
        Canvas(Modifier.fillMaxWidth().height(180.dp)) {
            val max = groups.flatMap { (_, values) -> listOf(values.filter { it.type == RecordType.INCOME }.sumOf { it.amount }, values.filter { it.type == RecordType.EXPENSE }.sumOf { it.amount }) }.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0
            val width = size.width / groups.size.toFloat()
            groups.forEachIndexed { index, pair ->
                val inc = pair.second.filter { it.type == RecordType.INCOME }.sumOf { it.amount }
                val out = pair.second.filter { it.type == RecordType.EXPENSE }.sumOf { it.amount }
                val x = index * width + width / 2f
                drawLine(Color(0xFF2E7D32), Offset(x, size.height), Offset(x, size.height * (1f - (inc / max).toFloat())), strokeWidth = 18f)
                drawLine(Color(0xFFC62828), Offset(x + 24f, size.height), Offset(x + 24f, size.height * (1f - (out / max).toFloat())), strokeWidth = 18f)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) { Text("Entrate"); Text("Uscite") }
    } }
}

@Composable private fun MovementList(records: List<MoneyRecord>, month: Calendar, previous: () -> Unit, next: () -> Unit, edit: (MoneyRecord) -> Unit, delete: (MoneyRecord) -> Unit) {
    var pending by remember { mutableStateOf<MoneyRecord?>(null) }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        MonthSelector(monthFormat.format(month.time), previous, next)
        Spacer(Modifier.height(8.dp))
        if (records.isEmpty()) Text("Nessun movimento per questo mese.") else LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(records.sortedByDescending { parseDate(it.date)?.timeInMillis ?: 0L }) { r ->
                Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp)) { RecordRow(r); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { TextButton({ edit(r) }) { Text("Modifica") }; TextButton({ pending = r }) { Text("Elimina") } } } }
            }
        }
    }
    pending?.let { r -> AlertDialog(onDismissRequest = { pending = null }, title = { Text("Eliminare il movimento?") }, text = { Text("${r.description.ifBlank { r.category }} — ${money.format(r.amount)}") }, confirmButton = { TextButton({ delete(r); pending = null }) { Text("Elimina") } }, dismissButton = { TextButton({ pending = null }) { Text("Annulla") } }) }
}

@Composable private fun Settings(members: List<String>, add: (String) -> Unit) {
    var value by remember { mutableStateOf("") }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Impostazioni", style = MaterialTheme.typography.headlineSmall); Text("Membri della famiglia", style = MaterialTheme.typography.titleLarge) }
        items(members) { Text(it) }
        item { OutlinedTextField(value, { value = it }, label = { Text("Nuovo membro") }, modifier = Modifier.fillMaxWidth()) }
        item { Button({ add(value); value = "" }, Modifier.fillMaxWidth()) { Text("Aggiungi membro") } }
        item { Text("Dati reali storicizzati. Nessun budget preventivo.") }
    }
}

@Composable private fun RecordEditor(record: MoneyRecord?, members: List<String>, dismiss: () -> Unit, save: (MoneyRecord) -> Unit) {
    val context = LocalContext.current
    var type by remember(record) { mutableStateOf(record?.type ?: RecordType.EXPENSE) }
    var amount by remember(record) { mutableStateOf(record?.amount?.toString() ?: "") }
    var category by remember(record) { mutableStateOf(record?.category ?: expenseCategories.first()) }
    var description by remember(record) { mutableStateOf(record?.description ?: "") }
    var date by remember(record) { mutableStateOf(record?.date ?: dateFormat.format(Date())) }
    var member by remember(record) { mutableStateOf(record?.member ?: members.first()) }
    var payment by remember(record) { mutableStateOf(record?.paymentMethod ?: paymentMethods.first()) }
    AlertDialog(onDismissRequest = dismiss, title = { Text(if (record == null) "Nuovo movimento" else "Modifica movimento") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) { Button({ type = RecordType.EXPENSE; category = expenseCategories.first() }, Modifier.weight(1f)) { Text("Uscita") }; Button({ type = RecordType.INCOME; category = incomeCategories.first() }, Modifier.weight(1f)) { Text("Entrata") } }
            OutlinedTextField(amount, { amount = it }, label = { Text("Importo (€)") }, modifier = Modifier.fillMaxWidth())
            Choice("Categoria", category, context, if (type == RecordType.INCOME) incomeCategories else expenseCategories) { category = it }
            Choice("Membro", member, context, members) { member = it }
            Choice("Pagamento", payment, context, paymentMethods) { payment = it }
            OutlinedTextField(description, { description = it }, label = { Text("Descrizione") }, modifier = Modifier.fillMaxWidth())
            OutlinedButton({ val c = parseDate(date) ?: Calendar.getInstance(); DatePickerDialog(context, { _, y, m, d -> c.set(y, m, d); date = dateFormat.format(c.time) }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show() }, Modifier.fillMaxWidth()) { Text("Data: $date") }
        }
    }, confirmButton = { TextButton(enabled = amount.replace(',', '.').toDoubleOrNull()?.let { it > 0 } == true, onClick = { save(MoneyRecord(record?.id ?: System.currentTimeMillis(), type, amount.replace(',', '.').toDouble(), category, description.trim(), date, member, payment)) }) { Text("Salva") } }, dismissButton = { TextButton(dismiss) { Text("Annulla") } })
}

@Composable private fun Choice(label: String, value: String, context: Context, options: List<String>, selected: (String) -> Unit) { OutlinedButton({ android.app.AlertDialog.Builder(context).setTitle(label).setItems(options.toTypedArray()) { _, i -> selected(options[i]) }.show() }, Modifier.fillMaxWidth()) { Text("$label: $value") } }
@Composable private fun RecordRow(r: MoneyRecord) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Column(Modifier.weight(1f)) { Text(r.description.ifBlank { r.category }, style = MaterialTheme.typography.titleMedium); Text("${r.category} • ${r.member} • ${r.date}"); Text(r.paymentMethod, style = MaterialTheme.typography.bodySmall) }; Text(if (r.type == RecordType.INCOME) "+${money.format(r.amount)}" else "-${money.format(r.amount)}") } }
@Composable private fun MonthSelector(label: String, previous: () -> Unit, next: () -> Unit) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { OutlinedButton(previous) { Text("‹") }; Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 10.dp)); OutlinedButton(next) { Text("›") } } }

@Composable private fun Analytics(records: List<MoneyRecord>) {
    val totalIn = records.filter { it.type == RecordType.INCOME }.sumOf { it.amount }
    val totalOut = records.filter { it.type == RecordType.EXPENSE }.sumOf { it.amount }
    val months = records.groupBy { it.date.substring(3) }.toList().sortedByDescending { it.first }
    val categories = records.filter { it.type == RecordType.EXPENSE }.groupBy { it.category }.mapValues { it.value.sumOf(MoneyRecord::amount) }.entries.sortedByDescending { it.value }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Analisi storica", style = MaterialTheme.typography.headlineSmall) }
        item { SummaryCard(totalIn, totalOut, totalIn - totalOut) }
        item { MonthlyChart(records) }
        item { Text("Spese per categoria", style = MaterialTheme.typography.titleLarge) }
        items(categories) { e -> val pct = if (totalOut > 0) e.value * 100 / totalOut else 0.0; Column { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(e.key); Text("${money.format(e.value)}  ${String.format(Locale.ITALIAN, "%.1f", pct)}%") }; LinearProgressIndicator(progress = { (pct / 100).toFloat().coerceIn(0f, 1f) }, Modifier.fillMaxWidth()) } }
        item { Text("Andamento per mese", style = MaterialTheme.typography.titleLarge) }
        items(months.take(24)) { e -> val i = e.second.filter { it.type == RecordType.INCOME }.sumOf { it.amount }; val o = e.second.filter { it.type == RecordType.EXPENSE }.sumOf { it.amount }; Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(e.first); Text("+${money.format(i)} / -${money.format(o)} / ${money.format(i - o)}") } }
    }
}

private fun shiftMonth(c: Calendar, delta: Int) = (c.clone() as Calendar).apply { add(Calendar.MONTH, delta) }
private fun parseDate(v: String) = runCatching { dateFormat.parse(v)?.let { Calendar.getInstance().apply { time = it } } }.getOrNull()
private fun sameMonth(v: String, m: Calendar): Boolean { val d = parseDate(v) ?: return false; return d.get(Calendar.YEAR) == m.get(Calendar.YEAR) && d.get(Calendar.MONTH) == m.get(Calendar.MONTH) }
private fun loadRecords(p: android.content.SharedPreferences): List<MoneyRecord> = runCatching { val a = JSONArray(p.getString(RECORDS, "[]")); List(a.length()) { val o = a.getJSONObject(it); MoneyRecord(o.getLong("id"), if (o.optString("type") == "INCOME") RecordType.INCOME else RecordType.EXPENSE, o.getDouble("amount"), o.optString("category", "Altro"), o.optString("description"), o.optString("date", dateFormat.format(Date())), o.optString("member", "Famiglia"), o.optString("paymentMethod", "Carta")) } }.getOrDefault(emptyList())
private fun saveRecords(p: android.content.SharedPreferences, l: List<MoneyRecord>) { val a = JSONArray(); l.forEach { r -> a.put(JSONObject().apply { put("id", r.id); put("type", r.type.name); put("amount", r.amount); put("category", r.category); put("description", r.description); put("date", r.date); put("member", r.member); put("paymentMethod", r.paymentMethod) }) }; p.edit().putString(RECORDS, a.toString()).apply() }
private fun loadMembers(p: android.content.SharedPreferences): List<String> = runCatching { val a = JSONArray(p.getString(MEMBERS, "[]")); List(a.length()) { a.getString(it) } }.getOrDefault(emptyList())
private fun saveMembers(p: android.content.SharedPreferences, l: List<String>) { val a = JSONArray(); l.forEach { a.put(it) }; p.edit().putString(MEMBERS, a.toString()).apply() }
