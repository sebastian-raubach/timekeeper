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

package baz.timesheetinator.dialog;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;

import java.sql.*;
import java.text.*;
import java.util.*;
import java.util.Date;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.*;

import baz.timesheetinator.*;
import baz.timesheetinator.database.*;
import baz.timesheetinator.i18n.*;
import baz.timesheetinator.util.*;
import jhi.swtcommons.util.*;

import static baz.timesheetinator.database.HistoryData.*;

/**
 * @author Sebastian Raubach
 */
public class HistoryDialog extends Dialog
{
	private static final SimpleDateFormat SDF = new SimpleDateFormat("HH:mm:ss");

	private List<Project> projects;

	private List<HistoryDay> data = new ArrayList<>();

	private Color[] colors = Gradient.createMultiGradient(new Color[]{Display.getDefault().getSystemColor(SWT.COLOR_WHITE), Display.getDefault().getSystemColor(SWT.COLOR_BLACK)}, 10);
	private Gradient gradient;

	private ProjectCellEditingSupport.GradientChangeListener listener = () -> data.stream()
																				  .map(HistoryDay::getTotal)
																				  .mapToInt(l -> l).max()
																				  .ifPresent(value -> gradient.setMax(value));

	public HistoryDialog(Shell parentShell) throws SQLException
	{
		super(parentShell);

		this.projects = Project.getAll();

		Project.sortByPosition(projects);

		List<baz.timesheetinator.database.HistoryData> d = baz.timesheetinator.database.HistoryData.getAll();

		Map<Date, Map<Project, baz.timesheetinator.database.HistoryData>> temp = new HashMap<>();

		for (baz.timesheetinator.database.HistoryData i : d)
		{
			if (SDF_DATE.format(i.getDate()).equals(SDF_DATE.format(new Date())))
				continue;

			Map<Project, HistoryData> m = temp.get(i.getDate());

			if (m == null)
				m = new HashMap<>();

			m.put(i.getProject(), i);

			temp.put(i.getDate(), m);
		}

		data.addAll(temp.keySet()
						.stream()
						.map(date -> new HistoryDay(date, temp.get(date)))
						.collect(Collectors.toList()));

		data.stream()
			.map(HistoryDay::getTotal)
			.mapToInt(l -> l)
			.max()
			.ifPresent(value -> gradient = new Gradient(colors, 0, value));

		data.sort(Comparator.comparing(HistoryDay::getDay));
	}

	@Override
	protected void configureShell(Shell newShell)
	{
		super.configureShell(newShell);

		newShell.setText(RB.getString(RB.DIALOG_HISTORY_TITLE));
	}

	@Override
	protected Point getInitialLocation(Point initialSize)
	{
		/* Center the dialog based on the parent */
		return ShellUtils.getLocationCenteredTo(getParentShell(), initialSize);
	}

	@Override
	protected boolean isResizable()
	{
		return true;
	}

	@Override
	protected Control createDialogArea(Composite parent)
	{
		Composite container = (Composite) super.createDialogArea(parent);
		container.setLayout(new FillLayout());

		TableViewer viewer = new TableViewer(container, SWT.FULL_SELECTION | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		viewer.setContentProvider(new ArrayContentProvider());

		TableViewerColumn column = new TableViewerColumn(viewer, SWT.NONE);
		TableColumn c = column.getColumn();
		c.setText(RB.getString(RB.COLUMN_DATE));

		column.setLabelProvider(new CellLabelProvider()
		{
			@Override
			public void update(ViewerCell viewerCell)
			{
				viewerCell.setText(Timesheetinator.Timer.DAY_WEEK.format(((HistoryDay) viewerCell.getElement()).getDay()));
			}
		});

		for (Project project : projects)
		{
			column = new TableViewerColumn(viewer, SWT.NONE);
			c = column.getColumn();
			c.setText(project.getName());

			column.setLabelProvider(new StyledCellLabelProvider()
			{
				@Override
				public void update(ViewerCell viewerCell)
				{
					HistoryDay day = (HistoryDay) viewerCell.getElement();
					long time = day.getTime(project);

					long second = TimeUnit.SECONDS.toSeconds(time) % 60;
					long minute = TimeUnit.SECONDS.toMinutes(time) % 60;
					long hour = TimeUnit.SECONDS.toHours(time) % 24;
					String formatted = String.format("%02d:%02d:%02d", hour, minute, second);

					viewerCell.setBackground(getBackgroundColor(day));
					viewerCell.setForeground(getTextColor(day));
					viewerCell.setText(formatted);
				}

				private Color getTextColor(HistoryDay day)
				{
					long time = day.getTime(project);

					long second = TimeUnit.SECONDS.toSeconds(time) % 60;
					long minute = TimeUnit.SECONDS.toMinutes(time) % 60;
					long hour = TimeUnit.SECONDS.toHours(time) % 24;

					long value = hour * 3600 + minute * 60 + second;

					return gradient.getTextColor(value);
				}

				private Color getBackgroundColor(HistoryDay day)
				{
					long time = day.getTime(project);

					long second = TimeUnit.SECONDS.toSeconds(time) % 60;
					long minute = TimeUnit.SECONDS.toMinutes(time) % 60;
					long hour = TimeUnit.SECONDS.toHours(time) % 24;

					long value = hour * 3600 + minute * 60 + second;

					return gradient.getColor(value);
				}

				@Override
				protected void paint(Event event, Object element)
				{
					HistoryDay day = (HistoryDay) element;

					int x = event.x;
					int y = event.y;

					int width = 1000;
					int height = event.height;

					GC gc = event.gc;
					Color oldBackground = gc.getBackground();

					gc.setBackground(getBackgroundColor(day));
					gc.fillRectangle(x, y, width, height);
					gc.setBackground(oldBackground);

					super.paint(event, element);
				}
			});

			column.setEditingSupport(new ProjectCellEditingSupport(viewer, project, listener));
		}

		column = new TableViewerColumn(viewer, SWT.NONE);
		c = column.getColumn();
		c.setText(RB.getString(RB.COLUMN_START));

		column.setLabelProvider(new CellLabelProvider()
		{
			@Override
			public void update(ViewerCell viewerCell)
			{
				HistoryDay day = (HistoryDay) viewerCell.getElement();

				if (day != null && day.getDailyLog() != null && day.getDailyLog().getStart() != null)
					viewerCell.setText(SDF.format(day.getDailyLog().getStart()));
			}
		});

		column = new TableViewerColumn(viewer, SWT.NONE);
		c = column.getColumn();
		c.setText(RB.getString(RB.COLUMN_END));

		column.setLabelProvider(new CellLabelProvider()
		{
			@Override
			public void update(ViewerCell viewerCell)
			{
				HistoryDay day = (HistoryDay) viewerCell.getElement();

				if (day != null && day.getDailyLog() != null && day.getDailyLog().getEnd() != null)
					viewerCell.setText(SDF.format(day.getDailyLog().getEnd()));
			}
		});

		column = new TableViewerColumn(viewer, SWT.NONE);
		c = column.getColumn();
		c.setText(RB.getString(RB.COLUMN_SUM));

		column.setLabelProvider(new StyledCellLabelProvider()
		{
			@Override
			public void update(ViewerCell viewerCell)
			{
				long time = ((HistoryDay) viewerCell.getElement()).getTotal();

				long second = TimeUnit.SECONDS.toSeconds(time) % 60;
				long minute = TimeUnit.SECONDS.toMinutes(time) % 60;
				long hour = TimeUnit.SECONDS.toHours(time) % 24;
				String formatted = String.format("%02d:%02d:%02d", hour, minute, second);

				long value = hour * 3600 + minute * 60 + second;

				viewerCell.setBackground(gradient.getColor(value));
				viewerCell.setForeground(gradient.getTextColor(value));
				viewerCell.setText(formatted);
			}

			@Override
			protected void paint(Event event, Object element)
			{
				long time = ((HistoryDay) element).getTotal();

				int x = event.x;
				int y = event.y;

				int width = 1000;
				int height = event.height;

				GC gc = event.gc;
				Color oldBackground = gc.getBackground();

				gc.setBackground(gradient.getColor(time));
				gc.fillRectangle(x, y, width, height);
				gc.setBackground(oldBackground);

				super.paint(event, element);
			}
		});

		viewer.setInput(data);

		viewer.getTable().setHeaderVisible(true);
		viewer.getTable().setLinesVisible(true);
		for (TableColumn col : viewer.getTable().getColumns())
		{
			col.pack();
		}

		if (data.size() > 0)
		{
			viewer.reveal(data.get(data.size() - 1));

			// Restrict the size of the dialog to 1/2 height and 1/2 width
			Point preferedSize = container.getShell().computeSize(SWT.DEFAULT, SWT.DEFAULT);

			Monitor monitor = container.getMonitor();
			if (monitor != null && preferedSize.x > monitor.getClientArea().width / 2)
				preferedSize.x = monitor.getClientArea().width / 2;
			if (monitor != null && preferedSize.y > monitor.getClientArea().height / 2)
				preferedSize.y = monitor.getClientArea().height / 2;

			container.getShell().setSize(preferedSize);
		}

		return container;
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent)
	{
	}

	@Override
	public int open()
	{
		if (data.size() < 1)
		{
			MessageDialog.openError(getShell(), RB.getString(RB.DIALOG_HISTORY_ERROR_TITLE), RB.getString(RB.DIALOG_HISTORY_ERROR_MESSAGE));
			this.close();
			return SWT.CANCEL;
		}
		else
		{
			return super.open();
		}
	}

	@Override
	public boolean close()
	{
		if (gradient != null)
			gradient.dispose();

		for (HistoryDay day : data)
		{
			if (day.hasChanged())
			{
				try
				{
					day.saveToDatabase();
				}
				catch (SQLException e)
				{
					e.printStackTrace();
				}
			}
		}

		return super.close();
	}
}
