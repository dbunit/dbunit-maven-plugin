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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.File;
import java.sql.ResultSet;
import java.util.Properties;

import org.apache.maven.plugin.MojoExecutionException;
import org.junit.jupiter.api.Test;

/**
 * Tests for connection and configuration handling in {@link AbstractDbUnitMojo}.
 *
 * <p>Uses {@link OperationMojo} as the concrete vehicle to exercise the abstract base class.
 */
public class AbstractDbUnitMojoConnectionTest extends AbstractDbUnitMojoTest
{
    /**
     * Tests that an unknown JDBC driver class causes a {@link MojoExecutionException}.
     */
    @Test
    public void testCreateConnection_unknownDriverClass_throwsMojoExecutionException()
    {
        final OperationMojo mojo = new OperationMojo();
        populateMojoCommonConfiguration(mojo);
        mojo.driver = "com.nonexistent.Driver";
        mojo.src = new File(p.getProperty("xmlDataSource"));
        mojo.format = "xml";
        mojo.type = "CLEAN_INSERT";

        assertThatThrownBy(mojo::execute)
            .as("Unknown JDBC driver class should throw MojoExecutionException.")
            .isInstanceOf(MojoExecutionException.class);
    }

    /**
     * Tests that a URL not understood by the driver causes a {@link MojoExecutionException}.
     */
    @Test
    public void testCreateConnection_driverReturnsNullForUrl_throwsMojoExecutionException()
    {
        final OperationMojo mojo = new OperationMojo();
        populateMojoCommonConfiguration(mojo);
        mojo.url = "jdbc:nonexistent://invalid";
        mojo.src = new File(p.getProperty("xmlDataSource"));
        mojo.format = "xml";
        mojo.type = "CLEAN_INSERT";

        assertThatThrownBy(mojo::execute)
            .as("URL not understood by driver should throw MojoExecutionException.")
            .isInstanceOf(MojoExecutionException.class);
    }

    /**
     * Tests that null username and password cause a {@link MojoExecutionException}, not an NPE.
     */
    @Test
    public void testCreateConnection_nullCredentials_throwsMojoExecutionExceptionNotNpe()
    {
        final OperationMojo mojo = new OperationMojo();
        populateMojoCommonConfiguration(mojo);
        mojo.username = null;
        mojo.password = null;
        mojo.src = new File(p.getProperty("xmlDataSource"));
        mojo.format = "xml";
        mojo.type = "CLEAN_INSERT";

        assertThatThrownBy(mojo::execute)
            .as("Null credentials should throw MojoExecutionException, not NPE.")
            .isInstanceOf(MojoExecutionException.class);
    }

    /**
     * Tests that dbconfig Properties are applied to the DatabaseConfig without throwing.
     */
    @Test
    public void testExecute_withDbconfigProperties_appliedSuccessfully() throws Exception
    {
        final Properties dbconfig = new Properties();
        dbconfig.setProperty("datatypeFactory", "org.dbunit.ext.hsqldb.HsqldbDataTypeFactory");
        dbconfig.setProperty("batchedStatements", "false");

        final OperationMojo mojo = new OperationMojo();
        populateMojoCommonConfiguration(mojo);
        mojo.dbconfig = dbconfig;
        mojo.src = new File(p.getProperty("xmlDataSource"));
        mojo.format = "xml";
        mojo.type = "CLEAN_INSERT";
        mojo.execute();

        final ResultSet rs = c.createStatement().executeQuery("select count(*) from person");
        rs.next();
        assertThat(rs.getInt(1)).as("Person count after CLEAN_INSERT with dbconfig properties.").isEqualTo(2);
    }
}
