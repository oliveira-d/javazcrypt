# Javazcrypt

Simple command-line password manager and file encryption utility.

Build it with Maven using this command in the project's folder:

mvn package

Usage:

java -jar javazcrypt.jar [options] <file_path>

Options:

  -k <keyfile_path>	          use key file with the password to encrypt the file
  
  --create <file_path>	          create new file

  -d,--decrypt                    decrypt file

  -e,--encrypt                    encrypt file

  -o <output_file>                specify output file when encrypting or decrypting. if no output file is specified file will be edited in place

If neither decryption or encryption operation is specified, javazcrypt will try to open or create a password database.
