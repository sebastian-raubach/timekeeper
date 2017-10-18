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

package baz.timesheetinator.database;

import java.sql.*;
import java.text.*;
import java.util.*;
import java.util.Date;

import baz.timesheetinator.*;

/**
 * @author Sebastian Raubach
 */
public class DailyLog extends DatabaseObject
{
	public static final SimpleDateFormat SDF_DATE      = new SimpleDateFormat("yyyy-MM-dd");
	public static final SimpleDateFormat SDF_DATE_TIME = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	public static final String ID    = "id";
	public static final String DATE  = "date";
	public static final String START = "start";
	public static final String END   = "end";

	private Date date;
	private Date start;
	private Date end;

	public DailyLog(Integer id)
	{
		super(id);
	}

	public DailyLog(Integer id, Date date, Date start, Date end)
	{
		super(id);
		this.date = date;
		this.start = start;
		this.end = end;
	}

	public static DailyLog getForToday() throws SQLException
	{
		return getForDay(new Date(System.currentTimeMillis()));
	}

	public static DailyLog getForDay(Date day) throws SQLException
	{
		DailyLog result = null;

		try (Database db = Database.connect())
		{
			PreparedStatement stmt = db.preparedStatement("SELECT * FROM `dailylog` WHERE date(`date`) = ?");
			stmt.setString(1, SDF_DATE.format(day));

			ResultSet rs = stmt.executeQuery();

			if (rs.next())
			{
				result = parse(rs);
			}
		}
		catch (ParseException e)
		{
			throw new SQLException(e);
		}

		return result;
	}

	public static List<DailyLog> getAll() throws SQLException
	{
		try (Database db = Database.connect())
		{
			List<DailyLog> all = new ArrayList<>();
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

	private static DailyLog parse(ResultSet rs) throws SQLException, ParseException
	{
		return new DailyLog(rs.getInt(ID))
				.setDate(SDF_DATE.parse(rs.getString(DATE)))
				.setStart(SDF_DATE_TIME.parse(rs.getString(START)))
				.setEnd(SDF_DATE_TIME.parse(rs.getString(END)));
	}

	public Date getDate()
	{
		return date;
	}

	public DailyLog setDate(Date date)
	{
		this.date = date;
		return this;
	}

	public Date getStart()
	{
		return start;
	}

	public DailyLog setStart(Date start)
	{
		this.start = start;
		return this;
	}

	public Date getEnd()
	{
		return end;
	}

	public DailyLog setEnd(Date end)
	{
		this.end = end;
		return this;
	}

	@Override
	public String toString()
	{
		return "DailyLog{" +
				"date=" + date +
				", start=" + start +
				", end=" + end +
				"} " + super.toString();
	}

	public void write() throws SQLException
	{
		if (Timesheetinator.READ_ONLY_MODE)
			return;

		int i = 1;
		if (id == null)
		{
			try (Database db = Database.connect())
			{
				PreparedStatement stmt = db.preparedStatement("INSERT INTO `dailylog` (`date`, `start`, `end`) VALUES (date(?), datetime(?), datetime(?))");
				stmt.setString(i++, SDF_DATE.format(date));
				stmt.setString(i++, SDF_DATE_TIME.format(start));
				stmt.setString(i++, SDF_DATE_TIME.format(end));
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
				PreparedStatement stmt = db.preparedStatement("UPDATE `dailylog` SET `date` = date(?), `start` = datetime(?), `end` = datetime(?) WHERE `id` = ?");
				stmt.setString(i++, SDF_DATE.format(date));
				stmt.setString(i++, SDF_DATE_TIME.format(start));
				stmt.setString(i++, SDF_DATE_TIME.format(end));
				stmt.setInt(i++, id);
				stmt.executeUpdate();
			}
		}
	}
}
