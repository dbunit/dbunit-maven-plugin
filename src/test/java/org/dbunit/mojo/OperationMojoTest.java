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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.maven.plugin.MojoExecutionException;
import org.junit.jupiter.api.Test;

/**
 * @author <a href="mailto:dantran@gmail.com">Dan Tran</a>
 * @version $Id$
 */
public class OperationMojoTest extends AbstractDbUnitMojoTest
{
    @Test
    public void testCleanInsertOperation() throws Exception
    {
        // init database with fixed data
        final OperationMojo operation = new OperationMojo();
        this.populateMojoCommonConfiguration(operation);
        operation.src = new File(p.getProperty("xmlDataSource"));
        operation.format = "xml";
        operation.type = "CLEAN_INSERT";
        operation.execute();

        // check to makesure we have 2 rows after inserts thru dataset
        Statement st = c.createStatement();
        ResultSet rs = st.executeQuery("select count(*) from person");
        rs.next();
        assertEquals(2, rs.getInt(1));

        // export database to another dataset file
        final File exportFile = new File(getBasedir(), "target/export.xml");
        final ExportMojo export = new ExportMojo();
        this.populateMojoCommonConfiguration(export);
        export.dest = exportFile;
        export.format = "xml";
        export.ordered = true;
        export.execute();

        // then import the exported dataset file back to DB
        operation.src = exportFile;
        operation.execute();

        // check to makesure we have 2 rows
        st = c.createStatement();
        rs = st.executeQuery("select count(*) from person");
        rs.next();
        assertEquals(2, rs.getInt(1));

        // finally compare the current contents of the DB with the orginal
        // dataset file
        final CompareMojo compare = new CompareMojo();
        this.populateMojoCommonConfiguration(compare);
        compare.src = new File(p.getProperty("xmlDataSource"));
        compare.format = "xml";
        compare.sort = false;
        compare.execute();
    }

    @Test
    public void testCleanInsertOperationCompositeDataset() throws Exception
    {
        // Insert some pre-existing data, to force integrity checks
        final Statement st = c.createStatement();
        st.executeUpdate(
                "insert into person ( id, first_name, last_name) values (1, 'First', 'Last')");
        st.executeUpdate(
                "insert into address ( id, street) values (1, 'Street')");

        // init database with single dataset from two separate files
        final OperationMojo operation = new OperationMojo();
        this.populateMojoCommonConfiguration(operation);
        operation.sources =
                new File[] {new File(p.getProperty("xmlDataSourcePerson")),
                        new File(p.getProperty("xmlDataSourceAddress")),};
        operation.format = "xml";
        operation.type = "CLEAN_INSERT";
        operation.composite = true;
        operation.execute();

        // compare the current contents of the DB with the single dataset file
        final CompareMojo compare = new CompareMojo();
        this.populateMojoCommonConfiguration(compare);
        compare.src = new File(p.getProperty("xmlDataSource"));
        compare.format = "xml";
        compare.sort = false;
        compare.execute();
    }

    @Test
    public void testCleanInsertOperationCompositeCombineDataset()
            throws Exception
    {
        // Insert some pre-existing data, to force integrity checks
        final Statement st = c.createStatement();
        st.executeUpdate(
                "insert into person ( id, first_name, last_name) values (1, 'First', 'Last')");
        st.executeUpdate(
                "insert into address ( id, street) values (1, 'Street')");

        // init database with single dataset from three separate files, where
        // two of the files
        // define rows for the same table 'Person'
        final OperationMojo operation = new OperationMojo();
        this.populateMojoCommonConfiguration(operation);
        operation.sources = new File[] {
                new File(p.getProperty("xmlDataSourcePerson")),
                new File(p.getProperty("xmlDataSourceAddress")),
                new File(p.getProperty("xmlDataSourceAdditionalPerson")),};
        operation.format = "xml";
        operation.type = "CLEAN_INSERT";
        operation.composite = true;
        operation.combine = true;
        operation.execute();

        // check to make sure we have 3 rows
        final ResultSet rs = st.executeQuery("select count(*) from person");
        rs.next();
        assertEquals(3, rs.getInt(1));
    }

    @Test
    public void testCleanInsertOperationMultipleDatasetsCauseIntegrityConstraintViolation()
            throws Exception
    {
        // Insert some pre-existing data, to force integrity checks
        final Statement st = c.createStatement();
        st.executeUpdate(
                "insert into person ( id, first_name, last_name) values (1, 'First', 'Last')");
        st.executeUpdate(
                "insert into address ( id, street) values (1, 'Street')");

        // init database with two datasets from two separate files
        final OperationMojo operation = new OperationMojo();
        this.populateMojoCommonConfiguration(operation);
        operation.sources =
                new File[] {new File(p.getProperty("xmlDataSourcePerson")),
                        new File(p.getProperty("xmlDataSourceAddress")),};
        operation.format = "xml";
        operation.type = "CLEAN_INSERT";
        operation.composite = false;
        try
        {
            operation.execute();
            fail("MojoExecutionException expected");
        } catch (final MojoExecutionException expected)
        {
            final Throwable rootCause = expected.getCause().getCause();
            assertTrue(rootCause instanceof SQLException,
                    "SQLException expected");
            assertTrue(
                    rootCause.getMessage()
                            .contains("Integrity constraint violation"),
                    "Integrity constraint violation expected");
        }
    }

    @Test
    public void testSkip() throws Exception
    {
        // init database with fixed data
        final OperationMojo operation = new OperationMojo();
        this.populateMojoCommonConfiguration(operation);
        operation.src = new File(p.getProperty("xmlDataSource"));
        operation.format = "xml";
        operation.type = "CLEAN_INSERT";
        operation.skip = true;
        operation.execute();

        // check to makesure we have 0 rows
        final Statement st = c.createStatement();
        final ResultSet rs = st.executeQuery("select count(*) from person");
        rs.next();
        // no data since skip is set
        assertEquals(0, rs.getInt(1));
    }

    @Test
    public void testExecute_insertType_insertsRows() throws Exception
    {
        final OperationMojo mojo = new OperationMojo();
        populateMojoCommonConfiguration(mojo);
        mojo.src = new File(p.getProperty("xmlDataSource"));
        mojo.format = "xml";
        mojo.type = "INSERT";
        mojo.execute();

        final ResultSet rs = c.createStatement().executeQuery("select count(*) from person");
        rs.next();
        assertThat(rs.getInt(1)).as("Person count after INSERT.").isEqualTo(2);
    }

    @Test
    public void testExecute_updateTypeAfterInsert_lastNameUpdated() throws Exception
    {
        final OperationMojo insert = new OperationMojo();
        populateMojoCommonConfiguration(insert);
        insert.src = new File(p.getProperty("xmlDataSource"));
        insert.format = "xml";
        insert.type = "INSERT";
        insert.execute();

        final OperationMojo update = new OperationMojo();
        populateMojoCommonConfiguration(update);
        update.src = new File(p.getProperty("xmlDataSourcePersonUpdated"));
        update.format = "xml";
        update.type = "UPDATE";
        update.execute();

        final ResultSet rs = c.createStatement().executeQuery("select last_name from person where id=1");
        rs.next();
        assertThat(rs.getString(1)).as("Last name of person id=1 after UPDATE.").isEqualTo("Updated");
    }

    @Test
    public void testExecute_deleteTypeAfterInsert_onePersonRemains() throws Exception
    {
        final OperationMojo insert = new OperationMojo();
        populateMojoCommonConfiguration(insert);
        insert.src = new File(p.getProperty("xmlDataSource"));
        insert.format = "xml";
        insert.type = "INSERT";
        insert.execute();

        final OperationMojo delete = new OperationMojo();
        populateMojoCommonConfiguration(delete);
        delete.src = new File(p.getProperty("xmlDataSourcePerson1"));
        delete.format = "xml";
        delete.type = "DELETE";
        delete.execute();

        final ResultSet rs = c.createStatement().executeQuery("select count(*) from person");
        rs.next();
        assertThat(rs.getInt(1)).as("Person count after DELETE of one row.").isEqualTo(1);
    }

    @Test
    public void testExecute_deleteAllTypeAfterInsert_noPersonsInDb() throws Exception
    {
        final OperationMojo insert = new OperationMojo();
        populateMojoCommonConfiguration(insert);
        insert.src = new File(p.getProperty("xmlDataSource"));
        insert.format = "xml";
        insert.type = "INSERT";
        insert.execute();

        final OperationMojo deleteAll = new OperationMojo();
        populateMojoCommonConfiguration(deleteAll);
        deleteAll.src = new File(p.getProperty("xmlDataSource"));
        deleteAll.format = "xml";
        deleteAll.type = "DELETE_ALL";
        deleteAll.execute();

        final ResultSet rs = c.createStatement().executeQuery("select count(*) from person");
        rs.next();
        assertThat(rs.getInt(1)).as("Person count after DELETE_ALL.").isEqualTo(0);
    }

    @Test
    public void testExecute_refreshTypeWithPreexistingRow_dataRefreshed() throws Exception
    {
        c.createStatement().executeUpdate(
                "insert into person (id, first_name, last_name) values (1, 'Old', 'Name')");

        final OperationMojo refresh = new OperationMojo();
        populateMojoCommonConfiguration(refresh);
        refresh.src = new File(p.getProperty("xmlDataSource"));
        refresh.format = "xml";
        refresh.type = "REFRESH";
        refresh.execute();

        final ResultSet countRs = c.createStatement().executeQuery("select count(*) from person");
        countRs.next();
        assertThat(countRs.getInt(1)).as("Person count after REFRESH.").isEqualTo(2);

        final ResultSet nameRs = c.createStatement().executeQuery(
                "select first_name from person where id=1");
        nameRs.next();
        assertThat(nameRs.getString(1)).as("First name of person id=1 after REFRESH.").isEqualTo("Brian");
    }

    @Test
    public void testExecute_transactionTrue_dataInserted() throws Exception
    {
        final OperationMojo mojo = new OperationMojo();
        populateMojoCommonConfiguration(mojo);
        mojo.src = new File(p.getProperty("xmlDataSource"));
        mojo.format = "xml";
        mojo.type = "CLEAN_INSERT";
        mojo.transaction = true;
        mojo.execute();

        final ResultSet rs = c.createStatement().executeQuery("select count(*) from person");
        rs.next();
        assertThat(rs.getInt(1)).as("Person count after transactional CLEAN_INSERT.").isEqualTo(2);
    }

    @Test
    public void testExecute_flatFormat_dataInserted() throws Exception
    {
        final OperationMojo mojo = new OperationMojo();
        populateMojoCommonConfiguration(mojo);
        mojo.src = new File(p.getProperty("xmlDataSourceFlat"));
        mojo.format = "flat";
        mojo.type = "CLEAN_INSERT";
        mojo.execute();

        final ResultSet rs = c.createStatement().executeQuery("select count(*) from person");
        rs.next();
        assertThat(rs.getInt(1)).as("Person count after flat-format CLEAN_INSERT.").isEqualTo(2);
    }

    @Test
    public void testExecute_srcAndSourcesCombined_threePersonsInDb() throws Exception
    {
        final OperationMojo mojo = new OperationMojo();
        populateMojoCommonConfiguration(mojo);
        mojo.src = new File(p.getProperty("xmlDataSourcePerson"));
        mojo.sources = new File[]{new File(p.getProperty("xmlDataSourceAdditionalPerson"))};
        mojo.format = "xml";
        mojo.type = "INSERT";
        mojo.execute();

        final ResultSet rs = c.createStatement().executeQuery("select count(*) from person");
        rs.next();
        assertThat(rs.getInt(1)).as("Person count when src and sources are concatenated.").isEqualTo(3);
    }

    @Test
    public void testExecute_replacementDataSetClass_nullStoredInDb() throws Exception
    {
        final OperationMojo mojo = new OperationMojo();
        populateMojoCommonConfiguration(mojo);
        mojo.src = new File(p.getProperty("xmlDataSourceWithNullToken"));
        mojo.format = "xml";
        mojo.type = "INSERT";
        mojo.replacementDataSetClass = NullReplacementDataSet.class.getName();
        mojo.execute();

        final ResultSet rs = c.createStatement().executeQuery(
                "select last_name from person where id=1");
        rs.next();
        assertThat(rs.getString(1)).as("last_name should be stored as SQL NULL.").isNull();
    }

    @Test
    public void testExecute_replacementDataSetClassComposite_nullStoredInDb() throws Exception
    {
        final OperationMojo mojo = new OperationMojo();
        populateMojoCommonConfiguration(mojo);
        mojo.sources = new File[]{new File(p.getProperty("xmlDataSourceWithNullToken"))};
        mojo.format = "xml";
        mojo.type = "INSERT";
        mojo.composite = true;
        mojo.replacementDataSetClass = NullReplacementDataSet.class.getName();
        mojo.execute();

        final ResultSet rs = c.createStatement().executeQuery(
                "select last_name from person where id=1");
        rs.next();
        assertThat(rs.getString(1)).as("last_name should be stored as SQL NULL (composite).").isNull();
    }

    @Test
    public void testExecute_orderedCompositeCleanInsert_dataInserted() throws Exception
    {
        c.createStatement().executeUpdate(
                "insert into person (id, first_name, last_name) values (1, 'Old', 'Name')");
        c.createStatement().executeUpdate(
                "insert into address (id, street) values (1, 'Old Street')");

        final OperationMojo mojo = new OperationMojo();
        populateMojoCommonConfiguration(mojo);
        mojo.sources = new File[]{
            new File(p.getProperty("xmlDataSourcePerson")),
            new File(p.getProperty("xmlDataSourceAddress"))
        };
        mojo.format = "xml";
        mojo.type = "CLEAN_INSERT";
        mojo.composite = true;
        mojo.ordered = true;
        mojo.execute();

        final ResultSet rs = c.createStatement().executeQuery("select count(*) from person");
        rs.next();
        assertThat(rs.getInt(1)).as("Person count after ordered composite CLEAN_INSERT.").isEqualTo(2);
    }
}
