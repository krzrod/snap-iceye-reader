package com.iceye.esa.snap.dataio.util;

import it.geosolutions.imageio.plugins.tiff.TIFFField;
import it.geosolutions.imageioimpl.plugins.tiff.TIFFImageMetadata;
import org.w3c.dom.Document;
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
import java.util.Optional;
import java.util.stream.Stream;

/**
 * GDAL metadata utilities. Provides parsing and validation of a GDAL metadata document in XML format.
 *
 * @author krzysztof.rodak@scalosoft.com
 */
public class GdalUtil {

    public static final String GDALMETADATA_TAG = "GDALMetadata";

    private static DocumentBuilderFactory xmlBuilderFactory = null;
    private static DocumentBuilder xmlBuilder = null;

    /**
     *  Checks if GDAL metadata contains Item identified by give name, having given value. Case insensitive comparison.
     */
    public boolean checkItemValue(final String itemName, final String checkedValue, final Document gdalMetadata) {
        return findItemValue(itemName, gdalMetadata)
                .map(v -> v.equalsIgnoreCase(checkedValue)).orElse(false);
    }

    /**
     * Tries to retrieve a value of GDAL metadata Item, identified bt name.
     *
     * @param name Name of Item.
     * @param gdalMetadata GDAL metadata.
     * @return non-empty with Item's value if item was found and has a value, empty otherwise.
     */
    public Optional<String> findItemValue(String name, Document gdalMetadata) {
        try {
            XPath xPath = XPathFactory.newInstance().newXPath();
            String expression = MessageFormat.format("//Item[translate(@name, ''{1}'', ''{0}'')=''{0}'']/text()", name.toUpperCase(), name.toLowerCase());
            String value = (String) xPath.compile(expression).evaluate(gdalMetadata, XPathConstants.STRING);
            return Optional.ofNullable(value);
        }
        catch(XPathExpressionException e) {
            throw new RuntimeException(MessageFormat.format("Could eval XPath for given Item name: ''{0}''", name));
        }
    }

    public Optional<Document> findGdalMetadata(TIFFImageMetadata tiffMetadata) {
        return Stream.of(tiffMetadata.getRootIFD().getTIFFFields())
                .flatMap(this::toStringArray)
                .filter(GdalUtil::isValidGdalText)
                .map(this::parseXml)
                .filter(GdalUtil::isValidGdalXml)
                .findFirst();
    }

    private Stream<String> toStringArray(final TIFFField tiffField) {
        if (tiffField.getType() == 2 && tiffField.getData() instanceof String[] && ((String[]) tiffField.getData()).length > 0) {
            return Stream.of((String[]) tiffField.getData());
        }

        return Stream.empty();
    }

    /**
     * Creates a GDAL metadata representation as XML Document, from String.
     *
     * @param xmlString String representation of XML
     * @return parsed Document or null if xmlString was null.
     * @throws Exception Cannot parse xmlString into Document
     */
    private Document parseXml(final String xmlString) {
        try {
            if (xmlString == null) {
                return null;
            }

            return getXmlBuilder().parse(new InputSource(new StringReader(xmlString)));
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean isValidGdalXml(final Document gdal) {
        return gdal != null && gdal.getFirstChild() != null && gdal.getFirstChild().getChildNodes().getLength() > 0
                && gdal.getFirstChild().getNodeName().equals(GDALMETADATA_TAG);
    }


    /**
     * Fast operation to pre-qualify String for parsing.
     */
    private static boolean isValidGdalText(final String text) {
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
