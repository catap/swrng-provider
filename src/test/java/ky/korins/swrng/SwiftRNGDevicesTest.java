package ky.korins.swrng;

import junit.framework.TestCase;

import java.io.IOException;

public class SwiftRNGDevicesTest extends TestCase {
    public void testMultipleDevices() throws IOException {
        String path = SwiftRNGDevices.scanDevices().get(0);
        SwiftRNGDevices devices = new SwiftRNGDevices(path, path, path);
        byte[] small = new byte[1];
        devices.getRandomBytes(small, 0, small.length);
        byte[] big = new byte[1000000];
        devices.getRandomBytes(big, 0, big.length);
    }
}