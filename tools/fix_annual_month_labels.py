from pathlib import Path
import subprocess

# Always start from the last complete application source. The previous automated
# patch accidentally truncated MainActivity.kt, so do not build from that state.
main = Path('app/src/main/java/com/moneyfamily/app/MainActivity.kt')
subprocess.run([
    'git', 'checkout', '887e8741442f3ab701899bfa00fddd41527208e3', '--', str(main)
], check=True)

s = main.read_text()

# Remove duplicate ActivityResultContracts imports from the restored source.
duplicate = (
    'import androidx.activity.result.contract.ActivityResultContracts\n'
    'import androidx.activity.compose.rememberLauncherForActivityResult\n'
    'import androidx.activity.result.contract.ActivityResultContracts\n'
)
s = s.replace(duplicate,
              'import androidx.activity.result.contract.ActivityResultContracts\n'
              'import androidx.activity.compose.rememberLauncherForActivityResult\n', 1)

# Stable month labels: set the day to 1 before changing Calendar.MONTH.
s = s.replace(
    'Calendar.getInstance().apply{set(Calendar.MONTH,m)}.time',
    'Calendar.getInstance().apply{set(Calendar.DAY_OF_MONTH,1);set(Calendar.MONTH,m)}.time'
)
main.write_text(s)

# Add the downloadable Excel template to the Insert screen.
subprocess.run(['python3', 'tools/add_excel_template.py'], check=True)
print('Restored complete source; fixed annual month labels; added Excel template download')
