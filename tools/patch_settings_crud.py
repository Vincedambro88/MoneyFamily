from pathlib import Path

main = Path('app/src/main/java/com/moneyfamily/app/MainActivity.kt')
s = main.read_text()
if 'private fun MasterDataCrud(' in s:
    raise SystemExit('Settings CRUD patch already present')
marker='@Composable private fun Configuration('
start=s.find(marker)
if start < 0: raise SystemExit('Configuration composable not found')
brace=s.find('{',start)
if brace < 0: raise SystemExit('Configuration body not found')
s=s[:brace+1]+'\n MasterDataCrud(types,cats,members,repo,refresh)\n'+s[brace+1:]
crud=r'''

@Composable private fun MasterDataCrud(types:List<TypeEntity>,cats:List<CategoryEntity>,members:List<FamilyMemberEntity>,repo:RoomRepository,refresh:()->Unit){
 var dialogKind by remember{mutableStateOf("")};var editId by remember{mutableStateOf(0L)};var editName by remember{mutableStateOf("")}
 var deleteKind by remember{mutableStateOf("")};var deleteId by remember{mutableStateOf(0L)};var deleteName by remember{mutableStateOf("")}
 val scope=rememberCoroutineScope()
 fun openEdit(k:String,id:Long,n:String){dialogKind=k;editId=id;editName=n}
 fun askDelete(k:String,id:Long,n:String){deleteKind=k;deleteId=id;deleteName=n}
 Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(12.dp)){
  Text("Gestione anagrafiche",style=MaterialTheme.typography.headlineSmall)
  Text("Modifica o disattiva tipologie, categorie e membri. Le operazioni storiche non vengono cancellate.",style=MaterialTheme.typography.bodyMedium)
  MasterDataSection("Tipologie",types.map{Triple(it.id,it.name,it.active)},{openEdit("TYPE",it.first,it.second)},{askDelete("TYPE",it.first,it.second)})
  MasterDataSection("Categorie",cats.map{Triple(it.id,it.name,it.active)},{openEdit("CATEGORY",it.first,it.second)},{askDelete("CATEGORY",it.first,it.second)})
  MasterDataSection("Membri della famiglia",members.map{Triple(it.id,it.name,it.active)},{openEdit("MEMBER",it.first,it.second)},{askDelete("MEMBER",it.first,it.second)})
 }
 if(dialogKind.isNotBlank()) AlertDialog(onDismissRequest={dialogKind=""},title={Text("Modifica valore")},text={OutlinedTextField(value=editName,onValueChange={editName=it},label={Text("Nome")},singleLine=true)},confirmButton={TextButton(onClick={val n=editName.trim();if(n.isNotBlank())scope.launch{when(dialogKind){"TYPE"->repo.updateType(TypeEntity(editId,n,true));"CATEGORY"->repo.updateCategory(CategoryEntity(editId,n,true));"MEMBER"->repo.updateMember(FamilyMemberEntity(editId,n,true))};dialogKind="";refresh()}}){Text("Salva")}},dismissButton={TextButton(onClick={dialogKind=""}){Text("Annulla")}})
 if(deleteKind.isNotBlank()) AlertDialog(onDismissRequest={deleteKind=""},title={Text("Disattiva valore")},text={Text("Vuoi disattivare \"$deleteName\"? Le operazioni storiche resteranno disponibili, ma il valore non sarà più proposto nei nuovi inserimenti.")},confirmButton={TextButton(onClick={scope.launch{when(deleteKind){"TYPE"->repo.setTypeActive(deleteId,false);"CATEGORY"->repo.setCategoryActive(deleteId,false);"MEMBER"->repo.setMemberActive(deleteId,false)};deleteKind="";refresh()}}){Text("Disattiva")}},dismissButton={TextButton(onClick={deleteKind=""}){Text("Annulla")}})
}

@Composable private fun MasterDataSection(title:String,items:List<Triple<Long,String,Boolean>>,onEdit:(Triple<Long,String,Boolean>)->Unit,onDelete:(Triple<Long,String,Boolean>)->Unit){
 Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(20.dp)){Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){
  Text(title,style=MaterialTheme.typography.titleLarge)
  if(items.isEmpty())Text("Nessun valore")
  items.forEach{item->Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(item.second);if(!item.third)Text("Disattivato",style=MaterialTheme.typography.labelSmall)};TextButton(onClick={onEdit(item)}){Text("✏")};TextButton(onClick={onDelete(item)}){Text(if(item.third)"🗑" else "↻")}}}
 }}
}
'''
s=s.rstrip()+crud+'\n'
main.write_text(s)
