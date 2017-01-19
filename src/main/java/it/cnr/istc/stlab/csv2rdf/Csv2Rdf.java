package it.cnr.istc.stlab.csv2rdf;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Option.Builder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.jena.propertytable.lang.CSV2RDF;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;
import it.cnr.istc.stlab.csv2rdf.csvgraph.GraphCSV;

public class Csv2Rdf {

	public static final String SEPARATOR_OPTION = "s";
	public static final String SEPARATOR_OPTIONS_LONG = "separator";
	
	public static final String OUTPUT_OPTION = "o";
	public static final String OUTPUT_OPTIONS_LONG = "output";
	
	public static final String NAMESPACE_OPTION = "n";
	public static final String NAMESPACE_OPTIONS_LONG = "namespace";
	
	public static final String MAPPING_OPTION = "m";
	public static final String MAPPING_OPTIONS_LONG = "mapping";
	
	public static void main(String[] args) {
		
		/*
         * Set-up the options for the command line parser.
         */
        Options options = new Options();
        
        Builder optionBuilder = Option.builder(SEPARATOR_OPTION);
        Option separatorOption = optionBuilder.argName("char")
                                 .desc("The character used as separator within the CSV file (e.g. , or ;).")
                                 .hasArg()
                                 .type(char.class)
                                 .required(false)
                                 .longOpt(SEPARATOR_OPTIONS_LONG)
                                 .build();
        
        optionBuilder = Option.builder(OUTPUT_OPTION);
        Option outputOption = optionBuilder.argName("file")
                .desc("RDF output file.")
                .hasArg()
                .required(false)
                .longOpt(OUTPUT_OPTIONS_LONG)
                .build();
        
        optionBuilder = Option.builder(NAMESPACE_OPTION);
        Option namespaceOption = optionBuilder.argName("uri")
                .desc("Namespace for resources.")
                .hasArg()
                .required(false)
                .longOpt(NAMESPACE_OPTIONS_LONG)
                .build();
        
        optionBuilder = Option.builder(MAPPING_OPTION);
        Option mappingOption = optionBuilder.argName("file")
                .desc("A file providing the mapping between CSV columns and the properties of a target ontology/vocabulary. "
                		+ ""
                		+ "" 
                		+ "Such a file must contain as set of key=value lines, where each key represents a column position in the source CSV (the counting of positions starts from index 1) and each value is a pair property-datatype composed of property URI form a target ontology or vocabulary and a datatype URI. The property-datatype pairs are separated by the character '>'. The datatype is optional, hence it is possible to provide the property URI only without any datatype. The following is an example that associates the properties http://xmlns.com/foaf/0.1/givenName and http://dbpedia.org/ontology/birthDate to the second and third columns of a given source CSV. Additionally, the example specifies that the values associated with the property http://dbpedia.org/ontology/birthDate have to be typed as http://www.w3.org/2001/XMLSchema#date:"
                		+ ""
                		+ "" 
                		+ "    2=http://xmlns.com/foaf/0.1/givenName" + '\n'
                		+ "    3=http://dbpedia.org/ontology/birthDate > http://www.w3.org/2001/XMLSchema#date")
                .hasArg()
                .required(false)
                .longOpt(MAPPING_OPTIONS_LONG)
                .build();
        
        options.addOption(separatorOption);
        options.addOption(outputOption);
        options.addOption(namespaceOption);
        options.addOption(mappingOption);
        
        CommandLine commandLine = null;
        
        CommandLineParser cmdLineParser = new DefaultParser();
        try {
            commandLine = cmdLineParser.parse(options, args);
        } catch (ParseException e) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp( "csv2rdf", options );
        }
        if(commandLine != null){
        	String[] arguments = commandLine.getArgs();
        	if(arguments.length == 0){
        		System.out.println("No file provided as input.");
        		System.exit(0);
        	}
        	String csvFile = commandLine.getArgs()[0];
        	String separator = commandLine.getOptionValue(SEPARATOR_OPTION);
        	char sepChar = ',';
        	
        	separator = StringEscapeUtils.unescapeJava(separator);
        	
        	if(separator != null)
        		sepChar = separator.charAt(0);
        	
        	CSV2RDF.init();
			Model model = ModelFactory.createDefaultModel();
			
			File file = null;
			
				if(sepChar != ','){
					try {
	    				file = File.createTempFile("tmp", ".csv");
	    				CSVReader reader = new CSVReader(new InputStreamReader(new FileInputStream(new File(csvFile).getAbsolutePath()), "UTF-8"), sepChar);
	    				CSVWriter writer = new CSVWriter(new FileWriter(file)); 
	    				String[] row = null;
	    			
	    				while((row = reader.readNext()) != null)
	    					writer.writeNext(row);
	    				
	    				reader.close();
	    				writer.close();
					} catch (IOException e) {
						System.out.println("An error occurred while reading file " + csvFile + ".");
	    			} 
    			}
				else{
					file = new File(csvFile);
				}
			
			
			
			if(file != null){
				
				String namespace = "http://purl.org/example/";
				
				if(commandLine.hasOption(NAMESPACE_OPTION)){
					namespace = commandLine.getOptionValue(NAMESPACE_OPTION);
					if(namespace == null)
						namespace = "http://purl.org/example/";
				}
				Properties mapping = null;
				if(commandLine.hasOption(MAPPING_OPTION)){
					
					String mappingFileName = commandLine.getOptionValue(MAPPING_OPTION);
					if(mappingFileName != null){
						mapping = new Properties();
						try {
							mapping.load(new FileInputStream(new File(mappingFileName)));
						} catch (IOException e) {
							System.out.print("An error occurred while reading the mapping file provided (i.e. the file named " + mappingFileName + ").");
						}
					}
						
						namespace = "http://purl.org/example/";
				}
				
				Model csv = ModelFactory.createModelForGraph(new GraphCSV(namespace, mapping, file.getPath())) ;
				
				
				OutputStream out = System.out;
				if(commandLine.hasOption(OUTPUT_OPTION)){
					try {
						String value = commandLine.getOptionValue(OUTPUT_OPTION);
						if(value != null)
							out = new FileOutputStream(new File(value));
						else out = System.out;
					} catch (FileNotFoundException e) {
						e.printStackTrace();
						out = System.out;
					}
				}
				model.add(csv.listStatements());
				
				/*
				final String ns = namespace;
				try {
					StmtIterator stmtIt = csv.listStatements();
					Model tmpModel = ModelFactory.createDefaultModel();
					
					String fakeNamespace = "file://" + new URI(file.getPath()) + "#";
					stmtIt.forEachRemaining(stmt -> {
						Resource subject = stmt.getSubject();
						Property predicate = stmt.getPredicate();
						RDFNode object = stmt.getObject();
						
						if(subject.isURIResource()){
							String subjectUri = subject.getURI().replace(fakeNamespace, ns);
							subject = tmpModel.createResource(subjectUri);
						}
						
						String predicateUri = predicate.getURI().replace(fakeNamespace, ns);
						predicate = tmpModel.createProperty(predicateUri);
						
						if(object.isURIResource()){
							String objectUri = ((Resource)object).getURI().replace(fakeNamespace, ns);
							object = tmpModel.createResource(objectUri);
						}
						
						model.add(subject, predicate, object);
					});
				} catch (URISyntaxException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				*/
				
				model.write(out, "TURTLE");
				
				file.delete();
			}
        	
			
        }
		
		
	}
}
