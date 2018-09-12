/*
 * Copyright 2017 Sebastian Raubach
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package baz.timekeeper.database;

import java.io.*;
import java.nio.file.*;
import java.sql.*;

import baz.timekeeper.util.*;

/**
 * @author Sebastian Raubach
 */
public class Database implements AutoCloseable
{
	private static File DATABASE_FILE;

	static
	{
		DATABASE_FILE = new File(new File(System.getProperty("user.home"), "." + TimesheetPropertyReader.PROPERTIES_FOLDER), "timekeeper.db");

		if (!DATABASE_FILE.getParentFile().exists())
			DATABASE_FILE.getParentFile().mkdirs();

		File OLD_DATABASE_FILE = new File(new File(System.getProperty("user.home"), "." + TimesheetPropertyReader.PROPERTIES_FOLDER), "timesheetinator.db");

		if (OLD_DATABASE_FILE.exists())
		{
			try
			{
				Files.move(OLD_DATABASE_FILE.toPath(), DATABASE_FILE.toPath());
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}

		if (!DATABASE_FILE.exists())
			init();
	}

	private Connection connection;

	private static void init()
	{
		try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + DATABASE_FILE.toURI().toString()))
		{
			connection.prepareStatement("CREATE TABLE `projects` ( `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, `name` TEXT NOT NULL, `autostart` INTEGER NOT NULL DEFAULT 0, `visibility` INTEGER NOT NULL DEFAULT 1, `position` INTEGER NOT NULL DEFAULT 0);").execute();
			connection.prepareStatement("CREATE TABLE `historydata` ( `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, `project_id` INTEGER NOT NULL, `date` DATETIME NOT NULL, `time` INTEGER NOT NULL );").execute();
			connection.prepareStatement("CREATE TABLE `dailylog` (`id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, `date` DATETIME NOT NULL, `start` DATETIME NOT NULL, `end` DATETIME NOT NULL );").execute();
		}
		catch (SQLException e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * Connects to the given MySQL database with specified credentials
	 *
	 * @throws SQLException Thrown if any kind of {@link Exception} is thrown while trying to connect. The {@link SQLException} will contain the
	 *                      message of the original {@link Exception}.
	 */
	public static Database connect()
		throws SQLException
	{
		Database database = new Database();

		/* Connect to the database */
		database.connection = DriverManager.getConnection("jdbc:sqlite:" + DATABASE_FILE.toURI().toString());

		return database;
	}

	PreparedStatement preparedStatement(String sql)
		throws SQLException
	{
		return connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
	}

	@Override
	public void close()
		throws SQLException
	{
		connection.close();
	}
}
