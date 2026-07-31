# Reference APK inventory

This document records metadata extracted from a signed APK supplied for compatibility research. The APK itself and decompiled sources are intentionally not committed.

## Artifact

- Original filename: `com.houvven.guise.apk`
- SHA-256: `0230E97816A0220C324782C796AB47D3037D9CE334E0BD18884F966B714F2B17`
- Size: 3,049,266 bytes
- Package: `com.houvven.guise`
- Version: `2.0.0-alpha` (`versionCode` 201)
- Minimum SDK: 24
- Target SDK: 35
- Compile SDK: 34
- Native ABI: `arm64-v8a`

The public source tree already used `versionCode` 203 when it was taken over, so this APK is an older build rather than a newer unpublished version.

## Signature

- Certificate subject: `C=zh_cn, CN=Houvven`
- Certificate SHA-256: `f7aa2cd61d9f4a1e0604d33bbb0f4fa90916274a37ddcbfed857683c6797dbfc`
- Public key: RSA 2048-bit
- Verified schemes: APK Signature Scheme v2 and v3

The original signing key is not available in this repository. Reborn builds therefore cannot update this APK in place unless the original signing key is provided.

## Packaging observations

- Legacy Xposed entry: `com.houvven.guise.hook.HookEntry_YukiHookXposedInit`
- Xposed minimum API: 93
- Xposed description: `God only knows`
- Contains `assets/lservice/manager.apk`
- Contains `assets/main.jar`, used by libsu RootService
- Contains one main DEX and baseline profile assets
- R8/minification has obfuscated almost all implementation classes

The package identity, Xposed entry, bundled manager APK and application structure match the public source architecture. This APK is useful as a behavioral and packaging reference, but it does not currently indicate additional functionality absent from the source tree.
