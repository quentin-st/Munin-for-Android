package com.chteuchteu.munin;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;
import java.util.Scanner;

import static org.junit.Assert.fail;

abstract public class BaseUnitTest {
    protected static String readResource(Class<?> testClass, String path)
    {
        URL fullPath = testClass.getClassLoader().getResource(path);

        if (fullPath == null) {
            fail("Test failed - missing expected HTML output file, expected path was "+path);
            return null;
        }

        return readFile(fullPath.getFile());
    }

    protected static String readFile(String path)
    {
        Scanner sc = null;
        try {
            sc = new Scanner(new File(path));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        // Read content line by line
        StringBuilder content = new StringBuilder();
        while(sc.hasNextLine()){
            content.append(sc.nextLine());
        }
        return content.toString();
    }
}
