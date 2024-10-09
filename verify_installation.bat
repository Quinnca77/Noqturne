@echo off
setlocal

echo [INFO] Verifying installations...
python --version
IF %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Python installation failed.
) ELSE (
    echo [SUCCESS] Python is installed.
)

java --version
IF %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Java installation failed.
) ELSE (
    echo [SUCCESS] Java is installed.
)

yt-dlp --version
IF %ERRORLEVEL% NEQ 0 (
    echo [ERROR] yt-dlp installation failed.
) ELSE (
    echo [SUCCESS] yt-dlp is installed.
)

ffmpeg -version
IF %ERRORLEVEL% NEQ 0 (
    echo [ERROR] ffmpeg installation failed.
) ELSE (
    echo [SUCCESS] ffmpeg is installed.
)
pause