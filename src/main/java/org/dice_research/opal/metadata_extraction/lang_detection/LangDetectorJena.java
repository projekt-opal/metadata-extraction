package org.dice_research.opal.metadata_extraction.lang_detection;

import java.io.IOException;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.DCAT;
import org.apache.jena.vocabulary.RDF;
import org.dice_research.opal.common.vocabulary.OpalLanguage;

import opennlp.tools.langdetect.Language;

/**
 * Language detection of datasets in Jena model.
 *
 * @author Adrian Wilke
 */
public class LangDetectorJena {

	private LangDetector langDetector = new LangDetector();

	/**
	 * Detects languages of metadata of datasets and adds them to model.
	 * 
	 * If the metadata is empty or if the language is not supported, no data will be
	 * added to model.
	 * 
	 * @throws IOException on errors reading the language model
	 */
	public Model addLanguage(Model model) throws IOException {

		// Duplicate model object
		model = ModelFactory.createDefaultModel().add(model);

		// Go through datasets
		ResIterator datasetIterator = model.listSubjectsWithProperty(RDF.type, DCAT.dataset);
		StringBuilder stringBuilder = new StringBuilder();
		while (datasetIterator.hasNext()) {
			RDFNode dataset = datasetIterator.next();

			// Collect natural language parts.
			// Assumes there is exactly one title/description.

			NodeIterator descriptionIterator = model.listObjectsOfProperty(dataset.asResource(),
					org.apache.jena.vocabulary.DCTerms.description);
			if (descriptionIterator.hasNext()) {
				stringBuilder.append(descriptionIterator.next());
			}

			NodeIterator titleIterator = model.listObjectsOfProperty(dataset.asResource(),
					org.apache.jena.vocabulary.DCTerms.title);
			if (titleIterator.hasNext()) {
				stringBuilder.append(System.lineSeparator());
				stringBuilder.append(titleIterator.next());
			}

			// Detect language of dataset and add it to model

			if (stringBuilder.length() != 0) {
				Language lang = langDetector.detectLanguage(stringBuilder.toString());
				Resource languageResource = getLanguageResource(lang.getLang());
				if (languageResource != null) {
					model.add(dataset.asResource(), OpalLanguage.language, languageResource);
				}
			}
		}

		return model;
	}

	/**
	 * Transforms ISO 639-3 code into ISO 639-1 URI
	 * 
	 * ISO 639-3 is used in OpenNLP model
	 * https://www.apache.org/dist/opennlp/models/langdetect/1.8.3/README.txt
	 * 
	 * @param iso639_3code ISO 639-3 code
	 * @return OPAL language resource (ISO 639-1 URI) or null
	 */
	private Resource getLanguageResource(String iso639_3code) {
		if (iso639_3code.equals(ISO_639_3.CODE_DEU)) {
			return OpalLanguage.LANGUAGE_DE;
		} else if (iso639_3code.equals(ISO_639_3.CODE_ENG)) {
			return OpalLanguage.LANGUAGE_EN;
		} else if (iso639_3code.equals(ISO_639_3.CODE_FRA)) {
			return OpalLanguage.LANGUAGE_FR;
		} else if (iso639_3code.equals(ISO_639_3.CODE_SPA)) {
			return OpalLanguage.LANGUAGE_ES;
		} else {
			return null;
		}

	}
}