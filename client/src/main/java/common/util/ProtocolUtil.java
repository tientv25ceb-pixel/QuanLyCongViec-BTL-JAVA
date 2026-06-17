package common.util;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ProtocolUtil {
    
    /**
     * Ghi thông điệp với độ dài (4 bytes) đi trước nội dung JSON.
     */
    public static void writeMessage(OutputStream out, String json) throws IOException {
        byte[] bytes = json.getBytes("UTF-8");
        int len = bytes.length;
        
        // Ghi 4 bytes độ dài (Big-Endian)
        out.write((len >>> 24) & 0xFF);
        out.write((len >>> 16) & 0xFF);
        out.write((len >>> 8) & 0xFF);
        out.write((len >>> 0) & 0xFF);
        
        // Ghi nội dung
        out.write(bytes);
        out.flush();
    }

    /**
     * Đọc thông điệp bằng cách đọc 4 bytes độ dài trước, sau đó đọc đúng số byte nội dung.
     */
    public static String readMessage(InputStream in) throws IOException {
        int ch1 = in.read();
        int ch2 = in.read();
        int ch3 = in.read();
        int ch4 = in.read();
        
        if ((ch1 | ch2 | ch3 | ch4) < 0) {
            throw new EOFException("Ket thuc luong (Client ngat ket noi)");
        }
        
        int len = ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4 << 0));
        if (len < 0 || len > 10 * 1024 * 1024) { // Gioi han 10MB tránh OOM
            throw new IOException("Kich thuoc thong diep khong hop le: " + len);
        }
        
        byte[] bytes = new byte[len];
        int read = 0;
        while (read < len) {
            int count = in.read(bytes, read, len - read);
            if (count < 0) {
                throw new EOFException("Ket thuc luong khi dang doc thong diep");
            }
            read += count;
        }
        
        return new String(bytes, "UTF-8");
    }
}
