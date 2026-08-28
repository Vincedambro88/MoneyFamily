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
private const val BUDGET_KEY = "budget"

private val CATEGORIES = listOf("Alimentari", "Casa", "Auto", "Bollette", "Salute", "Figli", "Istruzione", "Abbigliamento", "Tempo libero", "Vacanze", "Sport", "Altro")
private val PAYMENT_METHODS = listOf("Contanti", "Carta", "Bonifico", "Addebito", "Altro")
private val moneyFormat = NumberFormat.getCurrencyInstance(Locale.ITALY)
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
private fun MoneyFamilyApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }
    var expenses by remember { mutableStateOf(loadExpenses(prefs)) }
    var members by remember { mutableStateOf(loadMembers(prefs).ifEmpty { listOf("Famiglia", "Papà", "Mamma", "Figlio 1", "Figlio 2") }) }
    var budget by remember { mutableStateOf(prefs.getFloat(BUDGET_KEY, 3000f).toDouble()) }
    var selectedMonth by remember { mutableStateOf(Calendar.getInstance()) }
    var tab by remember { mutableStateOf(0) }
    var editorOpen by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Expense?>(null) }

    if (loadMembers(prefs).isEmpty()) saveMembers(prefs, members)

    val monthExpenses = expenses.filter { sameMonth(it.date, selectedMonth) }
    val total = monthExpenses.sumOf { it.amount }
    val monthLabel = monthFormat.format(selectedMonth.time).replaceFirstChar { it.uppercase() }

    MaterialTheme {
        Scaffold(
            bottomBar = {
                NavigationBar {
                    val labels = listOf("Home", "Spese", "Budget", "Impostazioni")
                    labels.forEachIndexed { index, label ->
                        NavigationBarItem(
                            selected = tab == index,
                            onClick = { tab = index },
                            icon = { Text(label.take(1)) },
                            label = { Text(label) }
                        )
                    }
                }
            }
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding)) {
                Surface(tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                    Text("MoneyFamily", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(16.dp))
                }
                when (tab) {
                    0 -> Dashboard(
                        monthLabel = monthLabel,
                        total = total,
                        budget = budget,
                        expenses = monthExpenses,
                        members = members,
                        onPrevious = { selectedMonth = previousMonth(selectedMonth) },
                        onNext = { selectedMonth = nextMonth(selectedMonth) },
                        onAdd = { editing = null; editorOpen = true }
                    )
                    1 -> ExpenseHistory(
                        monthLabel = monthLabel,
                        expenses = monthExpenses,
                        onPrevious = { selectedMonth = previousMonth(selectedMonth) },
                        onNext = { selectedMonth = nextMonth(selectedMonth) },
                        onEdit = { editing = it; editorOpen = true },
                        onDelete = {
                            expenses = expenses.filterNot { e -> e.id == it.id }
                            saveExpenses(prefs, expenses)
                        }
                    )
                    2 -> BudgetScreen(
                        monthLabel = monthLabel,
                        total = total,
                        budget = budget,
                        onSave = {
                            budget = it
                            prefs.edit().putFloat(BUDGET_KEY, it.toFloat()).apply()
                        }
                    )
                    else -> SettingsScreen(
                        members = members,
                        onAdd = {
                            val value = it.trim()
                            if (value.isNotEmpty() && !members.contains(value)) {
                                members = members + value
                                saveMembers(prefs, members)
                            }
                        },
                        onRemove = {
                            if (members.size > 1) {
                                members = members.filterNot { m -> m == it }
                                saveMembers(prefs, members)
                            }
                        }
                    )
                }
            }
        }

        if (editorOpen) {
            ExpenseEditor(
                expense = editing,
                members = members,
                onDismiss = { editorOpen = false },
                onSave = { value ->
                    expenses = if (editing == null) {
                        listOf(value) + expenses
                    } else {
                        expenses.map { e -> if (e.id == value.id) value else e }
                    }
                    saveExpenses(prefs, expenses)
                    editorOpen = false
                }
            )
        }
    }
}

@Composable
private fun Dashboard(
    monthLabel: String,
    total: Double,
    budget: Double,
    expenses: List<Expense>,
    members: List<String>,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onAdd: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { MonthSelector(monthLabel, onPrevious, onNext) }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
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
        item { Text("Spese per membro", style = MaterialTheme.typography.titleLarge) }
        items(members) { member ->
            val memberTotal = expenses.filter { it.member == member }.sumOf { it.amount }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(member)
                Text(moneyFormat.format(memberTotal))
            }
        }
        item { Text("Ultime spese", style = MaterialTheme.typography.titleLarge) }
        if (expenses.isEmpty()) {
            item { Text("Nessuna spesa per questo mese.") }
        } else {
            items(expenses.sortedByDescending { parseDate(it.date)?.timeInMillis ?: 0L }.take(5)) { ExpenseRow(it) }
        }
        item { OutlinedButton(onClick = onAdd, modifier = Modifier.fillMaxWidth()) { Text("Inserisci nuova spesa") } }
    }
}

@Composable
private fun ExpenseHistory(
    monthLabel: String,
    expenses: List<Expense>,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onEdit: (Expense) -> Unit,
    onDelete: (Expense) -> Unit
) {
    var pendingDelete by remember { mutableStateOf<Expense?>(null) }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        MonthSelector(monthLabel, onPrevious, onNext)
        Spacer(Modifier.height(12.dp))
        if (expenses.isEmpty()) {
            Text("Nessuna spesa per questo mese.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(expenses.sortedByDescending { parseDate(it.date)?.timeInMillis ?: 0L }) { expense ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            ExpenseRow(expense)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                TextButton(onClick = { onEdit(expense) }) { Text("Modifica") }
                                TextButton(onClick = { pendingDelete = expense }) { Text("Elimina") }
                            }
                        }
                    }
                }
            }
        }
    }
    pendingDelete?.let { expense ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Eliminare la spesa?") },
            text = { Text("${expense.description.ifBlank { expense.category }} — ${moneyFormat.format(expense.amount)}") },
            confirmButton = {
                TextButton(onClick = { onDelete(expense); pendingDelete = null }) { Text("Elimina") }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Annulla") } }
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
private fun BudgetScreen(monthLabel: String, total: Double, budget: Double, onSave: (Double) -> Unit) {
    var value by remember(budget) { mutableStateOf(String.format(Locale.US, "%.2f", budget)) }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Budget", style = MaterialTheme.typography.headlineSmall)
        Text(monthLabel)
        OutlinedTextField(
            value = value,
            onValueChange = { value = it },
            label = { Text("Budget mensile (€)") },
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = { value.replace(',', '.').toDoubleOrNull()?.takeIf { it >= 0 }?.let(onSave) },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Salva budget") }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Utilizzato: ${moneyFormat.format(total)}")
                Text("Disponibile: ${moneyFormat.format(budget - total)}")
                LinearProgressIndicator(
                    progress = { if (budget > 0) (total / budget).coerceIn(0.0, 1.0).toFloat() else 0f },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun SettingsScreen(members: List<String>, onAdd: (String) -> Unit, onRemove: (String) -> Unit) {
    var newMember by remember { mutableStateOf("") }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Impostazioni", style = MaterialTheme.typography.headlineSmall) }
        item { Text("Membri della famiglia", style = MaterialTheme.typography.titleLarge) }
        items(members) { member ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(member)
                if (members.size > 1) TextButton(onClick = { onRemove(member) }) { Text("Rimuovi") }
            }
        }
        item { HorizontalDivider() }
        item {
            OutlinedTextField(
                value = newMember,
                onValueChange = { newMember = it },
                label = { Text("Nuovo membro") },
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            Button(onClick = { onAdd(newMember); newMember = "" }, modifier = Modifier.fillMaxWidth()) { Text("Aggiungi membro") }
        }
        item { Text("Categorie", style = MaterialTheme.typography.titleLarge) }
        item { Text(CATEGORIES.joinToString(" • ")) }
        item { Text("Metodi di pagamento", style = MaterialTheme.typography.titleLarge) }
        item { Text(PAYMENT_METHODS.joinToString(" • ")) }
    }
}

@Composable
private fun ExpenseEditor(expense: Expense?, members: List<String>, onDismiss: () -> Unit, onSave: (Expense) -> Unit) {
    val context = LocalContext.current
    var amount by remember(expense) { mutableStateOf(expense?.amount?.toString() ?: "") }
    var category by remember(expense) { mutableStateOf(expense?.category ?: CATEGORIES.first()) }
    var description by remember(expense) { mutableStateOf(expense?.description ?: "") }
    var date by remember(expense) { mutableStateOf(expense?.date ?: dateFormat.format(Date())) }
    var member by remember(expense) { mutableStateOf(expense?.member ?: members.firstOrNull().orEmpty()) }
    var payment by remember(expense) { mutableStateOf(expense?.paymentMethod ?: PAYMENT_METHODS.first()) }
    var categoryOpen by remember { mutableStateOf(false) }
    var memberOpen by remember { mutableStateOf(false) }
    var paymentOpen by remember { mutableStateOf(false) }
    val parsedAmount = amount.replace(',', '.').toDoubleOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (expense == null) "Nuova spesa" else "Modifica spesa") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Importo (€)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Box(Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { categoryOpen = true }, modifier = Modifier.fillMaxWidth()) { Text("Categoria: $category") }
                    DropdownMenu(expanded = categoryOpen, onDismissRequest = { categoryOpen = false }) {
                        CATEGORIES.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = { category = option; categoryOpen = false }
                            )
                        }
                    }
                }
                Box(Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { memberOpen = true }, modifier = Modifier.fillMaxWidth()) { Text("Membro: $member") }
                    DropdownMenu(expanded = memberOpen, onDismissRequest = { memberOpen = false }) {
                        members.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = { member = option; memberOpen = false }
                            )
                        }
                    }
                }
                Box(Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { paymentOpen = true }, modifier = Modifier.fillMaxWidth()) { Text("Pagamento: $payment") }
                    DropdownMenu(expanded = paymentOpen, onDismissRequest = { paymentOpen = false }) {
                        PAYMENT_METHODS.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = { payment = option; paymentOpen = false }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descrizione") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedButton(
                    onClick = {
                        val cal = parseDate(date) ?: Calendar.getInstance()
                        DatePickerDialog(
                            context,
                            { _, year, month, day ->
                                cal.set(year, month, day)
                                date = dateFormat.format(cal.time)
                            },
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH),
                            cal.get(Calendar.DAY_OF_MONTH)
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
                            amount = parsedAmount ?: 0.0,
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
        Surface(tonalElevation = 2.dp) { Text(label, modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp), style = MaterialTheme.typography.titleMedium) }
        OutlinedButton(onClick = onNext) { Text("›") }
    }
}

private fun loadExpenses(prefs: android.content.SharedPreferences): List<Expense> = try {
    val array = JSONArray(prefs.getString(EXPENSES_KEY, "[]"))
    buildList {
        for (i in 0 until array.length()) {
            val o = array.getJSONObject(i)
            add(
                Expense(
                    id = o.getLong("id"),
                    amount = o.getDouble("amount"),
                    category = o.optString("category", "Altro"),
                    description = o.optString("description"),
                    date = o.optString("date", dateFormat.format(Date())),
                    member = o.optString("member", "Famiglia"),
                    paymentMethod = o.optString("paymentMethod", "Carta")
                )
            )
        }
    }
} catch (_: Exception) { emptyList() }

private fun saveExpenses(prefs: android.content.SharedPreferences, expenses: List<Expense>) {
    val array = JSONArray()
    expenses.forEach { e ->
        array.put(JSONObject().apply {
            put("id", e.id)
            put("amount", e.amount)
            put("category", e.category)
            put("description", e.description)
            put("date", e.date)
            put("member", e.member)
            put("paymentMethod", e.paymentMethod)
        })
    }
    prefs.edit().putString(EXPENSES_KEY, array.toString()).apply()
}

private fun loadMembers(prefs: android.content.SharedPreferences): List<String> = try {
    val array = JSONArray(prefs.getString(MEMBERS_KEY, "[]"))
    List(array.length()) { array.getString(it) }
} catch (_: Exception) { emptyList() }

private fun saveMembers(prefs: android.content.SharedPreferences, members: List<String>) {
    val array = JSONArray()
    members.forEach { array.put(it) }
    prefs.edit().putString(MEMBERS_KEY, array.toString()).apply()
}

private fun parseDate(value: String): Calendar? = try {
    dateFormat.parse(value)?.let { parsed -> Calendar.getInstance().apply { time = parsed } }
} catch (_: Exception) { null }

private fun sameMonth(value: String, month: Calendar): Boolean {
    val date = parseDate(value) ?: return false
    return date.get(Calendar.YEAR) == month.get(Calendar.YEAR) && date.get(Calendar.MONTH) == month.get(Calendar.MONTH)
}

private fun previousMonth(source: Calendar): Calendar = (source.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
private fun nextMonth(source: Calendar): Calendar = (source.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
