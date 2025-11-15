package com.iceye.esa.snap.dataio.util;

import it.geosolutions.imageio.plugins.tiff.TIFFField;
import it.geosolutions.imageioimpl.plugins.tiff.TIFFImageMetadata;
import it.geosolutions.imageioimpl.plugins.tiff.TIFFImageReader;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Optional;
import java.util.stream.Stream;

public class TiffIOUtil {


    /**
     * Attempts to find metadata of a given TIFF image file,  from image at index 0 in the file.
     *
     * @param imageIndex index of an image within multi-paged TIFF, zero-based.
     * @param filePath   path to a TIFF file.
     * @return non-empty if TIFF metadata were present , otherwise empty.
     * @throws IOException Cannot find a TIFF reader for give file (not a TIFF), other IO problems e.g. a bad path.
     */
    public static Optional<TIFFImageMetadata> findTIFFMetadata(int imageIndex, final Path filePath) throws IOException {
        try (final ImageInputStream iis = ImageIO.createImageInputStream(filePath.toFile())) {
            TIFFImageReader reader = findImageReader(iis, TIFFImageReader.class)
                    .orElseThrow(()->new IOException("No TIFF reader for the given file exist."));
            reader.reset();
            reader.setInput(iis);
            return Optional.ofNullable((TIFFImageMetadata) reader.getImageMetadata(imageIndex));
        }
    }

    /**
     * Finds requested image reader for the stream amongst all image readers registered in ImageIO.
     *
     * @param imageInputStream input stream of the image.
     * @param requestedReaderClass requested type of the reeader.
     * @return non-empty if reader was found, otherwise empty.
     */
    private static <T extends ImageReader> Optional<T> findImageReader(final ImageInputStream imageInputStream, final Class<T> requestedReaderClass) {
        final Iterator<ImageReader> imageReaders = ImageIO.getImageReaders(imageInputStream);
        while (imageReaders.hasNext()) {
            final ImageReader reader = imageReaders.next();
            if (reader.getClass().equals(requestedReaderClass)) {
                return Optional.of(requestedReaderClass.cast(reader));
            }
        }

        return Optional.empty();
    }


}
