from pathlib import Path

p = Path('app/src/main/java/com/moneyfamily/app/MainActivity.kt')
s = p.read_text()
old = 'Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp)){'
new = 'Column(Modifier.weight(1f).height(170.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)){'
if old not in s:
    raise SystemExit('Pie legend container not found; refusing to modify MainActivity.kt')
s = s.replace(old, new, 2)
p.write_text(s)
print('Pie legends made vertically scrollable in monthly and annual summaries.')
