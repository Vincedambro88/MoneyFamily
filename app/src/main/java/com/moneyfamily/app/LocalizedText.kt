package com.moneyfamily.app

import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import java.util.Locale

private val mfResourceIds: Map<String, Int> = mapOf(
    "Dashboard" to R.string.mf_dashboard,
    "Operazioni" to R.string.mf_operazioni,
    "Inserisci" to R.string.mf_inserisci,
    "Impostazioni" to R.string.mf_impostazioni,
    "Budget" to R.string.mf_budget,
    "Premium" to R.string.mf_premium,
    "Entrate" to R.string.mf_entrate,
    "Uscite" to R.string.mf_uscite,
    "Saldo" to R.string.mf_saldo,
    "Composizione mensile" to R.string.mf_composizione_mensile,
    "Totali per categoria" to R.string.mf_totali_per_categoria,
    "Composizione per categoria" to R.string.mf_composizione_per_categoria,
    "Totali per componente" to R.string.mf_totali_per_componente,
    "Totali per tipologia" to R.string.mf_totali_per_tipologia,
    "Composizione per tipologia" to R.string.mf_composizione_per_tipologia,
    "Riepilogo" to R.string.mf_riepilogo,
    "Spese" to R.string.mf_spese,
    "Ricavi" to R.string.mf_ricavi,
    "Analisi" to R.string.mf_analisi,
    "Storico reale di entrate e uscite" to R.string.mf_storico_reale_di_entrate_e_uscite,
    "Andamento mensile" to R.string.mf_andamento_mensile,
    "Spese per categoria" to R.string.mf_spese_per_categoria,
    "Analisi per membro" to R.string.mf_analisi_per_membro,
    "Anno" to R.string.mf_anno,
    "Mese" to R.string.mf_mese,
    "Anno / Anno" to R.string.mf_anno_anno,
    "Mese / Mese" to R.string.mf_mese_mese,
    "Periodo 1" to R.string.mf_periodo_1,
    "Periodo 2" to R.string.mf_periodo_2,
    "Categoria" to R.string.mf_categoria,
    "Categorie" to R.string.mf_categorie,
    "Tipologia" to R.string.mf_tipologia,
    "Tipologie" to R.string.mf_tipologie,
    "Descrizione" to R.string.mf_descrizione,
    "Importo (+ ricavo / - spesa)" to R.string.mf_importo_ricavo_spesa,
    "Effettuata da" to R.string.mf_effettuata_da,
    "Componente" to R.string.mf_componente,
    "Famiglia" to R.string.mf_famiglia,
    "Membri della famiglia" to R.string.mf_membri_della_famiglia,
    "Associazioni" to R.string.mf_associazioni,
    "Associazioni tipologia → categoria" to R.string.mf_associazioni_tipologia_categoria,
    "Gestione anagrafiche" to R.string.mf_gestione_anagrafiche,
    "+ Nuova operazione" to R.string.mf_nuova_operazione,
    "+ Inserisci operazione" to R.string.mf_inserisci_operazione,
    "+ Aggiungi categoria" to R.string.mf_aggiungi_categoria,
    "+ Aggiungi membro" to R.string.mf_aggiungi_membro,
    "+ Aggiungi tipologia" to R.string.mf_aggiungi_tipologia,
    "+ Crea famiglia" to R.string.mf_crea_famiglia,
    "Nuova operazione" to R.string.mf_nuova_operazione_2,
    "Modifica operazione" to R.string.mf_modifica_operazione,
    "Modifica" to R.string.mf_modifica,
    "Elimina" to R.string.mf_elimina,
    "Cancella" to R.string.mf_cancella,
    "Salva" to R.string.mf_salva,
    "Annulla" to R.string.mf_annulla,
    "Crea" to R.string.mf_crea,
    "Seleziona" to R.string.mf_seleziona,
    "Tutte" to R.string.mf_tutte,
    "Tutti" to R.string.mf_tutti,
    "Cerca descrizione" to R.string.mf_cerca_descrizione,
    "Scarica in Excel" to R.string.mf_scarica_in_excel,
    "Scarica modello Excel" to R.string.mf_scarica_modello_excel,
    "Importa da Excel" to R.string.mf_importa_da_excel,
    "Operazioni esportate in Excel" to R.string.mf_operazioni_esportate_in_excel,
    "Modello Excel salvato" to R.string.mf_modello_excel_salvato,
    "Operazioni importate" to R.string.mf_operazioni_importate,
    "Nessun dato" to R.string.mf_nessun_dato,
    "Nessun dato per il mese" to R.string.mf_nessun_dato_per_il_mese,
    "Nessun valore" to R.string.mf_nessun_valore,
    "Non classificata" to R.string.mf_non_classificata,
    "Non assegnato" to R.string.mf_non_assegnato,
    "Da classificare" to R.string.mf_da_classificare,
    "ATTIVO" to R.string.mf_attivo,
    "Disattivato" to R.string.mf_disattivato,
    "Disattiva" to R.string.mf_disattiva,
    "Attendere…" to R.string.mf_attendere,
    "Accedi" to R.string.mf_accedi,
    "Registrati" to R.string.mf_registrati,
    "Crea account" to R.string.mf_crea_account,
    "Email" to R.string.mf_email,
    "Password" to R.string.mf_password,
    "Password dimenticata?" to R.string.mf_password_dimenticata,
    "Recupera password" to R.string.mf_recupera_password,
    "Reimposta la password" to R.string.mf_reimposta_la_password,
    "Nuova password" to R.string.mf_nuova_password,
    "Conferma nuova password" to R.string.mf_conferma_nuova_password,
    "Aggiorna password" to R.string.mf_aggiorna_password,
    "Invia link di recupero" to R.string.mf_invia_link_di_recupero,
    "Torna ad Accedi" to R.string.mf_torna_ad_accedi,
    "Esci da questo dispositivo" to R.string.mf_esci_da_questo_dispositivo,
    "Account MoneyFamily" to R.string.mf_account_moneyfamily,
    "Account e famiglia" to R.string.mf_account_e_famiglia,
    "Account autenticato. Il Cloud sarà utilizzato quando Premium sarà attivo." to R.string.mf_account_autenticato_il_cloud_sar_utilizzato_quando_premium_sar_attivo,
    "Cloud attivo. Lo stesso account può essere utilizzato su più dispositivi." to R.string.mf_cloud_attivo_lo_stesso_account_pu_essere_utilizzato_su_pi_dispositivi,
    "Account Cloud: dopo la registrazione verrai autenticato automaticamente e i dati Premium saranno sincronizzati tra i dispositivi." to R.string.mf_account_cloud_dopo_la_registrazione_verrai_autenticato_automaticamente,
    "I dati restano sul dispositivo e vengono inviati al Cloud solo quando lo richiedi." to R.string.mf_i_dati_restano_sul_dispositivo_e_vengono_inviati_al_cloud_solo_quando_,
    "☁️ Salva sul Cloud" to R.string.mf_salva_sul_cloud,
    "☁️ Carica dal Cloud" to R.string.mf_carica_dal_cloud,
    "Salvataggio…" to R.string.mf_salvataggio,
    "Caricamento…" to R.string.mf_caricamento,
    "Verifica Premium" to R.string.mf_verifica_premium,
    "Premium attivo per questo account." to R.string.mf_premium_attivo_per_questo_account,
    "Confronto costi effettivi" to R.string.mf_confronto_costi_effettivi,
    "Budget mensile per tipologia" to R.string.mf_budget_mensile_per_tipologia,
    "Confronto Budget / Effettivo — Categoria" to R.string.mf_confronto_budget_effettivo_categoria,
    "Confronto Budget / Effettivo — Tipologia" to R.string.mf_confronto_budget_effettivo_tipologia,
    "Copia budget al mese successivo" to R.string.mf_copia_budget_al_mese_successivo,
    "Raggruppa il confronto per:" to R.string.mf_raggruppa_il_confronto_per,
    "Visualizzazione attiva: solo CATEGORIE" to R.string.mf_visualizzazione_attiva_solo_categorie,
    "Visualizzazione attiva: solo TIPOLOGIE" to R.string.mf_visualizzazione_attiva_solo_tipologie,
    "Backup" to R.string.mf_backup,
    "Backup e ripristino" to R.string.mf_backup_e_ripristino,
    "Crea backup" to R.string.mf_crea_backup,
    "Ripristino" to R.string.mf_ripristino,
    "Ripristina backup" to R.string.mf_ripristina_backup,
    "Ripristina" to R.string.mf_ripristina,
    "Backup creato correttamente" to R.string.mf_backup_creato_correttamente,
    "Ripristino completato correttamente" to R.string.mf_ripristino_completato_correttamente,
    "← Torna a MoneyFamily" to R.string.mf_torna_a_moneyfamily,
    "Membri" to R.string.mf_membri,
    "MoneyFamily Premium" to R.string.mf_moneyfamily_premium
)


private val mfExtraTranslations: Map<String, Map<String, String>> = mapOf(
    "en" to mapOf(
        "Composizione esercizio " to "Year breakdown ", "Configurazione" to "Settings", "Nuova categoria" to "New category", "Nuova tipologia" to "New type", "Nuovo membro" to "New member",
        "Modifica categoria" to "Edit category", "Modifica tipologia" to "Edit type", "Modifica membro" to "Edit member", "Modifica valore" to "Edit value", "Disattiva valore" to "Disable value",
        "Tocca la categoria per modificarla." to "Tap the category to edit it.", "Nome" to "Name", "Nome famiglia" to "Family name", "Membri della famiglia" to "Family members",
        "Gestione anagrafiche" to "Master data management", "Nessuna famiglia associata a questo account." to "No family is associated with this account.",
        "Cloud non configurato su questo dispositivo. Funzionamento locale invariato." to "Cloud is not configured on this device. Local operation is unchanged.",
        "Cloud non configurato su questo dispositivo. Il funzionamento locale della versione Free resta invariato." to "Cloud is not configured on this device. Free version local operation is unchanged.",
        "Con Premium sono disponibili:" to "With Premium you get:", "Sblocca Premium per test" to "Unlock Premium for testing",
        "Budget, analisi avanzate e sincronizzazione sono sbloccati." to "Budget, advanced analysis and synchronization are unlocked.",
        "Verifica dell'acquisto Premium in corso…" to "Verifying Premium purchase…", "Il prodotto Premium non è disponibile su Google Play." to "The Premium product is not available on Google Play.",
        "Il prodotto Premium deve essere configurato su Google Play per rendere disponibile l'acquisto." to "The Premium product must be configured on Google Play to enable purchase.",
        "Inserisci" to "Add", "Inserisci la nuova password per completare il recupero dell'account." to "Enter the new password to complete account recovery.",
        "La password deve contenere almeno 6 caratteri." to "The password must contain at least 6 characters.", "Le password non coincidono." to "Passwords do not match.",
        "Impossibile aggiornare la password." to "Unable to update password.", "Impossibile inviare il link di recupero." to "Unable to send the recovery link.",
        "Se l'indirizzo è associato a un account, riceverai le istruzioni per impostare una nuova password." to "If the address is associated with an account, you will receive instructions to set a new password.",
        "Accedi allo stesso account su più dispositivi per condividere la stessa famiglia." to "Use the same account on multiple devices to share the same family.",
        "Usa lo stesso account sugli altri dispositivi per accedere agli stessi dati della famiglia." to "Use the same account on other devices to access the same family data.",
        "Premium: condiviso a livello famiglia" to "Premium: shared at family level", "Membri:" to "Members:", "Famiglia:" to "Family:",
        "Confronta due mesi specifici oppure due anni, per tipologia o categoria." to "Compare two specific months or two years, by type or category.",
        "Nessun costo disponibile." to "No cost data available.", "Totale mensile" to "Monthly total",
        "Cerca tra tutte le operazioni dell'anno" to "Search all operations in the year", "Cancella operazioni" to "Delete operations",
        "Cancella operazioni dell'anno" to "Delete operations for the year", "Cancella tutte le operazioni del periodo" to "Delete all operations in the period",
        "Annulla" to "Cancel", "Ripristino completato correttamente" to "Restore completed successfully", "Backup creato correttamente" to "Backup created successfully",
        "Crea backup" to "Create backup", "Backup e ripristino" to "Backup and restore", "Ripristina backup" to "Restore backup",
        "Salva una copia completa dei dati MoneyFamily in un file JSON. Il backup comprende operazioni e configurazioni." to "Save a complete copy of MoneyFamily data to a JSON file. The backup includes operations and configuration.",
        "Seleziona un backup MoneyFamily precedentemente creato. I dati attuali verranno sostituiti." to "Select a previously created MoneyFamily backup. Current data will be replaced.",
        "Esporta tutte le operazioni nello stesso tracciato previsto dal modello Excel." to "Export all operations using the same format required by the Excel template.",
        "Cloud aggiornato" to "Cloud updated", "Caricamento completato" to "Upload completed", "Errore" to "Error", "Errore di connessione" to "Connection error",
        "Operazione non riuscita" to "Operation failed", "Logout non riuscito" to "Logout failed", "Creazione famiglia non riuscita" to "Family creation failed",
        "Caricamento Cloud non riuscito: timeout dopo 60 secondi." to "Cloud upload failed: timeout after 60 seconds.",
        "Salvataggio Cloud non riuscito: timeout dopo 60 secondi." to "Cloud save failed: timeout after 60 seconds.",
        "Visualizzazione attiva: solo CATEGORIE" to "Active view: CATEGORIES only", "Visualizzazione attiva: solo TIPOLOGIE" to "Active view: TYPES only",
        "Raggruppa il confronto per:" to "Group comparison by:", "Copia budget al mese successivo" to "Copy budget to next month",
        "⚠ Budget raggiunto/superato" to "⚠ Budget reached/exceeded", "⚠ Budget utilizzato almeno all'80%" to "⚠ Budget used at least 80%"
    ),
    "de" to mapOf("Configurazione" to "Einstellungen","Nuova categoria" to "Neue Kategorie","Nuova tipologia" to "Neue Typologie","Nuovo membro" to "Neues Mitglied","Modifica" to "Bearbeiten","Elimina" to "Löschen","Annulla" to "Abbrechen","Crea" to "Erstellen","Nome" to "Name","Nome famiglia" to "Familienname","Membri della famiglia" to "Familienmitglieder","Nessun dato" to "Keine Daten","Nessun costo disponibile." to "Keine Kostendaten verfügbar.","Totale mensile" to "Monatssumme","Errore di connessione" to "Verbindungsfehler","Operazione non riuscita" to "Vorgang fehlgeschlagen","Verifica dell'acquisto Premium in corso…" to "Premium-Kauf wird überprüft…", "Con Premium sono disponibili:" to "Mit Premium verfügbar:", "Sblocca Premium per test" to "Premium zum Testen freischalten", "La password deve contenere almeno 6 caratteri." to "Das Passwort muss mindestens 6 Zeichen enthalten.", "Le password non coincidono." to "Die Passwörter stimmen nicht überein.", "Impossibile aggiornare la password." to "Passwort konnte nicht aktualisiert werden.", "Impossibile inviare il link di recupero." to "Wiederherstellungslink konnte nicht gesendet werden.", "Cloud aggiornato" to "Cloud aktualisiert", "Caricamento completato" to "Laden abgeschlossen", "Errore" to "Fehler", "Logout non riuscito" to "Abmeldung fehlgeschlagen", "Creazione famiglia non riuscita" to "Familie konnte nicht erstellt werden", "Cerca " to "Suchen ", "Riepilogo annuale " to "Jahresübersicht ", "⚠ Budget raggiunto/superato" to "⚠ Budget erreicht/überschritten", "⚠ Budget utilizzato almeno all'80%" to "⚠ Budget zu mindestens 80% verwendet"),
    "fr" to mapOf("Configurazione" to "Paramètres","Nuova categoria" to "Nouvelle catégorie","Nuova tipologia" to "Nouveau type","Nuovo membro" to "Nouveau membre","Modifica" to "Modifier","Elimina" to "Supprimer","Annulla" to "Annuler","Crea" to "Créer","Nome" to "Nom","Nome famiglia" to "Nom de famille","Membri della famiglia" to "Membres de la famille","Nessun dato" to "Aucune donnée","Nessun costo disponibile." to "Aucune donnée de coût disponible.","Totale mensile" to "Total mensuel","Errore di connessione" to "Erreur de connexion","Operazione non riuscita" to "Opération échouée","Verifica dell'acquisto Premium in corso…" to "Vérification de l'achat Premium…", "Con Premium sono disponibili:" to "Avec Premium, vous disposez de :", "Sblocca Premium per test" to "Débloquer Premium pour tester", "La password deve contenere almeno 6 caratteri." to "Le mot de passe doit contenir au moins 6 caractères.", "Le password non coincidono." to "Les mots de passe ne correspondent pas.", "Impossibile aggiornare la password." to "Impossible de mettre à jour le mot de passe.", "Impossibile inviare il link di recupero." to "Impossible d'envoyer le lien de récupération.", "Cloud aggiornato" to "Cloud mis à jour", "Caricamento completato" to "Chargement terminé", "Errore" to "Erreur", "Logout non riuscito" to "Déconnexion impossible", "Creazione famiglia non riuscita" to "Impossible de créer la famille", "Cerca " to "Rechercher ", "Riepilogo annuale " to "Résumé annuel ", "⚠ Budget raggiunto/superato" to "⚠ Budget atteint/dépassé", "⚠ Budget utilizzato almeno all'80%" to "⚠ Budget utilisé à au moins 80%"),
    "es" to mapOf("Configurazione" to "Configuración","Nuova categoria" to "Nueva categoría","Nuova tipologia" to "Nuevo tipo","Nuovo membro" to "Nuevo miembro","Modifica" to "Editar","Elimina" to "Eliminar","Annulla" to "Cancelar","Crea" to "Crear","Nome" to "Nombre","Nome famiglia" to "Nombre de familia","Membri della famiglia" to "Miembros de la familia","Nessun dato" to "Sin datos","Nessun costo disponibile." to "No hay datos de costes disponibles.","Totale mensile" to "Total mensual","Errore di connessione" to "Error de conexión","Operazione non riuscita" to "Operación fallida","Verifica dell'acquisto Premium in corso…" to "Verificando la compra Premium…", "Con Premium sono disponibili:" to "Con Premium tienes:", "Sblocca Premium per test" to "Desbloquear Premium para probar", "La password deve contenere almeno 6 caratteri." to "La contraseña debe contener al menos 6 caracteres.", "Le password non coincidono." to "Las contraseñas no coinciden.", "Impossibile aggiornare la password." to "No se ha podido actualizar la contraseña.", "Impossibile inviare il link di recupero." to "No se ha podido enviar el enlace de recuperación.", "Cloud aggiornato" to "Cloud actualizado", "Caricamento completato" to "Carga completada", "Errore" to "Error", "Logout non riuscito" to "No se ha podido cerrar sesión", "Creazione famiglia non riuscita" to "No se ha podido crear la familia", "Cerca " to "Buscar ", "Riepilogo annuale " to "Resumen anual ", "⚠ Budget raggiunto/superato" to "⚠ Presupuesto alcanzado/superado", "⚠ Budget utilizzato almeno all'80%" to "⚠ Presupuesto utilizado al menos al 80%"),
    "pt" to mapOf("Configurazione" to "Configurações","Nuova categoria" to "Nova categoria","Nuova tipologia" to "Novo tipo","Nuovo membro" to "Novo membro","Modifica" to "Editar","Elimina" to "Excluir","Annulla" to "Cancelar","Crea" to "Criar","Nome" to "Nome","Nome famiglia" to "Nome da família","Membri della famiglia" to "Membros da família","Nessun dato" to "Sem dados","Nessun costo disponibile." to "Sem dados de custos disponíveis.","Totale mensile" to "Total mensal","Errore di connessione" to "Erro de conexão","Operazione non riuscita" to "Operação falhou","Verifica dell'acquisto Premium in corso…" to "Verificando a compra Premium…", "Con Premium sono disponibili:" to "Com o Premium, você tem:", "Sblocca Premium per test" to "Desbloquear Premium para testar", "La password deve contenere almeno 6 caratteri." to "A palavra-passe deve ter pelo menos 6 caracteres.", "Le password non coincidono." to "As palavras-passe não coincidem.", "Impossibile aggiornare la password." to "Não foi possível atualizar a palavra-passe.", "Impossibile inviare il link di recupero." to "Não foi possível enviar o link de recuperação.", "Cloud aggiornato" to "Cloud atualizado", "Caricamento completato" to "Carregamento concluído", "Errore" to "Erro", "Logout non riuscito" to "Não foi possível terminar sessão", "Creazione famiglia non riuscita" to "Não foi possível criar a família", "Cerca " to "Pesquisar ", "Riepilogo annuale " to "Resumo anual ", "⚠ Budget raggiunto/superato" to "⚠ Orçamento atingido/excedido", "⚠ Budget utilizzato almeno all'80%" to "⚠ Orçamento utilizado em pelo menos 80%")
)

private val mfDynamicPrefixTranslations: Map<String, Map<String, String>> = mapOf(
    "en" to mapOf("Riepilogo annuale " to "Annual summary ", "Nessun dato per il mese" to "No data for this month"),
    "de" to mapOf("Riepilogo annuale " to "Jahresübersicht ", "Nessun dato per il mese" to "Keine Daten für diesen Monat"),
    "fr" to mapOf("Riepilogo annuale " to "Résumé annuel ", "Nessun dato per il mese" to "Aucune donnée pour ce mois"),
    "es" to mapOf("Riepilogo annuale " to "Resumen anual ", "Nessun dato per il mese" to "No hay datos para este mes"),
    "pt" to mapOf("Riepilogo annuale " to "Resumo anual ", "Nessun dato per il mese" to "Sem dados para este mês")
)

private val mfPrefixTranslations: Map<String, Map<String, String>> = mapOf(
    "en" to mapOf(
        "Entrate: " to "Income: ", "Uscite: " to "Expenses: ", "Saldo: " to "Balance: ", "Ricavi: " to "Income: ", "Spese: " to "Expenses: ",
        "Operazioni: " to "Operations: ", "Membri: " to "Members: ", "Famiglia: " to "Family: ", "Account: " to "Account: ",
        "Budget: " to "Budget: ", "Effettivo: " to "Actual: ", "Errore verifica Premium: " to "Premium verification error: ",
        "Cloud aggiornato: " to "Cloud updated: ", "Caricamento completato: " to "Loading completed: ", "Errore: " to "Error: ",
        "Riepilogo " to "Summary ", "Riepilogo esercizio " to "Year summary ", "Riepilogo annuale " to "Annual summary ",
        "Totali per categoria — " to "Totals by category — ", "Composizione per categoria — " to "Category breakdown — ",
        "Totali per componente — " to "Totals by family member — ", "Totali per tipologia — " to "Totals by type — ",
        "Composizione per tipologia — " to "Type breakdown — ", "Cerca " to "Search ", "Data " to "Date ",
        "Acquista Premium · " to "Buy Premium · "
    ),
    "de" to mapOf(
        "Entrate: " to "Einnahmen: ", "Uscite: " to "Ausgaben: ", "Saldo: " to "Saldo: ", "Ricavi: " to "Einnahmen: ", "Spese: " to "Ausgaben: ",
        "Operazioni: " to "Vorgänge: ", "Membri: " to "Mitglieder: ", "Famiglia: " to "Familie: ", "Account: " to "Konto: ",
        "Budget: " to "Budget: ", "Effettivo: " to "Tatsächlich: ", "Errore verifica Premium: " to "Premium-Prüffehler: ",
        "Cloud aggiornato: " to "Cloud aktualisiert: ", "Caricamento completato: " to "Laden abgeschlossen: ", "Errore: " to "Fehler: ",
        "Riepilogo " to "Zusammenfassung ", "Riepilogo esercizio " to "Jahresübersicht ", "Riepilogo annuale " to "Jahresübersicht ",
        "Totali per categoria — " to "Summen nach Kategorie — ", "Composizione per categoria — " to "Aufschlüsselung nach Kategorie — ",
        "Totali per componente — " to "Summen nach Familienmitglied — ", "Totali per tipologia — " to "Summen nach Typ — ",
        "Composizione per tipologia — " to "Aufschlüsselung nach Typ — ", "Cerca " to "Suchen ", "Data " to "Datum ",
        "Acquista Premium · " to "Premium kaufen · "
    ),
    "fr" to mapOf(
        "Entrate: " to "Revenus : ", "Uscite: " to "Dépenses : ", "Saldo: " to "Solde : ", "Ricavi: " to "Revenus : ", "Spese: " to "Dépenses : ",
        "Operazioni: " to "Opérations : ", "Membri: " to "Membres : ", "Famiglia: " to "Famille : ", "Account: " to "Compte : ",
        "Budget: " to "Budget : ", "Effettivo: " to "Réel : ", "Errore verifica Premium: " to "Erreur de vérification Premium : ",
        "Cloud aggiornato: " to "Cloud mis à jour : ", "Caricamento completato: " to "Chargement terminé : ", "Errore: " to "Erreur : ",
        "Riepilogo " to "Résumé ", "Riepilogo esercizio " to "Résumé annuel ", "Riepilogo annuale " to "Résumé annuel ",
        "Totali per categoria — " to "Totaux par catégorie — ", "Composizione per categoria — " to "Répartition par catégorie — ",
        "Totali per componente — " to "Totaux par membre — ", "Totali per tipologia — " to "Totaux par type — ",
        "Composizione per tipologia — " to "Répartition par type — ", "Cerca " to "Rechercher ", "Data " to "Date ",
        "Acquista Premium · " to "Acheter Premium · "
    ),
    "es" to mapOf(
        "Entrate: " to "Ingresos: ", "Uscite: " to "Gastos: ", "Saldo: " to "Saldo: ", "Ricavi: " to "Ingresos: ", "Spese: " to "Gastos: ",
        "Operazioni: " to "Operaciones: ", "Membri: " to "Miembros: ", "Famiglia: " to "Familia: ", "Account: " to "Cuenta: ",
        "Budget: " to "Presupuesto: ", "Effettivo: " to "Real: ", "Errore verifica Premium: " to "Error de verificación Premium: ",
        "Cloud aggiornato: " to "Cloud actualizado: ", "Caricamento completato: " to "Carga completada: ", "Errore: " to "Error: ",
        "Riepilogo " to "Resumen ", "Riepilogo esercizio " to "Resumen anual ", "Riepilogo annuale " to "Resumen anual ",
        "Totali per categoria — " to "Totales por categoría — ", "Composizione per categoria — " to "Desglose por categoría — ",
        "Totali per componente — " to "Totales por miembro — ", "Totali per tipologia — " to "Totales por tipo — ",
        "Composizione per tipologia — " to "Desglose por tipo — ", "Cerca " to "Buscar ", "Data " to "Fecha ",
        "Acquista Premium · " to "Comprar Premium · "
    ),
    "pt" to mapOf(
        "Entrate: " to "Receitas: ", "Uscite: " to "Despesas: ", "Saldo: " to "Saldo: ", "Ricavi: " to "Receitas: ", "Spese: " to "Despesas: ",
        "Operazioni: " to "Operações: ", "Membri: " to "Membros: ", "Famiglia: " to "Família: ", "Account: " to "Conta: ",
        "Budget: " to "Orçamento: ", "Effettivo: " to "Real: ", "Errore verifica Premium: " to "Erro de verificação do Premium: ",
        "Cloud aggiornato: " to "Cloud atualizado: ", "Caricamento completato: " to "Carregamento concluído: ", "Errore: " to "Erro: ",
        "Riepilogo " to "Resumo ", "Riepilogo esercizio " to "Resumo anual ", "Riepilogo annuale " to "Resumo anual ",
        "Totali per categoria — " to "Totais por categoria — ", "Composizione per categoria — " to "Resumo por categoria — ",
        "Totali per componente — " to "Totais por membro — ", "Totali per tipologia — " to "Totais por tipo — ",
        "Composizione per tipologia — " to "Resumo por tipo — ", "Cerca " to "Pesquisar ", "Data " to "Data ",
        "Acquista Premium · " to "Comprar Premium · "
    )
)

@Composable
private val mfAdditionalTranslations: Map<String, Map<String, String>> = mapOf(
    "en" to mapOf(
        "Riepilogo" to "Summary", "totale" to "total", "Nessun dato" to "No data", "Non classificata" to "Unclassified", "Non assegnato" to "Unassigned", "Da classificare" to "To classify",
        "Mese" to "Month", "Anno" to "Year", "Modifica" to "Edit", "Elimina" to "Delete", "Cancella" to "Delete", "Annulla" to "Cancel", "Salva" to "Save", "Crea" to "Create", "Seleziona" to "Select",
        "Configurazione" to "Settings", "Tipologie" to "Types", "Nuova tipologia" to "New type", "Nuova categoria" to "New category", "Nuovo membro" to "New member", "Categorie" to "Categories",
        "Membri della famiglia" to "Family members", "Associazioni tipologia → categoria" to "Type → category mappings", "Tocca la categoria per modificarla." to "Tap the category to edit it.",
        "Elimina valore" to "Delete value", "Disattiva valore" to "Disable value", "Nome" to "Name", "Gestione anagrafiche" to "Master data management",
        "Nuova operazione" to "New operation", "Modifica operazione" to "Edit operation", "Importo (+ ricavo / - spesa)" to "Amount (+ income / - expense)", "Effettuata da" to "Performed by",
        "Cerca descrizione" to "Search description", "Operazioni esportate in Excel" to "Operations exported to Excel", "Modello Excel salvato" to "Excel template saved", "Operazioni importate" to "Operations imported",
        "Non classificata" to "Unclassified", "ATTIVO" to "ACTIVE", "Disattivato" to "Disabled", "Disattiva" to "Disable", "Attendere…" to "Please wait…",
        "Account e famiglia" to "Account and family", "Reimposta la password" to "Reset password", "Inserisci la nuova password per completare il recupero dell'account." to "Enter the new password to complete account recovery.",
        "La password deve contenere almeno 6 caratteri." to "Password must contain at least 6 characters.", "Le password non coincidono." to "Passwords do not match.",
        "Impossibile aggiornare la password." to "Unable to update password.", "Impossibile inviare il link di recupero." to "Unable to send the recovery link.",
        "Recupera password" to "Recover password", "Inserisci la tua email per ricevere il link per reimpostare la password." to "Enter your email to receive a password reset link.",
        "Se l'indirizzo è associato a un account, riceverai le istruzioni per impostare una nuova password." to "If the address is associated with an account, you will receive instructions to set a new password.",
        "Accedi allo stesso account su più dispositivi per condividere la stessa famiglia." to "Use the same account on multiple devices to share the same family.",
        "Nessuna famiglia associata a questo account." to "No family is associated with this account.", "Nome famiglia" to "Family name", "Premium: condiviso a livello famiglia" to "Premium: shared at family level",
        "Usa lo stesso account sugli altri dispositivi per accedere agli stessi dati della famiglia." to "Use the same account on other devices to access the same family data.",
        "MoneyFamily Premium" to "MoneyFamily Premium", "Budget, analisi avanzate e sincronizzazione sono sbloccati." to "Budget, advanced analysis and synchronization are unlocked.",
        "Sblocca Budget, confronto mese/anno, warning e funzioni cloud." to "Unlock Budget, month/year comparison, warnings and cloud features.",
        "Il prodotto Premium deve essere configurato su Google Play per rendere disponibile l'acquisto." to "The Premium product must be configured on Google Play to enable purchase.",
        "Acquista Premium" to "Buy Premium", "Confronto costi effettivi" to "Actual cost comparison", "Confronta due mesi specifici oppure due anni, per tipologia o categoria." to "Compare two specific months or two years, by type or category.",
        "Periodo 1" to "Period 1", "Periodo 2" to "Period 2", "Raggruppa il confronto per:" to "Group comparison by:", "TIPOLOGIA" to "TYPE", "CATEGORIA" to "CATEGORY",
        "Visualizzazione attiva: solo TIPOLOGIE" to "Active view: TYPES only", "Visualizzazione attiva: solo CATEGORIE" to "Active view: CATEGORIES only", "Nessun costo disponibile." to "No cost data available.",
        "Budget mensile per tipologia" to "Monthly budget by type", "Copia budget al mese successivo" to "Copy budget to next month", "⚠ Budget raggiunto/superato" to "⚠ Budget reached/exceeded", "⚠ Budget utilizzato almeno all'80%" to "⚠ Budget used at least 80%",
        "Composizione esercizio " to "Year breakdown ", "Inserisci" to "Add", "← Indietro" to "← Back", "← Torna a MoneyFamily" to "← Back to MoneyFamily",
        "Ripristino completato correttamente" to "Restore completed successfully", "Backup creato correttamente" to "Backup created successfully"
    ),
    "de" to mapOf(
        "Riepilogo" to "Zusammenfassung", "totale" to "gesamt", "Nessun dato" to "Keine Daten", "Non classificata" to "Nicht klassifiziert", "Non assegnato" to "Nicht zugewiesen", "Da classificare" to "Zu klassifizieren",
        "Mese" to "Monat", "Anno" to "Jahr", "Modifica" to "Bearbeiten", "Elimina" to "Löschen", "Cancella" to "Löschen", "Annulla" to "Abbrechen", "Salva" to "Speichern", "Crea" to "Erstellen", "Seleziona" to "Auswählen",
        "Configurazione" to "Einstellungen", "Nuova tipologia" to "Neue Typologie", "Nuova categoria" to "Neue Kategorie", "Nuovo membro" to "Neues Mitglied", "Categorie" to "Kategorien", "Tipologie" to "Typologien",
        "Membri della famiglia" to "Familienmitglieder", "Gestione anagrafiche" to "Stammdatenverwaltung", "Nome" to "Name", "Nome famiglia" to "Familienname", "Tocca la categoria per modificarla." to "Tippen Sie auf die Kategorie zum Bearbeiten.",
        "Nuova operazione" to "Neuer Vorgang", "Modifica operazione" to "Vorgang bearbeiten", "Effettuata da" to "Ausgeführt von", "Cerca descrizione" to "Beschreibung suchen", "ATTIVO" to "AKTIV",
        "Attendere…" to "Bitte warten…", "Account e famiglia" to "Konto und Familie", "Reimposta la password" to "Passwort zurücksetzen", "Recupera password" to "Passwort wiederherstellen",
        "La password deve contenere almeno 6 caratteri." to "Das Passwort muss mindestens 6 Zeichen enthalten.", "Le password non coincidono." to "Die Passwörter stimmen nicht überein.",
        "Impossibile aggiornare la password." to "Passwort konnte nicht aktualisiert werden.", "Impossibile inviare il link di recupero." to "Wiederherstellungslink konnte nicht gesendet werden.",
        "Nessuna famiglia associata a questo account." to "Diesem Konto ist keine Familie zugeordnet.", "Nome famiglia" to "Familienname", "Premium: condiviso a livello famiglia" to "Premium: für die Familie geteilt",
        "Confronto costi effettivi" to "Vergleich der tatsächlichen Kosten", "Periodo 1" to "Zeitraum 1", "Periodo 2" to "Zeitraum 2", "Raggruppa il confronto per:" to "Vergleich gruppieren nach:",
        "TIPOLOGIA" to "TYP", "CATEGORIA" to "KATEGORIE", "Visualizzazione attiva: solo TIPOLOGIE" to "Aktive Ansicht: nur TYPEN", "Visualizzazione attiva: solo CATEGORIE" to "Aktive Ansicht: nur KATEGORIEN",
        "Nessun costo disponibile." to "Keine Kostendaten verfügbar.", "Budget mensile per tipologia" to "Monatliches Budget nach Typ", "Copia budget al mese successivo" to "Budget in den nächsten Monat kopieren",
        "⚠ Budget raggiunto/superato" to "⚠ Budget erreicht/überschritten", "⚠ Budget utilizzato almeno all'80%" to "⚠ Budget zu mindestens 80% verwendet", "Inserisci" to "Hinzufügen", "← Indietro" to "← Zurück"
    ),
    "fr" to mapOf(
        "Riepilogo" to "Résumé", "totale" to "total", "Nessun dato" to "Aucune donnée", "Non classificata" to "Non classée", "Non assegnato" to "Non attribué", "Da classificare" to "À classer",
        "Mese" to "Mois", "Anno" to "Année", "Modifica" to "Modifier", "Elimina" to "Supprimer", "Cancella" to "Supprimer", "Annulla" to "Annuler", "Salva" to "Enregistrer", "Crea" to "Créer", "Seleziona" to "Sélectionner",
        "Configurazione" to "Paramètres", "Nuova tipologia" to "Nouveau type", "Nuova categoria" to "Nouvelle catégorie", "Nuovo membro" to "Nouveau membre", "Categorie" to "Catégories", "Tipologie" to "Types",
        "Membri della famiglia" to "Membres de la famille", "Gestione anagrafiche" to "Gestion des données de référence", "Nome" to "Nom", "Nome famiglia" to "Nom de famille",
        "Nuova operazione" to "Nouvelle opération", "Modifica operazione" to "Modifier l’opération", "Effettuata da" to "Effectuée par", "Cerca descrizione" to "Rechercher une description", "ATTIVO" to "ACTIF",
        "Attendere…" to "Veuillez patienter…", "Account e famiglia" to "Compte et famille", "Reimposta la password" to "Réinitialiser le mot de passe", "Recupera password" to "Récupérer le mot de passe",
        "La password deve contenere almeno 6 caratteri." to "Le mot de passe doit contenir au moins 6 caractères.", "Le password non coincidono." to "Les mots de passe ne correspondent pas.",
        "Impossibile aggiornare la password." to "Impossible de mettre à jour le mot de passe.", "Impossibile inviare il link di recupero." to "Impossible d’envoyer le lien de récupération.",
        "Nessuna famiglia associata a questo account." to "Aucune famille n’est associée à ce compte.", "Premium: condiviso a livello famiglia" to "Premium : partagé au niveau de la famille",
        "Confronto costi effettivi" to "Comparaison des coûts réels", "Periodo 1" to "Période 1", "Periodo 2" to "Période 2", "Raggruppa il confronto per:" to "Regrouper la comparaison par :",
        "TIPOLOGIA" to "TYPE", "CATEGORIA" to "CATÉGORIE", "Visualizzazione attiva: solo TIPOLOGIE" to "Vue active : TYPES uniquement", "Visualizzazione attiva: solo CATEGORIE" to "Vue active : CATÉGORIES uniquement",
        "Nessun costo disponibile." to "Aucune donnée de coût disponible.", "Budget mensile per tipologia" to "Budget mensuel par type", "Copia budget al mese successivo" to "Copier le budget au mois suivant",
        "⚠ Budget raggiunto/superato" to "⚠ Budget atteint/dépassé", "⚠ Budget utilizzato almeno all'80%" to "⚠ Budget utilisé à au moins 80 %", "Inserisci" to "Ajouter", "← Indietro" to "← Retour"
    ),
    "es" to mapOf(
        "Riepilogo" to "Resumen", "totale" to "total", "Nessun dato" to "Sin datos", "Non classificata" to "Sin clasificar", "Non assegnato" to "Sin asignar", "Da classificare" to "Por clasificar",
        "Mese" to "Mes", "Anno" to "Año", "Modifica" to "Editar", "Elimina" to "Eliminar", "Cancella" to "Eliminar", "Annulla" to "Cancelar", "Salva" to "Guardar", "Crea" to "Crear", "Seleziona" to "Seleccionar",
        "Configurazione" to "Configuración", "Nuova tipologia" to "Nuevo tipo", "Nuova categoria" to "Nueva categoría", "Nuovo membro" to "Nuevo miembro", "Categorie" to "Categorías", "Tipologie" to "Tipos",
        "Membri della famiglia" to "Miembros de la familia", "Gestione anagrafiche" to "Gestión de datos maestros", "Nome" to "Nombre", "Nome famiglia" to "Nombre de familia",
        "Nuova operazione" to "Nueva operación", "Modifica operazione" to "Editar operación", "Effettuata da" to "Realizada por", "Cerca descrizione" to "Buscar descripción", "ATTIVO" to "ACTIVO",
        "Attendere…" to "Espere…", "Account e famiglia" to "Cuenta y familia", "Reimposta la password" to "Restablecer contraseña", "Recupera password" to "Recuperar contraseña",
        "La password deve contenere almeno 6 caratteri." to "La contraseña debe contener al menos 6 caracteres.", "Le password non coincidono." to "Las contraseñas no coinciden.",
        "Impossibile aggiornare la password." to "No se pudo actualizar la contraseña.", "Impossibile inviare il link di recupero." to "No se pudo enviar el enlace de recuperación.",
        "Nessuna famiglia associata a questo account." to "No hay ninguna familia asociada a esta cuenta.", "Premium: condiviso a livello famiglia" to "Premium: compartido a nivel familiar",
        "Confronto costi effettivi" to "Comparación de costes reales", "Periodo 1" to "Período 1", "Periodo 2" to "Período 2", "Raggruppa il confronto per:" to "Agrupar comparación por:",
        "TIPOLOGIA" to "TIPO", "CATEGORIA" to "CATEGORÍA", "Visualizzazione attiva: solo TIPOLOGIE" to "Vista activa: solo TIPOS", "Visualizzazione attiva: solo CATEGORIE" to "Vista activa: solo CATEGORÍAS",
        "Nessun costo disponibile." to "No hay datos de costes disponibles.", "Budget mensile per tipologia" to "Presupuesto mensual por tipo", "Copia budget al mese successivo" to "Copiar presupuesto al mes siguiente",
        "⚠ Budget raggiunto/superato" to "⚠ Presupuesto alcanzado/superado", "⚠ Budget utilizzato almeno all'80%" to "⚠ Presupuesto utilizado al menos al 80%", "Inserisci" to "Añadir", "← Indietro" to "← Atrás"
    ),
    "pt" to mapOf(
        "Riepilogo" to "Resumo", "totale" to "total", "Nessun dato" to "Sem dados", "Non classificata" to "Não classificada", "Non assegnato" to "Não atribuído", "Da classificare" to "Por classificar",
        "Mese" to "Mês", "Anno" to "Ano", "Modifica" to "Editar", "Elimina" to "Eliminar", "Cancella" to "Eliminar", "Annulla" to "Cancelar", "Salva" to "Guardar", "Crea" to "Criar", "Seleziona" to "Selecionar",
        "Configurazione" to "Definições", "Nuova tipologia" to "Novo tipo", "Nuova categoria" to "Nova categoria", "Nuovo membro" to "Novo membro", "Categorie" to "Categorias", "Tipologie" to "Tipos",
        "Membri della famiglia" to "Membros da família", "Gestione anagrafiche" to "Gestão de dados mestre", "Nome" to "Nome", "Nome famiglia" to "Nome da família",
        "Nuova operazione" to "Nova operação", "Modifica operazione" to "Editar operação", "Effettuata da" to "Realizado por", "Cerca descrizione" to "Pesquisar descrição", "ATTIVO" to "ATIVO",
        "Attendere…" to "Aguarde…", "Account e famiglia" to "Conta e família", "Reimposta la password" to "Repor palavra-passe", "Recupera password" to "Recuperar palavra-passe",
        "La password deve contenere almeno 6 caratteri." to "A palavra-passe deve ter pelo menos 6 caracteres.", "Le password non coincidono." to "As palavras-passe não coincidem.",
        "Impossibile aggiornare la password." to "Não foi possível atualizar a palavra-passe.", "Impossibile inviare il link di recupero." to "Não foi possível enviar a ligação de recuperação.",
        "Nessuna famiglia associata a questo account." to "Não existe uma família associada a esta conta.", "Premium: condiviso a livello famiglia" to "Premium: partilhado ao nível da família",
        "Confronto costi effettivi" to "Comparação dos custos reais", "Periodo 1" to "Período 1", "Periodo 2" to "Período 2", "Raggruppa il confronto per:" to "Agrupar comparação por:",
        "TIPOLOGIA" to "TIPO", "CATEGORIA" to "CATEGORIA", "Visualizzazione attiva: solo TIPOLOGIE" to "Vista ativa: apenas TIPOS", "Visualizzazione attiva: solo CATEGORIE" to "Vista ativa: apenas CATEGORIAS",
        "Nessun costo disponibile." to "Sem dados de custos disponíveis.", "Budget mensile per tipologia" to "Orçamento mensal por tipo", "Copia budget al mese successivo" to "Copiar orçamento para o mês seguinte",
        "⚠ Budget raggiunto/superato" to "⚠ Orçamento atingido/excedido", "⚠ Budget utilizzato almeno all'80%" to "⚠ Orçamento utilizado pelo menos a 80%", "Inserisci" to "Adicionar", "← Indietro" to "← Voltar"
    )
)

private fun localizeMoneyFamily(text: String): String {
    val lang = Locale.getDefault().language.lowercase(Locale.ROOT)
    if (lang != "it") {
        val resourceId = mfResourceIds[text]
        if (resourceId != null) return androidx.compose.ui.res.stringResource(resourceId)
    }
    var result = text
    (mfAdditionalTranslations[lang].orEmpty() + mfExtraTranslations[lang].orEmpty() + mfPrefixTranslations[lang].orEmpty() + mfDynamicPrefixTranslations[lang].orEmpty()).entries.sortedByDescending { it.key.length }.forEach { (from, to) ->
        if (result.startsWith(from)) result = to + result.removePrefix(from)
    }
    return result
}

/*
 * Compatibility wrapper used during the localization migration.
 * It changes only presentation text; it does not alter app state, database,
 * Premium, Cloud sync, Excel, or any business logic.
 */
@Composable
fun Text(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    style: TextStyle = LocalTextStyle.current
) {
    androidx.compose.material3.Text(
        text = localizeMoneyFamily(text),
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        fontStyle = fontStyle,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        letterSpacing = letterSpacing,
        textDecoration = textDecoration,
        textAlign = textAlign,
        lineHeight = lineHeight,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines,
        onTextLayout = onTextLayout,
        style = style
    )
}
