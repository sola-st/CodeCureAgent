package app;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;

public class TestTokens {

    // Load tokens from external configuration file or environment variables instead of hardcoding

    public static final String hs256_token = loadSecret("HS256_TOKEN");
    public static final String hs256_token_2 = loadSecret("HS256_TOKEN_2");
    public static final String invalid_token = loadSecret("INVALID_TOKEN");
    public static final String invalid_token_2 = loadSecret("INVALID_TOKEN_2");

    public static final String es256_token = loadSecret("ES256_TOKEN");
    public static final String es256_token_pub = loadSecret("ES256_TOKEN_PUB");

    // How To - CLI
    // echo -n '{"alg":"RS256","typ":"JWT"}' | base64 | sed s/\+/-/ | sed -E s/=+$//
    // echo -n '{"sub":"RS256inOTA","name":"John Doe"}' | base64 | sed s/\+/-/ | sed -E s/=+$//
    // openssl genrsa 2048 > jwtRSA256-private.pem
    // openssl rsa -in jwtRSA256-private.pem -pubout -outform PEM -out jwtRSA256-public.pem
    // echo -n "eyJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJFUzI1NmluT1RBIiwibmFtZSI6IkpvaG4gRG9lIn0" | openssl dgst -sha256 -binary -sign jwtRSA256-private.pem  | openssl enc -base64 | tr -d '\n=' | tr -- '+/' '-_'
    public static final String rs256_token = loadSecret("RS256_TOKEN");
    public static final String rs256_token_pub = loadSecret("RS256_TOKEN_PUB");
    public static final String rs256_token_priv = loadSecret("RS256_TOKEN_PRIV");

    private static String loadSecret(String key) {
        // First try to load from environment variable
        String secret = System.getenv(key);
        if (secret != null && !secret.isEmpty()) {
            return secret;
        }

        // Then try to load from a file named key in a secure config folder, e.g., ./config/
        try {
            byte[] bytes = Files.readAllBytes(Paths.get("config/" + key));
            secret = new String(bytes).trim();
            if (!secret.isEmpty()) {
                return secret;
            }
        } catch (IOException e) {
            // Log error or ignore silently
        }

        // Return null or empty if not found - caller should handle missing secrets safely
        return null;
    }
}