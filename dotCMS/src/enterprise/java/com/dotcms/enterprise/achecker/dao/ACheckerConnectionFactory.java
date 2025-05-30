/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/
package com.dotcms.enterprise.achecker.dao;

import java.io.File;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.dotmarketing.util.ConfigUtils;
import org.h2.jdbcx.JdbcConnectionPool;

public class ACheckerConnectionFactory {

	private static final Log LOG = LogFactory.getLog(ACheckerConnectionFactory.class);

	private static JdbcConnectionPool pool = null;

	private static Connection connection = null;

	/**
	 * This method retrieves the default connection to the dotCMS DB
	 * @return
	 * @throws Exception returns an exception if the connection could not be retrieved
	 */
	public static Connection getConnection() throws Exception {
		try {
			if (connection == null || connection.isClosed()) {
				connection = getConn();
				LOG.debug(  "Connection opened for thread "  );
			}
			return connection;
		} catch (Exception e) {
			LOG.error(  "---------- DBConnectionFactory: error : " + e);
			LOG.debug(  "---------- DBConnectionFactory: error ", e);
			throw new SQLException(e.toString());
		}
	}

	/**
	 * This method retrieves the default connection to the dotCMS DB
	 * @return	the connection to the DB
	 * @throws Exception returns an exception if the connection could not be retrieved
	 * */
	private static Connection getConn() throws Exception {
		if ( pool == null ) {
			try {
				int x = 1;
				String acheckerPath = ConfigUtils.getACheckerPath() + File.separator + "achecker.sql";
				String paramLoadScript = "INIT=RUNSCRIPT FROM '" + acheckerPath + "' CHARSET 'utf8'";
				String extraParms = ";LOCK_MODE=0;DB_CLOSE_ON_EXIT=TRUE;IGNORECASE=FALSE;FILE_LOCK=NO;MODE=MySQL;" + paramLoadScript;
				File dbRoot = new File(ConfigUtils.getDynamicContentPath() + File.separator + "h2db/" + x + "/achecker_db" + x + extraParms);
				String dbRootLocation = dbRoot.getAbsolutePath();
				String connectURI = "jdbc:h2:split:nio:"+dbRootLocation;
				connectURI = connectURI.replace("\\", "/");
				pool = JdbcConnectionPool.create(connectURI, "sa", "sa");
				pool.setMaxConnections(1000);
				pool.setLoginTimeout(3);
				Connection connection = pool.getConnection();
				return connection;
			} catch (Exception e) {
				e.printStackTrace();
				System.err.println("Unable to start db properly : " + e.getMessage());
			}
		}
		else {
			return pool.getConnection();
		}

		return null;
	}

	/**
	 * This method closes all the possible opened connections
	 * @throws SQLException returns an exception if the connection could not be closed
	 */
	public static void closeConnection() throws SQLException {
		try {
			if ( connection!= null  ) {
				connection.close();
				connection = null;
			}
		} catch (Exception e) {
			LOG.error(  "---------- DBConnectionFactory: error closing the db dbconnection ---------------", e);
			throw new SQLException(e.toString());
		}

	}

	public static int getDbVersion() throws Exception{
		int version = 0;
		try {
			Connection con = getConnection();
			DatabaseMetaData meta = con.getMetaData();
			version = meta.getDatabaseMajorVersion();
		} catch (Exception e) {
			LOG.error("---------- DBConnectionFactory: Error getting DB version " + "---------------", e);
			throw new Exception(e.toString());
		}
		return version;
	}
}
