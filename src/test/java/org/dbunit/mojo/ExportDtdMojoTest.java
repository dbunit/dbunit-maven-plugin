/*
 *
 * The DbUnit Database Testing Framework
 * Copyright (C)2002-2009, DbUnit.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 */
package org.dbunit.mojo;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ExportDtdMojo}.
 */
public class ExportDtdMojoTest extends AbstractDbUnitMojoTest
{
    /**
     * Tests that skip=true prevents DTD file creation.
     */
    @Test
    public void testExecute_skipTrue_noFileCreated() throws Exception
    {
        final File dtdFile = new File(getBasedir(), "target/test-export-dtd-skip.dtd");
        dtdFile.delete();

        final ExportDtdMojo mojo = new ExportDtdMojo();
        populateMojoCommonConfiguration(mojo);
        mojo.dest = dtdFile;
        mojo.skip = true;
        mojo.execute();

        assertThat(dtdFile).as("DTD file should not be created when skip=true.").doesNotExist();
    }

    /**
     * Tests that the DTD file is created and is non-empty.
     */
    @Test
    public void testExecute_defaultExport_dtdFileCreated() throws Exception
    {
        final File dtdFile = new File(getBasedir(), "target/test-export-dtd.dtd");
        final ExportDtdMojo mojo = new ExportDtdMojo();
        populateMojoCommonConfiguration(mojo);
        mojo.dest = dtdFile;
        mojo.execute();

        assertThat(dtdFile).as("DTD export file should be created.").exists();
        assertThat(dtdFile.length()).as("DTD export file should be non-empty.").isGreaterThan(0);
    }

    /**
     * Tests that the exported DTD contains element declarations for both database tables.
     */
    @Test
    public void testExecute_dtdContent_containsPersonAndAddressTables() throws Exception
    {
        final File dtdFile = new File(getBasedir(), "target/test-export-dtd-content.dtd");
        final ExportDtdMojo mojo = new ExportDtdMojo();
        populateMojoCommonConfiguration(mojo);
        mojo.dest = dtdFile;
        mojo.execute();

        final String content = new String(Files.readAllBytes(dtdFile.toPath()), StandardCharsets.UTF_8);
        assertThat(content).as("DTD should contain PERSON table element.").containsIgnoringCase("person");
        assertThat(content).as("DTD should contain ADDRESS table element.").containsIgnoringCase("address");
    }

    /**
     * Tests that the mojo auto-creates missing parent directories for the dest file.
     */
    @Test
    public void testExecute_missingParentDirectory_parentDirectoryCreated() throws Exception
    {
        final File dtdFile = new File(getBasedir(), "target/test-export-dtd-newdir/sub/export.dtd");
        final File parentDir = dtdFile.getParentFile();
        if (parentDir.exists())
        {
            parentDir.delete();
        }

        final ExportDtdMojo mojo = new ExportDtdMojo();
        populateMojoCommonConfiguration(mojo);
        mojo.dest = dtdFile;
        mojo.execute();

        assertThat(dtdFile).as("DTD file should be created even when parent directory is missing.").exists();
    }
}
