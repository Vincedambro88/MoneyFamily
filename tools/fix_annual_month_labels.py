from pathlib import Path

main = Path('app/src/main/java/com/moneyfamily/app/MainActivity.kt')
s = main.read_text()

old = 'SimpleDateFormat("MMM",Locale.ITALIAN).format(Calendar.getInstance().apply{set(Calendar.MONTH,m)}.time)'
new = 'SimpleDateFormat("MMM",Locale.ITALIAN).format(Calendar.getInstance().apply{set(Calendar.DAY_OF_MONTH,1);set(Calendar.MONTH,m)}.time)'

if old not in s:
    raise SystemExit('Annual month label expression not found')

s = s.replace(old, new, 1)
main.write_text(s)
print('Fixed annual month labels: day-of-month is reset before changing month.')
