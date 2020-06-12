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
