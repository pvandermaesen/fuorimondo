package com.fuorimondo.products;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.Set;
import java.util.UUID;

@Component
public class PhotoStorage {

    private static final Set<String> ALLOWED_MIME = Set.of("image/jpeg", "image/png", "image/webp");
    private static final long MAX_SIZE_BYTES = 5L * 1024 * 1024;

    private final Path rootDir;

    public PhotoStorage(@Value("${fuorimondo.uploads.dir:./uploads}") String uploadsDir) {
        this.rootDir = Paths.get(uploadsDir, "products").toAbsolutePath().normalize();
    }

    @PostConstruct
    void init() throws IOException {
        Files.createDirectories(rootDir);
    }

    public String store(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("Empty file");
        if (file.getSize() > MAX_SIZE_BYTES) throw new IllegalArgumentException("File too large");
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME.contains(contentType.toLowerCase()))
            throw new IllegalArgumentException("Unsupported media type: " + contentType);

        String ext = switch (contentType.toLowerCase()) {
            case "image/jpeg" -> ".jpg";
            case "image/png"  -> ".png";
            case "image/webp" -> ".webp";
            default -> throw new IllegalStateException();
        };
        String filename = UUID.randomUUID() + ext;
        Path target = rootDir.resolve(filename);
        Path temp = Files.createTempFile(rootDir, "upload-", ".tmp");
        try {
            file.transferTo(temp.toFile());
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temp);
        }
        return filename;
    }

    public Resource load(String filename) {
        Path path = rootDir.resolve(filename).normalize();
        if (!path.startsWith(rootDir) || !Files.exists(path))
            throw new IllegalArgumentException("Photo not found: " + filename);
        return new FileSystemResource(path);
    }

    public String contentTypeFor(String filename) {
        int dot = filename.lastIndexOf('.');
        String ext = dot >= 0 ? filename.substring(dot).toLowerCase() : "";
        return switch (ext) {
            case ".jpg", ".jpeg" -> "image/jpeg";
            case ".png"          -> "image/png";
            case ".webp"         -> "image/webp";
            default              -> "application/octet-stream";
        };
    }

    public void delete(String filename) {
        if (filename == null) return;
        Path path = rootDir.resolve(filename).normalize();
        if (!path.startsWith(rootDir)) return;
        try { Files.deleteIfExists(path); } catch (IOException ignored) {}
    }
}
