# Auto-tagger 
[![Github Quinnca77](https://img.shields.io/badge/Github-Quinnca77-black?logo=github)](https://github.com/Quinnca77)
![IDE](https://img.shields.io/badge/IDE-IntelliJ-blue?logo=intellijidea)
![OS](https://img.shields.io/badge/OS-Windows-blue)
![Apache Maven](https://img.shields.io/badge/Apache_Maven-red?logo=apachemaven)
![Python](https://img.shields.io/badge/Python-v3.9-blue?logo=python)
![License](https://img.shields.io/badge/License-GPL--3.0-yellow)
[![Ko-fi](https://img.shields.io/badge/Ko--Fi-Buy_me_a_coffee-ff5f5f?logo=ko-fi&style=for-the-badgeKofi)](https://ko-fi.com/quinnca77)

This Windows application aims to provide an automated way of downloading and tagging your songs from YouTube. By just supplying it with a playlist URL, it can download all of its songs
and give them the appropriate cover art quickly without needing any user interaction.

### Dependencies
To retrieve the cover art, a python library called `ytmusicapi` is used. Its repository can be found [here](https://github.com/sigma67/ytmusicapi). 

Also, `yt-dlp` is used to download songs from YouTube. Their repository can be found [here](https://github.com/yt-dlp/yt-dlp). 

That's not all. When converting the video downloaded from YouTube to `mp3` format, it uses `ffmpeg` to perform this conversion. Their site can be found [here](https://www.ffmpeg.org/).

## Installation
You have a few options to choose from when installing this application:
- Downloading the latest stable release
  - Download the .exe file in the latest release together with the python file
  - Download the .jar file in the latest release together with the python file (needs a valid java installation to run)

As long as you have both Python and Java installed on your machine, you can simply run the file [installation.bat](installation.bat). This is strongly recommended in order to set up your environment
exactly in the intended way. Do not be alarmed by the command prompt that gets opened by the program. If you wish to install things manually anyway (or still need to install Python and/or Java), 
you can go through the steps below.
### Installing Python
You can skip this step if you already have Python installed.

Go to [Python's download page](https://www.python.org/downloads/) and download the latest version. If you prefer an older version, any version >=3.9 should be fine.
> [!IMPORTANT]
> **Be sure to check the box that says to add Python to the PATH variable!!!**

Follow the rest of the installation instructions. You can verify that Python was successfully installed by opening a command prompt (Windows + Start, type `cmd`) and typing in
```
python --version
```
If this does not return an error, you're good.

### Installing necessary Python dependencies
Fortunately, this project only has one. If you already know what you are doing, you can create a virtual environment within the project directory which has `ytmusicapi` installed
as a Python library, but if you don't know what that means, running the following line in a command prompt will do the job for you as well:
```
pip install ytmusicapi
```


### Installing Java
Go to the [Java Download page](https://www.java.com/en/download/) and follow the installation instructions. In case you struggle with this you can follow this guide 
[here](https://phoenixnap.com/kb/install-java-windows). To verify your installation, you can run the following line in a command prompt:
```
java --version
```
Once again, if it doesn't return an error you're good.

### Installing yt-dlp
Now we are getting to the interesting stuff. Go to their [repository releases](https://github.com/yt-dlp/yt-dlp/releases) and from their latest release, download the `yt-dlp.exe` file. 
Drop this file in the same directory that you installed this repository in (whichever way of installing this repository you chose above doesn't matter). To verify you did this correctly,
go to the directory of this project and open a command prompt (this can be done quickly by going to the directory in File Explorer and typing `cmd` in the filepath textbox). Run:
```
yt-dlp --version
```
Once again, if this doesn't return an error, you get the drill.

### Installing ffmpeg
We have arrived at the last dependency! I hope you stuck with me so far. You can either download the official `ffmpeg` build from their [official website](https://www.ffmpeg.org/) or you can
download the custom one made by the maintainers of `yt-dlp` which can be found [here](https://github.com/yt-dlp/FFmpeg-Builds/releases).
> [!NOTE]
> I recommend downloading the one from `yt-dlp` themselves, since they mention it works better with their software.

That being said, I will also only specify installation instructions for the `yt-dlp` ffmpeg build. Navigate to their releases and download the zip file applicable to your Windows machine 
(when in doubt, just pick the one that has both "win" and "64" in there). Unzip this zip file somewhere on your device and open the `bin` folder. Copy the executable files within to the 
source directory of the auto-tagger project.

Now you're done installing and ready to use this application!

## Usage
This application has three main use cases:
- Tag all `mp3` files in the Downloads folder
- Download a song or playlist of songs and tag them immediately after
- Tag an individual song (any directory) with the thumbnail of a specified YouTube video

All of these are very straight-forward, but be aware that the cover art finding is done on the **mp3 file title!**. If your mp3 file title is complete nonsense, expect for the found cover art
to match that nonsense. 

> [!TIP]
> Use the `Rename file?` option to rename all files before they are tagged. Giving your songs sensible names in this window will make sure the cover art finder works optimally.

In case the cover art finder finds an entirely different cover art than intended, try finding the song on [YouTube Music](https://music.youtube.com/) yourself with the given artist name and
title. If you do not see the cover art that the cover art finder found, but instead the correct cover art, you can [contact](#contact) me with the details of your query and I'll see if I can
improve the algorithm somehow.

## Contributions
I am currently not accepting external contributions to this repository. If you really want to make changes yourself, please make a fork or open a GitHub issue if it's about a bug. 
No guarantees are given on the time taken for a bug to be patched, this is a hobby project after all.

## FAQ
#### Is this still being maintained?
Yes, although the last commit might have been a while ago. As long as there are still bugs to be fixed and improvements to be made (AKA always), I will be maintaining this project until stated otherwise.
#### I cannot get this project to work for me
Technically, that's not even a question, but I'll let that slip for now. Try to follow the steps in [Installation](#installation) as closely as possible and if you still cannot figure out why things 
don't work, you can always contact me.
#### Is there not a better way to find cover art than just checking the filename?
I have thought about this for a long time actually, and found that there is no more reliable way to do it (that I seem to be capable of anyway). In very early iterations of this application,
I used the [MusicBrainz API](https://musicbrainz.org/doc/MusicBrainz_API) to find a song and tag it with the appropriate cover art. This worked even with nonsense filenames, since it used the
contents of the song, the actual music, to find the song in their database. The only reason I did not use this method is that I found their database to be extremely lacking in its
coverage of songs. It seemed like the moment a song was even remotely unpopular, niche or unofficial in any way, MusicBrainz would not have an entry for it in their database. 
So I went with YouTube instead, which seems to have a way bigger database of songs than MusicBrainz.
#### Why the Downloads folder specifically?
This started out as a hobby project and I keep my Downloads folder clean. This made it a prime location for tagging `mp3` files in since they will be downloaded there anyway, saving me the effort
of relocating the songs to a different directory before tagging them.
#### My Downloads folder is a mess. Can I specify a different folder?
Currently, no. I do not see this being implemented in the near future either, so maybe consider cleaning it up?
#### I want to tag a song that is not an mp3 file. Can I do this?
Unfortunately, this application is made with only mp3 files in mind. Adding support for other extensions as well would be a big endeavour, and although I wouldn't exclude the possibility of this
being added in the future, I wouldn't count on it.
#### Will there ever be a Linux or Mac release?
No.

## Disclaimer 
This repository is for **educational and personal use** only. You are solely responsible for ensuring your use complies with all applicable laws and the terms of service of the websites you access. 
Unauthorized downloading or distribution of copyrighted content is **illegal**. The repository maintainers take **no responsibility** for misuse or violations of third-party terms of service.

## Contact
You can always contact me on my personal email <quinn.caris.github@gmail.com>. For business enquiries, my LinkedIn profile is linked (get it?) in my profile.
