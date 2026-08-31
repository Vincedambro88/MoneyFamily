from pathlib import Path
import subprocess

# The previous automated import-fix commit accidentally truncated MainActivity.kt.
# Restore the last complete application source, then re-apply the deterministic UI patches.
main = Path('app/src/main/java/com/moneyfamily/app/MainActivity.kt')
subprocess.run([
    'git', 'checkout', '887e8741442f3ab701899bfa00fddd41527208e3', '--', str(main)
], check=True)

s = main.read_text()
s = s.replace(
    'import androidx.activity.result.contract.ActivityResultContracts\nimport androidx.activity.compose.rememberLauncherForActivityResult\nimport androidx.activity.result.contract.ActivityResultContracts\n',
    'import androidx.activity.result.contract.ActivityResultContracts\n'
)
main.write_text(s)

# Restore the complete dashboard implementation and in-app Excel template/import UI.
subprocess.run(['python3', 'tools/patch_dashboard.py'], check=True)
subprocess.run(['python3', 'tools/add_excel_template.py'], check=True)

# Ensure annual month labels are generated from a stable day (1), so changing
# Calendar.MONTH can never roll into a neighbouring month on dates such as the 31st.
s = main.read_text()
old = 'SimpleDateFormat("MMM",Locale.ITALIAN).format(Calendar.getInstance().apply{set(Calendar.MONTH,m)}.time)'
new = 'SimpleDateFormat("MMM",Locale.ITALIAN).format(Calendar.getInstance().apply{set(Calendar.DAY_OF_MONTH,1);set(Calendar.MONTH,m)}.time)'
if old in s:
    s = s.replace(old, new, 1)
main.write_text(s)
print('Restored complete MainActivity and applied annual/Excel fixes')
