# Reference APK inventory

This document records metadata extracted from a signed APK supplied for compatibility research. The APK itself and decompiled sources are intentionally not committed.

## Guise 1.1.2 functional baseline

- Original filename: `Guise-1.1.2.release.apk`
- SHA-256: `B9190E71967F61AA3A4BBC3723E02D383CEF916B3FFC2096E1498FFA54261510`
- Package: `com.houvven.guise`
- Version: `1.1.2` (`versionCode` 12)
- Minimum SDK: 29
- Target/compile SDK: 33
- Native ABI: `arm64-v8a`, `armeabi-v7a`, `x86`, `x86_64`
- Xposed entry: `com.houvven.guise.xposed.HookInit`
- Certificate subject: `C=CHN`
- Certificate SHA-256: `4fefd82891f075e98cc028221bab64e981f55809cf2c87ff80ae36d1a04ccb6e`

The APK version matches `upstream/v1` commit `4e61662`. Its `assets/devices.db` and `assets/xposed_init` are byte-for-byte identical to that source snapshot, and `app/version.properties` contains the same version code and name. This source is therefore used as the complete functional baseline for Guise Reborn.

## Guise 2.0.0-alpha reference

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
