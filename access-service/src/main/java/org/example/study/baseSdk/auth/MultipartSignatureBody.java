package org.example.study.baseSdk.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HexFormat;

/** 按 part 名称排序并签署元数据与文件摘要，避免将整个文件缓存进堆内存。 */
public final class MultipartSignatureBody {
    private MultipartSignatureBody() { }

    public static byte[] canonical(HttpServletRequest request, long maxBytes) throws IOException {
        java.util.Collection<Part> parts;
        try { parts = request.getParts(); }
        catch (jakarta.servlet.ServletException | IllegalStateException exception) {
            throw new AuthenticationException("INVALID_MULTIPART", "Multipart request is invalid or exceeds its limit", 413);
        }
        if (parts.size() > 101) { throw new AuthenticationException("REQUEST_BODY_TOO_LARGE", "Too many multipart fields", 413); }
        var names = new HashSet<String>();
        long total = 0;
        StringBuilder canonical = new StringBuilder("multipart-v1\n");
        for (Part part : parts.stream().sorted(Comparator.comparing(Part::getName)).toList()) {
            if (!part.getName().matches("[a-zA-Z0-9_-]{1,64}") || !names.add(part.getName())) {
                throw new AuthenticationException("INVALID_MULTIPART", "Invalid or duplicated multipart field", 400);
            }
            if ((part.getName().equals("metadata") && part.getSize() > 65536)
                    || (part.getSubmittedFileName() == null && !part.getName().equals("metadata"))) {
                throw new AuthenticationException("INVALID_MULTIPART", "Unexpected or oversized metadata field", 400);
            }
            MessageDigest digest;
            try { digest = MessageDigest.getInstance("SHA-256"); }
            catch (NoSuchAlgorithmException exception) { throw new IllegalStateException("SHA-256 is unavailable", exception); }
            try (var input = part.getInputStream()) {
                byte[] buffer = new byte[8192];
                int count;
                while ((count = input.read(buffer)) != -1) {
                    total += count;
                    if (total > maxBytes) { throw new AuthenticationException("REQUEST_BODY_TOO_LARGE", "Request body is too large", 413); }
                    digest.update(buffer, 0, count);
                }
            }
            canonical.append(encoded(part.getName())).append(':').append(encoded(part.getSubmittedFileName()))
                    .append(':').append(encoded(part.getContentType())).append(':')
                    .append(HexFormat.of().formatHex(digest.digest())).append('\n');
        }
        return canonical.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static String encoded(String value) {
        return Base64.getEncoder().encodeToString((value == null ? "" : value).getBytes(StandardCharsets.UTF_8));
    }
}
