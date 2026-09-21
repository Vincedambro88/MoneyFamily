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
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var mode by remember { mutableStateOf("login") }
    var inputEmail by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var refreshKey by remember { mutableIntStateOf(0) }

    val setupRequired = billing.isPremiumSetupRequired()

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
                } else {
                    supabaseRepo.signUp(inputEmail, password)
                }
                supabaseRepo.ensureAccountWorkspace()
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
                    Text("• Accesso agli stessi dati da più dispositivi con lo stesso account.")

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

                    if (isPremium) {
                        Text("Premium attivo per questo account.", color = MaterialTheme.colorScheme.primary)
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

                        if (mode == "signup") {
                            Text(
                                "Se la conferma email è attiva, apri il link ricevuto e torna nell'app.",
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
