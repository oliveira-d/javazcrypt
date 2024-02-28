package com.github.oliveiradd.javazcrypt;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;
import org.w3c.dom.NodeList;

class pxmlElement {

    private Element element;
    private static String[] entryFields = {"user","password","URL","TOTP","notes"};
    static int passwordIndex = 1;
    static int TOTPIndex = 3;
    private static String[] cardFields = {"number","name","expiration","cvv"};

    // private static Document passwordDatabase; 

    pxmlElement(Document passwordDatabase) {
        this.element = passwordDatabase.getDocumentElement();
    }

    pxmlElement(Element element) {
        this.element = element;
    }


    // methods to list stuff

    int displayChildElements(boolean showValue, boolean hideSensitiveValues) {
        // System.out.println("Child elements of " + parentElement.getTagName() + ":");
        Element folder = this.element;
        NodeList childNodes = folder.getChildNodes();

        for (int i = 0; i < childNodes.getLength(); i++) {
            Node node = childNodes.item(i);

            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element childElement = (Element) node;
                System.out.printf("(%-"+7+"s (%d) %s",childElement.getTagName()+")",i+1,childElement.getAttribute("name"));
                if (showValue) {
                    switch (childElement.getAttribute("name")) {
                        case "TOTP":
                            if (childElement.getTextContent().length() > 0) {
                                String totp = TOTP.getCode(childElement.getTextContent(),childElement.getAttribute("algorithm"),childElement.getAttribute("totpInterval"),childElement.getAttribute("numberOfDigits"));
                                if (hideSensitiveValues) {
                                    System.out.printf(": ");
                                    for (int j=0; j<totp.length(); j++) {
                                        System.out.printf("*");
                                    }
                                } else {
                                    System.out.printf(": %s",totp);
                                }
                            } else {
                                System.out.printf(":");
                            }
                            break;
                        case "password":
                        case "cvv":
                            if (hideSensitiveValues) {
                                System.out.printf(": ");
                                for (int j=0; j<childElement.getTextContent().length(); j++) {
                                    System.out.printf("*");
                                }
                                break; // intentionally only break if code executes. otherwise execute default case
                            }
                        default:
                            System.out.printf(": %s",childElement.getTextContent());                      
                    }
                }
                System.out.printf("%n");
                // System.out.printf("%s - %d) %s",elementType,i,childElement.getAttribute("name"));
            }
        }
        return childNodes.getLength();
    }
    
    pxmlElement getChildElement(int index) {
        Element folder = this.element;
            // System.out.println("Child elements of " + parentElement.getTagName() + ":");
        NodeList childNodes = folder.getChildNodes();
        Node node = childNodes.item(index);
        Element childElement = null;
        if (node.getNodeType() == Node.ELEMENT_NODE) childElement = (Element) node;
        return new pxmlElement(childElement);
    }

    pxmlElement createChild(Document passwordDatabase, String childType, String childName) {
        Element folder = this.element;
        String[] childFields = null;
        switch (childType) {
            case "entry":
                childFields = entryFields;
                break;
            case "card":
                childFields = cardFields;
                break;
        }
        Element child = passwordDatabase.createElement(childType);
        child.setAttribute("name",childName);
        folder.appendChild(child);
        if (childFields != null) {
            for (int i=0; i<childFields.length; i++) {
                Element newField = passwordDatabase.createElement("field");
                newField.setAttribute("name",childFields[i]);
                child.appendChild(newField);
            }
        }
        return new pxmlElement(child);
    }

    void deleteItem(int index) {
        Element folder = this.element;
        if (!folder.getTagName().equals("dir")){
            System.err.println("Failed to create item inside"+folder.getAttribute("name")+". "+folder.getAttribute("name")+" is not a folder.");
        }
        NodeList childNodes = folder.getChildNodes();
        Node childNode = childNodes.item(index);
        Element childElement = null;
        if (childNode.getNodeType() == Node.ELEMENT_NODE) childElement = (Element) childNode;
        folder.removeChild(childElement);
    }

    void inputText(Document passwordDataBase, String newText) {
        Element field = this.element;
        if (!field.getTagName().equals("field") && !field.getTagName().equals("file")) {
            System.err.println("Failed to write text to item. "+field.getAttribute("name")+" is not an input field or a file.");
            return;
        }
        field.appendChild(passwordDataBase.createTextNode(newText));
    }

    void deleteTextContent() {
        Element field = this.element;
        // Get the list of child nodes
        Node child = field.getFirstChild();

        // Iterate through child nodes and remove text nodes
        while (child != null) {
            if (child.getNodeType() == Node.TEXT_NODE) {
                field.removeChild(child);
            }
            child = child.getNextSibling();
        }
    }

    void setAttribute(String key,String value) {
        this.element.setAttribute(key,value);
    }

    Node getParentNode() {
        return this.element.getParentNode();
    }

    void appendChild(Text textNode) {
        this.element.appendChild(textNode);
    }

    void appendChild(pxmlElement childElement) {
        this.element.appendChild(childElement.element);
    }

    String getTextContent() {
        return this.element.getTextContent();
    }

    String getTagName() {
        return this.element.getTagName();
    }

    String getAttribute(String key) {
        return this.element.getAttribute(key);
    }

    Element getElement() {
        return this.element;
    }

    
}
