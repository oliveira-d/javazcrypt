# Javazcrypt

Simple command-line password manager and file encryption utility.

Build it with Maven using this command in the project's folder:

mvn package

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
