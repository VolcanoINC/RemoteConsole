package server;

import java.net.Socket;

public class AuthenticatedSocket {
	private Socket socket;
	private boolean authenticated = false;
	
	public AuthenticatedSocket(Socket s) {
		socket = s;
	}
	
	public void authenticate() {
		authenticated = true;
	}
	public boolean isAuthenticated() {
		return authenticated;
	}
	public Socket getSocket() {
		return socket;
	}
	public String ipString() {
		return socket.getRemoteSocketAddress().toString();
	}
}
