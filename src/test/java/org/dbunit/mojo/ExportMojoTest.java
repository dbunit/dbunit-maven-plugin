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

import org.dbunit.ant.Query;
import org.dbunit.ant.Table;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ExportMojo}.
 */
public class ExportMojoTest extends AbstractDbUnitMojoTest
{
    private void insertPersonData() throws Exception
    {
        final OperationMojo insert = new OperationMojo();
        populateMojoCommonConfiguration(insert);
        insert.src = new File(p.getProperty("xmlDataSource"));
        insert.format = "xml";
        insert.type = "CLEAN_INSERT";
        insert.execute();
    }

    /**
     * Tests that skip=true prevents export file creation.
     */
    @Test
    public void testExecute_skipTrue_noFileCreated() throws Exception
    {
        final File exportFile = new File(getBasedir(), "target/test-export-skip.xml");
        exportFile.delete();

        final ExportMojo mojo = new ExportMojo();
        populateMojoCommonConfiguration(mojo);
        mojo.dest = exportFile;
        mojo.format = "xml";
        mojo.skip = true;
        mojo.execute();

        assertThat(exportFile).as("Export file should not be created when skip=true.").doesNotExist();
    }

    /**
     * Tests export using the flat XML format.
     */
    @Test
    public void testExecute_flatFormat_fileCreated() throws Exception
    {
        insertPersonData();

        final File exportFile = new File(getBasedir(), "target/test-export-flat.xml");
        final ExportMojo mojo = new ExportMojo();
        populateMojoCommonConfiguration(mojo);
        mojo.dest = exportFile;
        mojo.format = "flat";
        mojo.execute();

        assertThat(exportFile).as("Flat-format export file should be created.").exists();
        assertThat(exportFile.length()).as("Flat-format export file should be non-empty.").isGreaterThan(0);
    }

    /**
     * Tests that the tables filter restricts which tables are exported.
     */
    @Test
    public void testExecute_withTablesFilter_personTableExported() throws Exception
    {
        insertPersonData();

        final File exportFile = new File(getBasedir(), "target/test-export-tables.xml");
        final Table table = new Table();
        table.setName("person");

        final ExportMojo mojo = new ExportMojo();
        populateMojoCommonConfiguration(mojo);
        mojo.dest = exportFile;
        mojo.format = "xml";
        mojo.tables = new Table[]{table};
        mojo.execute();

        final String content = new String(Files.readAllBytes(exportFile.toPath()), StandardCharsets.UTF_8);
        assertThat(content).as("Export with tables filter should contain person table.").contains("person");
    }

    /**
     * Tests that a query filter restricts exported rows to those matching the SQL.
     */
    @Test
    public void testExecute_withQueriesFilter_singlePersonExported() throws Exception
    {
        insertPersonData();

        final File exportFile = new File(getBasedir(), "target/test-export-query.xml");
        final Query query = new Query();
        query.setName("person");
        query.setSql("SELECT * FROM person WHERE id = 1");

        final ExportMojo mojo = new ExportMojo();
        populateMojoCommonConfiguration(mojo);
        mojo.dest = exportFile;
        mojo.format = "xml";
        mojo.queries = new Query[]{query};
        mojo.execute();

        final String content = new String(Files.readAllBytes(exportFile.toPath()), StandardCharsets.UTF_8);
        assertThat(content).as("Query-filtered export should contain Brian (id=1).").contains("Brian");
        assertThat(content).as("Query-filtered export should not contain Rick (id=2).").doesNotContain("Rick");
    }

    /**
     * Tests that the excludes patterns prevent matching tables from appearing in the export.
     * Patterns are matched against the JDBC metadata table name (uppercase on HSQLDB).
     */
    @Test
    public void testExecute_withExcludes_excludedTableAbsentFromExport() throws Exception
    {
        insertPersonData();
        c.createStatement().executeUpdate("insert into address (id, street) values (1, 'Main St')");

        final File exportFile = new File(getBasedir(), "target/test-export-excludes.xml");
        final ExportMojo mojo = new ExportMojo();
        populateMojoCommonConfiguration(mojo);
        mojo.dest = exportFile;
        mojo.format = "xml";
        mojo.excludes = new String[]{"(?i)address"};
        mojo.execute();

        final String content = new String(Files.readAllBytes(exportFile.toPath()), StandardCharsets.UTF_8);
        assertThat(content).as("Export with excludes should contain person table.").containsIgnoringCase("person");
        assertThat(content).as("Export with excludes should not contain excluded address table.").doesNotContainIgnoringCase("address");
    }

    /**
     * Tests that the mojo auto-creates missing parent directories for the dest file.
     */
    @Test
    public void testExecute_missingParentDirectory_parentDirectoryCreated() throws Exception
    {
        insertPersonData();

        final File exportFile = new File(getBasedir(), "target/test-export-newdir/sub/export.xml");
        final File parentDir = exportFile.getParentFile();
        if (parentDir.exists())
        {
            parentDir.delete();
        }

        final ExportMojo mojo = new ExportMojo();
        populateMojoCommonConfiguration(mojo);
        mojo.dest = exportFile;
        mojo.format = "xml";
        mojo.execute();

        assertThat(exportFile).as("Export file should be created even when parent directory is missing.").exists();
    }
}
