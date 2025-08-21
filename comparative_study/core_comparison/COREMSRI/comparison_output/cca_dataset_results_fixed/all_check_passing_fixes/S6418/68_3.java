package app;

public class TestTokens {
	// Tokens are now expected to be loaded from a secure external source, e.g., environment variables, config files, or secrets manager.
	// The following are placeholders or examples, not hard-coded secrets.

	public static final String hs256_token = System.getenv("HS256_TOKEN");
	public static final String hs256_token_2 = System.getenv("HS256_TOKEN_2");
	public static final String invalid_token = System.getenv("INVALID_TOKEN");
	public static final String invalid_token_2 = System.getenv("INVALID_TOKEN_2");

	public static final String es256_token = System.getenv("ES256_TOKEN");
	public static final String es256_token_pub = System.getenv("ES256_TOKEN_PUB");

	// How To - CLI
	// echo -n '{"alg":"RS256","typ":"JWT"}' | base64 | sed s/\+/-/ | sed -E s/=+$//
	// echo -n '{"sub":"RS256inOTA","name":"John Doe"}' | base64 | sed s/\+/-/ | sed -E s/=+$//
	// openssl genrsa 2048 > jwtRSA256-private.pem
	// openssl rsa -in jwtRSA256-private.pem -pubout -outform PEM -out jwtRSA256-public.pem
	// echo -n "eyJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJFUzI1NmluT1RBIiwibmFtZSI6IkpvaG4gRG9lIn0" | openssl dgst -sha256 -binary -sign jwtRSA256-private.pem  | openssl enc -base64 | tr -d '\n=' | tr -- '+/' '-_'
	public static final String rs256_token = System.getenv("RS256_TOKEN");
	public static final String rs256_token_pub = System.getenv("RS256_TOKEN_PUB");
	public static final String rs256_token_priv = System.getenv("RS256_TOKEN_PRIV");
}