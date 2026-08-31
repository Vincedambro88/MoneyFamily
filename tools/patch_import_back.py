from pathlib import Path

main = Path('app/src/main/java/com/moneyfamily/app/MainActivity.kt')
s = main.read_text()

old_call = '2->InsertScreen(types,cats,members,links,repo,{save(it);tab=1});'
new_call = '2->InsertScreen(types,cats,members,links,repo,{tab=0},{save(it);tab=1},{refresh()});'
if old_call in s:
    s = s.replace(old_call, new_call, 1)
elif '2->InsertScreen(types,cats,members,links,repo,{tab=0},{save(it);tab=1},{refresh()});' not in s:
    raise SystemExit('InsertScreen call not found')

start = s.find('@Composable private fun InsertScreen(')
end = s.find('@Composable private fun Configuration(', start)
if start < 0 or end < 0:
    raise SystemExit('InsertScreen boundaries not found')

new_screen = r'''@Composable private fun InsertScreen(types:List<TypeEntity>,cats:List<CategoryEntity>,members:List<FamilyMemberEntity>,links:List<TypeCategoryEntity>,repo:RoomRepository,onBack:()->Unit,save:(UiMovement)->Unit,onImported:()->Unit){
 val context=LocalContext.current;val scope=rememberCoroutineScope();var status by remember{mutableStateOf("")};var showEditor by remember{mutableStateOf(false)}
 val launcher=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->if(uri!=null)scope.launch{runCatching{
  val rows=ExcelImporter.import(context,uri)
  rows.forEachIndexed{idx,r->
   val te=types.firstOrNull{it.name.equals(r.typeName,true)}
   val mapped=if(r.category.isBlank())links.firstOrNull{it.typeId==te?.id}?.let{l->cats.firstOrNull{c->c.id==l.categoryId}?.name}.orEmpty() else r.category
   repo.insert(Movement(id=System.currentTimeMillis()+idx,type=if(r.amount<0)MovementType.EXPENSE else MovementType.INCOME,amount=r.amount,category=mapped.ifBlank{cats.firstOrNull()?.name.orEmpty()},description=r.description,date=r.date,member=r.member.ifBlank{members.firstOrNull()?.name.orEmpty()},paymentMethod="",typeName=r.typeName))
  }
  onImported()
  status="Importate ${rows.size} operazioni"
 }.onFailure{status="Errore importazione: ${it.message?:"file non valido"}"}}}
 val templateLauncher=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")){uri->if(uri!=null)scope.launch{runCatching{ExcelTemplate.write(context,uri);status="Modello Excel salvato"}.onFailure{status="Errore salvataggio: ${it.message?:"operazione non riuscita"}"}}}
 Column(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
  Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){TextButton(onClick=onBack){Text("← Indietro")};Spacer(Modifier.weight(1f));Text("Inserisci",style=MaterialTheme.typography.headlineSmall)}
  Button(onClick={showEditor=true},modifier=Modifier.fillMaxWidth()){Text("+ Nuova operazione")}
  OutlinedButton(onClick={templateLauncher.launch("MoneyFamily_Modello_Importazione.xlsx")},modifier=Modifier.fillMaxWidth()){Text("Scarica modello Excel")}
  OutlinedButton(onClick={launcher.launch(arrayOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet","application/vnd.ms-excel","text/csv"))},modifier=Modifier.fillMaxWidth()){Text("Importa da Excel")}
  Text("Excel obbligatorio: Data | Descrizione | Importo | Tipologia | Categoria | Membro famiglia. Non inserire Entrata/Uscita: la natura dell'operazione deriva dal segno dell'Importo.",style=MaterialTheme.typography.bodyMedium)
  if(status.isNotBlank())Text(status,color=if(status.startsWith("Errore"))NegativeColor else PositiveColor)
 }
 if(showEditor)Editor(null,types,cats,members,links,repo,{showEditor=false}){save(it);showEditor=false}
}

'''
s = s[:start] + new_screen + s[end:]
main.write_text(s)
