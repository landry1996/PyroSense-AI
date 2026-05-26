package com.pyrosense.reporting.adapter.out.storage;

import com.pyrosense.reporting.domain.model.ReportFileReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class LocalFileStorageAdapterTest {

    @TempDir
    Path tempDir;

    private LocalFileStorageAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new LocalFileStorageAdapter(tempDir.toString());
    }

    @Test
    void shouldStoreAndRetrieveFile() {
        UUID reportId = UUID.randomUUID();
        byte[] content = "PDF content here".getBytes();

        ReportFileReference ref = adapter.store(reportId, "report.pdf", content);

        assertThat(ref).isNotNull();
        assertThat(ref.storagePath()).contains(reportId.toString());
        assertThat(ref.bucket()).isEqualTo("local");
        assertThat(ref.sizeBytes()).isEqualTo(content.length);

        byte[] retrieved = adapter.retrieve(ref);
        assertThat(retrieved).isEqualTo(content);
    }

    @Test
    void shouldDeleteFile() {
        UUID reportId = UUID.randomUUID();
        byte[] content = "to be deleted".getBytes();

        ReportFileReference ref = adapter.store(reportId, "to-delete.pdf", content);
        adapter.delete(ref);

        assertThatThrownBy(() -> adapter.retrieve(ref))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void shouldStoreMultipleFiles() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        ReportFileReference ref1 = adapter.store(id1, "report1.pdf", "content1".getBytes());
        ReportFileReference ref2 = adapter.store(id2, "report2.pdf", "content2".getBytes());

        assertThat(ref1.storagePath()).isNotEqualTo(ref2.storagePath());
        assertThat(adapter.retrieve(ref1)).isEqualTo("content1".getBytes());
        assertThat(adapter.retrieve(ref2)).isEqualTo("content2".getBytes());
    }
}
