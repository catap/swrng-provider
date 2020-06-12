package ky.korins.swrng;

import java.security.Provider;

public class SwiftRNGProvider extends Provider {

    public static final String NAME = "SwiftRNG";

    private static final long serialVersionUID = 1L;

    public SwiftRNGProvider() {
        super(NAME, "1.0", "SwiftRNG (SecureRandom)");
        this.put("SecureRandom." + NAME, SwiftRNG.class.getName());
        this.put("SecureRandom." + NAME + " ThreadSafe", "true");
    }
}
