import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.util.Hashtable;
import java.util.Map.Entry;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.text.EntityDefinition;
import org.apache.jena.query.text.TextDatasetFactory;
import org.apache.jena.query.text.TextIndexConfig;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.tdb2.TDB2Factory;
import org.apache.jena.util.FileManager;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.es.SpanishAnalyzer;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class SemanticSearcher {
	
	// Number of parameters
	private static int NUMBER_PARAMETERS = 6;
	
	private static Hashtable<String, String> infoNeeds = new Hashtable<>();

	public static void main(String[] args) {
		// Path of the parameters of the programm semantic searcher
		String rdfPath = null,  infoNeedsPath = null, resultsPath = null;
	
		
		// Check the number of parameters of the programm
		if (args.length != NUMBER_PARAMETERS) {
			System.err.println("Wrong amount of parameters");
			System.exit(1);
		}
		// Verification of the parameters
		for(int i = 0; i < args.length; i++) {
			if ("-rdf".equals(args[i])) {
				rdfPath = args[i+1];
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
			else {
				System.err.println("Parameter " + args[i] + " unrecognized");
				System.exit(2);
			}
		}
		
		// Read information needs file
		try {
			BufferedReader br = new BufferedReader(new FileReader(new File(infoNeedsPath)));
			String query;  
			while((query = br.readLine()) != null){
				infoNeeds.put(query.substring(0, query.indexOf(" ")), query.substring(query.indexOf(" ") + 1, query.length()));
			}
			br.close();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		
		
		// Load model
		Property titulo = ResourceFactory.createProperty("http://github.com/vpec/Recoreon/", "titulo");
		
		//definimos la configuración del repositorio indexado
		EntityDefinition entDef = new EntityDefinition("uri", "titulo", titulo);
		entDef.set("titulo", titulo.asNode());
		TextIndexConfig config = new TextIndexConfig(entDef);
	    config.setAnalyzer(new SpanishAnalyzer());
	    config.setQueryAnalyzer(new SpanishAnalyzer());
	    
	    //definimos el repositorio indexado todo en memoria
	    Dataset ds1 = DatasetFactory.createGeneral() ;
	    Directory dir =  new RAMDirectory();
	    Dataset ds = TextDatasetFactory.createLucene(ds1, dir, config) ;
		
	    // cargamos el fichero deseado y lo almacenamos en el repositorio indexado	  
        RDFDataMgr.read(ds.getDefaultModel(), rdfPath) ;
		
		try {
			FileWriter fileWriter = new FileWriter(resultsPath);
			PrintWriter printWriter = new PrintWriter(fileWriter);		    
			
			for(Entry<String, String> entry : infoNeeds.entrySet()) {
				System.out.println(entry.getKey());
				System.out.println(entry.getValue());
				Query query = QueryFactory.create(entry.getValue());
				QueryExecution qexec = QueryExecutionFactory.create(query, ds);
				try {
				    ResultSet results = qexec.execSelect() ;
				    for ( ; results.hasNext() ; ){
				      QuerySolution soln = results.nextSolution() ;
				      Resource uriDoc = soln.getResource("uriDoc");
				      printWriter.println(entry.getKey() + " " + uriDoc.getURI());
				    }
				}
				finally { 
					qexec.close(); 
				}
			}
			printWriter.close();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	    
		System.out.println("END OF PROGRAM");		
		
	}

}
