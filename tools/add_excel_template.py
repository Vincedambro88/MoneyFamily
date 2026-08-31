from pathlib import Path

p = Path('app/src/main/java/com/moneyfamily/app/MainActivity.kt')
s = p.read_text()

if 'ExcelTemplate.create()' in s:
    print('Excel template UI already present')
    raise SystemExit(0)

s = s.replace('import kotlinx.coroutines.launch', 'import kotlinx.coroutines.launch\nimport kotlinx.coroutines.Dispatchers\nimport kotlinx.coroutines.withContext')

start = s.index('@Composable private fun InsertScreen(')
end = s.index('@Composable private fun Configuration(', start)

new_block = r'''@Composable private fun InsertScreen(types:List<TypeEntity>,cats:List<CategoryEntity>,members:List<FamilyMemberEntity>,links:List<TypeCategoryEntity>,repo:RoomRepository,save:(UiMovement)->Unit){
 val context=LocalContext.current;val scope=rememberCoroutineScope();var status by remember{mutableStateOf("")};var showEditor by remember{mutableStateOf(false)}
 val importLauncher=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->if(uri!=null)scope.launch{runCatching{val rows=ExcelImporter.import(context,uri);rows.forEachIndexed{idx,r->val te=types.firstOrNull{it.name.equals(r.typeName,true)};val mapped=if(r.category.isBlank())links.firstOrNull{it.typeId==te?.id}?.let{l->cats.firstOrNull{c->c.id==l.categoryId}?.name}.orEmpty() else r.category;repo.insert(Movement(id=System.currentTimeMillis()+idx,type=if(r.amount<0)MovementType.EXPENSE else MovementType.INCOME,amount=r.amount,category=mapped.ifBlank{cats.firstOrNull()?.name.orEmpty()},description=r.description,date=r.date,member=r.member.ifBlank{members.firstOrNull()?.name.orEmpty()},paymentMethod="",typeName=r.typeName))};status="Importate ${rows.size} operazioni"}.onFailure{status="Errore importazione: ${it.message?:"file non valido"}"}}}
 val templateLauncher=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")){uri->if(uri!=null)scope.launch(Dispatchers.IO){runCatching{context.contentResolver.openOutputStream(uri)?.use{it.write(ExcelTemplate.create())}?:error("Impossibile creare il file");withContext(Dispatchers.Main){status="Modello Excel salvato"}}.onFailure{e->withContext(Dispatchers.Main){status="Errore salvataggio: ${e.message?:"operazione non riuscita"}"}}}}
 Column(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
  Text("Inserisci",style=MaterialTheme.typography.headlineSmall)
  Button(onClick={showEditor=true},modifier=Modifier.fillMaxWidth()){Text("+ Nuova operazione")}
  OutlinedButton(onClick={templateLauncher.launch("MoneyFamily_Modello_Importazione.xlsx")},modifier=Modifier.fillMaxWidth()){Text("Scarica modello Excel")}
  OutlinedButton(onClick={importLauncher.launch(arrayOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet","application/vnd.ms-excel","text/csv"))},modifier=Modifier.fillMaxWidth()){Text("Importa da Excel")}
  Text("Formato: Data | Importo | Tipologia | Categoria | Componente | Descrizione",style=MaterialTheme.typography.bodyMedium)
  Text("Usa il modello scaricabile per evitare errori di formato.",style=MaterialTheme.typography.bodyMedium)
  if(status.isNotBlank())Text(status,color=if(status.startsWith("Errore"))NegativeColor else PositiveColor)
 }
 if(showEditor)Editor(null,types,cats,members,links,repo,{showEditor=false}){save(it);showEditor=false}
}

'''
s = s[:start] + new_block + s[end:]
p.write_text(s)
print('Added in-app XLSX template download to InsertScreen')
