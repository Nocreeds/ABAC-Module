package abacCore;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class Test {
	
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
