package com.example.security;

import com.example.ApiApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.system.ApplicationHome;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.security.SecureRandom;
import java.util.*;

/** Resolves an explicit key, or a persistent key for local-only development. */
@Component
public class JwtKeyProvider {
    private static final Logger LOG = LoggerFactory.getLogger(JwtKeyProvider.class);
    private static final Object FILE_LOCK = new Object();
    private static final int MAX_ENCODED_BYTES = 4096;
    private final Environment environment;

    public JwtKeyProvider(Environment environment) { this.environment = environment; }

    public byte[] resolve(String configured) {
        // A malformed explicit key is an error, never a reason to silently rotate it.
        if (configured != null && !configured.isBlank()) return decode(configured);
        String specified = environment.getProperty("security.jwt.key-file", "").trim();
        boolean local = localOnly();
        if (!local && specified.isEmpty()) {
            throw new IllegalStateException("非本地环境必须配置 JWT 密钥：CAMPUS_JWT_SECRET 或 security.jwt.key-file");
        }
        try {
            Path file;
            if (specified.isEmpty()) {
                file = defaultKeyFile(applicationSource());
            } else {
                file = Path.of(specified).toAbsolutePath().normalize();
            }
            if (!local) return read(file); // Production may read an explicit existing file, never create one.
            synchronized (FILE_LOCK) {
                if (Files.exists(file, LinkOption.NOFOLLOW_LINKS)) return read(file);
                Path parent = file.getParent();
                Files.createDirectories(parent);
                try (FileChannel channel = FileChannel.open(parent.resolve("jwt-key.lock"),
                        StandardOpenOption.CREATE, StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS);
                     var ignored = channel.lock()) {
                    // Recheck after the cross-process lock: another start may have just created it.
                    if (Files.exists(file, LinkOption.NOFOLLOW_LINKS)) return read(file);
                    create(file);
                    LOG.info("本地 JWT 密钥已生成并保存：{}；请保留此文件，勿提交 Git", file);
                    return read(file);
                }
            }
        } catch (IOException | InvalidPathException | SecurityException e) {
            throw new IllegalStateException("无法读取或保存 JWT 密钥文件，请检查路径和权限；不会自动覆盖现有密钥");
        }
    }

    private boolean localOnly() {
        String[] profiles = environment.getActiveProfiles();
        if (profiles.length == 0) profiles = environment.getDefaultProfiles();
        return profiles.length == 1 && "local".equals(profiles[0]);
    }

    /** Resolve from application location, not user.dir, so IDEA and Maven share one file. */
    static Path applicationSource() {
        var source = new ApplicationHome(ApiApplication.class).getSource();
        if (source != null) return source.toPath();
        // ApplicationHome may not expose a source when running directly from target/classes.
        var codeSource = JwtKeyProvider.class.getProtectionDomain().getCodeSource();
        if (codeSource != null && "file".equals(codeSource.getLocation().getProtocol())) {
            try { return Path.of(codeSource.getLocation().toURI()); }
            catch (java.net.URISyntaxException e) { /* report the explicit-path option below */ }
        }
        throw new IllegalStateException("无法定位后端目录，请显式配置 security.jwt.key-file");
    }

    static Path defaultKeyFile(Path source) {
        if (source == null) throw new IllegalStateException("无法定位后端目录，请显式配置 security.jwt.key-file");
        source = source.toAbsolutePath().normalize();
        Path start = Files.isDirectory(source) ? source : source.getParent();
        for (Path candidate = start; candidate != null; candidate = candidate.getParent()) {
            if (Files.isRegularFile(candidate.resolve("pom.xml")) && Files.isDirectory(candidate.resolve("src/main"))) {
                return candidate.resolve(".local-secrets/jwt.key");
            }
        }
        if (Files.isRegularFile(source) && source.getFileName().toString().endsWith(".jar")) {
            return source.getParent().resolve(".local-secrets/jwt.key");
        }
        throw new IllegalStateException("无法定位后端目录，请显式配置 security.jwt.key-file");
    }

    public static byte[] decode(String encoded) {
        if (encoded == null || encoded.isBlank() || encoded.length() > MAX_ENCODED_BYTES) {
            throw new IllegalStateException("JWT 密钥必须是独立配置的 Base64 随机密钥");
        }
        byte[] bytes;
        try { bytes = Base64.getDecoder().decode(encoded.strip()); }
        catch (IllegalArgumentException e) { throw new IllegalStateException("JWT 密钥必须是独立配置的 Base64 随机密钥"); }
        if (bytes.length < 64) throw new IllegalStateException("JWT 密钥至少需要 64 字节随机内容");
        return bytes;
    }

    private byte[] read(Path file) throws IOException {
        if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) {
            throw new IllegalStateException("JWT 密钥文件不存在或不是普通文件，请检查 security.jwt.key-file");
        }
        byte[] encoded;
        try (var input = Files.newInputStream(file, LinkOption.NOFOLLOW_LINKS)) { encoded = input.readNBytes(MAX_ENCODED_BYTES + 1); }
        if (encoded.length > MAX_ENCODED_BYTES) throw new IllegalStateException("JWT 密钥文件内容无效；不会自动重新生成");
        byte[] value = decode(new String(encoded, StandardCharsets.UTF_8));
        LOG.info("使用已有 JWT 密钥文件：{}", file);
        return value;
    }

    private void create(Path file) throws IOException {
        Path temporary = Files.createTempFile(file.getParent(), ".jwt-", ".tmp");
        try {
            restrict(temporary); // Restrict before any secret content is written.
            byte[] bytes = new byte[64];
            new SecureRandom().nextBytes(bytes);
            byte[] encoded = (Base64.getEncoder().encodeToString(bytes) + "\n").getBytes(StandardCharsets.US_ASCII);
            try (var output = FileChannel.open(temporary, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
                var buffer = java.nio.ByteBuffer.wrap(encoded);
                while (buffer.hasRemaining()) output.write(buffer);
                output.force(true);
            }
            // Same-directory atomic publication avoids leaving an empty/partial final key on interruption.
            Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE);
        } finally { Files.deleteIfExists(temporary); }
    }

    private void restrict(Path file) throws IOException {
        var posix = Files.getFileAttributeView(file, PosixFileAttributeView.class);
        if (posix != null) { posix.setPermissions(PosixFilePermissions.fromString("rw-------")); return; }
        var acl = Files.getFileAttributeView(file, AclFileAttributeView.class);
        if (acl != null) {
            var owner = Files.getOwner(file);
            acl.setAcl(List.of(AclEntry.newBuilder().setType(AclEntryType.ALLOW).setPrincipal(owner)
                .setPermissions(EnumSet.allOf(AclEntryPermission.class)).build()));
            return;
        }
        throw new IOException("无法设置密钥文件访问权限");
    }
}
