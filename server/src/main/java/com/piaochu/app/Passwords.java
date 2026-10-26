package com.piaochu.app;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
/** 每个密码使用独立随机盐；会话只保存 SHA-256 摘要，数据库泄漏不能直接取出登录令牌。 */
final class Passwords {
    private static final SecureRandom RANDOM=new SecureRandom();
    static String hash(String password) {
        byte[] salt=new byte[16]; RANDOM.nextBytes(salt);
        return "600000:"+Base64.getEncoder().encodeToString(salt)+":"+Base64.getEncoder().encodeToString(derive(password,salt,600000));
    }
    static boolean matches(String password,String encoded) {
        try { String[] parts=encoded.split(":"); return MessageDigest.isEqual(Base64.getDecoder().decode(parts[2]),
            derive(password,Base64.getDecoder().decode(parts[1]),Integer.parseInt(parts[0]))); }
        catch(RuntimeException error) { return false; }
    }
    private static byte[] derive(String password,byte[] salt,int iterations) {
        PBEKeySpec spec=new PBEKeySpec(password.toCharArray(),salt,iterations,256);
        try { return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded(); }
        catch(GeneralSecurityException error) { throw new IllegalStateException("密码算法不可用",error); }
        finally { spec.clearPassword(); }
    }
    static String token() { byte[] bytes=new byte[32]; RANDOM.nextBytes(bytes); return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes); }
    static String digest(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch(NoSuchAlgorithmException error) { throw new IllegalStateException(error); }
    }
}
