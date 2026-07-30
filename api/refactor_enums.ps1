$api_path = "c:\Users\caike\OneDrive\Documentos\estudos\projeto_bibliotech\api\src\main\java\com\bibliotech\api"

function Replace-In-File {
    param([string]$path, [string]$old, [string]$new)
    $content = Get-Content $path -Raw
    $content = $content -replace [regex]::Escape($old), $new
    Set-Content $path $content -Encoding UTF8
}

Replace-In-File "$api_path\service\EmprestimoService.java" '"ATIVO"' 'com.bibliotech.api.model.EmprestimoStatus.ATIVO'
Replace-In-File "$api_path\service\EmprestimoService.java" '"ATRASADO"' 'com.bibliotech.api.model.EmprestimoStatus.ATRASADO'
Replace-In-File "$api_path\service\EmprestimoService.java" '"AGUARDANDO_RETIRADA"' 'com.bibliotech.api.model.EmprestimoStatus.AGUARDANDO_RETIRADA'
Replace-In-File "$api_path\service\EmprestimoService.java" '"DEVOLVIDO"' 'com.bibliotech.api.model.EmprestimoStatus.DEVOLVIDO'
Replace-In-File "$api_path\service\EmprestimoService.java" '"AGUARDANDO"' 'com.bibliotech.api.model.ReservaStatus.AGUARDANDO'
Replace-In-File "$api_path\service\EmprestimoService.java" '"NOTIFICADO"' 'com.bibliotech.api.model.ReservaStatus.NOTIFICADO'
Replace-In-File "$api_path\service\EmprestimoService.java" '"CONCLUIDA"' 'com.bibliotech.api.model.ReservaStatus.CONCLUIDA'

Replace-In-File "$api_path\service\ReservaService.java" '"AGUARDANDO"' 'com.bibliotech.api.model.ReservaStatus.AGUARDANDO'
Replace-In-File "$api_path\service\ReservaService.java" '"CANCELADA"' 'com.bibliotech.api.model.ReservaStatus.CANCELADA'
Replace-In-File "$api_path\service\ReservaService.java" '"CONCLUIDA"' 'com.bibliotech.api.model.ReservaStatus.CONCLUIDA'
Replace-In-File "$api_path\service\ReservaService.java" '"NOTIFICADO"' 'com.bibliotech.api.model.ReservaStatus.NOTIFICADO'

Replace-In-File "$api_path\controller\AutenticacaoController.java" '"ATIVO"' 'com.bibliotech.api.model.UsuarioStatus.ATIVO'
Replace-In-File "$api_path\controller\UsuarioController.java" '"INATIVO"' 'com.bibliotech.api.model.UsuarioStatus.INATIVO'

Replace-In-File "$api_path\service\VerificadorAtrasosJob.java" '"ATIVO"' 'com.bibliotech.api.model.EmprestimoStatus.ATIVO'
Replace-In-File "$api_path\service\VerificadorAtrasosJob.java" '"ATRASADO"' 'com.bibliotech.api.model.EmprestimoStatus.ATRASADO'

Replace-In-File "$api_path\task\NotificacaoTask.java" '"ATIVO"' 'com.bibliotech.api.model.EmprestimoStatus.ATIVO'

$repo = "$api_path\repository\ReservaRepository.java"
$repoContent = Get-Content $repo -Raw
if ($repoContent -notmatch "ReservaStatus;") {
    $repoContent = $repoContent -replace "import com.bibliotech.api.model.Reserva;", "import com.bibliotech.api.model.Reserva;`r`nimport com.bibliotech.api.model.ReservaStatus;"
    Set-Content $repo $repoContent -Encoding UTF8
}

$repo = "$api_path\repository\EmprestimoRepository.java"
$repoContent = Get-Content $repo -Raw
if ($repoContent -notmatch "EmprestimoStatus;") {
    $repoContent = $repoContent -replace "import com.bibliotech.api.model.Emprestimo;", "import com.bibliotech.api.model.Emprestimo;`r`nimport com.bibliotech.api.model.EmprestimoStatus;"
    Set-Content $repo $repoContent -Encoding UTF8
}
