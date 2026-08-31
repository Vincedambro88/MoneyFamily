from pathlib import Path

# Build-time source patching operates only on the checked-out commit.
# Do not checkout a historical SHA because Actions uses a shallow clone.
main = Path('app/src/main/java/com/moneyfamily/app/MainActivity.kt')
s = main.read_text()

# Stable month labels: set the day to 1 before changing Calendar.MONTH.
s = s.replace(
    'Calendar.getInstance().apply{set(Calendar.MONTH,m)}.time',
    'Calendar.getInstance().apply{set(Calendar.DAY_OF_MONTH,1);set(Calendar.MONTH,m)}.time'
)
main.write_text(s)

# Add the downloadable Excel template to the Insert screen.
exec(compile(Path('tools/add_excel_template.py').read_text(), 'tools/add_excel_template.py', 'exec'))
print('Fixed annual month labels and added Excel template download')
