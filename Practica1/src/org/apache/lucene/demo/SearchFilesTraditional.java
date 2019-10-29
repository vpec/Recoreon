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
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.Date;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.es.SpanishAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.DoublePoint;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TotalHitCountCollector;
import org.apache.lucene.store.FSDirectory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/** Simple command-line based search demo. */
public class SearchFilesTraditional {

	
	private static HashMap<String, String> infoNeedsMap = new HashMap<>();
	
	private SearchFilesTraditional() {}

	/** Simple command-line based search demo. */
	public static void main(String[] args) throws Exception {
		
		File fXmlFile = new File("../MiniTREC/necesidadesInformacionElegidas.xml");
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
		
		
		
		/*
		String usage = "Usage:\tjava org.apache.lucene.demo.SearchFiles [-index dir] [-field f] [-repeat n] [-queries file] [-query string] [-raw] [-paging hitsPerPage]\n\nSee http://lucene.apache.org/core/4_1_0/demo/ for details.";
		if (args.length > 0 && ("-h".equals(args[0]) || "-help".equals(args[0]))) {
			System.out.println(usage);
			System.exit(0);
		}
		String index = "index";
		String field = "contents";
		String queries = null;
		int repeat = 0;
		String queryString = null;

		for (int i = 0; i < args.length; i++) {
			if ("-index".equals(args[i])) {
				index = args[i + 1];
				i++;
			} else if ("-field".equals(args[i])) {
				field = args[i + 1];
				i++;
			} else if ("-queries".equals(args[i])) {
				queries = args[i + 1];
				i++;
			} else if ("-query".equals(args[i])) {
				queryString = args[i + 1];
				i++;
			} else if ("-repeat".equals(args[i])) {
				repeat = Integer.parseInt(args[i + 1]);
				i++;
			}
		}

		IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(index)));
		IndexSearcher searcher = new IndexSearcher(reader);
		Analyzer analyzer = new SpanishAnalyzer();

		BufferedReader in = null;
		if (queries != null) {
			in = new BufferedReader(new InputStreamReader(new FileInputStream(queries), "UTF-8"));
		} else {
			in = new BufferedReader(new InputStreamReader(System.in, "UTF-8"));
		}
		QueryParser parser = new QueryParser(field, analyzer);
		BooleanQuery.Builder finalQuery = new BooleanQuery.Builder();
		boolean atLeastOne = false;
		while (true) {
			if (queries == null && queryString == null) { // prompt the user
				System.out.println("Enter query: ");
			}

			String line = queryString != null ? queryString : in.readLine();

			if (line == null || line.length() == -1) {
				break;
			}

			line = line.trim();
			if (line.length() == 0) {
				if (!atLeastOne) {
					break;
				}
				// Execute query
				Query querySearch = finalQuery.build();
				System.out.println("Searching for: " + querySearch.toString(field));

				if (repeat > 0) { // repeat & time as benchmark
					Date start = new Date();
					for (int i = 0; i < repeat; i++) {
						searcher.search(querySearch, 100);
					}
					Date end = new Date();
					System.out.println("Time: " + (end.getTime() - start.getTime()) + "ms");
				}

				showSearchResults(in, searcher, querySearch);

				atLeastOne = false;
				finalQuery = new BooleanQuery.Builder();

				if (queryString != null) {
					break;
				}
			} else if (line.equals(":q")) {
				break;
			} else {
				atLeastOne = true;
				Double west, east, south, north;
				if (line.split(":")[0].equals("spatial")) {
					west = Double.parseDouble(line.substring(line.indexOf(':') + 1, line.indexOf(',')));
					line = line.substring(line.indexOf(',') + 1, line.length());
					east = Double.parseDouble(line.substring(0, line.indexOf(',')));
					line = line.substring(line.indexOf(',') + 1, line.length());
					south = Double.parseDouble(line.substring(0, line.indexOf(',')));
					line = line.substring(line.indexOf(',') + 1, line.length());
					north = Double.parseDouble(line.substring(0, line.length()));

					// Xmin <= east
					Query westRangeQuery = DoublePoint.newRangeQuery("west", Double.NEGATIVE_INFINITY, east);
					// Xmax >= west
					Query eastRangeQuery = DoublePoint.newRangeQuery("east", west, Double.POSITIVE_INFINITY);
					// Ymin <= north
					Query southRangeQuery = DoublePoint.newRangeQuery("south", Double.NEGATIVE_INFINITY, north);
					// Ymax >= south
					Query northRangeQuery = DoublePoint.newRangeQuery("north", south, Double.POSITIVE_INFINITY);

					// Construction
					BooleanQuery queryBool = new BooleanQuery.Builder().add(westRangeQuery, BooleanClause.Occur.MUST)
							.add(eastRangeQuery, BooleanClause.Occur.MUST)
							.add(southRangeQuery, BooleanClause.Occur.MUST)
							.add(northRangeQuery, BooleanClause.Occur.MUST).build();

					finalQuery.add(queryBool, BooleanClause.Occur.SHOULD);
				} else {
					finalQuery.add(parser.parse(line), BooleanClause.Occur.SHOULD);
				}
			}

		}
		reader.close();
		
		*/
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
	public static void showSearchResults(BufferedReader in, IndexSearcher searcher, Query query) throws IOException {
		TotalHitCountCollector collector = new TotalHitCountCollector();
		searcher.search(query, collector);
		TopDocs results  = searcher.search(query, Math.max(1, collector.getTotalHits()));
		ScoreDoc[] hits = results.scoreDocs;
		int numTotalHits = (int) results.totalHits;
		System.out.println(numTotalHits + " total matching documents");
		for (int i = 0; i < numTotalHits; i++) {
			Document doc = searcher.doc(hits[i].doc);
			System.out.println((i + 1) + ". doc=" + hits[i].doc + " path=" + doc.get("path") + " score=" + hits[i].score);
		}
	}
}

