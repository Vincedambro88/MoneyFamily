from pathlib import Path

main = Path('app/src/main/java/com/moneyfamily/app/MainActivity.kt')
s = main.read_text()
s=s.replace('Text("Colonne: Data | Importo | Tipologia | Categoria | Componente | Descrizione",style=MaterialTheme.typography.bodyMedium)', 'Text("Excel obbligatorio: Data | Importo | Tipologia | Categoria | Componente | Descrizione. Tutti i campi devono essere compilati.",style=MaterialTheme.typography.bodyMedium)')
main.write_text(s)
