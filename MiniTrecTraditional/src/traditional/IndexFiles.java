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

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.es.SpanishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.DoublePoint;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.Date;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;


/** Index all text files under a directory.
 * <p>
 * This is a command-line application demonstrating simple Lucene indexing.
 * Run it with no command-line arguments for usage information.
 */
public class IndexFiles {
  
	

  /**
   * Default constructor of the class
   */
  private IndexFiles() {}

  
  
  /** Index all text files under a directory. */
  public static void main(String[] args) {
	  
    String usage = "java org.apache.lucene.demo.IndexFiles"
                 + " [-index INDEX_PATH] [-docs DOCS_PATH] [-update]\n\n"
                 + "This indexes the documents in DOCS_PATH, creating a Lucene index"
                 + "in INDEX_PATH that can be searched with SearchFiles";
    
    // Index of the docs
    String indexPath = "index";
    String docsPath = null;
    
    // Processing of the parameters 
    for(int i = 0; i< args.length; i++) {
	   if ("-index".equals(args[i])) {
	      indexPath = args[i+1];
	      i++;
	   } 
	   else if ("-docs".equals(args[i])) {
	      docsPath = args[i+1];
	      i++;
	   }
	}

    
    if (docsPath == null) {
      System.err.println("Usage: " + usage);
      System.exit(1);
    }

    final File docDir = new File(docsPath);
    
    if (!docDir.exists() || !docDir.canRead()) {
      System.out.println("Document directory '" + docDir.getAbsolutePath() + 
    		  			 "' does not exist or is not readable, please check the path");
      System.exit(1);
    }
    
    
    Date start = new Date();
    try {
	      System.out.println("Indexing to directory '" + indexPath + "'...");
	      
	      Directory dir = FSDirectory.open(Paths.get(indexPath));
	      Analyzer analyzer = new CustomSpanishAnalyzer();
	      IndexWriterConfig iwc = new IndexWriterConfig(analyzer);

      
	      // Create a new index in the directory, removing any previously indexed documents:
	      iwc.setOpenMode(OpenMode.CREATE);

	      // Insertion of corpus documents in the index
          IndexWriter writer = new IndexWriter(dir, iwc);
          indexDocs(writer, docDir);
      
          // Show the total documents inserter
          System.out.println(docDir.list().length + " documents indexed");
          writer.close();

          Date end = new Date();
          // Show to total time spent in the insertion
          System.out.println(end.getTime() - start.getTime() + " total milliseconds");

    } 
    catch (IOException e) {
         System.out.println(" caught a " + e.getClass() + "\n with message: " + e.getMessage());
    }
}

  /**
   * Indexes the given file using the given writer, or if a directory is given,
   * recurses over files and directories found under the given directory.
   * 
   * NOTE: This method indexes one document per input file.  This is slow.  For good
   * throughput, put multiple documents into your input file(s).  An example of this is
   * in the benchmark module, which can create "line doc" files, one document per line,
   * using the
   * <a href="../../../../../contrib-benchmark/org/apache/lucene/benchmark/byTask/tasks/WriteLineDocTask.html"
   * >WriteLineDocTask</a>.
   *  
   * @param writer Writer to the index where the given file/dir info will be stored
   * @param file The file to index, or the directory to recurse into to find files to index
   * @throws IOException If there is a low-level I/O error
   */
  static void indexDocs(IndexWriter writer, File file)
    throws IOException {
    // do not try to index files that cannot be read
    if (file.canRead()) {
      if (file.isDirectory()) {
        String[] files = file.list();
        // an IO error could occur
        if (files != null) {
          for (int i = 0; i < files.length; i++) {
        	// Indexation of the i-doc in the index
            indexDocs(writer, new File(file, files[i]));
          }
        }
      } 
      else {
    	// Reading Flows
        FileInputStream fis;
        FileInputStream fisCopy;
        try {
          // Open the reading flows
          fis = new FileInputStream(file);
          fisCopy = new FileInputStream(file);
 
        } 
        catch (FileNotFoundException fnfe) {
          // at least on windows, some temporary files raise this exception with an "access denied" message
          // checking if the file can be read doesn't help
          return;
        }

        try {

          // make a new, empty document
          Document doc = new Document();

          // Added the path of the file as a field named "path"
          Field pathField = new StringField("path", file.getPath(), Field.Store.YES);
          doc.add(pathField);
          
          // Added the contents of the file to a field named "contents". 
          DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
          try {
              DocumentBuilder builder = factory.newDocumentBuilder();
              org.w3c.dom.Document document = builder.parse(fisCopy);
              Element element = document.getDocumentElement();
              NodeList nodes = element.getChildNodes();

              // Identificator of the tags of the file
              String id;
              for (int i = 0; i < nodes.getLength(); i++) {
                 id = nodes.item(i).getNodeName();
                 // Insertion of the textfields
                 // Control if the textfield is title, description or creator
                 if(id.contentEquals("dc:title") || 
                    id.contentEquals("dc:description") || 
                    id.contentEquals("dc:creator")) 
                 {
                	 // Ignore the "dc:" part from the textfield 
                	 id = id.substring(id.indexOf(":") + 1, id.length());
                	 // Add the content of the textfield
                	 doc.add(new TextField(id, nodes.item(i).getTextContent(), Field.Store.YES));
                 } 
                 // Control if the texfield is a date
                 else if(id.contentEquals("dc:date")){
                	 // Ignore the "dc:" part from the textfield 
                	 id = id.substring(id.indexOf(":") + 1, id.length());
                	 // Extract the date stored in the textfield
                	 String dateField = nodes.item(i).getTextContent();
                	 // Verification of whether the date is expressed as a range with the W3CDTF format
                	 if (dateField.contains("T")) {
                		 // Extraction of the dates of the range
                		 dateField = dateField.substring(0, dateField.indexOf("T"));
                		 dateField = dateField.substring(0, dateField.indexOf("-"));
                	 }
                	 // Add the textfield 
                	 doc.add(new TextField(id, dateField.replace("-", ""), Field.Store.YES));	 
                 }
                 // Control if the textfield is a type
                 else if(id.contentEquals("dc:type")) {
                	 // Ignore the "dc:" part from the textfield 
                	 id = id.substring(id.indexOf(":") + 1, id.length());
                	 String typeField = nodes.item(i).getTextContent().replace("info:eu-repo/semantics/", "");
                	 doc.add(new TextField(id, typeField, Field.Store.YES));
                 }
              }
          }
          catch(Exception e) {
        	  System.out.println(e.toString());
          }
          writer.addDocument(doc);

          
        } 
        finally {
          // Close the reading flows
          fis.close();
          fisCopy.close();
        }
      }
    }
  }
}