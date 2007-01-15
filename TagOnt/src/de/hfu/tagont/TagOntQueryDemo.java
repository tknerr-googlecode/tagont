package de.hfu.tagont;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

import com.hp.hpl.jena.ontology.OntDocumentManager;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

public class TagOntQueryDemo {
	
	//common prolog
	private String prolog = 
		"PREFIX tag: <"+NS.TAG+">\n" +
		"PREFIX rdf: <"+NS.RDF+">\n" +
		"PREFIX foaf: <"+NS.FOAF+">\n";
	
	
	public static void doImportTagsFromDelicious() {
		//overwrite or append?
		//DeliciousImporter.cleanAllFiles();
		
		//Import tags from users (randomly chosen)
		DeliciousImporter.importTagsFromUser("baokd");
		DeliciousImporter.importTagsFromUser("drastikat");
		DeliciousImporter.importTagsFromUser("thefalcon");
		DeliciousImporter.importTagsFromUser("tamquam");
		DeliciousImporter.importTagsFromUser("panh29");
		DeliciousImporter.importTagsFromUser("csonic");
		
		DeliciousImporter.importTagsFromUser("mahei");
		DeliciousImporter.importTagsFromUser("mindful_geek");
		DeliciousImporter.importTagsFromUser("simon_larkin");
		DeliciousImporter.importTagsFromUser("vanderzanden");
		DeliciousImporter.importTagsFromUser("44sunsets");
		
		DeliciousImporter.importTagsFromUser("seppevs");
		DeliciousImporter.importTagsFromUser("andreia");
		DeliciousImporter.importTagsFromUser("soundguy");
		DeliciousImporter.importTagsFromUser("corymyers");
		DeliciousImporter.importTagsFromUser("xioster");
		
		DeliciousImporter.importTagsFromUser("varanusz");
		DeliciousImporter.importTagsFromUser("cjc");
		DeliciousImporter.importTagsFromUser("aminch");
		
		System.out.println("-------- Imports finished ---------");
	}
	
	public static void main(String[] args) throws InterruptedException {
		
		//import tags
		//doImportTagsFromDelicious();
		
		//read the models from the combination of the two files
		OntModelSpec spec = new OntModelSpec(OntModelSpec.OWL_MEM);
		OntDocumentManager mgr = new OntDocumentManager();		
		mgr.addAltEntry(NS.TAG, "file:res/tagont.owl");
		mgr.addPrefixMapping(NS.FOAF, "foaf");
		mgr.addPrefixMapping(NS.TAG, "tag");
		mgr.setProcessImports(false);
		spec.setDocumentManager(mgr);
		OntModel model = ModelFactory.createOntologyModel(spec);
		model.read("file:tags.rdf");
		model.read("file:foaf.rdf");
		
		//kick off some demo queries
		TagOntQueryDemo demo = new TagOntQueryDemo();
		demo.getAllTaggers(model);
		demo.getAllTagsFromTagger(model, "joshua");
		demo.getAllResourcesTaggedByMultipleUsers(model);
		demo.getAllTagsForResource(model, "http://performancing.com/node/3422");
		demo.getTagsFromResourcePerUser(model, "http://performancing.com/node/3422", 
				new String[]{"mahei", "mindful_geek", "vanderzanden", "simon_larkin", "44sunsets"});
	}	

	
	
	/**
	 * get all foaf:Persons who are tagging
	 * @param model
	 */
	public void getAllTaggers(Model model) {	
		System.out.println();
		System.out.println("TagOntQueryDemo.getAllTaggers()");		
		System.out.println("-------------------------------");
		
		String queryString = prolog + 
				"SELECT DISTINCT ?p ?name ?fname ?lname " +
				"WHERE {" +
				"	?p tag:hasTagging ?x ." +
				"	OPTIONAL {?p foaf:name ?name} ." +
				"	OPTIONAL {?p foaf:firstName ?fname} ." +
				"	OPTIONAL {?p foaf:surname ?lname} " +
				"}";
		
		QueryExecution qexec = QueryExecutionFactory.create(queryString, model);
		try {
			ResultSet rs = qexec.execSelect();
			while (rs.hasNext()) {
				QuerySolution rb = (QuerySolution) rs.next();
				if (rb.get("p")!=null) System.out.println(""+rb.get("p"));
				if (rb.get("name")!=null) System.out.println("  name: "+rb.get("name").asNode().getLiteralLexicalForm());
				if (rb.get("fname")!=null) System.out.println("  firstname: "+rb.get("fname").asNode().getLiteralLexicalForm());
				if (rb.get("lname")!=null) System.out.println("  lastname: "+rb.get("lname").asNode().getLiteralLexicalForm());
			}
		} finally {
			qexec.close();
		}
	}
	
	/**
	 * get all tags being used by user with username
	 * @param model
	 */
	public void getAllTagsFromTagger(Model model, String username) {	
		System.out.println();
		System.out.println("TagOntQueryDemo.getAllTagsFromTagger("+username+")");		
		System.out.println("-----------------------------------------------------");
		
		String queryString = prolog + 
				"SELECT DISTINCT ?tag ?tagurl " +
				"WHERE {" +
				"	?user tag:hasTagging ?x ." +
				"	?user foaf:name '"+username+"' ." +
				"	?x tag:hasTag ?tagurl ." +
				"	?tagurl tag:prefTagLabel ?tag" +
				"}" +
				"ORDER BY ?tag";
		
		QueryExecution qexec = QueryExecutionFactory.create(queryString, model);
		try {
			ResultSet rs = qexec.execSelect();
			while (rs.hasNext()) {
				QuerySolution rb = (QuerySolution) rs.next();
				if (rb.get("tag")!=null) System.out.println(""+rb.get("tag") + "\t[" + rb.get("tagurl") + "]");
			}
		} finally {
			qexec.close();
		}
	}
	
	
	/**
	 * get all user's tags for the given resource
	 * @param model
	 */
	public void getAllTagsForResource(Model model, String resourceURL) {	
		System.out.println();
		System.out.println("TagOntQueryDemo.getAllTagsForResource("+resourceURL+")");		
		System.out.println("-----------------------------------------------------");
		
		String queryString = prolog + 
				"SELECT DISTINCT ?tag ?tagurl " +
				"WHERE {" +
				"	?x tag:hasTaggedResource '"+resourceURL+"' ." +
				"	?x tag:hasTag ?tagurl ." +
				"	?tagurl tag:prefTagLabel ?tag" +
				"}" +
				"ORDER BY ?tag";
		
		QueryExecution qexec = QueryExecutionFactory.create(queryString, model);
		try {
			ResultSet rs = qexec.execSelect();
			System.out.print("all tags: \t{");
			while (rs.hasNext()) {
				QuerySolution rb = (QuerySolution) rs.next();
				if (rb.get("tag")!=null) System.out.print(""+rb.get("tag"));
				if (rs.hasNext()) System.out.print(", ");
				//if (rb.get("tag")!=null) System.out.println(""+rb.get("tag") + "\t[" + rb.get("tagurl") + "]");
			}
			System.out.println("}");
		} finally {
			qexec.close();
		}
	}

	/**
	 * gets the set of tags assigned to a resource per user
	 * @param model
	 */
	public void getTagsFromResourcePerUser(Model model, String resourceURL, String[] usernames) {	
		System.out.println();
		System.out.println("TagOntQueryDemo.getTagsFromResourcePerUser("+resourceURL+")");		
		System.out.println("-----------------------------------------------------");
		
		for (int i=0; i<usernames.length; i++) {
			String queryString = prolog + 
					"SELECT ?tag " +
					"WHERE {" +
					"	?user foaf:name '"+usernames[i]+"' ." +
					"	?user tag:hasTagging ?x ." +
					"	?x tag:hasTaggedResource '"+resourceURL+"' ." +
					"	?x tag:hasTag ?tagurl ." +
					"	?tagurl tag:prefTagLabel ?tag" +
					"}" +
					"ORDER BY ?tag ?user";
			
			QueryExecution qexec = QueryExecutionFactory.create(queryString, model);
			try {
				ResultSet rs = qexec.execSelect();
				System.out.print("tags from "+usernames[i]+":\t{");
				while (rs.hasNext()) {
					QuerySolution rb = (QuerySolution) rs.next();
					if (rb.get("tag")!=null) System.out.print(rb.get("tag"));
					if (rs.hasNext()) System.out.print(", ");
				}
				System.out.println("}");
			} finally {
				qexec.close();
			}
		}
	}
	
	/**
	 * get all resources which have been tagged by at least two users
	 * @param model
	 */
	public void getAllResourcesTaggedByMultipleUsers(Model model) {	
		System.out.println();
		System.out.println("TagOntQueryDemo.getAllResourcesTaggedByMultipleUsers()");		
		System.out.println("------------------------------------------------------");
		
		String queryString = prolog + 
				"SELECT ?res ?user " +
				"WHERE {" +
				"	?user tag:hasTagging ?tagging ." +
				"	?tagging tag:hasTaggedResource ?res ." +
				"}";
		
		QueryExecution qexec = QueryExecutionFactory.create(queryString, model);
		try {
			ResultSet rs = qexec.execSelect();
			Hashtable ht = new Hashtable();
			while (rs.hasNext()) {
				QuerySolution rb = (QuerySolution) rs.next();
				String res = rb.get("res").toString();
				String user = rb.get("user").toString();
				if (res!=null && user!=null) {
					if (ht.containsKey(res)) {
						Vector v = (Vector) ht.get(res);
						v.add(user);
						ht.put(res, v);
					} else {
						Vector v = new Vector();
						v.add(user);
						ht.put(res, v);
					}
				}
			}
			Enumeration keys = ht.keys();
			while (keys.hasMoreElements()) {
				String res = (String) keys.nextElement();
				Vector v = (Vector) ht.get(res);
				if (v.size()>1) {
					String users = "";
					for (int i=0; i<v.size(); i++) {
						users += ((String)v.elementAt(i)).substring(19) + ", ";
					}
					if (users.endsWith(", ")) users = users.substring(0, users.length()-2);
					System.out.println(res + "\t("+users+")");
				}
			}
		} finally {
			qexec.close();
		}
	}
}
