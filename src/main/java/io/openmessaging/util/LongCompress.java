package io.openmessaging.util;

import java.nio.ByteBuffer;

public final class LongCompress {

    public static final void putLong(ByteBuffer buffer, long value, int size) {
        while (size --  > 0) {
            byte b = (byte) ((value >> (size * 8) & 0xff));
            buffer.put(b);
        }
    }

    public static final long getLong(ByteBuffer buffer, int size) {
        long res = 0;
        while (size-- > 0) {
            res |= (long)(buffer.get() & 0xff) << (size * 8);
        }
        return res;
    }

    public static final long getLong(ByteBuffer buffer, int size, int index) {
        long res = 0;
        while (size-- > 0) {
            res |= (long)(buffer.get(index) & 0xff) << (size * 8);
            index++;
        }
        return res;
    }

    public static final int size(long value) {
        int shift = 8;
        while (value >>> (shift - 1) * 8 == 0) {
            shift--;
        }
        return shift;
    }

    public static void main(String[] args) {
        long a = 999999;
        int size = size(a);
        System.out.println(size);
        ByteBuffer buf = ByteBuffer.allocateDirect(20);
        putLong(buf, a ,size);
        buf.flip();
        System.out.println(getLong(buf,size));

    }
}
