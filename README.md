# FamilyOnly — whitelist call screener for Android

Only your trusted numbers ring. Everything else is rejected silently.

FamilyOnly is a minimal, offline Android call screener. You add the numbers you trust (family, close friends, work), and FamilyOnly blocks all other incoming calls automatically — no ads, no network, no cloud.

## Download

Get the latest APK from [**Releases**](https://github.com/JustABeing00/FamilyOnly/releases) → download `FamilyOnly-v1.1.apk`, open it on your phone, allow **Install unknown apps**, then tap Install.

> Coming from v1.0? Uninstall v1.0 first, then install v1.1 (the signing key changed once — this is the last time you'll ever need to reinstall for an update).

## How it works

- Uses Android's `CallScreeningService` (`ROLE_CALL_SCREENING`).
- **Whitelist empty → allow all.** Safety default so you never lock yourself out.
- **Number in whitelist → allow**, phone rings normally.
- **Number not in list / hidden / too short → reject instantly**, skip notification and call log where possible, then clean up any leftover log trace.
- Matching is by **last 10 digits**, so `+91XXXXXXXXXX`, `0XXXXXXXXXX`, and `XXXXXXXXXX` all match the same contact.

Optional: pair it with your carrier's **busy-forward** (`**67*<number>#`) so blocked callers hear an unreachable / switched-off announcement instead of a busy tone. This is carrier-controlled — FamilyOnly just opens the dialer for you.

## Features

- Trusted-number whitelist, add / long-press to remove
- Instant silent reject for unknown + withheld numbers
- Automatic blocked-call log cleanup + manual "Erase blocked calls"
- Busy-forward helper (set / cancel `**67#`)
- Offline build — no Gradle, no downloads, no trackers
- Lightweight (~43 KB APK)

## Requirements

- Android 10+ (minSdk 29, targetSdk 34)
- Set FamilyOnly as the default **Call screening app** when prompted
- Permissions: `READ_CALL_LOG`, `WRITE_CALL_LOG` (log cleanup), `READ_PHONE_STATE`, `READ_CONTACTS`

## Getting started

1. Install the APK (see below).
2. Open **FamilyOnly**, add your trusted numbers, e.g. `+91XXXXXXXXXX`.
3. Tap **Enable call screening** → Allow.
4. Test from a second phone: trusted number rings, unknown number is rejected.

> If the whitelist is empty, all calls are allowed. Add at least one number to start screening.

## Busy-forward (optional)

1. Enter a forward target in the app (default suggestion: an unreachable number like `9999999999`).
2. Tap **Forward busy calls** → press CALL in the dialer.
3. Dial `*#67#` to verify status, `##67#` (or **Cancel busy-forward** in the app) to disable.

Forwarding codes and announcements vary by carrier and country. If `**67*` is rejected, use your phone Settings → Call forwarding, or try another valid-but-unreachable number.

## Install / build

Prebuilt APK: `FamilyOnlyScreener/build/familyonly.apk`

Offline build (Windows, no Gradle/network — just Android SDK build-tools + JDK):

```powershell
.\install.ps1
# rebuilds + installs + requests screening role + launches the app
```

Or step by step:

```powershell
powershell -ExecutionPolicy Bypass -File .\FamilyOnlyScreener\build.ps1
adb install -r .\FamilyOnlyScreener\build\familyonly.apk
```

Verify role:

```powershell
powershell -ExecutionPolicy Bypass -File .\verify.ps1
```

Project layout:

- `FamilyOnlyScreener/` — app source (`MainActivity`, `FamilyScreenerService`, `LogCleaner`)
- `FamilyOnlyScreener/build.ps1` — offline `aapt2 + javac + d8 + zipalign + apksigner` build
- `install.ps1` — rebuild + install + set screening role + launch
- `verify.ps1` — check role + telecom screening log

## Privacy

- 100% offline. No internet permission, no analytics, no ads.
- Numbers are stored only in local `SharedPreferences`. Nothing leaves the phone.

## Limitations (honest)

- On VoLTE you may still see ~1 ring before reject — that's network paging, no screener can prevent it without carrier-side blocking.
- Exact busy / unreachable wording is controlled by your carrier, not the app.
- Only cellular calls are screened. WhatsApp / Telegram / other VoIP calls are not affected.
- Keep your stock Phone app's own spam blocking OFF to avoid double handling — let FamilyOnly own screening.

## Contributing

Issues and PRs welcome. Keep it small, offline, and dependency-free.
