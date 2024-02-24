# javazcrypt

Simple command-line password manager and file encryption utility.

Build it with Gradle. Open a terminal in the project's folder and, on unix-like systems, execute this command:
```
sh gradlew build
```
On Windows, execute this command:
```
gradlew.bat build
```
Note: on Windows you need to set the %JAVA_HOME% environment variable. To find what the %JAVA_HOME% should be, With JDK installed, execute the command "where javac". The output will be %JAVA_HOME%\bin\javac.exe

```
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
```
