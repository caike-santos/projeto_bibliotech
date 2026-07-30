$frontend = "c:\Users\caike\OneDrive\Documentos\estudos\projeto_bibliotech\frontend"

$configContent = "const API_BASE_URL = 'https://bibliotech-api-e9wg.onrender.com';"
Set-Content "$frontend\config.js" $configContent -Encoding UTF8

$htmlFiles = Get-ChildItem "$frontend\*.html"
foreach ($file in $htmlFiles) {
    $content = Get-Content $file.FullName -Raw
    if ($content -notmatch "config.js") {
        $content = $content.Replace("</head>", "<script src=`"config.js`"></script>`r`n</head>")
        Set-Content $file.FullName $content -Encoding UTF8
    }
}

$jsFiles = Get-ChildItem "$frontend\*.js"
foreach ($file in $jsFiles) {
    if ($file.Name -ne "config.js") {
        $content = Get-Content $file.FullName -Raw
        $content = $content.Replace("'https://bibliotech-api-e9wg.onrender.com", "API_BASE_URL + '")
        $content = $content.Replace("`"https://bibliotech-api-e9wg.onrender.com", "API_BASE_URL + `"")
        $content = $content.Replace("``https://bibliotech-api-e9wg.onrender.com", "``${API_BASE_URL}")
        Set-Content $file.FullName $content -Encoding UTF8
    }
}
