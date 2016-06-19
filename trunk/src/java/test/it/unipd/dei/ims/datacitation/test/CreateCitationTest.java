package it.unipd.dei.ims.datacitation.test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.xml.sax.SAXException;

import it.unipd.dei.ims.datacitation.buildcitation.PathMatcher;
import it.unipd.dei.ims.datacitation.buildcitation.ReferenceBuilder;
import it.unipd.dei.ims.datacitation.citationprocessing.PathProcessor;
import it.unipd.dei.ims.datacitation.config.InitDataCitation;

public class CreateCitationTest {

	private static String name = "ms001024";

//	private static File eadFile = new File(
//			"/Users/silvello/Documents/datacitation_collections/LoC2014_groundTruth/" + name + ".xml");

	private static File eadFile = new File("/Users/silvello/Desktop/ead_various/chicago4.xml");
	
	public static void main(String[] args) throws IOException, SAXException, ParserConfigurationException, XPathExpressionException {

		String xPathNode = "ead/archdesc[1]/dsc[1]/c[4]/did[1]/unittitle[1]";
		
		// load property file
		InitDataCitation sample = new InitDataCitation();
		sample.loadProperties();

		String delimiter = sample.getProperty("datacitation.citation.separator");
		
		PathProcessor p = new PathProcessor(xPathNode);

		PathMatcher match = new PathMatcher(p.getProcessedPath());

		ArrayList<String> paths = match.getCandidatePaths();

		ReferenceBuilder refB = new ReferenceBuilder(xPathNode, eadFile.getAbsolutePath(), paths);

		refB.buildReference();
		
		System.out.println(refB.getHumanReadableReference());

		System.out.println(refB.getMachineReadableReference());

	}

}
