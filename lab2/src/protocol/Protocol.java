package protocol;


public final class Protocol {
    public static final byte STATUS_SUCCESS = 0;
    public static final byte STATUS_FAILURE = 1;
    public static final int MAX_FILENAME_BYTES = 4096;
    public static final int BUFFER_SIZE = 8 * 1024;

    private Protocol() { }
}
