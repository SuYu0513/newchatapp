# DBの完全キャッシュクリーンスクリプト
# ※実行前に必ずバックアップを取ってください

Write-Host "=== データベース完全クリーン開始 ===" -ForegroundColor Cyan

# 1. Mavenプロセスを停止
Write-Host "`n1. 実行中のアプリケーションを停止中..." -ForegroundColor Yellow
Get-Process | Where-Object {$_.ProcessName -like "*java*" -or $_.ProcessName -like "*mvn*"} | ForEach-Object {
    Write-Host "  プロセス停止: $($_.ProcessName) (PID: $($_.Id))" -ForegroundColor Gray
    Stop-Process -Id $_.Id -Force -ErrorAction SilentlyContinue
}
Start-Sleep -Seconds 2

# 2. データベースファイルを削除
Write-Host "`n2. データベースファイルを削除中..." -ForegroundColor Yellow
$dbFiles = @(
    ".\chatapp.db",
    ".\chatapp.db-shm",
    ".\chatapp.db-wal"
)

foreach ($file in $dbFiles) {
    if (Test-Path $file) {
        Remove-Item $file -Force
        Write-Host "  削除: $file" -ForegroundColor Green
    } else {
        Write-Host "  存在しない: $file" -ForegroundColor Gray
    }
}

# 3. targetディレクトリをクリーン（コンパイル済みクラスも削除）
Write-Host "`n3. ビルドキャッシュをクリーン中..." -ForegroundColor Yellow
if (Test-Path ".\target") {
    Remove-Item ".\target" -Recurse -Force
    Write-Host "  削除: target ディレクトリ" -ForegroundColor Green
}

# 4. Mavenクリーン実行
Write-Host "`n4. Maven クリーンを実行中..." -ForegroundColor Yellow
& .\mvnw.cmd clean
if ($LASTEXITCODE -eq 0) {
    Write-Host "  Maven クリーン完了" -ForegroundColor Green
} else {
    Write-Host "  Maven クリーンに失敗しました" -ForegroundColor Red
}

Write-Host "`n=== データベース完全クリーン完了 ===" -ForegroundColor Cyan
Write-Host "`n次回起動時に新しいデータベースが作成されます。" -ForegroundColor Green
Write-Host "起動コマンド: .\mvnw.cmd spring-boot:run`n" -ForegroundColor White
