package servers;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

import abacCore.Core;

public class Server {

	static final int PORT = 4444, PORTALARM = 3333;
	static Core core = new Core();
	static ArrayList<String> blackList = new ArrayList<>();

	public static void main(String args[]) {
		ServerSocket serverSocket = null, serverAlarm = null;
		Socket socket = null, socketAlarm = null;
		ArrayList<Socket> usersSocket = null;

		try {
			core.init();
			usersSocket = new ArrayList<Socket>();
			IoTs iot = new IoTs(core);
			Alarm alarm = new Alarm(core, usersSocket);
			iot.start();
			alarm.start();
			serverSocket = new ServerSocket(PORT);
			serverAlarm = new ServerSocket(PORTALARM);
		} catch (Exception e) {
			e.printStackTrace();
		}
		while (true) {
			try {
				System.out.println("connecting ...");
				socket = serverSocket.accept();
				socketAlarm = serverAlarm.accept();
				usersSocket.add(socketAlarm);

			} catch (IOException e) {
				System.out.println("I/O error: " + e);
			}
			// new thread for a client
			new Session(socket, core, blackList).start();
		}
	}
}
