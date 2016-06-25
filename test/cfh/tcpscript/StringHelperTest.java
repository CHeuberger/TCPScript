package cfh.tcpscript;

import static org.junit.Assert.*;

import java.nio.ByteBuffer;
import java.util.Arrays;

import org.junit.Test;

public class StringHelperTest {
    
    @Test  // local method
    public void testBytes() {
        assertArrayEquals(new byte[] {}, bytes());
        assertArrayEquals(new byte[] {0,  10, (byte) 129, (byte) 160}, bytes(0, 10, 129, 160));
        
        assertArrayEquals(new byte[] {}, bytes(""));
        assertArrayEquals(new byte[] {0, 1, 0x41, 0x42, 0x43, 0x7E, (byte) 0x85}, bytes("\u0000\u0001ABC~\u0085"));
    }
	/**
	 * @see StringHelper#toString(byte[])
	 */
    @Test
    public void testToString() {
        final String test1 = "testIt ~ ßáÁ";
        assertEquals(test1, StringHelper.toString(bytes(test1)));
        
        final String test2 = "\u0002Another(test)";
        String res2 = StringHelper.toString(bytes(test2));
        assertEquals(test2.substring(1), res2.substring(1));
        assertEquals(true, test2.charAt(0) != res2.charAt(0));
        
        //                    0                   1         
        //                    0123456     7     890     1234
        final String test3 = "012345\u0006\u000789\u00101234";
        String res3 = StringHelper.toString(bytes(test3));
        assertEquals(test3.substring(0, 6), res3.substring(0, 6));
        assertEquals(true, test3.charAt(6) != res3.charAt(6));
        assertEquals(true, test3.charAt(7) != res3.charAt(7));
        assertEquals(test3.substring(8, 10), res3.substring(8, 10));
        assertEquals(true, test3.charAt(10) != res3.charAt(10));
        assertEquals(test3.substring(11, 15), res3.substring(11, 15));
        
        final String test4 = "last\u0011";
        String res4 = StringHelper.toString(bytes(test4));
        assertEquals(test4.substring(0, 4), res4.substring(0, 4));
        assertEquals(true, test4.charAt(4) != res4.charAt(4));
        
        String test5 = "";
        ByteBuffer buffer = ByteBuffer.allocate(32);
        for (byte i = 0; i < 0x20; i++) {
            test5 += (char) i;
            buffer.put(i);
        }
        String res5 = StringHelper.toString(buffer.array());
        for (int i = 0; i < test5.length(); i++) {
            assertEquals(true, res5.charAt(i) != test5.charAt(i));
        }
    }

    @Test
    public void testToByte() {
        assertEquals(null, StringHelper.toByte(null));
        
        String test0 = "";
        assertArrayEquals(new byte[0], StringHelper.toByte(test0));
        
        String test1 = "test IT";
        assertArrayEquals(bytes(test1), StringHelper.toByte(test1));
        
        String test2 = "\\bback\\n";
        assertArrayEquals(bytes("\bback\n"), StringHelper.toByte(test2));
        
        String test3 = "\\b\\f\\n\\r\\t\\\"\\'\\\\";
        assertArrayEquals(bytes("\b\f\n\r\t\"\'\\"), StringHelper.toByte(test3));
        
        String test4 = "\\u0001\\u0012\\u0034\\u00AB";
        assertArrayEquals(bytes(1, 0x12, 0x34, 0xAB), StringHelper.toByte(test4));
        
        String test5 = "\\u00AB\\u20a0\\u21Ef";
        assertArrayEquals(bytes(0xAB, 0xa0, 0xEF), StringHelper.toByte(test5));
        
        String test6 = "\\123\\23\\4\\57\\329";
        assertArrayEquals(bytes(0123, 023, 04, 057, 032, '9'), StringHelper.toByte(test6));
        
        String test7 = "\\x00\\x01\\x10\\xA0\\xFF\\x123";
        assertArrayEquals(bytes(0, 0x01, 0x10, 0xA0, 0xFF, 0x12, '3'), StringHelper.toByte(test7));
        
        String test8 = "\\d000\\d001\\d010\\d020\\d100\\d234\\d255\\d256\\d321";
        byte[] res8 = StringHelper.toByte(test8);
        assertArrayEquals(Arrays.toString(res8), bytes(0, 1, 10, 20, 100, 234, 255, 25, '6', 32, '1'), res8);
        
        String test9 = "\\d0\\d1\\d10\\d20\\d10a\\d23b";
        byte[] res9 = StringHelper.toByte(test9);
        assertArrayEquals(Arrays.toString(res9), bytes(0, 1, 10, 20, 10, 'a', 23, 'b'), res9);
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testToByte_EscapeEnd() {
        String test = "test\\";
        StringHelper.toByte(test);
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testToByte_EscapeInvalid() {
        String test = "invalid\\p test";
        StringHelper.toByte(test);
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testToByte_WrongOctal() {
        String test = "\\90 wring octal";
        StringHelper.toByte(test);
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testToByte_EscapeUShort() {
        String test = "short \\u01";
        StringHelper.toByte(test);
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testToByte_Malformed() {
        String test = "malformed \\u00wrong";
        StringHelper.toByte(test);
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testToByte_EscapeDEmpty() {
        String test = "empty \\d";
        StringHelper.toByte(test);
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testToByte_EscapeDwrong() {
        String test = "wrong \\dnothing";
        StringHelper.toByte(test);
    }

    @Test
    public void testConvertSlash() {
        assertEquals(null, StringHelper.convertSlash(null));
        
        String test0 = "";
        assertEquals(test0, StringHelper.convertSlash(test0));
        
        String test1 = "test IT";
        assertEquals(test1, StringHelper.convertSlash(test1));
        
        String test2 = "\\bback\\n";
        assertEquals("\bback\n", StringHelper.convertSlash(test2));
        
        String test3 = "\\b\\f\\n\\r\\t\\\"\\'\\\\";
        assertEquals("\b\f\n\r\t\"\'\\", StringHelper.convertSlash(test3));
        
        String test4 = "\\u0001\\u0012\\u0123\\u1234";
        assertEquals("\u0001\u0012\u0123\u1234", StringHelper.convertSlash(test4));
        
        String test5 = "\\u00AB\\u20cd\\u21Ef";
        assertEquals("\u00ab\u20cd\u21eF", StringHelper.convertSlash(test5));
        
        String test6 = "\\123\\23\\4\\58\\329";
        assertEquals("\123\23\4\58\329", StringHelper.convertSlash(test6));
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testConvertSlash_EscapeEnd() {
        String test = "test\\";
        StringHelper.convertSlash(test);
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testConvertSlash_EscapeInvalid() {
        String test = "invalid\\p test";
        StringHelper.convertSlash(test);
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testConvertSlash_WrongOctal() {
        String test = "\\90 wrong octal";
        StringHelper.convertSlash(test);
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testConvertSlash_EscapeShort() {
        String test = "short \\u01";
        StringHelper.convertSlash(test);
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testConvertSlash_Malformed() {
        String test = "malformed \\u00wrong";
        StringHelper.convertSlash(test);
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testConvertSlash_EscapeDEmpty() {
        String test = "empty \\d";
        StringHelper.convertSlash(test);
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testConvertSlash_EscapeDwrong() {
        String test = "wrong \\dnothing";
        StringHelper.convertSlash(test);
    }
    
    private byte[] bytes(String text) {
        byte[] result = new byte[text.length()];
        for (int i = 0; i < text.length(); i++) {
            result[i] = (byte) text.charAt(i);
        }
        return result;
    }
    
    private byte[] bytes(int... values) {
        byte[] result = new byte[values.length];
        for (int i = 0; i < values.length; i++) {
            assert 0 <= i && i <= 0xFF : String.format("%1$0x02x (%1$d)", i);
            result[i] = (byte) (values[i] & 0xFF);
        }
        return result;
    }
}
