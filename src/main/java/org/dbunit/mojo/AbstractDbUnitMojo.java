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

import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.Properties;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.dbunit.DatabaseUnitException;
import org.dbunit.database.DatabaseConfig;
import org.dbunit.database.DatabaseConnection;
import org.dbunit.database.ForwardOnlyResultSetTableFactory;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.database.IMetadataHandler;
import org.dbunit.dataset.datatype.IDataTypeFactory;

/**
 * Common configurations for all DBUnit operations
 * @author <a href="mailto:dantran@gmail.com">Dan Tran</a>
 * @author <a href="mailto:topping@codehaus.org">Brian Topping</a>
 * @version $Id$
 * @requiresDependencyResolution compile
 * @since 1.0
 */
public abstract class AbstractDbUnitMojo
    extends AbstractMojo
{

    /**
     * The class name of the JDBC driver to be used.
     * 
     * @parameter expression="${driver}" 
     * @required
     */
    protected String driver;

    /**
     * Database username.  If not given, it will be looked up through 
     * settings.xml's server with ${settingsKey} as key
     * @parameter expression="${username}" 
     */
    protected String username;

    /**
     * Database password. If not given, it will be looked up through settings.xml's 
     * server with ${settingsKey} as key
     * @parameter expression="${password}" 
     */
    protected String password;

    /**
     * The JDBC URL for the database to access, e.g. jdbc:db2:SAMPLE.
     * 
     * @parameter expression="${url}" 
     * @required
     */
    protected String url;

    /**
     * The schema name that tables can be found under.
     * 
     * @parameter expression="${schema}" 
     */
    protected String schema;

    /**
     * DB configuration child element to configure {@link DatabaseConfig} properties
     * in a generic way. This makes the many attributes/properties in this class obsolete and
     * sets the value directly where it should go into which is the {@link DatabaseConfig}.
     * @parameter expression="${dbconfig}" 
     * @since 1.0
     */
    protected Properties dbconfig;

    /**
     * Set the DataType factory to add support for non-standard database vendor data types.
     * 
     * @parameter expression="${dataTypeFactoryName}" default-value="org.dbunit.dataset.datatype.DefaultDataTypeFactory"
     * @deprecated since 1.0 - use the {@link #dbconfig} attribute and the nested elements for this
     */
    protected String dataTypeFactoryName = "org.dbunit.dataset.datatype.DefaultDataTypeFactory";

    /**
     * Enable or disable usage of JDBC batched statement by DbUnit
     * @parameter expression="${supportBatchStatement}" default-value="false"
     * @deprecated since 1.0 - use the {@link #dbconfig} attribute and the nested elements for this
     */
    protected boolean supportBatchStatement;

    /**
     * Enable or disable multiple schemas support by prefixing table names with the schema name.
     * 
     * @parameter expression="${useQualifiedTableNames}" default-value="false"
     * @deprecated since 1.0 - use the {@link #dbconfig} attribute and the nested elements for this
     */
    protected boolean useQualifiedTableNames;

    /**
     * Enable or disable the warning message displayed when DbUnit encounter an unsupported data type.
     * @parameter expression="${datatypeWarning}" default-value="false"
     * @deprecated since 1.0 - use the {@link #dbconfig} attribute and the nested elements for this
     */
    protected boolean datatypeWarning;

    /**
     * escapePattern
     * 
     * @parameter expression="${escapePattern}" 
     * @deprecated since 1.0 - use the {@link #dbconfig} attribute and the nested elements for this
     */
    protected String escapePattern;

    /**
     * skipOracleRecycleBinTables
     * 
     * @parameter expression="${escapePattern}" default-value="false"
     * @since 1.0-beta-2
     * @deprecated since 1.0 - use the {@link #dbconfig} attribute and the nested elements for this
     */
    protected boolean skipOracleRecycleBinTables;
    
    /**
     * Skip the execution when true, very handy when using together with maven.test.skip.
     * 
     * @parameter expression="${skip}" default-value="false"
     */
    protected boolean skip;
    
    /**
     * Access to hidding username/password
     * @parameter expression="${settings}"
     * @readonly
     */
    private Settings settings;

    /**
     * Server's id in settings.xml to look up username and password.
     * Default to ${url} if not given.
     * @parameter expression="${settingsKey}" 
     */
    private String settingsKey;

    /**
     * Class name of metadata handler.
     * @parameter expression="${metadataHandlerName}" default-value="org.dbunit.database.DefaultMetadataHandler"
     * @since 1.0-beta-3
     * @deprecated since 1.0 - use the {@link #dbconfig} attribute and the nested elements for this
     */
    protected String metadataHandlerName;

    /**
     * Be case sensitive when handling tables.
     * @see http://www.dbunit.org/properties.html#casesensitivetablenames
     * 
     * @parameter default-value="false"
     * @deprecated since 1.0 - use the {@link #dbconfig} attribute and the nested elements for this
     */
    private boolean caseSensitiveTableNames;


    ////////////////////////////////////////////////////////////////////


    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        loadUserInfoFromSettings();
    }

    IDatabaseConnection createConnection()
        throws Exception
    {

        // Instantiate JDBC driver
        Class dc = Class.forName( driver );
        Driver driverInstance = (Driver) dc.newInstance();
        Properties info = new Properties();
        info.put( "user", username );

        if ( password != null )
        {
            info.put( "password", password );
        }

        Connection conn = driverInstance.connect( url, info );

        if ( conn == null )
        {
            // Driver doesn't understand the URL
            throw new SQLException( "No suitable Driver for " + url );
        }
        conn.setAutoCommit( true );

        IDatabaseConnection connection = new DatabaseConnection( conn, schema );
        DatabaseConfig config = connection.getConfig();
        
        //TODO this method is only here for backwards compatibility and should not be used anymore. Should be removed in the next major release.
        initializeDbConfigWithOldProps(config);
        
        if(this.dbconfig != null){
            getLog().debug("Setting dbconfig properties on the database config. " + dbconfig);
            try {
                config.setPropertiesByString(this.dbconfig);
            }
            catch(DatabaseUnitException e)
            {
                throw new MojoExecutionException("Could not populate dbunit config object", e);
            }
        }
        else {
            getLog().debug("No dbconfig element specified");
        }
        
        return connection;
    }

    /**
     * Initializes the given {@link DatabaseConfig} instance using field values of this mojo.
     * TODO this method is only here for backwards compatibility and should not be used anymore. Should be removed in the next major release. 
     * @param config
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws ClassNotFoundException
     * @deprecated since 1.0 - prefer the generic {@link #dbconfig} properties
     */
    private void initializeDbConfigWithOldProps(DatabaseConfig config) 
    throws InstantiationException, IllegalAccessException, ClassNotFoundException 
    {
        config.setFeature( DatabaseConfig.FEATURE_BATCHED_STATEMENTS, supportBatchStatement );
        config.setFeature( DatabaseConfig.FEATURE_QUALIFIED_TABLE_NAMES, useQualifiedTableNames );
        config.setFeature( DatabaseConfig.FEATURE_DATATYPE_WARNING, datatypeWarning );
        config.setFeature( DatabaseConfig.FEATURE_SKIP_ORACLE_RECYCLEBIN_TABLES, this.skipOracleRecycleBinTables );
        config.setFeature( DatabaseConfig.FEATURE_CASE_SENSITIVE_TABLE_NAMES, caseSensitiveTableNames );
        
        config.setProperty( DatabaseConfig.PROPERTY_ESCAPE_PATTERN, escapePattern );
        config.setProperty( DatabaseConfig.PROPERTY_RESULTSET_TABLE_FACTORY, new ForwardOnlyResultSetTableFactory() );

        // Setup data type factory
        IDataTypeFactory dataTypeFactory = (IDataTypeFactory) Class.forName( dataTypeFactoryName ).newInstance();
        config.setProperty( DatabaseConfig.PROPERTY_DATATYPE_FACTORY, dataTypeFactory );

        // Setup metadata handler
        IMetadataHandler metadataHandler = (IMetadataHandler) Class.forName( metadataHandlerName ).newInstance();
        config.setProperty( DatabaseConfig.PROPERTY_METADATA_HANDLER, metadataHandler );
    }

    /**
     * Load username password from settings if user has not set them in JVM properties
     */
    private void loadUserInfoFromSettings()
        throws MojoExecutionException
    {
        if ( this.settingsKey == null )
        {
            this.settingsKey = url;
        }

        if ( ( username == null || password == null ) && ( settings != null ) )
        {
            Server server = this.settings.getServer( this.settingsKey );

            if ( server != null )
            {
                if ( username == null )
                {
                    username = server.getUsername();
                }

                if ( password == null )
                {
                    password = server.getPassword();
                }
            }
        }

        if ( username == null )
        {
            //allow emtpy username
            username =  "" ;
        }

        if ( password == null )
        {
            //allow emtpy password
            password = "" ;
        }
    }


}
