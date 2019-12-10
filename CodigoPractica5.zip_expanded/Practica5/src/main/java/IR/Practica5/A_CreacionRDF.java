package IR.Practica5;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.VCARD;

/**
 * Ejemplo de como construir un modelo de Jena y añadir nuevos recursos 
 * mediante la clase Model
 */
public class A_CreacionRDF {
	
	/**
	 * muestra un modelo de jena de ejemplo por pantalla
	 */
	public static void main (String args[]) {
        Model model = A_CreacionRDF.generarEjemplo();
        // write the model in the standar output
        model.write(System.out, "TURTLE"); 
    }
	
	/**
	 * Genera un modelo de jena de ejemplo
	 */
	public static Model generarEjemplo(){
		// definiciones
        String personURI    = "http://somewhere/JohnSmith";
        String givenName    = "John";
        String familyName   = "Smith";
        String fullName     = givenName + " " + familyName;
        
        // New person
        String personURI2    = "http://somewhere/JohnLennon";
        String givenName2    = "John";
        String familyName2   = "Lennon";
        String fullName2     = givenName2 + " " + familyName2;
        
        // Another person
        String personURI3    = "http://somewhere/JohnCena";
        String givenName3    = "John";
        String familyName3   = "Cena";
        String fullName3     = givenName3 + " " + familyName3;

        // crea un modelo vacio
        Model model = ModelFactory.createDefaultModel();
        
        String url1 = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";
        String url2 = "http://xmlns.com/foaf/0.1/person";
        Property p1 = ResourceFactory.createProperty(url1);
        Property p2 = ResourceFactory.createProperty(url2);
        
        // Knows property
        Property propertyKnows = ResourceFactory.createProperty("http://xmlns.com/foaf/0.1/knows");

        // le a�ade las propiedades
        Resource johnSmith  = model.createResource(personURI)
             .addProperty(VCARD.FN, fullName)
             .addProperty(VCARD.N, 
                      model.createResource()
                           .addProperty(VCARD.Given, givenName)
                           .addProperty(VCARD.Family, familyName))
             .addProperty(p1, p2); // Specify that johnSmith is a person
        
        Resource johnLennon  = model.createResource(personURI2)
                .addProperty(VCARD.FN, fullName2)
                .addProperty(VCARD.N, 
                         model.createResource()
                              .addProperty(VCARD.Given, givenName2)
                              .addProperty(VCARD.Family, familyName2));
        // Specify that johnCena knows John Lennon
        Resource johnCena  = model.createResource(personURI3)
                .addProperty(VCARD.FN, fullName3)
                .addProperty(VCARD.N, 
                         model.createResource()
                              .addProperty(VCARD.Given, givenName3)
                              .addProperty(VCARD.Family, familyName3))
                .addProperty(propertyKnows, johnLennon);
        // And viceversa
        johnLennon.addProperty(propertyKnows, johnCena);
        
        // Create new statement (john smith knows john lennon)
        Statement statement = ResourceFactory.createStatement(johnSmith, propertyKnows, johnLennon);
        model.add(statement);
        
        return model;
	}
	
	
}
