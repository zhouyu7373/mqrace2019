package io.openmessaging;


import sun.security.util.BitArray;

public class ByteCloneTest {

    public static void main(String[] args) {
        byte[] bytes = new byte[30000];
        System.out.println(System.currentTimeMillis());
        for(int i=0;i<30;i++){
            bytes =  bytes.clone();
        }

        System.out.println(System.currentTimeMillis());
    }
}
