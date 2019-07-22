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

public class Core {
	private ArrayList<Map <String, String>> activeRules = new ArrayList<>();
	private Document mainDoc;
	private Connection connection = null;
	
	public ArrayList<Map<String, String>> getActiveRules() {
		return activeRules;
	}

	public void init() throws Exception {
		Class.forName("org.sqlite.JDBC");
		connection = DriverManager.getConnection("jdbc:sqlite:HomeSecurityDB.db");
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		mainDoc = builder.parse(new File("Policies.xml"));
		DOMUtils.trimEmptyTextNodes(mainDoc);
		/****************load policy models***************/
		Statement stat = connection.createStatement();
		ResultSet res = stat.executeQuery("select map from activerules");
		while (res.next()) {
			activeRules.add(warppStringToMap(res.getString("map")));
		}stat.close();
	}
	
	public String getObjectType(String IDObject) throws Exception {
		switch (IDObject.substring(0, 2)) {
		case "24": return "smartlock";
		case "25": return "firesensor";
		case "26": return "camera";
		case "27": return "bloodpressur";
		default: throw new Exception("invalide Object ID");
		}
	}
	
	public static Map<String, String> getTable(String att) throws Exception {
		String [] res = att.split("\\.");
		Map<String, String> m = new HashMap<String, String>();
		if(res.length >= 2 && res.length <= 3) {
			m.put("class", res[0]);
			m.put("column", res[1]);
			if(res.length == 3)m.put("line", res[2]);
			return m;
		}else throw new Exception("invalid policy: invalide attribute \""+att+"\"");
	}
	
	//Recursively loop through and calculate out one policy from the XML file
	public Boolean policyCalculator(Node node, String IDUser, String IDObject) throws Exception{
	       if(node.getNodeType() == Node.ELEMENT_NODE){
	    	   if(node.getNodeName().equals("and")) {
		           NodeList nl=node.getChildNodes();
		           Boolean fRes = true;
		           int j;
		           for(j=0;j<nl.getLength();j++) {
		        	   Boolean res = policyCalculator(nl.item(j), IDUser, IDObject);
		        	   if(res != null) {
		        		   fRes = fRes && res;
		        		   System.out.print(" and ");
		        	   }
		        	   
		           }
		           if(j == 0) throw new Exception("invalid policy: invalide \"and\" statement");
		           else return fRes;

	    	   }else if(node.getNodeName().equals("or")) {
		           NodeList nl=node.getChildNodes();
		           Boolean fRes = false;
		           int j;
		           for(j=0;j<nl.getLength();j++) {
		        	   Boolean res = policyCalculator(nl.item(j), IDUser, IDObject);
		        	   if(res != null) {
		        		   System.out.print(" or ");
		        		   fRes = fRes || res;
		        		   
		        	   }
		           }
		           if(j == 0) throw new Exception("invalid policy: invalide \"or\" statement");
		           else return fRes;

	    	   }else if(node.getNodeName().equals("att")) {
	    		   Map<String, String> m = getTable(node.getTextContent());
	    		   Statement statement = connection.createStatement();
	    		   ResultSet res;
	    		   if(m.containsKey("line")) {
	    				res = statement.executeQuery("SELECT "+m.get("column")+" FROM "+m.get("class")+" WHERE ID = "+m.get("line"));
	    			}else if(m.get("class").equals("utilisateurs")) {
	    				res = statement.executeQuery("SELECT "+m.get("column")+" FROM "+m.get("class")+" WHERE ID = "+IDUser);
	    			}else res = statement.executeQuery("SELECT "+m.get("column")+" FROM "+m.get("class")+" WHERE ID = "+IDObject);
	    		   System.out.print(" "+res.getBoolean(1)+" ");
	    		   Boolean boll = res.getBoolean(1);
	    		   statement.close();
	    		   return boll;
	    	   }else if(node.getNodeName().equals("equal")) {
	    		   NodeList nl=node.getChildNodes();
		           IntOrString [] res = new IntOrString[2];
		           int j,i=0;
		           for(j=0;j<nl.getLength();j++) {
		        	   if(nl.item(j).getNodeType() == Node.ELEMENT_NODE) {
		        		   System.out.println(nl.item(j).getTextContent());
		        		   if (i>1) throw new Exception("invalid policy: invalide \"equal\" statement");
		        		   if(nl.item(j).getNodeName().equals("att")) {
		        			   res[i] = result(nl.item(j), IDUser, IDObject);
		        		   }else if(nl.item(j).getNodeName().equals("val")) res[i] = new IntOrString(nl.item(j).getTextContent());
		        		   else throw new Exception("invalid policy: invalide \""+nl.item(j).getNodeName()+"\" attribute");
		        		   i++;
		        	   }
		           }
		           if(j == 0 && i == 0) throw new Exception("invalid policy: invalide \"equal\" statement");
		           System.out.print("("+res[0]+" == "+res[1]+" / "+res[0].equal(res[1])+")");
		           return res[0].equal(res[1]); 
	    	   }else if(node.getNodeName().equals("supequal")) {
	    		   NodeList nl=node.getChildNodes();
		           IntOrString [] res = new IntOrString[2];
		           int j,i=0;
		           for(j=0;j<nl.getLength();j++) {
		        	   if(nl.item(j).getNodeType() == Node.ELEMENT_NODE) {
		        		   if (i>1) throw new Exception("invalid policy: invalide \"supequal\" statement");
		        		   if(nl.item(j).getNodeName().equals("att")) {
		        			   res[i] = result(nl.item(j), IDUser, IDObject);
		        		   }else if(nl.item(j).getNodeName().equals("val")) res[i] = new IntOrString(nl.item(j).getTextContent());
		        		   else throw new Exception("invalid policy: invalide \""+nl.item(j).getNodeName()+"\" attribute");
		        		   i++;
		        	   }
		           }
		           if(j == 0 && i == 0) throw new Exception("invalid policy: invalide \"supequal\" statement");
		           System.out.print("("+res[0]+" >= "+res[1]+" / "+res[0].supequal(res[1])+")");
		           return res[0].supequal(res[1]); 
	    	   }else if(node.getNodeName().equals("infequal")) {
	    		   NodeList nl=node.getChildNodes();
		           IntOrString [] res = new IntOrString[2];
		           int j,i=0;
		           for(j=0;j<nl.getLength();j++) {
		        	   if(nl.item(j).getNodeType() == Node.ELEMENT_NODE) {
		        		   if (i>1) throw new Exception("invalid policy: invalide \"infequal\" statement");
		        		   if(nl.item(j).getNodeName().equals("att")) {
		        			   res[i] = result(nl.item(j), IDUser, IDObject);
		        		   }else if(nl.item(j).getNodeName().equals("val")) res[i] = new IntOrString(nl.item(j).getTextContent());
		        		   else throw new Exception("invalid policy: invalide \""+nl.item(j).getNodeName()+"\" attribute");
		        		   i++;
		        	   }
		           }
		           if(j == 0 && i == 0) throw new Exception("invalid policy: invalide \"infequal\" statement");
		           System.out.print("("+res[0]+" <= "+res[1]+" / "+res[0].infequal(res[1])+")");
		           return res[0].infequal(res[1]);
	    	   }else if(node.getNodeName().equals("sup")) {
	    		   NodeList nl=node.getChildNodes();
		           IntOrString [] res = new IntOrString[2];
		           int j,i=0;
		           for(j=0;j<nl.getLength();j++) {
		        	   if(nl.item(j).getNodeType() == Node.ELEMENT_NODE) {
		        		   if (i>1) throw new Exception("invalid policy: invalide \"sup\" statement");
		        		   if(nl.item(j).getNodeName().equals("att")) {
		        			   res[i] = result(nl.item(j), IDUser, IDObject);
		        		   }else if(nl.item(j).getNodeName().equals("val")) res[i] = new IntOrString(nl.item(j).getTextContent());
		        		   else throw new Exception("invalid policy: invalide \""+nl.item(j).getNodeName()+"\" attribute");
		        		   i++;
		        	   }
		           }
		           if(j == 0 && i == 0) throw new Exception("invalid policy: invalide \"sup\" statement");
		           System.out.print("("+res[0]+" > "+res[1]+" / "+res[0].sup(res[1])+")");
		           return res[0].sup(res[1]);
	    	   }else if(node.getNodeName().equals("inf")) {
	    		   NodeList nl=node.getChildNodes();
		           IntOrString [] res = new IntOrString[2];
		           int j,i=0;
		           for(j=0;j<nl.getLength();j++) {
		        	   if(nl.item(j).getNodeType() == Node.ELEMENT_NODE) {
		        		   if (i>1) throw new Exception("invalid policy: invalide \"inf\" statement");
		        		   if(nl.item(j).getNodeName().equals("att")) {
		        			   res[i] = result(nl.item(j), IDUser, IDObject);
		        		   }else if(nl.item(j).getNodeName().equals("val")) res[i] = new IntOrString(nl.item(j).getTextContent());
		        		   else throw new Exception("invalid policy: invalide \""+nl.item(j).getNodeName()+"\" attribute");
		        		   i++;
		        	   }
		           }
		           if(j == 0 && i == 0) throw new Exception("invalid policy: invalide \"inf\" statement");
		           System.out.print("("+res[0]+" < "+res[1]+" / "+res[0].inf(res[1])+")");
		           return res[0].inf(res[1]);
	    	   }
	    	   
	    	   else throw new Exception("invalid policy: invalid element \""+node.getNodeName()+"\"");
	    	   
	           //System.out.println(node.getNodeName()+" : "+node.getTextContent()+" j = "+i+" length = "+node.getChildNodes().getLength());
	       }return null;
	}
	
	public Boolean dacCalculator(String IDUser, String IDObject) throws Exception {
		
		Statement statement = connection.createStatement();
		ResultSet res = statement.executeQuery("SELECT "+IDObject+" FROM dac WHERE utilisateurs = "+IDUser);
		if (res.isClosed()) {
			throw new Exception("User ID or object ID incorrect");
		}
		Boolean boll = res.getBoolean(1);
		statement.close();
		return boll;
	}
	
	public Boolean Permission(String IDUser, String IDObject, String r_w) throws Exception {
		if(!dacCalculator(IDUser, IDObject)) {
			return false;
		}
		Boolean res;
		NodeList nodes = mainDoc.getFirstChild().getChildNodes();
		for(int i=0;i<nodes.getLength();i++) {
			if(nodes.item(i).getNodeName().equals(getObjectType(IDObject))) {
				NodeList nodes1 = nodes.item(i).getChildNodes();
				for(int j=0;j<nodes1.getLength();j++) {
					if(nodes1.item(j).getNodeName().equals(r_w)) {
						NodeList nodes2 = nodes1.item(j).getChildNodes();
						for(int k=0;k<nodes2.getLength();k++) {
							if(nodes2.item(k).getNodeType() == Node.ELEMENT_NODE) {
								res = policyCalculator(nodes2.item(k), IDUser, IDObject);
								if(res != null) return res;
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
		if(m.containsKey("line")) {
			res = statement.executeQuery("SELECT "+m.get("column")+" FROM "+m.get("class")+" WHERE ID = "+m.get("line"));
		}else if(m.get("class").equals("utilisateurs")) {
			res = statement.executeQuery("SELECT "+m.get("column")+" FROM "+m.get("class")+" WHERE ID = "+IDUser);
		}else res = statement.executeQuery("SELECT "+m.get("column")+" FROM "+m.get("class")+" WHERE ID = "+IDObject);
		if(res.getString(1).equals("")) throw new Exception("invalide attribute \""+node.getTextContent()+"\" = \""+res.getString(1)+"\"");
		IntOrString in = new IntOrString(res.getString(1));
		statement.close();
		return in;
	}
	
	//add pri-defined polices to policies.xml				(we don't need the object ID but the class ID)
	public void addPolicy(Map<String, String> m) throws Exception {
		Node newNode = DOMUtils.creatXMLNode(m);
		DOMUtils.trimEmptyTextNodes(newNode);
		NodeList nodes = mainDoc.getFirstChild().getChildNodes();
		for(int i=0;i<nodes.getLength();i++) {
			if(nodes.item(i).getNodeName().equals(getObjectType(m.get("OBJECT")))) {
				NodeList nodes1 = nodes.item(i).getChildNodes();
				for(int j=0;j<nodes1.getLength();j++) {
					if(nodes1.item(j).getNodeName().equals(m.get("R_W"))) {
						NodeList nodes2 = nodes1.item(j).getChildNodes();
						for(int k=0;k<nodes2.getLength();k++) {
							if(nodes2.item(k).getNodeName().equals("and")) {
								if(((Element)newNode).getAttribute("type").equals("deny")) {
									Node n = mainDoc.importNode(newNode, true);
									nodes2.item(k).appendChild(n);
								}else if(((Element)newNode).getAttribute("type").equals("Permit")) {
									NodeList nodes3 = nodes2.item(k).getChildNodes();
									for(int l=0;l<nodes3.getLength();l++) {
										if(nodes3.item(l).getNodeName().equals("or")) {
											Node n = mainDoc.importNode(newNode, true);
											nodes3.item(l).appendChild(n);
										}
									}
								}else throw new Exception("invalid Policy Model type");
								
							}
						}
					}
				}
			}
		}
		Statement statement = connection.createStatement();
		statement.executeUpdate("INSERT INTO main.activerules (map) VALUES (\""+warppMap(m)+"\")");
		DOMUtils.xMLPrinter(mainDoc);
		activeRules.add(m);
		statement.close();
	}
		
	public void CleanExpired() throws Exception {
		SimpleDateFormat dateFormat =new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date date = new Date(),dateMap;
		ArrayList<Map<String, String>> deletItem = new ArrayList<Map<String, String>>();
		Statement stat = connection.createStatement();
		Node node;
		for (Map <String, String> map : activeRules) {
			dateMap = dateFormat.parse(map.get("ETIME"));
			if(dateMap.before(date)) {
				node = DOMUtils.creatXMLNode(map);
				stat.executeUpdate("DELETE FROM activerules WHERE map = \""
						+ warppMap(map) + "\"");
				deletItem.add(map);
				DOMUtils.deleteXML(mainDoc.getFirstChild(), node);
			}
		}
		activeRules.removeAll(deletItem);
		stat.close();
		DOMUtils.xMLPrinter(mainDoc);
	}
	
	public void removePolicy(Map<String, String> m) throws Exception {
		Statement stat = connection.createStatement();
		stat.executeUpdate("DELETE FROM activerules WHERE map = \""
				+ warppMap(m) + "\"");
		DOMUtils.deleteXML(mainDoc.getFirstChild(), DOMUtils.creatXMLNode(m));
		activeRules.remove(m);
		stat.close();
		DOMUtils.xMLPrinter(mainDoc);
	}
	
	public String warppMap(Map <String, String> map) throws Exception {
		String res = "";
		for(Map.Entry<String, String> item : map.entrySet()) {
			res = res + item.getKey() + "|" + item.getValue() + ".";
		}if(res.equals("")) throw new Exception("invalid Rule Map");
		return res.substring(0, res.length()-1);
	}
	
	public Map<String, String> warppStringToMap(String str) throws Exception{
		Map<String, String> res = new HashMap<>();
		String [] list = str.split("\\.");
		for (String items : list) {
			String [] item = items.split("\\|");
			if(item.length != 2) throw new Exception("invalid Rule Map String");
			res.put(item[0], item[1]);
		}
		return res;	
	}
	
	//the function that change a user permission in DAC
	public boolean denyOrAcceptDAC(String IDUser, String IDObject, boolean decision) throws Exception {
		
		try {
			Statement stat = connection.createStatement();
			stat.executeUpdate("UPDATE dac SET \""+IDObject+"\"= "+((decision == true)? "1" : "0" )+" WHERE \"_rowid_\"="+IDUser);
		} catch (SQLException e) {
			throw new Exception("Error user "+IDUser+" or object "+IDObject+" not fund");
		}return false;
	}
}
