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
import org.dbunit.ant.Export;
import org.dbunit.ant.Query;
import org.dbunit.ant.Table;
import org.dbunit.database.IDatabaseConnection;

/**
 * Execute DbUnit Export operation.
 *
 * @author <a href="mailto:dantran@gmail.com">Dan Tran</a>
 * @author <a href="mailto:topping@codehaus.org">Brian Topping</a>
 * @author <a href="mailto:david@codehaus.org">David J. M. Karlsen</a>
 * @version $Id$
 * @since 1.0
 */
@Mojo(name = "export", requiresDependencyResolution = ResolutionScope.COMPILE)
public class ExportMojo extends AbstractDbUnitMojo
{
    /**
     * Location of exported DataSet file.
     *
     * @since 1.0
     */
    @Parameter(property = "dbunit.dest", defaultValue = "${project.build.directory}/dbunit/export.xml")
    protected File dest;

    /**
     * DataSet file format.
     *
     * @since 1.0
     */
    @Parameter(property = "dbunit.format", defaultValue = "xml")
    protected String format;

    /**
     * doctype.
     *
     * @since 1.0
     */
    @Parameter(property = "dbunit.doctype")
    protected String doctype;

    /**
     * List of DbUnit's Table. See DbUnit's JavaDoc for details.
     *
     * @since 1.0
     */
    @Parameter
    protected Table[] tables;

    /**
     * List of DbUnit's Query. See DbUnit's JavaDoc for details.
     *
     * @since 1.0
     */
    @Parameter
    protected Query[] queries;

    /**
     * Set to true to order exported data according to integrity constraints
     * defined in DB.
     *
     * @since 1.0
     */
    @Parameter(property = "dbunit.ordered")
    protected boolean ordered;

    /**
     * Encoding of exported data.
     *
     * @since 1.0
     */
    @Parameter(property = "dbunit.encoding", defaultValue = "${project.build.sourceEncoding}")
    protected String encoding;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException
    {
        if (skip)
        {
            this.getLog().info("Skip export execution");
            return;
        }

        super.execute();

        try
        {
            // dbunit require dest directory is ready
            dest.getParentFile().mkdirs();

            final IDatabaseConnection connection = createConnection();
            try
            {
                final Export export = new Export();
                export.setOrdered(ordered);
                for (int i = 0; queries != null && i < queries.length; ++i)
                {
                    export.addQuery((Query) queries[i]);
                }
                for (int i = 0; tables != null && i < tables.length; ++i)
                {
                    export.addTable((Table) tables[i]);
                }

                export.setDest(dest);
                export.setDoctype(doctype);
                export.setFormat(format);
                if(encoding != null) {
                    export.setEncoding(encoding);
                }

                export.execute(connection);
            } finally
            {
                connection.close();
            }
        } catch (final Exception e)
        {
            throw new MojoExecutionException("Error executing export", e);
        }
    }
}
