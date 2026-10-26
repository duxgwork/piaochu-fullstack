package com.piaochu.app;
import static com.piaochu.app.Contracts.*;
import java.time.*;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
@Service @Transactional
class AuthService {
    private final String missingAccountHash=Passwords.hash(java.util.UUID.randomUUID().toString());
    private final AccountRepository accounts; private final SessionRepository sessions; private final Clock clock;
    AuthService(AccountRepository a,SessionRepository s,Clock c) { accounts=a;sessions=s;clock=c; }
    Auth register(Register r) {
        String username=r.username().toLowerCase(Locale.ROOT);
        if(accounts.findByUsername(username).isPresent()) throw fail(409,"账号已存在，请登录或换一个账号");
        Account a=new Account();a.username=username;a.displayName=r.displayName().strip();a.role="CUSTOMER";
        a.passwordHash=Passwords.hash(r.password());a.stamp(clock.instant());accounts.saveAndFlush(a);return issue(a);
    }
    Auth login(Login r) {
        Account a=accounts.findByUsername(r.username().strip().toLowerCase(Locale.ROOT)).orElse(null);
        boolean valid=Passwords.matches(r.password(),a==null?missingAccountHash:a.passwordHash);
        if(a==null || !valid) throw fail(401,"账号或密码不正确");
        sessions.deleteByExpiresAtBefore(clock.instant());return issue(a);
    }
    private Auth issue(Account a) {
        String token=Passwords.token();Session s=new Session();s.userId=a.id;s.tokenHash=Passwords.digest(token);
        s.expiresAt=clock.instant().plus(Duration.ofDays(7));s.stamp(clock.instant());sessions.save(s);
        return new Auth(token,s.expiresAt.toString(),User.from(a));
    }
    @Transactional(readOnly=true) Actor authenticate(String header) {
        if(header==null || !header.startsWith("Bearer ") || header.length()>200) throw fail(401,"请先登录");
        String hash=Passwords.digest(header.substring(7));Session s=sessions.findByTokenHash(hash).orElseThrow(()->fail(401,"登录已失效，请重新登录"));
        if(!s.expiresAt.isAfter(clock.instant())) throw fail(401,"登录已过期，请重新登录");
        Account a=accounts.findById(s.userId).orElseThrow(()->fail(401,"账号不存在"));return new Actor(a.id,hash,a.role);
    }
    User me(Actor actor) { return User.from(accounts.findById(actor.id()).orElseThrow(()->fail(401,"账号不存在"))); }
    void logout(Actor actor) { sessions.deleteByTokenHash(actor.sessionHash()); }
    static ResponseStatusException fail(int status,String message) { return new ResponseStatusException(HttpStatus.valueOf(status),message); }
}
