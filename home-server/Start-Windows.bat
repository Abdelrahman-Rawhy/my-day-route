@echo off
title My Day Route - Home Server
cd /d "%~dp0"
where python >nul 2>nul || (echo Python is not installed. Get it free from https://www.python.org/downloads/ and tick "Add Python to PATH". & pause & exit /b)
python mdr_home_server.py
pause
