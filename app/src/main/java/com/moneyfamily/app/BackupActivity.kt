package com.moneyfamily.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.moneyfamily.app.data.BackupManager
import com.moneyfamily.app.data.RoomRepository
import kotlinx.coroutines.launch

class BackupActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { BackupScreen(onBack = { finish() }) }
    }
}

@Composable
private fun BackupScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { RoomRepository(context) }
    var status by remember { mutableStateOf("") }
    var restoreUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var showRestoreConfirm by remember { mutableStateOf(false) }

    val createLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) scope.launch {
            runCatching {
                BackupManager.writeBackup(context, uri, repository)
                status = "Backup creato correttamente"
            }.onFailure { status = "Errore backup: ${it.message ?: "operazione non riuscita"}" }
        }
    }

    val openLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            restoreUri = uri
            showRestoreConfirm = true
        }
    }

    DisposableEffect(Unit) { onDispose { repository.close() } }

    if (showRestoreConfirm) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirm = false; restoreUri = null },
            title = { Text("Ripristina backup") },
            text = { Text("Il ripristino sostituirà tutti i dati attualmente presenti nell'app. Le operazioni, tipologie, categorie, membri e associazioni del backup diventeranno i dati correnti. Continuare?") },
            confirmButton = {
                TextButton(onClick = {
                    val uri = restoreUri
                    showRestoreConfirm = false
                    restoreUri = null
                    if (uri != null) scope.launch {
                        runCatching {
                            BackupManager.restoreBackup(context, uri, repository)
                            status = "Ripristino completato correttamente"
                        }.onFailure { status = "Errore ripristino: ${it.message ?: "backup non valido"}" }
                    }
                }) { Text("Ripristina") }
            },
            dismissButton = { TextButton(onClick = { showRestoreConfirm = false; restoreUri = null }) { Text("Annulla") } }
        )
    }

    Column(
        Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Backup e ripristino", style = MaterialTheme.typography.headlineSmall)
        Text("Salva una copia completa dei dati MoneyFamily in un file JSON. Il backup comprende operazioni e configurazioni.", style = MaterialTheme.typography.bodyMedium)

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Backup", style = MaterialTheme.typography.titleLarge)
                Text("Il file contiene movimenti, tipologie, categorie, membri della famiglia e associazioni tipologia → categoria.")
                Button(
                    onClick = { createLauncher.launch("MoneyFamily_Backup.json") },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Crea backup") }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Ripristino", style = MaterialTheme.typography.titleLarge)
                Text("Seleziona un backup MoneyFamily precedentemente creato. I dati attuali verranno sostituiti.")
                OutlinedButton(
                    onClick = { openLauncher.launch(arrayOf("application/json", "text/json", "text/plain")) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Ripristina backup") }
            }
        }

        Spacer(Modifier.height(2.dp))
        if (status.isNotBlank()) Text(status, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.weight(1f))
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("← Torna a MoneyFamily") }
    }
}
