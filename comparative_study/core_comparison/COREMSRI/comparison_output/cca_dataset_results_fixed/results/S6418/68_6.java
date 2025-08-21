package app;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;

public class TestTokens {

	private static String loadSecretFromFile(String path) {
		try {
			return new String(Files.readAllBytes(Paths.get(path)));
		} catch (IOException e) {
			throw new RuntimeException("Failed to load secret from file: " + path, e);
		}
	}

	public static final String hs256_token = loadSecretFromFile("config/hs256_token.txt");
	public static final String hs256_token_2 = loadSecretFromFile("config/hs256_token_2.txt");
	public static final String invalid_token = loadSecretFromFile("config/invalid_token.txt");
	public static final String invalid_token_2 = loadSecretFromFile("config/invalid_token_2.txt");

	public static final String es256_token = loadSecretFromFile("config/es256_token.txt");
	public static final String es256_token_pub = loadSecretFromFile("config/es256_token_pub.txt");

	// How To - CLI
	// echo -n '{"alg":"RS256","typ":"JWT"}' | base64 | sed s/\+/-/ | sed -E s/=+$//
	// echo -n '{"sub":"RS256inOTA","name":"John Doe"}' | base64 | sed s/\+/-/ | sed -E s/=+$//
	// openssl genrsa 2048 > jwtRSA256-private.pem
	// openssl rsa -in jwtRSA256-private.pem -pubout -outform PEM -out jwtRSA256-public.pem
	// echo -n "eyJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJFUzI1NmluT1RBIiwibmFtZSI6IkpvaG4gRG9lIn0" | openssl dgst -sha256 -binary -sign jwtRSA256-private.pem  | openssl enc -base64 | tr -d '\n=' | tr -- '+/' '-_'
	public static final String rs256_token = loadSecretFromFile("config/rs256_token.txt");
	public static final String rs256_token_pub = loadSecretFromFile("config/rs256_token_pub.txt");
	public static final String rs256_token_priv = loadSecretFromFile("config/rs256_token_priv.txt");
}