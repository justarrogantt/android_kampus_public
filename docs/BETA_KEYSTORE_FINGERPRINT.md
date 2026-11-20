## Beta Keystore Fingerprint

Use this fingerprint to verify that local and CI beta APKs are signed with the same certificate.

- Alias: `ysukampus-beta`
- SHA1: `D0:24:A1:34:A7:B3:4E:DD:FB:29:23:DD:0A:6E:69:5F:F5:EF:E4:2E`
- SHA256: `41:52:1C:23:F3:A5:E8:C1:2C:80:27:41:25:C1:60:9B:30:DA:E6:97:17:80:2B:29:F2:84:70:D6:35:E9:18:E7`

The key was regenerated on 2026-09-12: the original keystore behind the previous
fingerprint (`7D:D6:...`) was lost and existed only as the `BETA_KEYSTORE_BASE64`
CI secret. Builds signed with the old key can no longer be upgraded in place —
`com.example.ysukampus.beta` had to be reinstalled once.
