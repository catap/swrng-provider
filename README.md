# SwiftRNG SecureRandomSPI

A `SecureRandomSPI` that makes SwiftRNG devices available to `SecureRandom`.

This code automatically detect attached devices on Linux and macOS,
and when a machine has more of one SwiftRNG it makes XOR between bytes.

To use it you should add this provider to dependency like
```
<dependency>
  <groupId>ky.korins</groupId>
  <artifactId>swrng-provider</artifactId>
  <version>1.0.1</version>
</dependency>
```
and use via specified instance:
```
  SecureRandom random = SecureRandom.getInstance("SwiftRNG");
```

But you should configure it. The easy way is adding this security provider by hand:
```
  Security.addProvider(new SwiftRNGProvider());
```
or you can add it to `java.security` as
```
security.provider.N=SwiftRNG
```
where `N` should be the value of the last provider incremented by `1`.

You can also select one or more devices that this code should use.
For example we would like to use SwiftRNG that attached as `/dev/cu.usbmodemSWRNGP000A0061` we can do:
```
SecureRandom random = SecureRandom.getInstance("SwiftRNG", new SwiftRNGParameters(Collections.singletonList("/dev/cu.usbmodemSWRNGP000A0061")));
```
or you can specified at `java.security` comma separated list of used devices such as:
```
securerandom.swiftrng.devices=/dev/cu.usbmodemSWRNGP000A0061,/dev/cu.usbmodemSWRNGP000A0062
```
and this is the only way to use this provider at Windows where you can get path to the device by `mode`.

Thus, this code is using locking to prevent parallel using the device and this add limitation
 that only one `SecureRandom` instance can be created per device that is thread safe.