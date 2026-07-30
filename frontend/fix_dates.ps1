$frontend = "c:\Users\caike\OneDrive\Documentos\estudos\projeto_bibliotech\frontend"

$jsFiles = Get-ChildItem "$frontend\*.js"
foreach ($file in $jsFiles) {
    if ($file.Name -ne "toast.js" -and $file.Name -ne "config.js") {
        $content = Get-Content $file.FullName -Raw
        $newContent = [regex]::Replace($content, "new Date\((.*?)\)\.toLocaleDateString\(\)", "formatarDataLocal(`$1)")
        
        if ($newContent -cne $content) {
            Set-Content $file.FullName $newContent -Encoding UTF8
        }
    }
}
