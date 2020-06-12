package ky.korins.swrng;

import java.io.IOException;
import java.security.ProviderException;
import java.security.SecureRandomSpi;

public class SwiftRNG extends SecureRandomSpi {

    SwiftRNGDevices devices;

    public SwiftRNG() throws IOException {
        devices = SwiftRNGDevices.openAllDevices();
    }

    @Override
    protected void engineSetSeed(byte[] seed) {
        // SwiftRNG doesn't support seed bytes, ignore it
    }

    @Override
    protected void engineNextBytes(byte[] bytes) {
        try {
            int p = 0;
            do {
                byte[] next = devices.getRandomBytes();
                int used = Math.min(bytes.length - p, next.length);
                System.arraycopy(next, 0, bytes, p, used);
                p += used;
            } while (p < bytes.length);
        } catch (IOException e) {
            throw new ProviderException("Can't obtain next bytes: " + e.getMessage(), e);
        }
    }

    @Override
    protected byte[] engineGenerateSeed(int numBytes) {
        if (numBytes < 0) {
            throw new IllegalArgumentException("numBytes must not be negative");
        }
        byte[] bytes = new byte[numBytes];
        if (numBytes > 0) {
            this.engineNextBytes(bytes);
        }
        return bytes;
    }
}
