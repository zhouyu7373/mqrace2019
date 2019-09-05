package io.openmessaging;

import java.nio.ByteBuffer;

public class TestAllocate {

    public static void main(String[] args) {
      ByteBuffer byteBuffer = ByteBuffer.allocateDirect(32);
      byteBuffer.putInt(123);
      byteBuffer.flip();
        System.out.println(byteBuffer.getInt());
        byteBuffer.flip();
        System.out.println(byteBuffer.getInt());
    }
}
