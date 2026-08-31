from pathlib import Path

main = Path('app/src/main/java/com/moneyfamily/app/MainActivity.kt')
s = main.read_text()

replacements = {
    'nonZero.take(10).forEachIndexed{index,entry->': 'nonZero.forEachIndexed{index,entry->',
    'values.take(12).forEach{(n,v)->BarChartRow(n,v,maxAbs)}': 'values.forEach{(n,v)->BarChartRow(n,v,maxAbs)}',
}

changed = False
for old, new in replacements.items():
    if old in s:
        s = s.replace(old, new)
        changed = True

if not changed:
    raise SystemExit('Summary display limits not found; refusing to modify MainActivity.kt')

main.write_text(s)
print('Updated summary lists: all moved categories and types are now displayed.')
