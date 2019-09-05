package io.openmessaging;

public interface Constant {

    String FILE_PRE = "/alidata1/race2019/data/";

    /**
     * message length 线上 50，34，42 本地 24，8，16
     */
    int BYTE_LENGTH = 34;
    //int BYTE_LENGTH = 8;

    int WRITE_SEG = 9000;

    int max_waiting = 16;

    int max_write = 64;


    /**
     * t的个数
     */
    int MSG_COUNT = 1024000000;

    int threadNum = 12;

    int ring_queue_length = max_waiting * 2;

    int a_index_step = 128;

    //int max_size = 200000;

    int max_size = 80000;

    int cacheCount = MSG_COUNT/WRITE_SEG/4;
}
