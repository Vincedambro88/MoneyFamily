from pathlib import Path
p = Path('app/src/main/java/com/moneyfamily/app/MainActivity.kt')
s = p.read_text()
if 'import androidx.compose.foundation.rememberScrollState' not in s:
    s = s.replace('import androidx.compose.foundation.Canvas', 'import androidx.compose.foundation.Canvas\nimport androidx.compose.foundation.rememberScrollState\nimport androidx.compose.foundation.verticalScroll')
start = s.index('@Composable private fun Configuration(')
end = s.index('@Composable private fun EntityConfigTypes(', start)
config = '''@Composable private fun Configuration(types:List<TypeEntity>,cats:List<CategoryEntity>,members:List<FamilyMemberEntity>,links:List<TypeCategoryEntity>,repo:RoomRepository,refresh:()->Unit){var section by remember{mutableStateOf(0)};Column(Modifier.fillMaxSize().padding(16.dp)){Text("Configurazione",style=MaterialTheme.typography.headlineSmall);Row(Modifier.fillMaxWidth()){listOf("Tipologie","Categorie","Famiglia","Associazioni").forEachIndexed{i,t->TextButton(onClick={section=i}){Text(t)}}};Box(Modifier.fillMaxWidth().weight(1f)){when(section){0->EntityConfigTypes("Tipologie",types,repo,refresh);1->EntityConfigCats("Categorie",cats,repo,refresh);2->MemberConfig(members,repo,refresh);3->LinkConfig(types,cats,links,repo,refresh)}}}}\n'''
s = s[:start] + config + s[end:]
start = s.index('@Composable private fun EntityConfigTypes(')
end = s.index('@Composable private fun EntityConfigCats(', start)
types_fn = '''@Composable private fun EntityConfigTypes(title:String,items:List<TypeEntity>,repo:RoomRepository,refresh:()->Unit){var name by remember{mutableStateOf("")};val scope=rememberCoroutineScope();Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)){items.forEach{Text(it.name,modifier=Modifier.fillMaxWidth().padding(vertical=2.dp))};OutlinedTextField(value=name,onValueChange={name=it},label={Text("Nuova $title")},modifier=Modifier.fillMaxWidth());Button(onClick={if(name.isNotBlank()){val n=name.trim();name="";scope.launch{repo.addType(n);refresh()}}},modifier=Modifier.fillMaxWidth()){Text("Aggiungi")}}}\n'''
s = s[:start] + types_fn + s[end:]
start = s.index('@Composable private fun EntityConfigCats(')
end = s.index('@Composable private fun MemberConfig(', start)
cats_fn = '''@Composable private fun EntityConfigCats(title:String,items:List<CategoryEntity>,repo:RoomRepository,refresh:()->Unit){var name by remember{mutableStateOf("")};val scope=rememberCoroutineScope();Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)){items.forEach{Text(it.name,modifier=Modifier.fillMaxWidth().padding(vertical=2.dp))};OutlinedTextField(value=name,onValueChange={name=it},label={Text("Nuova $title")},modifier=Modifier.fillMaxWidth());Button(onClick={if(name.isNotBlank()){val n=name.trim();name="";scope.launch{repo.addCategory(n);refresh()}}},modifier=Modifier.fillMaxWidth()){Text("Aggiungi")}}}\n'''
s = s[:start] + cats_fn + s[end:]
p.write_text(s)
