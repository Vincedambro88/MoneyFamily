from pathlib import Path

main = Path('app/src/main/java/com/moneyfamily/app/MainActivity.kt')
s = main.read_text()
s = s.replace('import androidx.activity.ComponentActivity', 'import androidx.activity.ComponentActivity\nimport androidx.activity.compose.rememberLauncherForActivityResult\nimport androidx.activity.result.contract.ActivityResultContracts')
needle='  item{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){MetricCard("Entrate",income,Modifier.weight(1f));MetricCard("Uscite",expense,Modifier.weight(1f));MetricCard("Saldo",balance,Modifier.weight(1f))}}'
s=s.replace(needle,needle+'\n  item{AnnualSummaryCard(data,month)}')
s=s.replace('2->Editor(null,types,cats,members,links,repo,{tab=0}){save(it);tab=1}','2->InsertScreen(types,cats,members,links,repo,{save(it);tab=1})')
marker='@Composable private fun Configuration('
block=r'''
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
 Column(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){Text("Inserisci",style=MaterialTheme.typography.headlineSmall);Button(onClick={showEditor=true},modifier=Modifier.fillMaxWidth()){Text("+ Nuova operazione")};OutlinedButton(onClick={launcher.launch(arrayOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet","application/vnd.ms-excel","text/csv"))},modifier=Modifier.fillMaxWidth()){Text("Importa da Excel")};Text("Colonne: Data | Importo | Tipologia | Categoria | Componente | Descrizione",style=MaterialTheme.typography.bodyMedium);if(status.isNotBlank())Text(status,color=if(status.startsWith("Errore"))NegativeColor else PositiveColor)}
 if(showEditor)Editor(null,types,cats,members,links,repo,{showEditor=false}){save(it);showEditor=false}
}

'''
s=s.replace(marker,block+marker)
main.write_text(s)

Path('app/src/main/java/com/moneyfamily/app/ExcelImporter.kt').write_text(r'''package com.moneyfamily.app

import android.content.Context
import android.net.Uri
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element

data class ImportedRow(val date:String,val amount:Double,val typeName:String,val category:String,val member:String,val description:String)
object ExcelImporter {
 fun import(context:Context,uri:Uri):List<ImportedRow>{val mime=context.contentResolver.getType(uri).orEmpty();return if(mime.contains("spreadsheet")||mime.contains("excel")||uri.toString().lowercase().endsWith(".xlsx"))readXlsx(context,uri)else readCsv(context,uri)}
 private fun readCsv(context:Context,uri:Uri):List<ImportedRow>{context.contentResolver.openInputStream(uri).use{ins->val lines=BufferedReader(InputStreamReader(ins!!,Charsets.UTF_8)).readLines();if(lines.isEmpty())return emptyList();val h=lines.first().split(';',',','\t').map{norm(it)};return lines.drop(1).mapNotNull{parseCells(it.split(';',',','\t'),h)}}}
 private fun readXlsx(context:Context,uri:Uri):List<ImportedRow>{val e=mutableMapOf<String,ByteArray>();context.contentResolver.openInputStream(uri).use{input->ZipInputStream(input!!).use{z->while(true){val x=z.nextEntry?:break;e[x.name]=z.readBytes()}}};val shared=e["xl/sharedStrings.xml"]?.let{shared(it)}?:emptyList();val bytes=e["xl/worksheets/sheet1.xml"]?:error("Foglio Excel non trovato");val rows=sheet(bytes,shared);if(rows.isEmpty())return emptyList();val h=rows.first().map{norm(it)};return rows.drop(1).mapNotNull{parseCells(it,h)}}
 private fun shared(b:ByteArray):List<String>{val d=DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(b.inputStream());val n=d.getElementsByTagName("si");return (0 until n.length).map{val e=n.item(it) as Element;val t=e.getElementsByTagName("t");(0 until t.length).joinToString(""){j->t.item(j).textContent}}}
 private fun sheet(b:ByteArray,shared:List<String>):List<List<String>>{val d=DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(b.inputStream());val rs=d.getElementsByTagName("row");return (0 until rs.length).map{ri->val r=rs.item(ri) as Element;val cs=r.getElementsByTagName("c");val v=MutableList(32){""};for(j in 0 until cs.length){val c=cs.item(j) as Element;val col=c.getAttribute("r").takeWhile{it.isLetter()};val idx=col.fold(0){a,ch->a*26+(ch-'A'+1)}-1;val x=c.getElementsByTagName("v");val raw=if(x.length>0)x.item(0).textContent else "";v[idx]=if(c.getAttribute("t")=="s")shared.getOrNull(raw.toIntOrNull()?:-1).orEmpty() else raw};while(v.lastOrNull().isNullOrBlank())v.removeAt(v.lastIndex);v}}
 private fun parseCells(cells:List<String>,h:List<String>):ImportedRow?{fun get(vararg n:String)=h.indexOfFirst{a->n.any{a==norm(it)}}.let{if(it>=0)cells.getOrNull(it).orEmpty()else""};val date=normalizeDate(get("data","date"));val amount=get("importo","amount","valore").replace(".","").replace(",",".").toDoubleOrNull()?:return null;if(date.isBlank())return null;return ImportedRow(date,amount,get("tipologia","tipo","type"),get("categoria","category"),get("componente","effettuata da","member","famiglia"),get("descrizione","description"))}
 private fun norm(s:String)=s.trim().lowercase(Locale.ITALIAN).replace("_"," ")
 private fun normalizeDate(v:String):String{val d=v.trim();d.toDoubleOrNull()?.let{n->if(n>20000){val c=Calendar.getInstance(TimeZone.getTimeZone("UTC"));c.timeInMillis=((n-25569)*86400000.0).toLong();return SimpleDateFormat("dd/MM/yyyy",Locale.ITALY).format(c.time)}};return d.replace("-","/")}
}
''')
