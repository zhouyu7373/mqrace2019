package io.openmessaging.io;

import io.openmessaging.Index;
import io.openmessaging.Message;
import io.openmessaging.Messages;
import io.openmessaging.util.LongCompress;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

import static io.openmessaging.Constant.*;
import static io.openmessaging.DefaultMessageStoreImpl.map;
import static io.openmessaging.DefaultMessageStoreImpl.start;
import static io.openmessaging.util.LongCompress.size;


public final class QueueIO extends Thread {

    private FileChannel data_channel;

    private FileChannel a_channel;

    private static long dataOffset = 0;

    private static long aOffset = 0;

    private static int msgCount = 0;

    public static Queue<Message>[] WRITE_QUEUES = new Queue[ring_queue_length];
    public static LinkedBlockingQueue<ByteBuffer> writeBufs = new LinkedBlockingQueue();
    public static LinkedBlockingQueue<ByteBuffer> aBufs = new LinkedBlockingQueue();

    public static final Map<Integer, ByteBuffer> cacheMap = new HashMap<>(cacheCount);

    private int dirMemSize = 1024 * 1024 * (1024 + 512);
    private long heapMemSize = 1024 * 1024 * (2048 + 512L+256+128);

    public static final Index[] INDEX = new Index[MSG_COUNT / WRITE_SEG];

    public static volatile long t_min = -1;

    public static volatile int computeIndex = 0;

    public static volatile boolean stopSend = false;

    public static volatile boolean unFinish = true;

    public static final CountDownLatch latch = new CountDownLatch(1);

    private static ThreadPoolExecutor writeAPool = new ThreadPoolExecutor(1, 1,
            0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue());

    private static LinkedBlockingQueue writeDataPoolQueue = new LinkedBlockingQueue();
    private static ThreadPoolExecutor writeDataPool = new ThreadPoolExecutor(1, 1,
            0L, TimeUnit.MILLISECONDS,
            writeDataPoolQueue);


    private ThreadLocal<ByteBuffer> localAbuf = ThreadLocal.withInitial(() -> ByteBuffer.allocateDirect(WRITE_SEG * 4 * 8));
    private ThreadLocal<ByteBuffer> localBodybuf = ThreadLocal.withInitial(() -> ByteBuffer.allocateDirect(WRITE_SEG * 4 * 34));

    private ThreadLocal<Messages> localMessages = ThreadLocal.withInitial(() -> new Messages());

    public QueueIO(String file_prefix, String fileIndex) {
        try {
            data_channel = new RandomAccessFile(file_prefix + fileIndex + ".data", "rw").getChannel();
            a_channel = new RandomAccessFile(file_prefix + fileIndex + ".a", "rw").getChannel();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            Integer[] indexs = new Integer[threadNum];
            for (; ; ) {
                Queue timeQueue;
                if (map.size() >= threadNum) {
                    indexs = map.values().toArray(indexs);
                    Arrays.sort(indexs);
                    int minIndex = indexs[0];
                    int maxIndex = indexs[indexs.length - 1];
                    if (computeIndex < minIndex) {
                        timeQueue = WRITE_QUEUES[computeIndex % ring_queue_length];
                        write(timeQueue);
                        computeIndex++;
                    } else if (stopSend) {
                        // 剩余的队列写入
                        for (; computeIndex <= maxIndex; computeIndex++) {
                            timeQueue = WRITE_QUEUES[computeIndex % ring_queue_length];
                            write(timeQueue);
                        }
                        writeFinish();
                        break;
                    } else {
                        Thread.sleep(1);
                    }
                } else {
                    Thread.sleep(1);
                }

            }
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
    }

    private void writeFinish() {
        writeAPool.shutdown();
        writeDataPool.shutdown();
        latch.countDown();
        System.out.println("write cost" + (System.currentTimeMillis() - start));
        map = null;
        writeBufs = null;
        WRITE_QUEUES = null;
        unFinish = false;
        System.gc();
    }

    private void write(Queue<Message> queue) {
        try {
            ByteBuffer writeBuf = writeBufs.take();
            ByteBuffer aBuf = aBufs.take();
            writeBuf.clear();
            aBuf.clear();
            if (queue.size() == 0) {
                return;
            }
            final int length = queue.size();
            Message[] messages = new Message[length];
            for (int i = 0; i < length; i++) {
                messages[i] = queue.poll();
            }

            Arrays.sort(messages, Comparator.comparingLong(Message::getA));
            long tBase = messages[0].getT();
            long maxA = messages[length - 1].getA();
            int aSize = size(maxA);
            Index index = INDEX[computeIndex];
            index.setBeforeCurCount(msgCount)
                    .setCount(length)
                    .setBaseT(tBase)
                    .setaSize((byte) aSize);

            long[] aMin = new long[length / a_index_step];
            long[] aMax = new long[length / a_index_step];
            int lastIndex = length / a_index_step * a_index_step - 1;
            long[] aTotal = new long[length / a_index_step];
            List lastA = index.getLastA();
            long total = 0;
            for (int i = 0, j = 0, z = 0; i < length; i++, j++) {
                Message m = messages[i];
                long t = m.getT();
                long a = m.getA();
                total += a;
                if (j % a_index_step == a_index_step - 1) {
                    aMin[z / a_index_step] = messages[z].getA();
                    aMax[j / a_index_step] = messages[j].getA();
                    aTotal[z / a_index_step] = total;
                    total = 0;
                    z += a_index_step;
                }
                if (i > lastIndex) {
                    lastA.add(a);
                }
                aBuf.putShort((short) (t - tBase));
                LongCompress.putLong(aBuf, a, aSize);
                writeBuf.put(m.getBody());
            }
            index.setaMin(aMin)
                    .setaMax(aMax)
                    .setaSumArray(aTotal)
                    .setLastA(lastA)
                    .setaOffset(aOffset);

            int dataSize = writeBuf.position();
            int a_Size = aBuf.position();

            long aOff = aOffset;
            long dataOff = dataOffset;
            int idx = computeIndex;
            writeDataPool.submit(() -> {
                try {
                    writeBuf.flip();
                    data_channel.write(writeBuf, dataOff);
                    writeBufs.offer(writeBuf);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            writeAPool.submit(() -> {
                try {
                    if (idx % 2 == 0  && heapMemSize > a_Size) {
                        putCache(aBuf, idx);
                    }
                    aBuf.flip();
                    a_channel.write(aBuf, aOff);
                    aBufs.offer(aBuf);

                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            msgCount += length;
            dataOffset += dataSize;
            aOffset += a_Size;


        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
    }


    public List<Message> read(long aMin, long aMax, long tMin, long tMax) throws IOException {

        Messages result = localMessages.get();
        result.clear();
        int s_seg = (int) ((tMin - t_min) / WRITE_SEG);
        int e_seg = (int) ((tMax - t_min) / WRITE_SEG);

        ByteBuffer aBuf = localAbuf.get();
        ByteBuffer dataBuf = localBodybuf.get();
        for (int i = s_seg; i <= e_seg && i < computeIndex; i++) {
            aBuf.clear();
            dataBuf.clear();
            Index index = INDEX[i];
            long[] mInArray = index.getaMin();
            List<Long> list = index.getLastA();
            int count = index.getCount();
            long baseT = index.getBaseT();
            long[] aIndex = index.getaMax();
            long beforeCount = index.getBeforeCurCount();
            byte aSize = index.getaSize();

            int aStart = 0;
            int aEnd = count;
            for (int z = 0; z < aIndex.length; z++) {
                long a = aIndex[z];
                if (a < aMin) {
                    aStart += a_index_step;
                } else {
                    break;
                }
            }
            if (list.size() > 0 && list.get(0) > aMax) {
                aEnd -= list.size();
                for (int z = aIndex.length - 1; z >= 0; z--) {
                    long a = mInArray[z];
                    if (a > aMax) {
                        aEnd -= a_index_step;
                    } else {
                        break;
                    }
                }
            }
            long start = index.getaOffset() + aStart * (2 + aSize);
            long end = index.getaOffset() + aEnd * (2 + aSize);

            long bodyStart = (beforeCount + aStart) * BYTE_LENGTH;
            long bodyEnd = (beforeCount + aEnd) * BYTE_LENGTH;
            aBuf.limit((int) (end - start));
            a_channel.read(aBuf, start);
            dataBuf.limit((int) (bodyEnd - bodyStart));
            data_channel.read(dataBuf, bodyStart);
            aBuf.flip();
            dataBuf.flip();

            for (; aStart < aEnd; aStart++) {
                long t = baseT + aBuf.getShort();
                if (t <= tMax && t >= tMin) {
                    long a = LongCompress.getLong(aBuf, aSize);
                    if (a >= aMin && a <= aMax) {
                        Message message = result.get();
                        dataBuf.get(message.getBody());
                        message.setA(a);
                        message.setT(t);
                    } else if (a > aMax) {
                        break;
                    } else {
                        dataBuf.position(dataBuf.position() + BYTE_LENGTH);
                    }

                } else {
                    dataBuf.position(dataBuf.position() + BYTE_LENGTH);
                    aBuf.position(aBuf.position() + aSize);
                }
            }
        }
        result.sort(Comparator.comparingLong(Message::getT));
        return result;
    }

    public final long avg(long aMin, long aMax, long tMin, long tMax) throws IOException {

        int s_seg = (int) ((tMin - t_min) / WRITE_SEG);
        int e_seg = (int) ((tMax - t_min) / WRITE_SEG);

        if (e_seg >= computeIndex) {
            e_seg = computeIndex - 1;
        }
        int num = 0;
        long total = 0;
        for (int i = s_seg; i <= e_seg; i++) {
            Index index = INDEX[i];
            int count = index.getCount();
            long baseT = index.getBaseT();
            long[] aIndex = index.getaMax();
            long[] mInArray = index.getaMin();
            long[] aSum = index.getaSumArray();
            long aOff = index.getaOffset();
            byte aSize = index.getaSize();
            List<Long> list = index.getLastA();
            if (i > s_seg && i < e_seg) {
                ByteBuffer cacheBuf = cacheMap.get(i);
                for (int z = 0; z < aIndex.length; z++) {
                    long maxA = aIndex[z];
                    long minA = mInArray[z];
                    long sum = aSum[z];
                    if (maxA < aMin) {
                        continue;
                    }
                    if (minA > aMax) {
                        break;
                    }

                    if (minA >= aMin && maxA <= aMax) {
                        num += a_index_step;
                        total += sum;
                        continue;
                    }
                    if (cacheBuf != null) {
                        for (int k = 0; k < a_index_step; k++) {
                            int idx = (z * a_index_step + k) * (2 + aSize);
                            long a = LongCompress.getLong(cacheBuf, aSize, idx + 2);
                            if (a >= aMin && a <= aMax) {
                                num++;
                                total += a;
                            } else if (a > aMax) {
                                break;
                            }

                        }
                    } else {
                        ByteBuffer buf = localAbuf.get();
                        long start = aOff + z * a_index_step * (2 + aSize);
                        long end = aOff + (z * a_index_step + a_index_step) * (2 + aSize);
                        buf.clear();
                        buf.limit((int) (end - start));
                        a_channel.read(buf, start);
                        buf.flip();
                        while (buf.hasRemaining()) {
                            buf.position(buf.position() + 2);
                            long va = LongCompress.getLong(buf, aSize);
                            if (va >= aMin && va <= aMax) {
                                num++;
                                total += va;
                            } else if (va > aMax) {
                                break;
                            }
                        }
                    }
                }
                if (list.size() > 0 && list.get(0) <= aMax) {
                    for (long va : index.getLastA()) {
                        if (va >= aMin && va <= aMax) {
                            num++;
                            total += va;
                        } else if (va > aMax) {
                            break;
                        }
                    }
                }
                continue;
            }
            int aStart = 0;
            int aEnd = count;
            for (int z = 0; z < aIndex.length; z++) {
                long a = aIndex[z];
                if (a < aMin) {
                    aStart += a_index_step;
                } else {
                    break;
                }
            }
            if (list.size() > 0 && list.get(0) > aMax) {
                aEnd -= list.size();
                for (int z = aIndex.length - 1; z >= 0; z--) {
                    long a = mInArray[z];
                    if (a > aMax) {
                        aEnd -= a_index_step;
                    } else {
                        break;
                    }
                }
            }
            ByteBuffer cacheBuf = cacheMap.get(i);
            if (cacheBuf != null) {
                for (; aStart < aEnd; aStart++) {
                    int idx = aStart * (2 + aSize);
                    long t = baseT + cacheBuf.getShort(idx);
                    if (t <= tMax && t >= tMin) {
                        long a = LongCompress.getLong(cacheBuf, aSize, idx + 2);
                        if (a >= aMin && a <= aMax) {
                            num++;
                            total += a;
                        } else if (a > aMax) {
                            break;
                        }
                    }
                }
            } else {
                ByteBuffer buf = localAbuf.get();
                buf.clear();
                long start = aOff + aStart * (2 + aSize);
                long end = aOff + aEnd * (2 + aSize);
                buf.limit((int) (end - start));
                a_channel.read(buf, start);
                buf.flip();
                while (buf.hasRemaining()) {
                    long t = baseT + buf.getShort();
                    if (t <= tMax && t >= tMin) {
                        long a = LongCompress.getLong(buf, aSize);
                        if (a >= aMin && a <= aMax) {
                            num++;
                            total += a;
                        } else if (a > aMax) {
                            break;
                        }
                    } else {
                        buf.position(buf.position() + aSize);
                    }
                }
            }
        }
        return num == 0 ? 0 : total / num;
    }

    private final void putCache(ByteBuffer buf, int index) {
        int size = buf.position();
        buf.flip();
        if (dirMemSize > size) {
            ByteBuffer cacheBuf = ByteBuffer.allocateDirect(size);
            cacheBuf.put(buf);
            cacheMap.put(index, cacheBuf);
            dirMemSize -= size;
        } else if (heapMemSize > size) {
            ByteBuffer cacheBuf = ByteBuffer.allocate(size);
            cacheBuf.put(buf);
            cacheMap.put(index, cacheBuf);
            heapMemSize -= size;
        } else {
            System.out.println("cache space full");
        }
    }
}