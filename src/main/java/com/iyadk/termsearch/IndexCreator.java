package com.iyadk.termsearch;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.TieredMergePolicy;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;

public class IndexCreator {
	private Path corpusPath;
	private Path indexPath;
	private Directory dirIndex;
	private String delimeter;
	private Analyzer analyzer;

	/*
	 * @param corpusPath Path to the corpus text
	 */
	public IndexCreator(String corpus) throws IOException {
		corpusPath = Paths.get(corpus);
		indexPath = Paths.get("./lucene-index");
		analyzer = UniqueAnalyzer.getInstance().analyzer;
		delimeter = ".txt:";
	}
	
	public IndexCreator(String corpus, String index) throws IOException {
		this(corpus);
		indexPath = Paths.get(index);
	}

	public String getDelimeter() {
		return delimeter;
	}

	public void setDelimeter(String delimeter) {
		this.delimeter = delimeter;
	}
	
	/*
	 * Retrieves the score to assign to the document
	 * 
	 * This is retrieved from the title using a position. A regex might be
	 * appropriate but can negatively impact performance.
	 * 
	 *  @param documentTitle Document title containing the document score to parse
	 */
	public int parseDocumentScore(String documentTitle) throws NumberFormatException, StringIndexOutOfBoundsException {
		return Integer.parseInt(documentTitle.substring(documentTitle.length() - 1, documentTitle.length()));
	}
	
	public void create() throws FileNotFoundException, IOException{
    	InputStream fileInputStream = new FileInputStream(corpusPath.toFile());
    	BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(fileInputStream), 262144);

		dirIndex = new MMapDirectory(indexPath);

		// Increase segments per tier to improve indexing performance
		TieredMergePolicy mergePolicy = new TieredMergePolicy();
		mergePolicy.setSegmentsPerTier(20);
		
		IndexWriterConfig writerConfig = new IndexWriterConfig(analyzer);
		writerConfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
		// Optimization based lucene documentation
		writerConfig.setUseCompoundFile(false);
		writerConfig.setMergePolicy(mergePolicy);

		IndexWriter writer = new IndexWriter(dirIndex, writerConfig);
		IndexSchema schema = new IndexSchema();

		class ThreadedIndexWriter implements Runnable {
			private BlockingQueue<String> queue;

			ThreadedIndexWriter(BlockingQueue<String> queue) {
				this.queue = queue;
			}

			@Override
			public void run() {
				// Print a brief output every several thousand lines of the corpus on the line number being processed
				//if ( lineNum % 100000 == 0 ) {
				//	System.out.printf("Processing line %d" + System.lineSeparator(), lineNum);
				//}

				String line;

				while (true) {
					try {
						line = queue.take();
					} catch (InterruptedException e1) {
						// TODO Auto-generated catch block
						break;
					}

					if (line.contentEquals("END OF FILE")) {
						break;
					}

					String splitLine[] = line.split(delimeter, 2);
					// TODO: Add improved checking for title and content parsing
					if (splitLine.length != 2) {
						System.out.printf("Line %d of corpus is not properly formatted: " + System.lineSeparator()
								+ "\t%s" + System.lineSeparator(), 1, line);
						continue;
					}

					// Get the document score from the document title
					int docScore;
					try {
						docScore = parseDocumentScore(splitLine[0]);
					} catch (NumberFormatException | StringIndexOutOfBoundsException e) {
						System.out.printf("Unable to parse score from line %d of corpus: " + System.lineSeparator()
								+ "\t%s" + System.lineSeparator(), 1, line);
						continue;
					}

					// Add the document to the index
					try {
						writer.addDocument(schema.createDocument(splitLine[0], splitLine[1], docScore));
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
	        }
		}

		BlockingQueue<String> dataQueue = new ArrayBlockingQueue<String>(100000);
		ExecutorService threadPool = Executors.newFixedThreadPool(5);
		threadPool.execute(new ThreadedIndexWriter(dataQueue));
		threadPool.execute(new ThreadedIndexWriter(dataQueue));
		threadPool.execute(new ThreadedIndexWriter(dataQueue));
		threadPool.execute(new ThreadedIndexWriter(dataQueue));
		threadPool.execute(new ThreadedIndexWriter(dataQueue));

		long lineCount = 1;
		String line;
		while ((line = bufferedReader.readLine()) != null) {
			lineCount++;
			try {
				dataQueue.put(line);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		// Close the buffers since we don't need them
	    bufferedReader.close();
	    fileInputStream.close();

	    // Poison the dataQueue to tell threads to stop
		try {
			dataQueue.put("END OF FILE");
			dataQueue.put("END OF FILE");
			dataQueue.put("END OF FILE");
			dataQueue.put("END OF FILE");
			dataQueue.put("END OF FILE");
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	    // Wait for all thread to termiante
	    threadPool.shutdown();
	    while (!threadPool.isTerminated()) { }

	    // Release the index and memory mapped directory
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