package server;

import java.io.*;

public class ConnectionThread implements Runnable {
	private AuthenticatedSocket connectionSocket;
	
	public ConnectionThread(AuthenticatedSocket socket) {
		connectionSocket = socket;
	}
	
	private String handleInputMessage(String msg) {
		if (!connectionSocket.isAuthenticated()) {
			if (msg.length() >= 5 && msg.substring(0,5).equals("AUTH:")) {
				String pwd = msg.substring(5);
				if (pwd.equals("the-password")) {
					connectionSocket.authenticate();
					Server.info("USER-" + connectionSocket.ipString() + " has successfully authenticated");
					return "AUTH_SUCCESS";
				} else {
					Server.warn("Failed authentication from " + connectionSocket.getSocket());
					
					//Failed authentication: Sleep for 3000 milliseconds to make brute-force attacks painfully slow.
					try { Thread.sleep(3000); } catch (InterruptedException e) {}
					return "AUTH_FAILED";
				}
			} else {
				return "AUTH_REQUIRED";
			}
		} else {
			Server.print("USER-" + connectionSocket.ipString() + ": " + msg,true);
			return Server.inputToProgram(msg);
		}
	}
	
	@Override
	public void run() {
		boolean run = true;
		while (run) {
			try {
				BufferedReader fromClient = new BufferedReader(new InputStreamReader(connectionSocket.getSocket().getInputStream()));
				DataOutputStream toClient = new DataOutputStream(connectionSocket.getSocket().getOutputStream());
				
				String msgClient = fromClient.readLine();
				while (msgClient != null && run) {
					if (msgClient.equalsIgnoreCase("SRVCMD:DISCONNECT")) {
						Server.info("USER-" + connectionSocket.ipString() + " has disconnected",true);
						run = false;
						break;
					} else {
						String response = handleInputMessage(msgClient) + "\n";
						toClient.writeBytes(response);
					}
					msgClient = fromClient.readLine();
				}
			} catch (IOException e) {
				run = false;
				Server.removeConnection(connectionSocket);
				Server.info("USER-" + connectionSocket.ipString() + " has lost connection",true);
				Server.error("Connection error: " + e.getMessage());
				Server.info("Closing connection for USER-" + connectionSocket.ipString());
			}
		}
		try {
			connectionSocket.getSocket().close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
