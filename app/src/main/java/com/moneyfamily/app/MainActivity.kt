package com.moneyfamily.app

import android.app.DatePickerDialog
import android.app.AlertDialog as AndroidAlertDialog
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private const val PREFS = "moneyfamily_data"
private const val RECORDS_KEY = "records"
private const val OLD_EXPENSES_KEY = "expenses"
private const val MEMBERS_KEY = "members"
private val CATEGORIES = listOf("Alimentari", "Casa", "Auto", "Bollette", "Salute", "Figli", "Istruzione", "Abbigliamento", "Tempo libero", "Vacanze", "Sport", "Stipendio", "Bonus", "Rimborso", "Altro")
private val PAYMENT_METHODS = listOf("Contanti", "Carta", "Bonifico", "Addebito", "Altro")
private val money = NumberFormat.getCurrencyInstance(Locale.ITALY)
private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.ITALY)
private val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.ITALIAN)

enum class RecordType { EXPENSE, INCOME }

data class MoneyRecord(val id: Long, val type: RecordType, val amount: Double, val category: String, val description: String, val date: String, val member: String, val paymentMethod: String)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { MoneyFamilyApp() } }
}

@Composable
private fun MoneyFamilyApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }
    var records by remember { mutableStateOf(loadRecords(prefs)) }
    var members by remember { mutableStateOf(loadMembers(prefs).ifEmpty { listOf("Famiglia", "Papà", "Mamma", "Figlio 1", "Figlio 2") }) }
    var month by remember { mutableStateOf(Calendar.getInstance()) }
    var tab by remember { mutableStateOf(0) }
    var editing by remember { mutableStateOf<MoneyRecord?>(null) }
    var editor by remember { mutableStateOf(false) }
    if (loadMembers(prefs).isEmpty()) saveMembers(prefs, members)
    val monthRecords = records.filter { sameMonth(it.date, month) }
    val income = monthRecords.filter { it.type == RecordType.INCOME }.sumOf { it.amount }
    val expense = monthRecords.filter { it.type == RecordType.EXPENSE }.sumOf { it.amount }
    val net = income - expense
    val label = monthFormat.format(month.time).replaceFirstChar { it.uppercase() }

    MaterialTheme {
        Scaffold(bottomBar = {
            NavigationBar {
                listOf("Home", "Movimenti", "Impostazioni").forEachIndexed { i, title ->
                    NavigationBarItem(selected = tab == i, onClick = { tab = i }, icon = { Text(title.take(1)) }, label = { Text(title) })
                }
            }
        }) { padding ->
            Column(Modifier.fillMaxSize().padding(padding)) {
                Text("MoneyFamily", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(16.dp))
                when (tab) {
                    0 -> Dashboard(label, income, expense, net, monthRecords, members, { month = previousMonth(month) }, { month = nextMonth(month) }, { editing = null; editor = true })
                    1 -> History(label, monthRecords, { month = previousMonth(month) }, { month = nextMonth(month) }, { editing = it; editor = true }, { item -> records = records.filterNot { it.id == item.id }; saveRecords(prefs, records) })
                    else -> Settings(members, { value -> val clean = value.trim(); if (clean.isNotEmpty() && clean !in members) { members += clean; saveMembers(prefs, members) } }, { value -> if (members.size > 1) { members = members.filterNot { it == value }; saveMembers(prefs, members) } })
                }
            }
        }
        if (editor) RecordEditor(editing, members, { editor = false }, { item -> records = if (editing == null) listOf(item) + records else records.map { if (it.id == item.id) item else it }; saveRecords(prefs, records); editor = false })
    }
}

@Composable
private fun Dashboard(label: String, income: Double, expense: Double, net: Double, records: List<MoneyRecord>, members: List<String>, previous: () -> Unit, next: () -> Unit, add: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { MonthSelector(label, previous, next) }
        item { Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) { Text("Riepilogo reale del mese", style = MaterialTheme.typography.titleMedium); Text("Entrate  ${money.format(income)}"); Text("Uscite  ${money.format(expense)}"); HorizontalDivider(); Text("Saldo  ${money.format(net)}", style = MaterialTheme.typography.headlineSmall) } } }
        item { Text("Uscite per categoria", style = MaterialTheme.typography.titleLarge) }
        items(records.filter { it.type == RecordType.EXPENSE }.groupBy { it.category }.entries.sortedByDescending { it.value.sumOf(MoneyRecord::amount) }) { entry -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(entry.key); Text(money.format(entry.value.sumOf { it.amount })) } }
        item { Text("Movimenti per membro", style = MaterialTheme.typography.titleLarge) }
        items(members) { member -> val inTotal = records.filter { it.member == member && it.type == RecordType.INCOME }.sumOf { it.amount }; val outTotal = records.filter { it.member == member && it.type == RecordType.EXPENSE }.sumOf { it.amount }; Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(member); Text("+${money.format(inTotal)} / -${money.format(outTotal)}") } }
        item { Text("Ultimi movimenti", style = MaterialTheme.typography.titleLarge) }
        items(records.sortedByDescending { parseDate(it.date)?.timeInMillis ?: 0L }.take(8)) { RecordRow(it) }
        item { OutlinedButton(onClick = add, Modifier.fillMaxWidth()) { Text("+ Inserisci movimento") } }
    }
}

@Composable
private fun History(label: String, records: List<MoneyRecord>, previous: () -> Unit, next: () -> Unit, edit: (MoneyRecord) -> Unit, delete: (MoneyRecord) -> Unit) {
    var pending by remember { mutableStateOf<MoneyRecord?>(null) }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        MonthSelector(label, previous, next); Spacer(Modifier.height(10.dp))
        if (records.isEmpty()) Text("Nessun movimento per questo mese.") else LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) { items(records.sortedByDescending { parseDate(it.date)?.timeInMillis ?: 0L }) { r -> Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp)) { RecordRow(r); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { TextButton({ edit(r) }) { Text("Modifica") }; TextButton({ pending = r }) { Text("Elimina") } } } } } }
    }
    pending?.let { r -> AlertDialog(onDismissRequest = { pending = null }, title = { Text("Eliminare il movimento?") }, text = { Text("${r.description.ifBlank { r.category }} — ${money.format(r.amount)}") }, confirmButton = { TextButton({ delete(r); pending = null }) { Text("Elimina") } }, dismissButton = { TextButton({ pending = null }) { Text("Annulla") } }) }
}

@Composable private fun RecordRow(r: MoneyRecord) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Column(Modifier.weight(1f)) { Text(r.description.ifBlank { r.category }, style = MaterialTheme.typography.titleMedium); Text("${r.category} • ${r.member} • ${r.date}"); Text(if (r.type == RecordType.INCOME) "ENTRATA • ${r.paymentMethod}" else "USCITA • ${r.paymentMethod}", style = MaterialTheme.typography.bodySmall) }; Spacer(Modifier.width(8.dp)); Text(if (r.type == RecordType.INCOME) "+${money.format(r.amount)}" else "-${money.format(r.amount)}", style = MaterialTheme.typography.titleMedium) } }

@Composable
private fun Settings(members: List<String>, add: (String) -> Unit, remove: (String) -> Unit) {
    var value by remember { mutableStateOf("") }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Impostazioni", style = MaterialTheme.typography.headlineSmall); Text("Membri della famiglia", style = MaterialTheme.typography.titleLarge) }
        items(members) { member -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(member); if (members.size > 1) TextButton({ remove(member) }) { Text("Rimuovi") } } }
        item { HorizontalDivider() }
        item { OutlinedTextField(value, { value = it }, label = { Text("Nuovo membro") }, modifier = Modifier.fillMaxWidth()) }
        item { Button({ add(value); value = "" }, Modifier.fillMaxWidth()) { Text("Aggiungi membro") } }
        item { Text("L'app storicizza movimenti reali: entrate e uscite. Non viene più utilizzato un budget preventivo.", style = MaterialTheme.typography.bodyMedium) }
    }
}

@Composable
private fun RecordEditor(record: MoneyRecord?, members: List<String>, dismiss: () -> Unit, save: (MoneyRecord) -> Unit) {
    val context = LocalContext.current
    var type by remember(record) { mutableStateOf(record?.type ?: RecordType.EXPENSE) }; var amount by remember(record) { mutableStateOf(record?.amount?.toString() ?: "") }; var category by remember(record) { mutableStateOf(record?.category ?: if (type == RecordType.INCOME) "Stipendio" else "Alimentari") }; var description by remember(record) { mutableStateOf(record?.description ?: "") }; var date by remember(record) { mutableStateOf(record?.date ?: dateFormat.format(Date())) }; var member by remember(record) { mutableStateOf(record?.member ?: members.firstOrNull().orEmpty()) }; var payment by remember(record) { mutableStateOf(record?.paymentMethod ?: PAYMENT_METHODS.first()) }
    AlertDialog(onDismissRequest = dismiss, title = { Text(if (record == null) "Nuovo movimento" else "Modifica movimento") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedButton({ type = RecordType.EXPENSE }, Modifier.weight(1f)) { Text(if (type == RecordType.EXPENSE) "✓ Uscita" else "Uscita") }; OutlinedButton({ type = RecordType.INCOME }, Modifier.weight(1f)) { Text(if (type == RecordType.INCOME) "✓ Entrata" else "Entrata") } }
        OutlinedTextField(amount, { amount = it }, label = { Text("Importo (€)") }, modifier = Modifier.fillMaxWidth())
        SelectButton("Categoria", category) { showOptions(context, if (type == RecordType.INCOME) CATEGORIES.filter { it in listOf("Stipendio", "Bonus", "Rimborso", "Altro") } else CATEGORIES.filter { it !in listOf("Stipendio", "Bonus", "Rimborso") }, category) { category = it } }
        SelectButton("Membro", member) { showOptions(context, members, member) { member = it } }
        SelectButton("Pagamento", payment) { showOptions(context, PAYMENT_METHODS, payment) { payment = it } }
        OutlinedTextField(description, { description = it }, label = { Text("Descrizione") }, modifier = Modifier.fillMaxWidth())
        OutlinedButton({ val cal = parseDate(date) ?: Calendar.getInstance(); DatePickerDialog(context, { _, y, m, d -> cal.set(y, m, d); date = dateFormat.format(cal.time) }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show() }, Modifier.fillMaxWidth()) { Text("Data: $date") }
    } }, confirmButton = { TextButton(enabled = amount.replace(',', '.').toDoubleOrNull()?.let { it > 0 } == true, onClick = { save(MoneyRecord(record?.id ?: System.currentTimeMillis(), type, amount.replace(',', '.').toDouble(), category, description.trim(), date, member, payment)) }) { Text("Salva") } }, dismissButton = { TextButton(dismiss) { Text("Annulla") } })
}

@Composable private fun SelectButton(label: String, value: String, onClick: () -> Unit) { OutlinedButton(onClick, Modifier.fillMaxWidth()) { Text("$label: $value") } }
private fun showOptions(context: Context, options: List<String>, current: String, onSelected: (String) -> Unit) { AndroidAlertDialog.Builder(context).setTitle("Seleziona").setSingleChoiceItems(options.toTypedArray(), options.indexOf(current).coerceAtLeast(0)) { dialog, which -> onSelected(options[which]); dialog.dismiss() }.setNegativeButton("Annulla", null).show() }
@Composable private fun MonthSelector(label: String, previous: () -> Unit, next: () -> Unit) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { OutlinedButton(previous) { Text("‹") }; Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 10.dp)); OutlinedButton(next) { Text("›") } } }

private fun loadRecords(prefs: android.content.SharedPreferences): List<MoneyRecord> {
    try { val a = JSONArray(prefs.getString(RECORDS_KEY, "[]")); return buildList { for (i in 0 until a.length()) { val o = a.getJSONObject(i); add(MoneyRecord(o.getLong("id"), if (o.optString("type") == "INCOME") RecordType.INCOME else RecordType.EXPENSE, o.getDouble("amount"), o.optString("category", "Altro"), o.optString("description"), o.optString("date", dateFormat.format(Date())), o.optString("member", "Famiglia"), o.optString("paymentMethod", "Carta"))) } } } catch (_: Exception) { }
    return loadLegacyExpenses(prefs)
}
private fun loadLegacyExpenses(prefs: android.content.SharedPreferences): List<MoneyRecord> = try { val a = JSONArray(prefs.getString(OLD_EXPENSES_KEY, "[]")); buildList { for (i in 0 until a.length()) { val o = a.getJSONObject(i); add(MoneyRecord(o.getLong("id"), RecordType.EXPENSE, o.getDouble("amount"), o.optString("category", "Altro"), o.optString("description"), o.optString("date", dateFormat.format(Date())), o.optString("member", "Famiglia"), o.optString("paymentMethod", "Carta"))) } } } catch (_: Exception) { emptyList() }
private fun saveRecords(prefs: android.content.SharedPreferences, list: List<MoneyRecord>) { val a = JSONArray(); list.forEach { r -> a.put(JSONObject().apply { put("id", r.id); put("type", r.type.name); put("amount", r.amount); put("category", r.category); put("description", r.description); put("date", r.date); put("member", r.member); put("paymentMethod", r.paymentMethod) }) }; prefs.edit().putString(RECORDS_KEY, a.toString()).remove(OLD_EXPENSES_KEY).remove("monthly_budgets").remove("budget").apply() }
private fun loadMembers(prefs: android.content.SharedPreferences): List<String> = try { val a = JSONArray(prefs.getString(MEMBERS_KEY, "[]")); List(a.length()) { a.getString(it) } } catch (_: Exception) { emptyList() }
private fun saveMembers(prefs: android.content.SharedPreferences, list: List<String>) { val a = JSONArray(); list.forEach(a::put); prefs.edit().putString(MEMBERS_KEY, a.toString()).apply() }
private fun parseDate(value: String): Calendar? = try { dateFormat.parse(value)?.let { Calendar.getInstance().apply { time = it } } } catch (_: Exception) { null }
private fun sameMonth(value: String, month: Calendar): Boolean { val d = parseDate(value) ?: return false; return d.get(Calendar.YEAR) == month.get(Calendar.YEAR) && d.get(Calendar.MONTH) == month.get(Calendar.MONTH) }
private fun previousMonth(c: Calendar) = (c.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
private fun nextMonth(c: Calendar) = (c.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
