package de.hfu.tagont;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.StringTokenizer;

import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntDocumentManager;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.AnonId;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFWriter;
import com.hp.hpl.jena.rdf.model.impl.RDFDefaultErrorHandler;

/**
 * 
 * @author Torben
 *
 */
public class DeliciousImporter {
	
	//flag
	private static boolean cleanFilesFlag = false;
	
	//xml:base for the generated document
	public static final String XMLBASE = "http://bubb.ghb.fh-furtwangen.de/TagOnt/tags.rdf";
	public static final String XMLBASE2 = "http://bubb.ghb.fh-furtwangen.de/TagOnt/foaf.rdf";
	
	/**
	 * causes the tags.rdf file and foaf.rdf file to be overridden
	 */
	public static void cleanAllFiles() {
		cleanFilesFlag = true;
	}
	
	/**
	 * imports a del.icio.us feed from the given user to the tagging ontology.<br>
	 * the location of the feed is http://del.icio.us/rss/username.<br>
	 * the output will be written to the tags.rdf file in the current directory.
	 * 
	 * @param username the del.icio.us username of the user
	 * be overwritten
	 */
	public static synchronized void importTagsFromUser (String username) {
				
		//set up the model for the generated file
		OntModelSpec spec = new OntModelSpec(OntModelSpec.OWL_MEM);
		OntDocumentManager mgr = new OntDocumentManager();		
		mgr.addAltEntry(NS.TAG, "file:res/tagont.owl");
		mgr.addPrefixMapping(NS.FOAF, "foaf");
		mgr.addPrefixMapping(NS.TAG, "tag");
		mgr.setProcessImports(false);
		spec.setDocumentManager(mgr);
		
		//create / load models
		OntModel tagModel = ModelFactory.createOntologyModel(spec);
		if (new File("file:tags.rdf").exists() || !cleanFilesFlag) 
			tagModel.read("file:tags.rdf");
		else 
			tagModel.createOntology("").addImport(tagModel.createResource(NS.TAG));
		
		OntModel foafModel = ModelFactory.createOntologyModel(spec);
		if (new File("file:foaf.rdf").exists() || !cleanFilesFlag) 
			foafModel.read("file:foaf.rdf");
		else
			foafModel.createOntology("").addImport(tagModel.createResource(NS.TAG));
		
		//reset
		if (cleanFilesFlag) cleanFilesFlag = false;
		
		//get the model/graph of the delicious rss feed
		Model in_model = ModelFactory.createDefaultModel();
		RDFDefaultErrorHandler.silent = true; //skip warnings
		in_model.read("http://del.icio.us/rss/"+username);
						
		//define prefixes
		String prolog = 
				"PREFIX rss: <"+NS.RSS+">\n"+
				"PREFIX rdf: <"+NS.RDF+">\n" +
				"PREFIX dc: <"+NS.DC+">\n";
		
		//query for the user (i.e. the subject of the rss:channel element)
		String queryUser = prolog + 
				"SELECT ?user " +
				"WHERE {?user rdf:type rss:channel}";
		
		//retrieve the user and store it 
		QueryExecution qexec = QueryExecutionFactory.create(queryUser, in_model);
		try {
			//there is only one channel element
			ResultSet rs = qexec.execSelect();
			QuerySolution rb = rs.nextSolution();
			String userUrl = rb.get("user").toString(); 	//Will be http://del.icio.us/username 
			
			//return if already imported
			if (foafModel.contains(foafModel.createResource(userUrl), 
					foafModel.createProperty(NS.RDF+"type"), 
					foafModel.createResource(NS.FOAF+"Person"))) {
				System.out.println("user "+username+" already imported.");
				return;
			}
				
			//add detailed user info to foaf file
			Individual u = foafModel.createIndividual(userUrl, foafModel.createResource(NS.FOAF+"Person"));	
			u.addProperty(foafModel.createProperty(NS.FOAF+"name"), username);
			u.addProperty(foafModel.createProperty(NS.FOAF+"firstName"), username);
			u.addProperty(foafModel.createProperty(NS.FOAF+"homepage"), userUrl);
			
			//add reference to user in tags file
			tagModel.createIndividual(userUrl, tagModel.createResource(NS.FOAF+"Person"));
			
		} finally {
			qexec.close();
		}
		
		//query the user's tagging data
		String queryTaggingData = prolog + 
				"SELECT ?item ?title ?desc ?date ?tags\n" +
				"WHERE {?item rdf:type rss:item .\n" +
						"?item rss:title ?title .\n" +
						"?item rss:description ?desc .\n" +
						"?item dc:date ?date .\n" +
						"?item dc:subject ?tags}";
		
		//retrieve tagging data and store them
		qexec = QueryExecutionFactory.create(queryTaggingData, in_model);
		try {
			ResultSet rs = qexec.execSelect();
			while (rs.hasNext()) {
				QuerySolution rb = rs.nextSolution();
				String item = rb.get("item").toString();
				String title = rb.get("title").toString();
				String desc = rb.get("desc").toString();
				String date = rb.get("date").toString();
				String tags = rb.get("tags").toString();
//				System.out.println("_ " + item);
//				System.out.println("  " + title);
//				System.out.println("  " + desc);
//				System.out.println("  " + date);
//				System.out.println("  " + tags);
//				System.out.println();
				Individual tagging = tagModel.createIndividual("genid:"+AnonId.create().toString(), tagModel.createResource(NS.TAG+"Tagging"));
				tagging.addProperty(tagModel.createProperty(NS.TAG+"hasTaggedResource"), item);
				tagging.addProperty(tagModel.createProperty(NS.DC+"title"), title);
				tagging.addProperty(tagModel.createProperty(NS.DC+"description"), desc);
				tagging.addProperty(tagModel.createProperty(NS.TAG+"isTaggedOn"), date);
				StringTokenizer st = new StringTokenizer(tags);
				while (st.hasMoreTokens()){
					String tagName = st.nextToken();
					Individual tag = tagModel.createIndividual("http://del.icio.us/tag/"+tagName, tagModel.createResource(NS.TAG+"Tag"));
					tag.addProperty(tagModel.createProperty(NS.TAG+"hasTagLabel"), tagName);
					tag.addProperty(tagModel.createProperty(NS.TAG+"prefTagLabel"), tagName);
					tagging.addProperty(tagModel.createProperty(NS.TAG+"hasTag"), tag);
				}
				//connect taggings with a user
				tagModel.getIndividual("http://del.icio.us/"+username).addProperty(tagModel.createProperty(NS.TAG+"hasTagging"), tagging);
				//foafModel.getIndividual("http://del.icio.us/"+username).addProperty(foafModel.createProperty(NS.TAG+"hasTagging"), tagging);
				
			}
		} finally {
			qexec.close();
		}
		
		//write model to file
		try {
			//write tags.rdf file
			RDFWriter out_writer = tagModel.getWriter("RDF/XML-ABBREV");
			out_writer.setProperty("xmlbase", XMLBASE);
			out_writer.write(tagModel, new FileOutputStream(new File("tags.rdf")), null);
			
			//write foaf.rdf file
			out_writer = foafModel.getWriter("RDF/XML-ABBREV");
			out_writer.setProperty("xmlbase", XMLBASE2);
			out_writer.write(foafModel, new FileOutputStream(new File("foaf.rdf")), null);
			
			System.out.println("Successfully imported feed from "+username+"!");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

	}
}
