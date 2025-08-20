package de.timroes.axmlrpc;

import java.net.HttpURLConnection;
import java.net.URL;
import javax.net.ssl.HttpsURLConnection;

/**
 * The AuthenticationManager handle HTTP authentication.
 * 
 * Note: Basic authentication is avoided because it only encodes credentials 
 * using Base64, which is insecure. Instead, token-based or other secure 
 * authentication mechanisms should be used.
 * 
 * This example demonstrates using bearer token authentication instead of basic auth.
 * 
 * @author Tim Roes
 */
public class AuthenticationManager {
	
	private String token;

	/**
	 * Clear the authentication token. No HTTP authentication will be used
	 * in the next calls.
	 */
	public void clearAuthData() {
		this.token = null;
	}
	
	/**
	 * Set the bearer token that should be used for authentication.
	 * 
	 * @param token Authentication token
	 */
	public void setAuthData(String token) {
		this.token = token;
	}

	/**
	 * Set the authentication at the HttpURLConnection.
	 * 
	 * @param http The HttpURLConnection to set authentication.
	 */
	public void setAuthentication(HttpURLConnection http) {
		
		if(token == null || token.length() <= 0) {
			return;
		}

		http.setRequestProperty("Authorization", "Bearer " + token);
		
	}
	
}
