# Jurnal Trading SMC (APK)

Aplikasi Android (WebView) berisi jurnal trading SMC dengan mode akun IDR dan USC (Exness Cent).
Data tersimpan di penyimpanan aplikasi. Gunakan Setup > Ekspor cadangan (JSON) secara berkala;
file tersimpan di folder Download. Menghapus/uninstall aplikasi menghapus data.

Build: push ke GitHub, tab Actions > "Build APK" > unduh artifact "JurnalTrading-APK".
Untuk APK permanen di halaman Releases: buat tag, mis. `git tag v1.0 && git push origin v1.0`.

## Update v1.1
- Zoom (cubit/double-tap) dinonaktifkan.
- Tombol "+ Trade baru" diperbaiki (bentrok nama fungsi `form` dengan properti `button.form`).
- Jurnal tampil sebagai kartu (ada tombol Tabel lengkap), tombol + melayang, ikon menu, link screenshot bisa dibuka.
- Data disimpan di penyimpanan aplikasi + localStorage; tombol Back menutup form / kembali ke Dashboard.
- APK kini ditandatangani dengan keystore tetap (`app/jurnal.keystore`) sehingga update berikutnya bisa menimpa tanpa uninstall.

## Update v1.2
- Perbaikan: trade yang kena SL (Exit di/ melewati SL) kini selalu terhitung Loss, bukan BE. Toleransi BE hanya untuk hasil mendekati nol.
- Tombol cepat "Kena SL" / "Kena TP" di form trade.
- Menu Pasar: tabel sesi (Sydney, Asia, London, New York, overlap) dan kill zone dalam UTC & WIB, status buka/tutup live.
- Berita ekonomi ala Forex Factory (feed mingguan): High merah, Medium oranye, Low putih; warna, level, mata uang bisa diatur; berita manual.
- Workflow: langkah setup-android dihapus (penyebab build gagal).
