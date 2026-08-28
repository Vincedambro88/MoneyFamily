package com.moneyfamily.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.text.NumberFormat
import java.util.Locale

data class Expense(val id: Long, val amount: Double, val category: String, val description: String, val date: String)

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MoneyFamilyApp() }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun MoneyFamilyApp() {
    var expenses by remember {
        mutableStateOf(listOf(
            Expense(1,45.90,"Alimentari","Supermercato","27/08/2026"),
            Expense(2,60.00,"Auto","Carburante","26/08/2026"),
            Expense(3,32.50,"Figli","Materiale scolastico","25/08/2026")
        ))
    }
    var showAdd by remember { mutableStateOf(false) }
    var tab by remember { mutableIntStateOf(0) }
    val total = expenses.sumOf { it.amount }
    val budget = 3000.0
    val currency = NumberFormat.getCurrencyInstance(Locale.ITALY)

    MaterialTheme {
        Scaffold(
            topBar = { TopAppBar(title = { Text("MoneyFamily") }) },
            bottomBar = {
                NavigationBar {
                    listOf("Home","Storico","Budget").forEachIndexed { i, label ->
                        NavigationBarItem(selected=tab==i,onClick={tab=i},icon={},label={Text(label)})
                    }
                }
            },
            floatingActionButton = {
                FloatingActionButton(onClick={showAdd=true}) { Text("+") }
            }
        ) { pad ->
            when(tab) {
                0 -> Dashboard(Modifier.padding(pad), total, budget, expenses, currency)
                1 -> History(Modifier.padding(pad), expenses, currency)
                else -> BudgetScreen(Modifier.padding(pad), total, budget, currency)
            }
        }
        if(showAdd) AddExpenseDialog(
            onDismiss={showAdd=false},
            onSave={a,c,d ->
                expenses = listOf(Expense(System.currentTimeMillis(),a,c,d,"27/08/2026")) + expenses
                showAdd=false
            }
        )
    }
}

@Composable
fun Dashboard(modifier: Modifier,total:Double,budget:Double,expenses:List<Expense>,currency:NumberFormat) {
    Column(modifier.padding(16.dp), verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Text("Agosto 2026",style=MaterialTheme.typography.headlineSmall)
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Spese del mese")
                Text(currency.format(total),style=MaterialTheme.typography.headlineMedium)
                Text("Budget: ${currency.format(budget)}")
                Text("Residuo: ${currency.format(budget-total)}")
            }
        }
        Text("Ultime spese",style=MaterialTheme.typography.titleLarge)
        expenses.take(5).forEach {
            ListItem(
                headlineContent={Text(it.description.ifBlank{it.category})},
                supportingContent={Text("${it.category} • ${it.date}")},
                trailingContent={Text(currency.format(it.amount))}
            )
        }
    }
}

@Composable
fun History(modifier: Modifier,expenses:List<Expense>,currency:NumberFormat) {
    LazyColumn(modifier.padding(16.dp)) {
        item { Text("Storico spese",style=MaterialTheme.typography.headlineSmall) }
        items(expenses) {
            ListItem(
                headlineContent={Text(it.description.ifBlank{it.category})},
                supportingContent={Text("${it.category} • ${it.date}")},
                trailingContent={Text(currency.format(it.amount))}
            )
        }
    }
}

@Composable
fun BudgetScreen(modifier: Modifier,total:Double,budget:Double,currency:NumberFormat) {
    Column(modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        Text("Budget",style=MaterialTheme.typography.headlineSmall)
        Text("Budget mensile: ${currency.format(budget)}")
        LinearProgressIndicator(
            progress={(total/budget).coerceIn(0.0,1.0).toFloat()},
            modifier=Modifier.fillMaxWidth()
        )
        Text("Utilizzato: ${currency.format(total)}")
        Text("Disponibile: ${currency.format((budget-total).coerceAtLeast(0.0))}")
    }
}

@Composable
fun AddExpenseDialog(onDismiss:()->Unit,onSave:(Double,String,String)->Unit) {
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Alimentari") }
    var description by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest=onDismiss,
        title={Text("Nuova spesa")},
        text={
            Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(amount,{amount=it},label={Text("Importo")})
                OutlinedTextField(category,{category=it},label={Text("Categoria")})
                OutlinedTextField(description,{description=it},label={Text("Descrizione")})
            }
        },
        confirmButton={
            TextButton(
                enabled=amount.replace(',','.').toDoubleOrNull()!=null,
                onClick={onSave(amount.replace(',','.').toDouble(),category,description)}
            ){Text("Salva")}
        },
        dismissButton={TextButton(onClick=onDismiss){Text("Annulla")}}
    )
}
