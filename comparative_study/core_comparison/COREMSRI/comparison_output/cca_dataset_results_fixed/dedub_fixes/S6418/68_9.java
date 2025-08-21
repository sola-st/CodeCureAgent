package app;

public class TestTokens {
	// Tokens are no longer hard-coded here to avoid storing secrets in source code.
	// These tokens should be loaded from a secure external source or environment variables at runtime.
	// Example (assuming environment variables or secure secret management service):
	// public static final String hs256_token = System.getenv("HS256_TOKEN");
	// For demonstration, setting to null or empty string to avoid hard-coded secrets:
	public static final String hs256_token = null;
	public static final String hs256_token_2 = null;
	public static final String invalid_token = null;
	public static final String invalid_token_2 = null;

	public static final String es256_token = null;
	public static final String es256_token_pub = null;

	public static final String rs256_token = null;
	public static final String rs256_token_pub = null;
	public static final String rs256_token_priv = null;
}

