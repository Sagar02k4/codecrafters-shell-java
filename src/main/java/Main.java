import java.util.Scanner;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Main {
    public static void main(String[] args) throws Exception {

        Scanner sc = new Scanner(System.in);

        // implemented type builtin
        // need to implement
        // pwd
        // cd
        // mkdir
        // cat
        // export
        // which
        // if statement
        // exit status
        
        while(true){
            System.out.print("$ ");
            String command = sc.nextLine().trim();
            if (command.isEmpty()) {
                continue;
            }

            String[] parts = command.split("\\s+");
            String verb = parts[0];

            if (verb.equals("echo")) {
                if (parts.length == 1) {
                    System.out.println();
                } else {
                    System.out.println(command.substring(5));
                }
            } else if (verb.equals("exit")) {
                break;
            } else if (verb.equals("type")) {
                if (parts.length < 2) {
                    System.out.println("type: not enough arguments");
                } else {
                    System.out.println(type(parts[1]));
                }
            } else {
                System.out.println(command + ": command not found");
            }
        }
        sc.close();

    }

    public static String type(String command){
        String[] commands = {"echo", "exit", "type"};
        String path = System.getenv("PATH");
        if (path == null) {
            path = "";
        }
        String[] pathDirs = path.split(File.pathSeparator);

        for(int i = 0; i < commands.length; i++){
            if(commands[i].equals(command)){
                return command + " is a shell builtin";
            }
        }

        for(int i = 0; i < pathDirs.length; i++){
            if (pathDirs[i].isEmpty()) {
                continue;
            }
            java.nio.file.Path candidate = Paths.get(pathDirs[i], command);
            if (Files.isRegularFile(candidate) && Files.isExecutable(candidate)){
                return command + " is " + candidate.toAbsolutePath().toString();
            }
        }

        return command + ": not found";
    }
}
