package vu.cltl.vuheideltimewrapper;


import de.unihd.dbs.uima.types.heideltime.Sentence;
import de.unihd.dbs.uima.types.heideltime.Token;
import eu.kyotoproject.kaf.KafSaxParser;
import eu.kyotoproject.kaf.KafTerm;
import eu.kyotoproject.kaf.KafTimex;
import eu.kyotoproject.kaf.KafWordForm;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIterator;
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
	final String DELIMITER = "\t";
	
	NAFWrapper(KafSaxParser kaf, String mappingFile){
		this.kaf = kaf;
        initMapping(mappingFile);

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
		    time.setValue(dctToString);
		    time.setFunctionInDocument("CREATION_TIME");

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
				}
				sentence = new ArrayList<>();
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
			text += " ";
		    text += wordForm.getWf();
		}
		return text;
	}
	
	/**
	 * Method that gets called to process the documents' jcas objects
	 */
	public void process(JCas jcas) throws AnalysisEngineProcessException {

		// grab the document text
		String docText = jcas.getDocumentText();
		HashMap<String, ArrayList<KafWordForm>> sentenceMap = getSentences();
		for (Map.Entry<String, ArrayList<KafWordForm>> entry : sentenceMap.entrySet()) {
			System.out.println("Key = " + entry.getKey() +
					", Value = " + entry.getValue());
			ArrayList<KafWordForm> sentence = entry.getValue();
			for (int i = 0; i < sentence.size(); i++) {
				KafWordForm kafWordForm = sentence.get(i);
				int offsetBegin = Integer.parseInt(kafWordForm.getCharOffset());
				int offsetLength = Integer.parseInt(kafWordForm.getCharLength());
				KafTerm kafTerm = kaf.getTermForWordId(kafWordForm.getWid());
				Token t = new Token(jcas);
				t.setPos(kafTerm.getPos());
				t.setBegin(offsetBegin);
				t.setEnd(offsetBegin+offsetLength);
				t.addToIndexes();
			}
		}

		// iterate over sentences in this document

		
		// TODO: DEBUG
		FSIterator fsi = jcas.getAnnotationIndex(Sentence.type).iterator();
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
		}
	}
	
	public void addTimex(int sentence, int begin, int end, String value){
		KafTimex time = new KafTimex();
		time.setValue(value);
		// @TODO Need to add the span here....
/*		List<WF> wfs = kaf.getWFsBySent(sentence);
		List<WF> wfSpan = new ArrayList<WF>();
		for (WF wf:wfs){
			int offset = wf.getOffset();
			if (offset >= begin && offset < end){
				wfSpan.add(wf);
			}
			else{
				//check if the identified timex is a substring of the wf
				int endoff = offset + wf.getLength();
				if (offset < begin && endoff >= end){
					wfSpan.add(wf);
				}
			}
		}
		time.setSpan(KAFDocument.newWFSpan(wfSpan));*/
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
