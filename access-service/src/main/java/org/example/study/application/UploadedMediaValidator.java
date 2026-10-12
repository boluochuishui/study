package org.example.study.application;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import javax.imageio.ImageIO;

import org.example.study.domain.ContentType;
import org.springframework.stereotype.Component;

/** 检查实际文件格式，图片先读取宽高而非整图解码，避免像素炸弹。 */
@Component
public class UploadedMediaValidator {
    private final BatchDetectionProperties limits;
    public UploadedMediaValidator(BatchDetectionProperties limits) { this.limits = limits; }

    public void validate(Path path, ContentType type) throws IOException {
        if (type == ContentType.IMAGE) {
            try (var input = ImageIO.createImageInputStream(path.toFile())) {
                if (input == null) { throw new IOException("Unsupported image format"); }
                var readers = ImageIO.getImageReaders(input);
                if (!readers.hasNext()) { throw new IOException("Unsupported image format"); }
                var reader = readers.next();
                try {
                    reader.setInput(input, true, true);
                    int width = reader.getWidth(0);
                    int height = reader.getHeight(0);
                    if (width <= 0 || height <= 0 || (long) width * height > limits.getMaxImagePixels()) {
                        throw new IOException("Image pixel limit exceeded");
                    }
                    // 第一版仅接收静态 PNG/JPEG，避免动画总帧数绕过像素预算。
                    String format = reader.getFormatName();
                    if (!format.equalsIgnoreCase("png") && !format.equalsIgnoreCase("jpeg")) {
                        throw new IOException("Only PNG and JPEG uploads are supported");
                    }
                    reader.read(0);
                } finally { reader.dispose(); }
            }
            return;
        }
        byte[] header;
        try (var input = Files.newInputStream(path)) { header = input.readNBytes(32); }
        String value = new String(header, StandardCharsets.ISO_8859_1);
        boolean mp4 = header.length >= 12 && value.substring(4, 8).equals("ftyp");
        boolean audio = value.startsWith("ID3") || value.startsWith("OggS") || value.startsWith("fLaC")
                || (value.startsWith("RIFF") && header.length >= 12 && value.substring(8, 12).equals("WAVE"))
                || (header.length >= 2 && (header[0] & 255) == 255 && (header[1] & 224) == 224);
        if ((type == ContentType.AUDIO && !(audio || mp4)) || (type == ContentType.VIDEO && !mp4)) {
            throw new IOException("Unsupported media format");
        }
    }
}
