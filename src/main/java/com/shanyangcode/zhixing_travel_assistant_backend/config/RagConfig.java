package com.shanyangcode.zhixing_travel_assistant_backend.config;

import com.shanyangcode.zhixing_travel_assistant_backend.rag.RagContentRetriever;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.loader.ClassPathDocumentLoader;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.splitter.DocumentByParagraphSplitter;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;


@Configuration
public class RagConfig {

    @Resource
    private EmbeddingModel embeddingModel; //文本转向量模型

    @Resource
    private EmbeddingStore<TextSegment> embeddingStore;

    @Value("${rag.docs-path}")
    private String docsPath;

    @Bean
    public EmbeddingStoreIngestor embeddingStoreIngestor(RagContentRetriever contentRetriever) {
        List<Document> documents;
        Path asPath = Path.of(docsPath);
        if (Files.isDirectory(asPath)) {
            documents = FileSystemDocumentLoader.loadDocuments(docsPath);
        } else {
            documents = ClassPathDocumentLoader.loadDocumentsRecursively(docsPath);
        }

        DocumentByParagraphSplitter parentSplitter = new DocumentByParagraphSplitter(1000, 200);
        DocumentByParagraphSplitter childSplitter = new DocumentByParagraphSplitter(300, 100);

        DocumentSplitter parentChildSplitter = document -> {
            String fileName = document.metadata().getString(Document.FILE_NAME);
            String source = fileName != null && !fileName.isBlank() ? fileName : "Unknown-Source";

            List<TextSegment> parentSegments = parentSplitter.split(document);
            List<TextSegment> childSegments = new ArrayList<>();

            for (int i = 0; i < parentSegments.size(); i++) {
                String parentId = source + "__parent_" + i;

                Metadata parentMetadata = document.metadata().copy()
                        .put("source", source)
                        .put("parent_id", parentId)
                        .put("chunk_type", "parent");

                String parentText = parentSegments.get(i).text();
                contentRetriever.putParentText(parentId, parentText);
                Document parentDoc = Document.from(parentText, parentMetadata);
                List<TextSegment> splitted = childSplitter.split(parentDoc);

                for (int j = 0; j < splitted.size(); j++) {
                    Metadata childMetadata = splitted.get(j).metadata().copy()
                            .put("source", source)
                            .put("parent_id", parentId)
                            .put("child_id", parentId + "__child_" + j)
                            .put("chunk_type", "child");
                    childSegments.add(TextSegment.from(splitted.get(j).text(), childMetadata));
                }
            }

            return childSegments;
        };

        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                .documentSplitter(parentChildSplitter)
                .textSegmentTransformer(textSegment -> {
                    String fileName = textSegment.metadata().getString(Document.FILE_NAME);
                    String source = fileName != null && !fileName.isBlank()
                            ? fileName
                            : textSegment.metadata().getString("source");
                    String prefix = source != null && !source.isBlank() ? source : "Unknown-Source";
                    return TextSegment.from(prefix + "\n" + textSegment.text(), textSegment.metadata());
                })
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .build();

        ingestor.ingest(documents);
        return ingestor;
    }
}
