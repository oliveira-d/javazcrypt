package com.github.oliveiradd.javazcrypt;

import java.io.FileOutputStream;
import java.io.IOException;
import java.awt.HeadlessException;

// file checking
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
// base 64 
import java.util.Base64;

import java.nio.charset.StandardCharsets;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;

// copy function
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;

// lock file while writting to it
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

class ContentManager {

    static void writeBytesToFile(String outputFile, byte[] outputBytes) throws IOException {
        try (FileOutputStream outputStream = new FileOutputStream(outputFile);
        FileChannel channel = outputStream.getChannel()) {

            FileLock lock = channel.lock();
            // outputStream.write(outputBytes);
            channel.write(java.nio.ByteBuffer.wrap(outputBytes));
            lock.release();
        }
    }
    //methods to manage XML file in memory

    static void printBytes(byte[] decryptedBytes) {
        String decryptedContent = new String(decryptedBytes, StandardCharsets.UTF_8);
        System.out.printf("%s",decryptedContent);
    }

    static byte[] convertXMLDocumentToByteArray(Document xmlDocument) throws Exception {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();

        // Transform the XML document into a byte array
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        transformer.transform(new DOMSource(xmlDocument), new StreamResult(outputStream));

        return outputStream.toByteArray();
    }

    static Document convertByteArrayToXMLDocument(byte[] byteArray) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        ByteArrayInputStream inputStream = new ByteArrayInputStream(byteArray);

        return builder.parse(inputStream);
    }

    static void copyToClipboard(String text) {
        try {
            Toolkit defaultToolkit = Toolkit.getDefaultToolkit();
            Clipboard clipboard = defaultToolkit.getSystemClipboard();
            clipboard.setContents(new StringSelection(text), null);
        } catch (HeadlessException e) {
            Main.message = "Cannot use clipboard. Running on headless system.";
        }
    }

    static String encodeBase64(String file) {
        try {
            byte[] inputFileBytes = Files.readAllBytes(Paths.get(file));
            String base64EncodedFile = Base64.getEncoder().encodeToString(inputFileBytes);
            return base64EncodedFile;
        } catch (Exception e) {
            System.err.println("Could not read file "+file);
        }
        return null;
    }

    static void outputEncodedFile(pxmlElement currentElement,String outputFile) {
        String encodedFile = currentElement.getTextContent();
        byte[] decodedData = Base64.getDecoder().decode(encodedFile);
        try {
            writeBytesToFile(outputFile,decodedData);
        } catch (IOException e) {
            System.err.println("Could not write content to file.");
        }
    }
}
