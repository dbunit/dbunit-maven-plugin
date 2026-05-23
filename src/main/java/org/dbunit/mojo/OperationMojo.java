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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.dbunit.ant.AbstractStep;
import org.dbunit.ant.Operation;
import org.dbunit.database.DatabaseSequenceFilter;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.CachedDataSet;
import org.dbunit.dataset.CompositeDataSet;
import org.dbunit.dataset.FilteredDataSet;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.csv.CsvProducer;
import org.dbunit.dataset.excel.XlsDataSet;
import org.dbunit.dataset.xml.FlatXmlProducer;
import org.dbunit.dataset.xml.XmlProducer;
import org.dbunit.dataset.yaml.YamlProducer;
import org.dbunit.operation.DatabaseOperation;
import org.dbunit.operation.TransactionOperation;

/**
 * Execute DbUnit's Database Operation with an external dataset file.
 *
 * @author <a href="mailto:dantran@gmail.com">Dan Tran</a>
 * @author <a href="mailto:topping@codehaus.org">Brian Topping</a>
 * @version $Id$
 * @since 1.0
 */
@Mojo(name = "operation", requiresDependencyResolution = ResolutionScope.COMPILE)
public class OperationMojo extends AbstractDbUnitMojo
{
    /**
     * Type of Database operation to perform. Supported types are UPDATE,
     * INSERT, DELETE, DELETE_ALL, REFRESH, CLEAN_INSERT, MSSQL_INSERT,
     * MSSQL_REFRESH, MSSQL_CLEAN_INSERT
     *
     * @since 1.0
     */
    @Parameter(property = "dbunit.type", required = true)
    protected String type;

    /**
     * When true, place the entire operation in one transaction.
     *
     * @since 1.0
     */
    @Parameter(property = "dbunit.transaction", defaultValue = "false")
    protected boolean transaction;

    /**
     * DataSet file; please use sources instead.
     *
     * @deprecated 1.0
     */
    @Deprecated
    @Parameter(property = "dbunit.src")
    protected File src;

    /**
     * DataSet files.
     *
     * @since 1.0
     */
    @Parameter(property = "dbunit.sources")
    protected File[] sources;

    /**
     * When true, merge all source files into a single composite dataset.
     *
     * @since 1.0
     */
    @Parameter(property = "dbunit.composite", defaultValue = "false")
    protected boolean composite;

    /**
     * When true, table rows from multiple sources having the same name are
     * combined into one table.<br>
     * Only relevant when composite is true.
     *
     * @since 1.0
     */
    @Parameter(property = "dbunit.combine", defaultValue = "false")
    protected boolean combine;

    /**
     * Set to true to order tables according to integrity constraints defined in
     * DB.
     *
     * @since 1.0
     */
    @Parameter(property = "dbunit.ordered", defaultValue = "false")
    protected boolean ordered;

    /**
     * Dataset file format type. Valid types are: flat, xml, csv, and dtd
     *
     * @since 1.0
     */
    @Parameter(property = "dbunit.format", defaultValue = "xml", required = true)
    protected String format;

    /**
     * Fully-qualified class name of a {@link org.dbunit.dataset.ReplacementDataSet}
     * subclass to wrap each loaded dataset before executing the operation.
     * The class must have a public constructor that accepts a single
     * {@link IDataSet} argument. Use this to apply token substitution (e.g.
     * replacing {@code [NULL]} with SQL {@code NULL}) without any coupling to
     * the plugin itself.
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
        final Log log = getLog();

        if (skip)
        {
            log.info("Skip operation: " + type + " execution");

            return;
        }

        super.execute();

        log.debug("Configuration=" + toString());

        final List concatenatedSources = new ArrayList();
        CollectionUtils.addIgnoreNull(concatenatedSources, src);
        if (sources != null)
        {
            concatenatedSources.addAll(Arrays.asList(sources));
        }

        log.info("Configured source files: count=" + concatenatedSources.size()
                + ", files=" + concatenatedSources);

        try
        {
            final IDatabaseConnection connection = createConnection();

            try
            {
                log.debug("composite set to " + composite);

                if (replacementDataSetClass == null)
                {
                    executeWithoutReplacement(connection, concatenatedSources);
                } else
                {
                    executeWithReplacement(connection, concatenatedSources);
                }
            } finally
            {
                connection.close();
            }
        } catch (final Exception e)
        {
            throw new MojoExecutionException(
                    "Error executing database operation: " + type, e);
        }
    }

    /**
     * Executes the operation by delegating to the {@link Operation} Ant task.
     * Used when {@link #replacementDataSetClass} is not set.
     *
     * @param connection the database connection
     * @param concatenatedSources the list of dataset source {@link File}s
     * @throws Exception on any dataset loading or operation error
     */
    private void executeWithoutReplacement(
            final IDatabaseConnection connection,
            final List concatenatedSources) throws Exception
    {
        if (composite)
        {
            final Operation op = new Operation();
            op.setFormat(format);
            final File[] srcFile =
                    (File[]) concatenatedSources.toArray(new File[] {});
            op.setSrc(srcFile);
            op.setOrdered(ordered);
            op.setCombine(combine);
            op.setTransaction(transaction);
            op.setType(type);

            getLog().info("Executing operation=" + op);
            op.execute(connection);
        } else
        {
            for (final Object element : concatenatedSources)
            {
                final File source = (File) element;
                getLog().debug("processing file=" + source);

                final Operation op = new Operation();
                op.setFormat(format);
                op.setSrc(source);
                op.setOrdered(ordered);
                op.setTransaction(transaction);
                op.setType(type);

                getLog().info("Executing operation=" + op);
                op.execute(connection);
            }
        }
    }

    /**
     * Executes the operation by loading datasets manually, wrapping them with
     * the user-supplied {@link org.dbunit.dataset.ReplacementDataSet} subclass,
     * and invoking {@link DatabaseOperation} directly. Used when
     * {@link #replacementDataSetClass} is set.
     *
     * @param connection the database connection
     * @param concatenatedSources the list of dataset source {@link File}s
     * @throws Exception on any dataset loading or operation error
     */
    private void executeWithReplacement(
            final IDatabaseConnection connection,
            final List concatenatedSources) throws Exception
    {
        final Operation opHelper = new Operation();
        opHelper.setType(type);
        final DatabaseOperation dbOp = opHelper.getDbOperation();

        final DatabaseOperation effectiveOp =
                transaction ? new TransactionOperation(dbOp) : dbOp;

        if (composite)
        {
            final IDataSet[] rawDatasets = new IDataSet[concatenatedSources.size()];
            for (int i = 0; i < concatenatedSources.size(); i++)
            {
                rawDatasets[i] = loadDataset((File) concatenatedSources.get(i), format);
            }
            IDataSet dataset = rawDatasets.length > 1
                    ? new CompositeDataSet(rawDatasets, combine)
                    : rawDatasets[0];
            dataset = wrapWithReplacementDataSet(dataset);
            if (ordered)
            {
                dataset = new FilteredDataSet(
                        new DatabaseSequenceFilter(connection), dataset);
            }
            effectiveOp.execute(connection, dataset);
        } else
        {
            for (final Object element : concatenatedSources)
            {
                IDataSet dataset = loadDataset((File) element, format);
                dataset = wrapWithReplacementDataSet(dataset);
                if (ordered)
                {
                    dataset = new FilteredDataSet(
                            new DatabaseSequenceFilter(connection), dataset);
                }
                effectiveOp.execute(connection, dataset);
            }
        }
    }

    /**
     * Instantiates {@link #replacementDataSetClass} with {@code raw} as the
     * constructor argument.
     *
     * @param raw the dataset to wrap
     * @return the wrapped dataset
     * @throws Exception if the class cannot be instantiated
     */
    private IDataSet wrapWithReplacementDataSet(final IDataSet raw)
            throws Exception
    {
        return (IDataSet) Class.forName(replacementDataSetClass)
                .getConstructor(IDataSet.class)
                .newInstance(raw);
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

    @Override
    public String toString()
    {
        return "OperationMojo[" + "type=" + type + ", transaction="
                + transaction + ", src=" + src + ", sources="
                + Arrays.toString(sources) + ", composite=" + composite
                + ", combine=" + combine + ", ordered=" + ordered + ", format="
                + format + ", replacementDataSetClass=" + replacementDataSetClass
                + "]";
    }
}
