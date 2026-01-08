Write-Host "Starting full stack (backend + frontend) ..." -ForegroundColor Cyan

docker compose down | Out-Null
docker compose up -d --build

Write-Host ""
Write-Host "All containers are up." -ForegroundColor Green
Write-Host "Open: http://localhost:5173" -ForegroundColor Yellow
Write-Host ""

docker compose ps