package abacCore;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

public class DOMUtils {

	public static void trimEmptyTextNodes(Node node) {
		Element element = null;
		if (node instanceof Document) {
			element = ((Document) node).getDocumentElement();
		} else if (node instanceof Element) {
			element = (Element) node;
		} else {
			return;
		}

		List<Node> nodesToRemove = new ArrayList<Node>();
		NodeList children = element.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child instanceof Element) {
				trimEmptyTextNodes(child);
			} else if (child instanceof Text) {
				Text t = (Text) child;
				if (t.getData().trim().length() == 0) {
					nodesToRemove.add(child);
				}
			}
		}

		for (Node n : nodesToRemove) {
			element.removeChild(n);
		}
	}

	public static void compareNodes(Node expected, Node actual, boolean trimEmptyTextNodes) throws Exception {
		if (trimEmptyTextNodes) {
			trimEmptyTextNodes(expected);
			trimEmptyTextNodes(actual);
		}
		compareNodes(expected, actual);
	}

	public static void compareNodes(Node expected, Node actual) throws Exception {
		if (expected.getNodeType() != actual.getNodeType()) {
			throw new Exception("Different types of nodes: " + expected + " " + actual);
		}
		if (expected instanceof Document) {
			Document expectedDoc = (Document) expected;
			Document actualDoc = (Document) actual;
			compareNodes(expectedDoc.getDocumentElement(), actualDoc.getDocumentElement());
		} else if (expected instanceof Element) {
			Element expectedElement = (Element) expected;
			Element actualElement = (Element) actual;

			// compare element names
			if (!expectedElement.getLocalName().equals(actualElement.getLocalName())) {
				throw new Exception("Element names do not match: " + expectedElement.getLocalName() + " "
						+ actualElement.getLocalName());
			}
			// compare element ns
			String expectedNS = expectedElement.getNamespaceURI();
			String actualNS = actualElement.getNamespaceURI();
			if ((expectedNS == null && actualNS != null) || (expectedNS != null && !expectedNS.equals(actualNS))) {
				throw new Exception("Element namespaces names do not match: " + expectedNS + " " + actualNS);
			}

			String elementName = "{" + expectedElement.getNamespaceURI() + "}" + actualElement.getLocalName();

			// compare attributes
			NamedNodeMap expectedAttrs = expectedElement.getAttributes();
			NamedNodeMap actualAttrs = actualElement.getAttributes();
			if (countNonNamespaceAttribures(expectedAttrs) != countNonNamespaceAttribures(actualAttrs)) {
				throw new Exception(elementName + ": Number of attributes do not match up: "
						+ countNonNamespaceAttribures(expectedAttrs) + " " + countNonNamespaceAttribures(actualAttrs));
			}
			for (int i = 0; i < expectedAttrs.getLength(); i++) {
				Attr expectedAttr = (Attr) expectedAttrs.item(i);
				if (expectedAttr.getName().startsWith("xmlns")) {
					continue;
				}
				Attr actualAttr = null;
				if (expectedAttr.getNamespaceURI() == null) {
					actualAttr = (Attr) actualAttrs.getNamedItem(expectedAttr.getName());
				} else {
					actualAttr = (Attr) actualAttrs.getNamedItemNS(expectedAttr.getNamespaceURI(),
							expectedAttr.getLocalName());
				}
				if (actualAttr == null) {
					throw new Exception(elementName + ": No attribute found:" + expectedAttr);
				}
				if (!expectedAttr.getValue().equals(actualAttr.getValue())) {
					throw new Exception(elementName + ": Attribute values do not match: " + expectedAttr.getValue()
							+ " " + actualAttr.getValue());
				}
			}

			// compare children
			NodeList expectedChildren = expectedElement.getChildNodes();
			NodeList actualChildren = actualElement.getChildNodes();
			if (expectedChildren.getLength() != actualChildren.getLength()) {
				throw new Exception(elementName + ": Number of children do not match up: "
						+ expectedChildren.getLength() + " " + actualChildren.getLength());
			}
			for (int i = 0; i < expectedChildren.getLength(); i++) {
				Node expectedChild = expectedChildren.item(i);
				Node actualChild = actualChildren.item(i);
				compareNodes(expectedChild, actualChild);
			}
		} else if (expected instanceof Text) {
			String expectedData = ((Text) expected).getData().trim();
			String actualData = ((Text) actual).getData().trim();

			if (!expectedData.equals(actualData)) {
				throw new Exception("Text does not match: " + expectedData + " " + actualData);
			}
		}
	}

	private static int countNonNamespaceAttribures(NamedNodeMap attrs) {
		int n = 0;
		for (int i = 0; i < attrs.getLength(); i++) {
			Attr attr = (Attr) attrs.item(i);
			if (!attr.getName().startsWith("xmlns")) {
				n++;
			}
		}
		return n;
	}

	public static final void xMLPrinter(Document xml) throws Exception {
		Transformer tf = TransformerFactory.newInstance().newTransformer();
		tf.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
		tf.setOutputProperty(OutputKeys.INDENT, "yes");
		tf.transform(new DOMSource(xml), new StreamResult(new File("/home/pi/rasp/Resource/Policies.xml")));
		// refresh loaded policy.xml
	}

	public static void changeVal(Node node, Map<String, String> input) {
		NodeList nodes = node.getChildNodes();
		for (int i = 0; i < nodes.getLength(); i++) {
			if (input.containsKey(nodes.item(i).getTextContent())) {
				nodes.item(i).setTextContent(input.get(nodes.item(i).getTextContent()));
			} else
				changeVal(nodes.item(i), input);
		}
	}

	// Turn Input from user into XML Nodes
	public static Node creatXMLNode(Map<String, String> input) throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document doc = builder.parse(new File("/home/pi/rasp/Resource/Models.xml"));
		trimEmptyTextNodes(doc);
		NodeList nodes = doc.getElementsByTagName(input.get("MODEL"));
		if (nodes.getLength() > 1)
			throw new Exception("redundancy detected in Model: " + input.get("MODEL"));
		else if (nodes.getLength() == 0)
			throw new Exception("no Model found: " + input.get("MODEL"));
		Node res = nodes.item(0);
		changeVal(res, input);
		for (int i = 0; i < res.getChildNodes().getLength(); i++) {
			if (res.getChildNodes().item(i).getNodeType() == Node.ELEMENT_NODE) {
				return res.getChildNodes().item(i);
			}
		}
		throw new Exception("Node creation failed");
	}

	public static void deleteXML(Node mainNode, Node searchNode) {
		NodeList nodes = ((Element) mainNode).getElementsByTagName("and");
		for (int i = 0; i < nodes.getLength(); i++) {
			if (searchNode.isEqualNode(nodes.item(i))) {
				Node parentNode = nodes.item(i).getParentNode();
				parentNode.removeChild(nodes.item(i));
			}
		}
	}

}
