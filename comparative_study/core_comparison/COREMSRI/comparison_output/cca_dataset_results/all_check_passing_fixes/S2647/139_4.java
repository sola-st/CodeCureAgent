package de.timroes.axmlrpc;

import java.net.HttpURLConnection;

/**
 * The AuthenticationManager handle authentication.
 * 
 * @author Tim Roes
 */
public class AuthenticationManager {
	
	private String user;
	private String pass;

	/**
	 * Clear the username and password. No authentication will be used
	 * in the next calls.
	 */
	public void clearAuthData() {
		this.user = null;
		this.pass = null;
	}
	
	/**
	 * Set the username and password that should be used to perform authentication.
	 * 
	 * @param user Username
	 * @param pass Password
	 */
	public void setAuthData(String user, String pass) {
		this.user = user;
		this.pass = pass;
	}

	/**
	 * Set the authentication at the HttpURLConnection.
	 * 
	 * @param http The HttpURLConnection to set authentication.
	 */
	public void setAuthentication(HttpURLConnection http) {
		
		if(user == null || pass == null 
				|| user.length() <= 0 || pass.length() <= 0) {
			return;
		}

		// Remove usage of Basic Authentication with Base64 encoding due to security concerns.
		// Instead, authentication should be handled via a more secure method, e.g., OAuth2 tokens, API keys, etc.
		// Here, we only set a placeholder header or do nothing.
		// You should implement a proper authentication mechanism here.

		// Example placeholder: (remove or replace as appropriate)
		// http.addRequestProperty("Authorization", "Bearer " + getSecureToken());

		// Currently, do not set any Authorization header to avoid using Basic Auth
	}
	
}
