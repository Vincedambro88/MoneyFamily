package com.moneyfamily.app

import android.app.DatePickerDialog
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import androidx.compose.ui.platform.LocalContext
import com.moneyfamily.app.data.*
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

private val money = NumberFormat.getCurrencyInstance(Locale.ITALY)
private val df = SimpleDateFormat("dd/MM/yyyy", Locale.ITALY)
private val mf = SimpleDateFormat("MMMM yyyy", Locale.ITALIAN)
data class UiMovement(val id:Long,val type:MovementType,val amount:Double,val category:String,val description:String,val date:String,val member:String,val typeName:String = "")
class MainActivity:ComponentActivity(){override fun onCreate(s:Bundle?){super.onCreate(s);setContent{MoneyFamilyApp()}}}

@Composable private fun MoneyFamilyApp(){
 val c=LocalContext.current
 val repo=remember{RoomRepository(c)}
 val scope=rememberCoroutineScope()
 var data by remember{mutableStateOf<List<UiMovement>>(emptyList())}
 var types by remember{mutableStateOf<List<TypeEntity>>(emptyList())}
 var cats by remember{mutableStateOf<List<CategoryEntity>>(emptyList())}
 var members by remember{mutableStateOf<List<FamilyMemberEntity>>(emptyList())}
 var links by remember{mutableStateOf<List<TypeCategoryEntity>>(emptyList())}
 var tab by remember{mutableStateOf(0)}
 var month by remember{mutableStateOf(Calendar.getInstance())}
 var edit by remember{mutableStateOf<UiMovement?>(null)}
 var add by remember{mutableStateOf(false)}
 fun refresh(){scope.launch{data=repo.all().map{it.ui()};types=repo.allTypes();cats=repo.allCategories();members=repo.allMembers();links=repo.allMappings()}}
 LaunchedEffect(Unit){refresh()}
 DisposableEffect(Unit){onDispose{repo.close()}}
 fun save(x:UiMovement){scope.launch{val m=x.model();if(data.any{it.id==x.id})repo.update(m)else repo.insert(m);refresh()}}
 fun remove(x:UiMovement){scope.launch{repo.delete(x.model());refresh()}}
 MaterialTheme{Scaffold(bottomBar={NavigationBar{listOf("Dashboard","Operazioni","Inserisci","Impostazioni").forEachIndexed{i,t->NavigationBarItem(selected=tab==i,onClick={tab=i},icon={Text(t.take(1))},label={Text(t)})}}}){p->Column(Modifier.fillMaxSize().padding(p)){Text("MoneyFamily",style=MaterialTheme.typography.headlineSmall,modifier=Modifier.padding(16.dp));when(tab){0->Dashboard(data,month,{month=shift(month,-1)},{month=shift(month,1)},{add=true});1->Operations(data,types,cats,members,month,{month=shift(month,-1)},{month=shift(month,1)},{edit=it},{remove(it)});2->Editor(null,types,cats,members,links,repo,{tab=0}){save(it);tab=1};3->Configuration(types,cats,members,links,repo){refresh()}}}};if(add)Editor(null,types,cats,members,links,repo,{add=false}){save(it);add=false};edit?.let{e->Editor(e,types,cats,members,links,repo,{edit=null}){save(it);edit=null}}}
}

@Composable private fun Dashboard(data:List<UiMovement>,month:Calendar,prev:()->Unit,next:()->Unit,add:()->Unit){
 val cur=data.filter{same(it.date,month)}
 val income=cur.filter{it.amount>0}.sumOf{it.amount}
 val expense=cur.filter{it.amount<0}.sumOf{it.amount}
 val balance=income+expense
 val catTotals=cur.groupBy{it.category.ifBlank{"Non classificata"}}.mapValues{(_,v)->v.sumOf{it.amount}}.filterValues{it!=0.0}.toList().sortedBy{it.second}
 val memberTotals=cur.groupBy{it.member.ifBlank{"Non assegnato"}}.mapValues{(_,v)->v.sumOf{it.amount}}.filterValues{it!=0.0}.toList().sortedByDescending{it.second}
 val typeTotals=cur.groupBy{it.typeName.ifBlank{"Non classificata"}}.mapValues{(_,v)->v.sumOf{it.amount}}.filterValues{it!=0.0}.toList().sortedBy{it.second}
 LazyColumn(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
  item{MonthBar(mf.format(month.time),prev,next)}
  item{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){MetricCard("Entrate",income,Modifier.weight(1f));MetricCard("Uscite",expense,Modifier.weight(1f));MetricCard("Saldo",balance,Modifier.weight(1f))}}
  item{PieChartCard("Composizione mensile",listOf("Entrate" to income,"Uscite" to expense))}
  item{BarChartCard("Totali per categoria",catTotals)}
  item{BarChartCard("Totali per componente",memberTotals)}
  item{BarChartCard("Totali per tipologia",typeTotals)}
  item{Card(Modifier.fillMaxWidth()){Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(4.dp)){Text("Riepilogo",style=MaterialTheme.typography.titleLarge);Text("Operazioni: ${cur.size}");Text("Spese: ${money.format(expense)}",color=NegativeColor);Text("Ricavi: ${money.format(income)}",color=PositiveColor);Text("Saldo: ${money.format(balance)}",color=if(balance<0)NegativeColor else PositiveColor)}}}
  item{Button(onClick=add,modifier=Modifier.fillMaxWidth()){Text("+ Inserisci operazione")}}
 }
}

private val PositiveColor=androidx.compose.ui.graphics.Color(0xFF2E7D32)
private val NegativeColor=androidx.compose.ui.graphics.Color(0xFFC62828)

@Composable private fun MetricCard(title:String,value:Double,modifier:Modifier){Card(modifier){Column(Modifier.padding(10.dp)){Text(title,style=MaterialTheme.typography.labelLarge);Text(money.format(value),style=MaterialTheme.typography.titleMedium,color=if(value<0)NegativeColor else PositiveColor)}}}

@Composable private fun PieChartCard(title:String,values:List<Pair<String,Double>>){
 val positive=values.sumOf{if(it.second>0)it.second else 0.0}
 val negative=values.sumOf{if(it.second<0)kotlin.math.abs(it.second) else 0.0}
 val total=positive+negative
 Card(Modifier.fillMaxWidth()){Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
  Text(title,style=MaterialTheme.typography.titleLarge)
  if(total==0.0) Text("Nessun movimento nel mese") else {
   Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
    Canvas(Modifier.size(150.dp)){
     var start=0f
     if(positive>0){val sweep=(positive/total*360.0).toFloat();drawArc(PositiveColor,start,sweep,true);start+=sweep}
     if(negative>0){val sweep=(negative/total*360.0).toFloat();drawArc(NegativeColor,start,sweep,true)}
    }
    Spacer(Modifier.width(16.dp))
    Column(verticalArrangement=Arrangement.spacedBy(6.dp)){
     if(positive>0)LegendRow("Entrate",positive,PositiveColor)
     if(negative>0)LegendRow("Uscite",-negative,NegativeColor)
    }
   }
  }
 }}
}

@Composable private fun LegendRow(name:String,value:Double,color:androidx.compose.ui.graphics.Color){Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){Box(Modifier.size(12.dp).clip(RoundedCornerShape(3.dp)).background(color));Text("$name  ${money.format(value)}")}}

@Composable private fun BarChartCard(title:String,values:List<Pair<String,Double>>){Card(Modifier.fillMaxWidth()){Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(9.dp)){Text(title,style=MaterialTheme.typography.titleLarge);if(values.isEmpty())Text("Nessun dato per il mese")else{val maxAbs=values.maxOf{abs(it.second)}.coerceAtLeast(1.0);values.take(12).forEach{(n,v)->BarChartRow(n,v,maxAbs)}}}}}

@Composable private fun BarChartRow(name:String,value:Double,maxAbs:Double){val fraction=(abs(value)/maxAbs).toFloat().coerceIn(0f,1f);val color=if(value<0)NegativeColor else PositiveColor;Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(4.dp)){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(name,Modifier.weight(1f));Text(money.format(value),color=color)};Box(Modifier.fillMaxWidth().height(18.dp).clip(RoundedCornerShape(9.dp)).background(MaterialTheme.colorScheme.surfaceVariant)){if(fraction>0)Box(Modifier.fillMaxWidth(fraction).fillMaxHeight().background(color))}}}

@Composable private fun Operations(data:List<UiMovement>,types:List<TypeEntity>,cats:List<CategoryEntity>,members:List<FamilyMemberEntity>,month:Calendar,prev:()->Unit,next:()->Unit,edit:(UiMovement)->Unit,remove:(UiMovement)->Unit){var q by remember{mutableStateOf("")};var type by remember{mutableStateOf("")};var cat by remember{mutableStateOf("")};var member by remember{mutableStateOf("")};var kind by remember{mutableStateOf("")};val filtered=data.filter{same(it.date,month)&&it.description.contains(q,true)&&(type.isBlank()||it.typeName==type)&&(cat.isBlank()||it.category==cat)&&(member.isBlank()||it.member==member)&&(kind.isBlank()||(kind=="Spese"&&it.amount<0)||(kind=="Ricavi"&&it.amount>0))};Column(Modifier.fillMaxSize().padding(16.dp)){MonthBar(mf.format(month.time),prev,next);OutlinedTextField(value=q,onValueChange={q=it},label={Text("Cerca descrizione")},modifier=Modifier.fillMaxWidth());Choice("Tipologia",type.ifBlank{"Tutte"},LocalContext.current,listOf("Tutte")+types.map{it.name}){type=if(it=="Tutte")"" else it};Choice("Categoria",cat.ifBlank{"Tutte"},LocalContext.current,listOf("Tutte")+cats.map{it.name}){cat=if(it=="Tutte")"" else it};Choice("Componente",member.ifBlank{"Tutti"},LocalContext.current,listOf("Tutti")+members.map{it.name}){member=if(it=="Tutti")"" else it};Choice("Tipo",kind.ifBlank{"Tutti"},LocalContext.current,listOf("Tutti","Spese","Ricavi")){kind=if(it=="Tutti")"" else it};LazyColumn(verticalArrangement=Arrangement.spacedBy(6.dp)){items(filtered.sortedByDescending{parse(it.date)?.timeInMillis?:0L}){x->Card(Modifier.fillMaxWidth()){Column(Modifier.padding(12.dp)){Text(x.description.ifBlank{x.type.name},style=MaterialTheme.typography.titleMedium);Text("${x.typeName.ifBlank{"Non classificata"}} • ${x.category} • ${x.member} • ${x.date}");Text(money.format(x.amount));Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End){TextButton(onClick={edit(x)}){Text("Modifica")};TextButton(onClick={remove(x)}){Text("Elimina")}}}}}}}}

@Composable private fun Editor(old:UiMovement?,types:List<TypeEntity>,cats:List<CategoryEntity>,members:List<FamilyMemberEntity>,links:List<TypeCategoryEntity>,repo:RoomRepository,cancel:()->Unit,save:(UiMovement)->Unit){val c=LocalContext.current;var amount by remember(old){mutableStateOf(old?.amount?.toString()?:"")};var desc by remember(old){mutableStateOf(old?.description?:"")};var date by remember(old){mutableStateOf(old?.date?:df.format(Date()))};var type by remember(old){mutableStateOf(old?.type?.name?:"")};var category by remember(old){mutableStateOf(old?.category?:"")};var member by remember(old){mutableStateOf(old?.member?:"")};LaunchedEffect(types,members,old){if(old==null){if(type.isBlank())type=types.firstOrNull()?.name?:"";if(member.isBlank())member=members.firstOrNull()?.name?:""}};LaunchedEffect(type,links,cats){if(old==null&&type.isNotBlank()){val t=types.find{it.name==type};val l=links.find{it.typeId==t?.id};if(l!=null)category=cats.find{it.id==l.categoryId}?.name?:category}};AlertDialog(onDismissRequest=cancel,title={Text(if(old==null)"Nuova operazione" else "Modifica operazione")},text={Column(verticalArrangement=Arrangement.spacedBy(7.dp)){OutlinedTextField(value=amount,onValueChange={amount=it},label={Text("Importo (+ ricavo / - spesa)")},modifier=Modifier.fillMaxWidth());Choice("Tipologia",type.ifBlank{"Seleziona"},c,types.filter{it.active}.map{it.name}){type=it};Choice("Categoria",category.ifBlank{"Seleziona"},c,cats.filter{it.active}.map{it.name}){category=it};Choice("Effettuata da",member.ifBlank{"Seleziona"},c,members.filter{it.active||it.name==old?.member}.map{it.name}){member=it};OutlinedTextField(value=desc,onValueChange={desc=it},label={Text("Descrizione")},modifier=Modifier.fillMaxWidth());OutlinedButton(onClick={val x=parse(date)?:Calendar.getInstance();DatePickerDialog(c,{_,y,m,d->x.set(y,m,d);date=df.format(x.time)},x.get(Calendar.YEAR),x.get(Calendar.MONTH),x.get(Calendar.DAY_OF_MONTH)).show()}){Text("Data $date")}}},confirmButton={TextButton(enabled=amount.replace(',','.').toDoubleOrNull()!=null&&type.isNotBlank()&&category.isNotBlank()&&member.isNotBlank(),onClick={val a=amount.replace(',','.').toDouble();save(UiMovement(old?.id?:System.currentTimeMillis(),if(a<0)MovementType.EXPENSE else MovementType.INCOME,a,category,desc,date,member,type))}){Text("Salva")}},dismissButton={TextButton(onClick=cancel){Text("Annulla")}})}

@Composable private fun Configuration(types:List<TypeEntity>,cats:List<CategoryEntity>,members:List<FamilyMemberEntity>,links:List<TypeCategoryEntity>,repo:RoomRepository,refresh:()->Unit){var section by remember{mutableStateOf(0)};Column(Modifier.fillMaxSize().padding(16.dp)){Text("Configurazione",style=MaterialTheme.typography.headlineSmall);Row{listOf("Tipologie","Categorie","Famiglia","Associazioni").forEachIndexed{i,t->TextButton(onClick={section=i}){Text(t)}}};when(section){0->EntityConfigTypes("Tipologie",types,repo,refresh);1->EntityConfigCats("Categorie",cats,repo,refresh);2->MemberConfig(members,repo,refresh);3->LinkConfig(types,cats,links,repo,refresh)}}}
@Composable private fun EntityConfigTypes(title:String,items:List<TypeEntity>,repo:RoomRepository,refresh:()->Unit){var name by remember{mutableStateOf("")};val scope=rememberCoroutineScope();Column(verticalArrangement=Arrangement.spacedBy(8.dp)){items.forEach{Text(it.name)};OutlinedTextField(value=name,onValueChange={name=it},label={Text("Nuova $title")},modifier=Modifier.fillMaxWidth());Button(onClick={if(name.isNotBlank()){val n=name.trim();name="";scope.launch{repo.addType(n);refresh()}}},modifier=Modifier.fillMaxWidth()){Text("Aggiungi")}}}
@Composable private fun EntityConfigCats(title:String,items:List<CategoryEntity>,repo:RoomRepository,refresh:()->Unit){var name by remember{mutableStateOf("")};val scope=rememberCoroutineScope();Column(verticalArrangement=Arrangement.spacedBy(8.dp)){items.forEach{Text(it.name)};OutlinedTextField(value=name,onValueChange={name=it},label={Text("Nuova $title")},modifier=Modifier.fillMaxWidth());Button(onClick={if(name.isNotBlank()){val n=name.trim();name="";scope.launch{repo.addCategory(n);refresh()}}},modifier=Modifier.fillMaxWidth()){Text("Aggiungi")}}}
@Composable private fun MemberConfig(items:List<FamilyMemberEntity>,repo:RoomRepository,refresh:()->Unit){var name by remember{mutableStateOf("")};val scope=rememberCoroutineScope();Column(verticalArrangement=Arrangement.spacedBy(8.dp)){items.forEach{m->Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(m.name);Text(if(m.active)"Attivo" else "Disattivo")}};OutlinedTextField(value=name,onValueChange={name=it},label={Text("Nuovo componente")},modifier=Modifier.fillMaxWidth());Button(onClick={if(name.isNotBlank()){val n=name.trim();name="";scope.launch{repo.addMember(n);refresh()}}},modifier=Modifier.fillMaxWidth()){Text("Aggiungi")}}}
@Composable private fun LinkConfig(types:List<TypeEntity>,cats:List<CategoryEntity>,links:List<TypeCategoryEntity>,repo:RoomRepository,refresh:()->Unit){val c=LocalContext.current;val scope=rememberCoroutineScope();LazyColumn{items(types.filter{it.active}){t->val current=cats.find{it.id==links.find{l->l.typeId==t.id}?.categoryId}?.name?:"Non associata";Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(t.name);TextButton(onClick={ChoiceDialog(c,"Categoria",cats.filter{it.active}.map{it.name}){n->cats.find{it.name==n}?.let{cat->scope.launch{repo.setTypeCategory(t.id,cat.id);refresh()}}}}){Text(current)}}}}}
private fun ChoiceDialog(c:Context,title:String,opts:List<String>,pick:(String)->Unit){android.app.AlertDialog.Builder(c).setTitle(title).setItems(opts.toTypedArray()){_,i->pick(opts[i])}.show()}
@Composable private fun Choice(label:String,value:String,c:Context,opts:List<String>,pick:(String)->Unit){OutlinedButton(onClick={ChoiceDialog(c,label,opts,pick)},modifier=Modifier.fillMaxWidth()){Text("$label: $value")}}
@Composable private fun MonthBar(label:String,prev:()->Unit,next:()->Unit){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){OutlinedButton(onClick=prev){Text("‹")};Text(label,Modifier.padding(top=10.dp));OutlinedButton(onClick=next){Text("›")}}}
private fun shift(c:Calendar,d:Int)=(c.clone() as Calendar).apply{add(Calendar.MONTH,d)}
private fun parse(s:String)=runCatching{df.parse(s)?.let{Calendar.getInstance().apply{time=it}}}.getOrNull()
private fun same(s:String,c:Calendar):Boolean{val d=parse(s)?:return false;return d.get(Calendar.YEAR)==c.get(Calendar.YEAR)&&d.get(Calendar.MONTH)==c.get(Calendar.MONTH)}
private fun Movement.ui()=UiMovement(id,type,amount,category,description,date,member,typeName)
private fun UiMovement.model()=Movement(id,type,amount,category,description,date,member,"",typeName)
