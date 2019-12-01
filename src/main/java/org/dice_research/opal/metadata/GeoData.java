package org.dice_research.opal.metadata;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;
import org.dice_research.opal.common.interfaces.JenaModelProcessor;
import org.dice_research.opal.common.vocabulary.Dcat;
import org.dice_research.opal.metadata.geo.GeoContainer;

import io.github.galbiston.geosparql_jena.implementation.datatype.WKTDatatype;

/**
 * Adds geo data of 8,495 places in Germany.
 * 
 * GeoData is added to dataset using property <http://purl.org/dc/terms/spatial>
 * to a node of type <http://purl.org/dc/terms/Location>. This node uses the
 * property <https://www.w3.org/TR/vocab-dcat-2/#Property:location_centroid> to
 * refer to a typed literal POINT of
 * <http://www.opengis.net/ont/geosparql#wktLiteral>.
 * 
 * Limitations:
 * 
 * - Used knowledge base uses a fuzzy-matching and was not validated
 * 
 * - Only places consisting of one word supported
 * 
 * @see https://www.w3.org/TR/vocab-dcat-2/#Property:dataset_spatial
 * @see https://www.w3.org/TR/vocab-dcat-2/#Class:Location
 * @see https://www.w3.org/TR/vocab-dcat-2/#Property:location_centroid
 * @see https://www.w3.org/TR/vocab-dcat-2/#ex-spatial-coverage-centroid
 *
 * @author Adrian Wilke
 */
public class GeoData implements JenaModelProcessor {

	public static final String PLACES_FILE = "places-germany.txt";
	protected static final boolean LABELS_TO_LOWER_CASE = false;

	protected static SortedMap<String, GeoContainer> geoContainers;

	@Override
	public Model process(Model model, String datasetUri) throws Exception {

		if (geoContainers == null) {
			initialize();
		}

		Resource dataset = ResourceFactory.createResource(datasetUri);

		// Process only if no spatial information exists
		if (model.listObjectsOfProperty(dataset, DCTerms.spatial).hasNext()) {
			return model;
		}

		StringBuilder stringBuilder = new StringBuilder();

		// Collect title(s)
		NodeIterator titleIterator = model.listObjectsOfProperty(dataset, DCTerms.title);
		while (titleIterator.hasNext()) {
			RDFNode titleNode = titleIterator.next();
			if (titleNode.isLiteral()) {
				String label = titleNode.asLiteral().getString();
				stringBuilder.append(LABELS_TO_LOWER_CASE ? label.toLowerCase() : label);
				stringBuilder.append(System.lineSeparator());
			}
		}

		// Collect description(s)
		NodeIterator decriptionIterator = model.listObjectsOfProperty(dataset, DCTerms.description);
		while (decriptionIterator.hasNext()) {
			RDFNode decriptionNode = decriptionIterator.next();
			if (decriptionNode.isLiteral()) {
				String label = decriptionNode.asLiteral().getString();
				stringBuilder.append(LABELS_TO_LOWER_CASE ? label.toLowerCase() : label);
				stringBuilder.append(System.lineSeparator());
			}
		}

		// Get lower-case words
		// Pattern pattern = Pattern.compile("\\b\\w{3,}"); // not in: 'weise'
		// Pattern pattern = Pattern.compile("\\w{3,}"); // contains 'blicherweise'
		// Pattern pattern = Pattern.compile("[a-z\\x7f-\\xff]{3,}"); // contains
		// 'üblicherweise'
		Pattern pattern;
		if (LABELS_TO_LOWER_CASE) {
			// Does also contain upper-case characters
			pattern = Pattern.compile("[a-z\\x7f-\\xff]{3,}");
		} else {
			pattern = Pattern.compile("[A-Za-z\\x7f-\\xff]{3,}");
		}
		Matcher matcher = pattern.matcher(stringBuilder);
		SortedSet<String> words = new TreeSet<>();
		while (matcher.find()) {
			words.add(matcher.group());
		}

		// Extract places
		List<String> places = new LinkedList<>();
		for (String word : words) {
			if (geoContainers.containsKey(word)) {
				places.add(word);
			}
		}

		// Add geo data
		for (String place : places) {
			GeoContainer container = geoContainers.get(place);
			// resource can be extended by an URI later.
			Resource placeResource = ResourceFactory.createResource();
			Literal wkt = ResourceFactory.createTypedLiteral("POINT(" + container.lat + " " + container.lon + ")",
					WKTDatatype.INSTANCE);
			model.add(placeResource, RDF.type, DCTerms.Location);
			model.add(placeResource, Dcat.centroid, wkt);
			model.add(dataset, DCTerms.spatial, placeResource);
		}

		return model;
	}

	/**
	 * Reads geo data.
	 * 
	 * @throws Exception On errors while reading.
	 */
	public void initialize() throws Exception {
		geoContainers = new TreeMap<>(new Comparator<String>() {
			@Override
			public int compare(String o1, String o2) {
				return GeoContainer.compare(o1, o2);
			}
		});

		File file = new File(this.getClass().getClassLoader().getResource(PLACES_FILE).toURI());
		try (BufferedReader bufferedReader = new BufferedReader(new FileReader(file))) {
			String line, label = null;
			int counter = 0;
			GeoContainer geoContainer = null;
			while ((line = bufferedReader.readLine()) != null) {
				if (counter == 0) {
					label = line;
					geoContainer = new GeoContainer();
				} else if (counter == 1) {
					geoContainer.lat = Float.valueOf(line);
				} else {
					geoContainer.lon = Float.valueOf(line);
					geoContainers.put(label, geoContainer);
				}
				counter = (counter + 1) % 3;
			}
		}
	}

}