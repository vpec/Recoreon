import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Hashtable;
import java.util.Map.Entry;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.util.FileManager;
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
		Model model = FileManager.get().loadModel(rdfPath);
		
		try {
			FileWriter fileWriter = new FileWriter(resultsPath);
			PrintWriter printWriter = new PrintWriter(fileWriter);		    
			
			for(Entry<String, String> entry : infoNeeds.entrySet()) {
				System.out.println(entry.getKey());
				System.out.println(entry.getValue());
				Query query = QueryFactory.create(entry.getValue());
				QueryExecution qexec = QueryExecutionFactory.create(query, model);
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
