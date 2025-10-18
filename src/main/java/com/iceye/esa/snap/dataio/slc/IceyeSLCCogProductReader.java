package com.iceye.esa.snap.dataio.slc;

import com.bc.ceres.core.ProgressMonitor;
import com.iceye.esa.snap.dataio.IceyeTiffReader;
import com.iceye.esa.snap.dataio.model.JsonMetadataWrapper;
import com.iceye.esa.snap.dataio.util.IceyeXConstants;
import com.iceye.esa.snap.dataio.util.JsonGRDHelper;
import com.iceye.esa.snap.dataio.util.JsonReader;
import org.esa.s1tbx.commons.io.ImageIOFile;
import org.esa.s1tbx.commons.io.SARReader;
import org.esa.s1tbx.commons.product.Missions;
import org.esa.s1tbx.io.geotiffxml.GeoTiffUtils;
import org.esa.snap.core.dataio.IllegalFileFormatException;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.core.util.io.FileUtils;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.datamodel.Unit;
import org.esa.snap.engine_utilities.eo.Constants;
import org.esa.snap.engine_utilities.gpf.OperatorUtils;
import org.esa.snap.engine_utilities.gpf.ReaderUtils;

import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import static com.iceye.esa.snap.dataio.util.ConversionUtil.*;

public class IceyeSLCCogProductReader extends IceyeTiffReader {

    private final Object lock = new Object();

    private static final Logger LOG = Logger.getLogger(IceyeSLCCogProductReader.class.getName());

    public IceyeSLCCogProductReader(final ProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
    }

    @Override
    protected void initReader() {
        product = null;
        tiffFields = new HashMap<>();
        bandMap.clear();
    }

    @Override
    protected Product readProductNodesImpl() throws IOException {
        initReader();
        try {
            final Path inputPath = getPathFromInput(getInput());
            if (inputPath == null) {
                close();
                throw new IllegalFileFormatException("File Could not be interpreted by the reader.");
            }

            final File inputFile = inputPath.toFile();

            JsonMetadataWrapper jsonMetadata = getJsonMetadata(inputPath);

            if (jsonMetadata == null) {
                close();
                throw new IllegalFileFormatException("File metadata Could not be interpreted by the reader.");
            }

            processMetadataJson(jsonMetadata);

            final String productType = this.tiffFields.get(IceyeXConstants.PRODUCT_TYPE.toUpperCase());
            final int rasterWidth = Integer.parseInt(this.tiffFields.get(IceyeXConstants.NUM_SAMPLES_PER_LINE.toUpperCase()));
            final int rasterHeight = Integer.parseInt(this.tiffFields.get(IceyeXConstants.NUM_OUTPUT_LINES.toUpperCase()));

            product = new Product(inputFile.getName(),
                    productType,
                    rasterWidth, rasterHeight,
                    this);
            product.setFileLocation(inputFile);
            product.setDescription(getProductDescription());
            product.setStartTime(ProductData.UTC.parse(this.tiffFields.get(IceyeXConstants.ACQUISITION_START_UTC.toUpperCase()), standardDateFormat));
            product.setEndTime(ProductData.UTC.parse(this.tiffFields.get(IceyeXConstants.ACQUISITION_END_UTC.toUpperCase()), standardDateFormat));

            addMetadataToProduct();
            addBandsToProduct();
            addTiePointGridsToProduct();
            addGeoCodingToProduct();
            addCommonSARMetadata(product);
            addDopplerMetadata();

            product.getGcpGroup();
            product.setModified(false);
            setQuicklookBandName(product);
            addQuicklook(product, inputFile.getName(), inputFile);

            return product;
        } catch (Exception e) {
            SystemUtils.LOG.severe(e.getMessage());
        }
        return null;
    }

    private JsonMetadataWrapper getJsonMetadata(Path inputPath) {
        File file = FileUtils.exchangeExtension(inputPath.toFile(), ".json");
        Path jsonPath = file.toPath();
        return JsonReader.readMetadata(jsonPath);
    }

    private void processMetadataJson(JsonMetadataWrapper metadataWrapper) {
        JsonGRDHelper.mapJsonMetadataToTiffFields(metadataWrapper, tiffFields);
    }

    @Override
    protected void addBandsToProduct() {
        try {
            final File inputFile = SARReader.getPathFromInput(getInput()).toFile();
            String imgPath = inputFile.getPath();
            final String name = imgPath.substring(imgPath.lastIndexOf('/') + 1).toLowerCase();
            final int rasterWidth = Integer.parseInt(this.tiffFields.get(IceyeXConstants.NUM_SAMPLES_PER_LINE.toUpperCase()));
            final int rasterHeight = Integer.parseInt(this.tiffFields.get(IceyeXConstants.NUM_OUTPUT_LINES.toUpperCase()));

            InputStream inStream = new BufferedInputStream(Files.newInputStream(inputFile.toPath()));
            ImageInputStream imgStream = ImageIOFile.createImageInputStream(inStream, new Dimension(rasterWidth, rasterHeight));
            final ImageReader reader = GeoTiffUtils.getTiffIIOReader(imgStream);
            final ImageIOFile img = new ImageIOFile(name, imgStream, reader, inputFile);

            String polarization = tiffFields.get(IceyeXConstants.MDS1_TX_RX_POLAR.toUpperCase());
            String cntStr = "_" + polarization;

            if (isComplex) {
                final Band bandI = new Band("i" + cntStr, ProductData.TYPE_UINT32, rasterWidth, rasterHeight);
                bandI.setUnit(Unit.REAL);
                bandI.setNoDataValue(0);
                bandI.setNoDataValueUsed(true);
                product.addBand(bandI);
                bandMap.put(bandI, new ImageIOFile.BandInfo(bandI, img, 0, 0));

                final Band bandQ = new Band("q" + cntStr, ProductData.TYPE_UINT32, rasterWidth, rasterHeight);
                bandQ.setUnit(Unit.IMAGINARY);
                bandQ.setNoDataValue(0);
                bandQ.setNoDataValueUsed(true);
                product.addBand(bandQ);
                bandMap.put(bandQ, new ImageIOFile.BandInfo(bandQ, img, 0, 0));

                ReaderUtils.createVirtualIntensityBand(product, bandI, bandQ, cntStr);
            } else {
                final Band band = new Band("Amplitude" + cntStr, ProductData.TYPE_UINT32, rasterWidth, rasterHeight);
                band.setUnit(Unit.AMPLITUDE);
                band.setNoDataValue(0);
                band.setNoDataValueUsed(true);
                product.addBand(band);
                bandMap.put(band, new ImageIOFile.BandInfo(band, img, 0, 0));

                createVirtualIntensityBand(product, band, cntStr);
            }
        } catch (IOException e) {
            SystemUtils.LOG.severe(e.getMessage());
        }
    }

    @Override
    protected void addMetadataToProduct() {
        final MetadataElement origMetadataRoot = AbstractMetadata.addOriginalProductMetadata(product.getMetadataRoot());
        for (Map.Entry<String, String> variable : tiffFields.entrySet()) {
            addAttribute(origMetadataRoot, variable.getKey(), variable.getValue());
        }
        addAbstractedMetadataHeader(product.getMetadataRoot());
    }

    private void addAbstractedMetadataHeader(MetadataElement root) {
        final MetadataElement absRoot = AbstractMetadata.addAbstractedMetadataHeader(root);

        try {
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.PRODUCT, tiffFields.get(IceyeXConstants.PRODUCT.toUpperCase()));
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.PRODUCT_TYPE, tiffFields.get(IceyeXConstants.PRODUCT_TYPE.toUpperCase()));
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.SPH_DESCRIPTOR, tiffFields.get(IceyeXConstants.SPH_DESCRIPTOR.toUpperCase()));
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.MISSION, Missions.ICEYE);

            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.ACQUISITION_MODE, tiffFields.get(IceyeXConstants.ACQUISITION_MODE.toUpperCase()));
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.antenna_pointing, tiffFields.get(IceyeXConstants.ANTENNA_POINTING.toUpperCase()));
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.BEAMS, IceyeXConstants.BEAMS_DEFAULT_VALUE);

            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.PROC_TIME, ProductData.UTC.parse(tiffFields.get(IceyeXConstants.PROC_TIME_UTC.toUpperCase()), standardDateFormat));

            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.ProcessingSystemIdentifier, tiffFields.get(IceyeXConstants.PROCESSING_SYSTEM_IDENTIFIER.toUpperCase()));
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.CYCLE, Integer.valueOf(tiffFields.get(IceyeXConstants.CYCLE.toUpperCase())));
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.REL_ORBIT, Integer.valueOf(tiffFields.get(IceyeXConstants.REL_ORBIT.toUpperCase())));
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.ABS_ORBIT, Integer.valueOf(tiffFields.get(IceyeXConstants.ABS_ORBIT.toUpperCase())));

//            double[] localIncidenceAngles = (double[]) netcdfFile.getRootGroup().findVariable(IceyeXConstants.INCIDENCE_ANGLES).read().getStorage();
            //todo check the incidence angles
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.incidence_near, Double.valueOf(tiffFields.get(IceyeXConstants.INCIDENCE_NEAR.toUpperCase())));
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.incidence_far, Double.valueOf(tiffFields.get(IceyeXConstants.INCIDENCE_FAR.toUpperCase())));
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.slice_num, IceyeXConstants.SLICE_NUM_DEFAULT_VALUE);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.data_take_id, IceyeXConstants.DATA_TAKE_ID_DEFAULT_VALUE);
            String geoRefSystem = StringUtils.isNullOrEmpty(tiffFields.get(IceyeXConstants.GEO_REFERENCE_SYSTEM.toUpperCase()))
                    ? IceyeXConstants.GEO_REFERENCE_SYSTEM_DEFAULT_VALUE : tiffFields.get(IceyeXConstants.GEO_REFERENCE_SYSTEM.toUpperCase());

            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.geo_ref_system, geoRefSystem);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.first_line_time, ProductData.UTC.parse(tiffFields.get(IceyeXConstants.FIRST_LINE_TIME.toUpperCase()), standardDateFormat));
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.last_line_time, ProductData.UTC.parse(tiffFields.get(IceyeXConstants.LAST_LINE_TIME.toUpperCase()), standardDateFormat));

            double[] firstNear = convertStringToDoubleArray(tiffFields.get(IceyeXConstants.FIRST_NEAR.toUpperCase()));
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.first_near_lat, firstNear[0]);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.first_near_long, firstNear[1]);
            double[] firstFar = convertStringToDoubleArray(tiffFields.get(IceyeXConstants.FIRST_FAR.toUpperCase()));
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.first_far_lat, firstFar[0]);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.first_far_long, firstFar[1]);
            double[] lastNear = convertStringToDoubleArray(tiffFields.get(IceyeXConstants.LAST_NEAR.toUpperCase()));
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.last_near_lat, lastNear[0]);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.last_near_long, lastNear[1]);
            double[] lastFar = convertStringToDoubleArray(tiffFields.get(IceyeXConstants.LAST_FAR.toUpperCase()));
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.last_far_lat, lastFar[0]);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.last_far_long, lastFar[1]);

            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.PASS, tiffFields.get(IceyeXConstants.PASS.toUpperCase()));

            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.SAMPLE_TYPE, getSampleType());
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.mds1_tx_rx_polar, tiffFields.get(IceyeXConstants.MDS1_TX_RX_POLAR.toUpperCase()));

            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.azimuth_looks, Float.valueOf(tiffFields.get(IceyeXConstants.AZIMUTH_LOOKS.toUpperCase())));
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.range_looks, Float.valueOf(tiffFields.get(IceyeXConstants.RANGE_LOOKS.toUpperCase())));

            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.range_spacing, Float.valueOf(tiffFields.get(IceyeXConstants.RANGE_SPACING.toUpperCase())));

            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.azimuth_spacing, Float.valueOf(tiffFields.get(IceyeXConstants.AZIMUTH_SPACING.toUpperCase())));
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.pulse_repetition_frequency, Float.valueOf(tiffFields.get(IceyeXConstants.PULSE_REPETITION_FREQUENCY.toUpperCase())));
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.radar_frequency, Double.valueOf(tiffFields.get(IceyeXConstants.RADAR_FREQUENCY.toUpperCase())) / Constants.oneMillion);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.line_time_interval, Double.valueOf(tiffFields.get(IceyeXConstants.LINE_TIME_INTERVAL.toUpperCase())));

            final int rasterWidth = Integer.parseInt(tiffFields.get(IceyeXConstants.NUM_SAMPLES_PER_LINE.toUpperCase()));
            final int rasterHeight = Integer.parseInt(tiffFields.get(IceyeXConstants.NUM_OUTPUT_LINES.toUpperCase()));
            double totalSize = (rasterHeight * rasterWidth * 2 * 2) / (1024.0f * 1024.0f);

            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.TOT_SIZE, totalSize);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.num_output_lines, Integer.valueOf(tiffFields.get(IceyeXConstants.NUM_OUTPUT_LINES.toUpperCase())));
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.num_samples_per_line, Integer.valueOf(tiffFields.get(IceyeXConstants.NUM_SAMPLES_PER_LINE.toUpperCase())));

            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.subset_offset_x, IceyeXConstants.SUBSET_OFFSET_X_DEFAULT_VALUE);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.subset_offset_y, IceyeXConstants.SUBSET_OFFSET_Y_DEFAULT_VALUE);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.srgr_flag, isComplex ? 0 : 1);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.avg_scene_height, Double.valueOf(tiffFields.get(IceyeXConstants.AVG_SCENE_HEIGHT.toUpperCase())));

            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.lat_pixel_res, IceyeXConstants.LAT_PIXEL_RES_DEFAULT_VALUE);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.lon_pixel_res, IceyeXConstants.LON_PIXEL_RES_DEFAULT_VALUE);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.slant_range_to_first_pixel, Double.valueOf(tiffFields.get(IceyeXConstants.SLANT_RANGE_TO_FIRST_PIXEL.toUpperCase())));

            int antElevCorrFlag = IceyeXConstants.ANT_ELEV_CORR_FLAG_DEFAULT_VALUE;
            if (tiffFields.get(IceyeXConstants.ANT_ELEV_CORR_FLAG.toUpperCase()) != null) {
                antElevCorrFlag = Integer.valueOf(tiffFields.get(IceyeXConstants.ANT_ELEV_CORR_FLAG.toUpperCase()));
            }
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.ant_elev_corr_flag, antElevCorrFlag);

            int rangeSpreadCompFlag = IceyeXConstants.RANGE_SPREAD_COMP_FLAG_DEFAULT_VALUE;
            if (tiffFields.get(IceyeXConstants.RANGE_SPREAD_COMP_FLAG.toUpperCase()) != null) {
                rangeSpreadCompFlag = Integer.valueOf(tiffFields.get(IceyeXConstants.RANGE_SPREAD_COMP_FLAG.toUpperCase()));
            }
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.range_spread_comp_flag, rangeSpreadCompFlag);

            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.replica_power_corr_flag, IceyeXConstants.REPLICA_POWER_CORR_FLAG_DEFAULT_VALUE);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.abs_calibration_flag, IceyeXConstants.ABS_CALIBRATION_FLAG_DEFAULT_VALUE);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.calibration_factor, Double.valueOf(tiffFields.get(IceyeXConstants.CALIBRATION_FACTOR.toUpperCase())));
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.inc_angle_comp_flag, IceyeXConstants.INC_ANGLE_COMP_FLAG_DEFAULT_VALUE);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.ref_inc_angle, IceyeXConstants.REF_INC_ANGLE_DEFAULT_VALUE);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.ref_slant_range, IceyeXConstants.REF_SLANT_RANGE_DEFAULT_VALUE);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.ref_slant_range_exp, IceyeXConstants.REF_SLANT_RANGE_EXP_DEFAULT_VALUE);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.rescaling_factor, IceyeXConstants.RESCALING_FACTOR_DEFAULT_VALUE);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.range_sampling_rate, Double.valueOf(tiffFields.get(IceyeXConstants.RANGE_SAMPLING_RATE.toUpperCase())) / 1e6);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.range_bandwidth, Double.valueOf(tiffFields.get(IceyeXConstants.RANGE_BANDWIDTH.toUpperCase())) / 1e6);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.azimuth_bandwidth, Double.valueOf(tiffFields.get(IceyeXConstants.AZIMUTH_BANDWIDTH.toUpperCase())));
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.multilook_flag, IceyeXConstants.MULTI_LOOK_FLAG_DEFAULT_VALUE);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.coregistered_stack, IceyeXConstants.CO_REGISTERED_STACK_DEFAULT_VALUE);

            addOrbitStateVectors(absRoot);
        } catch (ParseException e) {
            SystemUtils.LOG.severe(e.getMessage());
        }
    }

    @Override
    protected void addOrbitStateVectors(final MetadataElement absRoot) {
        try {
            final MetadataElement orbitVectorListElem = absRoot.getElement(AbstractMetadata.orbit_state_vectors);

            String[] stateVectorTime = convertDateStringToStringArray(tiffFields.get(IceyeXConstants.STATE_VECTOR_TIME.toUpperCase()));
            final int numPoints = stateVectorTime.length;
            final double[] satellitePositionX = convertStringToDoubleArrayBySpace(tiffFields.get(IceyeXConstants.ORBIT_VECTOR_N_X_POS.toUpperCase()));
            final double[] satellitePositionY = convertStringToDoubleArrayBySpace(tiffFields.get(IceyeXConstants.ORBIT_VECTOR_N_Y_POS.toUpperCase()));
            final double[] satellitePositionZ = convertStringToDoubleArrayBySpace(tiffFields.get(IceyeXConstants.ORBIT_VECTOR_N_Z_POS.toUpperCase()));
            final double[] satelliteVelocityX = convertStringToDoubleArrayBySpace(tiffFields.get(IceyeXConstants.ORBIT_VECTOR_N_X_VEL.toUpperCase()));
            final double[] satelliteVelocityY = convertStringToDoubleArrayBySpace(tiffFields.get(IceyeXConstants.ORBIT_VECTOR_N_Y_VEL.toUpperCase()));
            final double[] satelliteVelocityZ = convertStringToDoubleArrayBySpace(tiffFields.get(IceyeXConstants.ORBIT_VECTOR_N_Z_VEL.toUpperCase()));
            ProductData.UTC stateVectorUTC = ProductData.UTC.parse(stateVectorTime[0], standardDateFormat);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.STATE_VECTOR_TIME, stateVectorUTC);
            for (int i = 0; i < numPoints; i++) {
                ProductData.UTC vectorUTC = ProductData.UTC.parse(stateVectorTime[i], standardDateFormat);

                final MetadataElement orbitVectorElem = new MetadataElement(AbstractMetadata.orbit_vector + (i + 1));
                orbitVectorElem.setAttributeUTC(AbstractMetadata.orbit_vector_time, vectorUTC);

                orbitVectorElem.setAttributeDouble(AbstractMetadata.orbit_vector_x_pos, satellitePositionX[i]);
                orbitVectorElem.setAttributeDouble(AbstractMetadata.orbit_vector_y_pos, satellitePositionY[i]);
                orbitVectorElem.setAttributeDouble(AbstractMetadata.orbit_vector_z_pos, satellitePositionZ[i]);
                orbitVectorElem.setAttributeDouble(AbstractMetadata.orbit_vector_x_vel, satelliteVelocityX[i]);
                orbitVectorElem.setAttributeDouble(AbstractMetadata.orbit_vector_y_vel, satelliteVelocityY[i]);
                orbitVectorElem.setAttributeDouble(AbstractMetadata.orbit_vector_z_vel, satelliteVelocityZ[i]);

                orbitVectorListElem.addElement(orbitVectorElem);
            }
        } catch (ParseException e) {
            SystemUtils.LOG.severe(e.getMessage());
        }
    }

    @Override
    protected void addTiePointGridsToProduct() {
        addIncidenceAnglesSlantRangeTime(product);
        addGeocodingFromMetadata(product);
    }

    private void addIncidenceAnglesSlantRangeTime(final Product product) {

        final int gridWidth = 11;
        final int gridHeight = 11;
        final float subSamplingX = product.getSceneRasterWidth() / (float) (gridWidth - 1);
        final float subSamplingY = product.getSceneRasterHeight() / (float) (gridHeight - 1);

        final double nearRangeAngle = Double.parseDouble(this.tiffFields.get(IceyeXConstants.INCIDENCE_NEAR.toUpperCase()));
        final double farRangeAngle = Double.parseDouble(this.tiffFields.get(IceyeXConstants.INCIDENCE_FAR.toUpperCase()));

        final double firstRangeTime = Double.parseDouble(this.tiffFields.get(IceyeXConstants.FIRST_PIXEL_TIME.toUpperCase())) * Constants.sTOns;
        final double samplesPerLine = Integer.parseInt(this.tiffFields.get(IceyeXConstants.NUM_SAMPLES_PER_LINE.toUpperCase()));
        final double rangeSamplingRate = Double.valueOf(tiffFields.get(IceyeXConstants.RANGE_SAMPLING_RATE.toUpperCase()));
        final double lastRangeTime = firstRangeTime + samplesPerLine / rangeSamplingRate * Constants.sTOns;

        final float[] incidenceCorners = new float[]{(float) nearRangeAngle, (float) farRangeAngle, (float) nearRangeAngle, (float) farRangeAngle};
        final float[] slantRange = new float[]{(float) firstRangeTime, (float) lastRangeTime, (float) firstRangeTime, (float) lastRangeTime};

        final float[] fineAngles = new float[gridWidth * gridHeight];
        final float[] fineTimes = new float[gridWidth * gridHeight];

        ReaderUtils.createFineTiePointGrid(2, 2, gridWidth, gridHeight, incidenceCorners, fineAngles);
        ReaderUtils.createFineTiePointGrid(2, 2, gridWidth, gridHeight, slantRange, fineTimes);

        final TiePointGrid incidentAngleGrid = new TiePointGrid(OperatorUtils.TPG_INCIDENT_ANGLE, gridWidth, gridHeight, 0, 0,
                subSamplingX, subSamplingY, fineAngles);
        incidentAngleGrid.setUnit(Unit.DEGREES);
        product.addTiePointGrid(incidentAngleGrid);

        final TiePointGrid slantRangeGrid = new TiePointGrid(OperatorUtils.TPG_SLANT_RANGE_TIME, gridWidth, gridHeight, 0, 0,
                subSamplingX, subSamplingY, fineTimes);
        slantRangeGrid.setUnit(Unit.NANOSECONDS);
        product.addTiePointGrid(slantRangeGrid);
    }

    private void addGeocodingFromMetadata(final Product product) {

        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(product);

        double[] firstNear = convertStringToDoubleArray(tiffFields.get(IceyeXConstants.FIRST_NEAR.toUpperCase()));
        double[] firstFar = convertStringToDoubleArray(tiffFields.get(IceyeXConstants.FIRST_FAR.toUpperCase()));
        double[] lastNear = convertStringToDoubleArray(tiffFields.get(IceyeXConstants.LAST_NEAR.toUpperCase()));
        double[] lastFar = convertStringToDoubleArray(tiffFields.get(IceyeXConstants.LAST_FAR.toUpperCase()));

        final double latUL = firstNear[0];
        final double lonUL = firstNear[1];
        final double latUR = firstFar[0];
        final double lonUR = firstFar[1];
        final double latLL = lastNear[0];
        final double lonLL = lastNear[1];
        final double latLR = lastFar[0];
        final double lonLR = lastFar[1];

        absRoot.setAttributeDouble(AbstractMetadata.first_near_lat, latUL);
        absRoot.setAttributeDouble(AbstractMetadata.first_near_long, lonUL);
        absRoot.setAttributeDouble(AbstractMetadata.first_far_lat, latUR);
        absRoot.setAttributeDouble(AbstractMetadata.first_far_long, lonUR);
        absRoot.setAttributeDouble(AbstractMetadata.last_near_lat, latLL);
        absRoot.setAttributeDouble(AbstractMetadata.last_near_long, lonLL);
        absRoot.setAttributeDouble(AbstractMetadata.last_far_lat, latLR);
        absRoot.setAttributeDouble(AbstractMetadata.last_far_long, lonLR);

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.range_spacing,
                Double.valueOf(tiffFields.get(IceyeXConstants.RANGE_SPACING.toUpperCase())));
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.azimuth_spacing,
                Double.valueOf(tiffFields.get(IceyeXConstants.AZIMUTH_SPACING.toUpperCase())));

        final double[] latCorners = new double[]{latUL, latUR, latLL, latLR};
        final double[] lonCorners = new double[]{lonUL, lonUR, lonLL, lonLR};

        ReaderUtils.addGeoCoding(product, latCorners, lonCorners);
    }

    @Override
    protected void addGeoCodingToProduct() {
        //todo: not sure if needed

//        if (product.getSceneGeoCoding() == null) {
//            NetCDFReader.setTiePointGeoCoding(product);
//        }
//        try {
//            if (product.getSceneGeoCoding() == null) {
//                NetCDFReader.setPixelGeoCoding(product);
//            }
//        } catch (Exception e) {
//            throw new RuntimeException("Failed to setPixelGeocoding", e);
//        }
//    }
    }

    private void addDopplerMetadata() {
        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(product);
        final String imagingMode = absRoot.getAttributeString("ACQUISITION_MODE");

        if (imagingMode.equalsIgnoreCase("spotlight")) {
            final MetadataElement dopplerSpotlightElem = new MetadataElement("dopplerSpotlight");
            absRoot.addElement(dopplerSpotlightElem);
            addDopplerRateAndCentroidSpotlight(dopplerSpotlightElem);
            addAzimuthTimeZpSpotlight(dopplerSpotlightElem);
        }
        addDopplerCentroidCoefficients();
    }

    private void addDopplerRateAndCentroidSpotlight(MetadataElement elem) {
        // Compute doppler rate and centroid
        MetadataElement origProdRoot = AbstractMetadata.getOriginalProductMetadata(product);
        MetadataElement dopplerRateCoeffs = origProdRoot.getElement(IceyeXConstants.DR_COEFFS);
        double[] coeffs = convertStringToDoubleArrayBySpace(tiffFields.get(IceyeXConstants.DR_COEFFS.toUpperCase()));
        String dopplerRate = String.valueOf(coeffs[0]); // take first coefficient
        final double fmRate = Double.parseDouble(dopplerRate);
        final double dopplerCentroid = 0.0; // TODO: load from original metadata once it's accurate

        final int rasterWidth = product.getSceneRasterWidth();
        final double[] dopplerRateSpotlight = new double[rasterWidth];
        final double[] dopplerCentroidSpotlight = new double[rasterWidth];

        for (int i = 0; i < rasterWidth; i++) {
            dopplerRateSpotlight[i] = fmRate;
            dopplerCentroidSpotlight[i] = dopplerCentroid;
        }

        // Save in metadata
        String dopplerRateSpotlightStr = Arrays.toString(dopplerRateSpotlight).replace("]", "").replace("[", "");
        String dopplerCentroidSpotlightStr = Arrays.toString(dopplerCentroidSpotlight).replace("]", "").replace("[", "");

        AbstractMetadata.addAbstractedAttribute(elem, "dopplerRateSpotlight",
                ProductData.TYPE_ASCII, "", "Doppler Rate Spotlight");
        AbstractMetadata.setAttribute(elem, "dopplerRateSpotlight", dopplerRateSpotlightStr);

        AbstractMetadata.addAbstractedAttribute(elem, "dopplerCentroidSpotlight",
                ProductData.TYPE_ASCII, "", "Doppler Centroid Spotlight");
        AbstractMetadata.setAttribute(elem, "dopplerCentroidSpotlight", dopplerCentroidSpotlightStr);
    }

    private void addAzimuthTimeZpSpotlight(MetadataElement elem) {
        // Compute azimuth time
        MetadataElement origProdRoot = AbstractMetadata.getOriginalProductMetadata(product);
        final double firstAzimuthTimeZp = timeUTCtoSecs(tiffFields.get(IceyeXConstants.FIRST_LINE_TIME.toUpperCase()));
        final double lastAzimuthTimeZp = timeUTCtoSecs(tiffFields.get(IceyeXConstants.LAST_LINE_TIME.toUpperCase()));
        final double AzimuthTimeZpOffset = firstAzimuthTimeZp - 0.5 * (firstAzimuthTimeZp + lastAzimuthTimeZp);

        // Save in metadata
        final MetadataElement azimuthTimeZd = new MetadataElement("azimuthTimeZdSpotlight");
        elem.addElement(azimuthTimeZd);
        AbstractMetadata.addAbstractedAttribute(azimuthTimeZd, "AzimuthTimeZdOffset",
                ProductData.TYPE_FLOAT64, "", "Azimuth Time Zero Doppler Offset");
        AbstractMetadata.setAttribute(azimuthTimeZd, "AzimuthTimeZdOffset", AzimuthTimeZpOffset);
    }

    private double timeUTCtoSecs(String myDate) {
        ProductData.UTC localDateTime = null;
        try {
            localDateTime = ProductData.UTC.parse(myDate, standardDateFormat);
        } catch (ParseException e) {
            SystemUtils.LOG.severe(e.getMessage());
        }
        return localDateTime.getMJD() * 24.0 * 3600.0;
    }

    private void addDopplerCentroidCoefficients() {

        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(product);

        final MetadataElement dopplerCentroidCoefficientsElem = absRoot.getElement(AbstractMetadata.dop_coefficients);
        final MetadataElement dopplerListElem = new MetadataElement(AbstractMetadata.dop_coef_list + ".1");
        dopplerCentroidCoefficientsElem.addElement(dopplerListElem);

        final ProductData.UTC utcTime;
        try {
            utcTime = ProductData.UTC.parse(convertDateStringToStringArray(tiffFields.get(IceyeXConstants.DC_ESTIMATE_TIME_UTC.toUpperCase()))[0], standardDateFormat);
            dopplerListElem.setAttributeUTC(AbstractMetadata.dop_coef_time, utcTime);
        } catch (ParseException e) {
            SystemUtils.LOG.severe(e.getMessage());
        }

        AbstractMetadata.addAbstractedAttribute(dopplerListElem, AbstractMetadata.slant_range_time,
                ProductData.TYPE_FLOAT64, "ns", "Slant Range Time");
        AbstractMetadata.setAttribute(dopplerListElem, AbstractMetadata.slant_range_time, 0.0);

        int dimensionColumn = Integer.valueOf(tiffFields.get(IceyeXConstants.DC_ESTIMATE_POLY_ORDER.toUpperCase())) + 1;
        String[] coefValues = convertDateStringToStringArrayBySpace(tiffFields.get(IceyeXConstants.DC_ESTIMATE_COEFFS.toUpperCase()));

        for (int i = 0; i < dimensionColumn; i++) {
            final double coefValue = Double.parseDouble(coefValues[i]);
            final MetadataElement coefElem = new MetadataElement(AbstractMetadata.coefficient + '.' + (i + 1));
            dopplerListElem.addElement(coefElem);
            AbstractMetadata.addAbstractedAttribute(coefElem, AbstractMetadata.dop_coef,
                    ProductData.TYPE_FLOAT64, "", "Doppler Centroid Coefficient");
            AbstractMetadata.setAttribute(coefElem, AbstractMetadata.dop_coef, coefValue);
        }
    }

    public void callReadBandRasterData(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight,
                                       int sourceStepX, int sourceStepY, Band destBand, int destOffsetX,
                                       int destOffsetY, int destWidth, int destHeight, ProductData destBuffer,
                                       ProgressMonitor pm) throws IOException {
        readBandRasterDataImpl(sourceOffsetX, sourceOffsetY, sourceWidth, sourceHeight,
                sourceStepX, sourceStepY, destBand, destOffsetX, destOffsetY, destWidth, destHeight, destBuffer, pm);
    }

    @Override
    protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight,
                                          int sourceStepX, int sourceStepY, Band destBand, int destOffsetX,
                                          int destOffsetY, int destWidth, int destHeight, ProductData destBuffer,
                                          ProgressMonitor pm) throws IOException {

        final ImageIOFile.BandInfo bandInfo = bandMap.get(destBand);
        if (bandInfo != null && bandInfo.img != null) {

            int bandSampleOffset = bandInfo.bandSampleOffset;
            ImageIOFile img = bandInfo.img;
            final ImageReader reader = img.getReader();
            final ImageReadParam param = reader.getDefaultReadParam();
            LOG.info("-----SLC START READING NEW RASTER, BAND= " + destBand.getName() + "-------");


            LOG.info("Tile request: SRC offsetX=" + sourceOffsetX + ", SRC OffsetY="
                    + sourceOffsetY + ", stepX=" + sourceStepX + ", stepY=" + sourceStepY + ", "
                    + destOffsetX + ", " + destOffsetY + " width=" + destWidth + "height" + destHeight + "src width="
                    + sourceWidth + ", src height=" + sourceHeight + ", sampleOffset=" + bandSampleOffset);

            final Raster data;
            synchronized (lock) {
                param.setSourceSubsampling(sourceStepX, sourceStepY,
                        sourceOffsetX % sourceStepX,
                        sourceOffsetY % sourceStepY);

                final RenderedImage image = reader.readAsRenderedImage(0, param);
                data = image.getData(new Rectangle(destOffsetX, destOffsetY, destWidth, destHeight));
                LOG.info("---[NEW] - numbands=" + data.getNumBands() + ", tile=" + image.getTileWidth() + ", " + image.getTileHeight() + ", num X tiles=" + image.getNumXTiles() + ",Num Y tiles=" + image.getNumYTiles());
            }

            final int w = data.getWidth();
            final int h = data.getHeight();
            final DataBuffer dataBuffer = data.getDataBuffer();
            final SampleModel sampleModel = data.getSampleModel();
            if (destBuffer.getType() == ProductData.TYPE_FLOAT32) {
                sampleModel.getSamples(0, 0, w, h, bandSampleOffset, (float[]) destBuffer.getElems(), dataBuffer);
            } else if (destBuffer.getType() == ProductData.TYPE_UINT16) {
                // SNAP buffer is short[]
                short[] target = (short[]) destBuffer.getElems();

                // Use temporary int[] because getSamples can only fill int[]
                int[] tmp = new int[target.length];
                sampleModel.getSamples(0, 0, w, h, bandSampleOffset, tmp, dataBuffer);

                for (int i = 0; i < tmp.length; i++) {
                    target[i] = (short) (tmp[i] & 0xFFFF); // clamp to 16 bits
                }
            } else {
                sampleModel.getSamples(0, 0, w, h, bandSampleOffset, (int[]) destBuffer.getElems(), dataBuffer);
            }
        }
    }
}
