package com.iceye.esa.snap.dataio.util;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.util.*;
import java.util.function.Supplier;

/**
 * Creates metadata map from GDAL GeoTIFF metadata in XML format, obtained by a supplier.
 *
 * Use supplier to select various sources e.g. internal TIFF metadata, external XML file, etc.
 *
 * @author krzysztof.rodak@scalosoft.com
 */
public class GdalXmlMetadataFactory {

    private Supplier<Document> gdalMetadataSupplier;

    public GdalXmlMetadataFactory(final Supplier<Document> gdalMetadataSupplier)  {
        this.gdalMetadataSupplier = gdalMetadataSupplier;
    }

    public Map<String, String> create() throws IceyeReaderException, IOException {

        Document gdalMetadata = gdalMetadataSupplier.get();

        if (! GdalUtil.isValidGdalXml(gdalMetadata)) {
            throw new IceyeReaderException("Cannot create metadata - invalid GDAL metadata was provided.");
        }

        NodeList childNodes = gdalMetadata.getFirstChild().getChildNodes();
        final Map<String, String> valuesAsMap = new HashMap<>();
        for (int i = 1; i < childNodes.getLength(); i += 2) {
            valuesAsMap.put(childNodes.item(i).getAttributes().item(0).getNodeValue(), childNodes.item(i).getTextContent());
        }

        return valuesAsMap;
    }

}
