package cfh.tcpscript;


/**
 * TODO
 * 
 * @author Carlos Heuberger
 * $Revision: 1.8 $
 */
public class StringHelper {
    
    private StringHelper() {
        throw new AssertionError("static helper class");
    }

    private static final char[] convTable = {
        '\u24EA',  // 0x00
        '\u2460',
        '\u2461',
        '\u2462',
        '\u2463',
        '\u2464',
        '\u2465',
        '\u2466',  // 0x07
        '\u2467',
        '\u2468',
        '\u2469',
        '\u246A',
        '\u246B',
        '\u246C',
        '\u246D',
        '\u246E',
        '\u24AA',  // 0x10
        '\u2474',
        '\u2475',
        '\u2476',
        '\u2477',
        '\u2478',
        '\u2479',
        '\u247A',  // 0x17
        '\u247B',
        '\u247C',
        '\u247D',
        '\u247E',
        '\u247F',
        '\u2480',
        '\u2481',
        '\u2482'  // 0x1F
    };

    public static String toString(byte[] data) {
        if (data == null)
            return null;
        
        char[] conv = new char[data.length];
        for (int i = 0; i < data.length; i++) {
            if ((data[i]&0xFF) < convTable.length)
                conv[i] = convTable[(data[i]&0xFF)];
            else
                conv[i] = (char) (data[i] & 0xFF);
        }
        return new String(conv, 0, conv.length);
    }
    
    public static String toHexString(byte[] data) {
        StringBuilder builder = new StringBuilder();
        builder.append("[");
        boolean first = true;
        for (byte b : data) {
            if (first) {
                first = false;
            } else {
                builder.append(",");
            }
            builder.append(String.format("0x%02x", b));
        }
        builder.append("]");
        return builder.toString();
    }
    
    public static byte[] toByte(String str) {
        if (str == null)
            return null;
        
        String converted = convertSlash(str);
        byte[] buffer = new byte[converted.length()];
        for (int i = 0; i < converted.length(); i++) {
            buffer[i] = (byte) converted.charAt(i);
        }
        return buffer;
    }

    public static String convertSlash(String str) {
        if (str == null)
            return null;
        
        StringBuilder result = new StringBuilder();
        int j = 0;
        for (int i = 0; i < str.length(); i++) {
            assert j <= i : "i: " + i + ", j: " + j;
            char ch = str.charAt(i);
            if (ch == '\\') {
                i++;
                if (i >= str.length()) {
                    throw new IllegalArgumentException(
                            "Missing letter after '\\': " + str.substring(i-1));
                }
                ch = str.charAt(i);
                switch (ch) {
                    case 'b': ch = '\b'; break;
                    case 'f': ch = '\f'; break;
                    case 'n': ch = '\n'; break;
                    case 'r': ch = '\r'; break;
                    case 't': ch = '\t'; break;
                    case '"': break;   // leave as is
                    case '\'': break;  // leave as is 
                    case '\\': break;  // leave as is
                    case '0': case '1': case '2': case '3':
                    case '4': case '5': case '6': case '7':
                        int value = ch - '0';
                        boolean three = ch <= '3';
                        do {
                            if (i+1 >= str.length())
                                break;
                            ch = str.charAt(i+1);
                            if ('0' <= ch && ch <= '7') {
                                i++;
                                value = (value << 3) + ch - '0';
                            } else {
                                break;
                            }
                            three = ! three;
                        } while (! three);
                        ch = (char) value;
                        break;
                    case 'u':
                        i++;
                        if (i+4 > str.length()) {
                            throw new IllegalArgumentException(
                                    "Missing digit for \\uxxxx enconding: "
                                    + str.substring(i-1));
                        }
                        value = 0;
                        for (int k = i; k < i + 4; k++) {
                            ch = str.charAt(k);
                            switch (ch) {
                                case '0': case '1': case '2': case '3': case '4':
                                case '5': case '6': case '7': case '8': case '9':
                                    value = (value << 4) + ch - '0';
                                    break;
                                case 'a': case 'b': case 'c':
                                case 'd': case 'e': case 'f':
                                    value = (value << 4) + 10 + ch - 'a';
                                    break;
                                case 'A': case 'B': case 'C':
                                case 'D': case 'E': case 'F':
                                    value = (value << 4) + 10 + ch - 'A';
                                    break;
                                default:
                                    throw new IllegalArgumentException(
                                            "Malformed \\uxxxx encoding: " 
                                            + str.substring(i-1,i+4));    
                            }
                        }
                        ch = (char) value;
                        i += 3;  // 1 done by for()
                        break;
                    case 'x':
                        i++;
                        if (i+2 > str.length()) {
                            throw new IllegalArgumentException(
                                    "Missing digit for \\xhh enconding: "
                                    + str.substring(i-1));
                        }
                        value = 0;
                        for (int k = i; k < i + 2; k++) {
                            ch = str.charAt(k);
                            switch (ch) {
                                case '0': case '1': case '2': case '3': case '4':
                                case '5': case '6': case '7': case '8': case '9':
                                    value = (value << 4) + ch - '0';
                                    break;
                                case 'a': case 'b': case 'c':
                                case 'd': case 'e': case 'f':
                                    value = (value << 4) + 10 + ch - 'a';
                                    break;
                                case 'A': case 'B': case 'C':
                                case 'D': case 'E': case 'F':
                                    value = (value << 4) + 10 + ch - 'A';
                                    break;
                                default:
                                    throw new IllegalArgumentException(
                                            "Malformed \\xhh encoding: " + str.substring(i-1,i+2));
                            }
                        }
                        ch = (char) value;
                        i += 1;  // 1 done by for()
                        break;
                    case 'd':
                        i++;
                        if (i+3 > str.length()) {
                            throw new IllegalArgumentException(
                                    "Missing digit for \\dxxx enconding: " + str.substring(i-1));
                        }
                        value = 0;
                        int k;
                        boolean done = false;
                        for (k = i; k < i + 3 && !done; k++) {
                            ch = str.charAt(k);
                            switch (ch) {
                                case '0': case '1': case '2': case '3': case '4':
                                case '5': case '6': case '7': case '8': case '9':
                                    value = (value * 10) + ch - '0';
                                    if (value > 255) {
                                        value /= 10;
                                        k -= 1;
                                        done = true;
                                    }
                                    break;
                                default:
                                    if (k == i)
                                        throw new IllegalArgumentException(
                                                "Malformed \\dxxx encoding: " + str.substring(i-1,i+2));
                                    k -= 1;
                                    done = true;
                                    break;
                            }
                        }
                        ch = (char) value;
                        i = k-1;  // 1 done by for()
                        break;
                    default: throw new IllegalArgumentException(
                            "Invalid escape sequence (" +
                            "valid ones are \\b \\t \\n \\f \\r \\\" \\' \\\\ \\uxxxx \\xhh \\dxxx): " +
                            "'\\" + ch + "'");
                }
            }
            result.append(ch);
        }
        return result.toString();
    }
}
