# STEREO — Production Release Signing & Migration Guide

## 1. Overview & Audit

Prior to this setup, the initial alpha previews (`1.0.0-alpha.1` through `1.0.0-alpha.3`) were signed with the default Android debug certificate (`debug.keystore`). While sufficient for localized testing, distributing builds signed with a debug certificate presents serious risks:
- Debug keys are publicly known and insecure.
- They expire after 365 days on many systems.
- App stores (Google Play, F-Droid) and security software reject debug-signed APKs.
- Changing signing keys later breaks seamless in-app upgrades.

To ensure long-term integrity, STEREO now uses a **dedicated RSA 4096-bit production release keystore** with a 27+ year validity period (until 2054).

---

## 2. Certificate Fingerprint Comparison

### Legacy Alpha Preview Certificate (`debug.keystore`)
*Used for Alpha.1, Alpha.2, and published Alpha.3 preview releases.*
- **Owner / DN:** `C=US, O=Android, CN=Android Debug`
- **Key Algorithm:** RSA 2048-bit
- **SHA-256 Fingerprint:** `c7a123376dca9dc33d84e7b287dc0a15ee321bdfd8a3a9edaff36a64f65d1fc1`
- **SHA-1 Fingerprint:** `157fe3a002d08342689210b3b9aa7f24cf84135a`
- **MD5 Fingerprint:** `3eb18cf61f4f9952458dbff47bc41215`

### Permanent Production Release Certificate (`stereo-release.jks`)
*Permanent production signing key for Alpha.4+ and all official distributions.*
- **Owner / DN:** `CN=STEREO Music Player, OU=STEREO Core Team, O=STEREO, L=Hyderabad, ST=Telangana, C=IN`
- **Issuer:** `CN=STEREO Music Player, OU=STEREO Core Team, O=STEREO, L=Hyderabad, ST=Telangana, C=IN`
- **Key Algorithm:** RSA 4096-bit (PKCS12)
- **Validity:** November 19, 2026 — April 6, 2054 (~27.4 years / 10,000 days)
- **Alias:** `stereo-release`
- **SHA-256 Fingerprint:** `7E:38:21:4D:3F:83:E4:FD:CC:1B:03:50:32:85:19:41:83:92:DB:E4:F6:B2:F1:AC:5F:B2:BD:03:96:D6:C0:64`
- **SHA-1 Fingerprint:** `45:96:0E:F6:76:EC:DB:22:37:2C:51:3D:87:05:86:A2:25:1B:BD:D3`
- **MD5 Fingerprint:** `D1:B1:63:E6:18:CC:0E:7E:BE:54:06:E2:F9:7E:D5:8C`

---

## 3. Keystore Storage & Gradle Integration

### Physical Location
The release keystore is stored **strictly outside the Git repository** in the user's home profile:
```
~/.keystores/stereo-release.jks
```
A backup of the configuration parameters is stored at:
```
~/.keystores/stereo-release.properties
```

### Two-Tier Secure Loading in `app/build.gradle.kts`
Gradle resolves credentials using a secure hierarchy:

1. **Environment Variables (Primary / CI):**
   - `STEREO_KEYSTORE_PATH`
   - `STEREO_KEYSTORE_PASSWORD`
   - `STEREO_KEY_ALIAS`
   - `STEREO_KEY_PASSWORD`
2. **Local Properties File (Local Development):**
   - `keystore.properties` in project root (ignored by `.gitignore`).
3. **Graceful Fallback:**
   - If credentials are absent (e.g. untrusted open-source PRs on GitHub Actions), the build produces an unsigned release APK without crashing.

### `.gitignore` Protection
The repository `.gitignore` explicitly prevents accidental exposure:
```gitignore
keystore.properties
/keystore.properties
*.jks
*.keystore
*.key
*.p12
```

---

## 4. Secure Backup Strategy (3-2-1 Rule)

The production private key is irrecoverable if lost. If the key is lost, future updates cannot be installed over existing installations.

To protect this key:
1. **Primary Working Copy:** Located at `~/.keystores/stereo-release.jks`.
2. **Encrypted Password Manager:** Store the keystore file and passwords in an encrypted vault (Bitwarden, 1Password, or KeePassXC) as a secure attachment.
3. **Offline Cold Storage:** Copy the `.keystores` folder to an encrypted USB drive (BitLocker / VeraCrypt) stored offline in a secure physical location.
4. **Access Control:** Never upload `stereo-release.jks` or `keystore.properties` to cloud storage (Google Drive, Dropbox) unencrypted.

---

## 5. Update Incompatibility with Alpha.3 & Safe Migration

### Technical Cause
Android's Package Manager enforces cryptographic signature continuity. When replacing an existing app package (`com.nova.music`), Android verifies that the signature of the incoming APK matches the signature of the currently installed version.

Because Alpha.3 was signed with the debug key (`c7a1233...`) and future builds are signed with the production key (`7e38214...`), Android will reject direct updates with:
```
Failure [INSTALL_FAILED_UPDATE_INCOMPATIBLE: Existing package com.nova.music signatures do not match newer version; preserving data...]
```

### Safe User Migration Steps
When migrating to the production-signed release (e.g. Alpha.4):

1. **Uninstall the Legacy Alpha.3 Build:**
   - On Android: Go to **Settings > Apps > STEREO > Uninstall**.
   - Via ADB:
     ```bash
     adb uninstall com.nova.music
     # Or if debug variant was installed:
     adb uninstall com.nova.music.debug
     ```
2. **Install the Production-Signed Release:**
   - Download the new release APK.
   - Install via ADB or file manager:
     ```bash
     adb install -r STEREO-1.0.0-alpha.4.apk
     ```
3. **Future Continuity:**
   - All subsequent releases (Alpha.4, Beta, v1.0.0 final) will share this permanent production key, enabling seamless in-place updates.
