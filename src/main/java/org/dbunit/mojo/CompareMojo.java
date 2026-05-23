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

import java.io.File;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.dbunit.Assertion;
import org.dbunit.ant.AbstractStep;
import org.dbunit.ant.Compare;
import org.dbunit.ant.Query;
import org.dbunit.ant.Table;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.database.QueryDataSet;
import org.dbunit.dataset.CachedDataSet;
import org.dbunit.dataset.CompositeDataSet;
import org.dbunit.dataset.ForwardOnlyDataSet;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.ITable;
import org.dbunit.dataset.SortedTable;
import org.dbunit.dataset.csv.CsvProducer;
import org.dbunit.dataset.excel.XlsDataSet;
import org.dbunit.dataset.filter.DefaultColumnFilter;
import org.dbunit.dataset.xml.FlatXmlProducer;
import org.dbunit.dataset.xml.XmlProducer;
import org.dbunit.dataset.yaml.YamlProducer;

/**
 * Execute DbUnit Compare operation.
 *
 * @author <a href="mailto:dantran@gmail.com">Dan Tran</a>
 * @version $Id$
 * @since 1.0
 */
@Mojo(name = "compare", requiresDependencyResolution = ResolutionScope.COMPILE)
public class CompareMojo extends AbstractDbUnitMojo
{
    /**
     * DataSet file.
     *
     * @since 1.0
     */
    @Parameter(property = "dbunit.src", required = true)
    protected File src;

    /**
     * DataSet file format.
     *
     * @since 1.0
     */
    @Parameter(property = "dbunit.format", defaultValue = "xml")
    protected String format;

    /**
     * sort.
     *
     * @since 1.0
     */
    @Parameter(property = "dbunit.sort")
    protected boolean sort;

    /**
     * List of DbUnit's Table. See DbUnit's org.dbunit.ant.Table JavaDoc for
     * details.
     *
     * @since 1.0
     */
    @Parameter
    protected Table[] tables;

    /**
     * List of DbUnit's Query. See DbUnit's org.dbunit.ant.Query JavaDoc for
     * details.
     *
     * @since 1.0
     */
    @Parameter
    protected Query[] queries;

    /**
     * Fully-qualified class name of a {@link org.dbunit.dataset.ReplacementDataSet}
     * subclass to wrap the expected dataset before comparison. The class must
     * have a public constructor that accepts a single {@link IDataSet} argument.
     * Use this to apply token substitution (e.g. replacing {@code [NULL]} with
     * SQL {@code NULL}) without any coupling to the plugin itself.
     *
     * <p>Example:
     * <pre>
     * public class MyReplacements extends ReplacementDataSet {
     *     public MyReplacements(IDataSet wrapped) {
     *         super(wrapped);
     *         addReplacementObject("[NULL]", null);
     *     }
     * }
     * </pre>
     *
     * @since 1.3
     */
    @Parameter(property = "dbunit.replacementDataSetClass")
    protected String replacementDataSetClass;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException
    {
        if (skip)
        {
            this.getLog().info("Skip DbUnit comparison");
            return;
        }

        super.execute();

        try
        {
            final IDatabaseConnection connection = createConnection();
            try
            {
                if (replacementDataSetClass == null)
                {
                    compareWithoutReplacement(connection);
                } else
                {
                    compareWithReplacement(connection);
                }
            } finally
            {
                connection.close();
            }
        } catch (final Exception e)
        {
            throw new MojoExecutionException(
                    "Error executing DbUnit comparison.", e);
        }
    }

    /**
     * Performs the comparison by delegating to the {@link Compare} Ant task.
     * Used when {@link #replacementDataSetClass} is not set.
     *
     * @param connection the database connection to compare against
     * @throws Exception on any dataset or assertion error
     */
    private void compareWithoutReplacement(final IDatabaseConnection connection)
            throws Exception
    {
        final Compare dbUnitCompare = new Compare();
        dbUnitCompare.setSrc(src);
        dbUnitCompare.setFormat(format);
        dbUnitCompare.setSort(sort);

        for (int i = 0; queries != null && i < queries.length; ++i)
        {
            dbUnitCompare.addQuery((Query) queries[i]);
        }
        for (int i = 0; tables != null && i < tables.length; ++i)
        {
            dbUnitCompare.addTable((Table) tables[i]);
        }

        dbUnitCompare.execute(connection);
    }

    /**
     * Performs the comparison by loading the expected dataset, wrapping it
     * with the user-supplied {@link org.dbunit.dataset.ReplacementDataSet}
     * subclass, and comparing each table against the actual database contents.
     * Used when {@link #replacementDataSetClass} is set.
     *
     * @param connection the database connection to compare against
     * @throws Exception on any dataset or assertion error
     */
    private void compareWithReplacement(final IDatabaseConnection connection)
            throws Exception
    {
        final IDataSet rawExpected = loadDataset(src, format);
        final IDataSet expectedDataset = (IDataSet) Class
                .forName(replacementDataSetClass)
                .getConstructor(IDataSet.class)
                .newInstance(rawExpected);

        final boolean hasFilter =
                (tables != null && tables.length > 0)
                        || (queries != null && queries.length > 0);

        final IDataSet actualDataset = hasFilter
                ? buildFilteredActualDataset(connection)
                : connection.createDataSet();

        final String[] tableNames = hasFilter
                ? actualDataset.getTableNames()
                : expectedDataset.getTableNames();

        for (final String tableName : tableNames)
        {
            final ITable expectedTable = expectedDataset.getTable(tableName);
            final ITable actualTable = DefaultColumnFilter.includedColumnsTable(
                    actualDataset.getTable(tableName),
                    expectedTable.getTableMetaData().getColumns());

            if (sort)
            {
                Assertion.assertEquals(
                        new SortedTable(expectedTable),
                        new SortedTable(actualTable));
            } else
            {
                Assertion.assertEquals(expectedTable, actualTable);
            }
        }
    }

    /**
     * Builds an actual dataset restricted to the configured {@link #tables}
     * and {@link #queries}.
     *
     * <p>The {@link QueryDataSet} is wrapped in {@link ForwardOnlyDataSet} then
     * {@link CompositeDataSet} so the underlying {@code QueryTableIterator}
     * uses {@code nextWithoutClosing()} during initialization, keeping result
     * set tables accessible for the comparison phase.
     *
     * @param connection the database connection to query
     * @return the filtered actual dataset
     * @throws Exception on any dataset or SQL error
     */
    private IDataSet buildFilteredActualDataset(
            final IDatabaseConnection connection) throws Exception
    {
        final QueryDataSet queryDataSet = new QueryDataSet(connection);
        if (tables != null)
        {
            for (final Table table : tables)
            {
                queryDataSet.addTable(table.getName());
            }
        }
        if (queries != null)
        {
            for (final Query query : queries)
            {
                queryDataSet.addTable(query.getName(), query.getSql());
            }
        }
        return new CompositeDataSet(new IDataSet[]{new ForwardOnlyDataSet(queryDataSet)});
    }

    /**
     * Loads a dataset from {@code src} using the given {@code format}.
     *
     * @param src    the dataset file
     * @param format the dataset format (xml, flat, csv, xls, yml)
     * @return the loaded dataset
     * @throws Exception on any I/O or parsing error
     */
    private IDataSet loadDataset(final File src, final String format)
            throws Exception
    {
        if ("xml".equalsIgnoreCase(format))
        {
            return new CachedDataSet(
                    new XmlProducer(AbstractStep.getInputSource(src)));
        } else if ("flat".equalsIgnoreCase(format))
        {
            return new CachedDataSet(
                    new FlatXmlProducer(AbstractStep.getInputSource(src), true, true));
        } else if ("csv".equalsIgnoreCase(format))
        {
            return new CachedDataSet(new CsvProducer(src));
        } else if ("xls".equalsIgnoreCase(format))
        {
            return new CachedDataSet(new XlsDataSet(src));
        } else if ("yml".equalsIgnoreCase(format))
        {
            return new CachedDataSet(new YamlProducer(src), true);
        }
        throw new IllegalArgumentException("Unsupported format: " + format);
    }
}
