package information.retrieval;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.io.IOException;
import java.util.Scanner;
import edu.smu.tspell.wordnet.*;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;
import edu.stanford.nlp.process.Morphology;

public class HeuristicRules {
	public HeuristicRules() {	}
	Map<String, String> appraisalLexiconWordList = new LinkedHashMap<String, String>();
	Map<String, String> generalInquirerWordList = new LinkedHashMap<String, String>();
	Map<String, String> opinionFinderWordList = new LinkedHashMap<String, String>();
	HashMap<String,String> taggerAbbreviationMapping = new HashMap<String,String>();
	Map<String, ArrayList<String>> polarityInferenceReferenceEngine = new LinkedHashMap<String, ArrayList<String>>();
	public static void main(String[] args)throws Exception
    {
		HeuristicRules heuristicRules = new HeuristicRules();
		System.setProperty("wordnet.database.dir", "C:\\Program Files (x86)\\WordNet\\2.1\\dict");
		heuristicRules.initilizingSentimentalDictionaries();
		heuristicRules.taggerAbbreviationMap();
		heuristicRules.fileprocessing();				
	}
	private BufferedReader extractBufferedReaderForFile(String fileName) throws Exception { return new BufferedReader(new FileReader(new File(fileName))); }
	private Scanner extractCommaDelimitedCurrentLine(String line){ return new Scanner(line).useDelimiter(","); }
	private void dictionaryProcessor(BufferedReader buffer, Map<String, String> lexiconWordList) throws Exception {
		String currentLine;
		while ((currentLine = buffer.readLine()) != null) {
			String parsedKey, polarityOfWord;
			Scanner delimitedCurrentLine = extractCommaDelimitedCurrentLine(currentLine);
			parsedKey = delimitedCurrentLine.next().toString().toLowerCase() + delimitedCurrentLine.next().toString().toLowerCase();
			polarityOfWord = delimitedCurrentLine.next().toString().toLowerCase();
			lexiconWordList.put(parsedKey, polarityOfWord);
			delimitedCurrentLine.close();
		}
	}
	private void initilizingSentimentalDictionaries() throws Exception {
		BufferedReader bufferAL = extractBufferedReaderForFile("AppraisalLexicon.txt");
		dictionaryProcessor(bufferAL, appraisalLexiconWordList);
		bufferAL.close();
		BufferedReader bufferGI = extractBufferedReaderForFile("GeneralInquirer.txt");
		dictionaryProcessor(bufferGI, generalInquirerWordList);
		bufferGI.close();
		BufferedReader bufferOP = extractBufferedReaderForFile("OpinionFinder.txt");
		dictionaryProcessor(bufferOP, opinionFinderWordList);
		bufferOP.close();
	}
	private void taggerAbbreviationMap() {
		taggerAbbreviationMapping.put("JJ", "adjective"); taggerAbbreviationMapping.put("JJR", "adjective"); taggerAbbreviationMapping.put("JJS", "adjective");
		taggerAbbreviationMapping.put("NN", "noun"); taggerAbbreviationMapping.put("NNS", "noun"); taggerAbbreviationMapping.put("NNP", "noun");
		taggerAbbreviationMapping.put("NNPS", "noun"); taggerAbbreviationMapping.put("RB", "adverb"); taggerAbbreviationMapping.put("RBR", "adverb");
		taggerAbbreviationMapping.put("RBS", "adverb"); taggerAbbreviationMapping.put("WRB", "adverb"); taggerAbbreviationMapping.put("VB", "verb");
		taggerAbbreviationMapping.put("VBD", "verb"); taggerAbbreviationMapping.put("VBG", "verb"); taggerAbbreviationMapping.put("VBN", "verb");
		taggerAbbreviationMapping.put("VBP", "verb"); taggerAbbreviationMapping.put("VBZ", "verb");		
	}
	private MaxentTagger taggerObjectInitilization() throws Exception {
		MaxentTagger tagger = new MaxentTagger("models/wsj-0-18-left3words.tagger");
		return tagger;
	}
	public void fileprocessing() throws Exception, ClassNotFoundException, WordNetException
	{
		try {
			String currentLine, word;
			int rule1Matched = 0, rule1UnMatched = 0, rule2Matched = 0, rule2UnMatched = 0, ruleSimAntMatched = 0, ruleSimAntUnMatched = 0;
			int ruleSimSimMatched = 0, ruleSimSimUnMatched = 0, ruleAntAntMatched = 0, ruleAntAntUnMatched = 0; 
			MaxentTagger tagger;
			ArrayList<String> ruleOneSynsetWithPolarity, ruleTwoSynsetWithPolarity, ruleThreeSimToSimToSynsetWithPolarity, ruleThreeSimToAntonymSynsetWithPolarity, ruleThreeAntoAntoSynsetWithPolarity;		
			BufferedReader buffer5 = extractBufferedReaderForFile("polarityInferenceReferenceEngine.txt");
			while ((currentLine = buffer5.readLine()) != null) {
				String parsedKey;
				Scanner delimitedCurrentLine = extractCommaDelimitedCurrentLine(currentLine);
				parsedKey = delimitedCurrentLine.next().toString().toLowerCase() + delimitedCurrentLine.next().toString().toLowerCase();
				ArrayList<String> polarityAndSynsetSense = new ArrayList<String>();
				while(delimitedCurrentLine.hasNext()) {
					polarityAndSynsetSense.add(delimitedCurrentLine.next().toString());
				}
				polarityInferenceReferenceEngine.put(parsedKey, polarityAndSynsetSense);
				delimitedCurrentLine.close();
			}
			buffer5.close();
			WordNetDatabase database = WordNetDatabase.getFileInstance();
			tagger = taggerObjectInitilization();
			BufferedReader buffer4 = extractBufferedReaderForFile("WordNet.csv");
			FileWriter fstream = new FileWriter("outputFile.txt");
			BufferedWriter fileOut = new BufferedWriter(fstream);
			FileWriter reportStream = new FileWriter("report.txt");
			BufferedWriter reportOut = new BufferedWriter(reportStream);
			while ((word = buffer4.readLine()) != null) {
				ruleOneSynsetWithPolarity = ruleOneHeuristics(tagger, database, word, fileOut, reportOut);
				ruleTwoSynsetWithPolarity = ruleTwoHeuristics(tagger, database, word, fileOut, reportOut);
				ruleThreeSimToAntonymSynsetWithPolarity = ruleThreeSimToAntonymHeruristics(tagger, database, word, fileOut, reportOut);
				ruleThreeSimToSimToSynsetWithPolarity = ruleThreeSimToSimToHeruristics(tagger, database, word, fileOut, reportOut);
				ruleThreeAntoAntoSynsetWithPolarity	= ruleThreeAntoAntoHeruristics(tagger, database, word, fileOut, reportOut);
				fileOut.write("\n" + word + ", ");
				if(ruleOneSynsetWithPolarity != null) {
					rule1Matched++;
					fileOut.write(ruleOneSynsetWithPolarity.get(0) + ", " + ruleOneSynsetWithPolarity.get(1) + ", ");
				}
				else { 
					rule1UnMatched++;
					fileOut.write("Rule 1, Inapplicable, ");
				}
				if(ruleTwoSynsetWithPolarity != null) {
					rule2Matched++;
					fileOut.write(ruleTwoSynsetWithPolarity.get(0) + ", " + ruleTwoSynsetWithPolarity.get(1) + ", ");
				}
				else { 
					rule2UnMatched++;
					fileOut.write("Rule 2, Inapplicable, ");
				}
				if(ruleThreeSimToAntonymSynsetWithPolarity != null) {
					ruleSimAntMatched++;
					fileOut.write(ruleThreeSimToAntonymSynsetWithPolarity.get(0) + ", " + ruleThreeSimToAntonymSynsetWithPolarity.get(1) + ", ");
				}
				else {
					ruleSimAntUnMatched++;
					fileOut.write("Rule 3.1, Inapplicable, ");
				}
				if(ruleThreeSimToSimToSynsetWithPolarity != null) {
					ruleSimSimMatched++;
					fileOut.write(ruleThreeSimToSimToSynsetWithPolarity.get(0) + ", " + ruleThreeSimToSimToSynsetWithPolarity.get(1) + ", ");
				}
				else {
					ruleSimSimUnMatched++;
					fileOut.write("Rule 3.2, Inapplicable, ");
				}
				if(ruleThreeAntoAntoSynsetWithPolarity != null) {
					ruleAntAntMatched++;
					fileOut.write(ruleThreeAntoAntoSynsetWithPolarity.get(0) + ", " + ruleThreeAntoAntoSynsetWithPolarity.get(1));
				}
				else {
					ruleAntAntUnMatched++;
					fileOut.write("Rule 3.3, Inapplicable");	
				}
			}
			buffer4.close();
			fileOut.close();
			reportOut.close();
			System.out.println("\nRule1 Statistics:");
			System.out.println("Total Synsets Tested: " + (rule1Matched + rule1UnMatched));
			System.out.println("Matched Cases: " + rule1Matched);
			System.out.println("Unmatched Cases: " + rule1UnMatched);
			System.out.println("\nRule2 Statistics:");
			System.out.println("Total Synsets Tested: " + (rule2Matched + rule2UnMatched));
			System.out.println("Matched Cases: " + rule2Matched);
			System.out.println("Unmatched Cases: " + rule2UnMatched);
			System.out.println("\nRule3 - SimilarTo Antonym Statistics:");
			System.out.println("Total Synsets Tested: " + (ruleSimAntMatched + ruleSimAntUnMatched));
			System.out.println("Matched Cases: " + ruleSimAntMatched);
			System.out.println("Unmatched Cases: " + ruleSimAntUnMatched);
			System.out.println("\nRule3 - SimilarTo SimilarTo Statistics:");
			System.out.println("Total Synsets Tested: " + (ruleSimSimMatched + ruleSimSimUnMatched));
			System.out.println("Matched Cases: " + ruleSimSimMatched);
			System.out.println("Unmatched Cases: " + ruleSimSimUnMatched);
			System.out.println("\nRule3 - Antonym Antonym Statistics:");
			System.out.println("Total Synsets Tested: " + (ruleAntAntMatched + ruleAntAntUnMatched));
			System.out.println("Matched Cases: " + ruleAntAntMatched);
			System.out.println("Unmatched Cases: " + ruleAntAntUnMatched);
		} 
		catch(IOException e)
		{
			System.err.println("Error: " + e);
		}
	}
	private ArrayList<String> ruleOneHeuristics(MaxentTagger tagger, WordNetDatabase database, String word, BufferedWriter fileOut, BufferedWriter reportOut) throws Exception {
		Synset[] synsets = database.getSynsets(word.trim(), SynsetType.NOUN);
		ArrayList<String> synsetsWithPolarity = null;
		String hypernymKey, hyponymKey, hypernymPolarity, hyponymPolarity, synsetPolarity, synsetDefinitionPolarity;
		NounSynset synset, hypernymSynset, hyponymSynset;;
		if(synsets.length > 0) {
			for(int numberOfSenses = 0; numberOfSenses < synsets.length; numberOfSenses++) {
				synset = (NounSynset)synsets[numberOfSenses];				
				if(synset.getHypernyms().length > 0 && synset.getHyponyms().length > 0) {
					NounSynset[] hypernyms = synset.getHypernyms();				
					NounSynset[] hyponyms = synset.getHyponyms();
					for(int numberOfHypernyms = 0; numberOfHypernyms < hypernyms.length; numberOfHypernyms++) {
						hypernymSynset = (NounSynset)(hypernyms[numberOfHypernyms]);
						for(int numberOfHyponyms = 0; numberOfHyponyms < hyponyms.length; numberOfHyponyms++) {
							hyponymSynset = (NounSynset)(hyponyms[numberOfHyponyms]);
							hypernymKey = hypernymSynset.getWordForms()[0].trim() + "noun";
							hyponymKey = hyponymSynset.getWordForms()[0].trim() + "noun";
							if(polarityInferenceReferenceEngine.containsKey(hypernymKey) && polarityInferenceReferenceEngine.containsKey(hyponymKey) ) {
								int pirsHypernymSenseNumber, pirsHyponymSenseNumber;
								String hypernymHeadWord, hyponymHeadWord;
								pirsHypernymSenseNumber = Integer.parseInt(polarityInferenceReferenceEngine.get(hypernymKey).get(1));
								pirsHyponymSenseNumber = Integer.parseInt(polarityInferenceReferenceEngine.get(hyponymKey).get(1));
								hypernymHeadWord = hypernymSynset.getWordForms()[0].trim();
								hyponymHeadWord = hyponymSynset.getWordForms()[0].trim();
								Synset[] hypernymHeadWordSynsets = database.getSynsets(hypernymHeadWord, SynsetType.NOUN);
								Synset[] hyponymHeadWordSynsets = database.getSynsets(hyponymHeadWord, SynsetType.NOUN);
								NounSynset nounSynsetOfHypernymHeadWord = (NounSynset) hypernymHeadWordSynsets[pirsHypernymSenseNumber-1];
								NounSynset nounSynsetOfHyponymHeadWord = (NounSynset) hyponymHeadWordSynsets[pirsHyponymSenseNumber-1];
								if((hypernymSynset.getDefinition() == nounSynsetOfHypernymHeadWord.getDefinition()) &&
										(hyponymSynset.getDefinition() == nounSynsetOfHyponymHeadWord.getDefinition())) {	
									hypernymPolarity = polarityInferenceReferenceEngine.get(hypernymKey).get(0);
									hyponymPolarity = polarityInferenceReferenceEngine.get(hyponymKey).get(0);
									String sentence = synset.getDefinition();
									synsetDefinitionPolarity = SynsetDefinitionPolarityAnalyzer(tagger, word, sentence, fileOut);
									if(hypernymPolarity.equals(hyponymPolarity)) {
										if(synsetDefinitionPolarity.equals("neutral") || hypernymPolarity.equals(synsetDefinitionPolarity))
										{
											synsetPolarity = hypernymPolarity.toString().toLowerCase();
											reportOut.write("\nRule 1");
											reportOut.write("\nSynset: " + synsetsWithPolarityExtract(synset, null).get(0));
											reportOut.write("\nParts Of Speech: Noun");
											reportOut.write("\nSynset Definition: " + synset.getDefinition());
											reportOut.write("\nHypernym Synset: " + synsetsWithPolarityExtract(hypernymSynset, null).get(0));
											reportOut.write("\nHypernym Synset Definition: " + hypernymSynset.getDefinition());
											reportOut.write("\nHyponym Synset: " + synsetsWithPolarityExtract(hyponymSynset, null).get(0));
											reportOut.write("\nHyponym Synset Definition: " + hyponymSynset.getDefinition());
											reportOut.write("\nSynset Polarity(Program): " + synsetPolarity);
											reportOut.write("\nHypernym Polarity(Manual): ");
											reportOut.write("\nHyponym Polarity(Manual): ");
											reportOut.write("\nSynset Definition Polarity(Manual): ");
											reportOut.write("\nSynset Polarity(Manual): ");
											reportOut.write("\nInference: ");
											reportOut.write("\n");										
											return (synsetsWithPolarityExtract(synset, synsetPolarity));
										}
									}
								}
							}
						}
					}
				}
			}
		}
		return synsetsWithPolarity;
	}	
	private ArrayList<String> synsetsWithPolarityExtract(Synset synset, String synsetPolarity){
		ArrayList<String> synsetsWithPolarity = new ArrayList<String>();
		String synsetWords;
		synsetWords = "[ ";
		for(int numberOfSynsetWords = 0; numberOfSynsetWords <synset.getWordForms().length; numberOfSynsetWords++ )
			synsetWords = synsetWords + synset.getWordForms()[numberOfSynsetWords] +" ";
		synsetWords = synsetWords + "]";
		synsetsWithPolarity.add(synsetWords);
		synsetsWithPolarity.add(synsetPolarity);
		return synsetsWithPolarity;		
	}
	private ArrayList<String> ruleTwoHeuristics(MaxentTagger tagger, WordNetDatabase database, String word, BufferedWriter fileOut, BufferedWriter reportOut) throws Exception {
		Synset[] synsets = database.getSynsets(word.trim(), SynsetType.NOUN);
		ArrayList<String> synsetsWithPolarity = null;
		NounSynset nounSynset; 
		String synsetPolarity, synsetDefinitionPolarity;
		if(synsets.length > 0) {
			nounSynset = (NounSynset)(synsets[0]);
			if(nounSynset.getInstanceHypernyms().length > 0 || nounSynset.getInstanceHyponyms().length > 0 || 
					nounSynset.getPartHolonyms().length > 0 || nounSynset.getPartMeronyms().length > 0 ||
					nounSynset.getMemberHolonyms().length > 0 || nounSynset.getMemberMeronyms().length > 0 || 
					nounSynset.getSubstanceHolonyms().length > 0 || nounSynset.getSubstanceMeronyms().length > 0) {
				synsetPolarity = "neutral";
				String sentence = nounSynset.getDefinition();
				synsetDefinitionPolarity = SynsetDefinitionPolarityAnalyzer(tagger, word, sentence, fileOut);
				if(synsetPolarity.equals(synsetDefinitionPolarity)) {
					reportOut.write("\nRule 2");
					reportOut.write("\nSynset: " + synsetsWithPolarityExtract(nounSynset, null).get(0));
					reportOut.write("\nParts Of Speech: Noun");
					reportOut.write("\nSynset Definition: " + nounSynset.getDefinition());
					reportOut.write("\nMeronym Synset: ");
					reportOut.write("\nSynset Polarity(Program): " + synsetPolarity);
					reportOut.write("\nSynset Definition Polarity(Manual): ");
					reportOut.write("\nSynset Polarity(Manual): ");
					reportOut.write("\nInference: ");
					reportOut.write("\n");		
					return (synsetsWithPolarityExtract(nounSynset, synsetPolarity));
				}
			} 
		}
		return synsetsWithPolarity;
	}	
	private ArrayList<String> ruleThreeSimToSimToHeruristics(MaxentTagger tagger, WordNetDatabase database, String word, BufferedWriter fileOut, BufferedWriter reportOut) throws Exception {
		Synset[] adjectiveSynsets = database.getSynsets(word.trim(), SynsetType.ADJECTIVE);
		ArrayList<String> synsetsWithPolarity = null;
		AdjectiveSynset[] adjectiveSimilarToSynsets;
		AdjectiveSynset adjectiveSynset, firstAdjSimilarToSynset, secondAdjSimilarToSynset;
		String firstSimilarToSynsetPolarity, secondSimilarToSynsetPolarity, synsetPolarity, synsetDefinitionPolarity, firstSimilarToKey, secondSimilarToKey;
		if(adjectiveSynsets.length > 0) {
			for(int numberOfSenses = 0; numberOfSenses < adjectiveSynsets.length; numberOfSenses++) {
				adjectiveSynset = (AdjectiveSynset)(adjectiveSynsets[numberOfSenses]);
				adjectiveSimilarToSynsets = adjectiveSynset.getSimilar();
				for(int numberOfSimilarToSynsets = 0; numberOfSimilarToSynsets < adjectiveSimilarToSynsets.length; numberOfSimilarToSynsets++) {
					firstAdjSimilarToSynset = (AdjectiveSynset)(adjectiveSimilarToSynsets[numberOfSimilarToSynsets]);
					firstSimilarToKey = firstAdjSimilarToSynset.getWordForms()[0].trim() + "adjective";
					if(polarityInferenceReferenceEngine.containsKey(firstSimilarToKey)) {
						for(int restOfSimilarToSynsets = numberOfSimilarToSynsets + 1 ; restOfSimilarToSynsets < adjectiveSimilarToSynsets.length; restOfSimilarToSynsets++) {
							secondAdjSimilarToSynset = (AdjectiveSynset)(adjectiveSimilarToSynsets[restOfSimilarToSynsets]);
							secondSimilarToKey = secondAdjSimilarToSynset.getWordForms()[0].trim() + "adjective";
							if(polarityInferenceReferenceEngine.containsKey(secondSimilarToKey)) {
								firstSimilarToSynsetPolarity = polarityInferenceReferenceEngine.get(firstSimilarToKey).get(0);
								secondSimilarToSynsetPolarity = polarityInferenceReferenceEngine.get(secondSimilarToKey).get(0);
								String sentence = adjectiveSynset.getDefinition();
								synsetDefinitionPolarity = SynsetDefinitionPolarityAnalyzer(tagger, word, sentence, fileOut);
								if(firstSimilarToSynsetPolarity.equals(secondSimilarToSynsetPolarity)) {
									if(synsetDefinitionPolarity.equals("neutral") || firstSimilarToSynsetPolarity.equals(synsetDefinitionPolarity)) {
										synsetPolarity = firstSimilarToSynsetPolarity.toString().toLowerCase();
										reportOut.write("\nRule 3.2");
										reportOut.write("\nSynset: " + synsetsWithPolarityExtract(adjectiveSynset, null).get(0));
										reportOut.write("\nParts Of Speech: Adjective");
										reportOut.write("\nSynset Definition: " + adjectiveSynset.getDefinition());
										reportOut.write("\nSimilar-To Synset: " + synsetsWithPolarityExtract(firstAdjSimilarToSynset, null).get(0));
										reportOut.write("\nSimilar-To Synset Definition: " + firstAdjSimilarToSynset.getDefinition());
										reportOut.write("\nSimilar-To Synset: " + synsetsWithPolarityExtract(secondAdjSimilarToSynset, null).get(0));
										reportOut.write("\nSimilar-To Synset Definition: " + secondAdjSimilarToSynset.getDefinition());
										reportOut.write("\nSynset Polarity(Program): " + synsetPolarity);
										reportOut.write("\nSimilar-To Polarity(Manual): ");
										reportOut.write("\nSimilar-To Polarity(Manual): ");
										reportOut.write("\nSynset Definition Polarity(Manual): ");
										reportOut.write("\nSynset Polarity(Manual): ");
										reportOut.write("\nInference: ");
										reportOut.write("\n");							
										return (synsetsWithPolarityExtract(adjectiveSynset, synsetPolarity));
									}
								}
							}
						}
					}
				}
			}
		}
		return synsetsWithPolarity;
	}
	private ArrayList<String> ruleThreeSimToAntonymHeruristics(MaxentTagger tagger, WordNetDatabase database, String word, BufferedWriter fileOut, BufferedWriter reportOut) throws Exception {
		Synset[] adjectiveSynsets = database.getSynsets(word.trim(), SynsetType.ADJECTIVE);
		ArrayList<String> synsetsWithPolarity = null;
		AdjectiveSynset[] adjectiveSimilarToSynsets;
		WordSense[] adjectiveAntonymSynsets;
		AdjectiveSynset adjectiveSynset, adjSimilarToSynset, adjAntonymSynset;
		String similarToSynsetPolarity, antonymSynsetPolarity, synsetPolarity, synsetDefinitionPolarity, similarToKey, antonymKey;
		if(adjectiveSynsets.length > 0) {
			for(int numberOfSenses = 0; numberOfSenses < adjectiveSynsets.length; numberOfSenses++) {
				adjectiveSynset = (AdjectiveSynset)(adjectiveSynsets[numberOfSenses]);
				adjectiveSimilarToSynsets = adjectiveSynset.getSimilar();
				for(int numberOfSimilarToSynsets = 0; numberOfSimilarToSynsets < adjectiveSimilarToSynsets.length; numberOfSimilarToSynsets++) {
					adjSimilarToSynset = (AdjectiveSynset)(adjectiveSimilarToSynsets[numberOfSimilarToSynsets]);
					similarToKey = adjSimilarToSynset.getWordForms()[0].trim() + "adjective";
					if(polarityInferenceReferenceEngine.containsKey(similarToKey)) {
						adjectiveAntonymSynsets = adjectiveSynset.getAntonyms(word);
						for(int numberofAntonymSynsets = 0; numberofAntonymSynsets < adjectiveAntonymSynsets.length; numberofAntonymSynsets++) {
							adjAntonymSynset = (AdjectiveSynset) adjectiveAntonymSynsets[numberofAntonymSynsets].getSynset();
							antonymKey = adjAntonymSynset.getWordForms()[0].trim() + "adjective";
							if(polarityInferenceReferenceEngine.containsKey(antonymKey)) {
								similarToSynsetPolarity = polarityInferenceReferenceEngine.get(similarToKey).get(0);
								antonymSynsetPolarity = polarityInferenceReferenceEngine.get(antonymKey).get(0);
								if(antonymSynsetPolarity.equals("positive")) antonymSynsetPolarity = "negative";
								else if(antonymSynsetPolarity.equals("negative")) antonymSynsetPolarity = "positive";
								String sentence = adjectiveSynset.getDefinition();
								synsetDefinitionPolarity = SynsetDefinitionPolarityAnalyzer(tagger, word, sentence, fileOut);
								if(similarToSynsetPolarity.equals(antonymSynsetPolarity)) {
									if(synsetDefinitionPolarity.equals("neutral") || similarToSynsetPolarity.equals(synsetDefinitionPolarity)) {
										synsetPolarity = similarToSynsetPolarity.toString().toLowerCase();
										reportOut.write("\nRule 3.1");
										reportOut.write("\nSynset: " + synsetsWithPolarityExtract(adjectiveSynset, null).get(0));
										reportOut.write("\nParts Of Speech: Adjective");
										reportOut.write("\nSynset Definition: " + adjectiveSynset.getDefinition());
										reportOut.write("\nSimilar-To Synset: " + synsetsWithPolarityExtract(adjSimilarToSynset, null).get(0));
										reportOut.write("\nSimilar-To Synset Definition: " + adjSimilarToSynset.getDefinition());
										reportOut.write("\nAntonym Synset: " + synsetsWithPolarityExtract(adjAntonymSynset, null).get(0));
										reportOut.write("\nAntonym Synset Definition: " + adjAntonymSynset.getDefinition());
										reportOut.write("\nSynset Polarity(Program): " + synsetPolarity);
										reportOut.write("\nSimilar-To Polarity(Manual): ");
										reportOut.write("\nAntonym Polarity(Manual): ");
										reportOut.write("\nSynset Definition Polarity(Manual): ");
										reportOut.write("\nSynset Polarity(Manual): ");
										reportOut.write("\nInference: ");
										reportOut.write("\n");			
										return (synsetsWithPolarityExtract(adjectiveSynset, synsetPolarity));
									}
								}
							}
							
						}
					}											
				}
			}
		}
		return synsetsWithPolarity;
	}
	private ArrayList<String> ruleThreeAntoAntoHeruristics(MaxentTagger tagger, WordNetDatabase database, String word, BufferedWriter fileOut, BufferedWriter reportOut) throws Exception {
		Synset[] synsets = database.getSynsets(word.trim());
		ArrayList<String> synsetsWithPolarity = null;
		WordSense[] antonymSynsetWords;
		Synset synset, firstAntonymSynset, secondAntonymSynset;
		String firstAntonymSynsetPolarity, seocndAntonymSynsetPolarity, synsetPolarity, synsetDefinitionPolarity, firstAntonymKey = null, secondAntonymKey = null;
		if(synsets.length > 0) {
			for(int numberOfSenses = 0; numberOfSenses < synsets.length; numberOfSenses++) {
				synset = synsets[numberOfSenses];
				antonymSynsetWords = synset.getAntonyms(word);
				if(antonymSynsetWords.length > 1) {
					for(int numberOfAntonymWords = 0; numberOfAntonymWords < antonymSynsetWords.length; numberOfAntonymWords++) {
						firstAntonymSynset = antonymSynsetWords[numberOfAntonymWords].getSynset();
						if(firstAntonymSynset.getType() == SynsetType.NOUN) firstAntonymKey = firstAntonymSynset.getWordForms()[0].trim() + "noun";
						else if(firstAntonymSynset.getType() == SynsetType.ADJECTIVE) firstAntonymKey = firstAntonymSynset.getWordForms()[0].trim() + "adjective";
						else if(firstAntonymSynset.getType() == SynsetType.ADVERB) firstAntonymKey = firstAntonymSynset.getWordForms()[0].trim() + "adverb";
						else if(firstAntonymSynset.getType() == SynsetType.VERB) firstAntonymKey = firstAntonymSynset.getWordForms()[0].trim() + "verb";
						if(polarityInferenceReferenceEngine.containsKey(firstAntonymKey)) {
							for(int restOfAntonymWords = numberOfAntonymWords + 1 ; restOfAntonymWords < antonymSynsetWords.length; restOfAntonymWords++) {
								secondAntonymSynset = antonymSynsetWords[restOfAntonymWords].getSynset();
								if(secondAntonymSynset.getType() == SynsetType.NOUN) secondAntonymKey = secondAntonymSynset.getWordForms()[0].trim() + "noun";
								else if(secondAntonymSynset.getType() == SynsetType.ADJECTIVE) secondAntonymKey = secondAntonymSynset.getWordForms()[0].trim() + "adjective";
								else if(secondAntonymSynset.getType() == SynsetType.ADVERB) secondAntonymKey = secondAntonymSynset.getWordForms()[0].trim() + "adverb";
								else if(secondAntonymSynset.getType() == SynsetType.VERB) secondAntonymKey = secondAntonymSynset.getWordForms()[0].trim() + "verb";
								if(polarityInferenceReferenceEngine.containsKey(secondAntonymKey)) {
									firstAntonymSynsetPolarity = polarityInferenceReferenceEngine.get(firstAntonymKey).get(0);
									seocndAntonymSynsetPolarity = polarityInferenceReferenceEngine.get(secondAntonymKey).get(0);
									String sentence = synset.getDefinition();
									synsetDefinitionPolarity = SynsetDefinitionPolarityAnalyzer(tagger, word, sentence, fileOut);
									if(synsetDefinitionPolarity.equals("positive")) synsetDefinitionPolarity = "negative";
									else if(synsetDefinitionPolarity.equals("negative")) synsetDefinitionPolarity = "positive";
									if(firstAntonymSynsetPolarity.equals(seocndAntonymSynsetPolarity)) {
										if(synsetDefinitionPolarity.equals("neutral") || firstAntonymSynsetPolarity.equals(synsetDefinitionPolarity)) {
											if(synsetDefinitionPolarity.equals("positive")) synsetDefinitionPolarity = "negative";
											else if(synsetDefinitionPolarity.equals("negative")) synsetDefinitionPolarity = "positive";
											synsetPolarity = synsetDefinitionPolarity.toString().toLowerCase();
											reportOut.write("\nRule 3.3");
											reportOut.write("\nSynset: " + synsetsWithPolarityExtract(synset, null).get(0));
											reportOut.write("\nParts Of Speech: " + synset.getType());
											reportOut.write("\nSynset Definition: " + synset.getDefinition());
											reportOut.write("\nAntonym Synset: " + synsetsWithPolarityExtract(firstAntonymSynset, null).get(0));
											reportOut.write("\nAntonym Synset Definition: " + firstAntonymSynset.getDefinition());
											reportOut.write("\nAntonym Synset: " + synsetsWithPolarityExtract(secondAntonymSynset, null).get(0));
											reportOut.write("\nAntonym Synset Definition: " + secondAntonymSynset.getDefinition());
											reportOut.write("\nSynset Polarity(Program): " + synsetPolarity);
											reportOut.write("\nAntonym Polarity(Manual): ");
											reportOut.write("\nAntonym Polarity(Manual): ");
											reportOut.write("\nSynset Definition Polarity(Manual): ");
											reportOut.write("\nSynset Polarity(Manual): ");
											reportOut.write("\nInference: ");
											reportOut.write("\n");			
											return (synsetsWithPolarityExtract(synset, synsetPolarity));
										}
									}
								}
							}
						}
					}
				}
			}
		}
		return synsetsWithPolarity;
	}
	public String SynsetDefinitionPolarityAnalyzer (MaxentTagger tagger, String word, String sentence, BufferedWriter fileOut) throws Exception {		
		String multipleSentences[]=sentence.split(";");
		String definitionPolarity = null;
		int numberOfPositiveWords = 0, numberOfNegativeWords = 0, numberOfOxymoron = 0;
		for(int i = 0; i <multipleSentences.length; i++) {
			multipleSentences[i] = multipleSentences[i].replace("'s","");
			multipleSentences[i] = multipleSentences[i].replace("n't", "not");
			multipleSentences[i] = multipleSentences[i].replace("-", " ");
			multipleSentences[i] = multipleSentences[i].trim();
			String tagged = tagger.tagTokenizedString(multipleSentences[i]);
			String splitWordsInSentence[]=tagged.split(" ");
			boolean negationWord = false;
			for(int j=0; j<splitWordsInSentence.length; j++) {
				String individualWord[]=splitWordsInSentence[j].split("_");
				if(individualWord[0].toLowerCase() == "not" || individualWord[0].toLowerCase() == "no" || 
						individualWord[0].toLowerCase() == "lack" || individualWord[0].toLowerCase() == "without") 
					negationWord = true;
				if(taggerAbbreviationMapping.containsKey(individualWord[1])) {
					String aLWordPolarity = null;
					String gIWordPolarity = null;
					String oPWordPolarity = null;
					Morphology wordStemmer = new Morphology();
					String partsOfSpeech = taggerAbbreviationMapping.get(individualWord[1]);
					String key = individualWord[0].toLowerCase() + partsOfSpeech.toLowerCase();
					String stemmedKey = wordStemmer.stem(individualWord[0].toLowerCase()) + partsOfSpeech.toLowerCase();
					if(appraisalLexiconWordList.containsKey(key)){ aLWordPolarity = appraisalLexiconWordList.get(key); } 
					else if(appraisalLexiconWordList.containsKey(stemmedKey)) { aLWordPolarity = appraisalLexiconWordList.get(stemmedKey); }
					else { aLWordPolarity = "unknown"; }
					if(generalInquirerWordList.containsKey(key)){ gIWordPolarity = generalInquirerWordList.get(key); } 
					else if(generalInquirerWordList.containsKey(stemmedKey)) { gIWordPolarity = generalInquirerWordList.get(stemmedKey); }
					else { gIWordPolarity = "unknown"; }
					if(opinionFinderWordList.containsKey(key)){	oPWordPolarity = opinionFinderWordList.get(key); }
					else if(opinionFinderWordList.containsKey(stemmedKey)) { oPWordPolarity = opinionFinderWordList.get(stemmedKey); }
					else { oPWordPolarity = "unknown"; }
					if(aLWordPolarity.equalsIgnoreCase("positive") || gIWordPolarity.equalsIgnoreCase("positive") || oPWordPolarity.equalsIgnoreCase("positive")) {
						if(aLWordPolarity.equalsIgnoreCase("negative") || gIWordPolarity.equalsIgnoreCase("negative") || oPWordPolarity.equalsIgnoreCase("negative"))
							numberOfOxymoron++;
						else if (negationWord == false) numberOfPositiveWords++;
						else if (negationWord == true) numberOfNegativeWords++;
					}
					if(aLWordPolarity.equalsIgnoreCase("negative") || gIWordPolarity.equalsIgnoreCase("negative") || oPWordPolarity.equalsIgnoreCase("negative")) {
						if(aLWordPolarity.equalsIgnoreCase("positive") || gIWordPolarity.equalsIgnoreCase("positive") || oPWordPolarity.equalsIgnoreCase("positive"))
							numberOfOxymoron++;
						else if (negationWord == false) numberOfNegativeWords++;
						else if (negationWord == true) numberOfPositiveWords++;
					}
				}
			}
		}
		if(numberOfPositiveWords > 0 && numberOfNegativeWords == 0 && numberOfOxymoron == 0) {
			definitionPolarity = "positive";
			return(definitionPolarity);
		}
		else if(numberOfNegativeWords > 0 && numberOfPositiveWords == 0 && numberOfOxymoron == 0) {
			definitionPolarity = "negative";
			return(definitionPolarity);
		}
		else if(numberOfOxymoron > 0 || (numberOfNegativeWords >= 1 && numberOfPositiveWords >= 1)) {
			definitionPolarity = "oxymoron words";
			return(definitionPolarity);
		}
		else if(numberOfPositiveWords == 0 && numberOfNegativeWords == 0 && numberOfOxymoron == 0) {
			definitionPolarity = "neutral";
			return(definitionPolarity);
		}
		else return definitionPolarity;	
	}
}
