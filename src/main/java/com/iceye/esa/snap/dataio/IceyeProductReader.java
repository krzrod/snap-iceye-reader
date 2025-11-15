package com.iceye.esa.snap.dataio;

import com.bc.ceres.core.ProgressMonitor;
import com.iceye.esa.snap.dataio.util.GdalUtil;
import com.iceye.esa.snap.dataio.util.IceyeXConstants;
import com.iceye.esa.snap.dataio.util.TiffIOUtil;
import it.geosolutions.imageioimpl.plugins.tiff.TIFFImageMetadata;
import org.esa.s1tbx.commons.io.SARReader;
import org.esa.snap.core.dataio.IllegalFileFormatException;
import org.esa.snap.core.dataio.ProductIOException;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.util.io.FileUtils;
import org.esa.snap.engine_utilities.gpf.ReaderUtils;
import org.w3c.dom.Document;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.Optional;

import static org.esa.snap.core.util.SystemUtils.LOG;

/**
 * Main job of this class is to select proper implementation of a reader class, {@link AbstractIceyeReader},
 * and then relay all calls to this implementation. Class is selected based on file contents (metadata and extension)
 * or may be forced externally by setting VM arg e.g. -DiceyeReaderClass=com.iceye.esa.snap.dataio.IceyeGRDProductReader.
 *
 * @author Ahmad Hamouda, Krzysztof.Rodak@scalosoft.com
 */
public class IceyeProductReader extends SARReader {

    private static final String READER_CLASS_VM_ARG = "iceyeReaderClass";

    private AbstractIceyeReader reader;

    private final GdalUtil gdal = new GdalUtil();

    /**
     * Constructs a new abstract product reader.
     *
     * @param readerPlugIn the reader plug-in which created this reader, can be <code>null</code> for internal reader
     *                     implementations
     */
    public IceyeProductReader(final ProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
    }

    /**
     * Provides an implementation of the <code>readProductNodes</code> interface method. Clients implementing this
     * method can be sure that the input object and eventually the subset information has already been set.
     * <p/>
     * <p>This method is called as a last step in the <code>readProductNodes(input, subsetInfo)</code> method.
     */
    @Override
    protected Product readProductNodesImpl() {
        try {
            Path inputPath = ReaderUtils.getPathFromInput(getInput());
            if (inputPath == null) {
                throw new Exception("Unable to read " + getInput());
            }

            // change .json to .tif, change .xml to .h5 or .tif
            inputPath = resolveInputPath(inputPath);

            if (this.reader == null) {
                initReader(inputPath);
            }

            return this.reader.readProductNodes(inputPath.toFile(), getSubsetDef());

        } catch (Exception e) {
            LOG.severe(e.getMessage());
        }

        return null;
    }

    private void initReader(final Path inputPath) throws Exception {
        Class<?> readerCls = determineReaderClassOnVMArg();
        if (readerCls == null) {
            readerCls = determineReaderClassOnFile(inputPath);
        }

        if (IceyeSLCProductReader.class.equals(readerCls)) {
            LOG.info("Using ICEYE legacy SLC file format.");
            this.reader = new IceyeSLCProductReader(getReaderPlugIn());
        }
        else if (IceyeGRDProductReader.class.equals(readerCls)) {
            LOG.info("Using ICEYE legacy GRD file format.");
            this.reader = new IceyeGRDProductReader(getReaderPlugIn());
        }
        else if (IceyeGRDCogProductReader.class.equals(readerCls)) {
            LOG.info("Using ICEYE GRD-COG file format.");
            this.reader = new IceyeGRDCogProductReader(getReaderPlugIn());
        }
        else if (IceyeSLCCogProductReader.class.equals(readerCls)) {
            LOG.info("Using ICEYE SLC-COG file format.");
            this.reader = new IceyeSLCCogProductReader(getReaderPlugIn());
        }

        if (this.reader == null) {
            throw new IllegalFileFormatException("Unable to determine ICEYE file format.");
        }
    }

    private Class<?> determineReaderClassOnVMArg() {
        String vmArgReaderClassValue = System.getProperty(READER_CLASS_VM_ARG);
        try {
            return vmArgReaderClassValue != null ? Class.forName(vmArgReaderClassValue) : null;
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(MessageFormat.format("Invalid ICEYE reader class by VM arg ''{0}''.", vmArgReaderClassValue));
        }
    }

    private Class<?> determineReaderClassOnFile(final Path inputPath) throws IOException {
        final String fileName = inputPath.getFileName().toString().toLowerCase();
        if (fileName.endsWith(".h5")) {
            return IceyeSLCProductReader.class;
        }
        else if (fileName.endsWith(".tif")) {
            // read main GDAL metadata
            TIFFImageMetadata tiffMetadata = TiffIOUtil.findTIFFMetadata(0, inputPath)
                    .orElseThrow(() -> new IllegalFileFormatException(
                            "Missing TIFF metadata, file could not be interpreted by the reader."));

            // get GDAL xml out of TIFF metadata
            final Document gdalMetadata = gdal.findGdalMetadata(tiffMetadata)
                    .orElseThrow(() -> new IllegalFileFormatException(
                            "No ICEYE metadata found in the file."));

            if (gdal.checkItemValue(IceyeXConstants.SPH_DESCRIPTOR, IceyeXConstants.GRD, gdalMetadata)) {
                return IceyeGRDProductReader.class;
            }
            else if (gdal.checkItemValue(IceyeXConstants.PRODUCT_TYPE, IceyeXConstants.GRD_COG, gdalMetadata)) {
                return IceyeGRDCogProductReader.class;
            }
            else if (gdal.checkItemValue(IceyeXConstants.PRODUCT_TYPE, IceyeXConstants.SLC_COG, gdalMetadata)) {
                return IceyeSLCCogProductReader.class;
            }
        }

        return null;
    }

    private Path resolveInputPath(final Path inputPath) throws ProductIOException {
        File inputFile = inputPath.toFile();
        final String fileName = inputPath.getFileName().toString().toLowerCase();
        if (fileName.endsWith(".xml")) {
            inputFile = FileUtils.exchangeExtension(inputFile, ".h5");
            if (! inputFile.exists()) {
                inputFile = FileUtils.exchangeExtension(inputFile, ".tif");
            }
        } else if (fileName.endsWith(".json")) {
            // case if COG file is used with adjacent JSON holding its metadata ->
            // then change to 'tif' for both GRD and SLC
            inputFile = FileUtils.exchangeExtension(inputFile, ".tif");
        }

        if (! inputFile.exists()) {
            throw new ProductIOException("Product file cannot be resolved.");
        }

        return inputFile.toPath();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight,
                                          int sourceStepX, int sourceStepY, Band destBand, int destOffsetX,
                                          int destOffsetY, int destWidth, int destHeight, ProductData destBuffer,
                                          ProgressMonitor pm) throws IOException {
        if (this.reader == null) {
            throw new IllegalStateException("Reader was not initialized properly.");
        }

        this.reader.readBandRasterDataImpl(sourceOffsetX, sourceOffsetY, sourceWidth, sourceHeight,
                sourceStepX, sourceStepY, destBand, destOffsetX, destOffsetY, destWidth, destHeight, destBuffer, pm);
    }

    @Override
    public void close() throws IOException {
        if (reader != null) {
            reader.close();
        }
        reader = null;
        super.close();
    }

}