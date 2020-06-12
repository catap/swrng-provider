package ky.korins.swrng;

import java.io.Closeable;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.concurrent.locks.ReentrantLock;

public class SwiftRNGDevice implements Closeable {

    public static final int RANDOM_BYTES_CHUNK = 16000;

    enum Command {
        MODEL('m', 8),
        VERSION('v', 4),
        SERIAL_NUMBER('s', 15),

        FREQUENCY_TABLE('f', 512),

        RANDOM_BYTES('x', RANDOM_BYTES_CHUNK),
        FIRST_NOISE('<', RANDOM_BYTES_CHUNK),
        SECOND_NOISE('>', RANDOM_BYTES_CHUNK),

        DIAGNOSTICS('d', 0);

        final byte cmd;
        final int responseSize;
        final byte[] bufferedEntropy;
        final int bufferedEntropyPos;

        Command(char cmd, int responseSize) {
            this.cmd = (byte) cmd;
            this.responseSize = responseSize;
            bufferedEntropy = new byte[responseSize];
            bufferedEntropyPos = responseSize;
        }
    }

    private final String path;

    private final RandomAccessFile usbSerialDevice;

    private final String model;

    private final String version;

    private final String serialNumber;

    private final byte[] buffer = new byte[RANDOM_BYTES_CHUNK];
    private int bufferPos = RANDOM_BYTES_CHUNK;

    private final ReentrantLock bufferLock = new ReentrantLock();

    public SwiftRNGDevice(String path) throws IOException {
        this.path = path;
        usbSerialDevice = new RandomAccessFile(path, "rw");
        model = new String(execute(Command.MODEL));
        version = new String(execute(Command.VERSION));
        serialNumber = new String(execute(Command.SERIAL_NUMBER));
    }

    private void execute(Command cmd, byte[] b, int off) throws IOException {
        int p = 0;
        byte status;
        try (FileLock lock = usbSerialDevice.getChannel().lock()) {
            usbSerialDevice.write(cmd.cmd);
            while (p < cmd.responseSize) {
                p += usbSerialDevice.read(b, p + off, cmd.responseSize - p);
            }
            status = usbSerialDevice.readByte();
        }
        if (status != 0) {
            throw new SwiftRNGException("Unexpected status byte: " + status);
        }
    }

    private byte[] execute(Command cmd) throws IOException {
        byte[] response = new byte[cmd.responseSize];
        execute(cmd, response, 0);
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
