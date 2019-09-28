package servers;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import abacCore.Core;

public class Session extends Thread {
	private Socket socket;
	private Core core;
	InputStream inp = null;
	BufferedReader brinp = null;
	PrintWriter out = null;
	ArrayList<String> blackList;
	User user;
	int FPS = 3000;

	public Session(Socket clientSocket, Core core, ArrayList<String> blackList) {
		this.socket = clientSocket;
		this.core = core;
		this.blackList = blackList;
	}

	public void run() {

		try {
			inp = socket.getInputStream();
			brinp = new BufferedReader(new InputStreamReader(inp));
			out = new PrintWriter(socket.getOutputStream());
			if (blackList.contains(socket.getInetAddress().toString())) {
				System.out.println("user" + socket.getInetAddress().toString() + " blocked ");
				socket.close();
				return;
			}
			System.out.println("user not blocked initiat CHAP");
			Boolean ind = false;
			int i = 1;
			while (!ind) {
				ind = chapAuthent(socket);
				if (i > 4 || ind == null) {
					if (ind != null)
						// blackList.add(socket.getInetAddress().toString());
						socket.close();
					return;
				}
				if (ind == false) {
					System.out.println("failure: ");
					out.println("failure: ");
					out.flush();
					i++;
				}
			}
			System.out.println("success: welcome " + user.getName());
			out.println("success: ");
			out.flush();

		} catch (IOException e) {
			return;
		}

		String line;
		while (true) {
			try {
				System.out.println("start reading");
				line = brinp.readLine();
				if ((line == null) || line.equalsIgnoreCase("QUIT")) {
					socket.close();
					return;
				}
				System.out.println("result:" + line);
				Boolean access;
				String[] message = line.split(":");
				String res;
				switch (message[0]) {

				case "smartlock":
					// smartlock --> message[0]: objectID --> message[1]: r/w --> message[2]:
					// value --> message[3]
					System.out.println("geting permission");
					access = core.Permission(user.getId(), message[1], message[2]);
					if (message[2].equals("read") && access) {
						res = core.read(message[1], "state");
						System.out.println("success:" + res);
						out.println("success:" + res);
						out.flush();
					} else if (message[2].equals("write") && access) {
						core.write(message[1], "state", message[3]);
						System.out.println("success: ");
						out.println("success: ");
						out.flush();
					} else {
						System.out.println("failure: ");
						out.println("failure: ");
						out.flush();
					}
					break;

				case "heart":
					// heart --> message[0]: objectID --> message[1]
					System.out.println("geting permission");
					access = core.Permission(user.getId(), message[1], "read");
					if (access) {
						Thread t = new Thread(new Runnable() {
							@Override
							public void run() {
								try {
									while (true) {
										out.println("success:" + core.read(message[1], "state"));
										out.flush();
										System.out.println("success:" + core.read(message[1], "state"));
										Thread.sleep(FPS);
									}
								} catch (InterruptedException e) {
									out.println("Stop:");
									out.flush();
									System.out.println("Stop:");
								} catch (Exception e) {
									// TODO Auto-generated catch block
									System.out.println(e);
								}
							}
						});
						t.start();
						line = brinp.readLine();
						t.interrupt();
					} else {
						System.out.println("failure");
						out.println("failure:");
						out.flush();
					}
					break;
				case "firesensor":
					// firesensor --> message[0]: objectID --> message[1]
					System.out.println("geting permission");
					access = core.Permission(user.getId(), message[1], "read");
					if (access) {
						Thread tt = new Thread(new Runnable() {
							@Override
							public void run() {
								try {
									while (true) {
										String temp = core.read(message[1], "temp");
										String humidity = core.read(message[1], "humidity");
										out.println("success:" + temp + ":" + humidity);
										out.flush();
										System.out.println("success:" + temp + ":" + humidity);
										Thread.sleep(FPS);
									}
								} catch (InterruptedException e) {
									out.println("Stop:");
									out.flush();
									System.out.println("Stop:");
								} catch (Exception e) {
									// TODO Auto-generated catch block
									System.out.println(e);
								}
							}
						});
						tt.start();
						line = brinp.readLine();
						tt.interrupt();
					} else {
						System.out.println("failure");
						out.println("failure:");
						out.flush();
					}
					break;
				case "addehr":
					// addehr-->message[0]: fileName-->message[1]: doctorSpecialty-->message[2]:
					// level-->message[3]:length -->message[4]
					access = core.Permission(user.getId(), "291640", "read");
					if (access) {
						out.println("success:");
						out.flush();
						DataInputStream dIn = new DataInputStream(inp);
						int length = Integer.parseInt(message[4]);
						byte[] byt = new byte[length];
						if (length > 0) {
							dIn.readFully(byt, 0, byt.length); // read the message
							core.addEHR(byt, message[1], message[2]);
						}
					} else {
						System.out.println("failure");
						out.println("failure:");
						out.flush();
					}
					break;
				case "ehrlist":
					access = core.Permission(user.getId(), "291640", "read");
					if (access) {
						String temp = "success" + core.getEHRList();
						out.println(temp);
						out.flush();
						System.out.println(temp);
					} else {
						System.out.println("failure");
						out.println("failure:");
						out.flush();
					}
					break;
				case "readfile":
					// readFile --> message[0]: fileName --> message[1]
					access = core.Permission(user.getId(), "291640", "read");
					if (access) {
						byte[] byt = core.getEHR(message[1], user.getId());
						DataOutputStream dOut = new DataOutputStream(socket.getOutputStream());
						out.println("success:" + byt.length);
						out.flush();
						line = brinp.readLine();
						System.out.println("success:" + byt.length);
						dOut.write(byt, 0, byt.length);
						dOut.flush();
						System.out.println(line);
					} else {
						System.out.println("failure");
						out.println("failure:");
						out.flush();
					}
					break;
				case "alluserlist":
					// alluserlist --> message[0]
					System.out.println("geting permission");
					access = core.Permission(user.getId(), user.getId(), "read");
					if (access) {
						res = core.getUsersAll();
						System.out.println("success" + res);
						out.println("success" + res);
						out.flush();
					} else {
						System.out.println("failure");
						out.println("failure:");
						out.flush();
					}
					break;
				case "userlist":
					// userlist --> message[0]
					System.out.println("geting permission");
					access = core.Permission(user.getId(), user.getId(), "read");
					if (access) {
						res = core.getUsersForDen();
						System.out.println("success" + res);
						out.println("success" + res);
						out.flush();
					} else {
						System.out.println("failure");
						out.println("failure:");
						out.flush();
					}
					break;
				case "iotdevice":
					// iotdevice --> message[0]: deviceType --> message[1]
					switch (message[1]) {
					case "SmartLock":
						access = core.Permission(user.getId(), "241640", "read");
						if (access) {
							res = core.getTableObjects("smartlock", "id");
							System.out.println("success" + res);
							out.println("success" + res);
							out.flush();
						} else {
							System.out.println("failure");
							out.println("failure:");
							out.flush();
						}
						break;
					case "Heart Monitor":
						access = core.Permission(user.getId(), "281640", "read");
						if (access) {
							res = core.getTableObjects("heart", "id");
							System.out.println("success" + res);
							out.println("success" + res);
							out.flush();
						} else {
							System.out.println("failure");
							out.println("failure:");
							out.flush();
						}
						break;
					case "Heat Sensor":
						access = core.Permission(user.getId(), "251640", "read");
						if (access) {
							res = core.getTableObjects("firesensor", "id");
							System.out.println("success" + res);
							out.println("success" + res);
							out.flush();
						} else {
							out.println("failure:");
							out.flush();
						}
						break;

					default:
						out.println("incorrect type:");
						out.flush();
						break;
					}
					break;
				case "deletefile":
					// deletefile --> message[0]: fileName --> message[1]
					access = core.Permission(user.getId(), "291640", "write");
					if (access) {
						if (core.deleteEHR(message[1])) {
							System.out.println("success");
							out.println("success");
							out.flush();
							break;
						}
					}
					out.println("failure:");
					out.flush();
					break;
				case "timeresall":
					// timeresall -> message[0]: deviceType --> message[1]: endTime -->
					// message[2]message[3]message[4]:
					// startTime --> message[5]message[6]message[7]: ruleName --> message[9]
					access = core.Permission(user.getId(), "30", "write");
					if (access) {
						Map<String, String> m = new HashMap<>();
						m.put("STIME", message[5] + ":" + message[6] + ":" + message[7]);
						m.put("ETIME", message[2] + ":" + message[3] + ":" + message[4]);
						m.put("MODEL", "TimeResAll");
						switch (message[1]) {
						case "SmartLock":
							m.put("OBJECT", "241640");
							m.put("R_W", "write");
							core.addPolicy(m, message[8]);
							System.out.println("success");
							out.println("success");
							out.flush();
							break;
						case "Heart Monitor":
							m.put("OBJECT", "281640");
							m.put("R_W", "read");
							core.addPolicy(m, message[8]);
							System.out.println("success");
							out.println("success");
							out.flush();
							break;
						case "Heat Sensor":
							m.put("OBJECT", "251640");
							m.put("R_W", "read");
							core.addPolicy(m, message[8]);
							System.out.println("success");
							out.println("success");
							out.flush();
						}
						break;
					}
					System.out.println("failure");
					out.println("failure:");
					out.flush();
					break;
				case "timeresuser":
					// timeresall -> message[0]: deviceType --> message[1]: endTime -->
					// message[2]message[3]message[4]:
					// startTime --> message[5]message[6]message[7]: userName --> message[8]:
					// ruleName --> message[9]
					access = core.Permission(user.getId(), "30", "write");
					if (access) {
						Map<String, String> m = new HashMap<>();
						m.put("STIME", message[5] + ":" + message[6] + ":" + message[7]);
						m.put("ETIME", message[2] + ":" + message[3] + ":" + message[4]);
						m.put("IDLOCATION", core.getUser(message[8]).getId());
						m.put("MODEL", "TimeRes");
						switch (message[1]) {
						case "SmartLock":
							m.put("OBJECT", "241640");
							m.put("R_W", "write");
							core.addPolicy(m, message[9]);
							System.out.println("success");
							out.println("success");
							out.flush();
							break;
						case "Heart Monitor":
							m.put("OBJECT", "281640");
							m.put("R_W", "read");
							core.addPolicy(m, message[9]);
							System.out.println("success");
							out.println("success");
							out.flush();
							break;
						case "Heat Sensor":
							m.put("OBJECT", "251640");
							m.put("R_W", "read");
							core.addPolicy(m, message[9]);
							System.out.println("success");
							out.println("success");
							out.flush();
						}
						break;
					}
					System.out.println("failure");
					out.println("failure:");
					out.flush();
					break;
				case "timelist":
					// timelist --> message[0]
					System.out.println("geting permission");
					access = core.Permission(user.getId(), "30", "read");
					if (access) {
						res = core.getTableObjects("activerules", "name");
						System.out.println("success" + res);
						out.println("success" + res);
						out.flush();
					} else {
						System.out.println("failure");
						out.println("failure:");
						out.flush();
					}
					break;
				case "deletetime":
					// deletetime --> message[0]: ruleName --> message[1]
					access = core.Permission(user.getId(), "30", "write");
					if (access) {
						core.removePolicy(message[1]);
						System.out.println("success");
						out.println("success");
						out.flush();
						break;
					}
					System.out.println("failure");
					out.println("failure:");
					out.flush();
					break;
				case "lockall":
					// lockall --> message[0]: objectType --> message[1]
					access = core.Permission(user.getId(), "31", "write");
					if (access) {
						switch (message[1]) {
						case "SmartLock":
							core.denyOrAcceptAllDAC("smartlock", false);
							System.out.println("success");
							out.println("success");
							out.flush();
							break;
						case "Heart Monitor":
							core.denyOrAcceptAllDAC("heart", false);
							System.out.println("success");
							out.println("success");
							out.flush();
							break;
						case "Heat Sensor":
							core.denyOrAcceptAllDAC("firesensor", false);
							System.out.println("success");
							out.println("success");
							out.flush();
						}
					} else {
						System.out.println("failure");
						out.println("failure:");
						out.flush();
					}
					break;
				case "unlockall":
					// unlockall --> message[0]: objectType --> message[1]
					access = core.Permission(user.getId(), "31", "write");
					if (access) {
						switch (message[1]) {
						case "SmartLock":
							core.denyOrAcceptAllDAC("smartlock", true);
							System.out.println("success");
							out.println("success");
							out.flush();
							break;
						case "Heart Monitor":
							core.denyOrAcceptAllDAC("heart", true);
							System.out.println("success");
							out.println("success");
							out.flush();
							break;
						case "Heat Sensor":
							core.denyOrAcceptAllDAC("firesensor", true);
							System.out.println("success");
							out.println("success");
							out.flush();
						}
					} else {
						System.out.println("failure");
						out.println("failure:");
						out.flush();
					}
					break;
				case "lockuser":
					// lockall --> message[0]: objectType --> message[1]: user --> message[2]
					access = core.Permission(user.getId(), "31", "write");
					if (access) {
						switch (message[1]) {
						case "SmartLock":
							core.denyOrAcceptDAC(message[2], "smartlock", false);
							System.out.println("success");
							out.println("success");
							out.flush();
							break;
						case "Heart Monitor":
							core.denyOrAcceptDAC(message[2], "heart", false);
							System.out.println("success");
							out.println("success");
							out.flush();
							break;
						case "Heat Sensor":
							core.denyOrAcceptDAC(message[2], "firesensor", false);
							System.out.println("success");
							out.println("success");
							out.flush();
						}
					} else {
						System.out.println("failure");
						out.println("failure:");
						out.flush();
					}
					break;
				case "unlockuser":
					// unlockall --> message[0]: objectType --> message[1]: user --> message[2]
					access = core.Permission(user.getId(), "31", "write");
					if (access) {
						switch (message[1]) {
						case "SmartLock":
							core.denyOrAcceptDAC(message[2], "smartlock", true);
							System.out.println("success");
							out.println("success");
							out.flush();
							break;
						case "Heart Monitor":
							core.denyOrAcceptDAC(message[2], "heart", true);
							System.out.println("success");
							out.println("success");
							out.flush();
							break;
						case "Heat Sensor":
							core.denyOrAcceptDAC(message[2], "firesensor", true);
							System.out.println("success");
							out.println("success");
							out.flush();
						}
					} else {
						System.out.println("failure");
						out.println("failure:");
						out.flush();
					}
					break;
				case "locklist":
					// locklist --> message[0]: objectType --> massage[1]
					access = core.Permission(user.getId(), "31", "read");
					if (access) {
						switch (message[1]) {
						case "SmartLock":
							res = core.getDenyiedUserDAC("smartlock");
							System.out.println("success" + res);
							out.println("success" + res);
							out.flush();
							break;
						case "Heart Monitor":
							res = core.getDenyiedUserDAC("heart");
							System.out.println("success" + res);
							out.println("success" + res);
							out.flush();
							break;
						case "Heat Sensor":
							res = core.getDenyiedUserDAC("firesensor");
							System.out.println("success" + res);
							out.println("success" + res);
							out.flush();
						}
					} else {
						System.out.println("failure");
						out.println("failure:");
						out.flush();
					}
					break;
				case "adduser":
					// adduser --> message[0]: role --> message[1]: name --> message[2]: password
					// --> message[3]
					access = core.Permission(user.getId(), user.getId(), "write");
					if (access) {
						core.adduser(message[2], message[3], message[1]);
						System.out.println("success");
						out.println("success");
						out.flush();
					} else {
						System.out.println("failure");
						out.println("failure:");
						out.flush();
					}
					break;
				case "deleteuser":
					// deleteuser --> message[0]: name --> message[1]
					access = core.Permission(user.getId(), user.getId(), "write");
					if (access) {
						core.deleteuser(message[1]);
						System.out.println("success");
						out.println("success");
						out.flush();
					} else {
						System.out.println("failure");
						out.println("failure:");
						out.flush();
					}
					break;
				case "getcare":
					// getcare --> message[0]
					access = core.Permission(user.getId(), user.getId(), "write");
					if (access) {
						User u = core.getCaretaker();
						if (u == null)
							res = "null";
						else
							res = u.getName();
						System.out.println("success:" + res);
						out.println("success:" + res);
						out.flush();
					} else {
						System.out.println("failure");
						out.println("failure:");
						out.flush();
					}
					break;
				case "setcare":
					// setcare --> message[0]: name --> message[1]
					access = core.Permission(user.getId(), user.getId(), "write");
					if (access) {
						core.setCareTaker(message[1]);
						System.out.println("success");
						out.println("success");
						out.flush();
					} else {
						System.out.println("failure");
						out.println("failure:");
						out.flush();
					}
					break;
				case "removecare":
					// removecare --> message[0]
					access = core.Permission(user.getId(), user.getId(), "write");
					if (access) {
						core.removeCareTaker();
						System.out.println("success");
						out.println("success");
						out.flush();
					} else {
						System.out.println("failure");
						out.println("failure:");
						out.flush();
					}
					break;
				default:
					break;
				}

			} catch (Exception e) {
				System.out.println(e);
				out.println("failure:");
				out.flush();
			}
		}
	}

	public Boolean chapAuthent(Socket socket) {
		String[] temp, message;
		String challenge = null, res;
		try {

			res = brinp.readLine();
			System.out.println("---" + res);
			temp = res.split(":");
			if (!temp[0].equals("user"))
				throw new Exception();
			user = core.getUser(temp[1]);
			if (user == null)
				return false;
			challenge = generateRandomStringByUUIDNoDash();
			System.out.println("challeng == " + challenge);
			out.println("challenge:" + challenge);
			out.flush();
			res = MD5(challenge + user.getPassword());
			System.out.println("MD5(chall+pass) == " + res);
			message = brinp.readLine().split(":");
			System.out.println(message[1]);
			if (!message[0].equals("response"))
				throw new Exception();
			if (!message[1].equals(res))
				return false;
			return true;
		} catch (IOException e) {
			System.out.println(e);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			System.out.println(e);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			System.out.println(e);
		}
		return null;
	}

	public String MD5(String md5) {
		try {
			java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
			byte[] array = md.digest(md5.getBytes());
			StringBuffer sb = new StringBuffer();
			for (int i = 0; i < array.length; ++i) {
				sb.append(Integer.toHexString((array[i] & 0xFF) | 0x100).substring(1, 3));
			}
			return sb.toString();
		} catch (java.security.NoSuchAlgorithmException e) {
			System.out.println(e);
		}
		return null;
	}

	public static String generateRandomStringByUUIDNoDash() {
		return UUID.randomUUID().toString().replace("-", "");
	}
}
