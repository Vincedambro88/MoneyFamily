package com.moneyfamily.app

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.moneyfamily.app.data.*
import kotlinx.coroutines.launch

@Composable
fun PremiumSection(
    billing: PremiumBilling,
    supabaseRepo: SupabaseRepository,
    isPremium: Boolean,
    onPremiumChanged: (Boolean) -> Unit,
    onAccountChanged: () -> Unit,
    authRefreshVersion: Int = 0
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var email by remember { mutableStateOf<String?>(null) }
    var familyName by remember { mutableStateOf<String?>(null) }
    var familyId by remember { mutableStateOf<String?>(null) }
    var members by remember { mutableStateOf<List<SupabaseFamilyMemberDto>>(emptyList()) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var mode by remember { mutableStateOf("login") }
    var inputEmail by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var familyInput by remember { mutableStateOf("") }
    var showCreateFamily by remember { mutableStateOf(false) }
    var refreshKey by remember { mutableIntStateOf(0) }
    val setupRequired = billing.isPremiumSetupRequired()

    fun loadAccount() {
        if (!SupabaseClientProvider.isConfigured) return
        scope.launch {
            busy = true
            error = null
            runCatching {
                email = supabaseRepo.currentUserEmail()
                val family = supabaseRepo.familiesForCurrentUser().firstOrNull()
                familyId = family?.id
                familyName = family?.name
                members = family?.let { supabaseRepo.familyMembers(it.id) } ?: emptyList()
            }.onFailure { error = it.message ?: "Errore di connessione" }
            busy = false
        }
    }

    LaunchedEffect(refreshKey, isPremium, setupRequired, authRefreshVersion) {
        loadAccount()
        if (setupRequired && email != null && familyId != null && !isPremium) {
            billing.recheckOwnedPurchases()
        }
    }

    fun authenticate() {
        scope.launch {
            busy = true
            error = null
            runCatching {
                if (mode == "login") supabaseRepo.signIn(inputEmail, password)
                else supabaseRepo.signUp(inputEmail, password)
            }.onSuccess {
                inputEmail = ""
                password = ""
                refreshKey++
                onAccountChanged()
                billing.recheckOwnedPurchases()
            }.onFailure { error = it.message ?: "Operazione non riuscita" }
            busy = false
        }
    }

    fun createFamily() {
        scope.launch {
            busy = true
            error = null
            runCatching { supabaseRepo.createFamily(familyInput) }
                .onSuccess {
                    familyInput = ""
                    showCreateFamily = false
                    refreshKey++
                    onAccountChanged()
                    billing.recheckOwnedPurchases()
                }
                .onFailure { error = it.message ?: "Creazione famiglia non riuscita" }
            busy = false
        }
    }

    LazyColumn(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Text("Premium", style = MaterialTheme.typography.headlineSmall)
        }

        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("MoneyFamily Premium", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                        Text(if (isPremium) "ATTIVO" else "PREMIUM", style = MaterialTheme.typography.labelLarge)
                    }
                    Text("Con Premium sblocchi:")
                    Text("• Budget mensile per tipologia e categoria, con indicatori e warning.")
                    Text("• Confronto dei costi effettivi tra due mesi oppure due anni, per tipologia o categoria.")
                    Text("• Salvataggio dei dati su Cloud tramite Supabase.")
                    Text("• Accesso agli stessi dati da più dispositivi con lo stesso account.")
                    Text("• Condivisione dei dati a livello di famiglia.")

                    if (!isPremium && !setupRequired) {
                        val price = billing.price()
                        val activity = context as? Activity
                        Button(
                            enabled = price != null && activity != null,
                            onClick = { if (activity != null) billing.launchPurchase(activity) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (price != null) "Acquista Premium · $price" else "Acquista Premium")
                        }
                        if (price == null) Text("Il prodotto Premium non è disponibile su Google Play.", style = MaterialTheme.typography.labelSmall)
                    }

                    if (isPremium) {
                        Text("Premium attivo.", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        if (!isPremium && setupRequired) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        Text("Completa l'attivazione Premium", style = MaterialTheme.typography.titleLarge)
                        Text("Il pagamento Google Play è stato rilevato. Prima di sbloccare Premium devi creare o usare un account MoneyFamily e associarlo a una famiglia.")
                        Text("I dati già presenti nella versione Free restano locali e verranno sincronizzati sul Cloud dopo l'attivazione.")

                        if (email == null) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                FilterChip(selected = mode == "login", onClick = { mode = "login" }, label = { Text("Accedi") }, modifier = Modifier.weight(1f))
                                FilterChip(selected = mode == "signup", onClick = { mode = "signup" }, label = { Text("Crea account") }, modifier = Modifier.weight(1f))
                            }
                            OutlinedTextField(inputEmail, { inputEmail = it }, label = { Text("Email") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(password, { password = it }, label = { Text("Password") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            Button(
                                enabled = !busy && inputEmail.isNotBlank() && password.length >= 6,
                                onClick = ::authenticate,
                                modifier = Modifier.fillMaxWidth()
                            ) { Text(if (busy) "Attendere…" else if (mode == "login") "Accedi" else "Crea account") }
                            if (mode == "signup") Text("Se la conferma email è attiva, apri il link ricevuto e torna nell'app.", style = MaterialTheme.typography.bodySmall)
                        } else {
                            Text("Account: " + email)
                            if (familyId == null) {
                                Text("Crea la famiglia per completare l'attivazione.")
                                if (showCreateFamily) {
                                    OutlinedTextField(familyInput, { familyInput = it }, label = { Text("Nome famiglia") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton({ showCreateFamily = false }, Modifier.weight(1f)) { Text("Annulla") }
                                        Button(
                                            enabled = !busy && familyInput.isNotBlank(),
                                            onClick = ::createFamily,
                                            modifier = Modifier.weight(1f)
                                        ) { Text("Crea") }
                                    }
                                } else {
                                    Button({ showCreateFamily = true }, Modifier.fillMaxWidth()) { Text("+ Crea famiglia") }
                                }
                            } else {
                                Text("Famiglia: " + familyName.orEmpty())
                                Text("Verifica dell'acquisto Premium in corso…")
                                OutlinedButton({ billing.recheckOwnedPurchases() }, Modifier.fillMaxWidth()) { Text("Verifica Premium") }
                            }
                        }
                        error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                    }
                }
            }
        }

        if (isPremium) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        Text("Account e Cloud", style = MaterialTheme.typography.titleLarge)
                        if (email == null) {
                            Text("Accedi per attivare il salvataggio Cloud e la sincronizzazione multi-dispositivo.")
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                FilterChip(selected = mode == "login", onClick = { mode = "login" }, label = { Text("Accedi") }, modifier = Modifier.weight(1f))
                                FilterChip(selected = mode == "signup", onClick = { mode = "signup" }, label = { Text("Crea account") }, modifier = Modifier.weight(1f))
                            }
                            OutlinedTextField(inputEmail, { inputEmail = it }, label = { Text("Email") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(password, { password = it }, label = { Text("Password") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            Button(!busy && inputEmail.isNotBlank() && password.length >= 6, ::authenticate, Modifier.fillMaxWidth()) {
                                Text(if (busy) "Attendere…" else if (mode == "login") "Accedi" else "Crea account")
                            }
                        } else {
                            Text("Account: " + email)
                            if (familyId == null) {
                                Text("Nessuna famiglia associata.")
                                Button({ showCreateFamily = true }, Modifier.fillMaxWidth()) { Text("+ Crea famiglia") }
                                if (showCreateFamily) {
                                    OutlinedTextField(familyInput, { familyInput = it }, label = { Text("Nome famiglia") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                                    Button(!busy && familyInput.isNotBlank(), ::createFamily, Modifier.fillMaxWidth()) { Text("Crea") }
                                }
                            } else {
                                Text("Famiglia: " + familyName.orEmpty())
                                Text("Cloud attivo. Usa lo stesso account sugli altri dispositivi per accedere agli stessi dati.")
                                Text("Membri: " + members.size, style = MaterialTheme.typography.bodySmall)
                            }
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        runCatching { supabaseRepo.signOutCurrentDevice() }
                                            .onSuccess { refreshKey++; onPremiumChanged(false); onAccountChanged() }
                                            .onFailure { error = it.message }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Esci da questo dispositivo") }
                        }
                        error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                    }
                }
            }
        }
    }
}
