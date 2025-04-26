## ⚠️ Breaking Changes ⚠️  
If you have never used PINcredible, you can skip this section.  
If you have already used this app, please note that:
- The stable release 1.0.0 uses a different package name and app signature, so you most have to uninstall the currently installed version and install the new version.
- Your data from app versions older than 1.0.0a will not work anymore beginning with version 1.0.0a.  
  That is because of an internal rehaul of the persisting features.

Before upgrading save your PINs outside of PINcredible to be able to reenter them in version 1.0.0a and later.  
Thank you for your understanding!

<h1 align="center">PINcredible - Secure PIN vault</h1>

<p align="center">
  <a href="https://apilevels.com"><img alt="API level" src="https://proxy.cyb3rko.de/shields/badge/API-23%2B-coral?logo=android&logoColor=white"></a>
  <a href="https://f-droid.org/packages/de.cyb3rko.flashdim"><img alt="F-Droid release" src="https://proxy.cyb3rko.de/shields/f-droid/v/de.cyb3rko.pincredible.svg?logo=fdroid&color=blue"></a>
  <a href="https://github.com/cyb3rko/pincredible/releases/latest"><img alt="GitHub release" src="https://proxy.cyb3rko.de/shields/github/v/release/cyb3rko/pincredible.svg?logo=github"></a>
  <a href="https://github.com/cyb3rko/pincredible/commits/main"><img alt="Last commit" src="https://proxy.cyb3rko.de/shields/github/last-commit/cyb3rko/pincredible?color=F34C9F&logo=git&logoColor=white"></a>
  <a href="https://conventionalcommits.org"><img alt="Conventional Commits" src="https://proxy.cyb3rko.de/shields/badge/Conventional%20Commits-1.0.0-%23FE5196?logo=conventionalcommits&logoColor=white">
  <a href="https://www.apache.org/licenses/LICENSE-2.0"><img alt="License" src="https://proxy.cyb3rko.de/shields/github/license/cyb3rko/pincredible?color=1BCC1B&logo=apache"></a>
</p>

<p align="center">
  <img alt="PINcredible logo" src="https://i.imgur.com/hwfoyYb.png" width="120"/>
</p>

<p align="center">
    <font size="+1">Part of </font><a href="https://github.com/cyb3rko/backpack-apps"><font size="+1">BackPack</font></a>
</p>

---

- [About this project](#about-this-project)  
- [Feature Overview](#feature-overview)  
  - [Accessible color palette](#accessible-color-palette)  
- [Legal Liability](#legal-liability)  
- [Download](#download)  
  - [Verification](#verification)  
- [Supported devices](#supported-devices)  
- [Screenshots](#screenshots)  
- [Security Aspects](#security-aspects)  
- [Contribute](#contribute)  
- [Used Icons](#used-icons)  
- [License](#license)

---

## About this project
Over time I've used several apps to store my PINs, unfortunately none of them really convinced me.  
So here we are now, this is my own implementation of a secure PIN manager.

## Feature Overview
| | PINcredible | Others |
| --- | --- | --- |
| 🔢 PIN obfuscation | ✅ | ✅ |
| 📂 Open Source | ✅ | ❌ |
| 🔐 Local Encryption | ✅ | ❌ |
| 🎨 Modern Design | ✅ | ❌ |
| 🌐 Internet Connection | ❌ | ✅ |
| 🎞️ Ads | ❌ | ✅ |
| 🗿 Suspicious Permissions | ❌ | ✅ |

---

The app obfuscates the PIN in a table layout surrounded by secure random numbers.  
This brings two security benefits:
1. ❔ The app can not know where in the pattern the user given PIN is located at. Attackers can not extract the plaintext PIN.
2. 🕵️ This offers protection against [Shoulder Surfing](https://en.wikipedia.org/wiki/Shoulder_surfing_(computer_security)), for example while accessing your PIN in a super market or a bank.

---

For the input of PIN digits the app uses an in-app keyboard.  
This brings the following two security benefits:
1. ⌨️ (At least some) protection against [keylogging](https://en.wikipedia.org/wiki/Keystroke_logging)
2. 📱 (Optional) protection against touch location logging (by shuffling digit keyboard buttons)

### Accessible color palette

In addition to the default color palette PINcredible offers an accessible color palette (following the [IBM Color Blindness Palette](https://davidmathlogic.com/colorblind/#%23648FFF-%23785EF0-%23DC267F-%23FE6100-%23FFB000)).  

## Legal Liability

In no way do I accept liability for lost PINs and the resulting consequences or other consequences of using the app.  
Especially in the beta phase, but also afterwards, I do not guarantee that the app will always work properly and PINs will never be lost.

If you don't agree please don't use this app.

## Download

[<img height="80" alt="Get it on F-Droid" src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png"/>](https://f-droid.org/app/de.cyb3rko.pincredible)
[<img height="80" alt="Get it on GitHub" src="https://raw.githubusercontent.com/NeoApplications/Neo-Backup/034b226cea5c1b30eb4f6a6f313e4dadcbb0ece4/badge_github.png"/>](https://github.com/cyb3rko/pincredible/releases/latest)

Google Play release planned

### Verification

APK releases on F-Droid and GitHub are signed using the same key. They can be verified using [apksigner](https://developer.android.com/studio/command-line/apksigner.html#options-verify):

```
apksigner verify --print-certs -v example.apk
```

The output should look like:

```
Verifies
Verified using v1 scheme (JAR signing): true
Verified using v2 scheme (APK Signature Scheme v2): true
Verified using v3 scheme (APK Signature Scheme v3): true
```

The certificate content and digests should look like this:

```
DN: OU=PINcredible, O=Cyb3rKo OpenSource, L=GitHub / F-Droid, C=DE
Certificate Digests:
  SHA-256: 77:15:66:40:38:23:38:2c:74:27:4e:fb:33:d1:f2:72:5b:9e:4e:67:8b:6b:2f:af:3b:ce:a9:fe:e8:f2:a9:5e
  SHA-1:   30:12:e7:60:37:27:fa:83:c5:db:b4:6b:7d:22:d8:79:0b:4d:a7:d1
  MD5:     67:bb:02:ca:3c:ba:20:63:f7:a8:1c:0f:88:dd:59:38
```

## Supported Devices
The minimum supported Android version is API level 23, Android 6 (Marshmallow).  
Additionally this app takes advantage of the Android KeyStore system. At the moment I'm assuming every Android device with Android 6 upwards has this built-in.  
If you have any problems, maybe even because your device seems to be incompatible, please leave a message [here](https://github.com/cyb3rko/pincredible/issues).

## Screenshots
|<img src="https://i.imgur.com/APgDeAl.png" width="270">|<img src="https://i.imgur.com/WHCXpG3.png" width="270">|<img src="https://i.imgur.com/zPESUDi.png" width="270">|
|:---:|:---:|:---:|

## Security Aspects
Let's take a look at the technical details.

At first here are the algorithms used:
- AES/GCM/NoPadding (Advanced Encryption Standard in Galois/Counter Mode)
- XXH128 (XXHash3-128) [[xxHash Repo](https://github.com/Cyan4973/xxHash), thanks to [Matthew Dolan](https://github.com/mattmook) for the [Kotlin implementation](https://github.com/appmattus/crypto/tree/main/cryptohash/src/commonMain/kotlin/com/appmattus/crypto/internal/core/xxh3)]
- Argon2id (used for backup password inputs)

For easier understanding how the app works internally I've created the following diagram.  
Find the detailed explanation below.

<img src="https://i.imgur.com/ifWt3Vc.png">

---

**So what's happening here?**

### 1. App Start  
- retrieval of symmetric AES key and encrypted file containing available PIN names
- decryption of the file contents
- presenting available PIN names on screen

### 2. Clicking on a PIN  
- handing over PIN name to next screen and hashing it (XXHash)
- find corresponding file containing encrypted PIN pattern (including colors)
- retrieval of symmetric AES key and encrypted file containing PIN pattern
- presenting decrypted PIN pattern in table view

### 3. Clicking on 'add' button (PIN creation)
- decide rather you want to use the initial color pattern or generate a new one (using standard random numbers, no SecureRandom here as it's not cryptographically relevant)
- fill in your PIN somewhere and fill the remaining empty cells (using SecureRandom provided by your device)
- type in a custom name, it will be hashed and used as the file name
- retrieval of symmetric AES key
- encrypt and save PIN pattern to file, append chosen PIN name to PIN name file (for the home screen)

That's the whole magic behind PINcredible, if you have questions or if you are a Security Expert and you have recommendations for improving the overall security, please tell me [via the issues](https://github.com/cyb3rko/pincredible/issues) or via e-mail:  niko @ cyb3rko.de.

## Contribute
Of course I'm happy about any kind of contribution.

For creating [issues](https://github.com/cyb3rko/pincredible/issues) there's no real guideline you should follow.
If you create [pull requests](https://github.com/cyb3rko/pincredible/pulls) please try to use the syntax I use.
Using a unified code format makes it much easier for me and for everyone else.

## Used Icons

| 💛 |
| --- |  
| <a href="https://www.flaticon.com/free-icons/color-blindness-test" title="color-blindness-test icons">Color-blindness-test icons created by Freepik - Flaticon</a> |
| <a href="https://www.flaticon.com/free-icons/grid" title="grid icons">Grid icons created by prettycons - Flaticon</a> |
| <a href="https://www.flaticon.com/free-icons/random" title="random icons">Random icons created by Uniconlabs - Flaticon</a> |

## License

    Copyright 2023-2025, Cyb3rKo

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
    
        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
