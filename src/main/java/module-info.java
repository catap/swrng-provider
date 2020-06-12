module ky.korins.swrng {
    provides java.security.Provider with ky.korins.swrng.SwiftRNGProvider;
    exports ky.korins.swrng;
}