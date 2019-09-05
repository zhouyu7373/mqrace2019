package io.openmessaging;

import java.util.List;

public class PutTest {

    private static MessageStore messageStore = new DefaultMessageStoreImpl();


    public static void main(String[] args) {

        System.out.println(System.currentTimeMillis());
        Message msg;

        for (int i = 0; i < 10; i++) {
            msg = new Message(i, i, new byte[8]);
            messageStore.put(msg);
            if (i % 2 == 0) {
                msg = new Message(i, i, new byte[8]);
                messageStore.put(msg);
            }

        }
        System.out.println(System.currentTimeMillis());
        List<Message> list = messageStore.getMessage(999, 1001, 0, 1001);
        System.out.println(list);
        System.out.println(list.size());
        System.out.println(messageStore.getAvgValue(0, 80, 1, 101));

        //ByteBuffer buf = ByteBuffer.allocateDirect(1);

    }
}
