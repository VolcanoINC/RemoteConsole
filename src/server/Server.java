package server;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.text.AttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;

public class Server {
	public static ServerSocket serverSocket;
	public static boolean serverRunning = true;
	private static JTextPane outputTextArea;
	private static ArrayList<AuthenticatedSocket> sockets = new ArrayList<AuthenticatedSocket>();
	
	//Process/CMD related
	private static ProcessThread runningProgram;
	private static String workDir = "./";
	
	public static String inputToProgram(String msg) {
		if (runningProgram == null || !runningProgram.running()) {
			if (msg.length() >= 3 && msg.substring(0,3).equals("cd ")) {
				workDir = msg.substring(3);
				return "OUTPUT:Changed working directory to " + msg.substring(3);
			} else if (msg.equals("getworkdir")) {
				return "OUTPUT:Current working directory is " + workDir;
			} else {
				runningProgram = new ProcessThread(msg,workDir);
				Thread proc = new Thread(runningProgram);
				proc.start();
				return "ACK";
			}
		} else {
			try {
				BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(runningProgram.proc.getOutputStream()));
				writer.write(msg + "\n");
				writer.flush();
				return "ACK";
			} catch (IOException e) {
				error("Failed to write to process input: " + e.getMessage(),true);
				return "INPUT_ERROR";
			}
		}
	}
	
	
	//Output related
	private static void appendToPane(JTextPane tp, String msg, Color c) {
		StyleContext sc = StyleContext.getDefaultStyleContext();
		AttributeSet aset = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, c);
		int len = tp.getDocument().getLength();
		tp.setCaretPosition(len);
		tp.setCharacterAttributes(aset, false);
		tp.replaceSelection(msg + "\n");
	}
	
	public static void broadcast(String msg) {
		for (AuthenticatedSocket asock : sockets) {
			Socket s = asock.getSocket();
			if (!s.isConnected() || s.isClosed()) {
				sockets.remove(asock);
			} else if (asock.isAuthenticated()) {
				try {
					DataOutputStream toClient = new DataOutputStream(s.getOutputStream());
					toClient.writeBytes(msg + "\n");
				} catch (IOException e) {
					error("Failed to broadcast to " + asock.ipString());
				}
			}
		}
	}
	
	public static void print(String msg) { appendToPane(outputTextArea,msg,new Color(0,0,0)); }
	public static void info(String msg) { appendToPane(outputTextArea,msg,new Color(0,0,255)); }
	public static void warn(String msg) { appendToPane(outputTextArea,msg,new Color(255,100,0)); }
	public static void error(String msg) { appendToPane(outputTextArea,msg,new Color(255,0,0)); }
	public static void output(String msg) { appendToPane(outputTextArea,msg,new Color(128,128,128)); }
	
	public static void print(String msg,boolean doBroadcast) { print(msg); if (doBroadcast) broadcast("PRINT:" + msg); }
	public static void info(String msg,boolean doBroadcast) { info(msg); if (doBroadcast) broadcast("INFO:" + msg); }
	public static void warn(String msg,boolean doBroadcast) { warn(msg); if (doBroadcast) broadcast("WARN:" + msg); }
	public static void error(String msg,boolean doBroadcast) { error(msg); if (doBroadcast) broadcast("ERROR:" + msg); }
	public static void output(String msg,boolean doBroadcast) { output(msg); if (doBroadcast) broadcast("OUTPUT:" + msg); }
	
	public static void removeConnection(AuthenticatedSocket socket) {
		sockets.remove(socket);
	}
	
	public static void main(String[] args) throws IOException {
		//UI
		JFrame window = new JFrame("Remote Console Server");
		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		window.setSize(800, 600);
		window.setMinimumSize(new Dimension(800,600));
		outputTextArea = new JTextPane();
		new JScrollPane(outputTextArea);
		
		JPanel consolePanel = new JPanel();
		consolePanel.setLayout(new GridBagLayout());
		outputTextArea = new JTextPane();
		//outputTextArea.setEditable(false);
		JScrollPane scrollPane = new JScrollPane(outputTextArea);
		GridBagConstraints con = new GridBagConstraints();
		con.gridwidth = 1;
		con.gridheight = 1;
		con.weightx = 1;
		con.weighty = 1;
		con.gridx = 0;
		con.gridy = 0;
		con.fill = GridBagConstraints.BOTH;
		consolePanel.add(scrollPane,con);
		JPanel cmdBarPanel = new JPanel();
		cmdBarPanel.setLayout(new GridBagLayout());
		JTextField cmdInput = new JTextField();
		JButton sendBtn = new JButton("Run");
		con = new GridBagConstraints();
		con.gridwidth = 3;
		con.gridheight = 1;
		con.gridx = 0;
		con.weightx = 1;
		con.weighty = 1;
		con.fill = GridBagConstraints.BOTH;
		cmdBarPanel.add(cmdInput,con);
		con = new GridBagConstraints();
		con.gridwidth = 1;
		con.gridheight = 1;
		con.gridx = 3;
		con.weightx = 0;
		con.weighty = 1;
		con.fill = GridBagConstraints.BOTH;
		cmdBarPanel.add(sendBtn, con);
		con = new GridBagConstraints();
		con.gridwidth = 1;
		con.gridheight = 1;
		con.gridy = 1;
		con.weightx = 1;
		con.weighty = 0;
		con.fill = GridBagConstraints.HORIZONTAL;
		consolePanel.add(cmdBarPanel,con);
		
		window.add(consolePanel);
		window.setVisible(true);
		
		ActionListener submitListener = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String cmd = cmdInput.getText();
				print("SERVER: " + cmd,true);
				cmdInput.setText("");
				String out = inputToProgram(cmd);
				if (out.length() >= 7 && out.substring(0,7).equals("OUTPUT:")) {
					output(out.substring(7),true);
				}
			}
		};
		
		cmdInput.addActionListener(submitListener);
		sendBtn.addActionListener(submitListener);
		
		info("Starting Server...");
		serverSocket = new ServerSocket(1337);
		
		while (serverRunning) {
			try {
				Socket newSocket = serverSocket.accept();
				AuthenticatedSocket asock = new AuthenticatedSocket(newSocket);
				info("New connection: USER-" + asock.ipString());
				sockets.add(asock);
				Thread connectionThread = new Thread(new ConnectionThread(asock));
				connectionThread.start();
			} catch (IOException e) {
				error("ERROR: " + e.getMessage());
			}
		}
	}
}
