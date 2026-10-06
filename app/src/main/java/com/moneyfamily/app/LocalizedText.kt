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

private val mfTranslations: Map<String, Map<String, String>> = mapOf(
    "en" to mapOf(
        "Dashboard" to "Dashboard", "Operazioni" to "Operations", "Inserisci" to "Add", "Impostazioni" to "Settings", "Budget" to "Budget", "Premium" to "Premium",
        "Entrate" to "Income", "Uscite" to "Expenses", "Saldo" to "Balance", "Composizione mensile" to "Monthly breakdown",
        "Totali per categoria" to "Totals by category", "Composizione per categoria" to "Category breakdown", "Totali per componente" to "Totals by family member",
        "Totali per tipologia" to "Totals by type", "Composizione per tipologia" to "Type breakdown", "Riepilogo" to "Summary",
        "Spese" to "Expenses", "Ricavi" to "Income", "Analisi" to "Analysis", "Storico reale di entrate e uscite" to "Actual income and expense history",
        "Andamento mensile" to "Monthly trend", "Spese per categoria" to "Expenses by category", "Analisi per membro" to "Analysis by family member",
        "Anno" to "Year", "Mese" to "Month", "Anno / Anno" to "Year / Year", "Mese / Mese" to "Month / Month",
        "Periodo 1" to "Period 1", "Periodo 2" to "Period 2", "Categoria" to "Category", "Categorie" to "Categories",
        "Tipologia" to "Type", "Tipologie" to "Types", "Descrizione" to "Description", "Importo (+ ricavo / - spesa)" to "Amount (+ income / - expense)",
        "Effettuata da" to "Performed by", "Componente" to "Family member", "Famiglia" to "Family", "Membri della famiglia" to "Family members",
        "Associazioni" to "Mappings", "Associazioni tipologia → categoria" to "Type → category mappings", "Gestione anagrafiche" to "Manage lists",
        "+ Nuova operazione" to "+ New operation", "+ Inserisci operazione" to "+ Add operation", "+ Aggiungi categoria" to "+ Add category",
        "+ Aggiungi membro" to "+ Add member", "+ Aggiungi tipologia" to "+ Add type", "+ Crea famiglia" to "+ Create family",
        "Nuova operazione" to "New operation", "Modifica operazione" to "Edit operation", "Modifica" to "Edit", "Elimina" to "Delete",
        "Cancella" to "Delete", "Salva" to "Save", "Annulla" to "Cancel", "Crea" to "Create", "Seleziona" to "Select",
        "Tutte" to "All", "Tutti" to "All", "Cerca descrizione" to "Search description", "Scarica in Excel" to "Export to Excel",
        "Scarica modello Excel" to "Download Excel template", "Importa da Excel" to "Import from Excel", "Operazioni esportate in Excel" to "Operations exported to Excel",
        "Modello Excel salvato" to "Excel template saved", "Operazioni importate" to "Operations imported", "Nessun dato" to "No data",
        "Nessun dato per il mese" to "No data for this month", "Nessun valore" to "No value", "Non classificata" to "Unclassified", "Non assegnato" to "Unassigned",
        "Da classificare" to "To be classified", "ATTIVO" to "ACTIVE", "Disattivato" to "Disabled", "Disattiva" to "Disable", "Attendere…" to "Please wait…",
        "Accedi" to "Sign in", "Registrati" to "Sign up", "Crea account" to "Create account", "Email" to "Email", "Password" to "Password",
        "Password dimenticata?" to "Forgot password?", "Recupera password" to "Recover password", "Reimposta la password" to "Reset password",
        "Nuova password" to "New password", "Conferma nuova password" to "Confirm new password", "Aggiorna password" to "Update password",
        "Invia link di recupero" to "Send recovery link", "Torna ad Accedi" to "Back to sign in", "Esci da questo dispositivo" to "Sign out on this device",
        "Account MoneyFamily" to "MoneyFamily account", "Account e famiglia" to "Account and family", "Account autenticato. Il Cloud sarà utilizzato quando Premium sarà attivo." to "Account authenticated. Cloud will be available when Premium is active.",
        "Cloud attivo. Lo stesso account può essere utilizzato su più dispositivi." to "Cloud active. The same account can be used on multiple devices.",
        "Account Cloud: dopo la registrazione verrai autenticato automaticamente e i dati Premium saranno sincronizzati tra i dispositivi." to "Cloud account: after registration you will be signed in automatically and Premium data will be synchronized across devices.",
        "I dati restano sul dispositivo e vengono inviati al Cloud solo quando lo richiedi." to "Data stays on the device and is sent to the Cloud only when you request it.",
        "☁️ Salva sul Cloud" to "☁️ Save to Cloud", "☁️ Carica dal Cloud" to "☁️ Load from Cloud", "Salvataggio…" to "Saving…", "Caricamento…" to "Loading…",
        "Verifica Premium" to "Verify Premium", "Premium attivo per questo account." to "Premium is active for this account.",
        "Confronto costi effettivi" to "Actual cost comparison", "Budget mensile per tipologia" to "Monthly budget by type",
        "Confronto Budget / Effettivo — Categoria" to "Budget / Actual comparison — Category", "Confronto Budget / Effettivo — Tipologia" to "Budget / Actual comparison — Type",
        "Copia budget al mese successivo" to "Copy budget to next month", "Raggruppa il confronto per:" to "Group comparison by:",
        "Visualizzazione attiva: solo CATEGORIE" to "Active view: CATEGORIES only", "Visualizzazione attiva: solo TIPOLOGIE" to "Active view: TYPES only",
        "Backup" to "Backup", "Backup e ripristino" to "Backup and restore", "Crea backup" to "Create backup", "Ripristino" to "Restore",
        "Ripristina backup" to "Restore backup", "Ripristina" to "Restore", "Backup creato correttamente" to "Backup created successfully",
        "Ripristino completato correttamente" to "Restore completed successfully", "← Torna a MoneyFamily" to "← Back to MoneyFamily",
        "Impostazioni" to "Settings", "Tipologie" to "Types", "Categorie" to "Categories", "Membri" to "Members",
        "MoneyFamily Premium" to "MoneyFamily Premium"
    ),
    "de" to mapOf(
        "Dashboard" to "Dashboard", "Operazioni" to "Vorgänge", "Inserisci" to "Hinzufügen", "Impostazioni" to "Einstellungen", "Budget" to "Budget", "Premium" to "Premium",
        "Entrate" to "Einnahmen", "Uscite" to "Ausgaben", "Saldo" to "Saldo", "Composizione mensile" to "Monatliche Aufschlüsselung",
        "Totali per categoria" to "Summen nach Kategorie", "Composizione per categoria" to "Aufschlüsselung nach Kategorie", "Totali per componente" to "Summen nach Familienmitglied",
        "Totali per tipologia" to "Summen nach Typ", "Composizione per tipologia" to "Aufschlüsselung nach Typ", "Riepilogo" to "Zusammenfassung",
        "Spese" to "Ausgaben", "Ricavi" to "Einnahmen", "Analisi" to "Analyse", "Andamento mensile" to "Monatlicher Verlauf",
        "Spese per categoria" to "Ausgaben nach Kategorie", "Analisi per membro" to "Analyse nach Familienmitglied", "Anno" to "Jahr", "Mese" to "Monat",
        "Categoria" to "Kategorie", "Categorie" to "Kategorien", "Tipologia" to "Typ", "Tipologie" to "Typen", "Descrizione" to "Beschreibung",
        "Importo (+ ricavo / - spesa)" to "Betrag (+ Einnahme / - Ausgabe)", "Effettuata da" to "Ausgeführt von", "Componente" to "Familienmitglied",
        "Famiglia" to "Familie", "Membri della famiglia" to "Familienmitglieder", "Associazioni" to "Zuordnungen", "Nuova operazione" to "Neuer Vorgang",
        "Modifica operazione" to "Vorgang bearbeiten", "Modifica" to "Bearbeiten", "Elimina" to "Löschen", "Cancella" to "Löschen", "Salva" to "Speichern",
        "Annulla" to "Abbrechen", "Crea" to "Erstellen", "Accedi" to "Anmelden", "Registrati" to "Registrieren", "Crea account" to "Konto erstellen",
        "Email" to "E-Mail", "Password" to "Passwort", "Password dimenticata?" to "Passwort vergessen?", "Recupera password" to "Passwort wiederherstellen",
        "Nuova password" to "Neues Passwort", "Conferma nuova password" to "Neues Passwort bestätigen", "Aggiorna password" to "Passwort aktualisieren",
        "Invia link di recupero" to "Wiederherstellungslink senden", "Torna ad Accedi" to "Zur Anmeldung", "Esci da questo dispositivo" to "Von diesem Gerät abmelden",
        "Account MoneyFamily" to "MoneyFamily-Konto", "Cloud attivo. Lo stesso account può essere utilizzato su più dispositivi." to "Cloud aktiv. Dasselbe Konto kann auf mehreren Geräten verwendet werden.",
        "☁️ Salva sul Cloud" to "☁️ In Cloud speichern", "☁️ Carica dal Cloud" to "☁️ Aus Cloud laden", "Salvataggio…" to "Speichern…", "Caricamento…" to "Laden…",
        "Verifica Premium" to "Premium prüfen", "Premium attivo per questo account." to "Premium ist für dieses Konto aktiv.", "Confronto costi effettivi" to "Vergleich der tatsächlichen Kosten",
        "Budget mensile per tipologia" to "Monatliches Budget nach Typ", "Backup e ripristino" to "Sicherung und Wiederherstellung", "Crea backup" to "Backup erstellen",
        "Ripristino" to "Wiederherstellung", "Ripristina backup" to "Backup wiederherstellen", "Ripristina" to "Wiederherstellen", "Annulla" to "Abbrechen"
    ),
    "fr" to mapOf(
        "Dashboard" to "Tableau de bord", "Operazioni" to "Opérations", "Inserisci" to "Ajouter", "Impostazioni" to "Paramètres", "Budget" to "Budget", "Premium" to "Premium",
        "Entrate" to "Revenus", "Uscite" to "Dépenses", "Saldo" to "Solde", "Composizione mensile" to "Répartition mensuelle",
        "Totali per categoria" to "Totaux par catégorie", "Composizione per categoria" to "Répartition par catégorie", "Totali per componente" to "Totaux par membre",
        "Totali per tipologia" to "Totaux par type", "Composizione per tipologia" to "Répartition par type", "Riepilogo" to "Résumé",
        "Spese" to "Dépenses", "Ricavi" to "Revenus", "Analisi" to "Analyse", "Andamento mensile" to "Évolution mensuelle",
        "Spese per categoria" to "Dépenses par catégorie", "Analisi per membro" to "Analyse par membre", "Anno" to "Année", "Mese" to "Mois",
        "Categoria" to "Catégorie", "Categorie" to "Catégories", "Tipologia" to "Type", "Tipologie" to "Types", "Descrizione" to "Description",
        "Importo (+ ricavo / - spesa)" to "Montant (+ revenu / - dépense)", "Effettuata da" to "Effectué par", "Componente" to "Membre de la famille",
        "Famiglia" to "Famille", "Membri della famiglia" to "Membres de la famille", "Associazioni" to "Associations", "Nuova operazione" to "Nouvelle opération",
        "Modifica operazione" to "Modifier l’opération", "Modifica" to "Modifier", "Elimina" to "Supprimer", "Cancella" to "Supprimer", "Salva" to "Enregistrer",
        "Annulla" to "Annuler", "Crea" to "Créer", "Accedi" to "Se connecter", "Registrati" to "S’inscrire", "Crea account" to "Créer un compte",
        "Email" to "E-mail", "Password" to "Mot de passe", "Password dimenticata?" to "Mot de passe oublié ?", "Recupera password" to "Récupérer le mot de passe",
        "Nuova password" to "Nouveau mot de passe", "Conferma nuova password" to "Confirmer le nouveau mot de passe", "Aggiorna password" to "Mettre à jour le mot de passe",
        "Invia link di recupero" to "Envoyer le lien de récupération", "Torna ad Accedi" to "Retour à la connexion", "Esci da questo dispositivo" to "Se déconnecter de cet appareil",
        "Account MoneyFamily" to "Compte MoneyFamily", "Cloud attivo. Lo stesso account può essere utilizzato su più dispositivi." to "Cloud actif. Le même compte peut être utilisé sur plusieurs appareils.",
        "☁️ Salva sul Cloud" to "☁️ Enregistrer dans le Cloud", "☁️ Carica dal Cloud" to "☁️ Charger depuis le Cloud", "Salvataggio…" to "Enregistrement…", "Caricamento…" to "Chargement…",
        "Verifica Premium" to "Vérifier Premium", "Premium attivo per questo account." to "Premium est actif pour ce compte.", "Confronto costi effettivi" to "Comparaison des coûts réels",
        "Budget mensile per tipologia" to "Budget mensuel par type", "Backup e ripristino" to "Sauvegarde et restauration", "Crea backup" to "Créer une sauvegarde",
        "Ripristino" to "Restauration", "Ripristina backup" to "Restaurer la sauvegarde", "Ripristina" to "Restaurer", "Annulla" to "Annuler"
    ),
    "es" to mapOf(
        "Dashboard" to "Panel", "Operazioni" to "Operaciones", "Inserisci" to "Añadir", "Impostazioni" to "Ajustes", "Budget" to "Presupuesto", "Premium" to "Premium",
        "Entrate" to "Ingresos", "Uscite" to "Gastos", "Saldo" to "Saldo", "Composizione mensile" to "Desglose mensual",
        "Totali per categoria" to "Totales por categoría", "Composizione per categoria" to "Desglose por categoría", "Totali per componente" to "Totales por miembro",
        "Totali per tipologia" to "Totales por tipo", "Composizione per tipologia" to "Desglose por tipo", "Riepilogo" to "Resumen",
        "Spese" to "Gastos", "Ricavi" to "Ingresos", "Analisi" to "Análisis", "Andamento mensile" to "Evolución mensual",
        "Spese per categoria" to "Gastos por categoría", "Analisi per membro" to "Análisis por miembro", "Anno" to "Año", "Mese" to "Mes",
        "Categoria" to "Categoría", "Categorie" to "Categorías", "Tipologia" to "Tipo", "Tipologie" to "Tipos", "Descrizione" to "Descripción",
        "Importo (+ ricavo / - spesa)" to "Importe (+ ingreso / - gasto)", "Effettuata da" to "Realizado por", "Componente" to "Miembro de la familia",
        "Famiglia" to "Familia", "Membri della famiglia" to "Miembros de la familia", "Associazioni" to "Asociaciones", "Nuova operazione" to "Nueva operación",
        "Modifica operazione" to "Editar operación", "Modifica" to "Editar", "Elimina" to "Eliminar", "Cancella" to "Eliminar", "Salva" to "Guardar",
        "Annulla" to "Cancelar", "Crea" to "Crear", "Accedi" to "Iniciar sesión", "Registrati" to "Registrarse", "Crea account" to "Crear cuenta",
        "Email" to "Correo electrónico", "Password" to "Contraseña", "Password dimenticata?" to "¿Olvidaste la contraseña?", "Recupera password" to "Recuperar contraseña",
        "Nuova password" to "Nueva contraseña", "Conferma nuova password" to "Confirmar nueva contraseña", "Aggiorna password" to "Actualizar contraseña",
        "Invia link di recupero" to "Enviar enlace de recuperación", "Torna ad Accedi" to "Volver a iniciar sesión", "Esci da questo dispositivo" to "Cerrar sesión en este dispositivo",
        "Account MoneyFamily" to "Cuenta MoneyFamily", "Cloud attivo. Lo stesso account può essere utilizzato su più dispositivi." to "Cloud activo. La misma cuenta puede usarse en varios dispositivos.",
        "☁️ Salva sul Cloud" to "☁️ Guardar en Cloud", "☁️ Carica dal Cloud" to "☁️ Cargar desde Cloud", "Salvataggio…" to "Guardando…", "Caricamento…" to "Cargando…",
        "Verifica Premium" to "Verificar Premium", "Premium attivo per questo account." to "Premium está activo para esta cuenta.", "Confronto costi effettivi" to "Comparación de costes reales",
        "Budget mensile per tipologia" to "Presupuesto mensual por tipo", "Backup e ripristino" to "Copia de seguridad y restauración", "Crea backup" to "Crear copia de seguridad",
        "Ripristino" to "Restauración", "Ripristina backup" to "Restaurar copia de seguridad", "Ripristina" to "Restaurar"
    ),
    "pt" to mapOf(
        "Dashboard" to "Painel", "Operazioni" to "Operações", "Inserisci" to "Adicionar", "Impostazioni" to "Definições", "Budget" to "Orçamento", "Premium" to "Premium",
        "Entrate" to "Receitas", "Uscite" to "Despesas", "Saldo" to "Saldo", "Composizione mensile" to "Resumo mensal",
        "Totali per categoria" to "Totais por categoria", "Composizione per categoria" to "Resumo por categoria", "Totali per componente" to "Totais por membro",
        "Totali per tipologia" to "Totais por tipo", "Composizione per tipologia" to "Resumo por tipo", "Riepilogo" to "Resumo",
        "Spese" to "Despesas", "Ricavi" to "Receitas", "Analisi" to "Análise", "Andamento mensile" to "Evolução mensal",
        "Spese per categoria" to "Despesas por categoria", "Analisi per membro" to "Análise por membro", "Anno" to "Ano", "Mese" to "Mês",
        "Categoria" to "Categoria", "Categorie" to "Categorias", "Tipologia" to "Tipo", "Tipologie" to "Tipos", "Descrizione" to "Descrição",
        "Importo (+ ricavo / - spesa)" to "Valor (+ receita / - despesa)", "Effettuata da" to "Realizado por", "Componente" to "Membro da família",
        "Famiglia" to "Família", "Membri della famiglia" to "Membros da família", "Associazioni" to "Associações", "Nuova operazione" to "Nova operação",
        "Modifica operazione" to "Editar operação", "Modifica" to "Editar", "Elimina" to "Eliminar", "Cancella" to "Eliminar", "Salva" to "Guardar",
        "Annulla" to "Cancelar", "Crea" to "Criar", "Accedi" to "Iniciar sessão", "Registrati" to "Registar", "Crea account" to "Criar conta",
        "Email" to "E-mail", "Password" to "Palavra-passe", "Password dimenticata?" to "Esqueceu-se da palavra-passe?", "Recupera password" to "Recuperar palavra-passe",
        "Nuova password" to "Nova palavra-passe", "Conferma nuova password" to "Confirmar nova palavra-passe", "Aggiorna password" to "Atualizar palavra-passe",
        "Invia link di recupero" to "Enviar ligação de recuperação", "Torna ad Accedi" to "Voltar ao início de sessão", "Esci da questo dispositivo" to "Terminar sessão neste dispositivo",
        "Account MoneyFamily" to "Conta MoneyFamily", "Cloud attivo. Lo stesso account può essere utilizzato su più dispositivi." to "Cloud ativo. A mesma conta pode ser usada em vários dispositivos.",
        "☁️ Salva sul Cloud" to "☁️ Guardar na Cloud", "☁️ Carica dal Cloud" to "☁️ Carregar da Cloud", "Salvataggio…" to "A guardar…", "Caricamento…" to "A carregar…",
        "Verifica Premium" to "Verificar Premium", "Premium attivo per questo account." to "Premium está ativo para esta conta.", "Confronto costi effettivi" to "Comparação dos custos reais",
        "Budget mensile per tipologia" to "Orçamento mensal por tipo", "Backup e ripristino" to "Cópia de segurança e restauro", "Crea backup" to "Criar cópia de segurança",
        "Ripristino" to "Restauro", "Ripristina backup" to "Restaurar cópia de segurança", "Ripristina" to "Restaurar"
    )
)

private val mfPrefixTranslations: Map<String, Map<String, String>> = mapOf(
    "en" to mapOf(
        "Entrate: " to "Income: ", "Uscite: " to "Expenses: ", "Saldo: " to "Balance: ", "Ricavi: " to "Income: ", "Spese: " to "Expenses: ",
        "Operazioni: " to "Operations: ", "Membri: " to "Members: ", "Famiglia: " to "Family: ", "Account: " to "Account: ",
        "Budget: " to "Budget: ", "Effettivo: " to "Actual: ", "Errore verifica Premium: " to "Premium verification error: ",
        "Cloud aggiornato: " to "Cloud updated: ", "Caricamento completato: " to "Loading completed: ", "Errore: " to "Error: ",
        "Riepilogo " to "Summary ", "Riepilogo esercizio " to "Year summary ", "Totali per categoria — " to "Totals by category — ",
        "Composizione per categoria — " to "Category breakdown — ", "Totali per componente — " to "Totals by family member — ",
        "Totali per tipologia — " to "Totals by type — ", "Composizione per tipologia — " to "Type breakdown — ",
        "Nessun dato per il mese" to "No data for this month", "Cerca " to "Search "
    ),
    "de" to mapOf("Entrate: " to "Einnahmen: ", "Uscite: " to "Ausgaben: ", "Saldo: " to "Saldo: ", "Ricavi: " to "Einnahmen: ", "Spese: " to "Ausgaben: ", "Operazioni: " to "Vorgänge: ", "Account: " to "Konto: ", "Budget: " to "Budget: ", "Effettivo: " to "Tatsächlich: ", "Riepilogo " to "Zusammenfassung "),
    "fr" to mapOf("Entrate: " to "Revenus : ", "Uscite: " to "Dépenses : ", "Saldo: " to "Solde : ", "Ricavi: " to "Revenus : ", "Spese: " to "Dépenses : ", "Operazioni: " to "Opérations : ", "Account: " to "Compte : ", "Budget: " to "Budget : ", "Effettivo: " to "Réel : ", "Riepilogo " to "Résumé "),
    "es" to mapOf("Entrate: " to "Ingresos: ", "Uscite: " to "Gastos: ", "Saldo: " to "Saldo: ", "Ricavi: " to "Ingresos: ", "Spese: " to "Gastos: ", "Operazioni: " to "Operaciones: ", "Account: " to "Cuenta: ", "Budget: " to "Presupuesto: ", "Effettivo: " to "Real: ", "Riepilogo " to "Resumen "),
    "pt" to mapOf("Entrate: " to "Receitas: ", "Uscite: " to "Despesas: ", "Saldo: " to "Saldo: ", "Ricavi: " to "Receitas: ", "Spese: " to "Despesas: ", "Operazioni: " to "Operações: ", "Account: " to "Conta: ", "Budget: " to "Orçamento: ", "Effettivo: " to "Real: ", "Riepilogo " to "Resumo ")
)

private fun localizeMoneyFamily(text: String): String {
    val lang = Locale.getDefault().language.lowercase(Locale.ROOT)
    if (lang == "it") return text
    val exact = mfTranslations[lang]?.get(text)
    if (exact != null) return exact
    var result = text
    mfPrefixTranslations[lang].orEmpty().entries.sortedByDescending { it.key.length }.forEach { (from, to) ->
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
