package com.moneyfamily.app

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.moneyfamily.app.data.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

@Composable
fun PremiumSection(
    billing: PremiumBilling,
    supabaseRepo: SupabaseRepository,
    cloudSync: CloudSyncRepository,
    isPremium: Boolean,
    onPremiumChanged: (Boolean) -> Unit,
    onAccountChanged: () -> Unit,
    onCloudDataChanged: suspend () -> Unit = {},
    authRefreshVersion: Int = 0,
    passwordRecovery: Boolean = false
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var email by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var mode by remember { mutableStateOf("login") }
    var inputEmail by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var recoveryActive by remember(passwordRecovery) { mutableStateOf(passwordRecovery) }
    var resetSent by remember { mutableStateOf(false) }
    var refreshKey by remember { mutableIntStateOf(0) }
    var cloudBusy by remember { mutableStateOf(false) }
    var cloudMessage by remember { mutableStateOf<String?>(null) }

    val setupRequired = billing.isPremiumSetupRequired()

    LaunchedEffect(setupRequired, email) {
        if (setupRequired && email == null) {
            mode = "signup"
        }
    }

    fun loadAccount() {
        if (!SupabaseClientProvider.isConfigured) return
        scope.launch {
            busy = true
            error = null
            runCatching {
                email = supabaseRepo.currentUserEmail()
                if (email != null) {
                    // The backend workspace is created automatically. The user
                    // never sees or manages a family/group.
                    supabaseRepo.ensureAccountWorkspace()
                }
            }.onFailure {
                error = it.message ?: "Errore di connessione"
            }
            busy = false
        }
    }

    LaunchedEffect(refreshKey, isPremium, setupRequired, authRefreshVersion) {
        loadAccount()
        if (setupRequired && email != null && !isPremium) {
            billing.recheckOwnedPurchases()
        }
    }

    fun authenticate() {
        scope.launch {
            busy = true
            error = null
            runCatching {
                if (mode == "login") {
                    supabaseRepo.signIn(inputEmail, password)
                    supabaseRepo.ensureAccountWorkspace()
                } else {
                    // Production flow: Supabase Confirm Email is disabled.
                    // Signup therefore creates an authenticated Cloud session
                    // immediately and the account workspace is created now.
                    supabaseRepo.signUp(inputEmail, password)
                }
            }.onSuccess {
                inputEmail = ""
                password = ""
                refreshKey++
                onAccountChanged()
                billing.recheckOwnedPurchases()
            }.onFailure {
                error = it.message ?: "Operazione non riuscita"
            }
            busy = false
        }
    }

    LazyColumn(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Premium", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                Text(
                    if (isPremium) "ATTIVO" else "PREMIUM",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isPremium) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                )
            }
        }

        item {
            Card(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    Text("MoneyFamily Premium", style = MaterialTheme.typography.titleLarge)
                    Text("Con Premium sono disponibili:")
                    Text("• Budget mensile per tipologia e categoria, con indicatori e warning.")
                    Text("• Confronto dei costi effettivi tra due mesi oppure due anni.")
                    Text("• Confronto per tipologia e per categoria.")
                    Text("• Salvataggio e sincronizzazione Cloud tramite Supabase.")
                    Text("• Accesso allo stesso account su più dispositivi, per mantenere i dati Cloud sincronizzati.")

                    if (!isPremium && !setupRequired) {
                        val price = billing.price()
                        val activity = context as? Activity
                        Button(
                            enabled = price != null && activity != null,
                            onClick = {
                                if (activity != null) billing.launchPurchase(activity)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (price != null) "Acquista Premium · $price" else "Acquista Premium")
                        }
                        if (price == null) {
                            Text(
                                "Il prodotto Premium non è disponibile su Google Play.",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }

                    if (BuildConfig.INTERNAL_PREMIUM_TEST && !isPremium) {
                        OutlinedButton(
                            onClick = { billing.enableInternalTestPremium() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Sblocca Premium per test")
                        }
                        Text(
                            "Solo versione Test interno: attiva Premium localmente per verificare tutte le funzioni senza effettuare un acquisto.",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }

                    if (isPremium) {
                        Text("Premium attivo per questo account.", color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "I dati restano sul dispositivo e vengono inviati al Cloud solo quando lo richiedi.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                enabled = !cloudBusy && SupabaseClientProvider.isConfigured && email != null,
                                onClick = {
                                    scope.launch {
                                        cloudBusy = true
                                        cloudMessage = null
                                        try {
                                            withTimeout(60_000L) {
                                                cloudSync.saveToCloud()
                                                    .onSuccess { count ->
                                                        cloudMessage = "Cloud aggiornato: $count operazioni salvate."
                                                    }
                                                    .onFailure {
                                                        cloudMessage = "Salvataggio Cloud non riuscito: " + (it.message ?: "errore")
                                                    }
                                            }
                                        } catch (t: Throwable) {
                                            cloudMessage = if (t is kotlinx.coroutines.TimeoutCancellationException) {
                                                "Salvataggio Cloud non riuscito: timeout dopo 60 secondi."
                                            } else {
                                                "Salvataggio Cloud non riuscito: " + (t.message ?: "errore")
                                            }
                                        } finally {
                                            cloudBusy = false
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(if (cloudBusy) "Salvataggio…" else "☁️ Salva sul Cloud")
                            }
                            OutlinedButton(
                                enabled = !cloudBusy && SupabaseClientProvider.isConfigured && email != null,
                                onClick = {
                                    scope.launch {
                                        cloudBusy = true
                                        cloudMessage = null
                                        try {
                                            withTimeout(60_000L) {
                                                cloudSync.loadFromCloud()
                                                    .onSuccess { count ->
                                                        cloudMessage = "Caricamento completato: $count operazioni ripristinate dal Cloud."
                                                        onCloudDataChanged()
                                                    }
                                                    .onFailure {
                                                        cloudMessage = "Caricamento Cloud non riuscito: " + (it.message ?: "errore")
                                                    }
                                            }
                                        } catch (t: Throwable) {
                                            cloudMessage = if (t is kotlinx.coroutines.TimeoutCancellationException) {
                                                "Caricamento Cloud non riuscito: timeout dopo 60 secondi."
                                            } else {
                                                "Caricamento Cloud non riuscito: " + (t.message ?: "errore")
                                            }
                                        } finally {
                                            cloudBusy = false
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(if (cloudBusy) "Caricamento…" else "☁️ Carica dal Cloud")
                            }
                        }
                        cloudMessage?.let {
                            Text(
                                it,
                                color = if (it.startsWith("Cloud aggiornato") || it.startsWith("Caricamento completato")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }

        item {
            Card(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    Text("Account MoneyFamily", style = MaterialTheme.typography.titleLarge)

                    if (!SupabaseClientProvider.isConfigured) {
                        Text("Cloud non configurato su questo dispositivo. Il funzionamento locale della versione Free resta invariato.")
                    } else if (email == null) {
                        if (setupRequired) {
                            Text(
                                "Il pagamento Google Play è stato rilevato. Per sbloccare Premium devi accedere o creare un account MoneyFamily."
                            )
                        } else {
                            Text(
                                "L'account non è richiesto per la versione Free. Puoi crearlo qui per preparare l'accesso Cloud e multi-dispositivo."
                            )
                        }

                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = mode == "login",
                                onClick = { mode = "login" },
                                label = { Text("Accedi") },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = mode == "signup",
                                onClick = { mode = "signup" },
                                label = { Text("Crea account") },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        OutlinedTextField(
                            value = inputEmail,
                            onValueChange = { inputEmail = it },
                            label = { Text("Email") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (recoveryActive) {
                            Text("Reimposta la password", style = MaterialTheme.typography.titleMedium)
                            Text("Inserisci la nuova password per completare il recupero dell'account.")
                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = { Text("Nuova password") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = confirmPassword,
                                onValueChange = { confirmPassword = it },
                                label = { Text("Conferma nuova password") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (password.isNotBlank() && password.length < 6) {
                                Text("La password deve contenere almeno 6 caratteri.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                            }
                            if (confirmPassword.isNotBlank() && confirmPassword != password) {
                                Text("Le password non coincidono.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                            }
                            Button(
                                enabled = !busy && password.length >= 6 && password == confirmPassword,
                                onClick = {
                                    scope.launch {
                                        busy = true
                                        error = null
                                        runCatching { supabaseRepo.updatePassword(password) }
                                            .onSuccess {
                                                password = ""
                                                confirmPassword = ""
                                                recoveryActive = false
                                                resetSent = false
                                                mode = "login"
                                                refreshKey++
                                            }
                                            .onFailure {
                                                error = it.message ?: "Impossibile aggiornare la password."
                                            }
                                        busy = false
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(if (busy) "Attendere…" else "Aggiorna password")
                            }
                            TextButton(
                                onClick = {
                                    recoveryActive = false
                                    mode = "login"
                                    error = null
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Annulla")
                            }
                        } else if (mode == "reset") {
                            Text("Recupera password", style = MaterialTheme.typography.titleMedium)
                            Text("Inserisci la tua email per ricevere il link per reimpostare la password.")
                            OutlinedTextField(
                                value = inputEmail,
                                onValueChange = { inputEmail = it },
                                label = { Text("Email") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Button(
                                enabled = !busy && inputEmail.isNotBlank(),
                                onClick = {
                                    scope.launch {
                                        busy = true
                                        error = null
                                        resetSent = false
                                        runCatching { supabaseRepo.sendPasswordReset(inputEmail) }
                                            .onSuccess { resetSent = true }
                                            .onFailure { error = it.message ?: "Impossibile inviare il link di recupero." }
                                        busy = false
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(if (busy) "Attendere…" else "Invia link di recupero")
                            }
                            if (resetSent) {
                                Text(
                                    "Se l'indirizzo è associato a un account, riceverai le istruzioni per impostare una nuova password.",
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            TextButton(
                                onClick = {
                                    mode = "login"
                                    error = null
                                    resetSent = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Torna ad Accedi")
                            }
                        } else {
                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = { Text("Password") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Button(
                                enabled = !busy && inputEmail.isNotBlank() && password.length >= 6,
                                onClick = ::authenticate,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    if (busy) "Attendere…"
                                    else if (mode == "login") "Accedi"
                                    else "Crea account"
                                )
                            }
                            if (mode == "login") {
                                TextButton(
                                    onClick = {
                                        mode = "reset"
                                        error = null
                                        resetSent = false
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Password dimenticata?")
                                }
                            }
                        }

                        if (mode == "signup" && !recoveryActive) {
                            Text(
                                "Account Cloud: dopo la registrazione verrai autenticato automaticamente e i dati Premium saranno sincronizzati tra i dispositivi.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    } else {
                        Text("Account: $email")
                        Text(
                            if (isPremium)
                                "Cloud attivo. Lo stesso account può essere utilizzato su più dispositivi."
                            else
                                "Account autenticato. Il Cloud sarà utilizzato quando Premium sarà attivo.",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        if (setupRequired && !isPremium) {
                            Text(
                                "Verifica dell'acquisto Premium in corso…",
                                color = MaterialTheme.colorScheme.primary
                            )
                            OutlinedButton(
                                onClick = { billing.recheckOwnedPurchases() },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Verifica Premium")
                            }
                        }

                        OutlinedButton(
                            enabled = !busy,
                            onClick = {
                                scope.launch {
                                    runCatching { supabaseRepo.signOutCurrentDevice() }
                                        .onSuccess {
                                            email = null
                                            refreshKey++
                                            onPremiumChanged(false)
                                            onAccountChanged()
                                        }
                                        .onFailure {
                                            error = it.message ?: "Logout non riuscito"
                                        }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Esci da questo dispositivo")
                        }
                    }

                    error?.let {
                        Text(
                            it,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}
