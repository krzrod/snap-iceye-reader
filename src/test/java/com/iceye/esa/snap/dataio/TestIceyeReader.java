/*
 * Copyright (C) 2015 by Array Systems Computing Inc. http://www.array.ca
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package com.iceye.esa.snap.dataio;

import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.engine_utilities.gpf.TestProcessor;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.time.Duration;
import java.time.Instant;


/**
 * @author Ahmad Hamouda
 */
public class TestIceyeReader {

    public static final String LEGACY_TESTING_IMAGE_PATH = "D:\\iceye_sftp\\iceye_sftp\\images\\legacy\\strip\\grd";
    public static final String NEW_TESTING_IMG_PATH = "D:\\iceye_sftp\\iceye_sftp\\images\\cog\\strip\\grd";
    private IceyeProductReaderPlugIn readerPlugin;
    private ProductReader reader;

    private String[] exceptionExemptions = {"not supported"};

    public TestIceyeReader() {
        readerPlugin = new IceyeProductReaderPlugIn();
        reader = readerPlugin.createReaderInstance();
    }

    /**
     * Open all files in a folder recursively
     *
     * @throws Exception anything
     */
    @Test
    public void testOpenAll() {
        TestProcessor testProcessor = new TestProcessor(100, 100, 100, 100, 1, true, false);

        File file = new File(LEGACY_TESTING_IMAGE_PATH);
        File[] folderPaths = new File[1];
        folderPaths[0] = file;
        try {
            testProcessor.recurseReadFolder(this, folderPaths, readerPlugin, reader, null, exceptionExemptions);
        } catch (Exception e) {
            Assert.fail();
        }
    }

    @Test
    public void testOpenAllNew() {
        TestProcessor testProcessor = new TestProcessor(100, 100, 100, 100, 1, true, false);

        File file = new File(NEW_TESTING_IMG_PATH);
        File[] folderPaths = new File[1];
        folderPaths[0] = file;
        try {
            testProcessor.recurseReadFolder(this, folderPaths, readerPlugin, reader, null, exceptionExemptions);
        } catch (Exception e) {
            Assert.fail();
        }
    }

    @Test
    public void testAzimuthTimeInterval() {
        String start = "2025-08-07T10:31:22.039Z";
        String end = "2025-08-07T10:31:32.013Z";
        Instant endInstant = Instant.parse(end);
        Instant startInstant = Instant.parse(start);

        double totalSeconds = Duration.between(startInstant, endInstant).toMillis() / 1000.0;

        double interval = totalSeconds / 26697;

        System.out.println(interval);
    }
}