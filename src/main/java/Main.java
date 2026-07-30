import java.util.ArrayList;
import java.util.List;
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

            String[] parts = splitCommand(command);
            String verb = parts[0];

            if (verb.equals("echo")) {
                if (parts.length == 1) {
                    System.out.println();
                } else {
                    System.out.println(String.join(" ", java.util.Arrays.copyOfRange(parts, 1, parts.length)));
                }
            } else if (verb.equals("exit")) {
                break;
            } else if (verb.equals("type")) {
                if (parts.length < 2) {
                    System.out.println("type: not enough arguments");
                } else {
                    System.out.println(type(parts[1]));
                }
            } else if (verb.equals("pwd")) {
                System.out.println(System.getProperty("user.dir"));
            } else if (verb.equals("cd")) {
                String target = parts.length < 2 ? "~" : parts[1];
                String homeDir = System.getenv("HOME");
                if (homeDir == null || homeDir.isEmpty()) {
                    homeDir = System.getProperty("user.home");
                }

                if (target.equals("~")) {
                    target = homeDir;
                } else if (target.startsWith("~/")) {
                    target = homeDir + target.substring(1);
                }

                File currentDir = new File(System.getProperty("user.dir"));
                File targetDir = new File(target);

                if (!targetDir.isAbsolute()) {
                    targetDir = new File(currentDir, target);
                }

                try {
                    File resolvedDir = targetDir.getCanonicalFile();
                    if (resolvedDir.exists() && resolvedDir.isDirectory()) {
                        System.setProperty("user.dir", resolvedDir.getAbsolutePath());
                    } else {
                        System.out.println("cd: " + parts[1] + ": No such file or directory");
                    }
                } catch (Exception e) {
                    System.out.println("cd: " + parts[1] + ": No such file or directory");
                }
            } else if (getCommandPath(verb) != null) {
                Process process = new ProcessBuilder(parts)
                        .directory(new File(System.getProperty("user.dir")))
                        .start();
                process.getInputStream().transferTo(System.out);
                process.waitFor();
            } else {
                System.out.println(command + ": command not found");
            }
        }
        sc.close();

    }

    public static String[] splitCommand(String command) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inSingleQuotes = false;
        boolean inDoubleQuotes = false;
        boolean tokenStarted = false;
        boolean escapeNext = false;

        for (int i = 0; i < command.length(); i++) {
            char ch = command.charAt(i);

            if (escapeNext) {
                current.append(ch);
                escapeNext = false;
                tokenStarted = true;
                continue;
            }

            if (ch == '\\' && !inSingleQuotes && !inDoubleQuotes) {
                escapeNext = true;
                continue;
            }

            if (ch == '\'' && !inDoubleQuotes) {
                inSingleQuotes = !inSingleQuotes;
                tokenStarted = true;
                continue;
            }

            if (ch == '"' && !inSingleQuotes) {
                inDoubleQuotes = !inDoubleQuotes;
                tokenStarted = true;
                continue;
            }

            if (Character.isWhitespace(ch) && !inSingleQuotes && !inDoubleQuotes) {
                if (tokenStarted) {
                    tokens.add(current.toString());
                    current.setLength(0);
                    tokenStarted = false;
                }
                continue;
            }

            current.append(ch);
            tokenStarted = true;
        }

        if (tokenStarted) {
            tokens.add(current.toString());
        }

        return tokens.toArray(new String[0]);
    }

    public static String type(String command){
        String[] commands = {"echo", "exit", "type", "pwd", "cd"};
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

    public static String getCommandPath(String command){
        String path = System.getenv("PATH");
        if(path == null){
            return null;
        }
        String[] pathDirs = path.split(File.pathSeparator);
        for(String pathDir : pathDirs){
            File file = new File(pathDir, command);
            if(file.exists() && file.canExecute()){
                return file.getAbsolutePath();
            }
        }
        return null;
    }
}
