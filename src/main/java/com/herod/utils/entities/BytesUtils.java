package com.herod.utils.entities;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.apache.commons.codec.digest.DigestUtils;

public class BytesUtils {
    public static final int MAX_UDP_PACKET_SIZE = 65537;
    public static final int NUM_BYTES_IN_SHORT = 2;
    public static final int NUM_BYTES_IN_INT = 4;
    public static final int NUM_BYTES_IN_LONG = 8;
    protected static final char[] hexArray = "0123456789abcdef".toCharArray();
    private static long[] maxValueCache = new long[64];

    public BytesUtils() {
    }

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];

        for(int j = 0; j < bytes.length; ++j) {
            int v = bytes[j] & 255;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 15];
        }

        return new String(hexChars);
    }

    public static byte[] shortToBytes(short s) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(2);
        byteBuffer.putShort(s);
        return byteBuffer.array();
    }

    public static short bytesToShort(byte[] bytes) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        return byteBuffer.getShort();
    }

    public static String shortToHex(short s) {
        return Integer.toHexString(s);
    }

    public static short hexToShort(String s) {
        return Short.parseShort(s, 16);
    }

    public static byte[] hexStringToByteArray(String s) {
        String value = s.trim().replace("-", "").replace(":", "");
        int len = value.length();
        byte[] data = new byte[len / 2];

        for(int i = 0; i < len; i += 2) {
            data[i / 2] = (byte)((Character.digit(value.charAt(i), 16) << 4) + Character.digit(value.charAt(i + 1), 16));
        }

        return data;
    }

    public static byte[] intToBytes(int i) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(4);
        byteBuffer.putInt(i);
        return byteBuffer.array();
    }

    public static int bytesToInt(byte[] bytes) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        return byteBuffer.getInt();
    }

    public static String intToHex(int i) {
        return Integer.toHexString(i);
    }

    public static int hexToInt(String s) {
        return Integer.parseInt(s, 16);
    }

    public static byte[] longToBytes(long l) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(8);
        byteBuffer.putLong(l);
        return byteBuffer.array();
    }

    public static long bytesToLong(byte[] bytes) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        return byteBuffer.getLong();
    }

    public static String longToHex(long l) {
        return Long.toHexString(l);
    }

    public static long hexToLong(String s) {
        return Long.parseLong(s, 16);
    }

    public static String writeBytes(byte[] bytes) {
        StringBuffer stringBuffer = new StringBuffer();

        for(int i = 0; i < bytes.length; ++i) {
            if (i % 4 == 0) {
                stringBuffer.append("\n");
            }

            stringBuffer.append(writeBits(bytes[i]) + " ");
        }

        return stringBuffer.toString();
    }

    public static String writeBytes(byte[] bytes, int packetLength) {
        StringBuffer stringBuffer = new StringBuffer();

        for(int i = 0; i < packetLength; ++i) {
            if (i % 4 == 0) {
                stringBuffer.append("\n");
            }

            stringBuffer.append(writeBits(bytes[i]) + " ");
        }

        return stringBuffer.toString();
    }

    public static String writeBits(byte b) {
        StringBuffer stringBuffer = new StringBuffer();

        for(int i = 7; i >= 0; --i) {
            int bit = b >>> i & 1;
            stringBuffer.append(bit);
        }

        return stringBuffer.toString();
    }

    public static int getMaxIntValueForNumBits(int i) {
        if (i >= 32) {
            throw new RuntimeException("Number of bits exceeds Java int.");
        } else {
            return (int)maxValueCache[i];
        }
    }

    public static long getMaxLongValueForNumBits(int i) {
        if (i >= 64) {
            throw new RuntimeException("Number of bits exceeds Java long.");
        } else {
            return maxValueCache[i];
        }
    }

    public static String md5(byte[] data) throws IOException {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data);
        String md5 = DigestUtils.md5Hex(byteArrayInputStream);
        byteArrayInputStream.close();
        return md5;
    }

    public static byte getBit(byte b, int position) {
        return (byte)(b >> position & 1);
    }

    public static void bytesSum(byte[] linearBytes, byte[] linearPartialBytes) {
        ByteBuffer sum = ByteBuffer.wrap(linearBytes).order(ByteOrder.LITTLE_ENDIAN);
        ByteBuffer sample = ByteBuffer.wrap(linearPartialBytes).order(ByteOrder.LITTLE_ENDIAN);

        for(int index = 0; sum.hasRemaining(); ++index) {
            short short1 = sum.getShort();
            short short2 = sample.getShort();
            int temp = short1 + short2;
            short res;
            if (temp > 32767) {
                res = 32767;
            } else if (temp < -32768) {
                res = -32768;
            } else {
                res = (short)temp;
            }

            linearBytes[index] = (byte)(res & 255);
            ++index;
            linearBytes[index] = (byte)(res >> 8 & 255);
        }

    }

    public static List<byte[]> splitByByteSequence(byte[] src, byte[] sequence) {
        String value = new String(src);
        String separator = new String(sequence);
        String[] values = value.split(separator);
        List<byte[]> result = new ArrayList();
        Stream.of(values).forEach((val) -> {
            byte[] data = val.getBytes();
            result.add(data);
        });
        return result;
    }

    static {
        for(int i = 1; i < 64; ++i) {
            maxValueCache[i] = (1L << i) - 1L;
        }

    }
}
