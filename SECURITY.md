# Security Policy

## Supported Versions

Security updates and vulnerability patches are actively provided for the following versions of STEREO:

| Version | Supported | Status |
| :--- | :---: | :--- |
| `1.0.0-alpha.3` | :white_check_mark: | Current active release |
| `< 1.0.0-alpha.3` | :x: | Deprecated / Unsupported |

---

## Reporting a Vulnerability

The STEREO team takes security and privacy seriously. If you discover a security vulnerability, please report it privately rather than opening a public issue.

### How to Report

1. **Email:** Send details to `rishibandaru.3109@gmail.com` with the subject line `[SECURITY] STEREO Vulnerability Report`.
2. **Include Details:**
   - Detailed description of the vulnerability.
   - Exact steps or proof-of-concept to reproduce the issue.
   - Affected component(s) (e.g., Media3 playback pipeline, Room database, stream extraction, network communication).
   - Your assessment of the impact.

### Response Timeline

- **Acknowledgment:** Within 48 hours.
- **Assessment & Triage:** Within 5 business days.
- **Fix & Public Advisory:** Coordinated release once a patch is tested and verified.

---

## Threat Model & Privacy Considerations

- **Local Storage:** All listening history, playlists, liked songs, and taste profiles are stored strictly on-device in a local SQLite/Room database. No personal data is transmitted to external servers.
- **Network Requests:** Audio stream extraction and recommendation candidate retrieval interact directly with public YouTube Music endpoints. No authentication tokens or credentials are stored or transmitted.
- **No Third-Party Trackers:** STEREO contains zero analytics SDKs, telemetry, advertising libraries, or third-party tracking scripts.
