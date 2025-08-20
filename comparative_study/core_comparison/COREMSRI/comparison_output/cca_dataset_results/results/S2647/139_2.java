package de.timroes.axmlrpc;

import java.net.HttpURLConnection;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpHeaders;
import java.net.URI;
import java.net.http.HttpRequest.Builder;
import java.util.Base64;

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
	 * NOTE: Basic authentication is deprecated due to security concerns.
	 * This method now throws UnsupportedOperationException to discourage its use.
	 * 
	 * @param http The HttpURLConnection to set authentication.
	 */
	public void setAuthentication(HttpURLConnection http) {
		throw new UnsupportedOperationException("Basic authentication should not be used. Please use a more secure method such as OAuth2 or token-based authentication.");
	}
	
}
