package com.moneyfamily.app

import android.app.DatePickerDialog
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
private const val BUDGET_KEY = "budget"
private val DEFAULT_CATEGORIES = listOf("Alimentari", "Casa", "Auto", "Bollette", "Salute", "Figli", "Istruzione", "Abbigliamento", "Tempo libero", "Vacanze", "Sport", "Altro")
private val PAYMENT_METHODS = listOf("Contanti", "Carta", "Bonifico", "Addebito", "Altro")
private val moneyFormat: NumberFormat = NumberFormat.getCurrencyInstance(Locale.ITALY)
private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.ITALY)
private val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.ITALIAN)

private data class Expense(val id: Long, val amount: Double, val category: String, val description: String, val date: String, val member: String, val paymentMethod: String)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { MoneyFamilyApp() } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoneyFamilyApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }
    var expenses by remember { mutableStateOf(loadExpenses(prefs)) }
    var members by remember { mutableStateOf(loadMembers(prefs)) }
    var budget by remember { mutableStateOf(prefs.getFloat(BUDGET_KEY, 3000f).toDouble()) }
    var selectedMonth by remember { mutableStateOf(Calendar.getInstance()) }
    var tab by remember { mutableStateOf(0) }
    var showEditor by remember { mutableStateOf(false) }
    var editingExpense by remember { mutableStateOf<Expense?>(null) }

    LaunchedEffect(Unit) { if (members.isEmpty()) { members = listOf("Famiglia", "Papà", "Mamma", "Figlio 1", "Figlio 2"); saveMembers(prefs, members) } }
    val monthExpenses = expenses.filter { isSameMonth(it.date, selectedMonth) }
    val total = monthExpenses.sumOf { it.amount }
    val monthLabel = monthFormat.format(selectedMonth.time).replaceFirstChar { it.uppercase() }

    MaterialTheme {
        Scaffold(
            topBar = { TopAppBar(title = { Text("MoneyFamily") }) },
            bottomBar = { NavigationBar { listOf("Home", "Spese", "Budget", "Impostazioni").forEachIndexed { index, label -> NavigationBarItem(selected = tab == index, onClick = { tab = index }, icon = { Text(label.take(1)) }, label = { Text(label) }) } } },
            floatingActionButton = { if (tab <= 1) Button(onClick = { editingExpense = null; showEditor = true }) { Text("+ Spesa") } }
        ) { padding ->
            when (tab) {
                0 -> Dashboard(Modifier.padding(padding), monthLabel, total, budget, monthExpenses, members, { selectedMonth = previousMonth(selectedMonth) }, { selectedMonth = nextMonth(selectedMonth) }) { editingExpense = null; showEditor = true }
                1 -> ExpenseHistory(Modifier.padding(padding), monthExpenses, monthLabel, { selectedMonth = previousMonth(selectedMonth) }, { selectedMonth = nextMonth(selectedMonth) }, { editingExpense = it; showEditor = true }) { expense -> expenses = expenses.filterNot { it.id == expense.id }; saveExpenses(prefs, expenses) }
                2 -> BudgetScreen(Modifier.padding(padding), monthLabel, total, budget) { budget = it; prefs.edit().putFloat(BUDGET_KEY, it.toFloat()).apply() }
                else -> SettingsScreen(Modifier.padding(padding), members, { value -> if (value.isNotBlank() && !members.contains(value.trim())) { members = members + value.trim(); saveMembers(prefs, members) } }) { value -> if (members.size > 1) { members = members.filterNot { it == value }; saveMembers(prefs, members) } }
            }
        }
        if (showEditor) ExpenseEditor(editingExpense, members, { showEditor = false }) { expense -> expenses = if (editingExpense == null) listOf(expense) + expenses else expenses.map { if (it.id == expense.id) expense else it }; saveExpenses(prefs, expenses); showEditor = false }
    }
}

@Composable
private fun Dashboard(modifier: Modifier, monthLabel: String, total: Double, budget: Double, expenses: List<Expense>, members: List<String>, onPreviousMonth: () -> Unit, onNextMonth: () -> Unit, onAdd: () -> Unit) {
    LazyColumn(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { MonthSelector(monthLabel, onPreviousMonth, onNextMonth); Spacer(Modifier.height(12.dp)); Card(Modifier.fillMaxWidth(), RoundedCornerShape(16.dp)) { Column(Modifier.padding(18.dp), Arrangement.spacedBy(8.dp)) { Text("Spese del mese", style = MaterialTheme.typography.titleMedium); Text(moneyFormat.format(total), style = MaterialTheme.typography.headlineMedium); Text("Budget: ${moneyFormat.format(budget)}"); Text("Residuo: ${moneyFormat.format(budget - total)}"); LinearProgressIndicator({ if (budget > 0) (total / budget).coerceIn(0.0, 1.0).toFloat() else 0f }, Modifier.fillMaxWidth()) } } }
        item { Text("Per membro", style = MaterialTheme.typography.titleLarge) }
        items(members) { member -> Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) { Text(member); Text(moneyFormat.format(expenses.filter { it.member == member }.sumOf { it.amount })) } }
        item { Text("Ultime spese", style = MaterialTheme.typography.titleLarge) }
        if (expenses.isEmpty()) item { Text("Nessuna spesa per questo mese.") } else items(expenses.sortedByDescending { parseDate(it.date)?.timeInMillis ?: 0L }.take(5)) { ExpenseRow(it) }
        item { OutlinedButton(onClick = onAdd, Modifier.fillMaxWidth()) { Text("Inserisci nuova spesa") } }
    }
}

@Composable
private fun ExpenseHistory(modifier: Modifier, expenses: List<Expense>, monthLabel: String, onPreviousMonth: () -> Unit, onNextMonth: () -> Unit, onEdit: (Expense) -> Unit, onDelete: (Expense) -> Unit) {
    var expenseToDelete by remember { mutableStateOf<Expense?>(null) }
    Column(modifier.fillMaxSize().padding(16.dp)) {
        MonthSelector(monthLabel, onPreviousMonth, onNextMonth); Spacer(Modifier.height(12.dp))
        if (expenses.isEmpty()) Text("Nessuna spesa per questo mese.") else LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) { items(expenses.sortedByDescending { parseDate(it.date)?.timeInMillis ?: 0L }) { expense -> Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp)) { ExpenseRow(expense); Row(Modifier.fillMaxWidth(), Arrangement.End) { TextButton({ onEdit(expense) }) { Text("Modifica") }; TextButton({ expenseToDelete = expense }) { Text("Elimina") } } } } } }
    }
    expenseToDelete?.let { expense -> AlertDialog({ expenseToDelete = null }, { Text("Eliminare la spesa?") }, { Text("${expense.description.ifBlank { expense.category }} — ${moneyFormat.format(expense.amount)}") }, { TextButton({ onDelete(expense); expenseToDelete = null }) { Text("Elimina") } }, { TextButton({ expenseToDelete = null }) { Text("Annulla") } }) }
}

@Composable
private fun ExpenseRow(expense: Expense) { Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) { Column(Modifier.weight(1f)) { Text(expense.description.ifBlank { expense.category }, style = MaterialTheme.typography.titleMedium); Text("${expense.category} • ${expense.member} • ${expense.date}"); Text(expense.paymentMethod, style = MaterialTheme.typography.bodySmall) }; Spacer(Modifier.width(8.dp)); Text(moneyFormat.format(expense.amount), style = MaterialTheme.typography.titleMedium) } }

@Composable
private fun BudgetScreen(modifier: Modifier, monthLabel: String, total: Double, budget: Double, onBudgetChange: (Double) -> Unit) {
    var value by remember(budget) { mutableStateOf(String.format(Locale.US, "%.2f", budget)) }
    Column(modifier.fillMaxSize().padding(16.dp), Arrangement.spacedBy(14.dp)) { Text("Budget", style = MaterialTheme.typography.headlineSmall); Text(monthLabel); OutlinedTextField(value, { value = it }, label = { Text("Budget mensile (€)") }, Modifier.fillMaxWidth()); Button({ value.replace(',', '.').toDoubleOrNull()?.let(onBudgetChange) }, Modifier.fillMaxWidth()) { Text("Salva budget") }; Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), Arrangement.spacedBy(8.dp)) { Text("Utilizzato: ${moneyFormat.format(total)}"); Text("Disponibile: ${moneyFormat.format((budget - total).coerceAtLeast(0.0))}"); if (budget > 0) LinearProgressIndicator({ (total / budget).coerceIn(0.0, 1.0).toFloat() }, Modifier.fillMaxWidth()) } } }
}

@Composable
private fun SettingsScreen(modifier: Modifier, members: List<String>, onAddMember: (String) -> Unit, onRemoveMember: (String) -> Unit) {
    var newMember by remember { mutableStateOf("") }
    Column(modifier.fillMaxSize().padding(16.dp), Arrangement.spacedBy(12.dp)) { Text("Impostazioni", style = MaterialTheme.typography.headlineSmall); Text("Membri della famiglia", style = MaterialTheme.typography.titleLarge); members.forEach { member -> Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) { Text(member); if (members.size > 1) TextButton({ onRemoveMember(member) }) { Text("Rimuovi") } } }; HorizontalDivider(); OutlinedTextField(newMember, { newMember = it }, label = { Text("Nuovo membro") }, Modifier.fillMaxWidth()); Button({ onAddMember(newMember); newMember = "" }, Modifier.fillMaxWidth()) { Text("Aggiungi membro") }; Text("Categorie disponibili", style = MaterialTheme.typography.titleLarge); Text(DEFAULT_CATEGORIES.joinToString(" • ")); Text("Metodi di pagamento", style = MaterialTheme.typography.titleLarge); Text(PAYMENT_METHODS.joinToString(" • ")) }
}

@Composable
private fun ExpenseEditor(expense: Expense?, members: List<String>, onDismiss: () -> Unit, onSave: (Expense) -> Unit) {
    val context = LocalContext.current
    var amount by remember { mutableStateOf(expense?.amount?.toString() ?: "") }; var category by remember { mutableStateOf(expense?.category ?: DEFAULT_CATEGORIES.first()) }; var description by remember { mutableStateOf(expense?.description ?: "") }; var date by remember { mutableStateOf(expense?.date ?: dateFormat.format(Date())) }; var member by remember { mutableStateOf(expense?.member ?: members.firstOrNull().orEmpty()) }; var payment by remember { mutableStateOf(expense?.paymentMethod ?: PAYMENT_METHODS.first()) }; var categoryOpen by remember { mutableStateOf(false) }; var memberOpen by remember { mutableStateOf(false) }; var paymentOpen by remember { mutableStateOf(false) }; val parsedAmount = amount.replace(',', '.').toDoubleOrNull()
    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (expense == null) "Nuova spesa" else "Modifica spesa") }, text = { Column(Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(amount, { amount = it }, label = { Text("Importo (€)") }, Modifier.fillMaxWidth())
        Box { OutlinedButton({ categoryOpen = true }, Modifier.fillMaxWidth()) { Text("Categoria: $category") }; DropdownMenu(categoryOpen, { categoryOpen = false }) { DEFAULT_CATEGORIES.forEach { option -> DropdownMenuItem({ Text(option) }, { category = option; categoryOpen = false }) } } }
        Box { OutlinedButton({ memberOpen = true }, Modifier.fillMaxWidth()) { Text("Membro: $member") }; DropdownMenu(memberOpen, { memberOpen = false }) { members.forEach { option -> DropdownMenuItem({ Text(option) }, { member = option; memberOpen = false }) } } }
        Box { OutlinedButton({ paymentOpen = true }, Modifier.fillMaxWidth()) { Text("Pagamento: $payment") }; DropdownMenu(paymentOpen, { paymentOpen = false }) { PAYMENT_METHODS.forEach { option -> DropdownMenuItem({ Text(option) }, { payment = option; paymentOpen = false }) } } }
        OutlinedTextField(description, { description = it }, label = { Text("Descrizione") }, Modifier.fillMaxWidth())
        OutlinedButton({ val cal = parseDate(date) ?: Calendar.getInstance(); DatePickerDialog(context, { _, year, month, day -> cal.set(year, month, day); date = dateFormat.format(cal.time) }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show() }, Modifier.fillMaxWidth()) { Text("Data: $date") }
    } }, confirmButton = { TextButton(enabled = parsedAmount != null && parsedAmount > 0, onClick = { onSave(Expense(expense?.id ?: System.currentTimeMillis(), parsedAmount!!, category, description.trim(), date, member, payment)) }) { Text("Salva") } }, dismissButton = { TextButton(onDismiss) { Text("Annulla") } })
}

@Composable private fun MonthSelector(label: String, onPrevious: () -> Unit, onNext: () -> Unit) { Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) { OutlinedButton(onPrevious) { Text("‹") }; Surface(tonalElevation = 2.dp, shape = RoundedCornerShape(12.dp)) { Text(label, Modifier.padding(horizontal = 18.dp, vertical = 10.dp), MaterialTheme.typography.titleMedium) }; OutlinedButton(onNext) { Text("›") } } }

private fun loadExpenses(prefs: android.content.SharedPreferences): List<Expense> = try { val array = JSONArray(prefs.getString(EXPENSES_KEY, "[]")); buildList { for (i in 0 until array.length()) { val o = array.getJSONObject(i); add(Expense(o.getLong("id"), o.getDouble("amount"), o.optString("category", "Altro"), o.optString("description"), o.optString("date", dateFormat.format(Date())), o.optString("member", "Famiglia"), o.optString("paymentMethod", "Carta"))) } } } catch (_: Exception) { emptyList() }
private fun saveExpenses(prefs: android.content.SharedPreferences, expenses: List<Expense>) { val array = JSONArray(); expenses.forEach { e -> array.put(JSONObject().apply { put("id", e.id); put("amount", e.amount); put("category", e.category); put("description", e.description); put("date", e.date); put("member", e.member); put("paymentMethod", e.paymentMethod) }) }; prefs.edit().putString(EXPENSES_KEY, array.toString()).apply() }
private fun loadMembers(prefs: android.content.SharedPreferences): List<String> = try { val array = JSONArray(prefs.getString(MEMBERS_KEY, "[]")); List(array.length()) { array.getString(it) } } catch (_: Exception) { emptyList() }
private fun saveMembers(prefs: android.content.SharedPreferences, members: List<String>) { val array = JSONArray(); members.forEach(array::put); prefs.edit().putString(MEMBERS_KEY, array.toString()).apply() }
private fun parseDate(value: String): Calendar? = try { dateFormat.parse(value)?.let { Calendar.getInstance().apply { time = it } } } catch (_: Exception) { null }
private fun isSameMonth(value: String, month: Calendar): Boolean { val date = parseDate(value) ?: return false; return date.get(Calendar.YEAR) == month.get(Calendar.YEAR) && date.get(Calendar.MONTH) == month.get(Calendar.MONTH) }
private fun previousMonth(source: Calendar): Calendar = (source.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
private fun nextMonth(source: Calendar): Calendar = (source.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
