from pathlib import Path
import re

main = Path('app/src/main/java/com/moneyfamily/app/MainActivity.kt')
s = main.read_text()

old_call = '2->InsertScreen(types,cats,members,links,repo,{save(it);tab=1});'
new_call = '2->InsertScreen(types,cats,members,links,repo,{tab=0},{save(it);tab=1});'
if old_call not in s:
    raise SystemExit('InsertScreen call not found')
s = s.replace(old_call, new_call, 1)

start = s.find('@Composable private fun InsertScreen(')
end = s.find('@Composable private fun Configuration(', start)
if start < 0 or end < 0:
    raise SystemExit('InsertScreen boundaries not found')

new_screen = r'''@Composable private fun InsertScreen(types:List<TypeEntity>,cats:List<CategoryEntity>,members:List<FamilyMemberEntity>,links:List<TypeCategoryEntity>,repo:RoomRepository,onBack:()->Unit,save:(UiMovement)->Unit){
 val context=LocalContext.current;val scope=rememberCoroutineScope();var status by remember{mutableStateOf("")};var showEditor by remember{mutableStateOf(false)}
 val launcher=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->if(uri!=null)scope.launch{runCatching{val rows=ExcelImporter.import(context,uri);rows.forEachIndexed{idx,r->val te=types.firstOrNull{it.name.equals(r.typeName,true)};val mapped=if(r.category.isBlank())links.firstOrNull{it.typeId==te?.id}?.let{l->cats.firstOrNull{c->c.id==l.categoryId}?.name}.orEmpty() else r.category;repo.insert(Movement(id=System.currentTimeMillis()+idx,type=if(r.amount<0)MovementType.EXPENSE else MovementType.INCOME,amount=r.amount,category=mapped.ifBlank{cats.firstOrNull()?.name.orEmpty()},description=r.description,date=r.date,member=r.member.ifBlank{members.firstOrNull()?.name.orEmpty()},paymentMethod="",typeName=r.typeName))};status="Importate ${rows.size} operazioni"}.onFailure{status="Errore importazione: ${it.message?:"file non valido"}"}}}
 Column(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
  Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){TextButton(onClick=onBack){Text("← Indietro")};Spacer(Modifier.weight(1f));Text("Inserisci",style=MaterialTheme.typography.headlineSmall)}
  Button(onClick={showEditor=true},modifier=Modifier.fillMaxWidth()){Text("+ Nuova operazione")}
  OutlinedButton(onClick={launcher.launch(arrayOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet","application/vnd.ms-excel","text/csv"))},modifier=Modifier.fillMaxWidth()){Text("Importa da Excel")}
  Text("Excel obbligatorio: Data | Importo | Tipologia | Categoria | Componente | Descrizione. Tutti i campi devono essere compilati.",style=MaterialTheme.typography.bodyMedium)
  if(status.isNotBlank())Text(status,color=if(status.startsWith("Errore"))NegativeColor else PositiveColor)
 }
 if(showEditor)Editor(null,types,cats,members,links,repo,{showEditor=false}){save(it);showEditor=false}
}

'''
s = s[:start] + new_screen + s[end:]
main.write_text(s)
