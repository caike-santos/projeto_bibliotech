import os

def replace_in_file(filepath, replacements):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    for old, new in replacements:
        content = content.replace(old, new)
        
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(content)

api_path = r'c:\Users\caike\OneDrive\Documentos\estudos\projeto_bibliotech\api\src\main\java\com\bibliotech\api'

# 1. Update EmprestimoService
replace_in_file(os.path.join(api_path, 'service', 'EmprestimoService.java'), [
    ('"ATIVO"', 'com.bibliotech.api.model.EmprestimoStatus.ATIVO'),
    ('"ATRASADO"', 'com.bibliotech.api.model.EmprestimoStatus.ATRASADO'),
    ('"AGUARDANDO_RETIRADA"', 'com.bibliotech.api.model.EmprestimoStatus.AGUARDANDO_RETIRADA'),
    ('"DEVOLVIDO"', 'com.bibliotech.api.model.EmprestimoStatus.DEVOLVIDO'),
    ('"AGUARDANDO"', 'com.bibliotech.api.model.ReservaStatus.AGUARDANDO'),
    ('"NOTIFICADO"', 'com.bibliotech.api.model.ReservaStatus.NOTIFICADO')
])

# 2. Update ReservaService
replace_in_file(os.path.join(api_path, 'service', 'ReservaService.java'), [
    ('"AGUARDANDO"', 'com.bibliotech.api.model.ReservaStatus.AGUARDANDO'),
    ('"CANCELADA"', 'com.bibliotech.api.model.ReservaStatus.CANCELADA'),
    ('"CONCLUIDA"', 'com.bibliotech.api.model.ReservaStatus.CONCLUIDA'),
    ('"NOTIFICADO"', 'com.bibliotech.api.model.ReservaStatus.NOTIFICADO')
])

# 3. Update AutenticacaoController
replace_in_file(os.path.join(api_path, 'controller', 'AutenticacaoController.java'), [
    ('"ATIVO"', 'com.bibliotech.api.model.UsuarioStatus.ATIVO')
])

# 4. Update UsuarioController
replace_in_file(os.path.join(api_path, 'controller', 'UsuarioController.java'), [
    ('"INATIVO"', 'com.bibliotech.api.model.UsuarioStatus.INATIVO')
])

# 5. Update VerificadorAtrasosJob
replace_in_file(os.path.join(api_path, 'service', 'VerificadorAtrasosJob.java'), [
    ('"ATIVO"', 'com.bibliotech.api.model.EmprestimoStatus.ATIVO'),
    ('"ATRASADO"', 'com.bibliotech.api.model.EmprestimoStatus.ATRASADO')
])

# 6. Update NotificacaoTask
replace_in_file(os.path.join(api_path, 'task', 'NotificacaoTask.java'), [
    ('"ATIVO"', 'com.bibliotech.api.model.EmprestimoStatus.ATIVO')
])

# 7. Add imports to Repositories
repo_path = os.path.join(api_path, 'repository', 'ReservaRepository.java')
with open(repo_path, 'r', encoding='utf-8') as f:
    repo_content = f.read()
if 'import com.bibliotech.api.model.ReservaStatus;' not in repo_content:
    repo_content = repo_content.replace('import com.bibliotech.api.model.Reserva;', 'import com.bibliotech.api.model.Reserva;\nimport com.bibliotech.api.model.ReservaStatus;')
with open(repo_path, 'w', encoding='utf-8') as f:
    f.write(repo_content)

print("Done")
