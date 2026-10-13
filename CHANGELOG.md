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
