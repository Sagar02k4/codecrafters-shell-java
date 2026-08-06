import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

public class ShellCompleter implements Completer {
    // builtin command names that are auto completed 
    private final String[] builtIns = {"echo", "exit", "type", "pwd", "cd"};

    @Override
    public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
        String word = line.word(); // The current word being typed
        int wordIndex = line.wordIndex(); // 0 = first word, 1 = second word, etc.

        if (wordIndex == 0) {
            // We are typing the command itself. Search built-ins and PATH!
            addCommandCandidates(word, candidates);
        } else {
            // We are typing an argument (wordIndex > 0)
            String command = line.words().get(0); // get the root command

            if (command.equals("type")) {
                // The 'type' command takes other commands as arguments, so we do the same search!
                addCommandCandidates(word, candidates);
            } 
        }
    }

    private void addCommandCandidates(String prefix, List<Candidate> candidates) {
        // We use a HashSet so if an executable exists in multiple PATH folders, we only show it once
        Set<String> uniqueMatches = new HashSet<>();

        // Add built-in commands
        for (String cmd : builtIns) {
            if (cmd.startsWith(prefix)) {
                uniqueMatches.add(cmd);
            }
        }

        // Add executables from the system PATH
        String pathEnv = System.getenv("PATH");
        if (pathEnv != null) {
            String[] paths = pathEnv.split(File.pathSeparator);
            for (String p : paths) {
                File dir = new File(p);
                if (dir.exists() && dir.isDirectory()) {
                    File[] files = dir.listFiles();
                    if (files != null) {
                        for (File f : files) {
                            // If it starts with what we typed, is a file, and is executable
                            if (f.getName().startsWith(prefix) && !f.isDirectory() && f.canExecute()) {
                                uniqueMatches.add(f.getName());
                            }
                        }
                    }
                }
            }
        }

        // add all our unique matches to the JLine candidates list
        for (String match : uniqueMatches) {
            candidates.add(new Candidate(match)); // JLine will use these!
        }
    }
}