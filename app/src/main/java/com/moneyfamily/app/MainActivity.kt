package com.moneyfamily.app

import android.app.DatePickerDialog
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
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
private const val EXPENSES_KEY = "expenses"
private const val MEMBERS_KEY = "members"
private const val BUDGETS_KEY = "monthly_budgets"
private const val LEGACY_BUDGET_KEY = "budget"
private val CATEGORIES = listOf("Alimentari", "Casa", "Auto", "Bollette", "Salute", "Figli", "Istruzione", "Abbigliamento", "Tempo libero", "Vacanze", "Sport", "Altro")
private val PAYMENT_METHODS = listOf("Contanti", "Carta", "Bonifico", "Addebito", "Altro")
private val money = NumberFormat.getCurrencyInstance(Locale.ITALY)
private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.ITALY)
private val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.ITALIAN)

data class Expense(val id: Long, val amount: Double, val category: String, val description: String, val date: String, val member: String, val paymentMethod: String)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { MoneyFamilyApp() } }
}

@Composable
private fun MoneyFamilyApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }
    var expenses by remember { mutableStateOf(loadExpenses(prefs)) }
    var members by remember { mutableStateOf(loadMembers(prefs).ifEmpty { listOf("Famiglia", "Papà", "Mamma", "Figlio 1", "Figlio 2") }) }
    var month by remember { mutableStateOf(Calendar.getInstance()) }
    var tab by remember { mutableStateOf(0) }
    var editing by remember { mutableStateOf<Expense?>(null) }
    var editor by remember { mutableStateOf(false) }
    if (loadMembers(prefs).isEmpty()) saveMembers(prefs, members)
    val monthExpenses = expenses.filter { sameMonth(it.date, month) }
    val total = monthExpenses.sumOf { it.amount }
    val budget = loadBudget(prefs, month)
    val label = monthFormat.format(month.time).replaceFirstChar { it.uppercase() }

    MaterialTheme {
        Scaffold(bottomBar = {
            NavigationBar {
                listOf("Home", "Spese", "Budget", "Impostazioni").forEachIndexed { i, title ->
                    NavigationBarItem(selected = tab == i, onClick = { tab = i }, icon = { Text(title.take(1)) }, label = { Text(title) })
                }
            }
        }) { padding ->
            Column(Modifier.fillMaxSize().padding(padding)) {
                Text("MoneyFamily", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(16.dp))
                when (tab) {
                    0 -> Dashboard(label, total, budget, monthExpenses, members, { month = previousMonth(month) }, { month = nextMonth(month) }, { editing = null; editor = true })
                    1 -> History(label, monthExpenses, { month = previousMonth(month) }, { month = nextMonth(month) }, { editing = it; editor = true }, { item -> expenses = expenses.filterNot { it.id == item.id }; saveExpenses(prefs, expenses) })
                    2 -> BudgetScreen(label, total, budget) { saveBudget(prefs, month, it) }
                    else -> Settings(members, { value -> val clean = value.trim(); if (clean.isNotEmpty() && clean !in members) { members += clean; saveMembers(prefs, members) } }, { value -> if (members.size > 1) { members = members.filterNot { it == value }; saveMembers(prefs, members) } })
                }
            }
        }
        if (editor) ExpenseEditor(editing, members, { editor = false }, { item -> expenses = if (editing == null) listOf(item) + expenses else expenses.map { if (it.id == item.id) item else it }; saveExpenses(prefs, expenses); editor = false })
    }
}

@Composable
private fun Dashboard(label: String, total: Double, budget: Double, expenses: List<Expense>, members: List<String>, previous: () -> Unit, next: () -> Unit, add: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { MonthSelector(label, previous, next) }
        item { Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) { Text("Totale spese", style = MaterialTheme.typography.titleMedium); Text(money.format(total), style = MaterialTheme.typography.headlineMedium); Text("Budget: ${money.format(budget)}"); Text("Residuo: ${money.format(budget - total)}"); LinearProgressIndicator(progress = { if (budget > 0) (total / budget).coerceIn(0.0, 1.0).toFloat() else 0f }, Modifier.fillMaxWidth()) } } }
        item { Text("Per categoria", style = MaterialTheme.typography.titleLarge) }
        items(expenses.groupBy { it.category }.entries.sortedByDescending { it.value.sumOf(Expense::amount) }) { entry -> val value = entry.value.sumOf { it.amount }; val percent = if (total == 0.0) 0 else (value / total * 100).toInt(); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("${entry.key} ($percent%)"); Text(money.format(value)) } }
        item { Text("Per membro", style = MaterialTheme.typography.titleLarge) }
        items(members) { member -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(member); Text(money.format(expenses.filter { it.member == member }.sumOf { it.amount })) } }
        item { Text("Ultime spese", style = MaterialTheme.typography.titleLarge) }
        items(expenses.sortedByDescending { parseDate(it.date)?.timeInMillis ?: 0L }.take(5)) { ExpenseRow(it) }
        item { OutlinedButton(onClick = add, Modifier.fillMaxWidth()) { Text("+ Inserisci spesa") } }
    }
}

@Composable
private fun History(label: String, expenses: List<Expense>, previous: () -> Unit, next: () -> Unit, edit: (Expense) -> Unit, delete: (Expense) -> Unit) {
    var pending by remember { mutableStateOf<Expense?>(null) }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        MonthSelector(label, previous, next); Spacer(Modifier.height(10.dp))
        if (expenses.isEmpty()) Text("Nessuna spesa per questo mese.") else LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) { items(expenses.sortedByDescending { parseDate(it.date)?.timeInMillis ?: 0L }) { e -> Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp)) { ExpenseRow(e); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { TextButton({ edit(e) }) { Text("Modifica") }; TextButton({ pending = e }) { Text("Elimina") } } } } } }
    }
    pending?.let { e -> AlertDialog(onDismissRequest = { pending = null }, title = { Text("Eliminare la spesa?") }, text = { Text("${e.description.ifBlank { e.category }} — ${money.format(e.amount)}") }, confirmButton = { TextButton({ delete(e); pending = null }) { Text("Elimina") } }, dismissButton = { TextButton({ pending = null }) { Text("Annulla") } }) }
}

@Composable private fun ExpenseRow(e: Expense) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Column(Modifier.weight(1f)) { Text(e.description.ifBlank { e.category }, style = MaterialTheme.typography.titleMedium); Text("${e.category} • ${e.member} • ${e.date}"); Text(e.paymentMethod, style = MaterialTheme.typography.bodySmall) }; Spacer(Modifier.width(8.dp)); Text(money.format(e.amount), style = MaterialTheme.typography.titleMedium) } }

@Composable
private fun BudgetScreen(label: String, total: Double, budget: Double, save: (Double) -> Unit) {
    var value by remember(budget) { mutableStateOf(String.format(Locale.US, "%.2f", budget)) }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        MonthSelector(label, {}, {}); Text("Budget mensile", style = MaterialTheme.typography.headlineSmall)
        OutlinedTextField(value, { value = it }, label = { Text("Budget (€)") }, modifier = Modifier.fillMaxWidth())
        Button({ value.replace(',', '.').toDoubleOrNull()?.takeIf { it >= 0 }?.let(save) }, Modifier.fillMaxWidth()) { Text("Salva budget per questo mese") }
        Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Text("Speso: ${money.format(total)}"); Text("Residuo: ${money.format(budget - total)}"); LinearProgressIndicator(progress = { if (budget > 0) (total / budget).coerceIn(0.0, 1.0).toFloat() else 0f }, Modifier.fillMaxWidth()) } }
    }
}

@Composable
private fun Settings(members: List<String>, add: (String) -> Unit, remove: (String) -> Unit) {
    var value by remember { mutableStateOf("") }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Impostazioni", style = MaterialTheme.typography.headlineSmall); Text("Membri della famiglia", style = MaterialTheme.typography.titleLarge) }
        items(members) { member -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(member); if (members.size > 1) TextButton({ remove(member) }) { Text("Rimuovi") } } }
        item { HorizontalDivider() }
        item { OutlinedTextField(value, { value = it }, label = { Text("Nuovo membro") }, modifier = Modifier.fillMaxWidth()) }
        item { Button({ add(value); value = "" }, Modifier.fillMaxWidth()) { Text("Aggiungi membro") } }
        item { Text("Categorie disponibili", style = MaterialTheme.typography.titleLarge); Text(CATEGORIES.joinToString(" • ")); Text("Metodi di pagamento", style = MaterialTheme.typography.titleLarge); Text(PAYMENT_METHODS.joinToString(" • ")) }
    }
}

@Composable
private fun ExpenseEditor(expense: Expense?, members: List<String>, dismiss: () -> Unit, save: (Expense) -> Unit) {
    val context = LocalContext.current
    var amount by remember(expense) { mutableStateOf(expense?.amount?.toString() ?: "") }; var category by remember(expense) { mutableStateOf(expense?.category ?: CATEGORIES.first()) }; var description by remember(expense) { mutableStateOf(expense?.description ?: "") }; var date by remember(expense) { mutableStateOf(expense?.date ?: dateFormat.format(Date())) }; var member by remember(expense) { mutableStateOf(expense?.member ?: members.firstOrNull().orEmpty()) }; var payment by remember(expense) { mutableStateOf(expense?.paymentMethod ?: PAYMENT_METHODS.first()) }
    AlertDialog(onDismissRequest = dismiss, title = { Text(if (expense == null) "Nuova spesa" else "Modifica spesa") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(amount, { amount = it }, label = { Text("Importo (€)") }, modifier = Modifier.fillMaxWidth())
        SelectButton("Categoria", category) { showOptions(context, CATEGORIES, category) { category = it } }
        SelectButton("Membro", member) { showOptions(context, members, member) { member = it } }
        SelectButton("Pagamento", payment) { showOptions(context, PAYMENT_METHODS, payment) { payment = it } }
        OutlinedTextField(description, { description = it }, label = { Text("Descrizione") }, modifier = Modifier.fillMaxWidth())
        OutlinedButton({ val cal = parseDate(date) ?: Calendar.getInstance(); DatePickerDialog(context, { _, y, m, d -> cal.set(y, m, d); date = dateFormat.format(cal.time) }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show() }, Modifier.fillMaxWidth()) { Text("Data: $date") }
    } }, confirmButton = { TextButton(enabled = amount.replace(',', '.').toDoubleOrNull()?.let { it > 0 } == true, onClick = { save(Expense(expense?.id ?: System.currentTimeMillis(), amount.replace(',', '.').toDouble(), category, description.trim(), date, member, payment)) }) { Text("Salva") } }, dismissButton = { TextButton(dismiss) { Text("Annulla") } })
}

@Composable private fun SelectButton(label: String, value: String, onClick: () -> Unit) { OutlinedButton(onClick, Modifier.fillMaxWidth()) { Text("$label: $value") } }

private fun showOptions(context: Context, options: List<String>, current: String, onSelected: (String) -> Unit) {
    android.app.AlertDialog.Builder(context).setTitle("Seleziona").setSingleChoiceItems(options.toTypedArray(), options.indexOf(current).coerceAtLeast(0)) { dialog, which -> onSelected(options[which]); dialog.dismiss() }.setNegativeButton("Annulla", null).show()
}

@Composable private fun MonthSelector(label: String, previous: () -> Unit, next: () -> Unit) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { OutlinedButton(previous) { Text("‹") }; Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 10.dp)); OutlinedButton(next) { Text("›") } } }

private fun loadExpenses(prefs: android.content.SharedPreferences): List<Expense> = try { val a = JSONArray(prefs.getString(EXPENSES_KEY, "[]")); buildList { for (i in 0 until a.length()) { val o = a.getJSONObject(i); add(Expense(o.getLong("id"), o.getDouble("amount"), o.optString("category", "Altro"), o.optString("description"), o.optString("date", dateFormat.format(Date())), o.optString("member", "Famiglia"), o.optString("paymentMethod", "Carta"))) } } } catch (_: Exception) { emptyList() }
private fun saveExpenses(prefs: android.content.SharedPreferences, list: List<Expense>) { val a = JSONArray(); list.forEach { e -> a.put(JSONObject().apply { put("id", e.id); put("amount", e.amount); put("category", e.category); put("description", e.description); put("date", e.date); put("member", e.member); put("paymentMethod", e.paymentMethod) }) }; prefs.edit().putString(EXPENSES_KEY, a.toString()).apply() }
private fun loadMembers(prefs: android.content.SharedPreferences): List<String> = try { val a = JSONArray(prefs.getString(MEMBERS_KEY, "[]")); List(a.length()) { a.getString(it) } } catch (_: Exception) { emptyList() }
private fun saveMembers(prefs: android.content.SharedPreferences, list: List<String>) { val a = JSONArray(); list.forEach(a::put); prefs.edit().putString(MEMBERS_KEY, a.toString()).apply() }
private fun budgetKey(month: Calendar) = String.format(Locale.US, "%04d-%02d", month.get(Calendar.YEAR), month.get(Calendar.MONTH) + 1)
private fun loadBudget(prefs: android.content.SharedPreferences, month: Calendar): Double { val key = budgetKey(month); val raw = prefs.getString(BUDGETS_KEY, "") ?: ""; return raw.split(";").firstOrNull { it.startsWith("$key=") }?.substringAfter("=")?.toDoubleOrNull() ?: prefs.getFloat(LEGACY_BUDGET_KEY, 3000f).toDouble() }
private fun saveBudget(prefs: android.content.SharedPreferences, month: Calendar, value: Double) { val key = budgetKey(month); val old = (prefs.getString(BUDGETS_KEY, "") ?: "").split(";").filter { it.isNotBlank() && !it.startsWith("$key=") }.toMutableList(); old.add("$key=$value"); prefs.edit().putString(BUDGETS_KEY, old.joinToString(";")).putFloat(LEGACY_BUDGET_KEY, value.toFloat()).apply() }
private fun parseDate(value: String): Calendar? = try { dateFormat.parse(value)?.let { Calendar.getInstance().apply { time = it } } } catch (_: Exception) { null }
private fun sameMonth(value: String, month: Calendar): Boolean { val d = parseDate(value) ?: return false; return d.get(Calendar.YEAR) == month.get(Calendar.YEAR) && d.get(Calendar.MONTH) == month.get(Calendar.MONTH) }
private fun previousMonth(c: Calendar) = (c.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
private fun nextMonth(c: Calendar) = (c.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
