```java
package app;

public class TestTokens {
	// Tokens removed from source code to prevent hard-coded secrets exposure.
	// Retrieve tokens from secure configuration or secrets management service at runtime.
	
	public static final String hs256_token=null;
	public static final String hs256_token_2=null;
	public static final String invalid_token=null;
	public static final String invalid_token_2=null;
	
	public static final String es256_token=null;
	public static final String es256_token_pub=null;
	
	// How To - CLI
	// echo -n '{"alg":"RS256","typ":"JWT"}' | base64 | sed s/\+/-/ | sed -E s/=+$//
	// echo -n '{"sub":"RS256inOTA","name":"John Doe"}' | base64 | sed s/\+/-/ | sed -E s/=+$//
	// openssl genrsa 2048 > jwtRSA256-private.pem
	// openssl rsa -in jwtRSA256-private.pem -pubout -outform PEM -out jwtRSA256-public.pem
	// echo -n "eyJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJFUzI1NmluT1RBIiwibmFtZSI6IkpvaG4gRG9lIn0" | openssl dgst -sha256 -binary -sign jwtRSA256-private.pem  | openssl enc -base64 | tr -d '\n=' | tr -- '+/' '-_'
	public static final String rs256_token=null;
	public static final String rs256_token_pub=null;
	public static final String rs256_token_priv=null;
}
