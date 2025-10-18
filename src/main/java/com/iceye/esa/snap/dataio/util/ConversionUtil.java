package com.iceye.esa.snap.dataio.util;

import org.esa.snap.core.util.SystemUtils;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.Arrays;

public final class ConversionUtil {

    public static Document convertStringToXMLDocument(String xmlString) {
        //Parser that produces DOM object trees from XML content
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        //API to obtain DOM Document instance
        DocumentBuilder builder = null;
        try {
            //Create DocumentBuilder with default configuration
            builder = factory.newDocumentBuilder();

            //Parse the content to Document object
            return builder.parse(new InputSource(new StringReader(xmlString)));
        } catch (Exception e) {
            SystemUtils.LOG.severe(e.getMessage());
        }
        return null;
    }

    public static double[] convertStringToDoubleArray(String string) {
        return Arrays.stream(string.substring(1, string.length() - 1).trim().split(",")).mapToDouble(Double::parseDouble).toArray();
    }

    public static double[] convertStringToDoubleArrayBySpace(String string) {
        return Arrays.stream(string.replace("\n", " ").replaceAll("\\s+", " ").replace("  ", " ").replace("[", "").replace("]", "").trim().split(" ")).mapToDouble(Double::parseDouble).toArray();
    }

    public static String[] convertDateStringToStringArray(String string) {
        return string.substring(1, string.length() - 1).replace("'", "").trim().split(",");
    }

    public static String[] convertDateStringToStringArrayBySpace(String string) {
        return string.replace("\n", " ").replaceAll("\\s+", " ").replace("  ", " ").replace("[", "").replace("]", "").trim().split(" ");
    }
}
