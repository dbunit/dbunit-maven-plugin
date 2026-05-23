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

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.File;
import java.util.Properties;

import org.apache.maven.plugin.MojoExecutionException;
import org.dbunit.ant.Query;
import org.dbunit.ant.Table;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link CompareMojo}.
 */
public class CompareMojoTest extends AbstractDbUnitMojoTest
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
     * Tests that skip=true bypasses comparison even when the DB is empty.
     */
    @Test
    public void testExecute_skipTrue_noComparisonPerformed() throws Exception
    {
        // DB is empty — comparison would fail without skip
        final CompareMojo mojo = new CompareMojo();
        populateMojoCommonConfiguration(mojo);
        mojo.src = new File(p.getProperty("xmlDataSource"));
        mojo.format = "xml";
        mojo.skip = true;
        mojo.execute();
    }

    /**
     * Tests that a comparison against an empty DB (while expecting rows) throws.
     */
    @Test
    public void testExecute_emptyDbVsNonEmptyDataset_throwsMojoExecutionException()
    {
        // DB is empty after setUp — comparing against 2-row dataset must fail
        final CompareMojo mojo = new CompareMojo();
        populateMojoCommonConfiguration(mojo);
        mojo.src = new File(p.getProperty("xmlDataSource"));
        mojo.format = "xml";

        assertThatThrownBy(mojo::execute)
            .as("Comparison against empty DB should throw MojoExecutionException.")
            .isInstanceOf(MojoExecutionException.class);
    }

    /**
     * Tests that sort=true succeeds when DB data matches dataset.
     *
     * <p>Requires {@code CachedResultSetTableFactory} because the default
     * {@code ForwardOnlyResultSetTableFactory} does not support {@code getRowCount()},
     * which {@code SortedTable} requires to sort rows.
     */
    @Test
    public void testExecute_sortTrue_comparisonSucceeds() throws Exception
    {
        insertPersonData();

        final Properties dbconfig = new Properties();
        dbconfig.setProperty("resultSetTableFactory",
                "org.dbunit.database.CachedResultSetTableFactory");

        final CompareMojo mojo = new CompareMojo();
        populateMojoCommonConfiguration(mojo);
        mojo.dbconfig = dbconfig;
        mojo.src = new File(p.getProperty("xmlDataSource"));
        mojo.format = "xml";
        mojo.sort = true;
        mojo.execute();
    }

    /**
     * Tests that the tables filter restricts comparison to specified tables.
     */
    @Test
    public void testExecute_withTablesFilter_comparisonSucceeds() throws Exception
    {
        insertPersonData();

        final Table table = new Table();
        table.setName("person");

        final CompareMojo mojo = new CompareMojo();
        populateMojoCommonConfiguration(mojo);
        mojo.src = new File(p.getProperty("xmlDataSource"));
        mojo.format = "xml";
        mojo.tables = new Table[]{table};
        mojo.execute();
    }

    /**
     * Tests that a query filter selects specific rows for comparison.
     */
    @Test
    public void testExecute_withQueriesFilter_comparisonSucceeds() throws Exception
    {
        insertPersonData();

        final Query query = new Query();
        query.setName("person");
        query.setSql("SELECT * FROM person WHERE id = 1");

        final CompareMojo mojo = new CompareMojo();
        populateMojoCommonConfiguration(mojo);
        mojo.src = new File(p.getProperty("xmlDataSourcePerson1"));
        mojo.format = "xml";
        mojo.queries = new Query[]{query};
        mojo.execute();
    }

    /**
     * Tests that replacementDataSetClass substitutes tokens in the expected
     * dataset so a [NULL] token matches an actual NULL in the database.
     */
    @Test
    public void testExecute_replacementDataSetClass_nullInDbMatchesToken()
            throws Exception
    {
        c.createStatement().executeUpdate(
                "insert into person (id, first_name, last_name) values (1, 'Brian', null)");

        final CompareMojo mojo = new CompareMojo();
        populateMojoCommonConfiguration(mojo);
        mojo.src = new File(p.getProperty("xmlDataSourceWithNullToken"));
        mojo.format = "xml";
        mojo.replacementDataSetClass = NullReplacementDataSet.class.getName();
        mojo.execute();
    }

    /**
     * Tests that replacementDataSetClass works correctly when tables filter is
     * applied.
     */
    @Test
    public void testExecute_replacementDataSetClassWithTablesFilter_nullInDbMatchesToken()
            throws Exception
    {
        c.createStatement().executeUpdate(
                "insert into person (id, first_name, last_name) values (1, 'Brian', null)");

        final Table table = new Table();
        table.setName("person");

        final CompareMojo mojo = new CompareMojo();
        populateMojoCommonConfiguration(mojo);
        mojo.src = new File(p.getProperty("xmlDataSourceWithNullToken"));
        mojo.format = "xml";
        mojo.tables = new Table[]{table};
        mojo.replacementDataSetClass = NullReplacementDataSet.class.getName();
        mojo.execute();
    }

    /**
     * Tests that replacementDataSetClass works correctly when queries filter
     * is applied.
     */
    @Test
    public void testExecute_replacementDataSetClassWithQueriesFilter_nullInDbMatchesToken()
            throws Exception
    {
        c.createStatement().executeUpdate(
                "insert into person (id, first_name, last_name) values (1, 'Brian', null)");

        final Query query = new Query();
        query.setName("person");
        query.setSql("SELECT * FROM person WHERE id = 1");

        final CompareMojo mojo = new CompareMojo();
        populateMojoCommonConfiguration(mojo);
        mojo.src = new File(p.getProperty("xmlDataSourceWithNullToken"));
        mojo.format = "xml";
        mojo.queries = new Query[]{query};
        mojo.replacementDataSetClass = NullReplacementDataSet.class.getName();
        mojo.execute();
    }

    /**
     * Tests comparison using the flat XML dataset format.
     */
    @Test
    public void testExecute_flatFormat_comparisonSucceeds() throws Exception
    {
        insertPersonData();

        final CompareMojo mojo = new CompareMojo();
        populateMojoCommonConfiguration(mojo);
        mojo.src = new File(p.getProperty("xmlDataSourceFlat"));
        mojo.format = "flat";
        mojo.execute();
    }
}
