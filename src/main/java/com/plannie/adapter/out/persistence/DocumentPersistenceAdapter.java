package com.plannie.adapter.out.persistence;

import com.pgvector.PGvector;
import com.plannie.adapter.out.persistence.entity.DocumentJpaEntity;
import com.plannie.adapter.out.persistence.repository.DocumentJpaRepository;
import com.plannie.application.port.out.DeleteDocumentPort;
import com.plannie.application.port.out.LoadDocumentPort;
import com.plannie.application.port.out.SaveDocumentPort;
import com.plannie.application.port.out.SearchSimilarChunksPort;
import com.plannie.domain.document.Document;
import com.plannie.domain.document.DocumentChunk;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class DocumentPersistenceAdapter
        implements SaveDocumentPort, LoadDocumentPort, DeleteDocumentPort, SearchSimilarChunksPort {

    private final DocumentJpaRepository documentJpaRepository;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public Document saveDocument(Document document) {
        DocumentJpaEntity saved = documentJpaRepository.save(DocumentJpaEntity.from(document));
        return saved.toDomain();
    }

    @Override
    public void saveChunks(List<DocumentChunk> chunks) {
        String sql = "INSERT INTO document_chunks (document_id, chunk_index, content, embedding) VALUES (?, ?, ?, ?)";
        jdbcTemplate.batchUpdate(sql, chunks, chunks.size(), (ps, chunk) -> {
            ps.setLong(1, chunk.getDocumentId());
            ps.setInt(2, chunk.getChunkIndex());
            ps.setString(3, chunk.getContent());
            ps.setObject(4, new PGvector(chunk.getEmbedding()));
        });
    }

    @Override
    public List<Document> findByUserId(Long userId) {
        return documentJpaRepository.findByUserId(userId).stream()
                .map(DocumentJpaEntity::toDomain)
                .toList();
    }

    @Override
    public Optional<Document> findByIdAndUserId(Long documentId, Long userId) {
        return documentJpaRepository.findByIdAndUserId(documentId, userId)
                .map(DocumentJpaEntity::toDomain);
    }

    @Override
    @Transactional
    public void deleteByIdAndUserId(Long documentId, Long userId) {
        documentJpaRepository.deleteByIdAndUserId(documentId, userId);
    }

    @Override
    public List<String> searchTopK(Long userId, float[] queryEmbedding, int topK) {
        String sql = """
                SELECT dc.content
                FROM document_chunks dc
                JOIN documents d ON d.id = dc.document_id
                WHERE d.user_id = ?
                ORDER BY dc.embedding <=> ?::vector
                LIMIT ?
                """;
        return jdbcTemplate.queryForList(sql, String.class,
                userId, new PGvector(queryEmbedding).toString(), topK);
    }
}
