import org.junit.jupiter.api.*;
import java.nio.file.*;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.*;

public class CommandLineInterpreterTest {
    private static Path testDir;

    @BeforeAll
    public static void setUp() throws Exception {
        testDir = Files.createTempDirectory("cliTest");
        CommandLineInterpreter.cd(testDir.toString());  // Set test directory as current
    }

    @Test
    public void testPwd() {
        assertEquals(testDir.toAbsolutePath(), CommandLineInterpreter.getCurrentDirectory());
    }

    @Test
    public void testCd() {
        Path subDir = testDir.resolve("subDir");
        try {
            Files.createDirectory(subDir);
            CommandLineInterpreter.cd("subDir");
            assertEquals(subDir.toAbsolutePath(), CommandLineInterpreter.getCurrentDirectory());
        } catch (IOException e) {
            fail("Failed to create test directory for cd test.");
        } finally {
            CommandLineInterpreter.cd(testDir.toString()); // Return to testDir
            try { Files.deleteIfExists(subDir); } catch (IOException ignored) {}
        }
    }

    @Test
    public void testMkdir() {
        Path newDir = testDir.resolve("newDir");
        CommandLineInterpreter.mkdir("newDir");
        assertTrue(Files.exists(newDir) && Files.isDirectory(newDir), "Directory was not created.");

        try { Files.deleteIfExists(newDir); } catch (IOException ignored) {}
    }

    @Test
    public void testRmdir() {
        Path dirToRemove = testDir.resolve("dirToRemove");
        try {
            Files.createDirectory(dirToRemove);
            assertTrue(Files.exists(dirToRemove), "Directory should exist before removal.");
            CommandLineInterpreter.rmdir("dirToRemove");
            assertFalse(Files.exists(dirToRemove), "Directory was not removed.");

            Path fileToRemove = testDir.resolve("fileToRemove.txt");
            Files.createFile(fileToRemove);
            String output = captureOutput(() -> CommandLineInterpreter.rmdir("fileToRemove.txt"));
            assertTrue(output.contains("is not a directory"), "rmdir should fail on a file.");
            assertTrue(Files.exists(fileToRemove), "File should not be removed by rmdir.");
            Files.delete(fileToRemove);

        } catch (IOException e) {
            fail("Failed to set up test files for rmdir test.");
        }
    }

    @Test
    public void testTouchAndRm() {
        Path file = testDir.resolve("testFile.txt");
        CommandLineInterpreter.touch("testFile.txt");
        assertTrue(Files.exists(file) && Files.isRegularFile(file), "File was not created.");

        CommandLineInterpreter.rm("testFile.txt");
        assertFalse(Files.exists(file), "File was not removed.");
    }

    @Test
    public void testLs() throws IOException {
        Path file1 = Files.createFile(testDir.resolve("file1.txt"));
        Path file2 = Files.createFile(testDir.resolve("file2.txt"));

        String output = captureOutput(() -> CommandLineInterpreter.ls(new String[]{}));
        assertTrue(output.contains("file1.txt"), "file1.txt not listed by ls.");
        assertTrue(output.contains("file2.txt"), "file2.txt not listed by ls.");

        Files.delete(file1);
        Files.delete(file2);
    }

    @Test
    public void testLsAll() throws IOException {
        Path hiddenFile = Files.createFile(testDir.resolve(".hiddenFile.txt"));
        Path visibleFile = Files.createFile(testDir.resolve("visibleFile.txt"));

        String output = captureOutput(() -> CommandLineInterpreter.ls(new String[]{"-a"}));
        assertTrue(output.contains(".hiddenFile.txt"), "ls -a did not list hidden files.");
        assertTrue(output.contains("visibleFile.txt"), "ls -a did not list visible files.");

        Files.delete(hiddenFile);
        Files.delete(visibleFile);
    }

    @Test
    public void testLsReverse() throws IOException {
        Path firstFile = Files.createFile(testDir.resolve("firstFile.txt"));
        Path secondFile = Files.createFile(testDir.resolve("secondFile.txt"));

        String output = captureOutput(() -> CommandLineInterpreter.ls(new String[]{"-r"}));
        assertTrue(output.indexOf("secondFile.txt") < output.indexOf("firstFile.txt"), "ls -r did not list files in reverse order.");

        Files.delete(firstFile);
        Files.delete(secondFile);
    }


    @Test
    public void testCat() throws IOException {
        Path file = Files.createFile(testDir.resolve("sample.txt"));
        String content = "Hello, World!";
        Files.writeString(file, content);

        String output = captureOutput(() -> CommandLineInterpreter.cat("sample.txt"));
        assertEquals(content, output.trim(), "File contents do not match.");

        Files.delete(file);
    }

    @Test
    public void testMv() {
        Path originalFile = testDir.resolve("originalFile.txt");
        Path renamedFile = testDir.resolve("renamedFile.txt");
        Path newDir = testDir.resolve("newDir");
        Path movedFile = newDir.resolve("originalFile.txt");

        try {
            Files.createFile(originalFile);
            Files.createDirectory(newDir);

            CommandLineInterpreter.mv("originalFile.txt", "renamedFile.txt");
            assertTrue(Files.exists(renamedFile), "File was not renamed.");
            assertFalse(Files.exists(originalFile), "Original file should not exist after renaming.");

            CommandLineInterpreter.mv("renamedFile.txt", "newDir/originalFile.txt");
            assertTrue(Files.exists(movedFile), "File was not moved.");
            assertFalse(Files.exists(renamedFile), "Renamed file should not exist after moving.");

        } catch (IOException e) {
            fail("Failed to set up test files for mv test.");
        } finally {
            try {
                Files.deleteIfExists(movedFile);
                Files.deleteIfExists(newDir);
            } catch (IOException ignored) {}
        }
    }

    @Test
    public void testHelp() {
        String output = captureOutput(CommandLineInterpreter::help);
        assertTrue(output.contains("pwd"), "Help output missing 'pwd' command.");
        assertTrue(output.contains("cd"), "Help output missing 'cd' command.");
        assertTrue(output.contains("mkdir"), "Help output missing 'mkdir' command.");
        assertTrue(output.contains("mv"), "Help output missing 'mv' command.");
    }

    @Test
    public void testRedirection() throws IOException {
        Path outputFile = testDir.resolve("output.txt");
        String command = "ls > output.txt";
        Files.createFile(outputFile);

        String output = captureOutput(() -> CommandLineInterpreter.handleRedirection(command));
        assertTrue(output.contains("Output redirected to output.txt"));

        String fileContent = Files.readString(outputFile);
        assertTrue(fileContent.contains("output.txt"), "Redirection did not capture ls output.");

        Files.deleteIfExists(outputFile);
    }

    @Test
    public void testAppendRedirection() throws IOException {
        Path outputFile = testDir.resolve("output.txt");
        Files.writeString(outputFile, "Initial Content\n");

        CommandLineInterpreter.handleRedirection("pwd >> output.txt");
        String fileContent = Files.readString(outputFile);
        assertTrue(fileContent.contains("Initial Content"), "Append redirection overwrote file.");
        assertTrue(fileContent.contains(CommandLineInterpreter.getCurrentDirectory().toString()),
                "Append redirection did not add new content correctly.");

        Files.deleteIfExists(outputFile);
    }

    @Test
    public void testPipe() throws IOException {
        Path testFile = testDir.resolve("pipeTest.txt");
        Files.writeString(testFile, "Pipe Test Content\n");

        String output = captureOutput(() -> CommandLineInterpreter.handlePipe("ls | cat"));
        assertTrue(output.contains("pipeTest.txt"), "Pipe output did not contain expected file.");

        Files.deleteIfExists(testFile);
    }

    @AfterAll
    public static void tearDown() throws Exception {
        Files.walk(testDir)
                .map(Path::toFile)
                .forEach(File::delete);
    }

    private String captureOutput(Runnable command) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outputStream));
        command.run();
        System.setOut(originalOut);
        return outputStream.toString();
    }
}
