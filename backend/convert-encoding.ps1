$content = Get-Content "src/test/java/com/docassistant/document/service/DocumentServicePropertyTest.java" -Raw -Encoding Default
$Utf8NoBomEncoding = New-Object System.Text.UTF8Encoding $False
[System.IO.File]::WriteAllLines("$PWD/src/test/java/com/docassistant/document/service/DocumentServicePropertyTest.java", $content, $Utf8NoBomEncoding)
Write-Host "File encoding converted to UTF-8"
