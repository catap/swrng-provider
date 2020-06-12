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

    public byte[] getRandomBytes() throws IOException {
        if (devices.size() == 1) {
            return devices.get(0).getRandomBytes();
        }
        byte[] result = new byte[SwiftRNGDevice.RANDOM_BYTES_CHUNK];
        for (SwiftRNGDevice device : devices) {
            byte[] next = device.getRandomBytes();
            for (int i = 0; i < SwiftRNGDevice.RANDOM_BYTES_CHUNK; i++) {
                result[i] ^= next[i];
            }
        }
        return result;
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
