package com.iyadk.termsearch;

import java.io.BufferedReader;
import java.io.CharArrayReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.CharBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;

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
		
		// Read a file using memory mapping techniques to improve throughput
		try (RandomAccessFile corpus = new RandomAccessFile(corpusPath.toFile(), "r")){
            //Get file channel in read-only mode
            FileChannel fileChannel = corpus.getChannel();
            
            long lineNum = 1;
            long readSize = (long)Integer.MAX_VALUE / 2;
            filereadloop:
            for (long filePosition = 0; filePosition < fileChannel.size(); filePosition += readSize) {
	            if ( fileChannel.size() - filePosition < readSize )
	            	readSize = fileChannel.size() - filePosition;
	            	
	            System.out.printf("filePosition: %d \t readSize: %d \t fileSize: %d" + System.lineSeparator(), filePosition, readSize, fileChannel.size());
	            
            	//Get direct byte buffer access using channel.map() operation
	            MappedByteBuffer mappedByteBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, filePosition, readSize);
	            CharsetDecoder decoder = Charset.defaultCharset().newDecoder();
	            CharBuffer charBuffer = decoder.decode(mappedByteBuffer);
	            
	            // BufferedReader to walk through the file line by line
	            BufferedReader bufferedReader = new BufferedReader(new CharArrayReader(charBuffer.array())); 
	            String line;
	            
	            while ((line = bufferedReader.readLine()) != null) {
	            	String splitLine[] = line.split(":", 2);
	            	if (splitLine.length != 2) {
	            		System.out.printf("Line %d of corpus is not properly formatted: \n\t%s\n", lineNum, line);
	            		continue;
	            	}
	            	
	            	// Debuging
	            	// System.out.println(line);
	            	
	            	writer.addDocument(schema.createDocument(splitLine[0], splitLine[1]));
	            	
	            	if ( lineNum++ % 100000 == 0 ) {
	            		System.out.printf("Processing line %d" + System.lineSeparator(), lineNum);
	            		// Debuging
	            		//break filereadloop;
	            	}
	            	
	            }
            }
            
            fileChannel.close();
		} catch (NoSuchFileException e) {
			System.out.printf("The file %s does not exist.", corpusPath.toString());
			writer.deleteAll();
		}
		
		writer.close();
		
		dirIndex.close();
		
		System.out.println("Finished creating index");
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
			contentField.setStored( true );
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
