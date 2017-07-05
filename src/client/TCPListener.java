package client;

import java.io.*;
import java.net.*;

public class TCPListener implements Runnable {
	private Socket clientSocket;
	private boolean running = true;
	public TCPListener(Socket socket) {
		clientSocket = socket;
	}
	
	private void handleMessage(String msg) {
		if (msg.length() >= 6 && msg.substring(0,6).equals("PRINT:")) {
			Client.print(msg.substring(6));
		} else if (msg.length() >= 5 && msg.substring(0,5).equals("INFO:")) {
			Client.info(msg.substring(5));
		} else if (msg.length() >= 5 && msg.substring(0,5).equals("WARN:")) {
			Client.warn(msg.substring(5));
		} else if (msg.length() >= 6 && msg.substring(0,6).equals("ERROR:")) {
			Client.error(msg.substring(6));
		} else if (msg.length() >= 7 && msg.substring(0,7).equals("OUTPUT:")) {
			Client.output(msg.substring(7));
		} else if (msg.equals("INPUT_ERROR")) {
			//Error message provided by server, do nothing
		} else if (msg.equals("ACK")) {
			//Input acknowledged, do nothing
		} else if (msg.equals("AUTH_REQUIRED")) {
			Client.info("Not Authenticated. Please enter the command AUTH:<Password> to authenticate.");
		} else if (msg.equals("AUTH_SUCCESS")) {
			Client.info("Successfully authenticated, connection complete.");
		} else if (msg.equals("AUTH_FAILED")) {
			Client.info("Authentication failed. Please enter the command AUTH:<Password> to authenticate.");
		} else {
			Client.info("Unhandled packet: " + msg);
		}
	}
	
	@Override
	public void run() {
		while (running) {
			try {
				BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
				String fromServer = inFromServer.readLine();
				while (fromServer != null) {
					handleMessage(fromServer);
					fromServer = inFromServer.readLine();
				}
			} catch (IOException e) {
				Client.error("Network error, disconnecting from server");
				running = false;
			}
		}
		try {
			clientSocket.close();
		} catch (IOException e) {
			Client.error("ERROR: " + e.getMessage());
		}
	}
}
