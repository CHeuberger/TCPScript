package cfh.tcpscript;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ 
    ChannelTest.class, 
// TODO    ClientServerTest.class, 
    ServerChannelTest.class, 
    StringHelperTest.class })
public class AllTests {

}
