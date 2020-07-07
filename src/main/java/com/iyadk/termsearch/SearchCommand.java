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
@Command(name = "search")
public class SearchCommand implements Callable<Integer> {
	//@ArgGroup(validate=false, heading="General configuration%n")
	//SectionGeneral sectionGeneral;
	
	//static class SectionGeneral{
	@Option(names={"-i", "--index", "--index-path"},
			description="Path to read the index from (default: ${DEFAULT-VALUE})",
			defaultValue = "./lucene-index",
			paramLabel="INDEXPATH")
	private String indexDir;

	@Option(names={"-t", "--terms", "--search-terms-path"},
			description="Path to file containing terms to query (default: ${DEFAULT-VALUE})",
			defaultValue = "./terms.txt",
			paramLabel="FILE")
	private String termsFile;

	@Option(names={"-o", "--output", "--output-file"},
			description="Path to the file where the output will be written (default: ${DEFAULT-VALUE})",
			defaultValue="./output/results.tsv",
			paramLabel="FILE")
	private String outputFile;

	@Option(names={"--threads"},
			description="Number of threads/cores to use for searching (default: ${DEFAULT-VALUE})",
			defaultValue="4",
			type=Integer.class,
			paramLabel="NUM")
	private Integer threads;

	@Option(names = { "-h", "--help" },
			usageHelp = true,
			description = "Displays this message")
	private boolean helpRequested = false;
	
	//}

	//@ArgGroup(validate = false, heading="Search space configuration %n")
	//SectionSearchSpace sectionSearchSpace;
	
	//static class SectionSearchSpace{
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
	//}
	
	
	//@ArgGroup(validate = false, heading="Excerpt length configuration %n")
	//ectionHighlighting sectionHighlighting;
	
	//static class SectionHighlighting{
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
	//}
	
	//@ArgGroup(validate = false, heading="Excerpt scoring configuration %n")
	//SectionScoring sectionScoring;
	//
	//static class SectionScoring {
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

	@Option(names={"-x", "--exclude", "--exclude-words"},
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

	@Option(names={"-s", "--scoring", "--scoring-formula"},
			description="Result scoring formula. (default: ${DEFAULT-VALUE})\n"
					+ "Computed vars: _score (lucene search score), score (parsed doc score), & docMatchCount (# of times doc has matched)\n"
					+ "Functions: abs, cbrt, ceil, cos, exp, floor, log, max, min, pow, random, round, sin, sqrt, tan, trunc",
					defaultValue="_score/score",
					type=String.class
			)
	private String scoring;

	@Option(names={"-e", "--explain", "--explain-scoring"},
			description="Provides an explanation of results by providing the considered excerpts and their score\n",
			defaultValue="false",
			type=Boolean.class
			)
	private Boolean explainScoring;
	//}

	@Override
	public Integer call() throws Exception {
		System.out.println("Executing search command");

		SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

		System.out.println("Start time: " + dateFormatter.format(new Date()));
		System.out.println("-------------------------------------------------------");

		// TODO: Check if index exists and exit gracefully
		SearchIndex indexSearcher = new SearchIndex(termsFile, outputFile, indexDir);


		ExcerptScorer excerptScorer = new ExcerptScorer();
		
		if (scorePunctuation)
			excerptScorer.addCharScoringCriteria(new ExcerptScorerPunctuation())
				.setPenalty(punctuationPenalty);
		if (scoreUppercase)
			excerptScorer.addCharScoringCriteria(new ExcerptScorerUppercase())
				.setPenalty(uppercasePenalty);
		if (scoreDigits)
			excerptScorer.addCharScoringCriteria(new ExcerptScorerDigit())
				.setPenalty(digitPenalty);

		if (scoreFirstCharUppercase)
			excerptScorer.addFullScoringCriteria(new ExcerptScorerFirstChar(firstCharUppercaseStrict))
				.setPenalty(firstCharUppercasePenalty);
		excerptScorer.addFullScoringCriteria(new ExcerptScorerLength(highlightMin, highlightMax, highlightStrict))
			.setPenalty(highlightPenalty);
		if (excludeWordsPath != null) {
			excerptScorer.addFullScoringCriteria(new ExcerptScorerExcludedWords(excludeWordsPath, excludeWordsStrict))
					.setPenalty(excludeWordsPenalty);
		}

		indexSearcher.setExcerptScorer(excerptScorer);
		indexSearcher.setMatchLimit(matchLimit);
		indexSearcher.setResultLimit(resultLimit);
		indexSearcher.setSourceLimit(sourceLimit);
		indexSearcher.setHighlightLimit(highlightMin, highlightMax);
		indexSearcher.setNumThreads(threads);
		indexSearcher.setScoring(scoring);
		indexSearcher.setExplainScoring(explainScoring);
		indexSearcher.searchAll("content");

		indexSearcher.close();

		System.out.println("End time: " + dateFormatter.format(new Date()));

		return 0;
	}

}
