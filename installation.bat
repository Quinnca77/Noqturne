@echo off
setlocal

echo ======================================================
echo         Automated Setup Script for Auto-Tagger
echo ======================================================

:: Download yt-dlp if not already in the directory
if not exist "yt-dlp.exe" (
    echo [INFO] Downloading yt-dlp...
    curl -L -o yt-dlp.exe https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp.exe
) else (
    echo [INFO] yt-dlp is already installed
)

if not exist ffmpeg.exe (
    if not exist ffmpeg.zip (
        REM Download ffmpeg
        echo [INFO] Downloading ffmpeg...
        curl -L -o ffmpeg.zip https://github.com/yt-dlp/FFmpeg-Builds/releases/latest/download/ffmpeg-master-latest-win64-gpl.zip
        echo [INFO] Download successful!
    )
    if not exist ffmpeg-master-latest-win64-gpl\ (
        REM Extract ffmpeg using PowerShell
        echo [INFO] Extracting ffmpeg...
        powershell -Command "Expand-Archive -Path 'ffmpeg.zip' -DestinationPath '.'"
        echo [INFO] Extraction ffmpeg successful!
    )

    REM Move ffmpeg.exe and ffprobe.exe to the project directory
    cd ffmpeg-master-latest-win64-gpl\bin\
    move ffmpeg.exe ..\
    move ffprobe.exe ..\
    cd ..
    move ffmpeg.exe ..\
    move ffprobe.exe ..\
    cd ..

    REM delete unnecessary files
    echo [INFO] Deleting temporary files
    rmdir /S /Q ffmpeg-master-latest-win64-gpl\

    REM REMOVE FILE FFMPEG.ZIP
    del ffmpeg.zip
    echo [INFO] Deletion temporary files successful!
) else (
    echo [INFO] ffmpeg is already installed
)

:: Install Python dependencies (ytmusicapi)
echo [INFO] Installing Python dependencies (ytmusicapi)...
pip install ytmusicapi
echo [INFO] Installing Python dependencies successful!

echo ======================================================
echo               Installation complete!
echo ======================================================
pause
