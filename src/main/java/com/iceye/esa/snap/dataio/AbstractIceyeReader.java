package com.iceye.esa.snap.dataio;

import org.esa.s1tbx.commons.io.SARReader;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;

import java.text.DateFormat;

public abstract class AbstractIceyeReader extends SARReader {

    protected final DateFormat standardDateFormat = ProductData.UTC.createDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    protected Product product;
    protected boolean isComplex = false;


    protected AbstractIceyeReader(ProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
    }

    protected abstract String getProductDescription();

    protected abstract void addMetadataToProduct();

    protected abstract void addBandsToProduct();

    protected abstract void addTiePointGridsToProduct();

    protected abstract void addOrbitStateVectors(MetadataElement absRoot);

    protected abstract void addGeoCodingToProduct();

    protected abstract String getSampleType();
}
