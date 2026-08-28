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

private val DEFAULT_CATEGORIES = listOf(
    "Alimentari", "Casa", "Auto", "Bollette", "Salute", "Figli",
    "Istruzione", "Abbigliamento", "Tempo libero", "Vacanze", "Sport", "Altro"
)
private val PAYMENT_METHODS = listOf("Contanti", "Carta", "Bonifico", "Addebito", "Altro")

private val moneyFormat: NumberFormat = NumberFormat.getCurrencyInstance(Locale.ITALY)
private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.ITALY)
private val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.ITALIAN)

private data class Expense(
    val id: Long,
    val amount: Double,
    val category: String,
    val description: String,
    val date: String,
    val member: String,
    val paymentMethod: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MoneyFamilyApp() }
    }
}

@Composable
fun MoneyFamilyApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }
    val today = remember { Calendar.getInstance() }
    var expenses by remember { mutableStateOf(loadExpenses(prefs)) }
    var members by remember { mutableStateOf(loadMembers(prefs)) }
    var budget by remember { mutableStateOf(prefs.getFloat(BUDGET_KEY, 3000f).toDouble()) }
    var selectedMonth by remember { mutableStateOf(Calendar.getInstance()) }
    var tab by remember { mutableStateOf(0) }
    var showEditor by remember { mutableStateOf(false) }
    var editingExpense by remember { mutableStateOf<Expense?>(null) }

    LaunchedEffect(Unit) {
        if (members.isEmpty()) {
            members = listOf("Famiglia", "Papà", "Mamma", "Figlio 1", "Figlio 2")
            saveMembers(prefs, members)
        }
    }

    val monthExpenses = expenses.filter { isSameMonth(it.date, selectedMonth) }
    val total = monthExpenses.sumOf { it.amount }
    val monthLabel = monthFormat.format(selectedMonth.time).replaceFirstChar { it.uppercase() }

    MaterialTheme {
        Scaffold(
            topBar = { TopAppBar(title = { Text("MoneyFamily") }) },
            bottomBar = {
                NavigationBar {
                    listOf("Home", "Spese", "Budget", "Impostazioni").forEachIndexed { index, label ->
                        NavigationBarItem(
                            selected = tab == index,
                            onClick = { tab = index },
                            icon = { Text(label.take(1)) },
                            label = { Text(label) }
                        )
                    }
                }
            },
            floatingActionButton = {
                if (tab <= 1) {
                    Button(onClick = { editingExpense = null; showEditor = true }) { Text("+ Spesa") }
                }
            }
        ) { padding ->
            when (tab) {
                0 -> Dashboard(
                    modifier = Modifier.padding(padding),
                    monthLabel = monthLabel,
                    total = total,
                    budget = budget,
                    expenses = monthExpenses,
                    members = members,
                    onPreviousMonth = { selectedMonth = previousMonth(selectedMonth) },
                    onNextMonth = { selectedMonth = nextMonth(selectedMonth) },
                    onAdd = { editingExpense = null; showEditor = true }
                )
                1 -> ExpenseHistory(
                    modifier = Modifier.padding(padding),
                    expenses = monthExpenses,
                    monthLabel = monthLabel,
                    onPreviousMonth = { selectedMonth = previousMonth(selectedMonth) },
                    onNextMonth = { selectedMonth = nextMonth(selectedMonth) },
                    onEdit = { editingExpense = it; showEditor = true },
                    onDelete = {
                        expenses = expenses.filterNot { expense -> expense.id == it.id }
                        saveExpenses(prefs, expenses)
                    }
                )
                2 -> BudgetScreen(
                    modifier = Modifier.padding(padding),
                    monthLabel = monthLabel,
                    total = total,
                    budget = budget,
                    onBudgetChange = {
                        budget = it
                        prefs.edit().putFloat(BUDGET_KEY, it.toFloat()).apply()
                    }
                )
                else -> SettingsScreen(
                    modifier = Modifier.padding(padding),
                    members = members,
                    onAddMember = {
                        if (it.isNotBlank() && !members.contains(it.trim())) {
                            members = members + it.trim()
                            saveMembers(prefs, members)
                        }
                    },
                    onRemoveMember = {
                        if (members.size > 1) {
                            members = members.filterNot { member -> member == it }
                            saveMembers(prefs, members)
                        }
                    }
                )
            }
        }

        if (showEditor) {
            ExpenseEditor(
                expense = editingExpense,
                members = members,
                onDismiss = { showEditor = false },
                onSave = { expense ->
                    expenses = if (editingExpense == null) {
                        listOf(expense) + expenses
                    } else {
                        expenses.map { if (it.id == expense.id) expense else it }
                    }
                    saveExpenses(prefs, expenses)
                    showEditor = false
                }
            )
        }
    }
}

@Composable
private fun Dashboard(
    modifier: Modifier,
    monthLabel: String,
    total: Double,
    budget: Double,
    expenses: List<Expense>,
    members: List<String>,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onAdd: () -> Unit
) {
    LazyColumn(modifier = modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            MonthSelector(monthLabel, onPreviousMonth, onNextMonth)
            Spacer(Modifier.height(12.dp))
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Spese del mese", style = MaterialTheme.typography.titleMedium)
                    Text(moneyFormat.format(total), style = MaterialTheme.typography.headlineMedium)
                    Text("Budget: ${moneyFormat.format(budget)}")
                    Text("Residuo: ${moneyFormat.format(budget - total)}")
                    LinearProgressIndicator(
                        progress = { if (budget > 0) (total / budget).coerceIn(0.0, 1.0).toFloat() else 0f },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        item { Text("Per membro", style = MaterialTheme.typography.titleLarge) }
        items(members) { member ->
            val memberTotal = expenses.filter { it.member == member }.sumOf { it.amount }
            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(member)
                Text(moneyFormat.format(memberTotal))
            }
        }
        item { Text("Ultime spese", style = MaterialTheme.typography.titleLarge) }
        if (expenses.isEmpty()) {
            item { Text("Nessuna spesa per questo mese.") }
        } else {
            items(expenses.take(5)) { ExpenseRow(it) }
        }
        item {
            Spacer(Modifier.height(4.dp))
            OutlinedButton(onClick = onAdd, modifier = Modifier.fillMaxWidth()) { Text("Inserisci nuova spesa") }
        }
    }
}

@Composable
private fun ExpenseHistory(
    modifier: Modifier,
    expenses: List<Expense>,
    monthLabel: String,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onEdit: (Expense) -> Unit,
    onDelete: (Expense) -> Unit
) {
    var expenseToDelete by remember { mutableStateOf<Expense?>(null) }
    Column(modifier.fillMaxSize().padding(16.dp)) {
        MonthSelector(monthLabel, onPreviousMonth, onNextMonth)
        Spacer(Modifier.height(12.dp))
        if (expenses.isEmpty()) {
            Text("Nessuna spesa per questo mese.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(expenses.sortedByDescending { parseDate(it.date)?.time ?: 0L }) { expense ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            ExpenseRow(expense)
                            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                TextButton(onClick = { onEdit(expense) }) { Text("Modifica") }
                                TextButton(onClick = { expenseToDelete = expense }) { Text("Elimina") }
                            }
                        }
                    }
                }
            }
        }
    }
    expenseToDelete?.let { expense ->
        AlertDialog(
            onDismissRequest = { expenseToDelete = null },
            title = { Text("Eliminare la spesa?") },
            text = { Text("${expense.description.ifBlank { expense.category }} — ${moneyFormat.format(expense.amount)}") },
            confirmButton = {
                TextButton(onClick = { onDelete(expense); expenseToDelete = null }) { Text("Elimina") }
            },
            dismissButton = { TextButton(onClick = { expenseToDelete = null }) { Text("Annulla") } }
        )
    }
}

@Composable
private fun ExpenseRow(expense: Expense) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Column(Modifier.weight(1f)) {
            Text(expense.description.ifBlank { expense.category }, style = MaterialTheme.typography.titleMedium)
            Text("${expense.category} • ${expense.member} • ${expense.date}")
            Text(expense.paymentMethod, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.width(8.dp))
        Text(moneyFormat.format(expense.amount), style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun BudgetScreen(
    modifier: Modifier,
    monthLabel: String,
    total: Double,
    budget: Double,
    onBudgetChange: (Double) -> Unit
) {
    var value by remember(budget) { mutableStateOf(String.format(Locale.US, "%.2f", budget)) }
    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Budget", style = MaterialTheme.typography.headlineSmall)
        Text(monthLabel)
        OutlinedTextField(value, { value = it }, label = { Text("Budget mensile (€)") }, modifier = Modifier.fillMaxWidth())
        Button(
            onClick = { value.replace(',', '.').toDoubleOrNull()?.let(onBudgetChange) },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Salva budget") }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Utilizzato: ${moneyFormat.format(total)}")
                Text("Disponibile: ${moneyFormat.format((budget - total).coerceAtLeast(0.0))}")
                if (budget > 0) LinearProgressIndicator(
                    progress = { (total / budget).coerceIn(0.0, 1.0).toFloat() },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    modifier: Modifier,
    members: List<String>,
    onAddMember: (String) -> Unit,
    onRemoveMember: (String) -> Unit
) {
    var newMember by remember { mutableStateOf("") }
    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Impostazioni", style = MaterialTheme.typography.headlineSmall)
        Text("Membri della famiglia", style = MaterialTheme.typography.titleLarge)
        members.forEach { member ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(member)
                if (members.size > 1) TextButton(onClick = { onRemoveMember(member) }) { Text("Rimuovi") }
            }
        }
        HorizontalDivider()
        OutlinedTextField(newMember, { newMember = it }, label = { Text("Nuovo membro") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = { onAddMember(newMember); newMember = "" }, modifier = Modifier.fillMaxWidth()) { Text("Aggiungi membro") }
        Text("Categorie disponibili", style = MaterialTheme.typography.titleLarge)
        Text(DEFAULT_CATEGORIES.joinToString(" • "))
        Text("Metodi di pagamento", style = MaterialTheme.typography.titleLarge)
        Text(PAYMENT_METHODS.joinToString(" • "))
    }
}

@Composable
private fun ExpenseEditor(expense: Expense?, members: List<String>, onDismiss: () -> Unit, onSave: (Expense) -> Unit) {
    val context = LocalContext.current
    var amount by remember { mutableStateOf(expense?.amount?.toString() ?: "") }
    var category by remember { mutableStateOf(expense?.category ?: DEFAULT_CATEGORIES.first()) }
    var description by remember { mutableStateOf(expense?.description ?: "") }
    var date by remember { mutableStateOf(expense?.date ?: dateFormat.format(Date())) }
    var member by remember { mutableStateOf(expense?.member ?: members.firstOrNull().orEmpty()) }
    var payment by remember { mutableStateOf(expense?.paymentMethod ?: PAYMENT_METHODS.first()) }
    var categoryOpen by remember { mutableStateOf(false) }
    var memberOpen by remember { mutableStateOf(false) }
    var paymentOpen by remember { mutableStateOf(false) }
    val parsedAmount = amount.replace(',', '.').toDoubleOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (expense == null) "Nuova spesa" else "Modifica spesa") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(amount, { amount = it }, label = { Text("Importo (€)") }, modifier = Modifier.fillMaxWidth())
                Box {
                    OutlinedButton(onClick = { categoryOpen = true }, modifier = Modifier.fillMaxWidth()) { Text("Categoria: $category") }
                    DropdownMenu(categoryOpen, { categoryOpen = false }) {
                        DEFAULT_CATEGORIES.forEach { option -> DropdownMenuItem(text = { Text(option) }, onClick = { category = option; categoryOpen = false }) }
                    }
                }
                Box {
                    OutlinedButton(onClick = { memberOpen = true }, modifier = Modifier.fillMaxWidth()) { Text("Membro: $member") }
                    DropdownMenu(memberOpen, { memberOpen = false }) {
                        members.forEach { option -> DropdownMenuItem(text = { Text(option) }, onClick = { member = option; memberOpen = false }) }
                    }
                }
                Box {
                    OutlinedButton(onClick = { paymentOpen = true }, modifier = Modifier.fillMaxWidth()) { Text("Pagamento: $payment") }
                    DropdownMenu(paymentOpen, { paymentOpen = false }) {
                        PAYMENT_METHODS.forEach { option -> DropdownMenuItem(text = { Text(option) }, onClick = { payment = option; paymentOpen = false }) }
                    }
                }
                OutlinedTextField(description, { description = it }, label = { Text("Descrizione") }, modifier = Modifier.fillMaxWidth())
                OutlinedButton(
                    onClick = {
                        val cal = parseDate(date) ?: Calendar.getInstance()
                        DatePickerDialog(
                            context,
                            { _, year, month, day ->
                                cal.set(year, month, day)
                                date = dateFormat.format(cal.time)
                            },
                            cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Data: $date") }
            }
        },
        confirmButton = {
            TextButton(
                enabled = parsedAmount != null && parsedAmount > 0,
                onClick = {
                    onSave(
                        Expense(
                            id = expense?.id ?: System.currentTimeMillis(),
                            amount = parsedAmount!!,
                            category = category,
                            description = description.trim(),
                            date = date,
                            member = member,
                            paymentMethod = payment
                        )
                    )
                }
            ) { Text("Salva") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } }
    )
}

@Composable
private fun MonthSelector(label: String, onPrevious: () -> Unit, onNext: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        OutlinedButton(onClick = onPrevious) { Text("‹") }
        Surface(tonalElevation = 2.dp, shape = RoundedCornerShape(12.dp)) {
            Text(label, modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp), style = MaterialTheme.typography.titleMedium)
        }
        OutlinedButton(onClick = onNext) { Text("›") }
    }
}

private fun loadExpenses(prefs: android.content.SharedPreferences): List<Expense> {
    return try {
        val array = JSONArray(prefs.getString(EXPENSES_KEY, "[]"))
        buildList {
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                add(
                    Expense(
                        id = o.getLong("id"), amount = o.getDouble("amount"),
                        category = o.optString("category", "Altro"), description = o.optString("description"),
                        date = o.optString("date", dateFormat.format(Date())),
                        member = o.optString("member", "Famiglia"),
                        paymentMethod = o.optString("paymentMethod", "Carta")
                    )
                )
            }
        }
    } catch (_: Exception) { emptyList() }
}

private fun saveExpenses(prefs: android.content.SharedPreferences, expenses: List<Expense>) {
    val array = JSONArray()
    expenses.forEach { expense ->
        array.put(JSONObject().apply {
            put("id", expense.id); put("amount", expense.amount); put("category", expense.category)
            put("description", expense.description); put("date", expense.date); put("member", expense.member)
            put("paymentMethod", expense.paymentMethod)
        })
    }
    prefs.edit().putString(EXPENSES_KEY, array.toString()).apply()
}

private fun loadMembers(prefs: android.content.SharedPreferences): List<String> {
    return try {
        val array = JSONArray(prefs.getString(MEMBERS_KEY, "[]"))
        List(array.length()) { array.getString(it) }
    } catch (_: Exception) { emptyList() }
}

private fun saveMembers(prefs: android.content.SharedPreferences, members: List<String>) {
    val array = JSONArray(); members.forEach(array::put)
    prefs.edit().putString(MEMBERS_KEY, array.toString()).apply()
}

private fun parseDate(value: String): Calendar? = try {
    dateFormat.parse(value)?.let { Calendar.getInstance().apply { time = it } }
} catch (_: Exception) { null }

private fun isSameMonth(value: String, month: Calendar): Boolean {
    val date = parseDate(value) ?: return false
    return date.get(Calendar.YEAR) == month.get(Calendar.YEAR) && date.get(Calendar.MONTH) == month.get(Calendar.MONTH)
}

private fun previousMonth(source: Calendar): Calendar = (source.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
private fun nextMonth(source: Calendar): Calendar = (source.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
