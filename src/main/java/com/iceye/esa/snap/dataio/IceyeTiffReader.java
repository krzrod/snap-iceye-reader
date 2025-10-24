package com.iceye.esa.snap.dataio;

import com.iceye.esa.snap.dataio.util.IceyeXConstants;
import org.esa.s1tbx.commons.io.ImageIOFile;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.MetadataElement;

import java.util.HashMap;
import java.util.Map;

public abstract class IceyeTiffReader extends AbstractIceyeReader {

    protected final Map<Band, ImageIOFile.BandInfo> bandMap = new HashMap<>(10);
    protected Map<String, String> tiffFields;

    public IceyeTiffReader(ProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
    }

    @Override
    protected String getProductDescription() {
        StringBuilder description = new StringBuilder();
        description.append(this.tiffFields.get(IceyeXConstants.PRODUCT.toUpperCase())).append(" - ");
        description.append(this.tiffFields.get(IceyeXConstants.PRODUCT_TYPE.toUpperCase())).append(" - ");
        description.append(this.tiffFields.get(IceyeXConstants.SPH_DESCRIPTOR.toUpperCase())).append(" - ");
        description.append(this.tiffFields.get(IceyeXConstants.MISSION.toUpperCase()));

        return description.toString();
    }

    protected void initReader() {
        product = null;
        tiffFields = null;
    }

    protected void addAttribute(MetadataElement meta, String name, String value) {
        MetadataAttribute attribute = new MetadataAttribute(name, 41, 1);
        if (value.isEmpty()) {
            value = " ";
        }
        attribute.getData().setElems(value);
        meta.addAttribute(attribute);
    }

    @Override
    protected String getSampleType() {
        if (IceyeXConstants.SLC.equalsIgnoreCase(tiffFields.get(IceyeXConstants.SPH_DESCRIPTOR.toUpperCase()))) {
            isComplex = true;
            return IceyeXConstants.COMPLEX;
        }
        isComplex = false;
        return IceyeXConstants.DETECTED;
    }
}
