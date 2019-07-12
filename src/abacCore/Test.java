package abacCore;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.sun.javafx.image.impl.ByteIndexed.Getter;

public class Test {
	
	public static String getTable(String att) {
		return att.substring(0, att.indexOf("."));
	}
	
	//Recursively loop through and print out all the xml child tags in the document
	public static Boolean policyCalculator(Node node, Connection BD) throws Exception{
	       if(node.getNodeType() == Node.ELEMENT_NODE){
	    	   if(node.getNodeName().equals("and")) {
		           NodeList nl=node.getChildNodes();
		           Boolean fRes = true;
		           int j;
		           for(j=0;j<nl.getLength();j++) {
		        	   Boolean res = policyCalculator(nl.item(j), BD);
		        	   if(res != null) {
		        		   fRes = fRes && res;
		        		   System.out.print(" and ");
		        	   }
		        	   
		           }
		           if(j == 0) throw new Exception("invalid policy");
		           else return fRes;

	    	   }else if(node.getNodeName().equals("or")) {
		           NodeList nl=node.getChildNodes();
		           Boolean fRes = false;
		           int j;
		           for(j=0;j<nl.getLength();j++) {
		        	   Boolean res = policyCalculator(nl.item(j), BD);
		        	   if(res != null) {
		        		   System.out.print(" or ");
		        		   fRes = fRes || res;
		        		   
		        	   }
		           }
		           if(j == 0) throw new Exception("invalid policy");
		           else return fRes;

	    	   }else if(node.getNodeName().equals("att")) {
	    		   String att = node.getTextContent();
	    		   String classe = getTable(att);
	    		   Statement statement = BD.createStatement();
	    		   ResultSet res = statement.executeQuery("SELECT "+att+" FROM "+classe);
	    		   System.out.print(" "+res.getBoolean(1)+" ");
	    		   return res.getBoolean(1);
	    	   }else if(node.getNodeName().equals("equal")) {
	    		   
	    	   }else if(node.getNodeName().equals("supequal")) {
	    		   
	    	   }else if(node.getNodeName().equals("infequal")) {
	    		   
	    	   }else if(node.getNodeName().equals("sup")) {
	    		   
	    	   }else if(node.getNodeName().equals("inf")) {
	    		   
	    	   }
	    	   
	    	   else throw new Exception("invalid policy");
	    	   
	           //System.out.println(node.getNodeName()+" : "+node.getTextContent()+" j = "+i+" length = "+node.getChildNodes().getLength());
	       }return null;
	       
	}
	
	
	
	public static void main(String[] args) {
		
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		try {
			Class.forName("org.sqlite.JDBC");
			Connection connection = null;
			connection = DriverManager.getConnection("jdbc:sqlite:HomeSecurityDB.db");
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc = builder.parse(new File("Policies.xml"));
		    NodeList nodes = doc.getDocumentElement().getChildNodes();

		    System.out.println("\n*************RACINE************");
			System.out.println(doc.getDocumentElement().getNodeName());
			System.out.println(nodes.getLength());
			
			for(int k=0;k<nodes.getLength();k++){
	             System.out.println("==>" + policyCalculator(nodes.item(k),connection));
	        }
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
