plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp") version "2.0.21-1.0.28"
}

android {
    namespace = "com.moneyfamily.app"
    compileSdk = 36
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    defaultConfig { applicationId = "com.moneyfamily.app"; minSdk = 26; targetSdk = 36; versionCode = 5; versionName = "1.0.0" }
}

kotlin { jvmToolchain(17) }

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    doFirst {
        val source = file("src/main/java/com/moneyfamily/app/MainActivity.kt")
        if (source.exists()) {
            var text = source.readText()

            // Remove duplicate imports from the legacy source.
            val duplicate = "import androidx.activity.compose.rememberLauncherForActivityResult\nimport androidx.activity.result.contract.ActivityResultContracts\nimport androidx.activity.compose.rememberLauncherForActivityResult\nimport androidx.activity.result.contract.ActivityResultContracts"
            val single = "import androidx.activity.compose.rememberLauncherForActivityResult\nimport androidx.activity.result.contract.ActivityResultContracts"
            text = text.replace(duplicate, single)

            // Restore the original tabbed Settings screen so the Add fields remain
            // available. Add a compact X delete action beside every master-data value.
            val configStart = text.indexOf("@Composable private fun Configuration(")
            val typesStart = text.indexOf("@Composable private fun EntityConfigTypes", configStart)
            if (configStart >= 0 && typesStart > configStart) {
                val replacement = """
@Composable private fun Configuration(types:List<TypeEntity>,cats:List<CategoryEntity>,members:List<FamilyMemberEntity>,links:List<TypeCategoryEntity>,repo:RoomRepository,refresh:()->Unit){
    var section by remember{mutableStateOf(0)}
    Column(Modifier.fillMaxSize().padding(16.dp)){
        Text("Configurazione",style=MaterialTheme.typography.headlineSmall)
        Row(Modifier.fillMaxWidth()){
            listOf("Tipologie","Categorie","Famiglia","Associazioni").forEachIndexed{i,t->TextButton(onClick={section=i}){Text(t)}}
        }
        Box(Modifier.fillMaxWidth().weight(1f)){
            when(section){
                0->EntityConfigTypes("Tipologie",types,repo,refresh)
                1->EntityConfigCats("Categorie",cats,repo,refresh)
                2->MemberConfig(members,repo,refresh)
                3->LinkConfig(types,cats,links,repo,refresh)
            }
        }
    }
}

""".trimIndent()
                text = text.substring(0, configStart) + replacement + text.substring(typesStart)
            }

            // Tipologie: keep the existing insertion field and add an X beside each value.
            text = text.replace(
                "items.forEach{Text(it.name,modifier=Modifier.fillMaxWidth().padding(vertical=2.dp))};OutlinedTextField",
                "items.forEach{item->Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text(item.name,Modifier.weight(1f));TextButton(onClick={scope.launch{repo.setTypeActive(item.id,false);refresh()}}){Text(\"✕\")}}};OutlinedTextField"
            )

            // Categorie: keep the existing insertion field and add an X beside each value.
            text = text.replace(
                "items.forEach{Text(it.name,modifier=Modifier.fillMaxWidth().padding(vertical=2.dp))};OutlinedTextField",
                "items.forEach{item->Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text(item.name,Modifier.weight(1f));TextButton(onClick={scope.launch{repo.setCategoryActive(item.id,false);refresh()}}){Text(\"✕\")}}};OutlinedTextField"
            )

            // Family members: keep insertion enabled and add an X beside each member.
            text = text.replace(
                "items.forEach{m->Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(m.name);Text(if(m.active)\"Attivo\" else \"Disattivo\")}};OutlinedTextField",
                "items.forEach{m->Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text(m.name,Modifier.weight(1f));TextButton(onClick={scope.launch{repo.setMemberActive(m.id,false);refresh()}}){Text(\"✕\")}}};OutlinedTextField"
            )

            source.writeText(text)
        }
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.01.00"))
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
}
