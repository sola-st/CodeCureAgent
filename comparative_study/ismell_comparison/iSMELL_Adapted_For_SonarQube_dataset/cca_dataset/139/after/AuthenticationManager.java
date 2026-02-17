package de.timroes.axmlrpc;

import java.net.HttpURLConnection;
import java.util.UUID;

/**
 * The AuthenticationManager handle basic HTTP authentication.
 * 
 * @author Tim Roes
 */
public class AuthenticationManager {
	
	private String user;
	private String pass;
	private String bearerToken;

	/**
	 * Clear the username, password and token. No authentication will be used
	 * in the next calls.
	 */
	public void clearAuthData() {
		this.user = null;
		this.pass = null;
		this.bearerToken = null;
	}
	
	/**
	 * Set the username and password that should be used to perform authentication.
	 * The basic authentication is not used anymore; instead, a bearer token should be set.
	 * 
	 * @param user Username
	 * @param pass Password
	 */
	public void setAuthData(String user, String pass) {
		this.user = user;
		this.pass = pass;
		// Issue a bearer token or get one from an auth server.
		// For demonstration, generate a dummy token.
		this.bearerToken = generateBearerToken();
	}
	
	/**
	 * Generates a dummy bearer token - replace this with a real token retrieval.
	 */
	private String generateBearerToken() {
		// In real code, authenticate with user/pass to obtain a token.
		return UUID.randomUUID().toString();
	}

	/**
	 * Set the authentication at the HttpURLConnection.
	 * Use Bearer token authentication instead of Basic authentication.
	 * 
	 * @param http The HttpURLConnection to set authentication.
	 */
	public void setAuthentication(HttpURLConnection http) {
		
		if (bearerToken == null || bearerToken.isEmpty()) {
			return;
		}

		http.setRequestProperty("Authorization", "Bearer " + bearerToken);
		
	}
	
}