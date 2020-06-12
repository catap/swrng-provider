package ky.korins.swrng;

import java.security.SecureRandomParameters;
import java.util.Collections;
import java.util.List;

public class SwiftRNGParameters implements SecureRandomParameters {
    private final List<String> specifiedDevices;

    public SwiftRNGParameters(List<String> specifiedDevices) {
        this.specifiedDevices = Collections.unmodifiableList(specifiedDevices);
    }

    public List<String> getSpecifiedDevices() {
        return specifiedDevices;
    }
}
