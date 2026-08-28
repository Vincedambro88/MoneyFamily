package com.moneyfamily.app

import android.app.DatePickerDialog
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
 MaterialTheme{Scaffold(bottomBar={NavigationBar{listOf("Dashboard","Operazioni","Inserisci","Impostazioni").forEachIndexed{i,t->NavigationBarItem(selected=tab==i,onClick={tab=i},icon={Text(t.take(1))},label={Text(t)})}}}){p->Column(Modifier.fillMaxSize().padding(p)){Text("MoneyFamily",style=MaterialTheme.typography.headlineSmall,modifier=Modifier.padding(16.dp));when(tab){0->Dashboard(data,month,{month=shift(month,-1)},{month=shift(month,1)},{add=true});1->Operations(data,types,cats,members,month,{month=shift(month,-1)},{month=shift(month,1)},{edit=it},{remove(it)});2->InsertScreen(types,cats,members,links,repo,{save(it);tab=1});3->Configuration(types,cats,members,links,repo){refresh()}}}};if(add)Editor(null,types,cats,members,links,repo,{add=false}){save(it);add=false};edit?.let{e->Editor(e,types,cats,members,links,repo,{edit=null}){save(it);edit=null}}}
}

@Composable private fun Dashboard(data:List<UiMovement>,month:Calendar,prev:()->Unit,next:()->Unit,add:()->Unit){
 var annualPage by remember{mutableStateOf(false)}
 val cur=data.filter{same(it.date,month)}
 val income=cur.filter{it.amount>0}.sumOf{it.amount}
 val expense=cur.filter{it.amount<0}.sumOf{it.amount}
 val balance=income+expense
 val catTotals=cur.groupBy{it.category.ifBlank{"Non classificata"}}.mapValues{(_,v)->v.sumOf{it.amount}}.filterValues{it!=0.0}.toList().sortedBy{it.second}
 val memberTotals=cur.groupBy{it.member.ifBlank{"Non assegnato"}}.mapValues{(_,v)->v.sumOf{it.amount}}.filterValues{it!=0.0}.toList().sortedByDescending{it.second}
 val typeTotals=cur.groupBy{it.typeName.takeIf{name->name.isNotBlank()&&!name.equals("EXPENSE",true)&&!name.equals("INCOME",true)}?:"Da classificare"}.mapValues{(_,v)->v.sumOf{it.amount}}.filterValues{it!=0.0}.toList().sortedBy{it.second}
 val goPrev={if(annualPage){annualPage=false}else{prev()}}
 val goNext={if(annualPage){annualPage=false;next()}else if(month.get(Calendar.MONTH)==Calendar.DECEMBER){annualPage=true}else{next()}}
 if(annualPage){AnnualSummaryPage(data,month,goPrev,goNext,add)}else{LazyColumn(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
  item{MonthBar(mf.format(month.time),goPrev,goNext)}
  item{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){MetricCard("Entrate",income,Modifier.weight(1f));MetricCard("Uscite",expense,Modifier.weight(1f));MetricCard("Saldo",balance,Modifier.weight(1f))}}
  item{PieChartCard("Composizione mensile",listOf("Entrate" to income,"Uscite" to expense))}
  item{BarChartCard("Totali per categoria",catTotals)}
  item{PieChartCard("Composizione per categoria",catTotals)}
  item{BarChartCard("Totali per componente",memberTotals)}
  item{BarChartCard("Totali per tipologia",typeTotals)}
  item{PieChartCard("Composizione per tipologia",typeTotals)}
  item{Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(24.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surfaceVariant)){Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){Text("Riepilogo",style=MaterialTheme.typography.titleLarge);Text("Operazioni: ${cur.size}");Text("Spese: ${money.format(expense)}",color=NegativeColor);Text("Ricavi: ${money.format(income)}",color=PositiveColor);Text("Saldo: ${money.format(balance)}",color=if(balance<0)NegativeColor else PositiveColor)}}}
  item{if(month.get(Calendar.MONTH)==Calendar.DECEMBER){OutlinedButton(onClick={annualPage=true},modifier=Modifier.fillMaxWidth()){Text("Riepilogo annuale ${month.get(Calendar.YEAR)}")}}}
  item{Button(onClick=add,modifier=Modifier.fillMaxWidth()){Text("+ Inserisci operazione")}}
 }}
}

@Composable private fun AnnualSummaryPage(data:List<UiMovement>,month:Calendar,prev:()->Unit,next:()->Unit,add:()->Unit){
 val year=month.get(Calendar.YEAR)
 val yearly=data.filter{parse(it.date)?.get(Calendar.YEAR)==year}
 val income=yearly.filter{it.amount>0}.sumOf{it.amount}
 val expense=yearly.filter{it.amount<0}.sumOf{it.amount}
 val balance=income+expense
 val catTotals=yearly.groupBy{it.category.ifBlank{"Non classificata"}}.mapValues{(_,v)->v.sumOf{it.amount}}.filterValues{it!=0.0}.toList().sortedBy{it.second}
 val memberTotals=yearly.groupBy{it.member.ifBlank{"Non assegnato"}}.mapValues{(_,v)->v.sumOf{it.amount}}.filterValues{it!=0.0}.toList().sortedByDescending{it.second}
 val typeTotals=yearly.groupBy{it.typeName.takeIf{name->name.isNotBlank()&&!name.equals("EXPENSE",true)&&!name.equals("INCOME",true)}?:"Da classificare"}.mapValues{(_,v)->v.sumOf{it.amount}}.filterValues{it!=0.0}.toList().sortedBy{it.second}
 LazyColumn(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
  item{MonthBar("Riepilogo $year",prev,next)}
  item{Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(24.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surfaceVariant)){Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){Text("Riepilogo esercizio $year",style=MaterialTheme.typography.titleLarge);Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){MetricCard("Entrate",income,Modifier.weight(1f));MetricCard("Uscite",expense,Modifier.weight(1f));MetricCard("Saldo",balance,Modifier.weight(1f))}}}}
  item{AnnualSummaryCard(data,month)}
  item{PieChartCard("Composizione esercizio $year",listOf("Entrate" to income,"Uscite" to expense))}
  item{BarChartCard("Totali per categoria — $year",catTotals)}
  item{PieChartCard("Composizione per categoria — $year",catTotals)}
  item{BarChartCard("Totali per componente — $year",memberTotals)}
  item{BarChartCard("Totali per tipologia — $year",typeTotals)}
  item{PieChartCard("Composizione per tipologia — $year",typeTotals)}
  item{Button(onClick=add,modifier=Modifier.fillMaxWidth()){Text("+ Inserisci operazione")}}
 }
}

private val PositiveColor=androidx.compose.ui.graphics.Color(0xFF2E7D32)
private val NegativeColor=androidx.compose.ui.graphics.Color(0xFFC62828)

@Composable private fun MetricCard(title:String,value:Double,modifier:Modifier){
 Card(modifier,shape=RoundedCornerShape(22.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surfaceVariant)){Column(Modifier.padding(horizontal=14.dp,vertical=12.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){Text(title,style=MaterialTheme.typography.labelLarge);Text(money.format(value),style=MaterialTheme.typography.titleMedium,color=if(value<0)NegativeColor else PositiveColor)}}
}

@Composable private fun PieChartCard(title:String,values:List<Pair<String,Double>>){
 val palette=listOf(
  androidx.compose.ui.graphics.Color(0xFF4F46E5),
  androidx.compose.ui.graphics.Color(0xFF16A34A),
  androidx.compose.ui.graphics.Color(0xFFEA580C),
  androidx.compose.ui.graphics.Color(0xFF0891B2),
  androidx.compose.ui.graphics.Color(0xFFDB2777),
  androidx.compose.ui.graphics.Color(0xFF7C3AED),
  androidx.compose.ui.graphics.Color(0xFFCA8A04),
  androidx.compose.ui.graphics.Color(0xFF0F766E),
  androidx.compose.ui.graphics.Color(0xFFDC2626),
  androidx.compose.ui.graphics.Color(0xFF475569)
 )
 val nonZero=values.filter{it.second!=0.0}
 val total=nonZero.sumOf{abs(it.second)}
 Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(24.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surfaceVariant)){
  Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
   Text(title,style=MaterialTheme.typography.titleLarge)
   if(total==0.0){Text("Nessun dato")}
   else{
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
     Box(Modifier.size(158.dp),contentAlignment=Alignment.Center){
      Canvas(Modifier.fillMaxSize().padding(7.dp)){
       var start=0f
       nonZero.forEachIndexed{index,entry->
        val sweep=(abs(entry.second)/total*360.0).toFloat()
        drawArc(color=palette[index%palette.size],startAngle=start,sweepAngle=sweep,useCenter=false,style=androidx.compose.ui.graphics.drawscope.Stroke(width=42f,cap=androidx.compose.ui.graphics.StrokeCap.Butt))
        start+=sweep
       }
      }
      Column(horizontalAlignment=Alignment.CenterHorizontally){
       Text(money.format(nonZero.sumOf{it.second}),style=MaterialTheme.typography.titleMedium)
       Text("totale",style=MaterialTheme.typography.labelSmall)
      }
     }
     Spacer(Modifier.width(16.dp))
     Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp)){
      nonZero.take(10).forEachIndexed{index,entry->
       val color=palette[index%palette.size]
       Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){
        Box(Modifier.size(11.dp).clip(RoundedCornerShape(50)).background(color))
        Column(Modifier.weight(1f)){
         Text(entry.first,style=MaterialTheme.typography.labelLarge,maxLines=1)
         Text(money.format(entry.second),style=MaterialTheme.typography.bodyMedium,color=if(entry.second<0)NegativeColor else PositiveColor)
        }
       }
      }
     }
    }
   }
  }
 }
}

@Composable private fun LegendRow(name:String,value:Double,color:androidx.compose.ui.graphics.Color){Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(9.dp)){Box(Modifier.size(11.dp).clip(RoundedCornerShape(50)).background(color));Column{Text(name,style=MaterialTheme.typography.labelLarge);Text(money.format(value),style=MaterialTheme.typography.bodyMedium,color=color)}}}

@Composable private fun BarChartCard(title:String,values:List<Pair<String,Double>>){Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(24.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surfaceVariant)){Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(11.dp)){Text(title,style=MaterialTheme.typography.titleLarge);if(values.isEmpty())Text("Nessun dato per il mese")else{val maxAbs=values.maxOf{abs(it.second)}.coerceAtLeast(1.0);values.take(12).forEach{(n,v)->BarChartRow(n,v,maxAbs)}}}}}

@Composable private fun BarChartRow(name:String,value:Double,maxAbs:Double){val fraction=(abs(value)/maxAbs).toFloat().coerceIn(0f,1f);val color=if(value<0)NegativeColor else PositiveColor;Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(5.dp)){Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text(name,Modifier.weight(1f),style=MaterialTheme.typography.bodyLarge);Text(money.format(value),color=color,style=MaterialTheme.typography.bodyLarge)};Box(Modifier.fillMaxWidth().height(22.dp).clip(RoundedCornerShape(11.dp)).background(MaterialTheme.colorScheme.surface.copy(alpha=.55f))){if(fraction>0)Box(Modifier.fillMaxWidth(fraction).fillMaxHeight().clip(RoundedCornerShape(11.dp)).background(color))}}}

@Composable private fun Operations(data:List<UiMovement>,types:List<TypeEntity>,cats:List<CategoryEntity>,members:List<FamilyMemberEntity>,month:Calendar,prev:()->Unit,next:()->Unit,edit:(UiMovement)->Unit,remove:(UiMovement)->Unit){var q by remember{mutableStateOf("")};var type by remember{mutableStateOf("")};var cat by remember{mutableStateOf("")};var member by remember{mutableStateOf("")};var kind by remember{mutableStateOf("")};val filtered=data.filter{same(it.date,month)&&it.description.contains(q,true)&&(type.isBlank()||it.typeName==type)&&(cat.isBlank()||it.category==cat)&&(member.isBlank()||it.member==member)&&(kind.isBlank()||(kind=="Spese"&&it.amount<0)||(kind=="Ricavi"&&it.amount>0))};Column(Modifier.fillMaxSize().padding(16.dp)){MonthBar(mf.format(month.time),prev,next);OutlinedTextField(value=q,onValueChange={q=it},label={Text("Cerca descrizione")},modifier=Modifier.fillMaxWidth());Choice("Tipologia",type.ifBlank{"Tutte"},LocalContext.current,listOf("Tutte")+types.map{it.name}){type=if(it=="Tutte")"" else it};Choice("Categoria",cat.ifBlank{"Tutte"},LocalContext.current,listOf("Tutte")+cats.map{it.name}){cat=if(it=="Tutte")"" else it};Choice("Componente",member.ifBlank{"Tutti"},LocalContext.current,listOf("Tutti")+members.map{it.name}){member=if(it=="Tutti")"" else it};Choice("Tipo",kind.ifBlank{"Tutti"},LocalContext.current,listOf("Tutti","Spese","Ricavi")){kind=if(it=="Tutti")"" else it};LazyColumn(verticalArrangement=Arrangement.spacedBy(6.dp)){items(filtered.sortedByDescending{parse(it.date)?.timeInMillis?:0L}){x->Card(Modifier.fillMaxWidth()){Column(Modifier.padding(12.dp)){Text(x.description.ifBlank{x.type.name},style=MaterialTheme.typography.titleMedium);Text("${x.typeName.ifBlank{"Non classificata"}} • ${x.category} • ${x.member} • ${x.date}");Text(money.format(x.amount));Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End){TextButton(onClick={edit(x)}){Text("Modifica")};TextButton(onClick={remove(x)}){Text("Elimina")}}}}}}}}

@Composable private fun Editor(old:UiMovement?,types:List<TypeEntity>,cats:List<CategoryEntity>,members:List<FamilyMemberEntity>,links:List<TypeCategoryEntity>,repo:RoomRepository,cancel:()->Unit,save:(UiMovement)->Unit){val c=LocalContext.current;var amount by remember(old){mutableStateOf(old?.amount?.toString()?:"")};var desc by remember(old){mutableStateOf(old?.description?:"")};var date by remember(old){mutableStateOf(old?.date?:df.format(Date()))};var type by remember(old){mutableStateOf(old?.typeName?.takeIf{it.isNotBlank() && !it.equals("EXPENSE",true)}?:"")};var category by remember(old){mutableStateOf(old?.category?:"")};var member by remember(old){mutableStateOf(old?.member?:"")};LaunchedEffect(types,members,old){if(old==null){if(type.isBlank())type=types.firstOrNull()?.name?:"";if(member.isBlank())member=members.firstOrNull()?.name?:""}};LaunchedEffect(type,links,cats){if(old==null&&type.isNotBlank()){val t=types.find{it.name==type};val l=links.find{it.typeId==t?.id};if(l!=null)category=cats.find{it.id==l.categoryId}?.name?:category}};AlertDialog(onDismissRequest=cancel,title={Text(if(old==null)"Nuova operazione" else "Modifica operazione")},text={Column(verticalArrangement=Arrangement.spacedBy(7.dp)){OutlinedTextField(value=amount,onValueChange={amount=it},label={Text("Importo (+ ricavo / - spesa)")},modifier=Modifier.fillMaxWidth());Choice("Tipologia",type.ifBlank{"Seleziona"},c,types.filter{it.active}.map{it.name}){type=it};Choice("Categoria",category.ifBlank{"Seleziona"},c,cats.filter{it.active}.map{it.name}){category=it};Choice("Effettuata da",member.ifBlank{"Seleziona"},c,members.filter{it.active||it.name==old?.member}.map{it.name}){member=it};OutlinedTextField(value=desc,onValueChange={desc=it},label={Text("Descrizione")},modifier=Modifier.fillMaxWidth());OutlinedButton(onClick={val x=parse(date)?:Calendar.getInstance();DatePickerDialog(c,{_,y,m,d->x.set(y,m,d);date=df.format(x.time)},x.get(Calendar.YEAR),x.get(Calendar.MONTH),x.get(Calendar.DAY_OF_MONTH)).show()}){Text("Data $date")}}},confirmButton={TextButton(enabled=amount.replace(',','.').toDoubleOrNull()!=null&&type.isNotBlank()&&category.isNotBlank()&&member.isNotBlank(),onClick={val a=amount.replace(',','.').toDouble();save(UiMovement(old?.id?:System.currentTimeMillis(),if(a<0)MovementType.EXPENSE else MovementType.INCOME,a,category,desc,date,member,type))}){Text("Salva")}},dismissButton={TextButton(onClick=cancel){Text("Annulla")}})}


@Composable private fun AnnualSummaryCard(data:List<UiMovement>,month:Calendar){
 val year=month.get(Calendar.YEAR);val yearly=data.filter{parse(it.date)?.get(Calendar.YEAR)==year};val income=yearly.filter{it.amount>0}.sumOf{it.amount};val expense=yearly.filter{it.amount<0}.sumOf{it.amount};val balance=income+expense
 val monthly=(0..11).map{m->m to yearly.filter{parse(it.date)?.get(Calendar.MONTH)==m}.sumOf{it.amount}}
 Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(24.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surfaceVariant)){Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Text("Riepilogo esercizio $year",style=MaterialTheme.typography.titleLarge);Text("${yearly.size} operazioni",style=MaterialTheme.typography.labelMedium)}
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){MetricCard("Entrate",income,Modifier.weight(1f));MetricCard("Uscite",expense,Modifier.weight(1f));MetricCard("Saldo",balance,Modifier.weight(1f))}
  Text("Totale mensile",style=MaterialTheme.typography.titleMedium);val maxAbs=monthly.maxOf{abs(it.second)}.coerceAtLeast(1.0)
  monthly.forEach{(m,v)->Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){Text(SimpleDateFormat("MMM",Locale.ITALIAN).format(Calendar.getInstance().apply{set(Calendar.MONTH,m)}.time),Modifier.width(34.dp),style=MaterialTheme.typography.labelMedium);Box(Modifier.weight(1f).height(14.dp).clip(RoundedCornerShape(7.dp)).background(MaterialTheme.colorScheme.surface.copy(alpha=.55f))){if(v!=0.0)Box(Modifier.fillMaxWidth((abs(v)/maxAbs).toFloat().coerceIn(0f,1f)).fillMaxHeight().clip(RoundedCornerShape(7.dp)).background(if(v<0)NegativeColor else PositiveColor))};Text(money.format(v),Modifier.width(92.dp),color=if(v<0)NegativeColor else PositiveColor,style=MaterialTheme.typography.labelSmall)}}
 }}
}

@Composable private fun InsertScreen(types:List<TypeEntity>,cats:List<CategoryEntity>,members:List<FamilyMemberEntity>,links:List<TypeCategoryEntity>,repo:RoomRepository,save:(UiMovement)->Unit){
 val context=LocalContext.current;val scope=rememberCoroutineScope();var status by remember{mutableStateOf("")};var showEditor by remember{mutableStateOf(false)}
 val launcher=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->if(uri!=null)scope.launch{runCatching{val rows=ExcelImporter.import(context,uri);rows.forEachIndexed{idx,r->val te=types.firstOrNull{it.name.equals(r.typeName,true)};val mapped=if(r.category.isBlank())links.firstOrNull{it.typeId==te?.id}?.let{l->cats.firstOrNull{c->c.id==l.categoryId}?.name}.orEmpty() else r.category;repo.insert(Movement(id=System.currentTimeMillis()+idx,type=if(r.amount<0)MovementType.EXPENSE else MovementType.INCOME,amount=r.amount,category=mapped.ifBlank{cats.firstOrNull()?.name.orEmpty()},description=r.description,date=r.date,member=r.member.ifBlank{members.firstOrNull()?.name.orEmpty()},paymentMethod="",typeName=r.typeName))};status="Importate ${rows.size} operazioni"}.onFailure{status="Errore importazione: ${it.message?:"file non valido"}"}}}
 Column(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){Text("Inserisci",style=MaterialTheme.typography.headlineSmall);Button(onClick={showEditor=true},modifier=Modifier.fillMaxWidth()){Text("+ Nuova operazione")};OutlinedButton(onClick={launcher.launch(arrayOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet","application/vnd.ms-excel","text/csv"))},modifier=Modifier.fillMaxWidth()){Text("Importa da Excel")};Text("Excel obbligatorio: Data | Importo | Tipologia | Categoria | Componente | Descrizione. Tutti i campi devono essere compilati.",style=MaterialTheme.typography.bodyMedium);if(status.isNotBlank())Text(status,color=if(status.startsWith("Errore"))NegativeColor else PositiveColor)}
 if(showEditor)Editor(null,types,cats,members,links,repo,{showEditor=false}){save(it);showEditor=false}
}

@Composable private fun Configuration(types:List<TypeEntity>,cats:List<CategoryEntity>,members:List<FamilyMemberEntity>,links:List<TypeCategoryEntity>,repo:RoomRepository,refresh:()->Unit){var section by remember{mutableStateOf(0)};Column(Modifier.fillMaxSize().padding(16.dp)){Text("Configurazione",style=MaterialTheme.typography.headlineSmall);Row(Modifier.fillMaxWidth()){listOf("Tipologie","Categorie","Famiglia","Associazioni").forEachIndexed{i,t->TextButton(onClick={section=i}){Text(t)}}};Box(Modifier.fillMaxWidth().weight(1f)){when(section){0->EntityConfigTypes("Tipologie",types,repo,refresh);1->EntityConfigCats("Categorie",cats,repo,refresh);2->MemberConfig(members,repo,refresh);3->LinkConfig(types,cats,links,repo,refresh)}}}}
@Composable private fun EntityConfigTypes(title:String,items:List<TypeEntity>,repo:RoomRepository,refresh:()->Unit){var name by remember{mutableStateOf("")};val scope=rememberCoroutineScope();Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)){items.forEach{Text(it.name,modifier=Modifier.fillMaxWidth().padding(vertical=2.dp))};OutlinedTextField(value=name,onValueChange={name=it},label={Text("Nuova $title")},modifier=Modifier.fillMaxWidth());Button(onClick={if(name.isNotBlank()){val n=name.trim();name="";scope.launch{repo.addType(n);refresh()}}},modifier=Modifier.fillMaxWidth()){Text("Aggiungi")}}}
@Composable private fun EntityConfigCats(title:String,items:List<CategoryEntity>,repo:RoomRepository,refresh:()->Unit){var name by remember{mutableStateOf("")};val scope=rememberCoroutineScope();Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)){items.forEach{Text(it.name,modifier=Modifier.fillMaxWidth().padding(vertical=2.dp))};OutlinedTextField(value=name,onValueChange={name=it},label={Text("Nuova $title")},modifier=Modifier.fillMaxWidth());Button(onClick={if(name.isNotBlank()){val n=name.trim();name="";scope.launch{repo.addCategory(n);refresh()}}},modifier=Modifier.fillMaxWidth()){Text("Aggiungi")}}}
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
