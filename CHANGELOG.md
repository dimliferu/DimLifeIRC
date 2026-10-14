# DimLife IRC — Changelog

## 0.1.105 — 2026-10-14

- Fix legacy quit message migration match (3d1a8cc)

# DimLife IRC — Changelog

## 0.1.102 — 2026-10-14

- Use exact requested mIRC formatting for quit message (5f8ad6e)

# DimLife IRC — Changelog

## 0.1.101 — 2026-10-14

- Match branded quit and version colors to reference (54e894a)

# DimLife IRC — Changelog

## 0.1.100 — 2026-10-14

- Brand and color IRC quit and CTCP replies (ed52b28)

# DimLife IRC — Changelog

## 0.1.99 — 2026-10-14

- Update CTCP client identity to DimLife (0084269)

# DimLife IRC — Changelog

## 0.1.98 — 2026-10-14

- Apply first-run nick consistently to all IRC profiles (592393d)

# DimLife IRC — Changelog

## 0.1.96 — 2026-10-14

- fix: use DimLife realname as network default (38b698b)

# DimLife IRC — Changelog

## 0.1.94 — 2026-10-13

- ci: update changelog automatically on releases (2f56b29)

# DimLife IRC — Changelog

## 0.1.0

- Initial DimLife IRC branded build based on the upstream open-source IRC client.
- Dark interface with neon-green DimLife accents.
- DimLife IRC application identity, launcher icon and welcome branding.
- Automated Android APK builds through GitHub Actions.
- Automatic version numbering for CI builds and tag-based GitHub Releases.
- Existing IRC connectivity and messaging features retained during the rebrand.

## Release process

- Pushes to `main` build installable Debug APKs automatically.
- Version tags such as `v0.2.0` trigger a release build.
- Signed public releases require a persistent signing keystore configured in GitHub Actions secrets.

DimLife IRC preserves required upstream copyright notices and license terms. See [README.md](README.md) and [LICENSE](LICENSE).
