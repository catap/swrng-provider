package ky.korins.swrng;

import junit.framework.TestCase;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Arrays;

public class SwiftRNGTest extends TestCase {
    public void testEngineNextBytes() throws NoSuchAlgorithmException {
        Security.addProvider(new SwiftRNGProvider());
        SecureRandom random = SecureRandom.getInstance("SwiftRNG");
        byte[] small = new byte[1];
        random.nextBytes(small);
        byte[] big = new byte[1000000];
        random.nextBytes(big);
    }

    public void testMultipleDevices() throws IOException, NoSuchAlgorithmException {
        Security.addProvider(new SwiftRNGProvider());
        String path = SwiftRNGDevices.scanDevices().get(0);
        SwiftRNGParameters parameters = new SwiftRNGParameters(Arrays.asList(path, path, path));
        SecureRandom random = SecureRandom.getInstance("SwiftRNG", parameters);
        byte[] small = new byte[1];
        random.nextBytes(small);
        byte[] big = new byte[1000000];
        random.nextBytes(big);
    }
}