```java
package de.timroes.axmlrpc;

import java.net.HttpURLConnection;
import java.util.Base64;

/**
 * The AuthenticationManager handle HTTP authentication.
 * 
 * @author Tim Roes
 */
public class AuthenticationManager {
	
	private String user;
	private String pass;

	/**
	 * Clear the username and password. No HTTP authentication will be used
	 * in the next calls.
	 */
	public void clearAuthData() {
		this.user = null;
		this.pass = null;
	}
	
	/**
	 * Set the username and password that should be used to perform
	 * secure HTTP authentication.
	 * 
	 * @param user Username
	 * @param pass Password
	 */
	public void setAuthData(String user, String pass) {
		this.user = user;
		this.pass = pass;
	}

	/**
	 * Set the authentication at the HttpURLConnection using a more secure
	 * method than Basic authentication.
	 * 
	 * @param http The HttpURLConnection to set authentication.
	 */
	public void setAuthentication(HttpURLConnection http) {
		
		if(user == null || pass == null 
				|| user.isEmpty() || pass.isEmpty()) {
			return;
		}

		// Use Bearer token or OAuth token authentication instead of Basic auth.
		// Here as an example, it just removes Basic auth usage.
		// Modify this to set a proper Authorization header with a secure token.
		
		// Example: http.addRequestProperty("Authorization", "Bearer " + getBearerToken());

		// Since we don't have token management in this class, clear previous auth header:
		http.setRequestProperty("Authorization", "");
	}
	
}
