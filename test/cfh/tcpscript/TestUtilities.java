package cfh.tcpscript;

public class TestUtilities {
    
    public static void sleep() {
        try {
            Thread.sleep(10);
        } catch (InterruptedException ignored) {
        	Thread.currentThread().interrupt();
        }
    }

    private static int channelName = 0;
    
    public static String getChannelName() {
        return String.format("name%04d", channelName++);
    }
    
    private static int portNumber = 2000;
    
    public static int getPortNumber() {
        if (++portNumber > 65535) {
            portNumber = 1025;
        }
        return portNumber;
    }

    private TestUtilities() {
        throw new RuntimeException("should not be instanciated");
    }
}
