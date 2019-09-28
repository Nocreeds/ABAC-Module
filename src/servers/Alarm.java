package servers;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;

import abacCore.Core;

public class Alarm extends Thread {
	private ArrayList<Socket> usersSocket = null;
	private Core core = null;
	InputStream inp = null;
	BufferedReader brinp = null;
	PrintWriter out = null;
	int alert;

	public Alarm(Core core, ArrayList<Socket> usersSocket) {
		this.core = core;
		this.usersSocket = usersSocket;
	}

	public void run() {
		while (true) {
			try {
				Thread.sleep(5000);
				int level = core.isAlarm();
				if (level != 0) {
					for (Socket user : usersSocket) {
						try {
							inp = user.getInputStream();
							brinp = new BufferedReader(new InputStreamReader(inp));
							out = new PrintWriter(user.getOutputStream());
							System.out.println("Alert:" + level);
							out.println(level);
							out.flush();
							alert = level;
						} catch (Exception e) {
							usersSocket.remove(user);
						}
					}
				} else if (alert != level) {
					for (Socket user : usersSocket) {
						try {
							inp = user.getInputStream();
							brinp = new BufferedReader(new InputStreamReader(inp));
							out = new PrintWriter(user.getOutputStream());
							System.out.println("Alert:" + level);
							out.println(level);
							out.flush();
							alert = level;
						} catch (Exception e) {
							usersSocket.remove(user);
						}
					}
				}
			} catch (Exception e) {
				System.out.println(e.getMessage());
			}
		}
	}
}
