import java.io.File;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

import org.jline.reader.Completer;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

public class Main {
    public static void main(String[] args) throws Exception {

        //this creates a terminal
        Terminal terminal = TerminalBuilder.builder().system(true).build();

        // Create our custom completer!
        Completer shellCompleter = new ShellCompleter();
        LineReader lineReader = LineReaderBuilder.builder()
                .terminal(terminal)
                .completer(shellCompleter)
                .build();

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
            
            String command;
            try{
                //this reads a line from the terminal with the prompt "$ "
                command = lineReader.readLine("$ ").trim();
            }
            catch(UserInterruptException | EndOfFileException e){
                //this handles the exit if user press Ctrl+C or Ctrl+D
                break;
            }

            if (command.isEmpty()) {
                continue;
            }

            ParsedCommand parsedCommand = parseCommand(command);
            String[] parts = parsedCommand.arguments;
            String redirectTarget = parsedCommand.redirectTarget;
            String stderrTarget = parsedCommand.stderrTarget;
            boolean appendRedirect = parsedCommand.appendRedirect;
            boolean appendStderr = parsedCommand.appendStderr;
            if (parts.length == 0) {
                continue;
            }

            String verb = parts[0];
            PrintStream output = System.out;
            PrintStream errOutput = System.err;

            if (redirectTarget != null) {
                Path outputPath = Paths.get(redirectTarget);
                output = new PrintStream(Files.newOutputStream(outputPath,
                        StandardOpenOption.CREATE,
                        appendRedirect ? StandardOpenOption.APPEND : StandardOpenOption.TRUNCATE_EXISTING,
                        StandardOpenOption.WRITE));
            }
            if (stderrTarget != null) {
                Path errorPath = Paths.get(stderrTarget);
                errOutput = new PrintStream(Files.newOutputStream(errorPath,
                        StandardOpenOption.CREATE,
                        appendStderr ? StandardOpenOption.APPEND : StandardOpenOption.TRUNCATE_EXISTING,
                        StandardOpenOption.WRITE));
            }

            try {
                if (verb.equals("echo")) {
                    if (parts.length == 1) {
                        output.println();
                    } else {
                        output.println(String.join(" ", java.util.Arrays.copyOfRange(parts, 1, parts.length)));
                    }
                } else if (verb.equals("exit")) {
                    break;
                } else if (verb.equals("type")) {
                    if (parts.length < 2) {
                        errOutput.println("type: not enough arguments");
                    } else {
                        output.println(type(parts[1]));
                    }
                } else if (verb.equals("pwd")) {
                    output.println(System.getProperty("user.dir"));
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
                            errOutput.println("cd: " + parts[1] + ": No such file or directory");
                        }
                    } catch (Exception e) {
                        errOutput.println("cd: " + parts[1] + ": No such file or directory");
                    }
                } else if (getCommandPath(verb) != null) {
                    ProcessBuilder processBuilder = new ProcessBuilder(parts)
                            .directory(new File(System.getProperty("user.dir")));
                    if (redirectTarget != null) {
                        if (appendRedirect) {
                            processBuilder.redirectOutput(ProcessBuilder.Redirect.appendTo(new File(redirectTarget)));
                        } else {
                            processBuilder.redirectOutput(ProcessBuilder.Redirect.to(new File(redirectTarget)));
                        }
                    }
                    if (stderrTarget != null) {
                        if (appendStderr) {
                            processBuilder.redirectError(ProcessBuilder.Redirect.appendTo(new File(stderrTarget)));
                        } else {
                            processBuilder.redirectError(ProcessBuilder.Redirect.to(new File(stderrTarget)));
                        }
                    } else {
                        processBuilder.redirectError(ProcessBuilder.Redirect.INHERIT);
                    }
                    Process process = processBuilder.start();
                    if (redirectTarget == null) {
                        process.getInputStream().transferTo(System.out);
                    }
                    process.waitFor();
                } else {
                    errOutput.println(command + ": command not found");
                }
            } finally {
                if (redirectTarget != null) {
                    output.close();
                }
                if (stderrTarget != null) {
                    errOutput.close();
                }
            }
        }

    }

    public static ParsedCommand parseCommand(String command) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inSingleQuotes = false;
        boolean inDoubleQuotes = false;
        boolean tokenStarted = false;
        boolean escapeNext = false;
        String redirectTarget = null;
        String stderrTarget = null;
        boolean appendRedirect = false;
        boolean appendStderr = false;

        for (int i = 0; i < command.length(); i++) {
            char ch = command.charAt(i);

            if (escapeNext) {
                current.append(ch);
                escapeNext = false;
                tokenStarted = true;
                continue;
            }

            if (ch == '\\' && !inSingleQuotes) {
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

            if (!inSingleQuotes && !inDoubleQuotes) {
                if (ch == '>' && current.length() == 0) {
                    if (tokenStarted) {
                        tokens.add(current.toString());
                        current.setLength(0);
                        tokenStarted = false;
                    }
                    if (i + 1 < command.length() && command.charAt(i + 1) == '>') {
                        tokens.add(">>");
                        i++;
                    } else {
                        tokens.add(">" );
                    }
                    continue;
                }

                if ((ch == '1' || ch == '2') && i + 1 < command.length() && command.charAt(i + 1) == '>' && current.length() == 0) {
                    if (tokenStarted) {
                        tokens.add(current.toString());
                        current.setLength(0);
                        tokenStarted = false;
                    }
                    tokens.add(ch == '2' ? "2>" : ">");
                    i++;
                    continue;
                }
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

        List<String> arguments = new ArrayList<>();
        for (int i = 0; i < tokens.size(); i++) {
            String token = tokens.get(i);
            if (token.equals(">")) {
                if (i + 1 < tokens.size()) {
                    redirectTarget = tokens.get(i + 1);
                    appendRedirect = false;
                    i++;
                }
            } else if (token.equals(">>")) {
                if (i + 1 < tokens.size()) {
                    redirectTarget = tokens.get(i + 1);
                    appendRedirect = true;
                    i++;
                }
            } else if (token.equals("2>")) {
                if (i + 1 < tokens.size()) {
                    stderrTarget = tokens.get(i + 1);
                    appendStderr = false;
                    i++;
                }
            } else if (token.equals("2>>")) {
                if (i + 1 < tokens.size()) {
                    stderrTarget = tokens.get(i + 1);
                    appendStderr = true;
                    i++;
                }
            } else {
                arguments.add(token);
            }
        }

        return new ParsedCommand(arguments.toArray(new String[0]), redirectTarget, stderrTarget, appendRedirect, appendStderr);
    }

    public static class ParsedCommand {
        public final String[] arguments;
        public final String redirectTarget;
        public final String stderrTarget;
        public final boolean appendRedirect;
        public final boolean appendStderr;

        public ParsedCommand(String[] arguments, String redirectTarget, String stderrTarget, boolean appendRedirect, boolean appendStderr) {
            this.arguments = arguments;
            this.redirectTarget = redirectTarget;
            this.stderrTarget = stderrTarget;
            this.appendRedirect = appendRedirect;
            this.appendStderr = appendStderr;
        }
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
