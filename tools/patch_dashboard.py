# MoneyFamily dashboard patcher - v3
from pathlib import Path

p = Path('app/src/main/java/com/moneyfamily/app/MainActivity.kt')
s = p.read_text()

# Keep the existing dashboard/type fixes from the previous patch, then make configuration lists scrollable.
s = s.replace(
    'data class UiMovement(val id:Long,val type:MovementType,val amount:Double,val category:String,val description:String,val date:String,val member:String)',
    'data class UiMovement(val id:Long,val type:MovementType,val amount:Double,val category:String,val description:String,val date:String,val member:String,val typeName:String = "")'
)

# The settings screen has a fixed header and a variable-height content area. Give the content
# the remaining height so LazyColumn can scroll instead of being clipped by the bottom navigation.
old_config = '''@Composable private fun Configuration(types:List<TypeEntity>,cats:List<CategoryEntity>,members:List<FamilyMemberEntity>,links:List<TypeCategoryEntity>,repo:RoomRepository,refresh:()->Unit){var section by remember{mutableStateOf(0)};Column(Modifier.fillMaxSize().padding(16.dp)){Text("Configurazione",style=MaterialTheme.typography.headlineSmall);Row{listOf("Tipologie","Categorie","Famiglia","Associazioni").forEachIndexed{i,t->TextButton(onClick={section=i}){Text(t)}}};when(section){0->EntityConfigTypes("Tipologie",types,repo,refresh);1->EntityConfigCats("Categorie",cats,repo,refresh);2->MemberConfig(members,repo,refresh);3->LinkConfig(types,cats,links,repo,refresh)}}}'''
new_config = '''@Composable private fun Configuration(types:List<TypeEntity>,cats:List<CategoryEntity>,members:List<FamilyMemberEntity>,links:List<TypeCategoryEntity>,repo:RoomRepository,refresh:()->Unit){var section by remember{mutableStateOf(0)};Column(Modifier.fillMaxSize().padding(16.dp)){Text("Configurazione",style=MaterialTheme.typography.headlineSmall);Row(Modifier.fillMaxWidth()){listOf("Tipologie","Categorie","Famiglia","Associazioni").forEachIndexed{i,t->TextButton(onClick={section=i}){Text(t)}}};Box(Modifier.fillMaxWidth().weight(1f)){when(section){0->EntityConfigTypes("Tipologie",types,repo,refresh);1->EntityConfigCats("Categorie",cats,repo,refresh);2->MemberConfig(members,repo,refresh);3->LinkConfig(types,cats,links,repo,refresh)}}}}'''
s = s.replace(old_config, new_config)

old_types = '''@Composable private fun EntityConfigTypes(title:String,items:List<TypeEntity>,repo:RoomRepository,refresh:()->Unit){var name by remember{mutableStateOf("")};val scope=rememberCoroutineScope();Column(verticalArrangement=Arrangement.spacedBy(8.dp)){items.forEach{Text(it.name)};OutlinedTextField(value=name,onValueChange={name=it},label={Text("Nuova $title")},modifier=Modifier.fillMaxWidth());Button(onClick={if(name.isNotBlank()){val n=name.trim();name="";scope.launch{repo.addType(n);refresh()}}},modifier=Modifier.fillMaxWidth()){Text("Aggiungi")}}}'''
new_types = '''@Composable private fun EntityConfigTypes(title:String,items:List<TypeEntity>,repo:RoomRepository,refresh:()->Unit){var name by remember{mutableStateOf("")};val scope=rememberCoroutineScope();Column(Modifier.fillMaxSize()){LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp)){items(items){Text(it.name,modifier=Modifier.fillMaxWidth().padding(vertical=2.dp))}};OutlinedTextField(value=name,onValueChange={name=it},label={Text("Nuova $title")},modifier=Modifier.fillMaxWidth());Button(onClick={if(name.isNotBlank()){val n=name.trim();name="";scope.launch{repo.addType(n);refresh()}}},modifier=Modifier.fillMaxWidth()){Text("Aggiungi")}}}'''
s = s.replace(old_types, new_types)

old_cats = '''@Composable private fun EntityConfigCats(title:String,items:List<CategoryEntity>,repo:RoomRepository,refresh:()->Unit){var name by remember{mutableStateOf("")};val scope=rememberCoroutineScope();Column(verticalArrangement=Arrangement.spacedBy(8.dp)){items.forEach{Text(it.name)};OutlinedTextField(value=name,onValueChange={name=it},label={Text("Nuova $title")},modifier=Modifier.fillMaxWidth());Button(onClick={if(name.isNotBlank()){val n=name.trim();name="";scope.launch{repo.addCategory(n);refresh()}}},modifier=Modifier.fillMaxWidth()){Text("Aggiungi")}}}'''
new_cats = '''@Composable private fun EntityConfigCats(title:String,items:List<CategoryEntity>,repo:RoomRepository,refresh:()->Unit){var name by remember{mutableStateOf("")};val scope=rememberCoroutineScope();Column(Modifier.fillMaxSize()){LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp)){items(items){Text(it.name,modifier=Modifier.fillMaxWidth().padding(vertical=2.dp))}};OutlinedTextField(value=name,onValueChange={name=it},label={Text("Nuova $title")},modifier=Modifier.fillMaxWidth());Button(onClick={if(name.isNotBlank()){val n=name.trim();name="";scope.launch{repo.addCategory(n);refresh()}}},modifier=Modifier.fillMaxWidth()){Text("Aggiungi")}}}'''
s = s.replace(old_cats, new_cats)

# Also convert any remaining direct list rendering in the family and association tabs when present.
s = s.replace('Column(verticalArrangement=Arrangement.spacedBy(8.dp)){items.forEach{Text(it.name)};', 'Column(Modifier.fillMaxSize()){LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp)){items(items){Text(it.name)}};')

p.write_text(s)
print('patched configuration scrolling')
