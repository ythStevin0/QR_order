# PRD - QR Order (Pemesanan Mandiri Meja & Take Away)

| | |
|---|---|
| Versi | 1.0 (draf) |
| Tanggal | 10 Oktober 2026 |
| Platform | Aplikasi mobile Flutter (Android & iOS) |
| Backend | Firebase (Firestore, Auth, Cloud Functions) |
| Status | Draf untuk ditinjau |

> Angka target pada dokumen ini adalah usulan awal dan perlu divalidasi dengan data operasional outlet.

---

## 1. Ringkasan

QR Order adalah aplikasi mobile yang memungkinkan pelanggan restoran memesan dan membayar sendiri tanpa antre di kasir, mengikuti alur seperti Gacoan. Pelanggan memindai QR di meja (makan di tempat) atau QR take away di kasir, memilih menu, membayar lewat QRIS, lalu memantau status pesanan secara real-time. Pesanan yang sudah dibayar langsung muncul di layar dapur. Admin mengelola menu, meja, dan mencetak QR dari aplikasi yang sama.

**Nilai utama:** antrean kasir berkurang, kesalahan pencatatan pesanan turun, dapur menerima pesanan yang sudah pasti dibayar, dan pemilik outlet punya satu sistem sederhana tanpa server sendiri.

## 2. Latar Belakang & Masalah

- Antrean di kasir menjadi hambatan utama saat jam ramai; pelanggan menunggu, kasir kewalahan.
- Pencatatan manual rawan salah (menu tertukar, catatan khusus hilang, nomor meja salah).
- Pembayaran di akhir menimbulkan risiko pelanggan pergi sebelum membayar.
- Pesanan take away butuh sistem antrean yang jelas agar pelanggan tahu kapan pesanannya siap.

## 3. Tujuan, Metrik, dan Non-Tujuan

### Tujuan
1. Pelanggan dapat menyelesaikan pesanan dari scan sampai bayar tanpa bantuan staf.
2. Dapur menerima pesanan jelas, real-time, dan hanya yang sudah dibayar.
3. Admin dapat mengubah menu dan mencetak QR tanpa bantuan teknis.

### Metrik keberhasilan (usulan)

| Metrik | Target usulan | Cara ukur |
|---|---|---|
| Waktu dari scan sampai pembayaran selesai | ≤ 3 menit (median) | Timestamp pesanan dibuat vs berstatus `paid` |
| Tingkat penyelesaian (scan → bayar) | ≥ 80% | Pesanan `paid` / pesanan dibuat |
| Kesalahan pesanan dilaporkan | < 2% pesanan | Catatan staf / komplain |
| Waktu dari `paid` sampai `ready` | Dipantau, target ditetapkan setelah data 2 minggu | Timestamp status |
| Pengurangan antrean kasir | Pengamatan sebelum vs sesudah | Observasi jam ramai |

### Non-tujuan (versi 1.0)
- Pemesanan jarak jauh/delivery.
- Program loyalitas dan voucher.
- Manajemen stok bahan baku.
- Dukungan banyak outlet.

## 4. Persona Pengguna

| Persona | Kebutuhan utama | Konteks |
|---|---|---|
| **Pelanggan** | Pesan cepat, harga jelas, bayar mudah, tahu kapan pesanan siap | Memakai HP sendiri, sering terburu-buru, tanpa akun |
| **Staf dapur/kasir** | Melihat pesanan baru yang sudah dibayar, mengubah status dengan satu ketukan | Layar HP/tablet di dapur, tangan sibuk |
| **Admin/pemilik** | Mengubah menu dan ketersediaan, mengelola meja, mencetak QR | Bukan teknisi, perlu antarmuka sederhana |

## 5. Ruang Lingkup

### MVP (v1.0)
- Scan QR meja dan QR take away.
- Menu per kategori, keranjang, catatan per item.
- Pembayaran QRIS (lewat Cloud Function + webhook).
- Nomor antrean take away harian.
- Status pesanan real-time untuk pelanggan.
- Layar dapur (KDS) dengan alur status.
- Panel admin: menu, meja, QR.
- Login staf dengan peran `admin` dan `kitchen`.

### Fase berikutnya
- Notifikasi push saat pesanan siap.
- Cetak QR sebagai PDF dan cetak struk dapur ke printer thermal.
- Laporan penjualan harian dan menu terlaris.
- Opsi menu (level pedas, topping), stok otomatis.
- Pajak dan service charge.
- Pembatalan/refund, penanganan QRIS kedaluwarsa.
- Pembayaran tunai di kasir.
- Banyak outlet.

### Di luar lingkup
- Aplikasi web/PWA untuk pelanggan.
- Integrasi aplikasi ojek online.

## 6. Alur Pengguna Utama

### 6.1 Makan di tempat
1. Pelanggan membuka aplikasi, kamera menyala otomatis.
2. Scan QR di meja (`qrorder://table/<nomor>`); aplikasi mengenali nomor meja.
3. Memilih menu, mengisi catatan, membuka keranjang.
4. Menekan **Bayar**; pesanan dibuat berstatus `pendingPayment`.
5. Layar QRIS tampil; pelanggan membayar dengan e-wallet/mobile banking.
6. Setelah webhook mengubah status ke `paid`, layar pindah ke status pesanan.
7. Dapur memproses; status berubah `preparing` → `ready` → `completed`; pesanan diantar ke meja.

### 6.2 Take away
1. Pelanggan scan QR take away di kasir (`qrorder://takeaway`).
2. Memilih menu dan **wajib mengisi nama pemesan**.
3. Membayar QRIS.
4. Menerima **nomor antrean harian** (reset tiap hari) dan memantau status.
5. Saat `ready`, pelanggan mengambil pesanan; staf menandai "Sudah diambil".

### 6.3 Dapur/kasir
1. Login staf dengan peran `kitchen`.
2. Daftar pesanan aktif (`paid`, `preparing`, `ready`) urut waktu masuk.
3. Ketuk **Mulai masak** → **Tandai siap** → **Sudah diantar/diambil**.

### 6.4 Admin
1. Login dengan peran `admin`.
2. Tab **Menu**: tambah/ubah/hapus item, saklar tersedia/habis.
3. Tab **Meja & QR**: tambah/hapus meja, tampilkan QR meja dan QR take away untuk dicetak.
4. Tab **Pesanan**: ringkasan pesanan aktif.

## 7. Kebutuhan Fungsional

Prioritas: **P0** wajib MVP, **P1** sangat diharapkan, **P2** fase berikutnya.

### 7.1 Pelanggan

| ID | Kebutuhan | Prioritas |
|---|---|---|
| FR-C1 | Aplikasi memindai QR dan membedakan konteks meja vs take away dari isi QR | P0 |
| FR-C2 | QR yang tidak dikenali ditolak dengan pesan jelas | P0 |
| FR-C3 | Menu tampil per kategori, hanya item yang tersedia | P0 |
| FR-C4 | Tambah/kurangi jumlah item, catatan per item | P0 |
| FR-C5 | Keranjang menampilkan subtotal per item dan total | P0 |
| FR-C6 | Take away wajib mengisi nama sebelum membayar | P0 |
| FR-C7 | Pembayaran QRIS dengan nominal sesuai total pesanan | P0 |
| FR-C8 | Layar pindah otomatis ke status begitu pembayaran terkonfirmasi | P0 |
| FR-C9 | Status pesanan real-time: diterima, disiapkan, siap, selesai | P0 |
| FR-C10 | Nomor antrean take away tampil jelas dan unik per hari | P0 |
| FR-C11 | Tanpa akun/login untuk pelanggan | P0 |
| FR-C12 | Notifikasi push saat pesanan siap | P2 |
| FR-C13 | Riwayat pesanan di perangkat | P2 |

### 7.2 Dapur/kasir

| ID | Kebutuhan | Prioritas |
|---|---|---|
| FR-K1 | Login staf dengan peran `kitchen` | P0 |
| FR-K2 | Hanya pesanan berstatus `paid`, `preparing`, `ready` yang tampil | P0 |
| FR-K3 | Kartu pesanan menampilkan meja/nomor antrean, nama, item, jumlah, catatan, jam | P0 |
| FR-K4 | Perpindahan status satu langkah per ketukan, warna berbeda per status | P0 |
| FR-K5 | Daftar diperbarui real-time tanpa refresh | P0 |
| FR-K6 | Bunyi/getar saat pesanan baru masuk | P1 |
| FR-K7 | Cetak struk dapur | P2 |
| FR-K8 | Pembatalan pesanan oleh staf | P2 |

### 7.3 Admin

| ID | Kebutuhan | Prioritas |
|---|---|---|
| FR-A1 | Login dengan peran `admin` | P0 |
| FR-A2 | CRUD menu: nama, kategori, harga (Rp), deskripsi, URL gambar | P0 |
| FR-A3 | Saklar tersedia/habis tanpa menghapus item | P0 |
| FR-A4 | Tambah/hapus nomor meja | P0 |
| FR-A5 | Tampilkan QR per meja dan QR take away | P0 |
| FR-A6 | Ringkasan pesanan aktif | P1 |
| FR-A7 | Ekspor QR ke PDF siap cetak | P2 |
| FR-A8 | Laporan penjualan harian, menu terlaris | P2 |
| FR-A9 | Unggah foto menu dari galeri (bukan hanya URL) | P2 |

### 7.4 Pembayaran

| ID | Kebutuhan | Prioritas |
|---|---|---|
| FR-P1 | Pembuatan QRIS lewat Cloud Function; kunci rahasia penyedia tidak ada di aplikasi | P0 |
| FR-P2 | Webhook penyedia diverifikasi tanda tangannya lalu mengubah status ke `paid` lewat Admin SDK | P0 |
| FR-P3 | Total dihitung ulang di server dari data menu, bukan dipercaya dari klien | P0 |
| FR-P4 | QRIS kedaluwarsa ditangani dan pesanan `pendingPayment` lama dibersihkan | P1 |
| FR-P5 | Pembayaran tunai di kasir | P2 |
| FR-P6 | Refund | P2 |

## 8. Kebutuhan Non-Fungsional

| Area | Kebutuhan |
|---|---|
| Performa | Menu termuat ≤ 2 detik pada jaringan 4G; perubahan status tampil ≤ 2 detik |
| Keandalan | Nomor antrean tidak boleh ganda (transaksi Firestore); pesanan tidak hilang saat koneksi putus sesaat |
| Keamanan | Aturan Firestore membatasi tulis menu/meja ke admin dan ubah status ke staf; status `paid` hanya dari server; pertimbangkan Firebase App Check |
| Privasi | Hanya menyimpan nama pemesan (take away) dan data pesanan; tanpa akun pelanggan |
| Kompatibilitas | Android dan iOS versi yang didukung Flutter stabil; izin kamera diperlukan |
| Bahasa | Antarmuka Bahasa Indonesia; mata uang Rupiah tanpa desimal |
| Aksesibilitas | Target sentuh memadai, kontras jelas, status tidak hanya dibedakan lewat warna |
| Ketersediaan data | Layar dapur memakai pembaruan real-time Firestore |

## 9. Arsitektur, Model Data, dan Pembayaran

### Arsitektur ringkas
- **Aplikasi Flutter** (satu aplikasi, tiga peran: pelanggan, dapur, admin).
- **Firestore**: koleksi `menu`, `tables`, `orders`, `counters`, `staff`.
- **Firebase Auth**: email/password khusus staf; peran dibaca dari `staff/{uid}.role`.
- **Cloud Functions**: `createQris` dan webhook pembayaran.
- **Penyedia QRIS**: Midtrans atau Xendit (belum diputuskan).

### Isi QR
| Jenis | Isi |
|---|---|
| Meja | `qrorder://table/<nomor>` |
| Take away | `qrorder://takeaway` |

### Model data inti

| Koleksi | Field penting |
|---|---|
| `menu` | name, category, price, description, imageUrl, available |
| `tables` | number |
| `orders` | type, tableNumber, queueNumber, customerName, lines[], total, status, createdAt |
| `counters/{yyyy-MM-dd}` | takeAway (penghitung antrean harian) |
| `staff/{uid}` | role (`admin` / `kitchen`) |

### Status pesanan
`pendingPayment` → `paid` → `preparing` → `ready` → `completed` (dan `cancelled`).

### Alur pembayaran
1. Aplikasi membuat pesanan `pendingPayment`.
2. Aplikasi memanggil `createQris` dengan `orderId`; server menghitung total dari `menu`.
3. Penyedia mengembalikan string QRIS; aplikasi menampilkannya sebagai QR.
4. Pelanggan membayar; penyedia memanggil webhook.
5. Webhook memverifikasi tanda tangan, lalu menetapkan `paid`.
6. Aplikasi pelanggan mendengarkan dokumen pesanan dan pindah layar otomatis.

> Pada kode saat ini pembayaran masih **mock** (`PaymentService.useMock = true`).

## 10. Rencana Rilis

| Fase | Isi | Kriteria selesai |
|---|---|---|
| 0. Setup | Proyek Firebase, konfigurasi Flutter, izin kamera, aturan Firestore, akun staf | Aplikasi berjalan di perangkat uji |
| 1. MVP dengan mock | Alur pelanggan, dapur, admin dengan pembayaran mock | Seluruh alur dapat diuji end-to-end di satu outlet uji |
| 2. Pembayaran nyata | `createQris`, webhook, hitung ulang total di server, pembersihan pesanan kedaluwarsa | Transaksi QRIS sandbox lalu produksi kecil berhasil |
| 3. Uji lapangan | Pilot di satu outlet pada jam ramai | Metrik bagian 3 terukur, bug kritis nol |
| 4. Peningkatan | Notifikasi push, cetak QR PDF, struk dapur, laporan | Sesuai prioritas hasil pilot |

Estimasi waktu belum ditetapkan; perlu disepakati setelah keputusan penyedia pembayaran dan ukuran tim.

## 11. Risiko, Asumsi, dan Ketergantungan

### Risiko

| Risiko | Dampak | Mitigasi |
|---|---|---|
| Harga dikirim dari klien dan bisa dimanipulasi | Kerugian finansial | Hitung ulang total di server (FR-P3) |
| `counters` terbuka agar transaksi dari klien berjalan | Penyalahgunaan | Pindahkan pembuatan pesanan ke Cloud Function |
| Spam pesanan palsu | Beban dapur dan biaya | Dapur hanya melihat pesanan `paid`; App Check |
| Webhook terlambat atau gagal | Pelanggan sudah bayar tetapi tidak terkonfirmasi | Pemeriksaan status berkala ke penyedia, rekonsiliasi harian |
| Sinyal buruk di outlet | Pesanan gagal | Pesan error jelas, percobaan ulang, Wi-Fi outlet |
| Pelanggan tidak mau memasang aplikasi | Adopsi rendah | Lihat pertanyaan terbuka tentang versi web |

### Asumsi
- Pelanggan bersedia memasang aplikasi atau sudah memilikinya.
- Outlet punya koneksi internet stabil dan perangkat untuk dapur.
- QRIS menjadi metode pembayaran utama.

### Ketergantungan
- Firebase dan penyedia QRIS (Midtrans/Xendit).
- Izin kamera dan toko aplikasi (Play Store, App Store).

## 12. Pertanyaan Terbuka

1. **Aplikasi vs web:** apakah pelanggan wajib memasang aplikasi? QR yang membuka halaman web biasanya mengurangi hambatan adopsi dibanding aplikasi terpasang.
2. **Penyedia QRIS:** Midtrans atau Xendit? Perbedaan biaya dan proses onboarding perlu dibandingkan.
3. **Pembayaran tunai:** apakah pelanggan boleh membayar di kasir untuk pesanan meja?
4. **Pajak dan service charge:** apakah ditampilkan terpisah di keranjang?
5. **Kebijakan pembatalan:** siapa yang boleh membatalkan setelah dibayar, dan bagaimana refund?
6. **Jumlah outlet:** satu outlet atau banyak sejak awal? Ini memengaruhi struktur data.
7. **Penggunaan perangkat dapur:** satu HP/tablet bersama atau beberapa stasiun (mie, minuman)?
8. **Bahasa:** hanya Indonesia, atau juga Inggris untuk wisatawan?
