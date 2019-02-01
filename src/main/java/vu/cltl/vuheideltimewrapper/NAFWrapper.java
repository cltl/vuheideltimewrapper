package vu.cltl.vuheideltimewrapper;


import de.unihd.dbs.uima.types.heideltime.Sentence;
import de.unihd.dbs.uima.types.heideltime.Token;
import eu.kyotoproject.kaf.KafSaxParser;
import eu.kyotoproject.kaf.KafTerm;
import eu.kyotoproject.kaf.KafTimex;
import eu.kyotoproject.kaf.KafWordForm;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NAFWrapper{
	private KafSaxParser kaf;
	private HashMap<String,String> mapping;
	private HashMap<String,HashSet<String>> exceptions;
	private HashMap<String, ArrayList<KafWordForm>> sentenceMap;
	final String DELIMITER = "\t";
	
	NAFWrapper(KafSaxParser kaf, String mappingFile){
		this.kaf = kaf;
		sentenceMap = new HashMap<>();
        initMapping(mappingFile);
	}

    public HashMap<String, ArrayList<KafWordForm>> getSentenceMap() {
        return sentenceMap;
    }

    private void initMapping(String file){
   		mapping = new HashMap<String,String>();
   		exceptions = new HashMap<String, HashSet<String>>();

   		BufferedReader br = null;
   		Pattern lemmaPattern = Pattern.compile("Lemma\\s(.*)$");
   		Pattern exceptPattern = Pattern.compile("except\\slemma\\:\\s(.*)$");
   		Pattern includePattern = Pattern.compile("lemma\\:\\s(.*)$");
   		try {
   			String line = "";
   			br = new BufferedReader(new InputStreamReader(new FileInputStream(file), "utf-8"));
   			//Read the file line by line
   			while ((line = br.readLine()) != null) {
   				String[] tags = line.split(DELIMITER);
   				String ixaPipes = tags[0];
   				String treetagger = tags[1];
   				mapping.put(ixaPipes, treetagger);
   				if (ixaPipes.matches("Lemma.*")){
   					HashSet<String> info = new HashSet<String>();
   					Matcher matcher = lemmaPattern.matcher(ixaPipes);
   					while (matcher.find()){
   						info.add(matcher.group(1));
   					}
   					exceptions.put(ixaPipes, info);
   				}
   				else if (ixaPipes.matches("except lemma.*")){
   					HashSet<String> info = new HashSet<String>();
   					Matcher matcher = exceptPattern.matcher(ixaPipes);
   					while (matcher.find()){
   						String foundLemmas = matcher.group(1);
   						String[] fLemmas = foundLemmas.split(",");
   						for (String f: fLemmas){
   							info.add(f);
   						}
   					}
   					exceptions.put(ixaPipes, info);
   				}
   				else if (ixaPipes.matches("lemma.*")){
   					HashSet<String> info = new HashSet<String>();
   					Matcher matcher = includePattern.matcher(ixaPipes);
   					while (matcher.find()){
   						String foundLemmas = matcher.group(1);
   						String[] fLemmas = foundLemmas.split(",");
   						for (String f: fLemmas){
   							info.add(f);
   						}
   					}
   					exceptions.put(ixaPipes, info);
   				}
   			}

   		} catch (IOException e) {
   			e.printStackTrace();
   		} finally {
   			try {
   				if (br != null)br.close();
   			} catch (IOException ex) {
   				ex.printStackTrace();
   			}
   		}

   	}

	private String getPOS (KafTerm term){
		String pos = "";
		String morpho = term.getMorphofeat();
		String lemma = term.getLemma();
		for (String toCompare:this.mapping.keySet()){
			if (toCompare.matches("Lemma.*")){
				HashSet<String> lemmas = exceptions.get(toCompare);
				if (lemmas.contains(lemma)){
					return mapping.get(toCompare);
				}
			}
			else if (toCompare.matches("except lemma.*")){
				HashSet<String> lemmas = exceptions.get(toCompare);
				if (!lemmas.contains(lemma)){
					return mapping.get(toCompare);
				}
			}
			else if (toCompare.matches("lemma.*")){
				HashSet<String> lemmas = exceptions.get(toCompare);
				if (lemmas.contains(lemma)){
					return mapping.get(toCompare);
				}
			}
			else if (morpho.matches(toCompare)){
				return mapping.get(toCompare);
			}
		}
		
		return pos;
		
	}
	

	@SuppressWarnings("finally")
	public Date creationTime(){
		//SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-ddThh:mm:ssz");
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		
		//initialize date with current date (will be overwritten by dct)
		//trying dct first throws exception
	    Calendar cal = Calendar.getInstance();
		Date date = cal.getTime();
		try {
		    String dct = kaf.getKafMetaData().getCreationtime();
		    if ( dct != null){
			date = format.parse(dct);
			cal.setTime(date);
		    }
		    else{
			date = cal.getTime();
		    }
		    KafTimex time = new KafTimex();
		    String dctToString = getTimexFormat(cal);
		    String timeId = "tx"+(kaf.kafTimexLayer.size()+1);
		    time.setValue(dctToString);
		    time.setType("DATE");
		    time.setFunctionInDocument("CREATION_TIME");
		    time.setId(timeId);
		    kaf.kafTimexLayer.add(time);

		} catch (ParseException e) {
			e.printStackTrace();
		}
		finally {
			return date;
		}
		
	}

	public HashMap<String, ArrayList<KafWordForm>> getSentences () {
		HashMap<String, ArrayList<KafWordForm>> sentences = new HashMap<String, ArrayList<KafWordForm>>();
		String sentenceId = "";
		ArrayList<KafWordForm> sentence = new ArrayList<>();
		for (int i = 0; i < kaf.kafWordFormList.size(); i++) {
			KafWordForm wordForm = kaf.kafWordFormList.get(i);
			if (!wordForm.getSent().equalsIgnoreCase(sentenceId)) {
				if (!sentenceId.isEmpty()) {
					sentences.put(sentenceId, sentence);
                    sentence = new ArrayList<>();
				}
				else {
                }
                sentenceId = wordForm.getSent();
			}
            sentence.add(wordForm);
        }
		if (!sentenceId.isEmpty()) {
			sentences.put(sentenceId, sentence);
		}
		return sentences;
	}

	public String getText(){
		String text = "";

		for (int i = 0; i < kaf.kafWordFormList.size(); i++) {
		    KafWordForm wordForm = kaf.kafWordFormList.get(i);
		    while (text.length()< Integer.parseInt(wordForm.getCharOffset())) {
		    	/// add white space padding till text matches the next offset word position
		    	text+=" ";
			}
		    text += wordForm.getWf();
		}
		return text.trim();
	}
	
	/**
	 * Method that gets called to process the documents' jcas objects
	 */
	public void process(JCas jcas) throws AnalysisEngineProcessException {
		Integer offset = 0; // a cursor of sorts to keep up with the position in the document text
		int nsent = 0;
		sentenceMap = getSentences();
		//// we need to iterate over the sentences in the right order
		for (int s = 0; s <= sentenceMap.size(); s++) {
			String sentenceId = new Integer(s).toString();
			if (sentenceMap.containsKey(sentenceId)) {
				Sentence sentence = new Sentence(jcas);
				sentence.setSentenceId(s);
				sentence.setBegin(offset);
				ArrayList<KafWordForm> nafSentence = sentenceMap.get(sentenceId);
				for (int i = 0; i < nafSentence.size(); i++) {
					KafWordForm kafWordForm = nafSentence.get(i);
					int offsetBegin = Integer.parseInt(kafWordForm.getCharOffset());
					int offsetLength = Integer.parseInt(kafWordForm.getCharLength());
					KafTerm kafTerm = kaf.getTermForWordId(kafWordForm.getWid());
					Token t = new Token(jcas);
					t.setPos(kafTerm.getPos());
					t.setBegin(offsetBegin);
					t.setEnd(offsetBegin+offsetLength);
					offset = t.getEnd();
					t.addToIndexes();
				}
				sentence.setEnd(offset);
				sentence.addToIndexes();
				//System.err.println("Sentence: " + sentence.getBegin() + ":" + sentence.getEnd() + " = " + sentence.getCoveredText());
			}
		}

		// iterate over sentences in this document

		
		// THIS CODE IS FROM IXA CHECKING THEIR TOKENIZATION LAYER
/*		FSIterator fsi = jcas.getAnnotationIndex(Sentence.type).iterator();
		while(fsi.hasNext()) {
			Sentence s = (Sentence) fsi.next();
			if(s.getBegin() < 0 || s.getEnd() < 0) {
				System.err.println("Sentence: " + s.getBegin() + ":" + s.getEnd() + " = " + s.getCoveredText());
				System.err.println("wrong index in text: " + jcas.getDocumentText());
				System.exit(-1);
			}
		}
		FSIterator fsi2 = jcas.getAnnotationIndex(Token.type).iterator();
		while(fsi2.hasNext()) {
			Token t = (Token) fsi2.next();
			if(t.getBegin() < 0 || t.getEnd() < 0) {
				System.err.println("In text: " + jcas.getDocumentText());
				System.err.println("Token: " + t.getBegin() + ":" + t.getEnd());
				System.exit(-1);
			}
		}*/
	}
	
	public void addTimex(int sentence, int begin, int end, String value, String type){
		KafTimex timeEx = new KafTimex();
		timeEx.setValue(value);
		timeEx.setType(type);
		String timexId = "tx"+(kaf.kafTimexLayer.size()+1);
		timeEx.setId(timexId);
		ArrayList<KafWordForm> sentenceArray = sentenceMap.get(new Integer(sentence).toString());
        for (int i = 0; i < sentenceArray.size(); i++) {
            KafWordForm kafWordForm = sentenceArray.get(i);
            int offset = Integer.parseInt(kafWordForm.getCharOffset());
			if (offset >= begin && offset < end){
				timeEx.addSpan(kafWordForm.getWid());
			}
			else{
				//check if the identified timex is a substring of the wf
				int endoff = offset + Integer.parseInt(kafWordForm.getCharLength());
				if (offset < begin && endoff >= end){
					timeEx.addSpan(kafWordForm.getWid());
				}
			}
        }
        this.kaf.kafTimexLayer.add(timeEx);
	}
	
    private String getTimexFormat(Calendar cal){
	String format = null;
	
	int year = cal.get(Calendar.YEAR);
	format = Integer.toString(year) + "-";
	int month = cal.get(Calendar.MONTH) + 1;
	if (month < 10){
	    format += Integer.toString(0);
	}
	format += Integer.toString(month) + "-";
	int day = cal.get(Calendar.DAY_OF_MONTH);
	if (day < 10){
	    format += Integer.toString(0);
	}
	format += Integer.toString(day);
	

	return format;
    }

}
