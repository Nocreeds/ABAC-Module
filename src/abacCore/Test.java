package abacCore;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class Test {
	
	public static String getObjectType(String IDObject) throws Exception {
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
	public static Boolean policyCalculator(Node node, Connection BD, String IDUser, String IDObject) throws Exception{
	       if(node.getNodeType() == Node.ELEMENT_NODE){
	    	   if(node.getNodeName().equals("and")) {
		           NodeList nl=node.getChildNodes();
		           Boolean fRes = true;
		           int j;
		           for(j=0;j<nl.getLength();j++) {
		        	   Boolean res = policyCalculator(nl.item(j), BD, IDUser, IDObject);
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
		        	   Boolean res = policyCalculator(nl.item(j), BD, IDUser, IDObject);
		        	   if(res != null) {
		        		   System.out.print(" or ");
		        		   fRes = fRes || res;
		        		   
		        	   }
		           }
		           if(j == 0) throw new Exception("invalid policy: invalide \"or\" statement");
		           else return fRes;

	    	   }else if(node.getNodeName().equals("att")) {
	    		   Map<String, String> m = getTable(node.getTextContent());
	    		   Statement statement = BD.createStatement();
	    		   ResultSet res;
	    		   if(m.containsKey("line")) {
	    				res = statement.executeQuery("SELECT "+m.get("column")+" FROM "+m.get("class")+" WHERE ID = "+m.get("line"));
	    			}else if(m.get("class").equals("utilisateurs")) {
	    				res = statement.executeQuery("SELECT "+m.get("column")+" FROM "+m.get("class")+" WHERE ID = "+IDUser);
	    			}else res = statement.executeQuery("SELECT "+m.get("column")+" FROM "+m.get("class")+" WHERE ID = "+IDObject);
	    		   System.out.print(" "+res.getBoolean(1)+" ");
	    		   return res.getBoolean(1);
	    	   }else if(node.getNodeName().equals("equal")) {
	    		   NodeList nl=node.getChildNodes();
		           IntOrString [] res = new IntOrString[2];
		           int j,i=0;
		           for(j=0;j<nl.getLength();j++) {
		        	   if(nl.item(j).getNodeType() == Node.ELEMENT_NODE) {
		        		   System.out.println(nl.item(j).getTextContent());
		        		   if (i>1) throw new Exception("invalid policy: invalide \"equal\" statement");
		        		   if(nl.item(j).getNodeName().equals("att")) {
		        			   res[i] = result(nl.item(j), BD, IDUser, IDObject);
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
		        			   res[i] = result(nl.item(j), BD, IDUser, IDObject);
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
		        			   res[i] = result(nl.item(j), BD, IDUser, IDObject);
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
		        			   res[i] = result(nl.item(j), BD, IDUser, IDObject);
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
		        			   res[i] = result(nl.item(j), BD, IDUser, IDObject);
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
	
	public static Boolean dacCalculator(Connection BD, String IDUser, String IDObject) throws SQLException {
		Statement statement = BD.createStatement();
		ResultSet res = statement.executeQuery("SELECT "+IDObject+" FROM dac WHERE utilisateurs = "+IDUser);
		Boolean boll = res.getBoolean(1);
		statement.close();
		return boll;
	}
	
	public static Boolean Permission(Node node, Connection BD, String IDUser, String IDObject, String r_w) throws Exception {
		if(!dacCalculator(BD, IDUser, IDObject)) {
			return false;
		}
		Boolean res;
		NodeList nodes = node.getChildNodes();
		for(int i=0;i<nodes.getLength();i++) {
			if(nodes.item(i).getNodeName().equals(getObjectType(IDObject))) {
				NodeList nodes1 = nodes.item(i).getChildNodes();
				for(int j=0;j<nodes1.getLength();j++) {
					if(nodes1.item(j).getNodeName().equals(r_w)) {
						res = policyCalculator(nodes.item(i), BD, IDUser, IDObject);
						if(res != null) return res;
						return true;
					}
				}
			}
		}
		return true;
	}
	
	private static IntOrString result(Node node, Connection BD, String IDUser, String IDObject) throws Exception {
		Map<String, String> m = getTable(node.getTextContent());
		Statement statement = BD.createStatement();
		ResultSet res;
		if(m.containsKey("line")) {
			res = statement.executeQuery("SELECT "+m.get("column")+" FROM "+m.get("class")+" WHERE ID = "+m.get("line"));
		}else if(m.get("class").equals("utilisateurs")) {
			res = statement.executeQuery("SELECT "+m.get("column")+" FROM "+m.get("class")+" WHERE ID = "+IDUser);
		}else res = statement.executeQuery("SELECT "+m.get("column")+" FROM "+m.get("class")+" WHERE ID = "+IDObject);
		if(res.getString(1).equals("")) throw new Exception("invalide attribute \""+node.getTextContent()+"\" = \""+res.getString(1)+"\"");
		return new IntOrString(res.getString(1));
	}

	public static void main(String[] args) {
		
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		try {
			/*------------------------------BD Connection-----------------------------*/
			Class.forName("org.sqlite.JDBC");
			Connection connection = null;
			connection = DriverManager.getConnection("jdbc:sqlite:HomeSecurityDB.db");
			/*------------------------------XML Extraction-----------------------------*/
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc = builder.parse(new File("Policies.xml"));
		    NodeList nodes = doc.getDocumentElement().getChildNodes();

		    System.out.println("\n*************RACINE************");
			System.out.println(doc.getDocumentElement().getNodeName());
			System.out.println(nodes.getLength());
			
	        System.out.println("==>" + Permission(doc.getDocumentElement(), connection, "231646", "0", "read"));
	        connection.close();
	          
	        
		} catch (ClassNotFoundException e1) {
			System.out.println(e1);
			e1.printStackTrace();
		} catch (SQLException e) {
			System.out.println(e);
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}