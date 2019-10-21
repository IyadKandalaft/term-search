package com.iyadk.termsearch;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;

public class IndexCreator {
	private Path corpusPath;
	private Path indexPath;
	private Directory dirIndex;
	/*
	 * @param corpusPath Path to the corpus text
	 */
	public IndexCreator(String corpus) {
		corpusPath = Paths.get(corpus);
		indexPath = Paths.get("./lucene-index");
	}
	
	public IndexCreator(String corpus, String index) {
		this(corpus);
		indexPath = Paths.get(index);
	}
	
	public void create() throws IOException {
		dirIndex = new MMapDirectory(indexPath);
		StandardAnalyzer analyzer = new StandardAnalyzer();
		IndexWriterConfig writerConfig = new IndexWriterConfig(analyzer);
		
		writerConfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
		
		IndexWriter writer = new IndexWriter(dirIndex, writerConfig);
		
		IndexSchema schema = new IndexSchema();
		
		File f = new File(corpusPath.toString());
		try(Stream<String> lines = Files.lines(f.toPath(), StandardCharsets.UTF_8)){
		    lines.forEach(line -> System.out.println(line));
		} catch (IOException e) {}
		
		writer.addDocument(schema.createDocument("Test", "abc 123"));
		
		writer.close();
		dirIndex.close();
	}
	
	private class IndexSchema{
		private FieldType titleField;
		private FieldType contentField;
		
		public IndexSchema() {
			createSchema();
		}
		
		/*
		 * Creates the field types for the index
		 */
		private void createSchema() {
			titleField = new FieldType();
			titleField.setOmitNorms( true );
			titleField.setIndexOptions( IndexOptions.DOCS );
			titleField.setStored( true );
			titleField.setTokenized( false );
			titleField.freeze();
			
			contentField = new FieldType();
			contentField.setIndexOptions( IndexOptions.DOCS );
			contentField.setStoreTermVectors( true );
			contentField.setStoreTermVectorPositions( true );
			contentField.setTokenized( true );
			contentField.setStored( false );
			contentField.freeze();
		}
		
		/*
		 * Returns a document based on the defined schema to insert into the index
		 * @param title The title of the document
		 * @param content The content of the document
		 */
		public Document createDocument(String title, String content) {
			Document doc = new Document();
			
			doc.add(new Field("title", title, titleField));
			doc.add(new Field("content", content, contentField));
			
			return doc;
		}
	}
}
