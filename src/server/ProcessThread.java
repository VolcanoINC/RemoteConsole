package server;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;

public class ProcessThread implements Runnable {
	public Process proc;
	public OutputStream toProgram;
	private boolean isRunning = true;
	
	public ProcessThread(String cmd,String workDir) {
		Runtime rt = Runtime.getRuntime();
		try {
			proc = rt.exec(cmd,null,new File(workDir));
			toProgram = proc.getOutputStream();
		} catch (IOException e) {
			isRunning = false;
			Server.error("Unable to start process: " + e.getMessage(),true);
		}
	}
	
	@Override
	public void run() {
		if (!isRunning) return;
		
		BufferedReader fromProcess = new BufferedReader(new InputStreamReader(proc.getInputStream()));
		try {
			String line = null;
			while ((line = fromProcess.readLine()) != null && proc.isAlive()) {
				Server.output(line,true);
			}
			Server.info("Program closed with exit code: " + proc.waitFor(),true);
			Server.broadcast("PROC_EXIT:" + proc.exitValue());
		} catch (Exception e) {
			Server.error("Program output stream failed: " + e.getMessage(),true);
			Server.broadcast("PROC_EXIT:0");
		}
		isRunning = false;
	}
	
	public boolean running() {
		return isRunning;
	}
}
