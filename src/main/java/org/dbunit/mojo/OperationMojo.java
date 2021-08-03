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
import java.util.Iterator;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.dbunit.ant.Operation;
import org.dbunit.database.IDatabaseConnection;

/**
 * Execute DbUnit's Database Operation with an external dataset file.
 *
 * @goal operation
 * @author <a href="mailto:dantran@gmail.com">Dan Tran</a>
 * @author <a href="mailto:topping@codehaus.org">Brian Topping</a>
 * @version $Id$
 * @since 1.0
 */
public class OperationMojo extends AbstractDbUnitMojo {
    /**
     * Type of Database operation to perform. Supported types are UPDATE,
     * INSERT, DELETE, DELETE_ALL, REFRESH, CLEAN_INSERT, MSSQL_INSERT,
     * MSSQL_REFRESH, MSSQL_CLEAN_INSERT
     *
     * @parameter property="type"
     * @required
     */
    protected String type;

    /**
     * When true, place the entired operation in one transaction.
     *
     * @parameter property="transaction" default-value="false"
     */
    protected boolean transaction;

    /**
     * DataSet file Please use sources instead.
     *
     * @parameter property="src"
     * @deprecated 1.0
     */
    @Deprecated
    protected File src;

    /**
     * DataSet files.
     *
     * @parameter
     */
    protected File[] sources;

    /**
     * Dataset file format type. Valid types are: flat, xml, csv, and dtd
     *
     * @parameter property="format" default-value="xml";
     * @required
     */
    protected String format;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            this.getLog().info("Skip operation: " + type + " execution");

            return;
        }

        super.execute();

        final List concatenatedSources = new ArrayList();
        CollectionUtils.addIgnoreNull(concatenatedSources, src);
        if (sources != null) {
            concatenatedSources.addAll(Arrays.asList(sources));
        }

        try {
            final IDatabaseConnection connection = createConnection();

            try {
                for (final Iterator i = concatenatedSources.iterator(); i
                        .hasNext();) {
                    final File source = (File) i.next();
                    final Operation op = new Operation();
                    op.setFormat(format);
                    op.setSrc(source);
                    op.setTransaction(transaction);
                    op.setType(type);
                    op.execute(connection);
                }
            } finally {
                connection.close();
            }
        } catch (final Exception e) {
            throw new MojoExecutionException(
                    "Error executing database operation: " + type, e);
        }
    }
}
