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

package baz.timesheetinator.util;

import java.sql.*;
import java.util.*;
import java.util.Date;

import baz.timesheetinator.database.*;


/**
 * @author Sebastian Raubach
 */
public class HistoryDay
{
	private Date                      day;
	private Map<Project, HistoryData> data;
	private Map<HistoryData, Integer> originalTimes = new HashMap<>();
	private Map<HistoryData, Integer> currentTimes  = new HashMap<>();
	private DailyLog dailyLog;

	private int maxTime;
	private int total;

	public HistoryDay(Date day, Map<Project, HistoryData> data)
	{
		this.day = day;
		this.data = data;

		try
		{
			this.dailyLog = DailyLog.getForDay(day);
		}
		catch (SQLException e)
		{
			this.dailyLog = null;
		}

		for (HistoryData d : data.values())
		{
			originalTimes.put(d, d.getTime());
			currentTimes.put(d, d.getTime());
		}

		maxTime = Collections.max(originalTimes.values());

		total = originalTimes.values().stream()
							 .mapToInt(l -> l)
							 .sum();
	}

	public Date getDay()
	{
		return day;
	}

	public long getTime(Project project)
	{
		if (data.containsKey(project))
			return data.get(project).getTime();
		else
			return 0;
	}

	public void setTime(Project project, int time)
	{
		time /= 1000;
		if (data.containsKey(project))
		{
			data.get(project).setTime(time);
			currentTimes.put(data.get(project), time);
		}
		else
		{
			HistoryData d = new HistoryData(null, project, day, time);
			data.put(project, d);
			originalTimes.put(d, 0);
			currentTimes.put(d, 0);
		}

		maxTime = Collections.max(currentTimes.values());

		total = currentTimes.values().stream()
							.mapToInt(l -> l)
							.sum();
	}

	public boolean hasChanged()
	{
		for (HistoryData d : data.values())
		{
			if (d.getTime() != originalTimes.get(d))
				return true;
		}

		return false;
	}

	public DailyLog getDailyLog()
	{
		return dailyLog;
	}

	public int getTotal()
	{
		return total;
	}

	public void saveToDatabase() throws SQLException
	{
		for (HistoryData d : data.values())
			d.write();
	}

	public long getMaxTime()
	{
		return maxTime;
	}
}
