package ky.korins.swrng;

import java.io.IOException;

public class Demo {

    public static void main(String[] args) throws IOException {
        for (String path : SwiftRNGDevices.scanDevices()) {
            SwiftRNGDevice devices = new SwiftRNGDevice(path);
            System.out.println(devices.toString());
        }
    }
}
