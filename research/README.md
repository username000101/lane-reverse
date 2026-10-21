# Research-скрипты

Инструменты, которыми разбирали оригинальную `libs/libbnit.so`. Для работы
самого крейта они не нужны — это история исследования и способ перепроверить
выводы.

| Файл | Что делает |
|---|---|
| `emu.py` | эмулятор `libbnit.so` на Unicorn: подгружает ELF, подменяет libc/JNI, `/dev/urandom`, `boot_id`, `/proc/self/maps`, `ifstream`/`getline` и позволяет вызвать `cdrl` с любыми входами |
| `disasm.py` | аннотирующий дизассемблер AArch64 (Capstone): разрешает `ADRP+ADD` в строки, подписывает `BL` именами импортов и функций из `.eh_frame` |
| `elfutil.py` | минимальный парсер ELF64 (секции, сегменты, relocations, строки) |
| `pltmap.py` | карта PLT-стабов → импортируемые символы |
| `dexstrings.py` | вытаскивает таблицу строк из `classes*.dex` внутри APK |
| `schema.py` | рисует markdown-схему по JSON-ответу |
| `apk/` | декомпилированные через jadx классы, на которых основан раздел про `/track/stream` (`TracksApi`, `TrackStreamingResult`, `BNITInterceptor`, `ResolvingMediaSource`, `AudioQuality`) |
| `strenc.py` | поиск XOR-обфусцированных литералов (ключ `(i % 13) + 0x4A`) |

## Зависимости

```
pip install unicorn capstone pyelftools
```

## Примеры

```bash
python research/emu.py                 # прогнать cdrl и напечатать результат
python research/disasm.py 0x88b4 0x300 # дизассемблировать начало cdrl
python research/strenc.py              # вытащить обфусцированные строки
```

`emu.py` рассчитан на `libs/libbnit.so`; пути считаются от расположения скрипта.

## Чем проверялось

Сверка шла в два слоя: эмулятор оригинала против Python-модели (на тот момент),
затем golden-векторы из эмулятора зафиксированы в `cargo test`. Отдельно
проверялись реальные токены из `http-request-dumps/`: они расшифровываются тем
же ключом, а HMAC сходится по URL запроса.
