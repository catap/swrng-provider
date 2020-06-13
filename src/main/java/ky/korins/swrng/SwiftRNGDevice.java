package ky.korins.swrng;

import purejavacomm.*;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.locks.ReentrantLock;

public class SwiftRNGDevice implements Closeable {

    // constants are similar with swrngapi.cpp v1.2
    public static final int RANDOM_BYTES_CHUNK = 16000;

    private static final int READ_TIMEOUT_MILLIS = 100;
    private static final int EXECUTE_TIMEOUT_MILLIS = 2 * 1000 * 1000;

    private static final int CLEANUP_ITERATIONS = 3;
    private static final int EXECUTE_RETRY_COUNT = 15;

    enum Command {
        MODEL('m', 8),
        VERSION('v', 4),
        SERIAL_NUMBER('s', 15),

        FREQUENCY_TABLE('f', 512),

        RANDOM_BYTES('x', RANDOM_BYTES_CHUNK),
        FIRST_NOISE('<', RANDOM_BYTES_CHUNK),
        SECOND_NOISE('>', RANDOM_BYTES_CHUNK),

        DIAGNOSTICS('d', 0);

        final char cmd;
        final int responseSize;
        final byte[] bufferedEntropy;
        final int bufferedEntropyPos;

        Command(char cmd, int responseSize) {
            this.cmd = cmd;
            this.responseSize = responseSize;
            bufferedEntropy = new byte[responseSize];
            bufferedEntropyPos = responseSize;
        }
    }

    private final String path;

    private final CommPort usbSerialDevice;
    private final InputStream in;
    private final OutputStream out;

    private final ReentrantLock usbSerialDeviceLock = new ReentrantLock();

    private final String model;

    private final String version;

    private final String serialNumber;

    private final byte[] buffer = new byte[RANDOM_BYTES_CHUNK];
    private int bufferPos = RANDOM_BYTES_CHUNK;

    private final ReentrantLock bufferLock = new ReentrantLock();

    public SwiftRNGDevice(String path) throws IOException {
        this.path = path;
        try {
            usbSerialDevice = CommPortIdentifier
                    .getPortIdentifier(path)
                    .open("SwiftRNGDevice", READ_TIMEOUT_MILLIS);
        } catch (NoSuchPortException e) {
            throw new SwiftRNGException("Device not found: " + path);
        } catch (PortInUseException e) {
            throw new SwiftRNGException("Someone used device: " + path);
        }
        try {
            usbSerialDevice.enableReceiveTimeout(READ_TIMEOUT_MILLIS);
        } catch (UnsupportedCommOperationException e) {
            throw new SwiftRNGException("Can't setup timeout to device: " + path);
        }
        in = usbSerialDevice.getInputStream();
        out = usbSerialDevice.getOutputStream();
        cleanup();
        model = new String(execute(Command.MODEL));
        version = new String(execute(Command.VERSION));
        serialNumber = new String(execute(Command.SERIAL_NUMBER));
        setPowerProfile(9);
        selfDiagnostics();
    }

    private void cleanup() throws IOException {
        for (int i = 0; i < CLEANUP_ITERATIONS; i++) {
            while (in.read(buffer) > 0) ;
        }
    }

    private void execute(final char cmd, int responseSize, byte[] b, int off) throws IOException {
        int i = 0;
        usbSerialDeviceLock.lock();
        try {
            while (true) {
                try {
                    out.write(cmd);
                    long started = System.currentTimeMillis();
                    while (responseSize > 0) {
                        long spent = System.currentTimeMillis() - started;
                        if (spent > EXECUTE_TIMEOUT_MILLIS) {
                            throw new SwiftRNGException("Timeout happened at execution command '" + cmd + "' after " + spent + "ms");
                        }
                        int r = in.read(b, off, responseSize);
                        if (r < 0) {
                            throw new SwiftRNGException("Read from SwiftRNG returns " + r);
                        }
                        responseSize -= r;
                        off += r;
                    }
                    int status = in.read();
                    if (status != 0) {
                        throw new SwiftRNGException("Unexpected status: " + status);
                    }
                    return;
                } catch (Exception e) {
                    if (++i >= EXECUTE_RETRY_COUNT) {
                        throw e;
                    }
                    cleanup();
                }
            }
        } finally {
            usbSerialDeviceLock.unlock();
        }
    }

    private void execute(Command cmd, byte[] b, int off) throws IOException {
        execute(cmd.cmd, cmd.responseSize, b, off);
    }

    private byte[] execute(Command cmd) throws IOException {
        byte[] response = new byte[cmd.responseSize];
        execute(cmd.cmd, cmd.responseSize, response, 0);
        return response;
    }

    @Override
    public void close() throws IOException {
        usbSerialDevice.close();
    }

    public byte[] getFrequencyTable() throws IOException {
        return execute(Command.FREQUENCY_TABLE);
    }

    public void selfDiagnostics() throws IOException {
        execute(Command.DIAGNOSTICS);
    }

    public void setPowerProfile(int profile) throws IOException {
        if (profile < 0 || profile > 9) {
            throw new IllegalArgumentException("Unsupported power profile: " + profile);
        }
        execute((char) ('0' + profile), 0, null, 0);
    }

    public byte[] getRandomBytes() throws IOException {
        return execute(Command.RANDOM_BYTES);
    }


    public void getRandomBytes(byte[] b, int off, int len) throws IOException {
        int chunks = len / RANDOM_BYTES_CHUNK;
        while (chunks > 0) {
            execute(Command.RANDOM_BYTES, b, off);
            off += RANDOM_BYTES_CHUNK;
            len -= RANDOM_BYTES_CHUNK;
            chunks -= 1;
        }
        while (len > 0) {
            bufferLock.lock();
            try {
                if (bufferPos == RANDOM_BYTES_CHUNK) {
                    execute(Command.RANDOM_BYTES, buffer, 0);
                    bufferPos = 0;
                }
                int fromBuffer = Math.min(RANDOM_BYTES_CHUNK - bufferPos, len);
                if (fromBuffer > 0) {
                    System.arraycopy(buffer, bufferPos, b, off, fromBuffer);
                    off += fromBuffer;
                    len -= fromBuffer;
                    bufferPos += fromBuffer;
                }
            } finally {
                bufferLock.unlock();
            }
        }
    }

    public byte[] getFirstNoise() throws IOException {
        return execute(Command.FIRST_NOISE);
    }

    public byte[] getSecondNoise() throws IOException {
        return execute(Command.SECOND_NOISE);
    }

    @Override
    public String toString() {
        return "SwiftRNGDevice{" +
                "path=" + path +
                ", model='" + model + '\'' +
                ", version='" + version + '\'' +
                ", serialNumber='" + serialNumber + '\'' +
                '}';
    }
}
