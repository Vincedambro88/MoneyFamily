package com.moneyfamily.app

import android.app.DatePickerDialog
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

data class MoneyRecord(
    val id: Long,
    val income: Boolean,
    val amount: Double,
    val category: String,
    val description: String,
    val date: String,
    val member: String,
    val payment: String
)

private val money = NumberFormat.getCurrencyInstance(Locale.ITALY)
private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.ITALY)
private val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.ITALIAN)
private val expenses = listOf("Alimentari", "Casa", "Auto", "Bollette", "Salute", "Figli", "Istruzione", "Abbigliamento", "Tempo libero", "Vacanze", "Sport", "Altro")
private val incomes = listOf("Stipendio", "Bonus", "Rimborso", "Altro")
private val payments = listOf("Contanti", "Carta", "Bonifico", "Addebito", "Altro")
private const val PREFS = "moneyfamily_data"
private const val RECORDS = "records"
private const val MEMBERS = "members"

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
                else -> Settings(members) { name -> if (name.isNotBlank() && name !in members) { members = members + name.trim(); saveMembers(prefs, members) } }
            }
        }
    }

    if (adding || editing != null) {
        RecordEditor(editing, members, { adding = false; editing = null }) { saveRecord(it); adding = false; editing = null }
    }
}

@Composable
private fun Dashboard(records: List<MoneyRecord>, members: List<String>, month: Calendar, previous: () -> Unit, next: () -> Unit, add: () -> Unit) {
    val current = records.filter { sameMonth(it.date, month) }
    val income = current.filter { it.income }.sumOf { it.amount }
    val expense = current.filterNot { it.income }.sumOf { it.amount }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { MonthSelector(monthFormat.format(month.time), previous, next) }
        item { SummaryCard(income, expense, income - expense) }
        item { MonthlyChart(records) }
        item { Text("Spese per categoria", style = MaterialTheme.typography.titleLarge) }
        items(current.filterNot { it.income }.groupBy { it.category }.entries.sortedByDescending { it.value.sumOf(MoneyRecord::amount) }) { entry ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(entry.key); Text(money.format(entry.value.sumOf(MoneyRecord::amount))) }
        }
        item { Text("Per membro", style = MaterialTheme.typography.titleLarge) }
        items(members) { member ->
            val out = current.filter { !it.income && it.member == member }.sumOf { it.amount }
            val inc = current.filter { it.income && it.member == member }.sumOf { it.amount }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(member); Text("+${money.format(inc)} / -${money.format(out)}") }
        }
        item { Button(add, Modifier.fillMaxWidth()) { Text("+ Inserisci movimento") } }
    }
}

@Composable
private fun SummaryCard(income: Double, expense: Double, balance: Double) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp)) {
            Text("Riepilogo reale", style = MaterialTheme.typography.titleMedium)
            Text("Entrate  ${money.format(income)}")
            Text("Uscite  ${money.format(expense)}")
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            Text("Saldo  ${money.format(balance)}", style = MaterialTheme.typography.headlineSmall)
        }
    }
}

@Composable
private fun MonthlyChart(records: List<MoneyRecord>) {
    val groups = records.groupBy { it.date.takeLast(7) }.toList().sortedBy { it.first }.takeLast(6)
    if (groups.isEmpty()) return
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Andamento ultimi mesi", style = MaterialTheme.typography.titleMedium)
            Canvas(Modifier.fillMaxWidth().height(180.dp)) {
                val max = groups.flatMap { (_, values) -> listOf(values.filter { it.income }.sumOf { it.amount }, values.filterNot { it.income }.sumOf { it.amount }) }.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0
                val width = size.width / groups.size
                groups.forEachIndexed { index, pair ->
                    val inc = pair.second.filter { it.income }.sumOf { it.amount }
                    val out = pair.second.filterNot { it.income }.sumOf { it.amount }
                    val x = index * width + width / 2
                    drawLine(Color(0xFF2E7D32), Offset(x, size.height), Offset(x, size.height * (1 - inc / max)), strokeWidth = 18f)
                    drawLine(Color(0xFFC62828), Offset(x + 24, size.height), Offset(x + 24, size.height * (1 - out / max)), strokeWidth = 18f)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) { Text("Entrate"); Text("Uscite") }
        }
    }
}

@Composable
private fun MovementList(records: List<MoneyRecord>, month: Calendar, previous: () -> Unit, next: () -> Unit, edit: (MoneyRecord) -> Unit, delete: (MoneyRecord) -> Unit) {
    var pending by remember { mutableStateOf<MoneyRecord?>(null) }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        MonthSelector(monthFormat.format(month.time), previous, next)
        Spacer(Modifier.height(8.dp))
        if (records.isEmpty()) Text("Nessun movimento per questo mese.")
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(records.sortedByDescending { it.date }) { record ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(Modifier.weight(1f)) { Text(record.description.ifBlank { record.category }); Text("${record.category} • ${record.member} • ${record.date}") }
                            Text(if (record.income) "+${money.format(record.amount)}" else "-${money.format(record.amount)}")
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { TextButton({ edit(record) }) { Text("Modifica") }; TextButton({ pending = record }) { Text("Elimina") } }
                    }
                }
            }
        }
    }
    pending?.let { record -> AlertDialog(onDismissRequest = { pending = null }, title = { Text("Eliminare movimento?") }, text = { Text(money.format(record.amount)) }, confirmButton = { TextButton({ delete(record); pending = null }) { Text("Elimina") } }, dismissButton = { TextButton({ pending = null }) { Text("Annulla") } }) }
}

@Composable
private fun Analytics(records: List<MoneyRecord>) {
    val income = records.filter { it.income }.sumOf { it.amount }
    val expense = records.filterNot { it.income }.sumOf { it.amount }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Analisi storica", style = MaterialTheme.typography.headlineSmall) }
        item { SummaryCard(income, expense, income - expense) }
        item { MonthlyChart(records) }
        item { Text("Storico mensile", style = MaterialTheme.typography.titleLarge) }
        items(records.groupBy { it.date.takeLast(7) }.entries.sortedByDescending { it.key }) { entry ->
            val inc = entry.value.filter { it.income }.sumOf { it.amount }
            val out = entry.value.filterNot { it.income }.sumOf { it.amount }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(entry.key); Text("+${money.format(inc)} / -${money.format(out)} / ${money.format(inc - out)}") }
        }
    }
}

@Composable
private fun Settings(members: List<String>, add: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Impostazioni", style = MaterialTheme.typography.headlineSmall)
        Text("Membri della famiglia", style = MaterialTheme.typography.titleLarge)
        members.forEach { Text(it) }
        OutlinedTextField(name, { name = it }, label = { Text("Nuovo membro") }, modifier = Modifier.fillMaxWidth())
        Button({ add(name); name = "" }, Modifier.fillMaxWidth()) { Text("Aggiungi") }
        Text("Dati reali storicizzati. Nessun budget preventivo.")
    }
}

@Composable
private fun RecordEditor(record: MoneyRecord?, members: List<String>, dismiss: () -> Unit, save: (MoneyRecord) -> Unit) {
    val context = LocalContext.current
    var income by remember(record) { mutableStateOf(record?.income ?: false) }
    var amount by remember(record) { mutableStateOf(record?.amount?.toString() ?: "") }
    var category by remember(record) { mutableStateOf(record?.category ?: expenses.first()) }
    var description by remember(record) { mutableStateOf(record?.description ?: "") }
    var date by remember(record) { mutableStateOf(record?.date ?: dateFormat.format(Date())) }
    var member by remember(record) { mutableStateOf(record?.member ?: members.first()) }
    var payment by remember(record) { mutableStateOf(record?.payment ?: payments.first()) }
    AlertDialog(onDismissRequest = dismiss, title = { Text(if (record == null) "Nuovo movimento" else "Modifica movimento") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) { Button({ income = false; category = expenses.first() }, Modifier.weight(1f)) { Text("Uscita") }; Button({ income = true; category = incomes.first() }, Modifier.weight(1f)) { Text("Entrata") } }
            OutlinedTextField(amount, { amount = it }, label = { Text("Importo (€)") }, modifier = Modifier.fillMaxWidth())
            Choice("Categoria", category, context, if (income) incomes else expenses) { category = it }
            Choice("Membro", member, context, members) { member = it }
            Choice("Pagamento", payment, context, payments) { payment = it }
            OutlinedTextField(description, { description = it }, label = { Text("Descrizione") }, modifier = Modifier.fillMaxWidth())
            OutlinedButton({
                val c = parseDate(date) ?: Calendar.getInstance()
                DatePickerDialog(context, { _, y, m, d -> c.set(y, m, d); date = dateFormat.format(c.time) }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
            }, Modifier.fillMaxWidth()) { Text("Data: $date") }
        }
    }, confirmButton = {
        TextButton(enabled = amount.replace(',', '.').toDoubleOrNull()?.let { it > 0 } == true, onClick = { save(MoneyRecord(record?.id ?: System.currentTimeMillis(), income, amount.replace(',', '.').toDouble(), category, description.trim(), date, member, payment)) }) { Text("Salva") }
    }, dismissButton = { TextButton(dismiss) { Text("Annulla") } })
}

@Composable
private fun Choice(label: String, value: String, context: Context, options: List<String>, selected: (String) -> Unit) {
    OutlinedButton({ android.app.AlertDialog.Builder(context).setTitle(label).setItems(options.toTypedArray()) { _, index -> selected(options[index]) }.show() }, Modifier.fillMaxWidth()) { Text("$label: $value") }
}

@Composable
private fun MonthSelector(label: String, previous: () -> Unit, next: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { OutlinedButton(previous) { Text("‹") }; Text(label.replaceFirstChar { it.uppercase() }, modifier = Modifier.padding(top = 10.dp)); OutlinedButton(next) { Text("›") } }
}

private fun shiftMonth(calendar: Calendar, delta: Int): Calendar = (calendar.clone() as Calendar).apply { add(Calendar.MONTH, delta) }
private fun parseDate(value: String): Calendar? = runCatching { dateFormat.parse(value)?.let { Calendar.getInstance().apply { time = it } } }.getOrNull()
private fun sameMonth(value: String, month: Calendar): Boolean { val date = parseDate(value) ?: return false; return date.get(Calendar.YEAR) == month.get(Calendar.YEAR) && date.get(Calendar.MONTH) == month.get(Calendar.MONTH) }

private fun loadRecords(prefs: android.content.SharedPreferences): List<MoneyRecord> = runCatching {
    val array = JSONArray(prefs.getString(RECORDS, "[]"))
    List(array.length()) { i -> val o = array.getJSONObject(i); MoneyRecord(o.getLong("id"), o.optBoolean("income"), o.getDouble("amount"), o.optString("category", "Altro"), o.optString("description"), o.optString("date", dateFormat.format(Date())), o.optString("member", "Famiglia"), o.optString("payment", "Carta")) }
}.getOrDefault(emptyList())

private fun saveRecords(prefs: android.content.SharedPreferences, records: List<MoneyRecord>) {
    val array = JSONArray()
    records.forEach { r -> array.put(JSONObject().apply { put("id", r.id); put("income", r.income); put("amount", r.amount); put("category", r.category); put("description", r.description); put("date", r.date); put("member", r.member); put("payment", r.payment) }) }
    prefs.edit().putString(RECORDS, array.toString()).apply()
}

private fun loadMembers(prefs: android.content.SharedPreferences): List<String> = runCatching {
    val array = JSONArray(prefs.getString(MEMBERS, "[]")); List(array.length()) { array.getString(it) }
}.getOrDefault(emptyList())

private fun saveMembers(prefs: android.content.SharedPreferences, members: List<String>) {
    val array = JSONArray(); members.forEach { array.put(it) }; prefs.edit().putString(MEMBERS, array.toString()).apply()
}
