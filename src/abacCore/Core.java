package abacCore;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class Core {
	public static String getTable(String att) {
		return att.substring(0, att.indexOf("."));
	}
	
	//Recursively loop through and print out all the xml child tags in the document
		public static Boolean policyCalculator(Node node, int i) throws Exception{
		       if(/*node.hasChildNodes()  ||*/ node.getNodeType() == Node.ELEMENT_NODE){
		    	   if(node.getNodeName().equals("and")) {
			           NodeList nl=node.getChildNodes();
			           for(int j=0;j<nl.getLength();j++) {
			        	   if(policyCalculator(nl.item(j), j))
			        	   System.out.print(" and ");
			           }
			           return true;

		    	   }else if(node.getNodeName().equals("or")) {
			           NodeList nl=node.getChildNodes();
			           for(int j=0;j<nl.getLength();j++) {
			        	   if(policyCalculator(nl.item(j), j))
			        	   System.out.print(" or ");
			           }
			           return true;

		    	   }else if(node.getNodeName().equals("attribute")) {
		    		   System.out.print(" "+node.getTextContent()+" ");
		    		   return true;
		    	   }//else throw new Exception("invalid policy");
		    	   
		           System.out.println(node.getNodeName()+" : "+node.getTextContent()+" j = "+i+" length = "+node.getChildNodes().getLength());
		       }return false;
		   }
		
		
		public static void main(String[] args) {
			
			try {
				Class.forName("org.sqlite.JDBC");
				Connection connection = null;
				connection = DriverManager.getConnection("jdbc:sqlite:HomeSecurityDB.db");
				Statement statement = connection.createStatement();
				
//				statement.executeUpdate("DROP TABLE IF EXISTS person");
//		        statement.executeUpdate("CREATE TABLE person (id INTEGER, name STRING)");
//				
//		        int ids [] = {1,2,3,4,5};
//		         String names [] = {"Peter","Pallar","William","Paul","James Bond"};
	//
//		         for(int i=0;i<ids.length;i++){
//		              statement.executeUpdate("INSERT INTO person values(' "+ids[i]+"', '"+names[i]+"')");   
//		         }

		         //statement.executeUpdate("UPDATE person SET name='Peter' WHERE id='1'");
		         //statement.executeUpdate("DELETE FROM person WHERE id='1'");

//		           ResultSet resultSet = statement.executeQuery("SELECT * from person");
//		           while(resultSet.next())
//		           {
//		              // iterate & read the result set
//		              System.out.println("name = " + resultSet.getString("name"));
//		              System.out.println("id = " + resultSet.getInt("id"));
//		           }
		           connection.close();
		          
		        
			} catch (ClassNotFoundException e1) {
				System.out.println(e1);
				e1.printStackTrace();
			} catch (SQLException e) {
				System.out.println(e);
			}
			
					
			/////////////////////////////////////////////////////////////////////
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			try {
				DocumentBuilder builder = factory.newDocumentBuilder();
				Document doc = builder.parse(new File("Policies.xml"));
				
				System.out.println("*************PROLOGUE************");
				System.out.println("version : " + doc.getXmlVersion());
				System.out.println("encodage : " + doc.getXmlEncoding());		
			    System.out.println("standalone : " + doc.getXmlStandalone());
			    
			    NodeList nodes = doc.getDocumentElement().getChildNodes();
			    
			    System.out.println("\n*************RACINE************");
				System.out.println(doc.getDocumentElement().getNodeName());
				System.out.println(nodes.getLength());
				
				for(int k=0;k<nodes.getLength();k++){
		             policyCalculator(nodes.item(k),0);
		         }
				
			} catch (ParserConfigurationException e) {
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
