package ky.korins.swrng;

import java.io.Closeable;
import java.io.IOException;
import java.io.RandomAccessFile;

public class SwiftRNGDevice implements Closeable {

    private final String path;

    private final RandomAccessFile usbSerialDevice;

    private final String model;

    private final String version;

    private final String serialNumber;

    public SwiftRNGDevice(String path) throws IOException {
        this.path = path;
        usbSerialDevice = new RandomAccessFile(path, "rw");
        model = new String(execute(Command.MODEL));
        version = new String(execute(Command.VERSION));
        serialNumber = new String(execute(Command.SERIAL_NUMBER));
    }

    private synchronized byte[] execute(Command cmd) throws IOException {
        usbSerialDevice.write(cmd.cmd);
        int p = 0;
        byte[] response = new byte[cmd.responseSize];
        do {
            p += usbSerialDevice.read(response, p, response.length - p);
        } while (p < response.length);
        byte status = usbSerialDevice.readByte();
        if (status != 0) {
            throw new SwiftRNGException("Unexpected status byte: " + (int)status);
        }
        return response;
    }

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

        Command(char cmd, int responseSize) {
            this.cmd = (byte) cmd;
            this.responseSize = responseSize;
        }
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
