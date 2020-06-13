package ky.korins.swrng;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedList;
import java.util.List;

public class SwiftRNGDevices {
    private final SwiftRNGDevice[] devices;

    private volatile int index = 0;

    public SwiftRNGDevices(List<String> paths) throws IOException {
        if (paths.isEmpty()) {
            throw new SwiftRNGException("At least one device should be specified");
        }

        List<SwiftRNGDevice> devices = new LinkedList<>();
        for (String path : paths) {
            SwiftRNGDevice device = new SwiftRNGDevice(path);
            devices.add(device);
        }
        this.devices = devices.toArray(new SwiftRNGDevice[0]);
    }

    public void getRandomBytes(byte[] b, int off, int len) throws IOException {
        devices[index++ % devices.length].getRandomBytes(b, off, len);
    }

    public static List<String> scanDevices() throws IOException {
        List<String> devices = new LinkedList<>();

        // Linux support a nice way to detect devices
        File serialById = new File("/dev/serial/by-id");
        File dev = new File("/dev");
        if (serialById.isDirectory()) {
            Files.list(serialById.toPath()).forEach(file -> {
                if (file.getFileName().toString().contains("TectroLabs_SwiftRNG")) {
                    devices.add(file.toAbsolutePath().toString());
                }
            });
        } else if (dev.isDirectory()) {
            Files.list(dev.toPath()).forEach(file -> {
                if (file.getFileName().toString().startsWith("cu.usbmodemSWRNG")) {
                    devices.add(file.toAbsolutePath().toString());
                }
            });
        }

        return devices;
    }

    public static void main(String[] args) throws IOException {
        for (String path : scanDevices()) {
            System.out.println(new SwiftRNGDevice(path));
        }
    }
}
