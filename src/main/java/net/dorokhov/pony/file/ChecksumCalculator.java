package net.dorokhov.pony.file;

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
}
