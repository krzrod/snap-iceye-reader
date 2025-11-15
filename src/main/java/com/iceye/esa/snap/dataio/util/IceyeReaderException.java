package com.iceye.esa.snap.dataio.util;

import com.iceye.esa.snap.dataio.IceyeProductReader;
import org.esa.snap.core.dataio.ProductIOException;

import java.io.Serial;

public class IceyeReaderException extends ProductIOException  {

    @Serial
    private static final long serialVersionUID = 496889746719266090L;

    public IceyeReaderException() {
    }

    public IceyeReaderException(String message) {
        super(message);
    }

    public IceyeReaderException(String message, Throwable cause) {
        super(message, cause);
    }

}
