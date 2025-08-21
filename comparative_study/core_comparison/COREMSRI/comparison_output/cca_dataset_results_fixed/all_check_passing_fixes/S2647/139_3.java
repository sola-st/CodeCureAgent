package de.timroes.axmlrpc;

import java.net.HttpURLConnection;

/**
 * The AuthenticationManager handle authentication using a token.
 * 
 * @author Tim Roes
 */
public class AuthenticationManager {
	
	private String token;

	/**
	 * Clear the authentication token. No authentication will be used
	 * in the next calls.
	 */
	public void clearAuthData() {
		this.token = null;
	}
	
	/**
	 * Set the token that should be used to perform authentication.
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
