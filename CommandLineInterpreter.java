import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.Arrays;

public class CommandLineInterpreter {
    private static Path currentDirectory = Paths.get("").toAbsolutePath();
    private static boolean running = true;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Welcome to the CLI. Type 'help' to see available commands.");

        while (running) {
            System.out.print(currentDirectory + " > ");
            String input = scanner.nextLine().trim();

            if (input.contains(">") || input.contains(">>")) {
                handleRedirection(input);
                continue;
            } else if (input.contains("|")) {
                handlePipe(input);
                continue;
            }

            String[] commandParts = input.split("\\s+");
            if (commandParts.length == 0) continue;

            String command = commandParts[0].toLowerCase();
            try {
                switch (command) {
                    case "pwd":
                        pwd();
                        break;
                    case "cd":
                        if (commandParts.length > 1) {
                            cd(commandParts[1]);
                        } else {
                            System.out.println("Error: 'cd' requires a directory path.");
                        }
                        break;
                    case "ls":
                        String[] lsOptions = Arrays.copyOfRange(commandParts, 1, commandParts.length);
                        ls(lsOptions);
                        break;
                    case "mkdir":
                        if (commandParts.length > 1) {
                            mkdir(commandParts[1]);
                        } else {
                            System.out.println("Error: 'mkdir' requires a directory name.");
                        }
                        break;
                    case "rmdir":
                        if (commandParts.length > 1) {
                            rmdir(commandParts[1]);
                        } else {
                            System.out.println("Error: 'rmdir' requires a directory name.");
                        }
                        break;
                    case "touch":
                        if (commandParts.length > 1) {
                            touch(commandParts[1]);
                        } else {
                            System.out.println("Error: 'touch' requires a file name.");
                        }
                        break;
                    case "rm":
                        if (commandParts.length > 1) {
                            rm(commandParts[1]);
                        } else {
                            System.out.println("Error: 'rm' requires a file name.");
                        }
                        break;
                    case "cat":
                        if (commandParts.length > 1) {
                            cat(commandParts[1]);
                        } else {
                            System.out.println("Error: 'cat' requires a file name.");
                        }
                        break;
                    case "mv":
                        if (commandParts.length > 2) {
                            mv(commandParts[1], commandParts[2]);
                        } else {
                            System.out.println("Error: 'mv' requires a source and a destination.");
                        }
                        break;
                    case "help":
                        help();
                        break;
                    case "exit":
                        exit();
                        break;
                    default:
                        System.out.println("Error: Unknown command. Type 'help' for a list of commands.");
                }
            } catch (Exception e) {
                System.out.println("Error executing command: " + e.getMessage());
            }
        }
    }

    public static void pwd() {
        System.out.println(currentDirectory);
    }

    public static void cd(String directory) {
        Path newDir = currentDirectory.resolve(directory).normalize();
        if (Files.isDirectory(newDir)) {
            currentDirectory = newDir;
        } else {
            System.out.println("Error: Directory not found.");
        }
    }

    public static void ls(String[] options) {
        boolean showAll = false;
        boolean reverseOrder = false;

        // Parse options for -a (all) and -r (reverse)
        for (String option : options) {
            if (option.equals("-a")) {
                showAll = true;
            } else if (option.equals("-r")) {
                reverseOrder = true;
            }
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(currentDirectory)) {
            List<Path> entries = new ArrayList<>();
            for (Path entry : stream) {
                if (showAll || !entry.getFileName().toString().startsWith(".")) {
                    entries.add(entry);
                }
            }

            // Reverse the list if -r option is enabled
            if (reverseOrder) {
                Collections.reverse(entries);
            }

            // Print the entries
            for (Path entry : entries) {
                System.out.println(entry.getFileName());
            }
        } catch (IOException e) {
            System.out.println("Error: Unable to list directory contents.");
        }
    }


    public static void mkdir(String directoryName) {
        Path newDir = currentDirectory.resolve(directoryName);
        try {
            Files.createDirectory(newDir);
            System.out.println("Directory created: " + directoryName);
        } catch (IOException e) {
            System.out.println("Error: Could not create directory.");
        }
    }

    public static void rmdir(String directoryName) {
        Path dirToDelete = currentDirectory.resolve(directoryName);
        try {
            if (Files.exists(dirToDelete)) {
                if (Files.isDirectory(dirToDelete)) {
                    Files.delete(dirToDelete);
                    System.out.println("Directory removed: " + directoryName);
                } else {
                    System.out.println("Error: '" + directoryName + "' is not a directory.");
                }
            } else {
                System.out.println("Error: Directory not found.");
            }
        } catch (IOException e) {
            System.out.println("Error: Could not remove directory.");
        }
    }

    public static void touch(String fileName) {
        Path filePath = currentDirectory.resolve(fileName);
        try {
            Files.createFile(filePath);
            System.out.println("File created: " + fileName);
        } catch (IOException e) {
            System.out.println("Error: Could not create file.");
        }
    }

    public static void rm(String fileName) {
        Path filePath = currentDirectory.resolve(fileName);
        try {
            if (Files.exists(filePath)) {
                if (Files.isRegularFile(filePath)) {
                    Files.delete(filePath);
                    System.out.println("File removed: " + fileName);
                } else {
                    System.out.println("Error: '" + fileName + "' is a directory, not a file.");
                }
            } else {
                System.out.println("Error: File not found.");
            }
        } catch (IOException e) {
            System.out.println("Error: Could not remove file.");
        }
    }

    public static void cat(String fileName) {
        Path filePath = currentDirectory.resolve(fileName);
        try {
            Files.lines(filePath).forEach(System.out::println);
        } catch (IOException e) {
            System.out.println("Error: Could not read file.");
        }
    }

    public static void mv(String sourceName, String destinationName) {
        Path sourcePath = currentDirectory.resolve(sourceName);
        Path destinationPath = currentDirectory.resolve(destinationName);

        try {
            if (Files.exists(sourcePath)) {
                Files.move(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING);
                System.out.println("Moved/Renamed '" + sourceName + "' to '" + destinationName + "'.");
            } else {
                System.out.println("Error: Source '" + sourceName + "' does not exist.");
            }
        } catch (IOException e) {
            System.out.println("Error: Could not move/rename '" + sourceName + "'.");
        }
    }

    public static Path getCurrentDirectory() {
        return currentDirectory;
    }

    public static void help() {
        System.out.println("Available Commands:\n"
                + "pwd - Print working directory\n"
                + "cd <directory> - Change directory\n"
                + "ls - List directory contents\n"
                + "mkdir <name> - Create directory\n"
                + "rmdir <name> - Remove directory\n"
                + "touch <name> - Create file\n"
                + "rm <name> - Remove file\n"
                + "cat <name> - Display file contents\n"
                + "mv <source> <destination> - Move or rename a file or directory\n"
                + "> <file> - Redirect output to a file (overwrite)\n"
                + ">> <file> - Redirect output to a file (append)\n"
                + "| - Pipe the output of one command to another\n"
                + "exit - Exit the CLI\n"
                + "help - Display this help message\n");
    }

    public static void exit() {
        System.out.println("Exiting CLI. Goodbye!");
        running = false;
    }

    public static void handleRedirection(String input) {
        String[] parts;
        boolean append = input.contains(">>");

        if (append) {
            parts = input.split(">>");
        } else {
            parts = input.split(">");
        }

        if (parts.length < 2) {
            System.out.println("Error: Invalid syntax for redirection.");
            return;
        }

        String command = parts[0].trim();
        String fileName = parts[1].trim();
        Path filePath = currentDirectory.resolve(fileName);

        try {
            PrintStream originalOut = System.out;
            PrintStream fileOut = new PrintStream(new FileOutputStream(filePath.toFile(), append));
            System.setOut(fileOut);

            if (command.equals("ls")) {
                ls(new String[]{});
            } else if (command.equals("pwd")) {
                pwd();
            } else {
                System.out.println("Error: Redirection only supports ls and pwd commands.");
            }

            System.setOut(originalOut);
            fileOut.close();
            System.out.println("Output redirected to " + fileName);

        } catch (IOException e) {
            System.out.println("Error: Could not redirect output to file.");
        }
    }

    public static void handlePipe(String input) {
        String[] commands = input.split("\\|");

        if (commands.length < 2) {
            System.out.println("Error: Invalid syntax for piping.");
            return;
        }

        String firstCommand = commands[0].trim();
        String secondCommand = commands[1].trim();

        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PrintStream originalOut = System.out;
            System.setOut(new PrintStream(outputStream));

            if (firstCommand.equals("ls")) {
                ls(new String[]{});
            } else if (firstCommand.equals("pwd")) {
                pwd();
            } else {
                System.out.println("Error: Unsupported command for piping.");
                return;
            }

            System.out.flush();
            System.setOut(originalOut);

            if (secondCommand.startsWith("cat")) {
                String output = outputStream.toString();
                System.out.println(output);
            } else {
                System.out.println("Error: Unsupported second command for piping.");
            }

        } catch (Exception e) {
            System.out.println("Error executing piped command: " + e.getMessage());
        }
    }
}
