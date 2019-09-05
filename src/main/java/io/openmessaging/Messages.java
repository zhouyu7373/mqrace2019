package io.openmessaging;

import java.util.AbstractList;
import java.util.Arrays;
import java.util.Comparator;

import static io.openmessaging.Constant.BYTE_LENGTH;
import static io.openmessaging.Constant.max_size;

public class Messages extends AbstractList<Message> {
    private Message[] messages = new Message[max_size];

    private int writeIndex = 0;

    public Messages() {
        super();
        for (int i = 0; i < messages.length; i++) {
            messages[i] = new Message(0, 0, new byte[BYTE_LENGTH]);
        }
    }

    @Override
    public void clear() {
        writeIndex = 0;
    }

    public Message get() {
        return messages[writeIndex++];
    }

    @Override
    public boolean add(Message m) {
        messages[writeIndex++] = m;
        return true;
    }

    @Override
    public Message get(int index) {
        return messages[index];
    }

    @Override
    public int size() {
        return writeIndex;
    }
    @Override
    public void sort(Comparator c) {
        Arrays.sort(messages,0, writeIndex, c);
    }

    @Override
    public Message set(int index, Message m) {
        return messages[index];
    }
}
