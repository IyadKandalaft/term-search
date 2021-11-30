package com.iyadk.termsearch;

import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Callable;

import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/*
 * search subcommand that is used to trigger searches of a lucene index based on a set of terms
 */
@Command(
		name = "search",
		usageHelpWidth=140,
		description = "Search the lucene index for a list of terms."
		)
public class SearchCommand implements Callable<Integer> {
	@ArgGroup(
			heading="%nGeneral options:%n",
			order=1,
			validate=false,
			exclusive=false
			)
	GeneralOptions generalOptions = new GeneralOptions();
	
	static class GeneralOptions{
		@Option(names={"-t", "--terms"},
				description="Path to file containing terms to query (default: ${DEFAULT-VALUE})",
				defaultValue="terms.txt",
				paramLabel="FILE")
		private String termsFile;

		@Option(names={"-i", "--index"},
				description="Path to read the index from (default: ${DEFAULT-VALUE})",
				defaultValue = "./lucene-index",
				paramLabel="INDEXPATH")
		private String indexDir;

		@Option(names={"-o", "--output"},
				description="Path to the file where the output will be written (default: ${DEFAULT-VALUE})",
				defaultValue="./output/results.tsv",
				paramLabel="FILE",
				required = false)
		private String outputFile;

		@Option(names={"--threads"},
				description="Threads/cores to use for searching (default: ${DEFAULT-VALUE})",
				defaultValue="4",
				type=Integer.class,
				paramLabel="NUM",
				required = false)
		private Integer threads;

		@Option(names = { "-h", "--help" },
				usageHelp = true,
				description = "Displays this message")
		private boolean helpRequested = false;
	}

	@ArgGroup(
			heading="%nSearch space options:%n%n",
			order=2,
			validate=false,
			exclusive=false
			)
	SearchSpaceOptions searchSpaceOptions = new SearchSpaceOptions();
	
	static class SearchSpaceOptions{
		@Option(names={"-ml", "--match-limit"},
				description="Maximum number of matches to review (default: ${DEFAULT-VALUE})",
				defaultValue="20",
				type=Integer.class,
				paramLabel="NUM")
		private Integer matchLimit;
	
		@Option(names={"-rl", "--result-limit"},
				description="Maximum number of matches to return (default: ${DEFAULT-VALUE})",
				defaultValue="5",
				type=Integer.class,
				paramLabel="NUM")
		private Integer resultLimit;
	
		@Option(names={"-sl", "--source-limit"},
				description="Maximum number of matches to return per source (default: ${DEFAULT-VALUE})",
				defaultValue="130",
				type=Integer.class,
				paramLabel="NUM")
		private Integer sourceLimit;
		
		@Option(names={"-es", "--expand-search"},
				description="If no results are found, repeat the search with an increased match limit\n",
				defaultValue="true",
				type=Boolean.class
				)
		private Boolean expandSearch;

		@Option(names={"-ei", "--expand-iterations"},
				description="If search expansion is enabled, this controls the number of times the search is expanded\n",
				defaultValue="3",
				type=Integer.class
				)
		private Integer expandIterations;

		@Option(names={"-ef", "--expand-factor"},
				description="If search expansion is enabled, this controls the expansion multiplier at each iteration (match limit * expand factor * expand iteration)\n",
				defaultValue="2.0",
				type=Double.class
				)
		private Double expandFactor;
	}

	@ArgGroup(
			heading="%nExcerpt highlighting options:%n%n",
			order=3,
			validate=false,
			exclusive=false
			)
	HighlightingOptions highlightingOptions = new HighlightingOptions();
	
	static class HighlightingOptions{
		@Option(names={"-hm", "--highlight-min"},
				description="Minimum length of an excerpt containing the matched term (default: ${DEFAULT-VALUE})",
				defaultValue="60",
				type=Integer.class,
				paramLabel="NUM")
		private Integer highlightMin;
	
		@Option(names={"-hx", "--highlight-max"},
				description="Maximum length of an excerpt containing a matched term (default: ${DEFAULT-VALUE})",
				defaultValue="180",
				type=Integer.class,
				paramLabel="NUM")
		private Integer highlightMax;
	}
	
	@ArgGroup(
			heading="%nExcerpt scoring options:%n%n",
			order=4,
			validate=false,
			exclusive=false
			)
	ScoringOptions scoringOptions = new ScoringOptions();
	
	static class ScoringOptions {
		@Option(names={"-s", "--scoring-formula"},
				description="Order matches based on a mathematical expression (default: ${DEFAULT-VALUE})\n"
						+ "Computed variables: \n"
						+ "\t_score - Lucene's default search score\n"
						+ "\tscore - Parsed document score from corpus\n"
						+ "\tdocMatchCount - Number of times a document has been selected as a result\n"
						+ "Functions: \n"
						+ "\tabs, cbrt, ceil, cos, exp, floor, log, max, min,\n"
						+ "\tpow, random, round, sin, sqrt, tan, trunc\n",
						defaultValue="_score/score",
						type=String.class,
						paramLabel="EXPR"
				)
		private String scoringFormula;

		@Option(names={"-hp", "--highlight-penalty"},
				description="Penalty to assign excerpts scores that exceed the minimum or maximum length (default: ${DEFAULT-VALUE})",
				defaultValue="100",
				type=Integer.class,
				paramLabel="NUM")
		private Integer highlightPenalty;
		
		@Option(names={"--no-highlight-strict"},
				description="Minimum and maximum excerpt length is strictly enforced (default: ${DEFAULT-VALUE})",
				negatable=true,
				defaultValue="true",
				type=Boolean.class)
		private Boolean highlightStrict;
		
		
		@Option(names={"--score-punctuation"},
				negatable=true,
				description="Score punctuation in excerpts (default: ${DEFAULT-VALUE})",
				defaultValue="true")
		private Boolean scorePunctuation;
		
		@Option(names={"-pp", "--punctuation-penalty"},
				description="Penalty to assign excerpts for each punctuation character (default: ${DEFAULT-VALUE})",
				defaultValue="1",
				type=Integer.class,
				paramLabel="NUM")
		private Integer punctuationPenalty;
		
		@Option(names={"--score-uppercase"},
				negatable=true,
				description="Score uppercase characters in excerpts (default: ${DEFAULT-VALUE})",
				defaultValue="true")
		private Boolean scoreUppercase;
	
		@Option(names={"-up", "--uppercase-penalty"},
				description="Penalty to assign excerpts for each uppercase character (default: ${DEFAULT-VALUE})",
				defaultValue="1",
				type=Integer.class,
				paramLabel="NUM")
		private Integer uppercasePenalty;
	
		@Option(names={"--score-digits"},
				negatable=true,
				description="Score digits in excerpts (default: ${DEFAULT-VALUE})",
				defaultValue="true")
		private Boolean scoreDigits;
	
		@Option(names={"-dp", "--digit-penalty"},
				description="Penalty to assign excerpts for each digit character (default: ${DEFAULT-VALUE})",
				defaultValue="1",
				type=Integer.class,
				paramLabel="NUM")
		private Integer digitPenalty;
	
		@Option(names={"-x", "--exclude-words"},
				description="File of words to exclude words found in the file (one word per line)",
				required=false,
				type=Path.class
				)
		private Path excludeWordsPath;
	
		@Option(names={"--no-exclude-words-strict"},
				description="Excerpts containing excluded words are strictly ignored (default: ${DEFAULT-VALUE})",
				defaultValue="true",
				type=Boolean.class
				)
		private Boolean excludeWordsStrict;
	
		@Option(names={"-xp", "--exclude-words-penalty"},
				description="Penalty assigned to excerpts that contain excluded words (default: ${DEFAULT-VALUE})",
				defaultValue="100",
				type=Integer.class
				)
		private Integer excludeWordsPenalty;
	
		@Option(names={"--score-first-char"},
				negatable=true,
				description="Score first characters in excerpts (default: ${DEFAULT-VALUE})",
				defaultValue="true")
		private Boolean scoreFirstCharUppercase;
	
		@Option(names={"--no-first-char-strict"},
				description="The first character of excerpt must be an uppercase character or the excerpt is strictly ignored (default: ${DEFAULT-VALUE})",
				defaultValue="true",
				type=Boolean.class
				)
		private Boolean firstCharUppercaseStrict;
	
		@Option(names={"-fp", "--first-char-penalty"},
				description="Penalty to assign when the first charcter of an excerpt is not an uppercase (default: ${DEFAULT-VALUE})",
				defaultValue="100",
				type=Integer.class
				)
		private Integer firstCharUppercasePenalty;
		
		@Option(names={"-e", "--explain-scoring"},
				description="Provides an explanation of results by providing the considered excerpts and their score\n",
				defaultValue="false",
				type=Boolean.class
				)
		private Boolean explainScoring;
	}

	@Override
	public Integer call() throws Exception {
		System.out.println("Executing search command");

		SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

		System.out.println("Start time: " + dateFormatter.format(new Date()));
		System.out.println("-------------------------------------------------------");

		// TODO: Check if index exists and exit gracefully
		// Initial the searcher and set general options
		SearchIndex indexSearcher = new SearchIndex(generalOptions.termsFile, generalOptions.outputFile, generalOptions.indexDir);
		indexSearcher.setNumThreads(generalOptions.threads);

		// Configure scoring options
		ExcerptScorer excerptScorer = new ExcerptScorer();
		if (scoringOptions.scorePunctuation) {
			excerptScorer.addCharScoringCriteria(new ExcerptScorerPunctuation())
					.setPenalty(scoringOptions.punctuationPenalty);
		}
		if (scoringOptions.scoreUppercase) {
			excerptScorer.addCharScoringCriteria(new ExcerptScorerUppercase())
					.setPenalty(scoringOptions.uppercasePenalty);
		}
		if (scoringOptions.scoreDigits) {
			excerptScorer.addCharScoringCriteria(new ExcerptScorerDigit()).setPenalty(scoringOptions.digitPenalty);
		}
		if (scoringOptions.scoreFirstCharUppercase) {
			excerptScorer.addFullScoringCriteria(new ExcerptScorerFirstChar(scoringOptions.firstCharUppercaseStrict))
					.setPenalty(scoringOptions.firstCharUppercasePenalty);
		}
		if (scoringOptions.excludeWordsPath != null) {
			excerptScorer.addFullScoringCriteria(
					new ExcerptScorerExcludedWords(scoringOptions.excludeWordsPath, scoringOptions.excludeWordsStrict))
					.setPenalty(scoringOptions.excludeWordsPenalty);
		}
		excerptScorer
				.addFullScoringCriteria(new ExcerptScorerLength(
												highlightingOptions.highlightMin,
												highlightingOptions.highlightMax,
												scoringOptions.highlightStrict))
				.setPenalty(scoringOptions.highlightPenalty);
		indexSearcher.setExcerptScorer(excerptScorer);
		indexSearcher.setScoring(scoringOptions.scoringFormula);
		indexSearcher.setExplainScoring(scoringOptions.explainScoring);

		// Set search space options
		indexSearcher.setExpandSearch(searchSpaceOptions.expandSearch);
		indexSearcher.setExpandIterations(searchSpaceOptions.expandIterations);
		indexSearcher.setExpandFactor(searchSpaceOptions.expandFactor);
		indexSearcher.setMatchLimit(searchSpaceOptions.matchLimit);
		indexSearcher.setResultLimit(searchSpaceOptions.resultLimit);
		indexSearcher.setSourceLimit(searchSpaceOptions.sourceLimit);

		// Set highlighting options
		indexSearcher.setHighlightLimit(highlightingOptions.highlightMin, highlightingOptions.highlightMax);
		
		// Execute a search
		indexSearcher.searchAll("content");

		indexSearcher.close();

		System.out.println("End time: " + dateFormatter.format(new Date()));

		return 0;
	}

}
