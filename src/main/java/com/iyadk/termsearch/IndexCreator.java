package com.iyadk.termsearch;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumMap;
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
	private int numThreads;
	private static enum qKeys {
		LINE, LINEID
	}

	/*
	 * @param corpusPath Path to the corpus text
	 */
	public IndexCreator(String corpus) throws IOException {
		corpusPath = Paths.get(corpus);
		indexPath = Paths.get("./lucene-index");
		analyzer = UniqueAnalyzer.getInstance().analyzer;
		delimeter = ".txt:";
		numThreads=4;
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
	
	public int getNumThreads() {
		return numThreads;
	}

	public void setNumThreads(int numThreads) {
		this.numThreads = numThreads;
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
			private BlockingQueue<EnumMap<qKeys,String>> queue;

			ThreadedIndexWriter(BlockingQueue<EnumMap<qKeys,String>> queue) {
				this.queue = queue;
			}

			@Override
			public void run() {
				while (true) {
					String line;
					String lineId;
					EnumMap<qKeys,String> queueItem;

					try {
						queueItem = queue.take();
						line = queueItem.get(qKeys.LINE);
						lineId = queueItem.get(qKeys.LINEID);
					} catch (InterruptedException e1) {
						break;
					}

					if (line.contentEquals("END OF FILE")) {
						break;
					}

					// Print progress for every 100000 queue items
					if ( (Integer.parseInt(lineId) % 100000) == 0 ) {
						System.out.printf("Processing line %s" + System.lineSeparator(), lineId);
					}

					String splitLine[] = line.split(delimeter, 2);
					if (splitLine.length != 2) {
						System.out.printf("Line %s of corpus is not properly formatted: " + System.lineSeparator()
								+ "\t%s" + System.lineSeparator(), lineId, line.substring(0,Math.min(line.length(), 100)));
						continue;
					}

					// Get the document score from the document title
					int docScore;
					try {
						docScore = parseDocumentScore(splitLine[0]);
					} catch (NumberFormatException | StringIndexOutOfBoundsException e) {
						System.out.printf("Unable to parse score from line %s of corpus: " + System.lineSeparator()
								+ "\t%s" + System.lineSeparator(), lineId, line.substring(0,Math.min(line.length(), 100)));
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

		// Create a queue to receive lines from the file that will be consumed by the threads
		BlockingQueue<EnumMap<qKeys, String>> dataQueue = new ArrayBlockingQueue<>(10000);

		// Spawn the thread pool of consumers for the queue
		ExecutorService threadPool = Executors.newFixedThreadPool(numThreads);
		for(int i=0; i < numThreads; i++) {
			threadPool.execute(new ThreadedIndexWriter(dataQueue));
		}

		// Read the file byte by byte to parse out lines based on \n
		// This ignores \r characters since the corpus can be malformed
		StringBuilder line = new StringBuilder(4096);
		long lineCount = 1;
		int currentChar;
		while((currentChar = bufferedReader.read()) > -1) {
			if (currentChar == '\n') {
				// Create a queue item containing the line and the line count
				EnumMap<qKeys,String> queueItem = new EnumMap<>(qKeys.class);
				queueItem.put(qKeys.LINE, line.toString());
				queueItem.put(qKeys.LINEID, Long.toString(lineCount++));
				try {
					dataQueue.put(queueItem);
				} catch (InterruptedException e) {
					break;
				}

				// Clear the line to prepare to read a new line
				line = new StringBuilder(4096);
				continue;
			}

			line.append((char)currentChar);
		}

		// Close the buffers since we don't need them
	    bufferedReader.close();
	    fileInputStream.close();

	    // Poison the dataQueue to tell threads to stop
	    // There must be one poison pill per thread
		EnumMap<qKeys,String> poisonPill = new EnumMap<>(qKeys.class);
		poisonPill.put(qKeys.LINE, "END OF FILE");
		poisonPill.put(qKeys.LINEID, "");
		try {
			for(int i=0; i < numThreads; i++) {
				dataQueue.put(poisonPill);
			}
		} catch (InterruptedException e) {
			System.out.println("Unable to add poison pills to queue.");
		}

	    // Wait for all thread to terminate
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