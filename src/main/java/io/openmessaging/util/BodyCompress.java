package io.openmessaging.util;

import java.nio.ByteBuffer;

import static io.openmessaging.Constant.BYTE_LENGTH;


public class BodyCompress {

    public static void putBody(byte[] src,byte[] base, ByteBuffer buf) {
        int start = buf.position();
        buf.position(start + 4);
        byte[] head = new byte[5];
        for (int i = 0; i < BYTE_LENGTH; i++) {
            if ((src[i]^base[i]) != 0) {
                head[i / 8] |= 1 << i % 8;
            } else {
                buf.put(src[i]);
            }
        }
       for(int i=0;i<5;i++) {
           buf.put(start++,head[i]);
       }
    }

    public static void main(String[] args) {
        byte[] base = new byte[]{0, 0, 1, 108, -52, -118, 112, 81, 0, 0, 5, -2, -53, 30, 126, 125, 0, 0, 0, 91, 51, -94, -67, 62, 0, 0, 5, -2, 11, 16, 10, -75, 17, 4};
       byte[] xor = new byte[]{0, 0, 1, 108, -52, -118, 65, 1, 0, 0, 65, -82, -54, -117, 87, 41, 0, 0, 0, 91, 51, -94, -47, 74, 0, 0, 65, -90, 66, -121, 3, -47, 1, 0};


        ByteBuffer buffer = ByteBuffer.allocate(34);

        //putBody(bytes, buffer, );

        System.out.println(buffer.toString());

    }
}
