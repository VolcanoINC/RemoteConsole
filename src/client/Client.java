package client;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.text.AttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;

public class Client {
	private static JPanel connectPanel;
	private static JPanel consolePanel;
	private static JTextField ipInput;
	private static JTextField ptInput;
	private static JButton connectBtn;
	private static JTextPane outputTextArea;
	private static DataOutputStream outToServer;
	
	private static void appendToPane(JTextPane tp, String msg, Color c) {
		StyleContext sc = StyleContext.getDefaultStyleContext();
		AttributeSet aset = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, c);
		int len = tp.getDocument().getLength();
		tp.setCaretPosition(len);
		tp.setCharacterAttributes(aset, false);
		tp.replaceSelection(msg + "\n");
	}
	
	public static void print(String msg) { appendToPane(outputTextArea,msg,new Color(0,0,0)); }
	public static void info(String msg) { appendToPane(outputTextArea,msg,new Color(0,0,255)); }
	public static void warn(String msg) { appendToPane(outputTextArea,msg,new Color(255,100,0)); }
	public static void error(String msg) { appendToPane(outputTextArea,msg,new Color(255,0,0)); }
	public static void output(String msg) { appendToPane(outputTextArea,msg,new Color(128,128,128)); }
	
	public static void main(String[] args) {
		JFrame window = new JFrame("Remote Console");
		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		window.setSize(800, 600);
		window.setMinimumSize(new Dimension(800,600));
		
		connectPanel = new JPanel();
		ipInput = new JTextField("IP",10);
		ptInput = new JTextField("Port",5);
		connectBtn = new JButton("Connect");
		connectPanel.add(ipInput);
		connectPanel.add(ptInput);
		connectPanel.add(connectBtn);
		
		consolePanel = new JPanel();
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
		JButton sendBtn = new JButton("Send");
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
		
		JTabbedPane tabs = new JTabbedPane();
		tabs.addTab("Connect",connectPanel);
		tabs.addTab("Console",consolePanel);
		
		window.add(tabs);
		
		window.setVisible(true);
		
		info("Starting Client...");
		
		connectBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				String ipAddr = ipInput.getText();
				int port = Integer.parseInt(ptInput.getText());
				try {
					info("Connecting to " + ipAddr + ":" + port);
					Socket clientSocket = new Socket(ipAddr,port);
					info("Connection established");
					outToServer = new DataOutputStream(clientSocket.getOutputStream());
					outToServer.writeBytes("AUTH:the-password\n");
					
					info("Starting listener thread...");
					
					Thread listenerThread = new Thread(new TCPListener(clientSocket));
					listenerThread.start();
					
					tabs.setEnabledAt(1,true);
					tabs.setSelectedIndex(1);
				} catch (IOException e) {
					error("ERROR: " + e.getMessage());
				}
			}
		});
		
		ActionListener submitListener = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String cmd = cmdInput.getText();
				try {
					outToServer.writeBytes(cmd + "\n");
				} catch (IOException e1) {
					error("Failed to send: " + e1.getMessage());
					info("Command: " + cmd);
				}
				cmdInput.setText("");
			}
		};
		
		cmdInput.addActionListener(submitListener);
		sendBtn.addActionListener(submitListener);
	}
}
