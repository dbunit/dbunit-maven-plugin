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
import org.apache.maven.plugins.annotations.Parameter;
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
 * Common configurations for all DBUnit operations.
 *
 * @author <a href="mailto:dantran@gmail.com">Dan Tran</a>
 * @author <a href="mailto:topping@codehaus.org">Brian Topping</a>
 * @version $Id$
 * @since 1.0
 */
public abstract class AbstractDbUnitMojo extends AbstractMojo
{

    /**
     * The class name of the JDBC driver to be used.
     *
     * @since 1.0
     */
    @Parameter(property = "dbunit.driver", required = true)
    protected String driver;

    /**
     * Database username. If not given, it will be looked up through
     * settings.xml's server with ${settingsKey} as key.
     *
     * @since 1.0
     */
    @Parameter(property = "dbunit.username")
    protected String username;

    /**
     * Database password. If not given, it will be looked up through
     * settings.xml's server with ${settingsKey} as key.
     *
     * @since 1.0
     */
    @Parameter(property = "dbunit.password")
    protected String password;

    /**
     * The JDBC URL for the database to access, e.g. jdbc:db2:SAMPLE.
     *
     * @since 1.0
     */
    @Parameter(property = "dbunit.url", required = true)
    protected String url;

    /**
     * The schema name that tables can be found under.
     *
     * @since 1.0
     */
    @Parameter(property = "dbunit.schema")
    protected String schema;

    /**
     * DB configuration child element to configure {@link DatabaseConfig}
     * properties in a generic way. This makes the many attributes/properties in
     * this class obsolete and sets the value directly where it should go into
     * which is the {@link DatabaseConfig}.
     *
     * @since 1.0
     */
    @Parameter(property = "dbunit.dbconfig")
    protected Properties dbconfig;

    /**
     * Set the DataType factory to add support for non-standard database vendor
     * data types.
     *
     * @deprecated since 1.0 - use the {@link #dbconfig} attribute and the
     *             nested elements for this
     */
    @Deprecated
    @Parameter(property = "dbunit.dataTypeFactoryName", defaultValue = "org.dbunit.dataset.datatype.DefaultDataTypeFactory")
    protected String dataTypeFactoryName =
            "org.dbunit.dataset.datatype.DefaultDataTypeFactory";

    /**
     * Enable or disable usage of JDBC batched statement by DbUnit.
     *
     * @deprecated since 1.0 - use the {@link #dbconfig} attribute and the
     *             nested elements for this
     */
    @Deprecated
    @Parameter(property = "dbunit.supportBatchStatement", defaultValue = "false")
    protected boolean supportBatchStatement;

    /**
     * Enable or disable multiple schemas support by prefixing table names with
     * the schema name.
     *
     * @deprecated since 1.0 - use the {@link #dbconfig} attribute and the
     *             nested elements for this
     */
    @Deprecated
    @Parameter(property = "dbunit.useQualifiedTableNames", defaultValue = "false")
    protected boolean useQualifiedTableNames;

    /**
     * Enable or disable the warning message displayed when DbUnit encounter an
     * unsupported data type.
     *
     * @deprecated since 1.0 - use the {@link #dbconfig} attribute and the
     *             nested elements for this
     */
    @Deprecated
    @Parameter(property = "dbunit.datatypeWarning", defaultValue = "false")
    protected boolean datatypeWarning;

    /**
     * escapePattern.
     *
     * @deprecated since 1.0 - use the {@link #dbconfig} attribute and the
     *             nested elements for this
     */
    @Deprecated
    @Parameter(property = "dbunit.escapePattern")
    protected String escapePattern;

    /**
     * skipOracleRecycleBinTables.
     *
     * @since 1.0-beta-2
     * @deprecated since 1.0 - use the {@link #dbconfig} attribute and the
     *             nested elements for this
     */
    @Deprecated
    @Parameter(property = "dbunit.skipOracleRecycleBinTables", defaultValue = "false")
    protected boolean skipOracleRecycleBinTables;

    /**
     * Skip the execution when true, very handy when using together with
     * maven.test.skip.
     *
     * @since 1.0
     */
    @Parameter(property = "dbunit.skip", defaultValue = "false")
    protected boolean skip;

    /**
     * Access to hiding username/password.
     */
    @Parameter(defaultValue = "${settings}", readonly = true)
    private Settings settings;

    /**
     * Server's id in settings.xml to look up username and password. Default to
     * ${url} if not given.
     *
     * @since 1.0
     */
    @Parameter(property = "dbunit.settingsKey")
    private String settingsKey;

    /**
     * Class name of metadata handler.
     *
     * @since 1.0-beta-3
     * @deprecated since 1.0 - use the {@link #dbconfig} attribute and the
     *             nested elements for this
     */
    @Deprecated
    @Parameter(property = "dbunit.metadataHandlerName", defaultValue = "org.dbunit.database.DefaultMetadataHandler")
    protected String metadataHandlerName;

    /**
     * Be case sensitive when handling tables.
     *
     * @see http://dbunit.sourceforge.net/properties.html#casesensitivetablenames
     *
     * @deprecated since 1.0 - use the {@link #dbconfig} attribute and the
     *             nested elements for this
     */
    @Deprecated
    @Parameter(property = "dbunit.caseSensitiveTableNames", defaultValue = "false")
    private boolean caseSensitiveTableNames;

    ////////////////////////////////////////////////////////////////////

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException
    {
        loadUserInfoFromSettings();
    }

    IDatabaseConnection createConnection() throws Exception
    {

        // Instantiate JDBC driver
        final Class dc = Class.forName(driver);
        final Driver driverInstance = (Driver) dc.newInstance();
        final Properties info = new Properties();
        info.put("user", username);

        if (password != null)
        {
            info.put("password", password);
        }

        final Connection conn = driverInstance.connect(url, info);

        if (conn == null)
        {
            // Driver doesn't understand the URL
            throw new SQLException("No suitable Driver for " + url);
        }
        conn.setAutoCommit(true);

        final IDatabaseConnection connection =
                new DatabaseConnection(conn, schema);
        final DatabaseConfig config = connection.getConfig();

        // TODO this method is only here for backwards compatibility and should
        // not be used anymore. Should be removed in the next major release.
        initializeDbConfigWithOldProps(config);

        if (this.dbconfig != null)
        {
            getLog().debug(
                    "Setting dbconfig properties on the database config. "
                            + dbconfig);
            try
            {
                config.setPropertiesByString(this.dbconfig);
                getLog().debug("DatabaseConfig updated from <dbconfig> element="
                        + config);
            } catch (final DatabaseUnitException e)
            {
                throw new MojoExecutionException(
                        "Could not populate dbunit config object", e);
            }
        } else
        {
            getLog().debug("No <dbconfig> element specified, skipping");
        }

        return connection;
    }

    /**
     * Initializes the given {@link DatabaseConfig} instance using field values
     * of this mojo. TODO this method is only here for backwards compatibility
     * and should not be used anymore. Should be removed in the next major
     * release.
     *
     * @param config
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws ClassNotFoundException
     * @deprecated since 1.0 - prefer the generic {@link #dbconfig} properties
     */
    @Deprecated
    private void initializeDbConfigWithOldProps(final DatabaseConfig config)
            throws InstantiationException, IllegalAccessException,
            ClassNotFoundException
    {
        config.setFeature(DatabaseConfig.FEATURE_BATCHED_STATEMENTS,
                supportBatchStatement);
        config.setFeature(DatabaseConfig.FEATURE_QUALIFIED_TABLE_NAMES,
                useQualifiedTableNames);
        config.setFeature(DatabaseConfig.FEATURE_DATATYPE_WARNING,
                datatypeWarning);
        config.setFeature(DatabaseConfig.FEATURE_SKIP_ORACLE_RECYCLEBIN_TABLES,
                this.skipOracleRecycleBinTables);
        config.setFeature(DatabaseConfig.FEATURE_CASE_SENSITIVE_TABLE_NAMES,
                caseSensitiveTableNames);

        config.setProperty(DatabaseConfig.PROPERTY_ESCAPE_PATTERN,
                escapePattern);
        config.setProperty(DatabaseConfig.PROPERTY_RESULTSET_TABLE_FACTORY,
                new ForwardOnlyResultSetTableFactory());

        // Setup data type factory
        final IDataTypeFactory dataTypeFactory = (IDataTypeFactory) Class
                .forName(dataTypeFactoryName).newInstance();
        config.setProperty(DatabaseConfig.PROPERTY_DATATYPE_FACTORY,
                dataTypeFactory);

        // Setup metadata handler
        final IMetadataHandler metadataHandler = (IMetadataHandler) Class
                .forName(metadataHandlerName).newInstance();
        config.setProperty(DatabaseConfig.PROPERTY_METADATA_HANDLER,
                metadataHandler);

        getLog().debug("DatabaseConfig initialized from properties=" + config);
    }

    /**
     * Load username password from settings if user has not set them in JVM
     * properties
     */
    private void loadUserInfoFromSettings() throws MojoExecutionException
    {
        if (this.settingsKey == null)
        {
            this.settingsKey = url;
        }

        if ((username == null || password == null) && (settings != null))
        {
            final Server server = this.settings.getServer(this.settingsKey);

            if (server != null)
            {
                if (username == null)
                {
                    username = server.getUsername();
                }

                if (password == null)
                {
                    password = server.getPassword();
                }
            }
        }

        if (username == null)
        {
            // allow emtpy username
            username = "";
        }

        if (password == null)
        {
            // allow emtpy password
            password = "";
        }
    }
}
