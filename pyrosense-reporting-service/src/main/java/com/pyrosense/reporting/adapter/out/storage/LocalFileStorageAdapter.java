package com.pyrosense.reporting.adapter.out.storage;

import com.pyrosense.reporting.application.port.out.FileStoragePort;
import com.pyrosense.reporting.domain.model.ReportFileReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Component
public class LocalFileStorageAdapter implements FileStoragePort {

    private static final Logger log = LoggerFactory.getLogger(LocalFileStorageAdapter.class);

    private final Path storageRoot;

    public LocalFileStorageAdapter(
            @Value("${pyrosense.reporting.storage.local-path:./report-storage}") String localPath) {
        this.storageRoot = Path.of(localPath);
        try {
            Files.createDirectories(storageRoot);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot create report storage directory: " + localPath, e);
        }
    }

    @Override
    public ReportFileReference store(UUID reportId, String fileName, byte[] content) {
        Path reportDir = storageRoot.resolve(reportId.toString());
        try {
            Files.createDirectories(reportDir);
            Path filePath = reportDir.resolve(fileName);
            Files.write(filePath, content);
            log.debug("Stored report file: {} ({} bytes)", filePath, content.length);
            return new ReportFileReference(filePath.toString(), "local", content.length);
        } catch (IOException e) {
            throw new RuntimeException("Failed to store report file: " + fileName, e);
        }
    }

    @Override
    public byte[] retrieve(ReportFileReference reference) {
        try {
            return Files.readAllBytes(Path.of(reference.storagePath()));
        } catch (IOException e) {
            throw new RuntimeException("Failed to retrieve report file: " + reference.storagePath(), e);
        }
    }

    @Override
    public void delete(ReportFileReference reference) {
        try {
            Files.deleteIfExists(Path.of(reference.storagePath()));
            log.debug("Deleted report file: {}", reference.storagePath());
        } catch (IOException e) {
            log.warn("Failed to delete report file: {}", reference.storagePath(), e);
        }
    }
}
