@echo off
title OrdenExpress Muebleria
echo ===================================================
echo   Iniciando OrdenExpress Muebleria (Backend + Frontend)
echo ===================================================
echo Iniciando Backend (Spring Boot en puerto 8080)...
start "Backend Spring Boot" cmd /k "cd /d "%~dp0backend" && mvnw.cmd spring-boot:run"

echo Iniciando Frontend (Vite en puerto 5173)...
start "Frontend Vue 3" cmd /k "cd /d "%~dp0frontend" && npm run dev"

echo.
echo Todo listo. El frontend se abrira automaticamente en tu navegador.