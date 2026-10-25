package OTO.auth;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import OTO.auth.AuthDtos.DefinirSenhaRequest;
import OTO.auth.AuthDtos.LoginRequest;
import OTO.auth.AuthDtos.LoginResponse;
import jakarta.validation.Valid;

/** rotas públicas: login e definição de senha pelo link enviado pelo OTO. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest requisicao) {
        return auth.login(requisicao);
    }

    @PostMapping("/definir-senha")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void definirSenha(@Valid @RequestBody DefinirSenhaRequest requisicao) {
        auth.definirSenha(requisicao);
    }
}
