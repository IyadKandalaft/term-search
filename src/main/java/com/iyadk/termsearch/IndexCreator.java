package com.iyadk.termsearch;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;

public class IndexCreator {
	private Path corpusPath;
	private Path indexPath;
	private Directory dirIndex;
	Analyzer analyzer;
	/*
	 * @param corpusPath Path to the corpus text
	 */
	public IndexCreator(String corpus) throws IOException {
		corpusPath = Paths.get(corpus);
		indexPath = Paths.get("./lucene-index");
		analyzer = UniqueAnalyzer.getInstance().analyzer;
	}
	
	public IndexCreator(String corpus, String index) throws IOException {
		this(corpus);
		indexPath = Paths.get(index);
	}
	
	public int getDocumentScore(String line) throws NumberFormatException, StringIndexOutOfBoundsException {
		return Integer.parseInt(line.substring(line.length() - 5, line.length() - 4));
	}
	
	public void create() throws FileNotFoundException, IOException{
    	InputStream fileInputStream = new FileInputStream(corpusPath.toFile());
    	BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(fileInputStream), 128 * 2^20);

		dirIndex = new MMapDirectory(indexPath);
		IndexWriterConfig writerConfig = new IndexWriterConfig(analyzer);
		
		writerConfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
		IndexWriter writer = new IndexWriter(dirIndex, writerConfig);

		IndexSchema schema = new IndexSchema();

        long lineNum = 1;
        String line;
        while ((line = bufferedReader.readLine()) != null) {
        	String splitLine[] = line.split(":", 2);
        	// TODO: Add improved checking for title and content parsing 
        	if (splitLine.length != 2) {
        		System.out.printf("Line %d of corpus is not properly formatted: " + System.lineSeparator() + "\t%s" + System.lineSeparator(), lineNum, line);
        		continue;
        	}

        	// Get the document score from the document title
        	int docScore;
    		try {
    			docScore = getDocumentScore(splitLine[0]);
			} catch (NumberFormatException | StringIndexOutOfBoundsException e) {
        		System.out.printf("Unable to parse score from line %d of corpus: " + System.lineSeparator() + "\t%s" + System.lineSeparator(), lineNum, line);
				continue;
			}

    		// Add the document to the index
        	writer.addDocument(schema.createDocument(splitLine[0], splitLine[1], docScore));

        	// Print a brief output every several thousand lines of the corpus on the line number being processed
        	if ( lineNum++ % 100000 == 0 ) {
        		System.out.printf("Processing line %d" + System.lineSeparator(), lineNum);
        	}
        }
        
	    bufferedReader.close();
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
			contentField.setIndexOptions( IndexOptions.DOCS_AND_FREQS_AND_POSITIONS );
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
		public Document createDocument(String title, String content, int score) {
			Document doc = new Document();
			
			doc.add(new Field("title", title, titleField));
			doc.add(new Field("content", content, contentField));
			doc.add(new NumericDocValuesField("score",score));
			
			return doc;
		}
	}
}