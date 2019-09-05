package io.openmessaging;

import java.nio.ByteBuffer;

public class TestBit {
    public static void main(String[] args) {
        long a = 6588666370661L;
        int shift = 8;
        while (a >>> (shift - 1) * 8 == 0) {
            shift--;
        }
        System.out.println(Long.toBinaryString(1321312));
        System.out.println(shift);

        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(16);
        putLong(byteBuffer, 6588666370661L, shift);
        putLong(byteBuffer, 1321312+1L, shift);
        System.out.println(byteBuffer.position());
        byteBuffer.flip();
        System.out.println(getLong(byteBuffer, shift));
        System.out.println(getLong(byteBuffer, shift));
    }

    public static void putLong(ByteBuffer buffer, long value, int size) {
        while (size-- > 0) {
            buffer.put((byte) (value >> (size * 8) & 0xff));
        }
    }

    public static long getLong(ByteBuffer buffer, int size) {
        long res = 0;
        while (size-- > 0) {
            res |= (buffer.get()&0xff) << size * 8;
        }
        return res;
    }
}
