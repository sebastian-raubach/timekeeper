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

import java.io.*;

import baz.timesheetinator.*;
import jhi.swtcommons.util.*;

/**
 * @author Sebastian Raubach
 */
public class TimesheetPropertyReader extends PropertyReader
{
	public static final  String                        PROPERTIES_FOLDER  = "baz";
	private static final String                        PREFERENCE_OPACITY = "preferences.opacity";
	private static final String                        PREFERENCE_UPDATE  = "preference.update.interval";
	private static final String                        PROPERTIES_FILE    = "/timesheetinator.properties";
	public static        int                           opacity            = 255;
	public static        Install4jUtils.UpdateInterval updateInterval     = Install4jUtils.UpdateInterval.STARTUP;
	private static File localFile;

	public TimesheetPropertyReader()
	{
		super(PROPERTIES_FILE);
	}

	@Override
	public void load() throws IOException
	{
		localFile = new File(new File(System.getProperty("user.home"), "." + PROPERTIES_FOLDER), PROPERTIES_FILE);

		InputStream stream = null;

		try
		{
			if (localFile.exists())
			{
				stream = new FileInputStream(localFile);
			}
			else
			{
				if (Timesheetinator.WITHIN_JAR)
					stream = PropertyReader.class.getResourceAsStream(PROPERTIES_FILE);
				else
					stream = new FileInputStream(new File("res", PROPERTIES_FILE));
			}

			properties.load(stream);
		}
		finally
		{
			if (stream != null)
			{
				try
				{
					stream.close();
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}
		}

		opacity = getPropertyInteger(PREFERENCE_OPACITY, 255);
		try
		{
			updateInterval = Install4jUtils.UpdateInterval.valueOf(getProperty(PREFERENCE_UPDATE));
		}
		catch (Exception e)
		{
			updateInterval = Install4jUtils.UpdateInterval.STARTUP;
		}
	}

	@Override
	public void store() throws IOException
	{
		if (localFile == null)
			return;

		set(PREFERENCE_OPACITY, Integer.toString(opacity));
		set(PREFERENCE_UPDATE, updateInterval.name());

		localFile.getParentFile().mkdirs();
		localFile.createNewFile();
		properties.store(new FileOutputStream(localFile), null);
	}
}
