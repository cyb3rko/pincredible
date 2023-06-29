## ⚠️Important Note - Breaking Changes in Final Release⚠️  
If you have never used PINcredible, you can skip this section.  
If you have already used this app, please note that your current PINs and even PIN backups will not work anymore in the stable release 1.0.0.  
That is because of an internal rehaul of the persisting features. I will switch from whole class serialization to custom serialization methods.

Before upgrading save your PINs to be able to reenter them in release 1.0.0.  
Thank you for your understanding!

---

<p align="center">
  <img alt="PINcredible" src="https://i.imgur.com/hwfoyYb.png" width="150"/>
</p>

<h1 align="center">PINcredible (Beta)</h1>

<p align="center">
    <font size="+1">Part of </font><a href="https://github.com/cyb3rko/backpack-apps"><font size="+1">BackPack</font></a>
</p>

[![API](https://img.shields.io/badge/API-23%2B-brightgreen.svg?style=flat)](https://apilevels.com)
[![release](https://img.shields.io/github/release/cyb3rko/pincredible.svg)](https://github.com/cyb3rko/pincredible/releases/latest)
[![fdroid](https://img.shields.io/f-droid/v/com.cyb3rko.pincredible.svg)](https://f-droid.org/packages/com.cyb3rko.pincredible)
[![license](https://img.shields.io/github/license/cyb3rko/pincredible)](https://www.apache.org/licenses/LICENSE-2.0)
[![last commit](https://img.shields.io/github/last-commit/cyb3rko/pincredible?color=F34C9F)](https://github.com/cyb3rko/pincredible/commits/main)

[![FOSSA Status](https://app.fossa.com/api/projects/custom%2B35689%2Fgithub.com%2Fcyb3rko%2Fpincredible.svg?type=small)](https://fossa.com/)

- [About this project](#about-this-project)  
- [Beta Phase - Breaking Changes](#beta-phase---%EF%B8%8Fbreaking-changes%EF%B8%8F)  
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

## Beta phase - ⚠️Breaking Changes⚠️

While the app is still in beta phase please expect a few breaking changes.  
Some beta updates do not work with the previous app versions.  
Therefore you may have to readd your saved PINs and recreate your backups in never versions.

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

Google Play download link available after beta phase

[<img height="80" alt="Get it on F-Droid"
src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png"
/>](https://f-droid.org/packages/com.cyb3rko.pincredible/)
[<img height="80" src="https://raw.githubusercontent.com/gotify/android/master/download-badge.png"/>](https://github.com/cyb3rko/pincredible/releases/latest)

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
```

The certificate content and digests should look like this:

```
DN: C=DE, CN=Niko Diamadis
Certificate Digests:
  SHA-256: 7b:d9:79:cd:5f:f9:29:e0:72:90:e8:8d:67:b2:d8:1f:22:8e:a2:64:e4:33:f7:84:e4:c6:63:73:e3:16:bc:ad
  SHA-1:   c7:52:14:9f:4d:c3:e4:02:26:92:0b:68:20:94:6e:da:99:01:69:29
  MD5:     8d:15:71:36:6e:30:7c:23:c9:2c:e8:9d:f2:38:5f:e1
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
- SHA512 (250.000 iterations; used for backup password inputs)

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

    Copyright 2023, Cyb3rKo

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
    
        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
