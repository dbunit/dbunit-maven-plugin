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
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.dbunit.database.DatabaseSequenceFilter;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.FilteredDataSet;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.xml.FlatDtdDataSet;

/**
 * Execute DbUnit Export DTD operation. Exports a flat DTD schema from the
 * connected database, with tables ordered by foreign key constraints.
 * The generated DTD can be used to validate flat XML dataset files and to
 * enable IDE auto-completion.
 *
 * @author <a href="mailto:dantran@gmail.com">Dan Tran</a>
 * @author <a href="mailto:topping@codehaus.org">Brian Topping</a>
 * @author <a href="mailto:david@codehaus.org">David J. M. Karlsen</a>
 * @version $Id$
 * @since 1.2.2
 */
@Mojo(name = "export-dtd", requiresDependencyResolution = ResolutionScope.COMPILE)
public class ExportDtdMojo extends AbstractDbUnitMojo
{
    /**
     * Location of the exported DTD file.
     *
     * @since 1.2.2
     */
    @Parameter(property = "dbunit.dest", defaultValue = "${project.build.directory}/dbunit/export.dtd")
    protected File dest;

    /**
     * Encoding of the exported DTD file.
     *
     * @since 1.2.2
     */
    @Parameter(property = "dbunit.encoding", defaultValue = "${project.build.sourceEncoding}")
    protected String encoding;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException
    {
        if (skip)
        {
            this.getLog().info("Skip export-dtd execution");
            return;
        }

        super.execute();

        try
        {
            dest.getParentFile().mkdirs();

            final IDatabaseConnection connection = createConnection();
            try
            {
                final IDataSet dataSet = new FilteredDataSet(
                        new DatabaseSequenceFilter(connection),
                        connection.createDataSet());

                final String effectiveEncoding = (encoding != null && !encoding.isEmpty())
                        ? encoding
                        : Charset.defaultCharset().name();

                try (final OutputStream fileOut = new FileOutputStream(dest);
                     final Writer writer = new OutputStreamWriter(fileOut, effectiveEncoding))
                {
                    FlatDtdDataSet.write(dataSet, writer);
                }
            } finally
            {
                connection.close();
            }
        } catch (final Exception e)
        {
            throw new MojoExecutionException("Error executing export-dtd", e);
        }
    }
}
