/*
 ***************************************************
 * Traditional information Retrieval System ********
 * Authors: Victor Penasco Estivalez - 741294 ******
 * 			Ruben Rodriguez Esteban  - 737215 ******
 * Date: 7-11-19 ***********************************
 ***************************************************
 */


package traditional;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.es.SpanishAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TotalHitCountCollector;
import org.apache.lucene.store.FSDirectory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.tokenize.SimpleTokenizer;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.util.Span;

/** Simple command-line based search demo. */
public class SearchFilesTraditional {

	// Hashmap which stores the information needs in tuples <key, valor>
	// where the key is the id of the need information need andthe valor 
	// is the content
	private static HashMap<String, String> infoNeedsMap = new HashMap<>();
	
	
	/**
	 * Default constructor
	 */
	private SearchFilesTraditional() {}

	
	
	/** Simple command-line based search demo. */
	public static void main(String[] args) throws Exception {
		
		// Path of the index where de documents are stored
		String indexPath = "index";
	    String infoNeedsPath = null;
	    String resultsPath = null;
		
	    // Verification of the parameters
		for(int i = 0; i < args.length; i++) {
		      if ("-index".equals(args[i])) {
		        indexPath = args[i+1];
		        i++;
		      } 
		      else if ("-infoNeeds".equals(args[i])) {
		    	  infoNeedsPath = args[i+1];
		        i++;
		      }
		      else if ("-output".equals(args[i])) {
		    	  resultsPath = args[i+1];
			        i++;
		      }
		}
		
		// Store the path of the file with the information needs
		File fXmlFile = new File(infoNeedsPath);
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		org.w3c.dom.Document doc = dBuilder.parse(fXmlFile);
		
		// Application of the normalization process to the documents
		doc.getDocumentElement().normalize();
		// Extract in a list the informationNeed tags 
		NodeList nList = doc.getElementsByTagName("informationNeed");
		 
		for (int temp = 0; temp < nList.getLength(); temp++) {
			Node nNode = nList.item(temp);		
			if (nNode.getNodeType() == Node.ELEMENT_NODE) {
				Element eElement = (Element) nNode;
				// Adding to the Hashmap the tuple <key, value> where the key is the id of the
				// information and the value is the content
				infoNeedsMap.put(eElement.getElementsByTagName("identifier").item(0).getTextContent(),
								 eElement.getElementsByTagName("text").item(0).getTextContent());
			}
		}
		
		// Path of the file which is going to store the documents 
		File resultsFile = new File(resultsPath);
		FileWriter resultsWriter;
		
		// Writing flow associated to the file 
		resultsWriter = new FileWriter(resultsFile);
		PrintWriter pw = new PrintWriter(resultsWriter);
		
		// Extraction of the current year
		int currentYear = Calendar.getInstance().get(Calendar.YEAR);
		String field;
		for (Entry<String, String> entry : infoNeedsMap.entrySet()) {				
			field = "contents";
			IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(indexPath)));
			
			// Creation of the searcher and the analyzer
			IndexSearcher searcher = new IndexSearcher(reader);
			Analyzer analyzer = new CustomSpanishAnalyzer();
			QueryParser parser = new QueryParser(field, analyzer);
			BooleanQuery.Builder finalQuery = new BooleanQuery.Builder();
				
			// Extraction of the content of each information need and  
			// removed ? and * characters because they may be confused as wildcard querys
			String line = entry.getValue().replace("?", "").replace("*", "");
			// Transform the query to lowerCase in order to make insensitive to uppercase letters
			String lineLowerCase = line.toLowerCase();
			
			// Application of the tokenization process of the query
			Tokenizer tokenizer = SimpleTokenizer.INSTANCE;
			String tokens[] = tokenizer.tokenize(line);
			
			// flags to detect in the the query are wanted bachelorThesis or masterThesis
			boolean bachelorThesis = false, masterThesis = false;
			
			/* Prioritize bachelorThesis */
			// Regex expression to detect bachelorThesis documents
			if(lineLowerCase.matches(".*(tfg|(trabajos? ?(de)? ?fin ?(de)? ?grado)).*")) {
				bachelorThesis = true;
				finalQuery.add(new BoostQuery(parser.parse("type:bachelorThesis"),  2), 
					           BooleanClause.Occur.SHOULD);
	    	}	
			/* Prioritize masterThesis */
			// Regex expression to detect masterThesis documents
			if(lineLowerCase.matches(".*(tfm|tesis|(trabajos? ?(de)? ?fin ?(de)? ?(master|máster))).*")) {
				masterThesis = true;
				finalQuery.add(new BoostQuery(parser.parse("type:masterThesis"),  2), 
						       BooleanClause.Occur.SHOULD);
	    	}
			// Path of the Postagger module used to assign tags for each word of the query
			InputStream modelIn = new FileInputStream("services/es-pos-maxent.bin");
			POSModel model = new POSModel(modelIn);
			POSTaggerME tagger = new POSTaggerME(model);
			String tags[] = tagger.tag(tokens);
			
			// Vector of the tokens 
			List<Integer> dateList = new ArrayList<>();
			int lowNumber = 0;
			
			for(int i = 0; i < tags.length; i++) {
				// Control if the actual tag corresponds to a number and it's a year between 2000 and 2019
				if(tags[i].startsWith("Z") && Integer.parseInt(tokens[i]) > 2000 && Integer.parseInt(tokens[i]) <= currentYear) {
					dateList.add(Integer.parseInt(tokens[i]));
				}
				// Control if the actual tag corresponds to a number
				else if(tags[i].startsWith("Z")){
					lowNumber = Integer.parseInt(tokens[i]);
				}
			}
			
			if(!dateList.isEmpty()) {
				// Controls if the date is only composed by a unique number and the date has the following format
				// a partir del YEAR
				// a partir de YEAR
				// a partir del a�o YEAR
				if(dateList.size() == 1 && lineLowerCase.matches(".*(a partir del? (año )?[0-9]+).*")) {
					// date ranke like [YEAR TO 2019]
					finalQuery.add(parser.parse("date:[" + ((Integer)(dateList.get(0))).toString() + " TO " 
					                                     + ((Integer)currentYear).toString() 
					                                     + "]"), BooleanClause.Occur.SHOULD);
				}
				// Controls if the date is only composed by a two numbers 
				else if(dateList.size() == 2) {
					// Extraction of both years
					Integer lower = dateList.get(0) < dateList.get(1) ? dateList.get(0) : dateList.get(1);
					Integer upper = lower == dateList.get(0) ? dateList.get(1) : dateList.get(0);
					// date range like [lower TO upper]
					finalQuery.add(parser.parse("date:[" + lower.toString() + " TO " + upper.toString() + "]"), BooleanClause.Occur.SHOULD);
				}
				else {
					// Normal cause
					for(Integer dateElement : dateList) {
						finalQuery.add(parser.parse("date:" + dateElement.toString()), BooleanClause.Occur.SHOULD);
					}
				}
			}
			
			// Case reserved for dates with the following formats:
			// ultimo ANYO
			// ultimos ANYO a�os
			// ultimos a�os
			if(lineLowerCase.matches(".*(últimos? [0-9]* ?años?).*")){
				// date range like [lower TO upper]
				finalQuery.add(parser.parse("date:[" + ((Integer)(currentYear - lowNumber)).toString() 
													 + " TO " + ((Integer)currentYear).toString() 
													 + "]"), BooleanClause.Occur.SHOULD);
			}
			
			// nameFinder module in order to detect names
			InputStream modelInNameFinder = new FileInputStream("services/es-ner-person.bin");
			TokenNameFinderModel modelNameFinder = new TokenNameFinderModel(modelInNameFinder);
			NameFinderME nameFinder = new NameFinderME(modelNameFinder);

		    Span nameSpans[] = nameFinder.find(tokens);
		    if(nameSpans.length > 0) {
		    	for(Span name : nameSpans) {
		    		// Control if in the query are this regular expresions 
		    		if(lineLowerCase.matches(".*(profesor|alumn|tutor|creador).*")) {
		    			// Added with a with exponent 5 in order to increase the score of this part of the query
		    			// because this part is expected to be the author of the doc
		    			finalQuery.add(new BoostQuery(parser.parse("creator:" + 
		    					   tokens[name.getStart()]), 5), BooleanClause.Occur.SHOULD);
			    	}
		    		else {
		    			// Added with a with exponent 2 in order to increase the score of this part of the query
		    			finalQuery.add(new BoostQuery(parser.parse("creator:" + 
		    					   tokens[name.getStart()]), 2), BooleanClause.Occur.SHOULD);
		    		}
		    		
			    }
		    }
			String lineDescription = "";
			String lineTitle = "";
			for(String word : tokens) {
				// Control if in there is co
				if(!word.toLowerCase().matches(".*(profesor|alumn|tutor|creador|tfg|tfm|tesis).*") &&
						!((bachelorThesis || masterThesis) && 
						   word.toLowerCase().matches("(trabajos?|fin|grado|máster|master)"))) 
				{
					lineDescription = lineDescription + " description:" + word;
					lineTitle = lineTitle + " title:" + word;
				}
			}
			// Extraction of the spaces
			lineDescription = lineDescription.trim();
			lineTitle = lineTitle.trim();
			finalQuery.add(parser.parse(lineDescription), BooleanClause.Occur.SHOULD);
			finalQuery.add(new BoostQuery(parser.parse(lineTitle), 2), BooleanClause.Occur.SHOULD);	
			// Execute query
			Query querySearch = finalQuery.build();
			System.out.println("Searching for: " + querySearch.toString(field));		
			// Write the results obtained for each query in the results file
			writeSearchResults(searcher, querySearch, entry.getKey(), pw);
			
			// Close the reading flow
			reader.close();
		}
		// Close the writting flow
		resultsWriter.close();
	}

		
	
	
	/**
	 * @param searcher is the module tasked with executing the query
	 * @param query is the query which id going to be executed
	 * @param is the need for information to look for in the query
	 * @pw is the module printer of the file 
	 * Stores in the file all the documents which are are related to the query
	 */
	public static void writeSearchResults(IndexSearcher searcher, Query query, String infoNeedId, PrintWriter pw) throws IOException{
		// Execution of the query
		TotalHitCountCollector collector = new TotalHitCountCollector();
		searcher.search(query, collector);
		// Store de documents found which their scores
		TopDocs results  = searcher.search(query, Math.max(1, collector.getTotalHits()));
		ScoreDoc[] hits = results.scoreDocs;
		int numTotalHits = (int) results.totalHits;
		// Show the total number of documents found
		System.out.println(numTotalHits + " total matching documents");
		for (int i = 0; i < numTotalHits; i++) {
			// Shows the title of each document found
			Document doc = searcher.doc(hits[i].doc);
			pw.println(infoNeedId + "\t" + doc.get("path").replaceFirst(".*/([^/?]+).*", "$1"));
		}
	}
}

