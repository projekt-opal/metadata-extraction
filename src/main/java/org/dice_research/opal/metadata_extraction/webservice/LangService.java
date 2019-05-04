package org.dice_research.opal.metadata_extraction.webservice;

import javax.enterprise.context.RequestScoped;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.dice_research.opal.metadata_extraction.lang_detection.LangDetector;

/**
 * Webservice for language detection.
 *
 * @author Adrian Wilke
 */
@Path("lang")
@RequestScoped
public class LangService {

	/**
	 * Returns detected language of plain text. ISO 639-3 codes (3 characters) are
	 * returned.
	 * 
	 * e.g. http://localhost:9080/metadata/lang?text=Sprachen%20lernen
	 */
	@GET
	@Consumes(MediaType.TEXT_PLAIN)
	@Produces(MediaType.TEXT_PLAIN)
	public Response getIso369_3(@QueryParam("text") String text) {
		try {
			return Response.ok(new LangDetector().detectLanguageCode(text)).build();
		} catch (Throwable t) {
			System.err.println(t + " " + this.getClass().getName());
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}
}