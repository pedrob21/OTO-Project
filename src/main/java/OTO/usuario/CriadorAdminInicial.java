package OTO.usuario;

import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sem um administrador ninguém consegue entrar para cadastrar os outros usuários.
 * Na subida, se não houver usuário com o e-mail de ADMIN_EMAIL, ele é criado com
 * a senha de ADMIN_SENHA. Depois do primeiro acesso, troque a senha pelo sistema.
 */
@Component
public class CriadorAdminInicial implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CriadorAdminInicial.class);

    private final AdminInicialProperties propriedades;
    private final UsuarioRepository usuarios;
    private final PasswordEncoder senhas;

    public CriadorAdminInicial(AdminInicialProperties propriedades, UsuarioRepository usuarios, PasswordEncoder senhas) {
        this.propriedades = propriedades;
        this.usuarios = usuarios;
        this.senhas = senhas;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String email = propriedades.email() == null ? "" : propriedades.email().trim().toLowerCase(Locale.ROOT);
        if (email.isEmpty()) {
            log.info("ADMIN_EMAIL não definido: nenhum administrador inicial será criado.");
            return;
        }
        if (usuarios.existsByEmail(email)) {
            return;
        }
        String senha = propriedades.senha();
        if (senha == null || senha.length() < 8) {
            log.warn("ADMIN_SENHA precisa ter pelo menos 8 caracteres; o administrador inicial não foi criado.");
            return;
        }
        String nome = propriedades.nome() == null || propriedades.nome().isBlank() ? "Administrador OTO" : propriedades.nome();
        Usuario admin = new Usuario(nome, email, Perfil.ADMIN);
        admin.definirSenha(senhas.encode(senha));
        usuarios.save(admin);
        log.info("Administrador inicial criado: {}", email);
    }
}
