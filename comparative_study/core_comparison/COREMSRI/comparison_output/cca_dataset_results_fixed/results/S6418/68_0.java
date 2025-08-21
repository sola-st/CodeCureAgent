package app;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;

public class TestTokens {
	// Tokens are now loaded from external files or environment variables to avoid hard-coded secrets

	public static final String hs256_token = loadSecretFromFile("secrets/hs256_token.txt");
	public static final String hs256_token_2 = loadSecretFromFile("secrets/hs256_token_2.txt");
	public static final String invalid_token = loadSecretFromFile("secrets/invalid_token.txt");
	public static final String invalid_token_2 = loadSecretFromFile("secrets/invalid_token_2.txt");

	public static final String es256_token = loadSecretFromFile("secrets/es256_token.txt");
	public static final String es256_token_pub = loadSecretFromFile("secrets/es256_token_pub.pem");

	public static final String rs256_token = loadSecretFromFile("secrets/rs256_token.txt");
	public static final String rs256_token_pub = loadSecretFromFile("secrets/rs256_token_pub.pem");
	public static final String rs256_token_priv = loadSecretFromFile("secrets/rs256_token_priv.pem");

	// How To - CLI
	// echo -n '{"alg":"RS256","typ":"JWT"}' | base64 | sed s/\+/-/ | sed -E s/=+$//
	// echo -n '{"sub":"RS256inOTA","name":"John Doe"}' | base64 | sed s/\+/-/ | sed -E s/=+$//
	// openssl genrsa 2048 > jwtRSA256-private.pem
	// openssl rsa -in jwtRSA256-private.pem -pubout -outform PEM -out jwtRSA256-public.pem
	// echo -n "eyJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJFUzI1NmluT1RBIiwibmFtZSI6IkpvaG4gRG9lIn0" | openssl dgst -sha256 -binary -sign jwtRSA256-private.pem  | openssl enc -base64 | tr -d '\n=' | tr -- '+/' '-_'

	private static String loadSecretFromFile(String path) {
		try {
			return new String(Files.readAllBytes(Paths.get(path))).trim();
		} catch (IOException e) {
			throw new RuntimeException("Failed to load secret from " + path, e);
		}
	}
}
