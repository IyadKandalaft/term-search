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
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.DoubleDocValuesField;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
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
	private List<Pattern> scoreOffsetPatterns;
	private List<Float> scoreOffsetValues;
	private ConcurrentHashMap<String, Double> documentIDs;
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
		numThreads=1;
		scoreOffsetPatterns = new LinkedList<>();
		scoreOffsetValues = new LinkedList<>();
		documentIDs = new ConcurrentHashMap<>();
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
	
	public void setOffsetLookup(String offsetLookupFile) throws IOException, FileNotFoundException, PatternSyntaxException, NumberFormatException {
		InputStream fileInputStream = new FileInputStream(Paths.get(offsetLookupFile).toFile());
		InputStreamReader fileInputStreamReader = new InputStreamReader(fileInputStream);
		BufferedReader bufferedReader = new BufferedReader(fileInputStreamReader, 262144);
		
		String line;
		int lineCount = 0;
		while ( (line = bufferedReader.readLine()) != null ){
			lineCount++;
			String[] lineParts = line.split("\t");
			if (lineParts.length != 2 ) {
				System.out.printf("Score offset file line %d could not be parsed: %s", lineCount++, line);
				continue;
			}			
			scoreOffsetPatterns.add(Pattern.compile(lineParts[0]));
			scoreOffsetValues.add(Float.parseFloat(lineParts[1]));
		}

		bufferedReader.close();
		fileInputStream.close();
		fileInputStream.close();
	}

	/**
	 * Retrieves the score to assign to the document
	 * 
	 * This is retrieved from the title using a position. A regex might be
	 * appropriate but can negatively impact performance.
	 * 
	 * @param documentTitle Document title containing the document score to parse
	 */
	public double parseDocumentScore(String documentTitle) throws NumberFormatException, StringIndexOutOfBoundsException {
		return Double.parseDouble(documentTitle.substring(documentTitle.length() - 1, documentTitle.length()));
	}
	
	public void create() throws FileNotFoundException, IOException{
		InputStream fileInputStream = new FileInputStream(corpusPath.toFile());
		InputStreamReader fileInputStreamReader = new InputStreamReader(fileInputStream);
		BufferedReader bufferedReader = new BufferedReader(fileInputStreamReader, 262144);

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
					if ( lineId.endsWith("00000") ) {
						System.out.printf("Processing line %s" + System.lineSeparator(), lineId);
					}

					String splitLine[] = line.split(delimeter, 2);
					if (splitLine.length < 2) {
						System.out.printf("Line %s of corpus is not properly formatted: " + System.lineSeparator()
								+ "\t%s" + System.lineSeparator(), lineId, line.substring(0,Math.min(line.length(), 100)));
						continue;
					}

					String docTitle = splitLine[0];
					String docContent = splitLine[1];
					
					// Get the document score from the document title
					double docScore;
					try {
						docScore = parseDocumentScore(docTitle);
					} catch (NumberFormatException | StringIndexOutOfBoundsException e) {
						System.out.printf("Unable to parse score from line %s of corpus: " + System.lineSeparator()
								+ "\t%s" + System.lineSeparator(), lineId, line.substring(0,Math.min(line.length(), 100)));
						continue;
					}
					
					for(int i = 0; i < scoreOffsetPatterns.size(); i++) {
						if ( scoreOffsetPatterns.get(i).matcher(docTitle).find() ) {
							docScore = (docScore + scoreOffsetValues.get(i));
						}
					}

					documentIDs.putIfAbsent(docTitle, Double.parseDouble(lineId));

					// Add the document to the index
					try {
						writer.addDocument(schema.createDocument(docTitle, docContent, docScore, documentIDs.get(docTitle)));
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
				addItemToQueue(line, lineCount, dataQueue);
				// Clear the line to prepare to read a new line
				line = new StringBuilder(4096);
				lineCount++;
				continue;
			}

			line.append((char)currentChar);
		}
		// Add the last line to the queue because it might not be terminated by \n
		addItemToQueue(line, lineCount, dataQueue);

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

	/**
	 * Convenience method to add a line and its number to a queue
	 * @param line The StringBuilder string to be added to the queue
	 * @param lineNum The number of the line being added 
	 * @param queue The queue to add the item to
	 */
	private void addItemToQueue(StringBuilder line, long lineNum, BlockingQueue<EnumMap<qKeys, String>> queue) {
		EnumMap<qKeys,String> queueItem = new EnumMap<>(qKeys.class);
		queueItem.put(qKeys.LINE, line.toString());
		queueItem.put(qKeys.LINEID, Long.toString(lineNum++));

		try {
			queue.put(queueItem);
		} catch (InterruptedException e) {
			System.out.println("Failed to add an item to the queue");
		}
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
			contentField.setIndexOptions( IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS );
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
		 * @param id The numeric ID to assign the document for later retrieval
		 */
		public Document createDocument(String title, String content, double score, double docId) {
			Document doc = new Document();

			doc.add(new Field("title", title, titleField));
			doc.add(new Field("content", content, contentField));
			doc.add(new DoubleDocValuesField("score", score));
			doc.add(new DoubleDocValuesField("docid", docId));

			return doc;
		}
	}
}