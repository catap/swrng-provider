module ky.korins.swrng {
    requires purejavacomm;
    provides java.security.Provider with ky.korins.swrng.SwiftRNGProvider;
    exports ky.korins.swrng;
}