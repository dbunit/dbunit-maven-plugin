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
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * @author <a href="mailto:dantran@gmail.com">Dan Tran</a>
 * @version $Id$
 */
public class OperationMojoTest
    extends AbstractDbUnitMojoTest
{
    
    public void testCleanInsertOperation()
        throws Exception
    {
        //init database with fixed data
        OperationMojo operation = new OperationMojo();
        this.populateMojoCommonConfiguration( operation );
        operation.src = new File( p.getProperty( "xmlDataSource" ) );
        operation.format = "xml";
        operation.type = "CLEAN_INSERT";
        operation.execute();
        
        //check to makesure we have 2 rows after inserts thru dataset
        Statement st = c.createStatement();
        ResultSet rs = st.executeQuery( "select count(*) from person" );
        rs.next();
        assertEquals( 2, rs.getInt(1) );  
        
        //export database to another dataset file
        File exportFile = new File( getBasedir(), "target/export.xml" );
        ExportMojo export = new ExportMojo();
        this.populateMojoCommonConfiguration( export );
        export.dest = exportFile;
        export.format = "xml";
        export.execute();
        
        //then import the exported dataset file back to DB
        operation.src = exportFile;
        operation.execute();
        
        //check to makesure we have 2 rows
        st = c.createStatement();
        rs = st.executeQuery( "select count(*) from person" );
        rs.next();
        assertEquals( 2, rs.getInt(1) );     
        
        //finally compare the current contents of the DB with the orginal dataset file
        CompareMojo compare = new CompareMojo();
        this.populateMojoCommonConfiguration( compare );
        compare.src = new File( p.getProperty( "xmlDataSource" ) );
        compare.format = "xml";
        compare.sort =  false ;
        compare.execute();
    }

    public void testSkip()
        throws Exception
    {
        //init database with fixed data
        OperationMojo operation = new OperationMojo();
        this.populateMojoCommonConfiguration( operation );
        operation.src = new File( p.getProperty( "xmlDataSource" ) );
        operation.format = "xml";
        operation.type = "CLEAN_INSERT";
        operation.skip = true;
        operation.execute();
            
        //check to makesure we have 0 rows
        Statement st = c.createStatement();
        ResultSet rs = st.executeQuery( "select count(*) from person" );
        rs.next();
        //no data  since skip is set
        assertEquals( 0, rs.getInt(1) );           
    }
}
