package ky.korins.swrng;

import java.io.IOException;
import java.security.*;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Implement the interface between SwiftRNG devices via <i>Service Provider Interface</i> (<b>SPI</b>)
 * for the {@link SecureRandom} class.
 *
 * This interface try to use all available SwiftRNG devices if user hasn't got any preferences.
 *
 * This preferences can be specified via "securerandom.swiftrng.devices" value in java.security,
 * or via {@link SwiftRNGParameters} parameters.
 */
public class SwiftRNG extends SecureRandomSpi {

    private SwiftRNGDevices devices;

    private static final String PROP_NAME = "securerandom.swiftrng.devices";

    public SwiftRNG() throws IOException {
        this(null);
    }

    public SwiftRNG(SecureRandomParameters params) throws IOException {
        super(params);
        List<String> specifiedDevices = new LinkedList<>();

        String config = AccessController.doPrivileged((PrivilegedAction<String>)
                () -> Security.getProperty(PROP_NAME));
        if (config != null && !config.isEmpty()) {
            specifiedDevices.addAll(Arrays.asList(config.split(",")));
        }

        if (params != null) {
            if (params instanceof SwiftRNGParameters) {
                specifiedDevices.addAll(((SwiftRNGParameters) params).getSpecifiedDevices());
            } else {
                throw new IllegalArgumentException("Unsupported params: " + params.getClass());
            }
        }

        if (specifiedDevices.isEmpty()) {
            specifiedDevices.addAll(SwiftRNGDevices.scanDevices());
        }

        devices = new SwiftRNGDevices(specifiedDevices);
    }

    @Override
    protected void engineSetSeed(byte[] seed) {
        // SwiftRNG doesn't support seed bytes, ignore it
    }

    @Override
    protected void engineNextBytes(byte[] bytes) {
        try {
            devices.getRandomBytes(bytes, 0, bytes.length);
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
