import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Hashtable;
import java.util.Map.Entry;


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
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.lucene.analysis.es.SpanishAnalyzer;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;

public class SemanticSearcher {
	
	// Number of parameters
	private static int NUMBER_PARAMETERS = 6;
	
	private static Hashtable<String, String> infoNeeds = new Hashtable<>();

	public static void main(String[] args) {
		// Path of the parameters of the program semantic searcher
		String rdfPath = null,  infoNeedsPath = null, resultsPath = null;
	
		
		// Check the number of parameters of the program
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
		
		String startingString = "oai_zaguan.unizar.es_";
		String endingString = ".xml";
		
		// Define properties
		Property titulo = ResourceFactory.createProperty("http://github.com/vpec/Recoreon/", "titulo");
		Property descripcion = ResourceFactory.createProperty("http://github.com/vpec/Recoreon/", "descripcion");
		
		// Repository configuration
		EntityDefinition entDef = new EntityDefinition("uri", "titulo", titulo);
		// Create index on titles
		entDef.set("titulo", titulo.asNode());
		// Create index on descriptions
		entDef.set("descripcion", descripcion.asNode());
		// Repository configuration
		TextIndexConfig config = new TextIndexConfig(entDef);
	    config.setAnalyzer(new SpanishAnalyzer());
	    config.setQueryAnalyzer(new SpanishAnalyzer());
	    config.setMultilingualSupport(true);
	    
	    // Define in-memory index
	    Dataset ds1 = DatasetFactory.createGeneral() ;
	    Directory dir =  new RAMDirectory();
	    Dataset ds = TextDatasetFactory.createLucene(ds1, dir, config) ;
		
	    // Load file and store it in created index  
        RDFDataMgr.read(ds.getDefaultModel(), rdfPath) ;
		
		try {
			// Prepare results' file to write
			FileWriter fileWriter = new FileWriter(resultsPath);
			PrintWriter printWriter = new PrintWriter(fileWriter);		    
			
			for(Entry<String, String> entry : infoNeeds.entrySet()) {
				// Execute queries
				Query query = QueryFactory.create(entry.getValue());
				QueryExecution qexec = QueryExecutionFactory.create(query, ds);
				try {
				    ResultSet results = qexec.execSelect() ;
				    for ( ; results.hasNext() ; ){
				      QuerySolution soln = results.nextSolution() ;
				      if(soln.getResource("uriDoc") != null) {
				    	  String uriDoc = soln.getResource("uriDoc").getURI();
					      String[] parts = uriDoc.split("/");
					      // Write result to file
					      printWriter.println(entry.getKey() + " " + startingString + parts[parts.length - 1] + endingString);
				      }
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
