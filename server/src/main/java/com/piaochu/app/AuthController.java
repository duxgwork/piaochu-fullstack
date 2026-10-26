package com.piaochu.app;
import static com.piaochu.app.Contracts.*;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
@RestController @RequestMapping("/api/auth")
class AuthController {
    private final AuthService auth;AuthController(AuthService a){auth=a;}
    @PostMapping("/register") @ResponseStatus(HttpStatus.CREATED) Auth register(@Valid @RequestBody Register r){return auth.register(r);}
    @PostMapping("/login") Auth login(@Valid @RequestBody Login r){return auth.login(r);}
    @GetMapping("/me") User me(@RequestAttribute("actor") Actor a){return auth.me(a);}
    @PostMapping("/logout") @ResponseStatus(HttpStatus.NO_CONTENT) void logout(@RequestAttribute("actor") Actor a){auth.logout(a);}
}
