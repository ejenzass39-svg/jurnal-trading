#!/data/data/com.termux/files/usr/bin/sh
# Upload proyek ini ke GitHub dari Termux. Jalankan di folder proyek: sh push_ke_github.sh
set -e
command -v git >/dev/null 2>&1 || pkg install -y git
printf "Username GitHub: "; read U
printf "Nama repo (sudah dibuat kosong di github.com/new): "; read R
printf "Personal Access Token (scope: repo, workflow): "; read T
[ -d .git ] || git init -b main
git add -A
git -c user.name="$U" -c user.email="$U@users.noreply.github.com" commit -m "Jurnal Trading APK" || true
git push -f "https://$U:$T@github.com/$U/$R.git" HEAD:main
echo "Selesai. Buka https://github.com/$U/$R/actions untuk melihat proses build."
