package com.github.oliveiradd.javazcrypt;

import java.io.Console;
import java.util.Scanner;

// keep track of path inside "xml filesystem"
import java.util.LinkedList;
import java.util.Deque;

// file checking
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

// encoding to store files in xml
import java.util.Base64;

// xml document
import java.io.IOException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

// display menu
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

// autocompletion
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.builtins.Completers.FileNameCompleter;

// generate passwd
import java.security.SecureRandom;

// clear clipboard
import java.util.Timer;
import java.util.TimerTask;

class Main {

    private static LinkedList<String> pathL = new LinkedList<>();
    private static Deque<String> pathQ = pathL;
    private static String password = null;
    private static String keyFile = null;
    private static String inputFile = null;
    private static String outputFile = null;
    private static Terminal terminal = getTerminal();
    private static pxmlElement clipboardElement = null; // leave it to the class so that is doesn't lose itself when switching between mainMenu() and secondaryMenu()
    private static boolean saved = true;
    private static Scanner scanner = new Scanner(System.in);
    private static Timer timer = null; // do not initialize timer here. For operations other than manipulating the database the timer won't be canceled and program will hang instead of quitting
    private static int timeInterval = 10000; // default time interval to clear clipboard - may be changed in the open database function
    static String message = null;
    private static boolean exitProgram = false;

    public static void main(String[] args) {

        String operation = "open";

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                // file encryption utility arguments
                case "-e":
                case "--encrypt":
                    operation = "encrypt";
                    break;
                case "-d":
                case "--decrypt":
                    operation = "decrypt";
                    break;
                case "-o":
                case "--output":
                    if (i < args.length - 1) {
                        outputFile = args[++i];
                    } else {
                        System.err.println("Missing output file.");
                    }
                    break;
                // general arguments
                case "-k":
                    if (i < args.length - 1) {
                        keyFile = args[++i]; // ++i - Move to the next argument - keyfile path won't serve in switch case anyway
                    } else {
                        System.out.println("Missing key file.");
                        System.exit(1);
                    }
                    break;
                case "-h":
                case "--help":
                    help();
                    return;
                default:
                    inputFile = args[i];
            }
        }

        if (inputFile == null) {
            if (operation.equals("open")) {
                LineReader lineReader = LineReaderBuilder.builder().terminal(terminal).completer(new FileNameCompleter()).build();
                inputFile = lineReader.readLine("No database specified. Enter the path for an existing database or create a new one: ");
            } else { // operation is encrypt or decrypt
                System.err.println("No input file specified.");
                System.exit(1);
            }
        }

        if (!Files.exists(Paths.get(inputFile))) {
            if (operation.equals("open")) {
                operation="create";
                System.out.println("Database not found. Creating new.");
            } else {
                System.err.printf("Cannot open '%s': file does not exist.%n",inputFile);
                System.exit(1);
            }
        } else if (!Files.isReadable(Paths.get(inputFile))) {
            System.err.printf("Cannot open '%s': no read permission.%n",inputFile);
            System.exit(1);
        } else if (!Files.isRegularFile(Paths.get(inputFile))) {
            System.err.printf("Cannot open '%s': not a regular file.%n",inputFile);
            System.exit(1);
        }

        if (operation.equals("encrypt") || operation.equals("decrypt")) {
            if (outputFile == null) {
                outputFile = inputFile;
            } else if (!Files.exists(Paths.get(outputFile))) {
                System.err.printf("'%s' already exists. Will not overwrite.%n",outputFile);
                System.exit(1);
            }
        }

        if (keyFile != null) {
            if (!Files.exists(Paths.get(keyFile))) {
                System.err.printf("Cannot open '%s': file does not exist.%n",keyFile);
                System.exit(1);
            } else if (!Files.isReadable(Paths.get(keyFile))) {
                System.err.printf("Cannot open '%s': no read permission.%n",keyFile);
                System.exit(1);
            } else if (!Files.isRegularFile(Paths.get(keyFile))) {
                System.err.printf("Cannot open '%s': not a regular file.%n",keyFile);
                System.exit(1);
            }
        }

        // get password
        Console console = System.console();
        char[] passwordChars = null;
        if (operation.equals("open") || operation.equals("decrypt")) {
            passwordChars = console.readPassword("Enter your password: ");
            password = new String(passwordChars);
        } else { // if (operation.equals("encrypt") || operation.equals("create"))
            String password2 = null;
            do {
                passwordChars = console.readPassword("Enter a password to encrypt the file: ");
                password = new String(passwordChars);
                passwordChars = console.readPassword("Confirm your password: ");
                password2 = new String(passwordChars);
                if (!password2.equals(password)) System.out.println("Passwords do not match. Try again.");
            } while (!password2.equals(password));
        }
        
        Document passwordDatabase = null;
        switch (operation) {
            case "open":
                passwordDatabase = openDatabase();
                break;
            case "create":
                passwordDatabase = createDatabase();
                break;
            case "encrypt":
                try {
                    byte[] encryptedBytes = CryptOps.encryptFile(inputFile, password, keyFile);
                    ContentManager.writeBytesToFile(outputFile,encryptedBytes);
                } catch (Exception e) {
                    System.err.println("Could not encrypt file.");
                }
                return;
            case "decrypt":
                try {
                    byte[] decryptedBytes = CryptOps.decryptFile(inputFile, password, keyFile);
                    ContentManager.writeBytesToFile(outputFile,decryptedBytes);
                } catch (Exception e) {
                    System.err.println("Could not decrypt file.");
                }
                return;
        }

        if (passwordDatabase == null) System.exit(1);

        pxmlElement currentElement = new pxmlElement(passwordDatabase);

        clearScreen();
        timer = new Timer(); // timer is only initialized here to avoid the program hanging instead of finishing execution after operations executed previously in the code 
        do {
            currentElement = mainMenu(passwordDatabase,currentElement);
            if (currentElement != null) {
                currentElement = secondaryMenu(passwordDatabase,currentElement);
            }
        } while (currentElement != null);
        timer.cancel();
        ContentManager.copyToClipboard(null); // clear clipboard in case the timer hasn't gotten to do so

        if(!saved) {
            System.out.printf("Database has been modified. Would you like to save the changes? (y/n): ");
            String answer = scanner.nextLine();
            answer = answer.toLowerCase();
            if (answer.equals("y") || answer.equals("yes")) saveFile(passwordDatabase);
        }
        scanner.close();
    }

    private static Document createDatabase() {
        try {
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document passwordDatabase = documentBuilder.newDocument();
            Element rootElement = passwordDatabase.createElement("dir");
            rootElement.setAttribute("name","root");
            rootElement.setAttribute("timeInterval","10"); //set time interval in seconds
            passwordDatabase.appendChild(rootElement);
            byte[] decryptedBytes = ContentManager.convertXMLDocumentToByteArray(passwordDatabase);
            byte[] encryptedBytes = CryptOps.encryptBytes(decryptedBytes,password,keyFile);
            ContentManager.writeBytesToFile(inputFile,encryptedBytes);
            return passwordDatabase;
        } catch(Exception e) {
            System.err.println("Error creating database.");
            e.printStackTrace();
            return null;
        }
    }

    private static Document openDatabase() {
        try {
            byte[] decryptedBytes = CryptOps.decryptFile(inputFile, password,keyFile);
            Document passwordDatabase = ContentManager.convertByteArrayToXMLDocument(decryptedBytes);
            System.out.println("Database opened successfuly.");
            try {
                timeInterval = Integer.parseInt(passwordDatabase.getDocumentElement().getAttribute("timeInterval"))*1000; // convert to milisseconds
            } catch (NumberFormatException e) {
                message = "Could not get clipboard time interval from settings. Using default.";
            }
            // ContentManager.printBytes(decryptedBytes);
            return passwordDatabase;
        } catch (Exception e) {
            System.out.println("Error opening database");
            return null;
        }
    }

    private static pxmlElement mainMenu(Document passwordDatabase,pxmlElement currentElement) {
        String input = null;
        int items;
        int index;
        pxmlElement fileElement = null;
        LineReader lineReader = null;
        do {
            if (message != null) {
                System.out.println(message);
                message = null;
            }
            // display clipboard
            if (clipboardElement != null) {
                fillWidth("=");
                System.out.println("On clipboard: "+clipboardElement.getAttribute("name")+" - enter mv to move it here.");
            }
            // display path
            fillWidth("=");
            System.out.printf("Path: /");
            for (int i=0; i<pathQ.size(); i++){
                System.out.printf("%s/",pathL.get(pathQ.size()-1-i));
            }
            System.out.println();
            fillWidth("=");
            System.out.println();
            //display main menu
            items = currentElement.displayChildElements(false,false);
            System.out.println();
            fillWidth("=");
            String[] options0 = {" d - add directory ","   e - add entry  "," c - add credit card "};
            String[] options1 = {"  r - rename item  ","  mv - move item  ","     del - delete    "};
            String[] options2 = {"  if - import file "," ef - export file ","     w - write out   "};
            String[] options3 = {"  .. or 0 - cd out ","   s - settings   ","       q - quit      "};
            displayMenu(options0);
            displayMenu(options1);
            displayMenu(options2);
            displayMenu(options3);
            fillWidth("=");
            System.out.printf("Enter menu option: ");
            // get input and make decisions
            input = scanner.nextLine();
            index = items+1; // intentionally set index > items so that the last line in this do-while just does not execute in case there's an exception when converting string to int
            switch (input) {
                case "d":
                    System.out.printf("Enter directory name: ");
                    String folderName = scanner.nextLine();
                    currentElement.createChild(passwordDatabase,"dir",folderName);
                    saved = false;
                    break;
                case "e":
                    System.out.printf("Enter entry name: ");
                    String entryName = scanner.nextLine();
                    currentElement.createChild(passwordDatabase,"entry",entryName);
                    saved = false;
                    break;
                case "c":
                    System.out.printf("Enter credit card name: ");
                    String cardName = scanner.nextLine();
                    currentElement.createChild(passwordDatabase,"card",cardName);
                    saved = false;
                    break;
                case "del":
                    System.out.printf("Enter index to delete: ");
                    input = scanner.nextLine();
                    try {
                        index = Integer.parseInt(input);
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                    if (index <= items && index >= 1) {
                        currentElement.deleteItem(index-1);
                        saved = false;
                    }
                    break;
                case "q":
                    exitProgram = true;
                    break;
                case "w":
                    saveFile(passwordDatabase);
                    break;
                case "0":
                case "..":
                    if (!currentElement.getTagName().equals("dir") || !currentElement.getAttribute("name").equals("root")) {
                        Node parentNode = currentElement.getParentNode();
                        if (parentNode != null && parentNode.getNodeType() == Node.ELEMENT_NODE) {
                            Element tempElement = (Element) parentNode;
                            currentElement = new pxmlElement(tempElement);
                            pathQ.pop();
                        }
                    }
                    break;
                case "r":
                    System.out.printf("Enter index to rename: ");
                    input = scanner.nextLine();
                    try {
                        index = Integer.parseInt(input);
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                    if (index <= items && index >= 1) {
                        System.out.printf("Enter new name: ");
                        String name = scanner.nextLine();
                        currentElement.getChildElement(index-1).setAttribute("name",name);
                        saved = false;
                    }
                    break;
                case "s":
                    clearScreen();
                    settingsMenu(passwordDatabase);
                    saved = false;
                    break;
                case "mv":
                    if (clipboardElement == null) {
                        System.out.printf("Enter index to move: ");
                        input = scanner.nextLine();
                        try {
                            index = Integer.parseInt(input);
                        } catch (NumberFormatException e) {
                            e.printStackTrace();
                        }
                        if (index <= items && index >= 1) clipboardElement = currentElement.getChildElement(index-1);
                    } else {
                        currentElement.appendChild(clipboardElement);
                        clipboardElement = null;
                        saved = false;
                    }
                    break;
                case "if":
                    lineReader = LineReaderBuilder.builder().terminal(terminal).completer(new FileNameCompleter()).build();
                    String importedFile = lineReader.readLine("Enter the path for the file you wish to import to this database: ");
                    while (importedFile.endsWith(" ")) {
                        // Remove the space using substring to avoid exception - this space may occur when completing with tab
                        importedFile = importedFile.substring(0, importedFile.length() - 1);
                    }
                    if (!Files.exists(Paths.get(importedFile))) {
                        message = "Cannot import '"+importedFile+"': file does not exist.";
                    } else if (!Files.isReadable(Paths.get(importedFile))) {
                        message = "Cannot import '"+importedFile+"': no read permission.";
                    } else if (!Files.isRegularFile(Paths.get(importedFile))) {
                        message = "Cannot import '"+importedFile+"': not a regular file.";
                    } else {
                        byte[] fileBytes = null; // compiler complains if i don't initialize it
                        try {
                            fileBytes = Files.readAllBytes(Paths.get(importedFile));
                        } catch (IOException e) {
                            message = "Could not open file "+importedFile;
                            break;
                        }
                        System.out.printf("Enter new name for the file: ");
                        String newFileName = scanner.nextLine();
                        String base64EncodedFile = Base64.getEncoder().encodeToString(fileBytes);
                        fileElement = currentElement.createChild(passwordDatabase,"file",newFileName);
                        fileElement.inputText(passwordDatabase,base64EncodedFile);
                        saved = false;
                    }
                    break;
                case "ef":
                    System.out.printf("Enter index of the file you want to output: ");
                    input = scanner.nextLine();
                    try {
                        index = Integer.parseInt(input);
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                    if (index <= items && index >= 1) {
                        fileElement = currentElement.getChildElement(index-1);
                        if (!fileElement.getTagName().equals("file")) {
                            message = "Cannot complete operation: "+fileElement.getAttribute("name")+" is not a file.";
                            break;
                        }
                        String encodedBytes = fileElement.getTextContent();
                        byte[] decodedBytes = Base64.getDecoder().decode(encodedBytes);
                        lineReader = LineReaderBuilder.builder().terminal(terminal).completer(new FileNameCompleter()).build();
                        String outputDecodedFile = lineReader.readLine("Enter file to output data: ");
                        while (outputDecodedFile.endsWith(" ")) {
                            // Remove the space using substring to avoid exception
                            outputDecodedFile = outputDecodedFile.substring(0, outputDecodedFile.length() - 1);
                        }
                        try{
                            ContentManager.writeBytesToFile(outputDecodedFile,decodedBytes);
                        } catch (IOException e) {
                            message = "Could not output decoded file.";
                        }
                    }
                    break;
                default:
                    try {
                        index = Integer.parseInt(input);
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                    if (index <= items && index >= 1) {
                        if (currentElement.getChildElement(index-1).getTagName().equals("file")) break;
                        currentElement = currentElement.getChildElement(index-1);
                        pathQ.push(currentElement.getAttribute("name"));
                        if (currentElement.getTagName().equals("entry") || currentElement.getTagName().equals("card")){
                            clearScreen();
                            return currentElement;
                        }
                    }
            }
            clearScreen();
        } while (!exitProgram);
        return null;
    }

    private static pxmlElement secondaryMenu(Document passwordDatabase,pxmlElement currentElement) {
        String input = null;
        int items;
        int index;
        String mode = "copy";
        boolean hideSensitiveValues = true;
        do {
            if (message != null) {
                System.out.println(message);
                message = null;
                fillWidth("=");
            }
            // display
            switch (mode) {
                case "copy":
                    System.out.printf("selected mode: (c) copy%nenter (e) for edit mode%n");
                    break;
                case "edit":
                    System.out.printf("selected mode: (e) edit%nenter (c) for copy mode%n");
                    break;
            }
            fillWidth("=");
            System.out.printf("Path: /");
            for (int i=0; i<pathQ.size(); i++) {
                System.out.printf("%s/",pathL.get(pathQ.size()-1-i));
            }
            System.out.println();
            fillWidth("=");
            System.out.println();
            items = currentElement.displayChildElements(true,hideSensitiveValues);
            System.out.println();
            fillWidth("=");
            String[] options0 = {" number - select field "," g - generate password "," w - write to file "};
            String[] options1 = {" 0 or .. - close entry "," v - toggle visibility ","     q - quit      "};
            if (currentElement.getTagName().equals("card")) {
                options0[1] = "";
                options1[1] = ""; // case of card = no password
            }
            displayMenu(options0);
            displayMenu(options1);
            fillWidth("=");
            System.out.printf("Enter menu option: ");
            // switch-case
            input = scanner.nextLine();
            index = items+1; // intentionally set index > items + 1 so that the last line in this do-while just does not execute in case there's an exception when converting string to int
            switch (input) {
                case "c":
                    mode = "copy";
                    break;
                case "e":
                    mode = "edit";
                    break;
                case "w":
                    saveFile(passwordDatabase);
                    break;
                case "..":
                case "0":
                    Node parentNode = currentElement.getParentNode();
                    if (parentNode != null && parentNode.getNodeType() == Node.ELEMENT_NODE) {
                        Element tempElement = (Element) parentNode;
                        currentElement = new pxmlElement(tempElement);
                        pathQ.pop();
                        clearScreen(); // needed here while not in the mainMenu because of return statement below
                        return currentElement;
                    }
                    break;
                case "g":
                    if (currentElement.getTagName().equals("card")) break;
                    String[] allPasswordElements = {"qwertyuiopasdfghjklçzxcvbnm","QWERTYUIOPASDFGHJKLÇZXCVBNM","0123456789","!@#$%¨&*()_-+=`´[]{}~^;:.><","¬¹²³£¢§°®ŧ←↓→øþ´ªæßðđŋħˀĸł´ºˇ«»©„“”µ•·̣"}; // NO ALPHABETICAL ORDER, DEAL WITH IT      
                    for (int i=0; i<allPasswordElements.length; i++) {
                        System.out.println((i+1)+") "+allPasswordElements[i]);
                    }
                    System.out.printf("Enter the indexes of elements you want in your password: ");
                    input = scanner.nextLine();
                    String chosenPasswordsElements = "";
                    for (int i=0; i<allPasswordElements.length; i++) {
                        if(input.contains(String.valueOf(i+1))) {
                            chosenPasswordsElements += allPasswordElements[i];
                        }
                    }
                    System.out.printf("Enter desired password length: ");
                    input = scanner.nextLine();
                    try {
                        int length = Integer.parseInt(input);
                        String entryPassword = generatePassword(length,chosenPasswordsElements);
                        Text textNode = passwordDatabase.createTextNode(entryPassword);
                        currentElement.getChildElement(ContentManager.passwordIndex).deleteTextContent(); // passwordEntry = 1
                        currentElement.getChildElement(ContentManager.passwordIndex).appendChild(textNode);
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                    break;
                case "q":
                    exitProgram = true;
                case "v":
                    hideSensitiveValues = !hideSensitiveValues;
                default:
                    try {
                        index = Integer.parseInt(input);
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                    if (index <= items && index >= 1) {
                        if (mode.equals("edit")) {
                            LineReader lineReader = LineReaderBuilder.builder().terminal(terminal).build();
                            pxmlElement field = currentElement.getChildElement(index-1);
                            String text = null;
                            if (field.getAttribute("name").equals("TOTP")) {
                                text = lineReader.readLine("Edit "+field.getAttribute("name")+" secret key: ",null,field.getTextContent());
                            } else {
                                text = lineReader.readLine("Edit "+field.getAttribute("name")+": ",null,field.getTextContent());
                            }
                            Text textNode = passwordDatabase.createTextNode(text);
                            // delete old node first, otherwise the statement below will just append.
                            field.deleteTextContent();
                            field.appendChild(textNode);
                        } else {
                            pxmlElement field = currentElement.getChildElement(index-1);
                            String text = field.getTextContent();
                            if (field.getAttribute("name").equals("TOTP")) {
                                if (field.getTextContent().length() > 0) {
                                    text = TOTP.getCode(field.getTextContent());
                                }
                            }
                            ContentManager.copyToClipboard(text);
                            timer.schedule(new TimerTask() {
                                @Override
                                public void run() {
                                    ContentManager.copyToClipboard(null);
                                }
                            }, timeInterval); // run in 10k milisseconds
                        }
                    }
            }
            clearScreen();
        } while (!exitProgram);
        return null;
    }

    private static void settingsMenu(Document passwordDatabase) {
        fillWidth("=");
        System.out.println();
        System.out.println("(p) change password");
        System.out.println("(k) change key file");
        System.out.println("(t) change time interval to clear clipboard");
        System.out.println();
        fillWidth("=");
        System.out.printf("Enter an option: ");
        String input = scanner.nextLine();
        LineReader lineReader = LineReaderBuilder.builder().terminal(terminal).build();
        switch (input) {
            case "p":
                changePassword();
                break;
            case "k":
                lineReader = LineReaderBuilder.builder().terminal(terminal).completer(new FileNameCompleter()).build();
                keyFile = lineReader.readLine("Enter path to key file: ");
                while (keyFile.endsWith(" ")) {
                    // Remove the space using substring to avoid exception - this space may occur when completing with tab
                    keyFile = keyFile.substring(0, keyFile.length() - 1);
                }
                if (!Files.exists(Paths.get(keyFile))) {
                    message = "Cannot open '"+keyFile+"': files does not exist.";
                    keyFile = null;
                } else if (!Files.isReadable(Paths.get(keyFile))) {
                    message = "Cannot open '"+keyFile+"': no read permission.";
                    keyFile = null;
                } else if (!Files.isRegularFile(Paths.get(keyFile))) {
                    message = "Cannot open '"+keyFile+"': not a regular file.";
                    keyFile = null;
                } else {
                    saved = false; // keyFile updated in memory
                }
                break;
            case "t":
                String timer = lineReader.readLine("Input time interval (seconds): ",null,passwordDatabase.getDocumentElement().getAttribute("timeInterval"));
                try {
                    int milisseconds = Integer.parseInt(timer);
                    timer = String.valueOf(milisseconds);
                    passwordDatabase.getDocumentElement().setAttribute("timeInterval",timer);
                } catch (NumberFormatException e) {
                    message = "Timer was not updated. Input was not a number.";
                }
                break;
        }
    }

    private static void saveFile(Document passwordDatabase) {
        try {
            byte[] decryptedBytes = ContentManager.convertXMLDocumentToByteArray(passwordDatabase);
            byte[] encryptedBytes = CryptOps.encryptBytes(decryptedBytes,password,keyFile);
            ContentManager.writeBytesToFile(inputFile,encryptedBytes);
            System.out.println("Content successfully written to file!");
            saved = true;
            } catch (Exception e) {
                message = "Could not write content to file.";
                e.printStackTrace();
        }
    }

    private static String generatePassword(int length, String possibleCharacters) {
        SecureRandom random = new SecureRandom();
        StringBuilder stringBuilder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int randomIndex = random.nextInt(possibleCharacters.length());
            char randomChar = possibleCharacters.charAt(randomIndex);
            stringBuilder.append(randomChar);
        }
        return stringBuilder.toString();
    }

    private static void changePassword() {
        Console console = System.console();
        char[] oldPasswordChars = console.readPassword("Enter current password: ");
        String oldPassword = new String(oldPasswordChars);
        if (oldPassword.equals("..")) return;
        char[] newPasswordChars;
        char[] newPasswordChars2;
        String newPassword;
        String newPassword2;        
        // if (oldPassword.equals(password)) {
        do {
            newPasswordChars = console.readPassword("Enter new password: ");
            newPassword = new String(newPasswordChars);
            if (newPassword.equals("..")) return;
            newPasswordChars2 = console.readPassword("Confirm password: ");
            newPassword2 = new String(newPasswordChars2);
            if (newPassword2.equals("..")) return;
            if (!newPassword2.equals(newPassword)) System.err.println("Passwords do not match. Try again.");
        } while (!newPassword2.equals(newPassword));
        password = newPassword;
        clearScreen();
        System.out.println("Password set successfully!");
        // }
    }

    private static Terminal getTerminal() {
        try {
            Terminal terminal = TerminalBuilder.builder().build();
            return terminal;
        } catch (Exception e) {
            return null;
        }
    }

    private static void displayMenu(String[] options) {
        int terminalWidth;
        if (terminal == null) terminalWidth = 0;
        terminalWidth = terminal.getWidth();
        int menuLength = 0;
        for (int i=0; i<options.length; i++) {
            menuLength += options[i].length();
        }
        int spacing = 0;
        if (terminalWidth > menuLength) {
            spacing = (terminalWidth-menuLength)/(options.length+1);
        }
        for (int i=0; i<options.length; i++) {
            for (int j=0; j<spacing; j++) {
                    System.out.printf(" ");
            }
            System.out.printf("%s",options[i]);
        }
        System.out.printf("%n");
    }

    private static void fillWidth(String filling) {
        if (terminal == null) return;
        int repeat = terminal.getWidth()/filling.length();
        for (int i=0; i<repeat; i++) {
            System.out.printf(filling);
        }
        System.out.printf("%n");
    }

    private static void clearScreen() {
        // ANSI escape code to clear the screen
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    private static void help() {
        String help = """
        Usage:

        java -jar javazcrypt.jar [options] <file_path>

        Options:

        -k <keyfile_path>  Use a key file, additionally to a password.
        -d, --decrypt      Decrypt file.
        -e, --encrypt      Encrypt file.
        -o <output_file>   Specify output file when encrypting or decrypting. 
                           If no output file is specified file will be edited in place.

        If neither decryption or encryption operation is specified, 
        javazcrypt will try to open or create a password database.

        Examples:

        javar -jar javazcrypt.jar myPasswordDatabase

        java -jar javazcrypt.jar -k keyFile myPasswordDatabase

        java -jar javazcrypt.jar -d myEncryptedFile

        java -jar javazcrypt.jar -e -o encryptedOutputFile myDecryptedFile
        """;
        System.out.println(help);
    }
}
