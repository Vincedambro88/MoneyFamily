from pathlib import Path

main = Path('app/src/main/java/com/moneyfamily/app/MainActivity.kt')
s = main.read_text()
start = s.find('@Composable private fun Configuration(')
end = s.find('private fun ChoiceDialog(', start)
if start < 0 or end < 0:
    raise SystemExit('Configuration markers not found')

replacement = r'''@Composable private fun Configuration(types:List<TypeEntity>,cats:List<CategoryEntity>,members:List<FamilyMemberEntity>,links:List<TypeCategoryEntity>,repo:RoomRepository,refresh:()->Unit){
 var section by remember{mutableStateOf(0)}
 Column(Modifier.fillMaxSize().padding(16.dp)){
  Text("Configurazione",style=MaterialTheme.typography.headlineSmall)
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(2.dp)){
   listOf("Tipologie","Categorie","Famiglia","Associazioni").forEachIndexed{i,t->TextButton(onClick={section=i}){Text(t)}}
  }
  Box(Modifier.fillMaxWidth().weight(1f)){
   when(section){
    0->ConfigTypesV2(types,repo,refresh)
    1->ConfigCatsV2(cats,repo,refresh)
    2->ConfigMembersV2(members,repo,refresh)
    3->ConfigLinksV2(types,cats,links,repo,refresh)
   }
  }
 }
}

@Composable private fun ConfigTypesV2(items:List<TypeEntity>,repo:RoomRepository,refresh:()->Unit){
 var name by remember{mutableStateOf("")};var edit by remember{mutableStateOf<TypeEntity?>(null)};var del by remember{mutableStateOf<TypeEntity?>(null)};val scope=rememberCoroutineScope()
 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(6.dp)){
  Text("Tipologie",style=MaterialTheme.typography.titleLarge)
  items.forEach{item->ConfigRowV2(item.name,item.active,{edit=item},{del=item})}
  OutlinedTextField(value=name,onValueChange={name=it},label={Text("Nuova tipologia")},modifier=Modifier.fillMaxWidth(),singleLine=true)
  Button(onClick={val n=name.trim();if(n.isNotBlank())scope.launch{repo.addType(n);name="";refresh()}},modifier=Modifier.fillMaxWidth()){Text("+ Aggiungi tipologia")}
 }
 edit?.let{e->EditNameDialogV2("Modifica tipologia",e.name){n->scope.launch{repo.updateType(TypeEntity(e.id,n,true));edit=null;refresh()}}}
 del?.let{e->ConfirmDeactivateV2(e.name){scope.launch{repo.setTypeActive(e.id,false);del=null;refresh()}}}
}

@Composable private fun ConfigCatsV2(items:List<CategoryEntity>,repo:RoomRepository,refresh:()->Unit){
 var name by remember{mutableStateOf("")};var edit by remember{mutableStateOf<CategoryEntity?>(null)};var del by remember{mutableStateOf<CategoryEntity?>(null)};val scope=rememberCoroutineScope()
 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(6.dp)){
  Text("Categorie",style=MaterialTheme.typography.titleLarge)
  items.forEach{item->ConfigRowV2(item.name,item.active,{edit=item},{del=item})}
  OutlinedTextField(value=name,onValueChange={name=it},label={Text("Nuova categoria")},modifier=Modifier.fillMaxWidth(),singleLine=true)
  Button(onClick={val n=name.trim();if(n.isNotBlank())scope.launch{repo.addCategory(n);name="";refresh()}},modifier=Modifier.fillMaxWidth()){Text("+ Aggiungi categoria")}
 }
 edit?.let{e->EditNameDialogV2("Modifica categoria",e.name){n->scope.launch{repo.updateCategory(CategoryEntity(e.id,n,true));edit=null;refresh()}}}
 del?.let{e->ConfirmDeactivateV2(e.name){scope.launch{repo.setCategoryActive(e.id,false);del=null;refresh()}}}
}

@Composable private fun ConfigMembersV2(items:List<FamilyMemberEntity>,repo:RoomRepository,refresh:()->Unit){
 var name by remember{mutableStateOf("")};var edit by remember{mutableStateOf<FamilyMemberEntity?>(null)};var del by remember{mutableStateOf<FamilyMemberEntity?>(null)};val scope=rememberCoroutineScope()
 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(6.dp)){
  Text("Membri della famiglia",style=MaterialTheme.typography.titleLarge)
  items.forEach{item->ConfigRowV2(item.name,item.active,{edit=item},{del=item})}
  OutlinedTextField(value=name,onValueChange={name=it},label={Text("Nuovo membro")},modifier=Modifier.fillMaxWidth(),singleLine=true)
  Button(onClick={val n=name.trim();if(n.isNotBlank())scope.launch{repo.addMember(n);name="";refresh()}},modifier=Modifier.fillMaxWidth()){Text("+ Aggiungi membro")}
 }
 edit?.let{e->EditNameDialogV2("Modifica membro",e.name){n->scope.launch{repo.updateMember(FamilyMemberEntity(e.id,n,true));edit=null;refresh()}}}
 del?.let{e->ConfirmDeactivateV2(e.name){scope.launch{repo.setMemberActive(e.id,false);del=null;refresh()}}}
}

@Composable private fun ConfigRowV2(name:String,active:Boolean,onEdit:()->Unit,onDelete:()->Unit){
 Row(Modifier.fillMaxWidth().padding(vertical=2.dp),verticalAlignment=Alignment.CenterVertically){
  Column(Modifier.weight(1f)){Text(name);if(!active)Text("Disattivato",style=MaterialTheme.typography.labelSmall)}
  TextButton(onClick=onEdit){Text("✏")}
  TextButton(onClick=onDelete){Text(if(active)"✕" else "↻")}
 }
}

@Composable private fun ConfigLinksV2(types:List<TypeEntity>,cats:List<CategoryEntity>,links:List<TypeCategoryEntity>,repo:RoomRepository,refresh:()->Unit){
 val c=LocalContext.current;val scope=rememberCoroutineScope()
 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(6.dp)){
  Text("Associazioni tipologia → categoria",style=MaterialTheme.typography.titleLarge)
  Text("Tocca la categoria per modificarla.",style=MaterialTheme.typography.bodyMedium)
  types.filter{it.active}.forEach{t->
   val currentId=links.find{it.typeId==t.id}?.categoryId
   val current=cats.find{it.id==currentId}
   Card(Modifier.fillMaxWidth()){
    Row(Modifier.fillMaxWidth().padding(horizontal=10.dp,vertical=4.dp),verticalAlignment=Alignment.CenterVertically){
     Text(t.name,Modifier.weight(1f))
     TextButton(onClick={ChoiceDialog(c,"Categoria per ${t.name}",cats.filter{it.active}.map{it.name}){n->cats.find{it.name==n}?.let{cat->scope.launch{repo.setTypeCategory(t.id,cat.id);refresh()}}}}){Text(current?.name?:"Non associata")}
    }
   }
  }
 }
}

@Composable private fun EditNameDialogV2(title:String,current:String,onSave:(String)->Unit){
 var value by remember(current){mutableStateOf(current)}
 AlertDialog(onDismissRequest={},title={Text(title)},text={OutlinedTextField(value=value,onValueChange={value=it},singleLine=true,label={Text("Nome")})},confirmButton={TextButton(onClick={val n=value.trim();if(n.isNotBlank())onSave(n))){Text("Salva")}},dismissButton={TextButton(onClick={}){Text("Annulla")}})
}

@Composable private fun ConfirmDeactivateV2(name:String,onConfirm:()->Unit){
 AlertDialog(onDismissRequest={},title={Text("Elimina valore")},text={Text("Vuoi eliminare/disattivare \"$name\"? Le operazioni storiche non verranno cancellate.")},confirmButton={TextButton(onClick=onConfirm){Text("Elimina")}},dismissButton={TextButton(onClick={}){Text("Annulla")}})
}

'''
main.write_text(s[:start] + replacement + s[end:])
