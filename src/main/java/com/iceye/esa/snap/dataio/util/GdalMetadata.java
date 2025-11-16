package com.iceye.esa.snap.dataio.util;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.StringReader;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * GDAL metadata utilities. Provides parsing and validation of a GDAL metadata document in XML format.
 * GDAL metadata is formed by a collection of <Item> elements having "name" tag and a value.
 *
 * @author krzysztof.rodak@scalosoft.com
 */
public class GdalMetadata {

    public static final String GDALMETADATA_TAG = "GDALMetadata";

    private static DocumentBuilderFactory xmlBuilderFactory = null;
    private static DocumentBuilder xmlBuilder = null;

    private Document gdalMetadata;

    protected GdalMetadata(Document gdalMetadata) {
        this.gdalMetadata = gdalMetadata;
    }

    /**
     * Provides GDAL metadata as a flat String-String map.
     *
     * @return map of GDAL metadata
     */
    public Map<String, String> getAsFlatMap() {
        NodeList childNodes = this.gdalMetadata.getFirstChild().getChildNodes();
        final Map<String, String> valuesAsMap = new HashMap<>();
        for (int i = 1; i < childNodes.getLength(); i += 2) {
            valuesAsMap.put(childNodes.item(i).getAttributes().item(0).getNodeValue(), childNodes.item(i).getTextContent());
        }

        return valuesAsMap;
    }

    /**
     *  Checks if GDAL metadata contains Item identified by give name, having given value. Case insensitive comparison.
     */
    public boolean checkItemValue(final String itemName, final String value) {
        return findItemValue(itemName)
                .map(v -> v.equalsIgnoreCase(value)).orElse(false);
    }

    /**
     * Tries to retrieve a value of GDAL metadata Item, identified bt name.
     *
     * @param name Name of Item.
     * @return non-empty with Item's value if item was found and has a value, empty otherwise.
     */
    public Optional<String> findItemValue(final String name) {
        try {
            XPath xPath = XPathFactory.newInstance().newXPath();
            String expression = MessageFormat.format("//Item[translate(@name, ''{1}'', ''{0}'')=''{0}'']/text()", name.toUpperCase(), name.toLowerCase());
            String value = (String) xPath.compile(expression).evaluate(this.gdalMetadata, XPathConstants.STRING);
            return Optional.ofNullable(value);
        }
        catch(XPathExpressionException e) {
            throw new RuntimeException(MessageFormat.format("Could eval XPath for given Item name: ''{0}''", name));
        }
    }

    /**
     * Creates a GDAL metadata representation as XML Document, from String.
     *
     * @param xmlString String representation of XML
     * @return Metadata parsed from XML string or null if xmlString was null or invalid.
     * @throws Exception Cannot parse xmlString into Document
     */
    public static GdalMetadata fromString(final String xmlString) {
        GdalMetadata gdal = null;
        try {
            if (isValidGdalXmlText(xmlString)) {
                final Document gdalMetadata = getXmlBuilder().parse(new InputSource(new StringReader(xmlString)));
                gdal = new GdalMetadata(gdalMetadata);
            }
        } catch (Exception e) {
            ;
        }

        return gdal;
    }

    /**
     * Checks weather given document is valid GDAL metadata XML.
     *
     * @return <code>true</code> if document confirms to GDAL, <code>false</code> otherwise.
     */
    public boolean isValid() {
        return this.gdalMetadata != null && this.gdalMetadata.getFirstChild() != null && this.gdalMetadata.getFirstChild().getChildNodes().getLength() > 0
                && this.gdalMetadata.getFirstChild().getNodeName().equals(GDALMETADATA_TAG);
    }

    /**
     * Fast operation to pre-qualify String for parsing.
     *
     * @return <code>true</code> if given text conforms fo stringified GDAL metadata XML, <code>false</code> otherwise.
     */
    public static boolean isValidGdalXmlText(final String text) {
        if (text == null) {
            return false;
        }

        String xmlText = text.trim();
        return xmlText.startsWith('<' + GDALMETADATA_TAG + '>') && xmlText.endsWith("</" + GDALMETADATA_TAG + '>');
    }

    /** Wraps Document builder creation for lazy initialization -> it's a heavy resource */
    private static DocumentBuilder getXmlBuilder() throws ParserConfigurationException {
        if (xmlBuilderFactory == null) {
            xmlBuilderFactory =  DocumentBuilderFactory.newInstance();
        }

        if (xmlBuilderFactory != null && xmlBuilder == null) {
            xmlBuilder = xmlBuilderFactory.newDocumentBuilder();
        }

        if (xmlBuilder != null) {
            xmlBuilder.reset();
        }
        else {
            throw new IllegalStateException("Cannot instantiate XML Document builder.");
        }

        return xmlBuilder;
    }
}
