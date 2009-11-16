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
import org.dbunit.ant.Compare;
import org.dbunit.ant.Query;
import org.dbunit.ant.Table;
import org.dbunit.database.IDatabaseConnection;

/**
 * Execute DbUnit Compare operation
 * 
 * @goal compare
 * @author <a href="mailto:dantran@gmail.com">Dan Tran</a>
 * @version $Id$
 * @since 1.0
 */
public class CompareMojo
    extends AbstractDbUnitMojo
{
    /**
     * DataSet file
     * 
     * @parameter expression="${src}"
     * @required
     */
    protected File src;
    
    /**
     * DataSet file format
     * @parameter expression="${format}" default-value="xml"
     */
    protected String format;
    
    /**
     * sort
     * @parameter expression="${sort}"
     */
    protected boolean sort;
    
    /**
     * List of DbUnit's Table.  See DbUnit's org.dbunit.ant.Table JavaDoc for details
     * @parameter
     */
    protected Table [] tables;
    
    /**
     * List of DbUnit's Query.  See DbUnit's org.dbunit.ant.Query JavaDoc for details
     * @parameter
     */
    protected Query [] queries;
    

    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        if ( skip )
        {
            this.getLog().info( "Skip DbUnit comparison" );
            return;
        }

        super.execute();
        
        try
        {
            IDatabaseConnection connection = createConnection();
            try
            {
                Compare dbUnitCompare = new Compare();
                dbUnitCompare.setSrc( src );
                dbUnitCompare.setFormat( format );
                dbUnitCompare.setSort( sort );
                
                for ( int i = 0 ; queries != null && i < queries.length; ++ i ) 
                {
                    dbUnitCompare.addQuery( (Query ) queries[i] );
                }
                for ( int i = 0 ; tables != null && i < tables.length; ++ i ) 
                {
                    dbUnitCompare.addTable( (Table ) tables[i] );
                }
                
                dbUnitCompare.execute( connection );
            }
            finally
            {
                connection.close();
            }
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( "Error executing DbUnit comparison.", e );
        }

    }
}
