package abacCore;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import co.junwei.cpabe.Cpabe;
import servers.User;

public class Core {
	private ArrayList<Map<String, String>> activeRules = new ArrayList<>();
	private Document mainDoc, alarmDoc;
	private Connection connection = null;
	private final String pubfile = "/home/pi/rasp/Resource/pub_key";
	private final String mskfile = "/home/pi/rasp/Resource/master_key";
	private final String policyfile = "/home/pi/rasp/Resource/Policies.xml";
	private final String alarmfile = "/home/pi/rasp/Resource/Alarm.xml";
	private Cpabe cpabe = null;
	private int alarm;

	public ArrayList<Map<String, String>> getActiveRules() {
		return activeRules;
	}

	public void init() throws Exception {
		/**************** Connect to the DB ***************/
		Class.forName("org.sqlite.JDBC");
		connection = DriverManager.getConnection("jdbc:sqlite:/home/pi/rasp/HomeSecurityDB.db");
		/**************** Load Files ***************/
		mainDoc = loadFile(policyfile);
		alarmDoc = loadFile(alarmfile);
		/**************** load policy models ***************/
		Statement stat = connection.createStatement();
		ResultSet res = stat.executeQuery("select map from activerules");
		while (res.next()) {
			activeRules.add(warppStringToMap(res.getString("map")));
		}
		ResultSet res1 = stat.executeQuery("select alert from system");
		while (res.next()) {
			alarm = res1.getInt(1);
		}
		stat.close();
		cpabe = new Cpabe();
	}

	private Document loadFile(String file) throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document doc = builder.parse(new File(file));
		DOMUtils.trimEmptyTextNodes(doc);
		return doc;
	}

	public String getObjectType(String IDObject) throws Exception {
		switch (IDObject.substring(0, 2)) {
		case "23":
			return "utilisateurs";
		case "24":
			return "smartlock";
		case "25":
			return "firesensor";
		case "26":
			return "camera";
		case "27":
			return "bloodpressur";
		case "28":
			return "heart";
		case "29":
			return "ehr";
		case "30":
			return "activerules";
		case "31":
			return "dac";
		default:
			throw new Exception("invalide Object ID");
		}
	}

	public static Map<String, String> getTable(String att) throws Exception {
		String[] res = att.split("\\.");
		Map<String, String> m = new HashMap<String, String>();
		if (res.length == 2) {
			m.put("class", res[0]);
			m.put("column", res[1]);
			return m;
		} else if (res.length == 3) {
			m.put("class", res[0]);
			m.put("column", res[1]);
			m.put("line", res[2]);
			return m;
		} else if (res.length == 1) {
			m.put("command", res[0]);
			return m;
		} else
			throw new Exception("invalid policy: invalide attribute \"" + att + "\"");
	}

	// Recursively loop through and calculate out one policy from the XML file
	public Boolean policyCalculator(Node node, String IDUser, String IDObject) throws Exception {
		if (node.getNodeType() == Node.ELEMENT_NODE) {
			if (node.getNodeName().equals("and")) {
				NodeList nl = node.getChildNodes();
				Boolean fRes = true;
				int j;
				for (j = 0; j < nl.getLength(); j++) {
					Boolean res = policyCalculator(nl.item(j), IDUser, IDObject);
					if (res != null) {
						fRes = fRes && res;
						// System.out.print(" and ");
					}

				}
				if (j == 0)
					throw new Exception("invalid policy: invalide \"and\" statement");
				else
					return fRes;

			} else if (node.getNodeName().equals("or")) {
				NodeList nl = node.getChildNodes();
				Boolean fRes = false;
				int j;
				for (j = 0; j < nl.getLength(); j++) {
					Boolean res = policyCalculator(nl.item(j), IDUser, IDObject);
					if (res != null) {
						// System.out.print(" or ");
						fRes = fRes || res;
					}
				}
				if (j == 0)
					return true;
				else
					return fRes;

			} else if (node.getNodeName().equals("att")) {
				Map<String, String> m = getTable(node.getTextContent());
				Statement statement = connection.createStatement();
				ResultSet res;
				if (m.containsKey("line")) {
					res = statement.executeQuery(
							"SELECT " + m.get("column") + " FROM " + m.get("class") + " WHERE ID = " + m.get("line"));
				} else if (m.get("class").equals("utilisateurs")) {
					res = statement.executeQuery(
							"SELECT " + m.get("column") + " FROM " + m.get("class") + " WHERE ID = " + IDUser);
				} else
					res = statement.executeQuery(
							"SELECT " + m.get("column") + " FROM " + m.get("class") + " WHERE ID = " + IDObject);
				// System.out.print(" " + res.getBoolean(1) + " ");
				Boolean boll = res.getBoolean(1);
				statement.close();
				return boll;
			} else if (node.getNodeName().equals("equal")) {
				NodeList nl = node.getChildNodes();
				IntOrString[] res = new IntOrString[2];
				int j, i = 0;
				for (j = 0; j < nl.getLength(); j++) {
					if (nl.item(j).getNodeType() == Node.ELEMENT_NODE) {
						// System.out.println(nl.item(j).getTextContent());
						if (i > 1)
							throw new Exception("invalid policy: invalide \"equal\" statement");
						if (nl.item(j).getNodeName().equals("att")) {
							res[i] = result(nl.item(j), IDUser, IDObject);
						} else if (nl.item(j).getNodeName().equals("val"))
							res[i] = new IntOrString(nl.item(j).getTextContent());
						else
							throw new Exception(
									"invalid policy: invalide \"" + nl.item(j).getNodeName() + "\" attribute");
						i++;
					}
				}
				if (j == 0 && i == 0)
					throw new Exception("invalid policy: invalide \"equal\" statement");
				// System.out.print("(" + res[0] + " == " + res[1] + " / " +
				// res[0].equal(res[1]) + ")");
				return res[0].equal(res[1]);
			} else if (node.getNodeName().equals("notequal")) {
				NodeList nl = node.getChildNodes();
				IntOrString[] res = new IntOrString[2];
				int j, i = 0;
				for (j = 0; j < nl.getLength(); j++) {
					if (nl.item(j).getNodeType() == Node.ELEMENT_NODE) {
						// System.out.println(nl.item(j).getTextContent());
						if (i > 1)
							throw new Exception("invalid policy: invalide \"notequal\" statement");
						if (nl.item(j).getNodeName().equals("att")) {
							res[i] = result(nl.item(j), IDUser, IDObject);
						} else if (nl.item(j).getNodeName().equals("val"))
							res[i] = new IntOrString(nl.item(j).getTextContent());
						else
							throw new Exception(
									"invalid policy: invalide \"" + nl.item(j).getNodeName() + "\" attribute");
						i++;
					}
				}
				if (j == 0 && i == 0)
					throw new Exception("invalid policy: invalide \"notequal\" statement");
				// System.out.print("(" + res[0] + " != " + res[1] + " / " +
				// !res[0].equal(res[1]) + ")");
				return !res[0].equal(res[1]);
			} else if (node.getNodeName().equals("supequal")) {
				NodeList nl = node.getChildNodes();
				IntOrString[] res = new IntOrString[2];
				int j, i = 0;
				for (j = 0; j < nl.getLength(); j++) {
					if (nl.item(j).getNodeType() == Node.ELEMENT_NODE) {
						if (i > 1)
							throw new Exception("invalid policy: invalide \"supequal\" statement");
						if (nl.item(j).getNodeName().equals("att")) {
							res[i] = result(nl.item(j), IDUser, IDObject);
						} else if (nl.item(j).getNodeName().equals("val"))
							res[i] = new IntOrString(nl.item(j).getTextContent());
						else
							throw new Exception(
									"invalid policy: invalide \"" + nl.item(j).getNodeName() + "\" attribute");
						i++;
					}
				}
				if (j == 0 && i == 0)
					throw new Exception("invalid policy: invalide \"supequal\" statement");
				// System.out.print("(" + res[0] + " >= " + res[1] + " / " +
				// res[0].supequal(res[1]) + ")");
				return res[0].supequal(res[1]);
			} else if (node.getNodeName().equals("infequal")) {
				NodeList nl = node.getChildNodes();
				IntOrString[] res = new IntOrString[2];
				int j, i = 0;
				for (j = 0; j < nl.getLength(); j++) {
					if (nl.item(j).getNodeType() == Node.ELEMENT_NODE) {
						if (i > 1)
							throw new Exception("invalid policy: invalide \"infequal\" statement");
						if (nl.item(j).getNodeName().equals("att")) {
							res[i] = result(nl.item(j), IDUser, IDObject);
						} else if (nl.item(j).getNodeName().equals("val"))
							res[i] = new IntOrString(nl.item(j).getTextContent());
						else
							throw new Exception(
									"invalid policy: invalide \"" + nl.item(j).getNodeName() + "\" attribute");
						i++;
					}
				}
				if (j == 0 && i == 0)
					throw new Exception("invalid policy: invalide \"infequal\" statement");
				// System.out.print("(" + res[0] + " <= " + res[1] + " / " +
				// res[0].infequal(res[1]) + ")");
				return res[0].infequal(res[1]);
			} else if (node.getNodeName().equals("sup")) {
				NodeList nl = node.getChildNodes();
				IntOrString[] res = new IntOrString[2];
				int j, i = 0;
				for (j = 0; j < nl.getLength(); j++) {
					if (nl.item(j).getNodeType() == Node.ELEMENT_NODE) {
						if (i > 1)
							throw new Exception("invalid policy: invalide \"sup\" statement");
						if (nl.item(j).getNodeName().equals("att")) {
							res[i] = result(nl.item(j), IDUser, IDObject);
						} else if (nl.item(j).getNodeName().equals("val"))
							res[i] = new IntOrString(nl.item(j).getTextContent());
						else
							throw new Exception(
									"invalid policy: invalide \"" + nl.item(j).getNodeName() + "\" attribute");
						i++;
					}
				}
				if (j == 0 && i == 0)
					throw new Exception("invalid policy: invalide \"sup\" statement");
				// System.out.print("(" + res[0] + " > " + res[1] + " / " + res[0].sup(res[1]) +
				// ")");
				return res[0].sup(res[1]);
			} else if (node.getNodeName().equals("inf")) {
				NodeList nl = node.getChildNodes();
				IntOrString[] res = new IntOrString[2];
				int j, i = 0;
				for (j = 0; j < nl.getLength(); j++) {
					if (nl.item(j).getNodeType() == Node.ELEMENT_NODE) {
						if (i > 1)
							throw new Exception("invalid policy: invalide \"inf\" statement");
						if (nl.item(j).getNodeName().equals("att")) {
							res[i] = result(nl.item(j), IDUser, IDObject);
						} else if (nl.item(j).getNodeName().equals("val"))
							res[i] = new IntOrString(nl.item(j).getTextContent());
						else
							throw new Exception(
									"invalid policy: invalide \"" + nl.item(j).getNodeName() + "\" attribute");
						i++;
					}
				}
				if (j == 0 && i == 0)
					throw new Exception("invalid policy: invalide \"inf\" statement");
				// System.out.print("(" + res[0] + " < " + res[1] + " / " + res[0].inf(res[1]) +
				// ")");
				return res[0].inf(res[1]);
			}

			else
				throw new Exception("invalid policy: invalid element \"" + node.getNodeName() + "\"");

			// System.out.println(node.getNodeName()+" : "+node.getTextContent()+" j = "+i+"
			// length = "+node.getChildNodes().getLength());
		}
		return null;
	}

	public Boolean dacCalculator(String IDUser, String IDObject) throws Exception {

		Statement statement = connection.createStatement();
		ResultSet res = statement.executeQuery("SELECT " + getObjectType(IDObject) + " FROM dac WHERE id = " + IDUser);
		if (res.isClosed()) {
			throw new Exception("User ID or object ID incorrect");
		}
		Boolean boll = res.getBoolean(1);
		statement.close();
		return boll;
	}

	public Boolean Permission(String IDUser, String IDObject, String r_w) throws Exception {
		if (dacCalculator(IDUser, IDObject)) {
			return false;
		}
		Boolean res;
		NodeList nodes = mainDoc.getFirstChild().getChildNodes();
		for (int i = 0; i < nodes.getLength(); i++) {
			if (nodes.item(i).getNodeName().equals(getObjectType(IDObject))) {
				NodeList nodes1 = nodes.item(i).getChildNodes();
				for (int j = 0; j < nodes1.getLength(); j++) {
					if (nodes1.item(j).getNodeName().equals(r_w)) {
						NodeList nodes2 = nodes1.item(j).getChildNodes();
						for (int k = 0; k < nodes2.getLength(); k++) {
							if (nodes2.item(k).getNodeType() == Node.ELEMENT_NODE) {
								res = policyCalculator(nodes2.item(k), IDUser, IDObject);
								if (res != null)
									return res;
								throw new Exception("inconue exeption !!!");
							}
						}
					}
				}
			}
		}
		return true;
	}

	private IntOrString result(Node node, String IDUser, String IDObject) throws Exception {
		Map<String, String> m = getTable(node.getTextContent());
		Statement statement = connection.createStatement();
		ResultSet res;
		if (m.containsKey("command")) {
			res = statement.executeQuery("SELECT " + m.get("command"));
		} else if (m.containsKey("line")) {
			res = statement.executeQuery(
					"SELECT " + m.get("column") + " FROM " + m.get("class") + " WHERE ID = " + m.get("line"));
		} else if (m.get("class").equals("utilisateurs")) {
			res = statement
					.executeQuery("SELECT " + m.get("column") + " FROM " + m.get("class") + " WHERE ID = " + IDUser);
		} else
			res = statement
					.executeQuery("SELECT " + m.get("column") + " FROM " + m.get("class") + " WHERE ID = " + IDObject);
		if (res.getString(1).equals(""))
			throw new Exception("invalide attribute \"" + node.getTextContent() + "\" = \"" + res.getString(1) + "\"");
		IntOrString in = new IntOrString(res.getString(1));
		statement.close();
		return in;
	}

	// add pri-defined polices to policies.xml (we don't need the object ID but the
	// class ID)
	public void addPolicy(Map<String, String> m, String name) throws Exception {
		Node newNode = DOMUtils.creatXMLNode(m);
		DOMUtils.trimEmptyTextNodes(newNode);
		NodeList nodes = mainDoc.getFirstChild().getChildNodes();
		for (int i = 0; i < nodes.getLength(); i++) {
			if (nodes.item(i).getNodeName().equals(getObjectType(m.get("OBJECT")))) {
				NodeList nodes1 = nodes.item(i).getChildNodes();
				for (int j = 0; j < nodes1.getLength(); j++) {
					if (nodes1.item(j).getNodeName().equals(m.get("R_W"))) {
						NodeList nodes2 = nodes1.item(j).getChildNodes();
						for (int k = 0; k < nodes2.getLength(); k++) {
							if (nodes2.item(k).getNodeName().equals("and")) {
								if (((Element) newNode).getAttribute("type").equals("deny")) {
									Node n = mainDoc.importNode(newNode, true);
									nodes2.item(k).appendChild(n);
								} else if (((Element) newNode).getAttribute("type").equals("permit")) {
									NodeList nodes3 = nodes2.item(k).getChildNodes();
									for (int l = 0; l < nodes3.getLength(); l++) {
										if (nodes3.item(l).getNodeName().equals("or")) {
											Node n = mainDoc.importNode(newNode, true);
											nodes3.item(l).appendChild(n);
										}
									}
								} else
									throw new Exception("invalid Policy Model type");

							}
						}
					}
				}
			}
		}
		Statement statement = connection.createStatement();
		statement.executeUpdate(
				"INSERT INTO main.activerules (map, name) VALUES (\"" + warppMap(m) + "\", \"" + name + "\")");
		DOMUtils.xMLPrinter(mainDoc);
		mainDoc = loadFile(policyfile);
		activeRules.add(m);
		statement.close();
	}

	public void CleanExpired() throws Exception {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date date = new Date(), dateMap;
		ArrayList<Map<String, String>> deletItem = new ArrayList<Map<String, String>>();
		Statement stat = connection.createStatement();
		Node node;
		for (Map<String, String> map : activeRules) {
			dateMap = dateFormat.parse(map.get("ETIME"));
			if (dateMap.before(date)) {
				node = DOMUtils.creatXMLNode(map);
				stat.executeUpdate("DELETE FROM activerules WHERE map = \"" + warppMap(map) + "\"");
				deletItem.add(map);
				DOMUtils.deleteXML(mainDoc.getFirstChild(), node);
			}
		}
		activeRules.removeAll(deletItem);
		stat.close();
		DOMUtils.xMLPrinter(mainDoc);
	}

	public void removePolicy(String name) throws Exception {
		Statement stat = connection.createStatement();
		ResultSet res = stat.executeQuery("SELECT map FROM activerules WHERE name = \"" + name + "\"");
		if (!res.next())
			throw new Exception("no active rules found");
		Map<String, String> m = warppStringToMap(res.getString(1));
		stat.executeUpdate("DELETE FROM activerules WHERE name = \"" + name + "\"");
		DOMUtils.deleteXML(mainDoc.getFirstChild(), DOMUtils.creatXMLNode(m));
		activeRules.remove(m);
		stat.close();
		DOMUtils.xMLPrinter(mainDoc);
		mainDoc = loadFile(policyfile);
	}

	public String warppMap(Map<String, String> map) throws Exception {
		String res = "";
		for (Map.Entry<String, String> item : map.entrySet()) {
			res = res + item.getKey() + "|" + item.getValue() + ".";
		}
		if (res.equals(""))
			throw new Exception("invalid Rule Map");
		return res.substring(0, res.length() - 1);
	}

	public Map<String, String> warppStringToMap(String str) throws Exception {
		Map<String, String> res = new HashMap<>();
		String[] list = str.split("\\.");
		for (String items : list) {
			String[] item = items.split("\\|");
			if (item.length != 2)
				throw new Exception("invalid Rule Map String");
			res.put(item[0], item[1]);
		}
		return res;
	}

	// the function that change a user permission in DAC
	public boolean denyOrAcceptDAC(String user, String objectType, boolean decision) throws Exception {

		try {
			Statement stat = connection.createStatement();
			stat.executeUpdate("UPDATE dac SET \"" + objectType + "\"= " + ((decision == false) ? "1" : "0")
					+ " WHERE \"_rowid_\"=" + getUser(user).getId());
		} catch (SQLException e) {
			throw new Exception("Error user " + user + " or object " + objectType + " not fund");
		}
		return false;
	}

	public boolean denyOrAcceptAllDAC(String objectType, boolean decision) throws Exception {

		try {
			Statement stat = connection.createStatement();
			stat.executeUpdate("UPDATE dac SET \"" + objectType + "\"= " + ((decision == false) ? "1" : "0")
					+ " WHERE id IN (SELECT id FROM utilisateurs WHERE role <> \"owner\" AND active <> 1)");
		} catch (SQLException e) {
			System.out.println(e);
			// throw new Exception("Error object " + objectType + " not fund");
		}
		return false;
	}

	public String getDenyiedUserDAC(String objectType) throws SQLException {
		String names = "";
		Statement s = connection.createStatement();
		ResultSet res = s.executeQuery(
				"SELECT name FROM utilisateurs WHERE id in (SELECT id FROM dac WHERE \"" + objectType + "\" = 1);");
		while (res.next()) {
			names = names + ":" + res.getString("name");
		}
		s.close();
		return names;
	}

	// the map => {(docname, type)}
	public void addEHR(byte[] file, String fileName, String docSpeciality) throws Exception {
		Statement s = connection.createStatement();
		// check that the doctor specialty exist
		ResultSet res = s.executeQuery("SELECT count(*) FROM abepolicy WHERE type = \"" + docSpeciality + "\"");
		if (res.getInt(1) != 1)
			throw new Exception("invalid doctor speciality");
		// add the new EHR
		s.executeUpdate("INSERT INTO ehr(\"fileName\",\"docSpeciality\") VALUES (\"" + fileName + "\", \""
				+ docSpeciality + "\")");
		String name = "/home/pi/rasp/Resource/" + fileName;
		// Crypt and save the file
		res = s.executeQuery("SELECT policy FROM abepolicy WHERE type = \"" + docSpeciality + "\"");
		String policy = res.getString(1);
		s.close();
		cpabe.enc(pubfile, policy, file, name);
	}

	public byte[] getEHR(String encfile, String IDUser) throws Exception {
		Statement s = connection.createStatement();
		ResultSet res = s.executeQuery("SELECT alert FROM system");
		if (!res.next())
			throw new Exception("system table empty");
		String att = "alert:" + res.getString(1);
		ResultSet res1 = s.executeQuery("SELECT name, role, active FROM utilisateurs WHERE id = \"" + IDUser + "\";");
		if (!res1.next())
			throw new Exception("invalid user");
		att = " name:" + res1.getString(1) + " role:" + res1.getString(2) + " active:" + res1.getString(3);
		// add system attribute to att before creating a key
		byte[] privetKey = cpabe.keygen(pubfile, mskfile, att);
		s.close();
		return cpabe.dec(pubfile, privetKey, "/home/pi/rasp/Resource/" + encfile);
	}

	public boolean deleteEHR(String encfile) throws Exception {
		Statement s = connection.createStatement();
		int n = s.executeUpdate("DELETE FROM ehr WHERE fileName = ('" + encfile + "')");
		if (n < 1) {
			System.out.println("Failed to delete the file");
			return false;
		}
		File file = new File("/home/pi/rasp/Resource/" + encfile);
		if (file.delete()) {
			System.out.println("File deleted successfully");
			return true;
		} else {
			System.out.println("Failed to delete the file");
			return false;
		}
	}

	public User getUser(String user) throws SQLException {
		Statement s = connection.createStatement();
		ResultSet res = s
				.executeQuery("SELECT name, password, role, ID FROM utilisateurs WHERE name = \"" + user + "\"");
		if (res.next()) {
			User u = new User(res.getString(1), res.getString(2), res.getString(3), res.getString(4));
			s.close();
			return u;
		}
		s.close();
		return null;
	}

	public String getTableObjects(String table, String object) throws SQLException {
		String names = "";
		Statement s = connection.createStatement();
		ResultSet res = s.executeQuery("SELECT \"" + object + "\" FROM \"" + table + "\";");
		while (res.next()) {
			names = names + ":" + res.getString(object);
		}
		s.close();
		return names;
	}

	public String getUsersAll() throws SQLException {
		String names = "";
		Statement s = connection.createStatement();
		ResultSet res = s.executeQuery("SELECT name FROM utilisateurs WHERE role <> \"owner\";");
		while (res.next()) {
			names = names + ":" + res.getString("name");
		}
		s.close();
		return names;
	}

	public String getUsersForDen() throws SQLException {
		String names = "";
		Statement s = connection.createStatement();
		ResultSet res = s.executeQuery("SELECT name FROM utilisateurs WHERE role <> \"owner\" AND active <> 1;");
		while (res.next()) {
			names = names + ":" + res.getString("name");
		}
		s.close();
		return names;
	}

	public void write(String id, String col, String val) throws Exception {
		Statement s = connection.createStatement();
		s.executeUpdate("UPDATE \"main\".\"" + getObjectType(id) + "\" SET \"" + col + "\"=\"" + val
				+ "\" WHERE \"_rowid_\"='" + id + "'");
		s.close();
	}

	public String read(String id, String col) throws Exception {
		Statement s = connection.createStatement();
		ResultSet res = s.executeQuery(
				"SELECT \"" + col + "\" FROM \"" + getObjectType(id) + "\" WHERE \"_rowid_\"='" + id + "'");
		String re = res.getString(1);
		s.close();
		return re;
	}

	public String getEHRList() throws Exception {
		Statement s = connection.createStatement();
		ResultSet res = s.executeQuery("SELECT fileName FROM  ehr");
		String message = "";
		while (res.next()) {
			message = message + ":" + res.getString(1);
		}
		s.close();
		return message;
	}

	public void adduser(String name, String password, String role) throws SQLException {
		Statement s = connection.createStatement();
		s.executeUpdate("INSERT INTO \"main\".\"utilisateurs\"(\"Name\",\"password\",\"role\") VALUES (\"" + name
				+ "\", \"" + password + "\", \"" + role + "\");");
		s.executeUpdate("INSERT INTO dac (id) VALUES ((SELECT id FROM utilisateurs WHERE name = \"" + name + "\"));");
		s.close();
	}

	public void deleteuser(String name) throws SQLException {
		Statement s = connection.createStatement();
		s.executeUpdate("DELETE FROM \"main\".\"utilisateurs\" WHERE name = \"" + name + "\";");
		s.executeUpdate("DELETE FROM dac WHERE id = (SELECT id FROM utilisateurs WHERE name = \"" + name + "\");");
		s.close();
	}

	public User getCaretaker() throws SQLException {
		Statement s = connection.createStatement();
		ResultSet res = s.executeQuery("SELECT name, password, role, ID FROM utilisateurs WHERE active = 1;");
		if (res.next()) {
			User u = new User(res.getString(1), res.getString(2), res.getString(3), res.getString(4));
			s.close();
			return u;
		}
		s.close();
		return null;
	}

	public void setCareTaker(String name) throws SQLException {
		Statement s = connection.createStatement();
		s.executeUpdate("UPDATE \"main\".\"utilisateurs\" SET \"active\"= 0");
		s.executeUpdate("UPDATE \"main\".\"utilisateurs\" SET \"active\"= 1 WHERE \"name\"='" + name + "';");
		s.executeUpdate("DELETE FROM dac WHERE id = (SELECT id FROM utilisateurs WHERE name = \"" + name + "\");");
		s.executeUpdate("INSERT INTO dac (id) VALUES ((SELECT id FROM utilisateurs WHERE name = \"" + name + "\"));");
		s.close();
	}

	public void removeCareTaker() throws SQLException {
		Statement s = connection.createStatement();
		s.executeUpdate("UPDATE \"main\".\"utilisateurs\" SET \"active\"= 0");
		s.close();
	}

	public int isAlarm() throws Exception {
		Boolean res;
		int ind = 3;
		NodeList nodes = alarmDoc.getFirstChild().getChildNodes();
		for (int i = 0; i < nodes.getLength(); i++) {
			if (nodes.item(i).getNodeType() == Node.ELEMENT_NODE) {
				res = policyCalculator(nodes.item(i).getFirstChild(), "", "");
				if (res != null) {
					if (res) {
						Statement s = connection.createStatement();
						s.executeUpdate("UPDATE \"main\".\"system\" SET \"alert\"= " + ind);
						s.close();
						alarm = ind;
						return ind;
					} else {
						// System.out.println("l" + ind);
						ind--;
					}
				} else
					throw new Exception("inconue exeption !!!");
			}
		}
		if (alarm != 0) {
			Statement s = connection.createStatement();
			s.executeUpdate("UPDATE \"main\".\"system\" SET \"alert\"= " + 0);
			s.close();
			alarm = 0;
		}
		return 0;
	}
}
