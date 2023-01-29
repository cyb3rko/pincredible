# PINcredible (*Beta*)
[![API](https://img.shields.io/badge/API-23%2B-brightgreen.svg?style=flat)](https://apilevels.com)
[![release](https://img.shields.io/github/release/cyb3rko/pincredible.svg)](https://github.com/cyb3rko/pincredible/releases/latest)
[![license](https://img.shields.io/github/license/cyb3rko/pincredible)](https://www.apache.org/licenses/LICENSE-2.0)
[![last commit](https://img.shields.io/github/last-commit/cyb3rko/pincredible?color=F34C9F)](https://github.com/cyb3rko/pincredible/commits/main)

![pincredible banner](https://socialify.git.ci/cyb3rko/pincredible/image?description=1&forks=1&issues=1&logo=https%3A%2F%2Fi.imgur.com%2FJ4rQhXH.png&name=1&owner=1&pattern=Brick%20Wall&pulls=1&stargazers=1&theme=Dark)

- [About this project](#about-this-project)   
- [Feature Overview](#feature-overview)  
- [Download](#download)
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

The app obfuscates the PIN in a table layout surrounded by secure random numbers.  
This brings two security benefits:
1. ❔ The app can not know where in the pattern the user given PIN is located at. Attackers can not extract the plaintext PIN.
2. 🕵️ This offers protection against [Shoulder Surfing](https://en.wikipedia.org/wiki/Shoulder_surfing_(computer_security)), for example while accessing your PIN in a super market or a bank.

## Download

Google Play and F-Droid download links coming soon

## Supported Devices
The minimum supported Android version is API level 23, Android 6 (Marshmallow).  
Additionally this app takes advantage of the Android KeyStore system. At the moment I'm assuming every Android device with Android 6 upwards has this built-in.  
If you have any problems, maybe even because your device seems to be incompatible, please leave a message [here](https://github.com/cyb3rko/pincredible/issues).

## Screenshots
|<img src="https://i.imgur.com/g5v30RS.png" width="270">|<img src="https://i.imgur.com/jxzmJAk.png" width="270">|<img src="https://i.imgur.com/NSTmKss.png" width="270">|
|:---:|:---:|:---:|

## Security Aspects
Let's take a look at the technical details.

At first here are the algorithms used:
- XXH128 (XXHash3-128) [[xxHash Repo](https://github.com/Cyan4973/xxHash), thanks to [Matthew Dolan](https://github.com/mattmook) for the [Kotlin implementation](https://github.com/appmattus/crypto/tree/main/cryptohash/src/commonMain/kotlin/com/appmattus/crypto/internal/core/xxh3)]
- AES/CBC/PKCS7 (Advanced Encryption Standard in CBC mode with PKCS7 padding)

For easier understanding how the app works internally I've created the following diagram.  
Find the detailed explanation below.

<img src="https://i.imgur.com/ifWt3Vc.png">

---

**So what's happening here?**

1. **App Start**  
When you open the app the home screen shows up and lists the stored PINs. In the background the symmetric AES key from the Android Keystore and the encrypted file containing the available PIN names are retrieved. After decrypting we have the available PIN names to show in the list.
2. **Clicking on a PIN**  
The next step would be clicking on a listed PIN name. It is handed over to the next screen and gets hashed by xxHash.  
This hash can then be used to find the corresponding file in the file system containing the encrypted PIN pattern (including colors).  
Again the AES key is retrieved and used to decrypt the retrieved file. The app is now ready to process the decrypted data and present you the stored PIN pattern in the table view.
3. **Clicking on 'add' button**  
The last step I want to talk about is the creation of new PIN patterns:  
The creation screen contains an empty but colored table view. First you decide rather you want to use the initial color pattern or let the app generate a new one (using standard random numbers, no SecureRandom here as it's not cryptographically relevant).  
When you've chosen a fitting pattern you start by putting in as many digits as you want into the table. Clicking the 'fill' button will then fill the remaining empty cells with SecureRandom numbers provided by your device.  
Before saving the PIN pattern you type in a custom name, which again will be hashed and used as the file name.  
Finally we again retrieve the AES key to encrypt the new PIN pattern file, save it in the file system and append the chosen PIN name to the PIN name file for your home screen.

That's the whole magic behind PINcredible, if you have questions or if you are a Security Expert and you have recommendations for improving the overall security, please tell me [via the issues](https://github.com/cyb3rko/pincredible/issues) or via e-mail:  niko @ cyb3rko.de.

## Contribute
Of course I'm happy about any kind of contribution.

For creating [issues](https://github.com/cyb3rko/pincredible/issues) there's no real guideline you should follow.
If you create [pull requests](https://github.com/cyb3rko/pincredible/pulls) please try to use the syntax I use.
Using a unified code format makes it much easier for me and for everyone else.

## Used Icons

| 💛 |
| --- |  
| <a href="https://www.flaticon.com/free-icons/information" title="information icons">Information icons created by Freepik - Flaticon</a> |
| <a href="https://www.flaticon.com/free-icons/security" title="security icons">Security icons created by Freepik - Flaticon</a> |
| <a href="https://www.flaticon.com/free-icons/safe-box" title="safe box icons">Safe box icons created by juicy_fish - Flaticon</a> |
| <a href="https://www.flaticon.com/free-icons/next" title="next icons">Next icons created by Roundicons - Flaticon</a> |
| <a href="https://www.flaticon.com/free-icons/branch" title="branch icons">Branch icons created by Creatype - Flaticon</a> |

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
