```java
package de.timroes.axmlrpc;

import java.net.HttpURLConnection;

/**
 * The AuthenticationManager handle HTTP authentication using Bearer token.
 * 
 * @author Tim Roes
 */
public class AuthenticationManager {
	
	private String bearerToken;

	/**
	 * Clear the authentication token. No authentication will be used
	 * in the next calls.
	 */
	public void clearAuthData() {
		this.bearerToken = null;
	}
	
	/**
	 * Set the Bearer token that should be used to perform authentication.
	 * 
	 * @param token Bearer token string
	 */
	public void setAuthData(String token) {
		this.bearerToken = token;
	}

	/**
	 * Set the authentication at the HttpURLConnection.
	 * 
	 * @param http The HttpURLConnection to set authentication.
	 */
	public void setAuthentication(HttpURLConnection http) {
		
		if(bearerToken == null || bearerToken.length() <= 0) {
			return;
		}

		http.setRequestProperty("Authorization", "Bearer " + bearerToken);
		
	}
	
}
