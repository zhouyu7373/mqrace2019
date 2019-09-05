package io.openmessaging;

import io.openmessaging.io.QueueIO;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import static io.openmessaging.Constant.*;
import static io.openmessaging.io.QueueIO.*;
import static java.lang.Thread.currentThread;

public class DefaultMessageStoreImpl extends MessageStore {

    private final QueueIO queueIO;

    public static Map<Thread, Integer> map = new ConcurrentHashMap<>(threadNum);

    public static long start;

    @Override
    public void put(Message message) {
        final long t = message.getT();
        if (t_min < 0) {
            // 第一次进入
            synchronized (this) {
                if (t_min < 0) {
                    t_min = t - 16;
                    if (t_min < 0) {
                        t_min = 0;
                    }
                    start =  System.currentTimeMillis();
                }
            }
        }
        try {
            WRITE_QUEUES[currentTimeQueue(t) % ring_queue_length].offer(message);
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
    }

    private int currentTimeQueue(final long t) {
        int tIndex;
        Thread thread = currentThread();
        final int index = (int) ((t - t_min) / WRITE_SEG);
        int pIndex = map.getOrDefault(thread, 0);
        if (index <= pIndex) {
            tIndex = index;
        } else {
            while (pIndex - computeIndex > max_waiting) {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            tIndex = index;
            map.put(thread, index);
        }
        return tIndex;
    }

    @Override
    public List<Message> getMessage(long aMin, long aMax, long tMin, long tMax) {

        try {
            if (unFinish /*&& t_max < tMax*/) {
                synchronized (this) {
                    if (!stopSend) {
                        stopSend = true;
                    }
                }
                latch.await();
            }
            return queueIO.read(aMin, aMax, tMin, tMax);
        } catch (Exception e) {
            System.out.println("tMin " + tMin + " tMax " + tMax);
            e.printStackTrace(System.out);
        }
        return new ArrayList<>();
    }

    @Override
    public long getAvgValue(long aMin, long aMax, long tMin, long tMax) {
        try {
            return queueIO.avg(aMin, aMax, tMin, tMax);
        } catch (Exception e) {
            System.out.println("tMin " + tMin + " tMax " + tMax);
            e.printStackTrace();
        }
        return 0;
    }

    public DefaultMessageStoreImpl() {
        queueIO = new QueueIO(FILE_PRE, "0");
        for (int i = 0; i < WRITE_QUEUES.length; i++) {
            WRITE_QUEUES[i] = new LinkedBlockingQueue<>();
        }

        for (int i = 0; i < max_write; i++) {
            writeBufs.offer(ByteBuffer.allocateDirect(WRITE_SEG * 16 * BYTE_LENGTH));
            aBufs.offer(ByteBuffer.allocateDirect(WRITE_SEG * 16 * 10));
        }

        for (int i = 0; i < INDEX.length; i++) {
            INDEX[i] = new Index();
        }

        queueIO.start();
    }


}
