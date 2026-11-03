# Backups and the passphrase

The backup is the only way the baby's data leaves the phone (Export / Import). Since 0.23.0 it
can be locked with a passphrase, so a file left in a drive, an email or a chat can't be read.

## What it does

- **Settings › Data › Protect backups**: type a passphrase (at least 4 characters) and Save.
  From then on, **Export backup** writes a locked file. Remove passphrase goes back to plain JSON.
- **Import backup** recognises a locked file. If this phone has the same passphrase, it opens at
  once; otherwise a sheet asks for it ("This backup is locked"). A wrong one says so and keeps what
  was typed. Import still never deletes and keeps the newest edit (`Backup.merge`).
- The spreadsheet (CSV) and the doctor's report (PDF) are never locked: they're meant to be shared.
- **There is no recovery.** Cochilo has no account and no server, so a lost passphrase means those
  files can't be opened by anyone. The sheet says so in plain words: write it down.

## How it's locked

`backup/Sealed.kt`, pure JVM crypto with unit tests (`SealedTest`):

- The key is derived from the passphrase with **PBKDF2-HMAC-SHA256**, 600,000 iterations (OWASP's
  2023 floor) and a random 16-byte salt per file.
- The backup JSON is sealed with **AES-256-GCM** (random 12-byte IV, 128-bit tag; the format name
  is authenticated too). Any change to the file, or a wrong passphrase, fails the tag check.
- The locked file is JSON: `{"format": "cochilo-backup-sealed", "version": 1, "kdf", "iterations",
  "salt", "iv", "data"}`. Inside is the ordinary backup (`BackupFile`, still version 3), so
  everything an older backup could do, a locked one can.
- `SealedFile.VERSION` is the lock's own version; a newer one is refused as "made by a newer
  Cochilo", like the backup itself.

## Where the passphrase lives

In the app's private storage on this phone (`Prefs.backupPassphrase`). Android's auto backup is
off (`allowBackup="false"`), so it never leaves the phone; it's there so exporting stays one tap.
The lock protects the file *off* the phone; on the phone, the data is as private as the phone is.
