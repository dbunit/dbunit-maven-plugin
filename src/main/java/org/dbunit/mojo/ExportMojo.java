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
import org.dbunit.ant.Export;
import org.dbunit.ant.Query;
import org.dbunit.ant.Table;
import org.dbunit.database.IDatabaseConnection;

/**
 * Execute DbUnit Export operation
 * 
 * @goal export
 * @author <a href="mailto:dantran@gmail.com">Dan Tran</a>
 * @author <a href="mailto:topping@codehaus.org">Brian Topping</a>
 * @author <a href="mailto:david@codehaus.org">David J. M. Karlsen</a>
 * @version $Id$
 * @since 1.0
 */
public class ExportMojo
    extends AbstractDbUnitMojo
{
    /**
     * Location of exported DataSet file
     * @parameter expression="${dest}" default-value="${project.build.directory}/dbunit/export.xml"
     */
    protected File dest;
    
    /**
     * DataSet file format
     * @parameter expression="${format}" default-value="xml"
     */
    protected String format;
    
    /**
     * doctype
     * @parameter expression="${doctype}"
     */
    protected String doctype;
    
    /**
     * List of DbUnit's Table.  See DbUnit's JavaDoc for details
     * @parameter
     */
    protected Table [] tables;
    
    /**
     * List of DbUnit's Query.  See DbUnit's JavaDoc for details
     * @parameter
     */
    protected Query [] queries;
    
    /**
     * Set to true to order exported data according to integrity constraints defined in DB.
     * @parameter expression="${ordered}"
     */
    protected boolean ordered;
    
    /**
     * Encoding of exported data.
     * @parameter expression="${encoding}" default-value="${project.build.sourceEncoding}"
     */
    protected String encoding;
    

    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        if ( skip )
        {
            this.getLog().info( "Skip export execution" );
            return;
        }

        super.execute();
        
        try
        {
            //dbunit require dest directory is ready
            dest.getParentFile().mkdirs();
            
            IDatabaseConnection connection = createConnection();
            try
            {
                Export export = new Export();
                export.setOrdered( ordered );
                for ( int i = 0 ; queries != null && i < queries.length; ++ i ) 
                {
                    export.addQuery( (Query ) queries[i] );
                }
                for ( int i = 0 ; tables != null && i < tables.length; ++ i ) 
                {
                    export.addTable( (Table ) tables[i] );
                }
                
                export.setDest( dest );
                export.setDoctype( doctype );
                export.setFormat( format );
                export.setEncoding( encoding );
                
                export.execute( connection );
            }
            finally
            {
                connection.close();
            }
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( "Error executing export", e );
        }

    }
}
