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

import java.sql.*;
import java.util.*;

import baz.timekeeper.*;

/**
 * @author Sebastian Raubach
 */
public class Project extends DatabaseObject
{
	public static final String ID         = "id";
	public static final String NAME       = "name";
	public static final String AUTOSTART  = "autostart";
	public static final String VISIBILITY = "visibility";
	public static final String POSITION   = "position";

	private String  name;
	private boolean autostart;
	private boolean visibility;
	private int     position;

	public Project(Integer id)
	{
		super(id);
	}

	public Project(Integer id, String name, boolean autostart, boolean visibility, int position)
	{
		super(id);
		this.name = name;
		this.autostart = autostart;
		this.visibility = visibility;
		this.position = position;
	}

	public static List<Project> getAll() throws SQLException
	{
		try (Database db = Database.connect())
		{
			List<Project> all = new ArrayList<>();
			PreparedStatement stmt = db.preparedStatement("SELECT * FROM `projects`");

			ResultSet rs = stmt.executeQuery();

			while (rs.next())
			{
				all.add(parse(rs));
			}

			return all;
		}
	}

	private static Project parse(ResultSet rs) throws SQLException
	{
		return new Project(rs.getInt(ID))
				.setName(rs.getString(NAME))
				.setAutostart(rs.getBoolean(AUTOSTART))
				.setVisibility(rs.getBoolean(VISIBILITY))
				.setPosition(rs.getInt(POSITION));
	}

	public static Project getById(int id) throws SQLException
	{
		try (Database db = Database.connect())
		{
			PreparedStatement stmt = db.preparedStatement("SELECT * FROM `projects` WHERE `id` = ?");
			stmt.setInt(1, id);

			ResultSet rs = stmt.executeQuery();

			if (rs.next())
			{
				return parse(rs);
			}
			else
			{
				return null;
			}
		}
	}

	public static int getMaxPosition()
	{
		try
		{
			int i = 0;

			for (Project p : getAll())
				i = Math.max(i, p.getPosition());

			return i;
		}
		catch (SQLException e)
		{
			return 0;
		}
	}

	public static void sortByPosition(List<Project> projects)
	{
		Collections.sort(projects, (o1, o2) ->
		{
			int p1 = o1.getPosition();
			int p2 = o2.getPosition();

			if (p1 == p2)
			{
				p1 = o1.getId();
				p2 = o2.getId();
			}

			return (int) Math.signum(p1 - p2);
		});
	}

	public String getName()
	{
		return name;
	}

	public Project setName(String name)
	{
		this.name = name;
		return this;
	}

	public boolean isAutostart()
	{
		return autostart;
	}

	public Project setAutostart(boolean autostart)
	{
		this.autostart = autostart;
		return this;
	}

	public boolean isVisibility()
	{
		return visibility;
	}

	public Project setVisibility(boolean visibility)
	{
		this.visibility = visibility;
		return this;
	}

	public int getPosition()
	{
		return position;
	}

	public Project setPosition(int position)
	{
		this.position = position;
		return this;
	}

	@Override
	public String toString()
	{
		return "Project{" +
				"name='" + name + '\'' +
				", autostart=" + autostart +
				", visibility=" + visibility +
				", position=" + position +
				"} " + super.toString();
	}

	public boolean remove() throws SQLException
	{
		if (Timekeeper.READ_ONLY_MODE)
			return false;

		if (id != null && id >= 0)
		{
			try (Database db = Database.connect())
			{
				PreparedStatement stmt = db.preparedStatement("DELETE FROM `projects` WHERE id = ?");
				stmt.setInt(1, id);
				return stmt.execute();
			}
		}
		else
		{
			return false;
		}
	}

	public void write() throws SQLException
	{
		if (Timekeeper.READ_ONLY_MODE)
			return;

		int i = 1;
		if (id == null || id < 0)
		{
			try (Database db = Database.connect())
			{
				PreparedStatement stmt = db.preparedStatement("INSERT INTO `projects` (`name`, `autostart`, `visibility`, `position`) VALUES (?, ?, ?, ?)");
				stmt.setString(i++, name);
				stmt.setBoolean(i++, autostart);
				stmt.setBoolean(i++, visibility);
				stmt.setInt(i++, position);
				int affectedRows = stmt.executeUpdate();

				if (affectedRows > 0)
				{
					try (ResultSet generatedKeys = stmt.getGeneratedKeys())
					{
						if (generatedKeys.next())
						{
							setId(generatedKeys.getInt(1));
						}
						else
						{
							throw new SQLException("Creating item failed, no ID obtained.");
						}
					}
				}
			}
		}
		else
		{
			try (Database db = Database.connect())
			{
				PreparedStatement stmt = db.preparedStatement("UPDATE `projects` SET `name` = ?, `autostart` = ?, `visibility` = ?, `position` = ? WHERE `id` = ?");
				stmt.setString(i++, name);
				stmt.setBoolean(i++, autostart);
				stmt.setBoolean(i++, visibility);
				stmt.setInt(i++, position);
				stmt.setInt(i++, id);
				stmt.executeUpdate();
			}
		}
	}
}
