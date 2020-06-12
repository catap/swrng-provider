package ky.korins.swrng;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedList;
import java.util.List;

public class SwiftRNGDevices {
    public List<SwiftRNGDevice> devices;

    public SwiftRNGDevices(String... paths) throws IOException {
        if (paths.length == 0) {
            throw new SwiftRNGException("At least one device should be specified");
        }

        devices = new LinkedList<>();
        for (String path : paths) {
            SwiftRNGDevice device = new SwiftRNGDevice(path);
            devices.add(device);
        }
    }

    public void getRandomBytes(byte[] b, int off, int len) throws IOException {
        if (devices.size() == 1) {
            devices.get(0).getRandomBytes(b, off, len);
        } else {
            byte[] next = new byte[len];
            for (SwiftRNGDevice device : devices) {
                device.getRandomBytes(next, 0, len);
                for (int i = 0; i < len; i++) {
                    b[i + off] ^= next[i];
                }
            }
        }
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

    public static SwiftRNGDevices openAllDevices() throws IOException {
        return new SwiftRNGDevices(scanDevices().toArray(new String[0]));
    }
}
