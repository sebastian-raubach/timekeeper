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
import java.text.*;
import java.util.*;
import java.util.Date;

import baz.timekeeper.*;

/**
 * @author Sebastian Raubach
 */
public class HistoryData extends DatabaseObject
{
	//	public static final SimpleDateFormat SDF_DATETIME = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
	public static final SimpleDateFormat SDF_DATE = new SimpleDateFormat("yyyy-MM-dd");

	public static final String ID         = "id";
	public static final String PROJECT_ID = "project_id";
	public static final String DATE       = "date";
	public static final String TIME       = "time";

	private Project project;
	private Date    date;
	private int     time;

	public HistoryData(Integer id)
	{
		super(id);
	}

	public HistoryData(Integer id, Project project, Date date, int time)
	{
		super(id);
		this.project = project;
		this.date = date;
		this.time = time;
	}

	public static List<HistoryData> getAll() throws SQLException
	{
		try (Database db = Database.connect())
		{
			List<HistoryData> all = new ArrayList<>();
			PreparedStatement stmt = db.preparedStatement("SELECT * FROM `historydata`");

			ResultSet rs = stmt.executeQuery();

			while (rs.next())
			{
				all.add(parse(rs));
			}

			return all;
		}
		catch (ParseException e)
		{
			throw new SQLException(e);
		}
	}

	public static Map<Project, HistoryData> getAllForToday() throws SQLException
	{
		try (Database db = Database.connect())
		{
			Map<Project, HistoryData> all = new HashMap<>();
			PreparedStatement stmt = db.preparedStatement("SELECT * FROM `historydata` WHERE date(`date`) = ?");
			stmt.setString(1, SDF_DATE.format(new Date()));

			ResultSet rs = stmt.executeQuery();

			while (rs.next())
			{
				HistoryData d = parse(rs);

				all.put(d.getProject(), d);
			}

			return all;
		}
		catch (ParseException e)
		{
			throw new SQLException(e);
		}
	}

	private static HistoryData parse(ResultSet rs) throws SQLException, ParseException
	{
		return new HistoryData(rs.getInt(ID))
				.setProject(Project.getById(rs.getInt(PROJECT_ID)))
				.setDate(SDF_DATE.parse(rs.getString(DATE)))
				.setTime(rs.getInt(TIME));
	}

	public static boolean removeForProject(Project project) throws SQLException
	{
		if (Timekeeper.READ_ONLY_MODE)
			return false;

		try (Database db = Database.connect())
		{
			PreparedStatement stmt = db.preparedStatement("DELETE FROM `historydata` WHERE `project_id` = ?");
			stmt.setInt(1, project.getId());
			return stmt.execute();
		}
	}

	public Project getProject()
	{
		return project;
	}

	public HistoryData setProject(Project project)
	{
		this.project = project;
		return this;
	}

	public Date getDate()
	{
		return date;
	}

	public HistoryData setDate(Date date)
	{
		this.date = date;
		return this;
	}

	public int getTime()
	{
		return time;
	}

	public HistoryData setTime(int time)
	{
		this.time = time;
		return this;
	}

	@Override
	public String toString()
	{
		return "HistoryData{" +
				"project=" + project +
				", date=" + date +
				", time=" + time +
				"} " + super.toString();
	}

	public void write() throws SQLException
	{
		if (Timekeeper.READ_ONLY_MODE)
			return;

		/* Check if the project (still) exists */
		Project p = Project.getById(project.getId());

		if (p == null)
			return;

		int i = 1;
		if (id == null)
		{
			try (Database db = Database.connect())
			{
				PreparedStatement stmt = db.preparedStatement("INSERT INTO `historydata` (`project_id`, `date`, `time`) VALUES (?, date(?), ?)");
				stmt.setInt(i++, project.getId());
				stmt.setString(i++, SDF_DATE.format(date));
				stmt.setInt(i++, time);
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
				PreparedStatement stmt = db.preparedStatement("UPDATE `historydata` SET `project_id` = ?, `date` = date(?), `time` = ? WHERE `id` = ?");
				stmt.setInt(i++, project.getId());
				stmt.setString(i++, SDF_DATE.format(date));
				stmt.setInt(i++, time);
				stmt.setInt(i++, id);
				stmt.executeUpdate();
			}
		}
	}

	@Override
	public int hashCode()
	{
		if (id == null)
		{
			int result = 1;
			result = 31 * result + (project != null ? project.hashCode() : 0);
			result = 31 * result + (date != null ? date.hashCode() : 0);
			return result;
		}
		else
		{
			return super.hashCode();
		}
	}
}
