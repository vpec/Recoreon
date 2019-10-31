package org.apache.lucene.demo;

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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.Date;
import java.util.HashMap;
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

	
	private static HashMap<String, String> infoNeedsMap = new HashMap<>();
	
	private SearchFilesTraditional() {}

	/** Simple command-line based search demo. */
	public static void main(String[] args) throws Exception {
		
		
		String indexPath = "index";
	    String infoNeedsPath = null;
	    String resultsPath = null;
		
		for(int i=0;i<args.length;i++) {
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
		
		File fXmlFile = new File(infoNeedsPath);
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		org.w3c.dom.Document doc = dBuilder.parse(fXmlFile);
				
		doc.getDocumentElement().normalize();
		NodeList nList = doc.getElementsByTagName("informationNeed");
				
		for (int temp = 0; temp < nList.getLength(); temp++) {
			Node nNode = nList.item(temp);		
			if (nNode.getNodeType() == Node.ELEMENT_NODE) {
				Element eElement = (Element) nNode;
				infoNeedsMap.put(eElement.getElementsByTagName("identifier").item(0).getTextContent(),
								 eElement.getElementsByTagName("text").item(0).getTextContent());
			}
		}
		
		for (Entry<String, String> entry : infoNeedsMap.entrySet()) {				
			System.out.println(entry.getKey());
			System.out.println(entry.getValue());
			
			
			
			String field = "contents";
			String queries = null;
			
			IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(indexPath)));
			IndexSearcher searcher = new IndexSearcher(reader);
			Analyzer analyzer = new SpanishAnalyzer2();

			QueryParser parser = new QueryParser(field, analyzer);
			BooleanQuery.Builder finalQuery = new BooleanQuery.Builder();
				
			// Remove ? and * because they may be confused as wildcard querys
			String line = entry.getValue().replace("?", "").replace("*", "");
			System.out.println(line);
			
			Tokenizer tokenizer = SimpleTokenizer.INSTANCE;
			String tokens[] = tokenizer.tokenize(line);
			
			
			
			
			InputStream modelIn = new FileInputStream("es-pos-maxent.bin");
			POSModel model = new POSModel(modelIn);
			POSTaggerME tagger = new POSTaggerME(model);
			String tags[] = tagger.tag(tokens);
			for(int i = 0; i < tags.length; i++) {
				//System.out.println(tokens[i] + " " + tags[i]);
			}
			
			/*
			 * Z = number
			 * NC = noun
			 * V* = verb
			 */
			
			InputStream modelInFinder = new FileInputStream("es-ner-person.bin");
			TokenNameFinderModel modelFinder = new TokenNameFinderModel(modelInFinder);
			NameFinderME nameFinder = new NameFinderME(modelFinder);

		    Span nameSpans[] = nameFinder.find(tokens);
		    
		    if(nameSpans.length > 0) {
		    	for(Span name : nameSpans) {
			    	finalQuery.add(new BoostQuery(parser.parse("creator:" + tokens[name.getStart()]), 10), BooleanClause.Occur.SHOULD);
			    }
		    }
		    		    

			
			
			
			String lineDescription = "";
			String lineTitle = "";
			for(String word : tokens) {
				lineDescription = lineDescription + " description:" + word;
				lineTitle = lineTitle + " title:" + word;
			}
			lineDescription = lineDescription.trim();
			lineTitle = lineTitle.trim();
			
			finalQuery.add(parser.parse(lineDescription), BooleanClause.Occur.SHOULD);
			finalQuery.add(new BoostQuery(parser.parse(lineTitle), 2), BooleanClause.Occur.SHOULD);
				
			// Execute query
			Query querySearch = finalQuery.build();
			
//			Query query = parser.parse(line);
			System.out.println("Searching for: " + querySearch.toString(field));
			
			showSearchResults(searcher, querySearch);
			
			reader.close();
		}	
	}

		
	/**
	 * This demonstrates a typical paging search scenario, where the search engine
	 * presents pages of size n to the user. The user can then go to the next page
	 * if interested in the next hits.
	 * 
	 * When the query is executed for the first time, then only enough results are
	 * collected to fill 5 result pages. If the user wants to page beyond this
	 * limit, then the query is executed another time and all hits are collected.
	 * 
	 */
	public static void showSearchResults(IndexSearcher searcher, Query query) throws IOException {
		TotalHitCountCollector collector = new TotalHitCountCollector();
		searcher.search(query, collector);
		TopDocs results  = searcher.search(query, Math.max(1, collector.getTotalHits()));
		ScoreDoc[] hits = results.scoreDocs;
		int numTotalHits = (int) results.totalHits;
		System.out.println(numTotalHits + " total matching documents");
		for (int i = 0; i < numTotalHits; i++) {
			Document doc = searcher.doc(hits[i].doc);
			if(i < 10) {
				System.out.println((i + 1) + ". doc=" + hits[i].doc + " path=" + doc.get("path") + " score=" + hits[i].score);
			}
		}
	}
}

