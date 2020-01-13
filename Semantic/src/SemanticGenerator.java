import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.util.ResourceUtils;
import org.apache.jena.vocabulary.VCARD;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public class SemanticGenerator {
	
	// Number of parameters
	private static int NUMBER_PARAMETERS = 8;
	
	// Hash table that stores skos concepts
	private static Hashtable<String, String> concepts = new Hashtable<>();
	
	
	public static void addTopics(Resource docResource, String text, Property tema) {
		// Get the words of the title of the document
		 String[] words = text.split("\\s+");
		 // Iteration through the words
		 for (String word : words) {
			 // Look the word in the hash table
			 if (concepts.containsKey(word)) {
				 // Store the topic of the document
				 docResource.addProperty(tema, concepts.get(word));
			 }
		 }
	}
	
	
	
	public static void readSkos(String skosPath) {
		// Open the file of the skos concepts
		FileInputStream fis;
		try {
		      // Open the reading flows
		  fis = new FileInputStream(skosPath); 
		} 
		catch (FileNotFoundException fnfe) {
		  // at least on windows, some temporary files raise this exception with an "access denied" message
		  // checking if the file can be read doesn't help
		      return;
		}
		try {
			 DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		    
		         DocumentBuilder builder = factory.newDocumentBuilder();
		         org.w3c.dom.Document document = builder.parse(fis);
		         document.getDocumentElement().normalize();
		         NodeList nodes = document.getElementsByTagName("skos:Concept");
		         
		         for (int i = 0; i < nodes.getLength(); i++) {
		        	 Element element = (Element)nodes.item(i);
		        	 String uri = element.getNamespaceURI();
		        	 
		        	 concepts.put(element.getElementsByTagName("skos:prefLabel").
		        			 item(0).getTextContent(), uri);
		        	 
		        	NodeList alternatives = element.getElementsByTagName("skos:altLabel");
		        	for (int j = 0; j < alternatives.getLength(); j++) {
			        	 concepts.put(alternatives.item(j).getTextContent(), uri);
		        	}
		         }
        }
		catch(Exception e) {
      	  System.out.println(e.toString());
		}
	}
	
	
		/** Simple command-line based search demo. */
		public static void main(String[] args) throws Exception {
	
			// Path of the parameters of the programm semantic generator
			String rdfPath = null, skosPath = null, owlPath = null, docsPath = null;
		
			
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
				else if ("-skos".equals(args[i])) {
					skosPath = args[i+1];
					i++;
				}
				else if ("-owl".equals(args[i])) {
					owlPath = args[i+1];
					i++;
				}
				else if ("-docs".equals(args[i])) {
					docsPath = args[i+1];
					i++;
				}
				else {
					System.err.println("Parameter " + args[i] + " unrecognized");
					System.exit(2);
				}
			}
			
			// Fill the hash table of skos concepts
			readSkos(skosPath);
			
			// Creation of an empty model
			Model model = ModelFactory.createDefaultModel();
			
			// Directory where the documents of the corpus are allocated
			File corpus = new File(docsPath);
			
			// List will all the documents of the corpus
			String[] documents = corpus.list();	
			
			// Creation of the properties of the model
	        String prefix_rdf = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
	        String prefix_rdfs = "http://www.w3.org/2000/01/rdf-schema#";
	        String prefix_m = "http://github.com/vpec/Recoreon/";
	        String prefix_xsd = "http://www.w3.org/2001/XMLSchema#";
	
	     	Property type = model.createProperty(prefix_rdf + "type");
	
	     	Property nombrePersona = model.createProperty(prefix_m + "nombrePersona");
	     	Property nombreOrganizacion = model.createProperty(prefix_m + "nombreOrganizacion");
	     	Property creador = model.createProperty(prefix_m + "creador");
	     	Property titulo = model.createProperty(prefix_m + "titulo");
	     	Property identificador = model.createProperty(prefix_m + "identificador");
	     	Property tema = model.createProperty(prefix_m + "tema");
	     	Property publicador = model.createProperty(prefix_m + "publicador");
	     	Property descripcion = model.createProperty(prefix_m + "cescripcion");
	     	Property formato = model.createProperty(prefix_m + "formato");
	     	Property idioma = model.createProperty(prefix_m + "idioma");
	     	Property fecha = model.createProperty(prefix_m + "fecha");
	     	Property derechos = model.createProperty(prefix_m + "derechos");
	     	
	     	for (String docPath : corpus.list()) {
		     	// Reading Flows
		     	FileInputStream fis;
		        try {
		              // Open the reading flows
		              fis = new FileInputStream(docsPath); 
		            } 
		            catch (FileNotFoundException fnfe) {
		              // at least on windows, some temporary files raise this exception with an "access denied" message
		              // checking if the file can be read doesn't help
		              return;
		            }
		        	try {
			
		          // Added the contents of the file to a field named "contents". 
		          DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		          try {
		              DocumentBuilder builder = factory.newDocumentBuilder();
		              org.w3c.dom.Document document = builder.parse(fis);
		              Element element = document.getDocumentElement();
		              NodeList nodes = element.getChildNodes();
		              String id;
		              // Creation of the resource
		              Resource docResource  = model.createResource();
		              for (int i = 0; i < nodes.getLength(); i++) {
		                 id = nodes.item(i).getNodeName();
		                 if(id.contentEquals("dc:title")) {
		                	 // Add title property to the resource 
		                	 docResource.addProperty(titulo, nodes.item(i).getTextContent());
		                	 // Added topics
		                	 addTopics(docResource, nodes.item(i).getTextContent(), tema);
		                 } 
		                 else if(id.contentEquals("dc:identifier")) {
		                	 // Add identifier property to the resource 
		                	 docResource.addProperty(identificador, nodes.item(i).getTextContent());
		                	 // Set resource URI
		                	 ResourceUtils.renameResource(docResource, nodes.item(i).getTextContent());
		                 } 
		                 else if(id.contentEquals("dc:type")) {
		                	 // Eliminates info:eu-repo/semantics from the type of the document
		                	 String typeField = nodes.item(i).getTextContent().replace("info:eu-repo/semantics/", "");
		                	 if (typeField.equals("bachelorThesis")) {
		                		 // Bacherlor thesis document
		                    	 docResource.addProperty(type, prefix_m + "BachelorThesis");
		                	 }
		                	 else {
		                		 // Master thesis document
		                		 docResource.addProperty(type, prefix_m + "MasterThesis");
		                	 }
		                 }
		                 else if(id.contentEquals("dc:creator")) {                
		                	 // Added the author of the document
		                     docResource.addProperty(creador, 
		                    	 model.createResource()
		                    	 	  .addProperty(nombrePersona, nodes.item(i).getTextContent()));
		                 }
		                 else if (id.contentEquals("dc:publisher")) {
		                	// Added the publisher of the document
		                     docResource.addProperty(publicador, 
		                    	 model.createResource()
		                    	 	  .addProperty(nombreOrganizacion, nodes.item(i).getTextContent()));
		                 }
		                 else if (id.contentEquals("dc:description")) {
		                 	// Added the description of the document
		                    docResource.addProperty(descripcion, nodes.item(i).getTextContent());
		                 }
		                 else if (id.contentEquals("dc:format")) {
		                 	// Added the format of the document
		                    docResource.addProperty(formato, prefix_m + "skos#pdf");
		                 }
		                 else if (id.contentEquals("dc:rights")) {
		                	// Added the rights of the document
		                	docResource.addProperty(derechos, prefix_m + "skos#licencia");
		                 }
		                 else if (id.contentEquals("dc:date")) {
		                	 // Extract the date stored in the textfield
		                	 String dateField = nodes.item(i).getTextContent();
		                	 // Verification of whether the date is expressed as a range with the W3CDTF format
		                	 if (dateField.contains("T")) {
		                		 // Extraction of the dates of the range
		                		 dateField = dateField.substring(0, dateField.indexOf("T"));
		                		 dateField = dateField.substring(0, dateField.indexOf("-"));
		                		 dateField = dateField.replace("-", "");
		                	 }
		                	 // Added the date of the document
		                	 Literal dateLiteral = model.createTypedLiteral(dateField, XSDDatatype.XSDgYear);
		                 	 docResource.addProperty(fecha, dateField);
		                 }
		                 else if (id.contentEquals("dc:language")) {
		                	 Literal languageLiteral;
		                	 if (nodes.item(i).getTextContent().equals("spa")) {
		                		 // Documents in spanish language
		                    	 languageLiteral = model.createTypedLiteral("es", XSDDatatype.XSDlanguage);
		                	 }
		                	 else {
		                		 // Documents in english language
		                    	 languageLiteral = model.createTypedLiteral("en", XSDDatatype.XSDlanguage);
		                	 }
		                	 // Added language of the document
		                	 docResource.addProperty(idioma, languageLiteral);
		                 }
		                 else if (id.contentEquals("dc:subject")) {
		                	 // Added topics of the document
		                	 addTopics(docResource, nodes.item(i).getTextContent(), tema);
		                 }
		              }
		          }
		          catch(Exception e) {
		        	  System.out.println(e.toString());
		          }
		
		        } 
		        finally {
		          // Close the reading flows
		          fis.close();
		        }
			}
	     	
	     	//lo guardamos en un fichero rdf en formato xml
			model.write(new FileOutputStream(new File("rdfPath")), "RDF/XML-ABBREV");
		}
}
