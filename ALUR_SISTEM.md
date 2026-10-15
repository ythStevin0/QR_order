# Alur Sistem QR Order

| | |
|---|---|
| Versi | 1.0 |
| Tanggal | 14 Oktober 2026 |
| Status | Draf alur untuk ditinjau. Melengkapi PRD v2 |
| Keterangan | Diagram ditulis dengan Mermaid (tampil otomatis di GitHub dan editor Markdown yang mendukungnya). Versi ini memuat diagram sebagai gambar PNG (folder `diagram/`) supaya tampil di editor mana pun. Sumber Mermaid tetap ada di bagian lipat di bawah tiap gambar. Kesembilan diagram sudah diuji dirender tanpa galat sintaks |

---

## 1. Asumsi yang dipakai

Beberapa hal belum diputuskan pemilik produk. Alur di bawah memakai asumsi ini, ditandai **[Asumsi]**, dan ada di bagian 12 untuk dikonfirmasi.

| # | Asumsi |
|---|---|
| A1 | Pelanggan **tidak wajib login** untuk memesan lewat website. Login hanya untuk fitur akun (riwayat, voucher, poin) |
| A2 | **Aplikasi Flutter** untuk pelanggan dan staf, dengan tampilan berbeda menurut **peran** |
| A3 | Voucher dan diskon hanya berlaku untuk **pengguna yang login** (di aplikasi) |
| A4 | Pesanan masuk ke dapur lewat **print agent** (printer struk + alarm) dan layar dapur |
| A5 | Pembayaran memakai QRIS lewat penyedia pembayaran (mock di awal) |
| A6 | Harga akhir, diskon, dan status pesanan **selalu dihitung dan diputuskan server** |

## 2. Gambaran besar

![Diagram 01-gambaran-besar](diagram/01-gambaran-besar.png)

<details>
<summary>Sumber Mermaid (untuk diedit)</summary>

```mermaid
flowchart LR
    subgraph Klien
        W["Web pelanggan (React)"]
        A["Aplikasi (Flutter)<br/>peran: pelanggan / staf"]
    end

    subgraph Backend["Backend (Spring Boot, modular monolith)"]
        API["API REST + SSE"]
        ORD["ordering"]
        PAY["payment"]
        CAT["catalog"]
        IDN["identity"]
        OUT["shared/outbox"]
    end

    DB[("PostgreSQL")]
    PG["Penyedia QRIS"]

    subgraph Outlet
        AG["Print agent"]
        PR["Printer struk"]
        AL["Alarm / buzzer"]
        KD["Layar dapur"]
    end

    QR(("QR meja / take away")) --> W
    QR --> A
    W --> API
    A --> API
    API --> ORD
    API --> PAY
    API --> CAT
    API --> IDN
    ORD --> DB
    PAY --> DB
    CAT --> DB
    IDN --> DB
    PAY <--> PG
    ORD --> OUT
    OUT --> DB
    OUT --> AG
    AG --> PR
    AG --> AL
    A --> KD
```

</details>

Intinya: satu backend, dua klien, dan satu jalur pemberitahuan ke dapur yang dipicu **hanya setelah pesanan lunas**.

## 3. Alur A: makan di tempat (scan QR meja)

QR meja berisi URL https, bukan skema khusus aplikasi, sehingga kamera HP biasa membukanya di browser.

```
https://pesan.contoh.id/o/<outlet>/meja/<token-meja>
```

![Diagram 02-alur-dine-in](diagram/02-alur-dine-in.png)

<details>
<summary>Sumber Mermaid (untuk diedit)</summary>

```mermaid
sequenceDiagram
    actor P as Pelanggan
    participant W as Web (React)
    participant S as Server
    participant G as Penyedia QRIS

    P->>W: Scan QR meja (kamera HP)
    W->>S: GET konteks meja (outlet + token meja)
    S-->>W: Outlet, nomor meja, menu tersedia
    P->>W: Pilih menu, isi catatan
    P->>W: Tekan Bayar
    W->>S: POST pesanan (menu + qty, tanpa harga) + kunci idempotency
    S->>S: Validasi meja, hitung harga dari DB
    S-->>W: Pesanan PENDING_PAYMENT + total
    W->>S: POST minta pembayaran
    S->>G: Buat QRIS
    G-->>S: Kode QRIS + batas waktu
    S-->>W: QRIS + hitung mundur
    P->>G: Bayar lewat e-wallet atau mobile banking
    G->>S: Webhook pembayaran berhasil
    S->>S: Verifikasi tanda tangan, status menjadi PAID
    S-->>W: Status PAID (SSE)
    W-->>P: Tampil status pesanan
```

</details>

Poin penting:

- Klien **tidak pernah** mengirim harga. Server mengambil harga dari database.
- Kunci idempotency mencegah pesanan ganda bila tombol Bayar ditekan dua kali atau jaringan mengulang permintaan.
- Hanya **webhook** yang mengubah status menjadi `PAID`. Staf tidak bisa menandai lunas.

## 4. Alur B: take away

![Diagram 03-alur-take-away](diagram/03-alur-take-away.png)

<details>
<summary>Sumber Mermaid (untuk diedit)</summary>

```mermaid
flowchart TD
    A["Scan QR take away di kasir atau pintu"] --> B["Buka web, konteks: outlet + take away"]
    B --> C["Pilih menu"]
    C --> D["Wajib isi nama pelanggan"]
    D --> E["Bayar QRIS"]
    E --> F{"Webhook pembayaran diterima?"}
    F -- "Ya" --> G["Status PAID, nomor antrean harian dibuat per outlet"]
    F -- "Kedaluwarsa" --> H["Pesanan CANCELLED, bisa buat pembayaran baru"]
    G --> I["Pelanggan memantau status dan nomor antrean"]
    I --> J["Status READY: pelanggan mengambil"]
    J --> K["Staf menandai COMPLETED"]
```

</details>

Nomor antrean unik **per hari bisnis per outlet** dan dibuat dengan penguncian database (`SELECT ... FOR UPDATE`) agar tidak ada dua pesanan dengan nomor sama.

## 5. Alur C: pelanggan di aplikasi (login, riwayat, voucher)

**[Asumsi A1, A3]** Aplikasi memberi nilai lebih daripada web: akun, riwayat, voucher.

![Diagram 04-alur-aplikasi](diagram/04-alur-aplikasi.png)

<details>
<summary>Sumber Mermaid (untuk diedit)</summary>

```mermaid
flowchart TD
    A["Buka aplikasi atau scan QR dengan deep link"] --> B{"Sudah login?"}
    B -- "Belum" --> C["Mode tamu: boleh memesan tanpa diskon"]
    B -- "Sudah" --> D["Tampilkan riwayat, favorit, voucher aktif"]
    C --> E["Pilih menu"]
    D --> E
    E --> F{"Punya voucher?"}
    F -- "Ya, dan login" --> G["Kirim kode voucher ke server"]
    F -- "Tidak" --> H["Hitung total tanpa diskon"]
    G --> I["Server validasi voucher, lihat bagian 6"]
    I --> J["Total akhir dihitung server"]
    H --> J
    J --> K["Bayar QRIS"]
    K --> L["Status PAID dan push notifikasi saat READY"]
```

</details>

Aturan: aplikasi **tidak** menghitung diskon sendiri. Ia hanya mengirim kode voucher dan menampilkan hasil dari server.

## 6. Alur validasi voucher (di server)

![Diagram 05-validasi-voucher](diagram/05-validasi-voucher.png)

<details>
<summary>Sumber Mermaid (untuk diedit)</summary>

```mermaid
flowchart TD
    S["Terima kode voucher + pesanan"] --> A{"Pengguna login?"}
    A -- "Tidak" --> X1["Tolak: login diperlukan"]
    A -- "Ya" --> B{"Voucher ada dan aktif?"}
    B -- "Tidak" --> X2["Tolak: tidak ditemukan atau kedaluwarsa"]
    B -- "Ya" --> C{"Berlaku untuk outlet dan jam ini?"}
    C -- "Tidak" --> X3["Tolak: di luar jadwal atau outlet"]
    C -- "Ya" --> D{"Minimum belanja terpenuhi?"}
    D -- "Tidak" --> X4["Tolak: belanja kurang"]
    D -- "Ya" --> E{"Kuota dan batas per akun masih ada?"}
    E -- "Tidak" --> X5["Tolak: kuota habis"]
    E -- "Ya" --> F["Hitung diskon, batasi tidak melebihi total"]
    F --> G["Cadangkan pemakaian secara atomik"]
    G --> H["Simpan ke pesanan: harga kotor, diskon, harga bersih"]
```

</details>

Aturan yang wajib:

| Aturan | Alasan |
|---|---|
| Pemakaian voucher **atomik** (kunci atau `@Version`) | Dua perangkat tidak boleh sama-sama memakai voucher satu kali pakai |
| **Idempoten** | Tekan bayar dua kali tidak boleh memakai voucher dua kali |
| Pesanan menyimpan harga kotor, diskon, harga bersih | Laporan dan rekonsiliasi QRIS harus cocok dengan total setelah diskon |
| Kebijakan refund jelas | Tentukan apakah voucher dikembalikan saat pesanan dibatalkan |
| Batas anggaran per promo | Mencegah promo merugikan tanpa terkontrol |

## 7. Alur pembayaran dan gangguan

![Diagram 06-pembayaran-gangguan](diagram/06-pembayaran-gangguan.png)

<details>
<summary>Sumber Mermaid (untuk diedit)</summary>

```mermaid
flowchart TD
    A["Pesanan PENDING_PAYMENT"] --> B["Minta QRIS ke penyedia"]
    B --> C{"Penyedia merespons?"}
    C -- "Ya" --> D["Tampilkan QRIS + hitung mundur"]
    C -- "Lambat atau gagal" --> E["Retry terbatas lalu circuit breaker"]
    E --> F{"Circuit breaker terbuka?"}
    F -- "Ya" --> G["Tawarkan bayar di kasir bila diaktifkan outlet"]
    F -- "Tidak" --> B
    D --> H{"Pelanggan membayar sebelum kedaluwarsa?"}
    H -- "Ya" --> I["Webhook diterima"]
    H -- "Tidak" --> J["Pesanan CANCELLED, voucher dikembalikan sesuai kebijakan"]
    I --> K{"Tanda tangan valid dan belum diproses?"}
    K -- "Tidak valid" --> L["Tolak dan catat WARN"]
    K -- "Duplikat" --> M["Abaikan, kembalikan sukses (idempoten)"]
    K -- "Valid dan baru" --> N["Status PAID, event ke outbox"]
    D --> O["Webhook terlambat atau hilang"]
    O --> P["Rekonsiliasi berkala memeriksa status ke penyedia"]
    P --> N
```

</details>

## 8. Alur pemberitahuan ke dapur (print agent / IoT)

Pemberitahuan dipicu **setelah pesanan `PAID`**, lewat pola outbox agar tidak ada pesanan lunas yang tidak sampai ke dapur.

![Diagram 07-notifikasi-dapur](diagram/07-notifikasi-dapur.png)

<details>
<summary>Sumber Mermaid (untuk diedit)</summary>

```mermaid
sequenceDiagram
    participant S as Server
    participant DB as PostgreSQL
    participant W as Worker outbox
    participant AG as Print agent outlet
    participant PR as Printer struk
    participant AL as Alarm
    participant KD as Layar dapur

    S->>DB: Simpan PAID + event dalam SATU transaksi
    W->>DB: Ambil event yang belum terkirim
    W->>AG: Kirim pesanan (SSE / WebSocket / MQTT)
    AG->>AG: Cek ID cetak, sudah pernah dicetak?
    AG->>PR: Cetak struk dapur (ESC/POS)
    AG->>AL: Bunyikan alarm
    AG-->>W: Konfirmasi diterima
    W->>DB: Tandai event terkirim
    W->>KD: Dorong pembaruan ke layar dapur
```

</details>

Aturan:

| Hal | Pendekatan |
|---|---|
| Event tidak boleh hilang | Ditulis ke tabel outbox dalam transaksi yang sama dengan status `PAID` |
| Cetak ganda | Setiap pesanan punya ID cetak. Agen tidak mencetak ID yang sama dua kali |
| Jaringan outlet putus | Agen menyimpan antrean lokal dan menyinkronkan ulang. Layar dapur menampilkan penanda "terputus" |
| Printer bermasalah | Tombol **cetak ulang** di layar dapur |
| Keamanan perangkat | Kredensial per perangkat, bisa dicabut, hanya menerima pesanan outlet-nya |

Catatan: alarm dan printer **hanya memberi tahu**. Perubahan status tetap dilakukan staf lewat layar (bagian 9).

## 9. Alur staf di aplikasi (berdasarkan peran)

![Diagram 08-peran-staf](diagram/08-peran-staf.png)

<details>
<summary>Sumber Mermaid (untuk diedit)</summary>

```mermaid
flowchart TD
    A["Login staf"] --> B{"Peran?"}
    B -- "Dapur" --> C["Layar antrean: PAID, PREPARING, READY"]
    B -- "Manajer" --> D["Antrean + menu habis + laporan harian"]
    B -- "Admin" --> E["Kelola menu, meja, QR, voucher"]
    C --> F["Pesanan baru: bunyi dan getar"]
    F --> G["Ketuk Mulai masak"]
    G --> H["Ketuk Tandai siap"]
    H --> I["Ketuk Selesai saat diantar atau diambil"]
    D --> J["Tandai menu habis: hilang dari menu pelanggan"]
```

</details>

| Peran | Boleh | Tidak boleh |
|---|---|---|
| Pelanggan | Memesan, melihat status pesanannya sendiri, memakai voucher | Mengubah status, melihat pesanan orang lain |
| Dapur | Melihat antrean outlet-nya, mengubah status PREPARING, READY, COMPLETED, cetak ulang | Menandai `PAID`, mengubah harga, melihat outlet lain |
| Manajer | Seperti dapur + menu habis + laporan outlet | Mengubah data outlet lain |
| Admin pusat | Menu induk, outlet, peran, voucher | Menandai `PAID` secara manual |

Pembatasan peran dan outlet ditegakkan **di server**. Menyembunyikan tombol di aplikasi bukan perlindungan.

## 10. Siklus status pesanan

![Diagram 09-status-pesanan](diagram/09-status-pesanan.png)

<details>
<summary>Sumber Mermaid (untuk diedit)</summary>

```mermaid
stateDiagram-v2
    [*] --> PENDING_PAYMENT
    PENDING_PAYMENT --> PAID: webhook pembayaran
    PENDING_PAYMENT --> CANCELLED: kedaluwarsa atau dibatalkan
    PAID --> PREPARING: dapur mulai
    PAID --> CANCELLED: dibatalkan staf, refund
    PREPARING --> READY: selesai dimasak
    READY --> COMPLETED: diantar atau diambil
    COMPLETED --> [*]
    CANCELLED --> [*]
```

</details>

| Perubahan | Pelaku yang sah |
|---|---|
| `PENDING_PAYMENT` ke `PAID` | **Hanya sistem pembayaran** (webhook atau rekonsiliasi) |
| `PENDING_PAYMENT` ke `CANCELLED` | Sistem (kedaluwarsa) atau pelanggan |
| `PAID` ke `PREPARING`, `PREPARING` ke `READY`, `READY` ke `COMPLETED` | Staf dapur atau manajer |
| `PAID` ke `CANCELLED` | Staf berwenang, dengan refund dan jejak audit |

Pembatalan hanya diizinkan sebelum mulai dimasak. Setiap perubahan status dicatat (siapa, kapan, dari, ke) untuk audit dan data prediksi waktu siap.

## 11. Kegagalan dan perilaku sistem

| Gangguan | Dampak | Perilaku yang diharapkan |
|---|---|---|
| Penyedia QRIS lambat atau mati | Pelanggan tidak bisa membayar | Retry terbatas, circuit breaker, tawarkan bayar di kasir |
| Webhook terlambat atau hilang | Pelanggan sudah bayar tetapi status belum berubah | Tampilkan "menunggu konfirmasi", rekonsiliasi berkala |
| Webhook dikirim berulang | Risiko status atau voucher ganda | Pemrosesan idempoten |
| Internet outlet putus | Dapur tidak menerima pesanan | Agen menyimpan antrean lokal, layar menandai terputus, sinkron otomatis saat tersambung |
| Printer macet | Struk tidak keluar | Alarm dan layar dapur tetap berfungsi, tombol cetak ulang |
| Server restart | Koneksi SSE putus | Klien menyambung ulang otomatis dan menarik status terbaru |
| Dua perangkat memakai voucher sama | Pemakaian ganda | Pemakaian atomik, satu berhasil |
| Tombol bayar ditekan dua kali | Pesanan ganda | Kunci idempotency |
| Pelanggan menyisipkan harga atau diskon di request | Manipulasi harga | Server abaikan, hitung dari database |

## 12. Pemetaan ke fase

| Fase | Isi alur |
|---|---|
| **0. Fondasi** | Build hijau, Flyway, `outlet_id`, idempotency, `@Version`, indeks |
| **1. MVP pilot (1 outlet)** | Alur A dan B di web, QRIS (mock lalu sandbox), layar dapur, print agent dasar, admin menu dan meja |
| **2. Skala sedang** | Multi-outlet, peran per outlet, outbox + worker penuh, rekonsiliasi, circuit breaker, laporan, pagination |
| **3. Aplikasi dan promo** | Aplikasi Flutter (akun, riwayat, push, voucher, mode staf), poin atau stempel |
| **4. Skala padat** | Event bus, replica, gateway real-time, multi-penyedia pembayaran, AI |

## 13. Pertanyaan terbuka

1. Apakah pelanggan **wajib login** untuk memakai diskon, atau cukup memasukkan kode di web juga? (Asumsi A1 dan A3 hanya untuk aplikasi.)
2. Apa arti **"pembeda untuk staf"**: tampilan khusus peran, benefit atau diskon staf, atau keduanya?
3. Apakah dapur memerlukan **layar untuk mengubah status**, atau cukup printer dan alarm? (Alur di atas mengasumsikan layar diperlukan agar status pelanggan akurat.)
4. Protokol print agent: **SSE, WebSocket, atau MQTT**? (Untuk pilot, SSE atau WebSocket paling sederhana.)
5. Kebijakan voucher saat pesanan dibatalkan atau di-refund: dikembalikan atau hangus?
6. Apakah bayar di kasir (mode degradasi) akan diaktifkan, dan siapa yang mengonfirmasi lunas?
