from pathlib import Path
main=Path('app/src/main/java/com/moneyfamily/app/MainActivity.kt')
s=main.read_text()
s=s.replace('import androidx.activity.ComponentActivity','import androidx.activity.ComponentActivity\nimport androidx.activity.compose.rememberLauncherForActivityResult\nimport androidx.activity.result.contract.ActivityResultContracts')
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
 val launcher=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->if(uri!=null)scope.launch{runCatching{val rows=ExcelImporter.import(context,uri);rows.forEachIndexed{idx,r->repo.insert(Movement(id=System.currentTimeMillis()+idx,type=if(r.amount<0)MovementType.EXPENSE else MovementType.INCOME,amount=r.amount,category=r.category,description=r.description,date=r.date,member=r.member,paymentMethod="",typeName=r.typeName))};status="Importate ${rows.size} operazioni"}.onFailure{status="Errore importazione: ${it.message?:"file non valido"}"}}}
 Column(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){Text("Inserisci",style=MaterialTheme.typography.headlineSmall);Button(onClick={showEditor=true},modifier=Modifier.fillMaxWidth()){Text("+ Nuova operazione")};OutlinedButton(onClick={launcher.launch(arrayOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet","application/vnd.ms-excel","text/csv"))},modifier=Modifier.fillMaxWidth()){Text("Importa da Excel")};Text("Excel obbligatorio: Data | Importo | Tipologia | Categoria | Componente | Descrizione. Tutti i campi devono essere compilati.",style=MaterialTheme.typography.bodyMedium);if(status.isNotBlank())Text(status,color=if(status.startsWith("Errore"))NegativeColor else PositiveColor)}
 if(showEditor)Editor(null,types,cats,members,links,repo,{showEditor=false}){save(it);showEditor=false}
}

'''
if marker not in s: raise SystemExit('configuration marker not found')
if 'AnnualSummaryCard(' not in s: s=s.replace(marker,block+marker)
main.write_text(s)
