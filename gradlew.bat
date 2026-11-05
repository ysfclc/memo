@echo off
setlocal EnableExtensions
set "VERSION=8.14.6"
set "SHA256=7988ed071b2a07900e2ec715fca15c6ab72bce6db433af301fa7fa7e9407bd24"
set "URL=https://services.gradle.org/distributions/gradle-8.14.6-bin.zip"

where java >nul 2>nul
if errorlevel 1 (
  echo Java 17 or newer is required. Install JDK 17 and retry. 1>&2
  exit /b 1
)
where gradle >nul 2>nul
if not errorlevel 1 (
  gradle --version | findstr /R /C:"^Gradle 8\.14\.6$" >nul
  if not errorlevel 1 goto use_path_gradle
)

set "CACHE=%USERPROFILE%\.gradle\synthos-distributions"
set "INSTALL=%CACHE%\gradle-%VERSION%"
set "ARCHIVE=%CACHE%\gradle-%VERSION%-bin.zip"
if exist "%INSTALL%\bin\gradle.bat" goto use_bootstrap
if not exist "%CACHE%" mkdir "%CACHE%"
echo Preparing verified Gradle %VERSION% distribution...
powershell -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference='Stop'; if (!(Test-Path '%ARCHIVE%') -or ((Get-FileHash -Algorithm SHA256 '%ARCHIVE%').Hash.ToLowerInvariant() -ne '%SHA256%')) { if (Test-Path '%ARCHIVE%') { Remove-Item '%ARCHIVE%' -Force }; Invoke-WebRequest -Uri '%URL%' -OutFile '%ARCHIVE%' }; $actual=(Get-FileHash -Algorithm SHA256 '%ARCHIVE%').Hash.ToLowerInvariant(); if ($actual -ne '%SHA256%') { Remove-Item '%ARCHIVE%' -Force; throw 'Gradle distribution checksum mismatch.' }; Expand-Archive -Path '%ARCHIVE%' -DestinationPath '%CACHE%' -Force"
if errorlevel 1 exit /b 1
:use_bootstrap
call "%INSTALL%\bin\gradle.bat" %*
exit /b %ERRORLEVEL%
:use_path_gradle
gradle %*
exit /b %ERRORLEVEL%
