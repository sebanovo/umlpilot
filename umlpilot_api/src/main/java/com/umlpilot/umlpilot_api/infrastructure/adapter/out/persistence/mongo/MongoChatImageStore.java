package com.umlpilot.umlpilot_api.infrastructure.adapter.out.persistence.mongo;

import com.mongodb.client.gridfs.model.GridFSFile;
import com.umlpilot.umlpilot_api.application.dto.ChatImageData;
import com.umlpilot.umlpilot_api.application.port.out.ChatImageStore;
import org.bson.Document;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Optional;

@Component
public class MongoChatImageStore implements ChatImageStore {

    private final GridFsTemplate gridFsTemplate;

    public MongoChatImageStore(GridFsTemplate gridFsTemplate) {
        this.gridFsTemplate = gridFsTemplate;
    }

    @Override
    public void store(String messageId, byte[] image, String filename, String contentType) {
        delete(messageId);
        Document metadata = new Document("messageId", messageId)
                .append("contentType", contentType == null ? "application/octet-stream" : contentType);
        gridFsTemplate.store(
                new ByteArrayInputStream(image),
                filename == null || filename.isBlank() ? "diagrama.png" : filename,
                contentType == null ? "application/octet-stream" : contentType,
                metadata);
    }

    @Override
    public Optional<ChatImageData> load(String messageId) {
        GridFSFile file = gridFsTemplate.findOne(
                Query.query(Criteria.where("metadata.messageId").is(messageId)));
        if (file == null) return Optional.empty();
        try {
            byte[] bytes = gridFsTemplate.getResource(file).getInputStream().readAllBytes();
            String contentType = "application/octet-stream";
            if (file.getMetadata() != null && file.getMetadata().getString("contentType") != null) {
                contentType = file.getMetadata().getString("contentType");
            }
            return Optional.of(new ChatImageData(bytes, contentType, file.getFilename()));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    private void delete(String messageId) {
        gridFsTemplate.delete(Query.query(Criteria.where("metadata.messageId").is(messageId)));
    }
}
