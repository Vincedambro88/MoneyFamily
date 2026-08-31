from pathlib import Path

main = Path('app/src/main/java/com/moneyfamily/app/MainActivity.kt')
s = main.read_text()

old = 'SimpleDateFormat("MMM",Locale.ITALIAN).format(Calendar.getInstance().apply{set(Calendar.MONTH,m)}.time)'
new = 'SimpleDateFormat("MMM",Locale.ITALIAN).format(Calendar.getInstance().apply{set(Calendar.DAY_OF_MONTH,1);set(Calendar.MONTH,m)}.time)'

if old in s:
    s = s.replace(old, new, 1)
    main.write_text(s)
    print('Fixed annual month labels')
else:
    print('Annual month label fix already applied; nothing to do')
