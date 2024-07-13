package net.dorokhov.pony3.core.library.service.file;

import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;

import java.io.*;

@Component
public class ChecksumCalculator {
    
    public String calculate(InputStream stream) throws IOException {
        return DigestUtils.md5DigestAsHex(stream);
    }
    
    public String calculate(File file) throws IOException {
        try (InputStream stream = new FileInputStream(file)) {
            return calculate(stream);
        }
    }
    
    public String calculate(byte[] content) {
        return DigestUtils.md5DigestAsHex(content);
    }
}
