package io.openmessaging;


import sun.security.util.BitArray;

public class TestInt {
    public static void main(String[] args) {

//        long a = 351527 * INDEX_LENGTH * 4L + ((673 - 1) * 4);
//        System.out.println(a);

//        for (int i = 0; i > -129; i--) {
//            System.out.println(Integer.toBinaryString(-1));
//        System.out.println(Byte.SIZE);
        int a = 111;
//        11001111
//        System.out.println(Integer.toBinaryString(207));
//        int bn = 0b01010101;
//        int bn2 = 0b01010101;
//        System.out.println(bn);
//        int[] table = new int[256];
//        for (int i = 0; i < 256; i++) {
//            String s = toBinary(i,8);
//            table[i] = getSubCount_2(s,"1");
//        }
//
//        for (int i = 0; i < table.length; i++) {
//            System.out.println(table[i]);
//        }

        byte tByte = new Byte(String.valueOf(0b01010101));
        System.out.println(tByte);
//        Integer.toBinaryString((tByte & 0xFF) + 0x100).substring(1);

//        }
    }

    //===== read exception, bucket1:351527,INDEX_LENGTH:2000,index1:673,result:-14827512962688


    /**
     * 将一个int数字转换为二进制的字符串形式。
     * @param num 需要转换的int类型数据
     * @param digits 要转换的二进制位数，位数不足则在前面补0
     * @return 二进制的字符串形式
     */
    public static String toBinary(int num, int digits) {
        int value = 1 << digits | num;
        String bs = Integer.toBinaryString(value); //0x20 | 这个是为了保证这个string长度是6位数
        return bs.substring(1);
    }

    public static int getSubCount_2(String str, String key) {
        int count = 0;
        int index = 0;
        while ((index = str.indexOf(key, index)) != -1) {
            index = index + key.length();

            count++;
        }
        return count;
    }
}
