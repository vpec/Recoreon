import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Hashtable;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.util.ResourceUtils;
import org.w3c.dom.Element;
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
				 docResource.addProperty(tema, ResourceFactory.createResource(concepts.get(word)));
			 }
		 }
	}
	
	
	
	public static void readSkos(String skosPath, String prefix_skos) {
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
		        	String uri = prefix_skos + element.getElementsByTagName("skos:prefLabel").
		        			 item(0).getTextContent();
		        	concepts.put(element.getElementsByTagName("skos:prefLabel").
		        			 item(0).getTextContent(), uri);
		
		        	NodeList alternatives = element.getElementsByTagName("skos:altLabel");
		        	for (int j = 0; j < alternatives.getLength(); j++) {
			        	 concepts.put(alternatives.item(j).getTextContent(), uri);
		        	}
		         }
        }
		catch(Exception e) {
      	  	e.printStackTrace();
		}
		finally {
			try {
				fis.close();
			} 
			catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	
		/** Simple command-line based search demo. */
		public static void main(String[] args) throws Exception {
	
			// Path of the parameters of the programm semantic generator
			String rdfPath = null,  skosPath = null, owlPath = null, docsPath = null;
		
			
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
			
			// Creation of the properties of the model
			String prefix_skos = "http://github.com/vpec/Recoreon/skos#";
	        String prefix_rdf = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
	        String prefix_rdfs = "http://www.w3.org/2000/01/rdf-schema#";
	        String prefix_m = "http://github.com/vpec/Recoreon/";
	        String prefix_xsd = "http://www.w3.org/2001/XMLSchema#";
			
			// Fill the hash table of skos concepts
			readSkos(skosPath, prefix_skos);
			
			// Creation of an empty model
			Model model = ModelFactory.createDefaultModel();
			model.setNsPrefix("m", prefix_m);
			
			// Directory where the documents of the corpus are allocated
			File corpus = new File(docsPath);
			
			
	     	Property type = ResourceFactory.createProperty(prefix_rdf + "type");
	     	Property nombrePersona = ResourceFactory.createProperty(prefix_m + "nombrePersona");
	     	Property nombreOrganizacion = ResourceFactory.createProperty(prefix_m + "nombreOrganizacion");
	     	Property creador = ResourceFactory.createProperty(prefix_m + "creador");
	     	Property titulo = ResourceFactory.createProperty(prefix_m + "titulo");
	     	Property identificador = ResourceFactory.createProperty(prefix_m + "identificador");
	     	Property tema = ResourceFactory.createProperty(prefix_m + "tema");
	     	Property publicador = ResourceFactory.createProperty(prefix_m + "publicador");
	     	Property descripcion = ResourceFactory.createProperty(prefix_m + "descripcion");
	     	Property formato = ResourceFactory.createProperty(prefix_m + "formato");
	     	Property idioma = ResourceFactory.createProperty(prefix_m + "idioma");
	     	Property fecha = ResourceFactory.createProperty(prefix_m + "fecha");
	     	Property derechos = ResourceFactory.createProperty(prefix_m + "derechos");
	     	
	     	String string;
	     	for (String docPath : corpus.list()) {
	     		string = docPath;
	     		// Reading Flows
		     	FileInputStream fis;
		        try {
		           // Open the reading flows
		           fis = new FileInputStream(docsPath + "/" + docPath); 
			        try {
				          // Added the contents of the file to a field named "contents". 
				          DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
				          try {
				              DocumentBuilder builder = factory.newDocumentBuilder();
				              org.w3c.dom.Document document = builder.parse(fis);
						      document.getDocumentElement().normalize();
						      
						      NodeList nodes = document.getElementsByTagName("dc:identifier");
						      // Creation of the resource
				              Resource docResource  = model.createResource(nodes.item(0).getTextContent());
						      // Add identifier property to the resource 						
			                  docResource.addProperty(identificador, nodes.item(0).getTextContent());
						      
			                  
			                  nodes = document.getElementsByTagName("dc:title");
			                  if(nodes.getLength() > 0) {
							      // Add identifier property to the resource 						
				                  docResource.addProperty(titulo, nodes.item(0).getTextContent());
				                  addTopics(docResource, nodes.item(0).getTextContent(), tema);
			                  }
			                  
			                  
			                  nodes = document.getElementsByTagName("dc:type");
			                  if(nodes.getLength() > 0) {
				                  // Eliminates info:eu-repo/semantics from the type of the document
				                  String typeField = nodes.item(0).getTextContent().replace("info:eu-repo/semantics/", "");
				                  if (typeField.equals("bachelorThesis")) {
				                	// Bacherlor thesis document
				                    docResource.addProperty(type, ResourceFactory.createResource(prefix_m + "BachelorThesis"));
				                  }
				                  else {
				                    // Master thesis document
				                     docResource.addProperty(type, ResourceFactory.createResource(prefix_m + "MasterThesis"));
				                  }
			                  }
			                	 
			                  nodes = document.getElementsByTagName("dc:creator"); 
			                  for (int i = 0; i < nodes.getLength(); i++) {
			                	// Added the author of the document
				                     docResource.addProperty(creador, 
				                    	 model.createResource()
				                    	 	  .addProperty(type, ResourceFactory.createResource(prefix_m + "Persona"))
				                    	 	  .addProperty(nombrePersona, nodes.item(i).getTextContent()));
					          }
			                  
			                  nodes = document.getElementsByTagName("dc:publisher");
			                  if(nodes.getLength() > 0) {
			                	// Added the publisher of the document
				                     docResource.addProperty(publicador, 
				                    	 model.createResource()
				                    	 	  .addProperty(type, ResourceFactory.createResource(prefix_m + "Organización"))
				                    	 	  .addProperty(nombreOrganizacion, nodes.item(0).getTextContent()));
			                  }
				              
				                
				             nodes = document.getElementsByTagName("dc:description");
				             if(nodes.getLength() > 0) {
				            	 docResource.addProperty(descripcion, nodes.item(0).getTextContent());
				            	 addTopics(docResource, nodes.item(0).getTextContent(), tema);
				             }

				             nodes = document.getElementsByTagName("dc:format");
				             if(nodes.getLength() > 0) {
					             // Added the format of the document
					             docResource.addProperty(formato, ResourceFactory.createResource(prefix_m + "skos#pdf"));
				             }
				             
				             nodes = document.getElementsByTagName("dc:rights");
				             if(nodes.getLength() > 0) {
					             // Added the rights of the document
				                 docResource.addProperty(derechos, ResourceFactory.createResource(prefix_m + "skos#licencia"));
				             }
			                 
				             nodes = document.getElementsByTagName("dc:date");
				             if(nodes.getLength() > 0) {
					             String dateField = nodes.item(0).getTextContent();
			                	 // Verification of whether the date is expressed as a range with the W3CDTF format
			                	 if (dateField.contains("T")) {
			                		 // Extraction of the dates of the range
			                		 dateField = dateField.substring(0, dateField.indexOf("T"));
			                		 dateField = dateField.substring(0, dateField.indexOf("-"));
			                		 dateField = dateField.replace("-", "");
			                	 }
			                	 // Added the date of the document
			                	 Literal dateLiteral = model.createTypedLiteral(dateField, XSDDatatype.XSDgYear);
			                 	 docResource.addProperty(fecha, dateLiteral);
				             }
		                 	 
				             nodes = document.getElementsByTagName("dc:language");
				             if(nodes.getLength() > 0) {
					             Literal languageLiteral;
			                	 if (nodes.item(0).getTextContent().equals("spa")) {
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
		                	 
				             nodes = document.getElementsByTagName("dc:subject");
		                	 for (int i = 0; i < nodes.getLength(); i++) {
		                		// Added topics of the document
			                	 addTopics(docResource, nodes.item(i).getTextContent(), tema);
						     }
				          }
				          catch(Exception e) {
				        	  e.printStackTrace();
				          }
				        } 
				        finally {
				          // Close the reading flows
				          fis.close();
				        }
		        }
		        catch (FileNotFoundException fnfe) {
		        		fnfe.printStackTrace();
		        }
			}
	     	
	     	//lo guardamos en un fichero rdf en formato xml
			model.write(new FileOutputStream(new File(rdfPath)), "RDF/XML-ABBREV");
			
			System.out.println("END OF PROGRAM");
		}
}
