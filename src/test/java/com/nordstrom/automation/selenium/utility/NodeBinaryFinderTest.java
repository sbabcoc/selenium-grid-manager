package com.nordstrom.automation.selenium.utility;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class NodeBinaryFinderTest {

    private static final String TEST_KEY = "test.binary.path";
    private Path tempDir;

    @Before
    public void createTempDir() throws IOException {
        tempDir = Files.createTempDirectory("BinaryFinderTest");
    }

    @After
    public void cleanup() throws IOException {
        System.clearProperty(TEST_KEY);
        Files.walk(tempDir).sorted(Comparator.reverseOrder())
            .map(Path::toFile).forEach(File::delete);
    }

    @Test
    public void findBinary_usesConfiguredPath() throws IOException {
        Path fakeExe = createFakeExecutable("fakebinary");
        System.setProperty(TEST_KEY, fakeExe.toString());
        assertEquals(NodeBinaryFinder.findBinary("fakebinary", TEST_KEY), fakeExe.toFile());
    }

    @Test(expected = IllegalStateException.class)
    public void findBinary_throwsWhenConfiguredPathMissing() {
        System.setProperty(TEST_KEY, "/nonexistent/path/binary");
        NodeBinaryFinder.findBinary("binary", TEST_KEY);
    }

    @Test(expected = IllegalStateException.class)
    public void findBinary_throwsWhenConfiguredPathNotExecutable() throws IOException {
        Path fakeFile = tempDir.resolve("notexecutable");
        Files.createFile(fakeFile);
        System.setProperty(TEST_KEY, fakeFile.toString());
        NodeBinaryFinder.findBinary("notexecutable", TEST_KEY);
    }

    @Test(expected = IllegalStateException.class)
    public void findBinary_throwsWhenNotOnPath() {
        NodeBinaryFinder.findBinary(
            "definitely_not_a_real_binary_xyzzy_" + System.nanoTime(), TEST_KEY);
    }

    private Path createFakeExecutable(String name) throws IOException {
        Path file = tempDir.resolve(name);
        Files.createFile(file);
        file.toFile().setExecutable(true);
        return file;
    }
}
