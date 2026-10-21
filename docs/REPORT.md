# libbnit.so — восстановленная логика `BNITManager.cdrl`

> Исторический отчёт: писался, когда в репозитории была C++-реализация с
> `#embed`. Сейчас единственная реализация — Rust-крейт `laned-bnit`;
> снимок `maps.txt` больше не входит в репозиторий (значение `inode`
> непроверяемо, см. `ALGORITHM.md` §5.2.1). Остальное содержимое актуально.

## 1. Кратко

`libbnit.so` — AArch64-библиотека (ELF64, ET_DYN, Android, собрана clang 19/r28c), которая
регистрирует три нативных метода через `JNI_OnLoad` → `RegisterNatives` и генерирует
клиентские заголовки для HTTP-запросов приложения `com.skiy.lane.app`.

Метод `cdrl` возвращает **массив из 4 элементов** (`byte[][]`, в Java превращается в `String[]`):

| # | Содержимое | Формат |
|---|------------|--------|
| 0 | `X-Core-Token:<base64>` | 127 байт → 170 симв. + 2 `.` (padding) |
| 1 | `X-Request-Trace-Id:<uuid>` | 36-символьный UUID |
| 2 | `X-Client-Meta:<base64>` | 24 байта → 32 символа |
| 3 | `byte[N]` | **тело запроса**: `RC4*(KEY‖nonce‖ts, body)`; для POST его и отправляют |

> **Важно:** порядок элементов в массиве, который вернула библиотека, **не фиксирован** —
> он зависит от содержимого строк (внутри используется hash-контейнер). В разных прогонах
> наблюдались порядки `[core, trace, meta]` и `[meta, core, trace]`. Заголовки нужно
> разбирать по префиксу, а не по индексу.

---

## 2. JNI-регистрация

`JNI_OnLoad` (0x8600) делает:

1. `JavaVM->GetEnv(&env, JNI_VERSION_1_6)` (слот 6 таблицы `JNIInvokeInterface`).
2. Декодирует имя класса (29 байт, XOR-ключ `(i % 13) + 0x4A`) → `com/skiy/lane/app/BNITManager`.
3. `FindClass` + `RegisterNatives` с таблицей из трёх методов (`.data` @ `0x14f18`):

| Имя | Сигнатура | Адрес |
|-----|-----------|-------|
| `verifyLicense` | `(Ljava/lang/String;)Ljava/lang/String;` | 0x8790 |
| `cdrl` | `(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;[BJ)[[B` | 0x88b4 |
| `verifyMagic` | `([BLjava/lang/String;)[B` | 0xb1e8 |

Строки в `.rodata` частично «пулятся» компилятором по фрагментам, а чувствительные
литералы XOR-обфусцированы тем же ключом `(i % 13) + 0x4A` (например, путь
`/proc/sys/kernel/random/boot_id`, `/proc/self/maps`, имя класса, `X-Request-Trace-Id:`,
`X-Client-Meta:`, `LICENSE_VALID_RSA_4096`).

---

## 3. `cdrl` — пошаговый алгоритм

Публичный API повторяет нативный метод:

```cpp
// C++
bnit::Output out = bnit::build("GET", "/user/playlists", "", /*body=*/{}, /*timestamp=*/0);
// out.core_token  -> "X-Core-Token:…"
// out.trace_id    -> "X-Request-Trace-Id:…"
// out.client_meta -> "X-Client-Meta:…"
```

```rust
// Rust (crate `bnit`)
let out = bnit::build("GET", "/user/playlists", "", &[], 0);
println!("{}", out.core_token);
```

Всё остальное (`boot_id`, `inode`, текущее время, 15 случайных `uint32`) —
внутри: `boot_id` генерируется как UUID v4, `inode` берётся из запечённого
`maps.txt`, `timestamp = 0` означает «взять текущее время».

Вход: `method: String`, `path: String`, `query: String`, `body: byte[]`,
`timestamp: long`.

### 3.1. Случайные числа

`func_bc9c` читает 4 байта из `/dev/urandom` (при ошибке — `gettimeofday`, XOR секунд и
микросекунд) и возвращает `uint32`. За один вызов `cdrl` делается **15** таких вызовов:

```
r[0..3]   — nonce (16 байт)
r[4]      — ключ №2 шифра токена
r[5..8]   — байты UUID (trace id)
r[9..14]  — 24 байта «client meta»
```

### 3.2. Время

```
now_ms = tv_sec * 1000 + tv_usec / 1000
ts     = to_string(now_ms + timestamp)   // timestamp — смещение в мс; 0 => «сейчас»
```

### 3.3. Поле `field` (4 байта)

```
key   = KEY32 ‖ hex_LE(r[0..3]) ‖ ts
field = RC4*(key, body)              // RC4 + мутация S, см. ниже
```

`RC4*` — RC4 с одной добавкой: после выдачи каждого байта мутируется ячейка
S-бокса `S[(k + i) & 0xff] += k` (где `k` — только что использованный байт
keystream). Без неё поток совпадает с оригиналом только на коротких телах;
из-за этого подпись запросов с непустым телом долго не сходилась.

### 3.4. Сообщение и HMAC

```
msg1 = method + ":" + path + ":" + query + ":" + ts + ":" + nonce_hex
       + ":" + field + ":" + boot_id + ":" + inode
hmac = HMAC-SHA256(KEY32, msg1)
```

`field` вставляется **сырыми байтами** (не hex).

* `nonce_hex` — 16 байт nonce в little-endian, hex-строка (32 символа).
* `boot_id` — первая строка `/proc/sys/kernel/random/boot_id` без `\n`.
  На практике это обычный UUID (36 символов), и сервер не может сверить его
  с устройством: в подписи он участвует как обычное поле. Поэтому в
  референсной реализации параметр **необязателен** — если не передан,
  генерируется случайный UUID v4.
* `inode` — `stat(apk_path).st_ino` в десятичном виде; при ошибке — `UNKNOWN_INODE`.
  `apk_path` ищется в `/proc/self/maps` так (проверено дизассемблером и эмуляцией):
  1. берётся **первая** строка, содержащая одновременно `.apk` и `/data/app/`;
  2. путь — всё от первого `/` в строке до конца;
  3. путь статится, берётся `st_ino`.
  Если подходящей строки нет — `cdrl` возвращает `null`.

  В референсной реализации значение берётся из столбца inode той же строки —
  оно совпадает с `st_ino`. Для снятого дампа это
  `split_CronetDynamite_installtime.apk` → `656421` (первая подходящая строка,
  а не `base.apk` приложения — так работает оригинал). Сейчас это константа
  `DEFAULT_INODE`, потому что значение всё равно непроверяемо.

### 3.5. Тело токена

```
plain = hex(hmac)                      // 64 символа
      + "|" + ts
      + "|" + nonce_hex
      + "|" + boot_id
      + "|" + inode
      + "|" + (payload.empty() ? "0" : "1")
```

### 3.6. Шифр токена (нестандартный RC4)

Начальная S-таблица — тождественная `0..255` (в `.rodata` она хранится как 16 переставленных
16-байтовых блоков, при загрузке склеивается в `00 01 02 … ff`).

KSA:

```
j = 0
for i in 0..255:
    j = (j + S[i] + KEY32[i % 32] + key2[i % 4]) & 0xff
    swap(S[i], S[j])
```

PRGA (с цепочкой переносов и поворотом):

```
i = j = 0; carry = 0x5A
for byte in plain:
    i = (i + 1) & 0xff
    j = (j + S[i]) & 0xff
    swap(S[i], S[j])
    t = ((S[i] + S[j]) & 0xff) ^ carry
    v = S[t] ^ byte
    v = rotl8(v, 3)
    carry = (carry + v) & 0xff
    out.push(carry)
```

`key2` — **4 байта `r[4]` в big-endian** (в отличие от остальных полей, где little-endian).

### 3.7. Сборка токена

```
body = key2 (4 байта BE) ‖ cipher(plain)
core = "X-Core-Token:" + base64_custom(body)
```

`base64_custom` — алфавит:

```
zxcvbnmasdfghjklqwertyuiop1234567890-_QWERTYUIOPASDFGHJKLZXCVBNM
```

паддинг — символ `.` (точка), а не `=`.

### 3.8. Trace id

16 байт = `r[5..8]` в little-endian, hex, вставка дефисов `8-4-4-4-12`:

```
X-Request-Trace-Id:18171615-1c1b-1a19-201f-1e1d24232221
```

### 3.9. Client meta

24 байта = `r[9..14]` little-endian → `base64_custom` → 32 символа:

```
X-Client-Meta:fc3QdeAYf9-AgDLIjvhDhr7Kj0tVkFEZ
```

### 3.10. Четвёртый элемент

`payload` после `RC4(key, payload)` (п. 3.3). Для 4-байтового входа — те же 4 байта,
что попали в `msg1`.

---

## 4. Константы

```
KEY32 = "SqperSzvbntKmv_CbnngeThis_12303!"     (32 байта)
        собирается из фрагментов "SqperSzv" + "bntKmv_C" + "bnngeThi" + "s_12303!"

ALPHABET = "zxcvbnmasdfghjklqwertyuiop1234567890-_QWERTYUIOPASDFGHJKLZXCVBNM"
PRGA_CARRY_INIT = 0x5A
S_BOX_INIT = identity 0..255
```

`libcrypto.so`/`libssl.so` присутствуют в `DT_NEEDED`, но **ни один символ из них не
импортируется** — вся криптография (SHA-256, HMAC, RC4, base64) реализована внутри
библиотеки. Название `LICENSE_VALID_RSA_4096` — просто строка.

---

## 5. Прочие нативные методы

* **`verifyLicense(String)`** — игнорирует аргумент и возвращает константу
  `LICENSE_VALID_RSA_4096` (проверка лицензии вырезана/заглушена).
* **`verifyMagic(byte[] data, String s)`** — `RC4(KEY32 ‖ s, data)`. Проверено эмуляцией:
  `verifyMagic({1,2,3,4,5}, "hello") = d89e19eff7`.

---

## 6. Как проверялось

Полностью статический разбор (Capstone/LLVM + свой ELF-парсер) дополнен **эмуляцией
исходной библиотеки** на Unicorn: загружался `libbnit.so`, подменялись libc/JNI,
`/dev/urandom`, `boot_id`, `/proc/self/maps`, `ifstream/getline`. Это дало ground truth.

Затем модель (`tools/bnit_model.py`) и C++-порт (`bnit_reimpl.cpp`) сравнивались с
эмулятором:

* 12/12 случайных наборов с 4-байтовым payload — полное совпадение (включая отрицательные `e`);
* совпадение по всем трём заголовкам и 4-му элементу;
* C++-порт выдаёт ту же строку токена, что и оригинальная библиотека.

Дополнительно формат сверен с 10 реальными дампами запросов
(`http-request-dumps/`): во всех случаях `x-core-token` — 216 символов
(215 + завершающая `.`), после base64-декода — 161 байт, внутри 157-байтовый
plaintext; `x-client-meta` — 32 символа (24 байта); trace-id — UUID.
Реализация на «реалистичных» входах (`GET`, `/user/playlists`, UUID, 6-значный
inode) даёт ровно те же длины.

---

## 7. Известные нюансы

1. **Порядок элементов результата не фиксирован** (см. §1).
2. ~~При `len(payload) > 7` поток расходится с чистым RC4~~ — разгадано:
   в `magic_crypt` PRGA после каждого байта мутирует S-бокс
   (`S[(k+i) & 0xff] += k`). Это не артефакт роста вектора, а часть алгоритма;
   реализовано во всех портах и проверено на телах до 120 байт.
3. `boot_id` и `inode` — просто поля внутри HMAC, сервер не знает их истинных
   значений, поэтому подпись не является реальной привязкой к устройству:
   подойдёт любой UUID и любое число. При отсутствии `.apk` в `/proc/self/maps`
   оригинал всё же возвращает `null` — это единственная жёсткая проверка.

---

## 8. Файлы

| Файл | Назначение |
|------|------------|
| `crates/laned-bnit/` | текущая реализация: ядро, C API, `cargo test` |
| `crates/laned-cli/` | подписанный HTTP-клиент и CLI |
| `crates/laned-tui/` | терминальный интерфейс |
| `research/emu.py` | Unicorn-эмулятор `libbnit.so` с заглушками libc/JNI |
| `research/disasm.py`, `pltmap.py`, `strenc.py`, `elfutil.py` | инструменты разбора |
| `research/dexstrings.py`, `apitable.py` | разбор APK: строки dex и таблица эндпоинтов |
| `research/apk/` | декомпилированные классы (jadx), на которых стоят выводы |
| `research/maps.example.txt` | синтетический пример формата `maps` |

Исторические C++/Python-реализации (`bnit_reimpl.cpp`, `tools/bnit_model.py`,
`tests/*.cpp`) удалены при переходе на воркспейс; результаты их сверки
зафиксированы в `cargo test`.

Реализация использует `flate2` (gzip для `red`-ответов) и `getrandom`
(device id и inode); SHA-256/HMAC/RC4/Base64/UUID написаны внутри.
