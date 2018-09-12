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

package baz.timekeeper.util;

import org.eclipse.jface.viewers.*;
import org.eclipse.swt.widgets.*;

import java.util.*;
import java.util.concurrent.*;

import baz.timekeeper.*;
import baz.timekeeper.database.*;
import jhi.swtcommons.util.*;

public class ProjectCellEditingSupport extends EditingSupport
{
	private final Project                project;
	private final GradientChangeListener listener;

	public ProjectCellEditingSupport(TableViewer viewer, Project project, GradientChangeListener listener)
	{
		super(viewer);
		this.project = project;
		this.listener = listener;
	}

	@Override
	protected CellEditor getCellEditor(Object element)
	{
		return new TextCellEditor((Composite) getViewer().getControl());
	}

	@Override
	protected boolean canEdit(Object element)
	{
		return true;
	}

	@Override
	protected Object getValue(Object element)
	{
		long time = ((HistoryDay) element).getTime(project);

		long second = TimeUnit.SECONDS.toSeconds(time) % 60;
		long minute = TimeUnit.SECONDS.toMinutes(time) % 60;
		long hour = TimeUnit.SECONDS.toHours(time) % 24;
		return String.format("%02d:%02d:%02d", hour, minute, second);
	}

	@Override
	protected void setValue(Object element, Object userInputValue)
	{
		try
		{
			int value = 0;
			Calendar cal = Calendar.getInstance();

			if (userInputValue != null && !StringUtils.isEmpty(String.valueOf(userInputValue)))
			{
				Date date = Timekeeper.Timer.TIME.parse(String.valueOf(userInputValue));
				cal.setTime(date);
				value = cal.get(Calendar.SECOND);
				value += cal.get(Calendar.MINUTE) * 60;
				value += cal.get(Calendar.HOUR_OF_DAY) * 3600;
				value *= 1000;
			}

			((HistoryDay) element).setTime(project, value);
//			getViewer().update(element, null);
			listener.onGradientChange();
			getViewer().refresh(true);
		}
		catch (Exception e)
		{
		}
	}

	public interface GradientChangeListener
	{
		void onGradientChange();
	}
} 