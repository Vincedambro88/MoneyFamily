package com.moneyfamily.app

import android.app.DatePickerDialog
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
import com.moneyfamily.app.data.Movement
import com.moneyfamily.app.data.MovementType
import com.moneyfamily.app.data.RoomRepository
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val money = NumberFormat.getCurrencyInstance(Locale.ITALY)
private val df = SimpleDateFormat("dd/MM/yyyy", Locale.ITALY)
private val mf = SimpleDateFormat("MMMM yyyy", Locale.ITALIAN)
private val outCats = listOf("Alimentari","Casa","Auto","Bollette","Salute","Figli","Istruzione","Abbigliamento","Tempo libero","Vacanze","Sport","Altro")
private val inCats = listOf("Stipendio","Bonus","Rimborso","Altro")
private val payments = listOf("Carta","Contanti","Bonifico","Addebito","Altro")

data class UiMovement(val id: Long, val type: MovementType, val amount: Double, val category: String, val description: String, val date: String, val member: String, val payment: String)

class MainActivity : ComponentActivity() {
    override fun onCreate(state: Bundle?) { super.onCreate(state); setContent { MoneyFamilyApp() } }
}

@Composable
private fun MoneyFamilyApp() {
    val context = LocalContext.current
    val repo = remember { RoomRepository(context) }
    val scope = rememberCoroutineScope()
    var data by remember { mutableStateOf<List<UiMovement>>(emptyList()) }
    var members by remember { mutableStateOf(listOf("Famiglia","Papà","Mamma","Figlio 1","Figlio 2")) }
    var tab by remember { mutableStateOf(0) }
    var month by remember { mutableStateOf(Calendar.getInstance()) }
    var edit by remember { mutableStateOf<UiMovement?>(null) }
    var add by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { data = repo.all().map { it.ui() } }
    DisposableEffect(Unit) { onDispose { repo.close() } }
    fun save(x: UiMovement) { scope.launch { if (data.any { it.id == x.id }) repo.update(x.model()) else repo.insert(x.model()); data = repo.all().map { it.ui() } } }
    fun remove(x: UiMovement) { scope.launch { repo.delete(x.model()); data = repo.all().map { it.ui() } } }
    MaterialTheme {
        Scaffold(bottomBar = { NavigationBar { listOf("Home","Movimenti","Analisi","Impostazioni").forEachIndexed { i,t -> NavigationBarItem(tab==i,{tab=i},{Text(t.take(1))},{Text(t)}) } } }) { p ->
            Column(Modifier.fillMaxSize().padding(p)) {
                Text("MoneyFamily", style=MaterialTheme.typography.headlineSmall, modifier=Modifier.padding(16.dp))
                when(tab) {
                    0 -> Home(data,members,month,{month=shift(month,-1)},{month=shift(month,1)},{add=true})
                    1 -> Movements(data.filter{same(it.date,month)},month,{month=shift(month,-1)},{month=shift(month,1)},{edit=it},{remove(it)})
                    2 -> Analysis(data)
                    3 -> Settings(members){ if(it.isNotBlank() && it !in members) members=members+it }
                }
            }
        }
        if(add || edit!=null) Editor(edit,members,{add=false;edit=null}){save(it);add=false;edit=null}
    }
}

@Composable private fun Home(data:List<UiMovement>,members:List<String>,month:Calendar,prev:()->Unit,next:()->Unit,add:()->Unit) {
    val cur=data.filter{same(it.date,month)}; val inc=cur.filter{it.type==MovementType.INCOME}.sumOf{it.amount}; val out=cur.filter{it.type==MovementType.EXPENSE}.sumOf{it.amount}
    LazyColumn(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){ item{MonthBar(mf.format(month.time),prev,next)};item{Card(Modifier.fillMaxWidth()){Column(Modifier.padding(18.dp)){Text("Riepilogo reale",style=MaterialTheme.typography.titleMedium);Text("Entrate ${money.format(inc)}");Text("Uscite ${money.format(out)}");Text("Saldo ${money.format(inc-out)}",style=MaterialTheme.typography.headlineSmall)}}};item{Text("Uscite per categoria",style=MaterialTheme.typography.titleLarge)};items(cur.filter{it.type==MovementType.EXPENSE}.groupBy{it.category}.entries.sortedByDescending{it.value.sumOf{m->m.amount}}){e->Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(e.key);Text(money.format(e.value.sumOf{it.amount}))}};item{Text("Per membro",style=MaterialTheme.typography.titleLarge)};items(members){m->Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(m);Text(money.format(cur.filter{it.member==m&&it.type==MovementType.EXPENSE}.sumOf{it.amount}))}};item{Button(add,Modifier.fillMaxWidth()){Text("+ Inserisci movimento")}} }
}

@Composable private fun Movements(data:List<UiMovement>,month:Calendar,prev:()->Unit,next:()->Unit,edit:(UiMovement)->Unit,remove:(UiMovement)->Unit){var pending by remember{mutableStateOf<UiMovement?>(null)};Column(Modifier.fillMaxSize().padding(16.dp)){MonthBar(mf.format(month.time),prev,next);Spacer(Modifier.height(8.dp));if(data.isEmpty())Text("Nessun movimento per questo mese.") else LazyColumn(verticalArrangement=Arrangement.spacedBy(6.dp)){items(data.sortedByDescending{parse(it.date)?.timeInMillis?:0L}){x->Card(Modifier.fillMaxWidth()){Column(Modifier.padding(12.dp)){Text(x.description.ifBlank{x.category},style=MaterialTheme.typography.titleMedium);Text("${x.category} • ${x.member} • ${x.date}");Text(if(x.type==MovementType.INCOME)"+${money.format(x.amount)}" else "-${money.format(x.amount)}");Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End){TextButton({edit(x)}){Text("Modifica")};TextButton({pending=x}){Text("Elimina")}}}}}}};pending?.let{x->AlertDialog(onDismissRequest={pending=null},title={Text("Eliminare?")},text={Text("${x.category} ${money.format(x.amount)}")},confirmButton={TextButton({remove(x);pending=null}){Text("Elimina")}},dismissButton={TextButton({pending=null}){Text("Annulla")}})}}

@Composable private fun Analysis(data:List<UiMovement>){val i=data.filter{it.type==MovementType.INCOME}.sumOf{it.amount};val o=data.filter{it.type==MovementType.EXPENSE}.sumOf{it.amount};val cats=data.filter{it.type==MovementType.EXPENSE}.groupBy{it.category}.mapValues{it.value.sumOf(UiMovement::amount)}.entries.sortedByDescending{it.value};LazyColumn(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){item{Text("Analisi storica",style=MaterialTheme.typography.headlineSmall)};item{Text("Entrate ${money.format(i)}");Text("Uscite ${money.format(o)}");Text("Saldo ${money.format(i-o)}",style=MaterialTheme.typography.titleLarge)};item{Text("Categorie",style=MaterialTheme.typography.titleLarge)};items(cats){e->Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(e.key);Text(money.format(e.value))}}}}

@Composable private fun Settings(members:List<String>,add:(String)->Unit){var v by remember{mutableStateOf("")};LazyColumn(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){item{Text("Impostazioni",style=MaterialTheme.typography.headlineSmall);Text("Membri della famiglia",style=MaterialTheme.typography.titleLarge)};items(members){Text(it)};item{OutlinedTextField(v,{v=it},label={Text("Nuovo membro")},modifier=Modifier.fillMaxWidth())};item{Button({add(v);v=""},Modifier.fillMaxWidth()){Text("Aggiungi membro")}};item{Text("Solo costi e ricavi reali storicizzati. Nessun budget.")}}}

@Composable private fun Editor(old:UiMovement?,members:List<String>,cancel:()->Unit,save:(UiMovement)->Unit){val c=LocalContext.current;var type by remember(old){mutableStateOf(old?.type?:MovementType.EXPENSE)};var amount by remember(old){mutableStateOf(old?.amount?.toString()?:"")};var cat by remember(old){mutableStateOf(old?.category?:outCats[0])};var desc by remember(old){mutableStateOf(old?.description?:"")};var date by remember(old){mutableStateOf(old?.date?:df.format(Date()))};var member by remember(old){mutableStateOf(old?.member?:members[0])};var pay by remember(old){mutableStateOf(old?.payment?:payments[0])};AlertDialog(onDismissRequest=cancel,title={Text(if(old==null)"Nuovo movimento" else "Modifica movimento")},text={Column(verticalArrangement=Arrangement.spacedBy(7.dp)){Row{Button({type=MovementType.EXPENSE;cat=outCats[0]}){Text("Uscita")};Spacer(Modifier.width(6.dp));Button({type=MovementType.INCOME;cat=inCats[0]}){Text("Entrata")}};OutlinedTextField(amount,{amount=it},label={Text("Importo (€)")});Choice("Categoria",cat,c,if(type==MovementType.INCOME)inCats else outCats){cat=it};Choice("Membro",member,c,members){member=it};Choice("Pagamento",pay,c,payments){pay=it};OutlinedTextField(desc,{desc=it},label={Text("Descrizione")});OutlinedButton({val x=parse(date)?:Calendar.getInstance();DatePickerDialog(c,{_,y,m,d->x.set(y,m,d);date=df.format(x.time)},x.get(1),x.get(2),x.get(5)).show()}){Text("Data $date")}}},confirmButton={TextButton(enabled=amount.replace(',','.').toDoubleOrNull()?.let{it>0}==true,onClick={save(UiMovement(old?.id?:System.currentTimeMillis(),type,amount.replace(',','.').toDouble(),cat,desc,date,member,pay))}){Text("Salva")}},dismissButton={TextButton(cancel){Text("Annulla")}})}
@Composable private fun Choice(label:String,value:String,c:Context,opts:List<String>,pick:(String)->Unit){OutlinedButton({android.app.AlertDialog.Builder(c).setTitle(label).setItems(opts.toTypedArray()){_,i->pick(opts[i])}.show()}){Text("$label: $value")}}
@Composable private fun MonthBar(label:String,prev:()->Unit,next:()->Unit){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){OutlinedButton(prev){Text("‹")};Text(label,modifier=Modifier.padding(top=10.dp));OutlinedButton(next){Text("›")}}}
private fun shift(c:Calendar,d:Int)=(c.clone() as Calendar).apply{add(Calendar.MONTH,d)}
private fun parse(s:String)=runCatching{df.parse(s)?.let{Calendar.getInstance().apply{time=it}}}.getOrNull()
private fun same(s:String,c:Calendar):Boolean{val d=parse(s)?:return false;return d.get(1)==c.get(1)&&d.get(2)==c.get(2)}
private fun Movement.ui()=UiMovement(id,type,amount,category,description,date,member,paymentMethod)
private fun UiMovement.model()=Movement(id,type,amount,category,description,date,member,payment)
