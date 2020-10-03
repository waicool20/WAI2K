# WAI2K

A Girls Frontline automation tool

---
**The main purpose of this tool is to study the effectiveness of using various computer vision techniques 
to automate and optimize various aspects of video games. Any usage of this tool is at your own risk. 
No one will be responsible for anything that happens to you or your own account except for yourself.**

If you understand this then read on

---

# Features

- Logistics Support
- Combat Support (Limited to implemented maps)
  - Corpse dragging
- Auto enhancement
- Auto disassembly
- Auto combat reports, make kalina work overtime!
- Auto combat simulations
- [Discord integration](https://github.com/waicool20/WAI2K/wiki/Discord-Integration)
- Can run in background without taking over desktop mouse
- More to come

# Setup

Prerequisites:

* [Java JDK 8 ( Get the Full Version! )](https://bell-sw.com/pages/java-8u252/)
* [WAI2K-Launcher.jar](https://github.com/waicool20/WAI2K/releases/latest)
* Emulator, must be 2160x1080 resolution

Start the app by running the launcher jar file (Double click on windows), first launch will be longer due to automatic downloading of necessary files!

It is recommended to use the Android Studio Emulator to run the game.

There is a certain difficulty in setting up the script due to the specificity that the script requires in the environment it runs in
which is understandable, hopefully the articles below can help you.
You can always join our discord community (Invite below) if you have any further questions.

- [Emulator setup](https://github.com/waicool20/WAI2K/wiki#emulator-setup)

# Build Instructions

Prerequisites: 

* [Java JDK 8](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)

Git clone instructions: 

```bash
git clone https://github.com/waicool20/WAI2K.git      // Clone repository, replace with ssh url if you prefer that  
cd WAI2K/                                             // Make sure you are in the WAI2K directory
git submodule update --init --recursive               // Pull all submodules, this includes the utility library and cvauto
```

To build the jar file after cloning/pulling the latest commits, go into the repo directory and run the right command

Windows:

> gradlew.bat

Linux/MacOS:

> ./gradlew

A compiled Jar file will be generated in the build/libs directory

# Screenshots

Main Window - Status Tab:  
![Main Window - Status Tab](screenshots/main-status.png?raw=true)

Main Window - Device Tab:  
![Main Window - Device Tab](screenshots/main-device.png?raw=true)

Console Window (Logs):  
![Console Window](screenshots/console.png?raw=true)

# Want to chat?

Want to chat? Or just ask a quick question instead of submitting a full blown issue? Or maybe you just want to share your waifu...
well then you're welcome to join us in Discord:
 
[<img src="https://discordapp.com/assets/fc0b01fe10a0b8c602fb0106d8189d9b.png" alt="alt text" width="200px">](https://discord.gg/2tt5Der)


