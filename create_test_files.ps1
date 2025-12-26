# Resume Support Test Setup Script
# This script creates test files of various sizes for testing resume functionality

Write-Host "==================================" -ForegroundColor Cyan
Write-Host "Resume Support Test File Creator" -ForegroundColor Cyan
Write-Host "==================================" -ForegroundColor Cyan
Write-Host ""

$desktopPath = [Environment]::GetFolderPath("Desktop")

# Test file configurations
$testFiles = @(
    @{Name="small_test.bin"; Size=1048576; SizeName="1 MB"},      # 1 MB
    @{Name="medium_test.bin"; Size=10485760; SizeName="10 MB"},   # 10 MB
    @{Name="large_test.bin"; Size=52428800; SizeName="50 MB"},    # 50 MB
    @{Name="huge_test.bin"; Size=104857600; SizeName="100 MB"}    # 100 MB
)

Write-Host "Creating test files on Desktop..." -ForegroundColor Yellow
Write-Host ""

foreach ($file in $testFiles) {
    $filePath = Join-Path $desktopPath $file.Name
    
    Write-Host "Creating $($file.Name) ($($file.SizeName))..." -NoNewline
    
    try {
        # Create the file
        fsutil file createnew $filePath $file.Size | Out-Null
        
        if (Test-Path $filePath) {
            Write-Host " ✓ Created" -ForegroundColor Green
            
            # Calculate hash for verification
            $hash = (Get-FileHash $filePath -Algorithm SHA256).Hash
            Write-Host "  SHA-256: $hash" -ForegroundColor Gray
        } else {
            Write-Host " ✗ Failed" -ForegroundColor Red
        }
    } catch {
        Write-Host " ✗ Error: $($_.Exception.Message)" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "==================================" -ForegroundColor Cyan
Write-Host "Test files created successfully!" -ForegroundColor Green
Write-Host "==================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "📁 Location: $desktopPath" -ForegroundColor Yellow
Write-Host ""
Write-Host "Next Steps:" -ForegroundColor Cyan
Write-Host "1. Start the server: cd server && mvn javafx:run" -ForegroundColor White
Write-Host "2. Start client 1: cd client && mvn javafx:run" -ForegroundColor White
Write-Host "3. Start client 2: cd client && mvn javafx:run" -ForegroundColor White
Write-Host "4. Send one of the test files" -ForegroundColor White
Write-Host "5. Interrupt the transfer (close sender)" -ForegroundColor White
Write-Host "6. Restart sender and resend the same file" -ForegroundColor White
Write-Host "7. Look for [RESUME] messages in console" -ForegroundColor White
Write-Host ""
Write-Host "Recommended test file: large_test.bin (50 MB)" -ForegroundColor Yellow
Write-Host ""
