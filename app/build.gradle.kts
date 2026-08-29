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

            // Keep the existing source intact, but remove duplicate imports.
            val duplicate = "import androidx.activity.compose.rememberLauncherForActivityResult\nimport androidx.activity.result.contract.ActivityResultContracts\nimport androidx.activity.compose.rememberLauncherForActivityResult\nimport androidx.activity.result.contract.ActivityResultContracts"
            val single = "import androidx.activity.compose.rememberLauncherForActivityResult\nimport androidx.activity.result.contract.ActivityResultContracts"
            text = text.replace(duplicate, single)

            // The stable source contains an old tabbed Configuration block after
            // the new master-data CRUD screen. Remove that duplicate UI so the
            // three requested sections are presented in one vertically scrollable page.
            val configStart = text.indexOf("@Composable private fun Configuration(")
            val typesStart = text.indexOf("@Composable private fun EntityConfigTypes", configStart)
            if (configStart >= 0 && typesStart > configStart) {
                val replacement = """
@Composable private fun Configuration(types:List<TypeEntity>,cats:List<CategoryEntity>,members:List<FamilyMemberEntity>,links:List<TypeCategoryEntity>,repo:RoomRepository,refresh:()->Unit){
    MasterDataCrud(types,cats,members,repo,refresh)
}

""".trimIndent()
                text = text.substring(0, configStart) + replacement + text.substring(typesStart)
            }

            // Make the master-data page itself scrollable. This is important when
            // the type list is long: Categories and Family Members must remain
            // reachable below it instead of being pushed outside the viewport.
            val crudStart = text.indexOf("@Composable private fun MasterDataCrud(")
            val sectionStart = text.indexOf("@Composable private fun MasterDataSection", crudStart)
            if (crudStart >= 0 && sectionStart > crudStart) {
                val crudBlock = text.substring(crudStart, sectionStart)
                val scrollableCrud = crudBlock.replace(
                    "Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(12.dp)){",
                    "Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)){"
                )
                text = text.substring(0, crudStart) + scrollableCrud + text.substring(sectionStart)
            }

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
