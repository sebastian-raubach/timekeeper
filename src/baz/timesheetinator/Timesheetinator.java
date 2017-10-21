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

package baz.timesheetinator;

import org.eclipse.jface.window.*;
import org.eclipse.swt.*;
import org.eclipse.swt.custom.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;

import java.io.*;
import java.sql.Date;
import java.sql.*;
import java.text.*;
import java.util.*;
import java.util.List;
import java.util.stream.*;

import baz.timesheetinator.database.*;
import baz.timesheetinator.dialog.*;
import baz.timesheetinator.i18n.*;
import baz.timesheetinator.util.*;
import jhi.swtcommons.gui.*;
import jhi.swtcommons.util.*;

/**
 * @author Sebastian Raubach
 */
public class Timesheetinator extends RestartableApplication
{
	private static final String APP_ID         = "2414-6232-2575-1498";
	private static final String UPDATE_ID      = "314";
	private static final String VERSION_NUMBER = "x.xx.xx.xx";
	private static final String UPDATER_URL    = "https://github.com/sebastian-raubach/timesheetinator/blob/master/installers/updates.xml";

	public static final  boolean READ_ONLY_MODE = false;
	private static final int     WRITE_INTERVAL = 300000;
	/** Indicates whether the application is run form a jar or not */
	public static boolean WITHIN_JAR;
	private static Map<Timer, HistoryData> timers         = new HashMap<>();
	private static TimesheetPropertyReader propertyReader = new TimesheetPropertyReader();
	private static ScrolledComposite scroll;

	private static DailyLog today = null;

	public Timesheetinator(Integer integer)
	{
		super(false, integer);
	}

	public static void main(String[] args)
	{
		/* Check if we are running from within a jar or the IDE */
		WITHIN_JAR = !Timesheetinator.class.getResource(Timesheetinator.class.getSimpleName() + ".class").toString().startsWith("file");

		new Timesheetinator(SWT.CLOSE | SWT.MIN | SWT.TITLE | SWT.ON_TOP | SWT.RESIZE);
	}

	private void loadProperties(Shell shell)
	{
		try
		{
			getPropertyReader().load();
			loadContent(shell);
		}
		catch (IOException | SQLException e)
		{
			e.printStackTrace();
		}
	}

	private void loadContent(Shell shell) throws SQLException
	{
		/* Periodically save the values every 5 minutes */
		display.timerExec(WRITE_INTERVAL, new Runnable()
		{
			@Override
			public void run()
			{
				if (display != null && !display.isDisposed())
				{
					display.asyncExec(() -> writeAll());
					display.timerExec(WRITE_INTERVAL, this);
				}
			}
		});

		scroll = new ScrolledComposite(shell, SWT.H_SCROLL);
		scroll.setLayout(new GridLayout(1, false));
		scroll.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		scroll.setExpandVertical(true);
		scroll.setExpandHorizontal(true);

		addData();

		addMenuBar(shell);

		shell.pack(true);
	}

	private void writeAll()
	{
		try
		{
			if (today != null)
			{
				today.setEnd(new Date(System.currentTimeMillis()));
				today.write();
			}
		}
		catch (SQLException e1)
		{
			e1.printStackTrace();
		}

		for (Map.Entry<Timer, HistoryData> entry : timers.entrySet())
		{
			entry.getValue().setTime(entry.getKey().getInt());
			try
			{
				entry.getValue().write();
			}
			catch (SQLException e1)
			{
				e1.printStackTrace();
			}
		}
	}

	private void addData() throws SQLException
	{
		DailyLog oldToday = DailyLog.getForToday();

		if (oldToday != null)
			today = oldToday;
		else if (today == null)
			today = new DailyLog(null, new Date(System.currentTimeMillis()), new Date(System.currentTimeMillis()), new Date(System.currentTimeMillis()));

		/* Stop all existing timers */
		writeAll();
		for (Map.Entry<Timer, HistoryData> entry : timers.entrySet())
		{
			entry.getKey().stop();
			try
			{
				entry.getValue().write();
			}
			catch (SQLException e1)
			{
				e1.printStackTrace();
			}
		}

		/* Clear memory */
		timers.clear();

		for (Control control : scroll.getChildren())
		{
			control.dispose();
		}

		List<Project> projects = Project.getAll();

		projects = projects.stream()
						   .filter(Project::isVisibility)
						   .collect(Collectors.toList());

		Project.sortByPosition(projects);

		if (CollectionUtils.isEmpty(projects))
		{
			openSettingsDialog();
		}
		else
		{
			Composite content = new Composite(scroll, SWT.NONE);
			content.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

			content.setLayout(new GridLayout(projects.size(), true));
			scroll.setContent(content);

			Map<Project, HistoryData> today = HistoryData.getAllForToday();

			boolean hasAutoStarted = false;
			Button button = null;

			/* For each project, add a group */
			for (Project project : projects)
			{
				/* Define the group */
				Group group = new Group(content, SWT.NONE);
				group.setLayout(new GridLayout(1, false));
				group.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
				group.setText(project.getName());

				/* Create the timer */
				Timer timer = new Timer(group, SWT.TIME);
				timer.getWidget().setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
				HistoryData data = new HistoryData(null, project, new Date(System.currentTimeMillis()), 0);

				/* Add the start button */
				button = new Button(group, SWT.PUSH);
				button.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
				button.setText(RB.getString(RB.BUTTON_START));
				button.addListener(SWT.Selection, timer);

				if (today != null && today.containsKey(project))
				{
					timer.calendar.add(Calendar.SECOND, today.get(project).getTime());
					data = today.get(project);
					timer.update();
				}

				/* Add the timer to the list */
				timers.put(timer, data);

				if (project.isAutostart() && !hasAutoStarted)
				{
					timer.start();
					/* We only allow one of them to auto-start */
					hasAutoStarted = true;
					button.forceFocus();
				}
			}

			if (!hasAutoStarted && button != null)
				button.forceFocus();

			scroll.setMinSize(content.computeSize(SWT.DEFAULT, SWT.DEFAULT));

			scroll.layout(true, true);
		}
	}

	private void addMenuBar(Shell shell)
	{
		Menu oldMenu = shell.getMenuBar();
		if (oldMenu != null && !oldMenu.isDisposed())
			oldMenu.dispose();

		Menu menuBar = new Menu(shell, SWT.BAR);
		Menu fileMenu = new Menu(menuBar);
		Menu aboutMenu = new Menu(menuBar);

        /* File */
		MenuItem item = new MenuItem(menuBar, SWT.CASCADE);
		item.setText(RB.getString(RB.MENU_FILE));
		item.setMenu(fileMenu);

		item = new MenuItem(fileMenu, SWT.NONE);
		item.setText(RB.getString(RB.MENU_FILE_STOP_ALL));
		item.addListener(SWT.Selection, e -> timers.keySet().forEach(Timer::stop));

		item = new MenuItem(fileMenu, SWT.NONE);
		item.setText(RB.getString(RB.MENU_FILE_HISTORY));
		item.addListener(SWT.Selection, e ->
		{
			try
			{
				new HistoryDialog(shell).open();
			}
			catch (SQLException e1)
			{
				e1.printStackTrace();
			}
		});

		item = new MenuItem(fileMenu, SWT.NONE);
		item.setText(RB.getString(RB.MENU_FILE_SETTINGS));
		item.addListener(SWT.Selection, e -> openSettingsDialog());

		/* Help */
		item = new MenuItem(menuBar, SWT.CASCADE);
		item.setText(RB.getString(RB.MENU_HELP));
		item.setMenu(aboutMenu);

		item = new MenuItem(aboutMenu, SWT.NONE);
		item.setText(RB.getString(RB.MENU_HELP_ONLINE_HELP));
		item.addListener(SWT.Selection, e -> OSUtils.open(RB.getString(RB.URL_GITHUB_WIKI)));

		/* Help - Check for updates */
		item = new MenuItem(aboutMenu, SWT.NONE);
		item.setText(RB.getString(RB.MENU_HELP_UPDATE));
		item.addListener(SWT.Selection, e -> checkForUpdate(false));
		item.setEnabled(WITHIN_JAR);

		/* Help - About */
		addAboutMenuItemListener(RB.getString(RB.MENU_HELP_ABOUT), aboutMenu, e -> new AboutDialog(shell).open());

		shell.setMenuBar(menuBar);
	}

	private void openSettingsDialog()
	{
		SettingsDialog dialog = new SettingsDialog(shell);

		if (dialog.open() == Window.OK)
		{
			try
			{
				getPropertyReader().store();
				shell.setAlpha(TimesheetPropertyReader.opacity);
			}
			catch (IOException e1)
			{
				e1.printStackTrace();
			}

				/* Update the main view */
			try
			{
				addData();
			}
			catch (SQLException e1)
			{
				e1.printStackTrace();
			}
		}
		else
		{
			try
			{
				if (CollectionUtils.isEmpty(Project.getAll()))
				{
					System.exit(0);
				}
			}
			catch (SQLException e)
			{
				System.exit(0);
			}
		}
	}

	@Override
	protected PropertyReader getPropertyReader()
	{
		return propertyReader;
	}

	@Override
	protected void onStart()
	{
		loadProperties(shell);
		shell.setText(RB.getString(RB.APPLICATION_TITLE));
		shell.setLayout(new GridLayout(1, false));
		shell.setAlpha(TimesheetPropertyReader.opacity);
		shell.setImage(Resources.Images.LOGO);
	}

	@Override
	protected void onExit()
	{
		writeAll();
	}

	@Override
	protected void initResources()
	{

	}

	@Override
	protected void disposeResources()
	{
		Resources.disposeResources();
	}

	/**
	 * Timer is a simple wrapper for a {@link DateTime} object. It remembers the "running state" of the timer and keeps track of the time that has
	 * already passed while this timer was running
	 */
	public static class Timer implements Listener
	{
		public static final SimpleDateFormat DAY      = new SimpleDateFormat("yyyy-MM-dd");
		public static final SimpleDateFormat DAY_WEEK = new SimpleDateFormat("yyyy-MM-dd EEE");
		public static final SimpleDateFormat TIME     = new SimpleDateFormat("HH:mm:ss");
		public static final SimpleDateFormat DAY_TIME = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

		/** Remember the state */
		private boolean running = false;
		/** The wrapped {@link DateTime} object */
		private DateTime time;
		/** The {@link Calendar} storing the current time count */
		private Calendar calendar = Calendar.getInstance();

		/* Define a runnable that updates the DateTime and Shell title */
		private Runnable runnable = new Runnable()
		{
			@Override
			public void run()
			{
				if (running && !time.isDisposed())
				{
					/* Execute this runnable again after one second */
					Display.getDefault().timerExec(1000, this);

					/* Add a second to the calendar */
					calendar.add(Calendar.SECOND, 1);

					/* Set all the fields of the DateTime, as there's no "add" method */
					update();

					/* Get the total of all timers */
					long total = timers.keySet()
									   .stream()
									   .mapToLong(t -> t.calendar.getTimeInMillis())
									   .sum();

					/* Set the shell title */
					time.getShell().setText(RB.getString(RB.APPLICATION_TITLE) + " [" + TIME.format(total) + "]");
				}
			}
		};

		Timer(Composite composite, int style)
		{
			time = new DateTime(composite, style);
			time.setHours(0);
			time.setMinutes(0);
			time.setSeconds(0);

			/* Initially set the calendar to 2000-01-01 00:00:00 */
			try
			{
				calendar.setTimeInMillis(DAY_TIME.parse("2000-01-01 00:00:00").getTime());
			}
			catch (ParseException e)
			{
				e.printStackTrace();
			}

			time.addListener(SWT.Selection, (e) ->
			{
				calendar.set(Calendar.HOUR_OF_DAY, time.getHours());
				calendar.set(Calendar.MINUTE, time.getMinutes());
				calendar.set(Calendar.SECOND, time.getSeconds());
			});
		}

		void update()
		{
			time.setHours(calendar.get(Calendar.HOUR_OF_DAY));
			time.setMinutes(calendar.get(Calendar.MINUTE));
			time.setSeconds(calendar.get(Calendar.SECOND));
		}

		int getInt()
		{
			return calendar.get(Calendar.SECOND) + calendar.get(Calendar.MINUTE) * 60 + calendar.get(Calendar.HOUR_OF_DAY) * 3600;
		}

		long getLong()
		{
			return calendar.get(Calendar.SECOND) + calendar.get(Calendar.MINUTE) * 60 + calendar.get(Calendar.HOUR_OF_DAY) * 3600;
		}

		DateTime getWidget()
		{
			return time;
		}

		void start()
		{
			if (!running)
			{
				running = true;
				Display.getDefault().timerExec(1000, runnable);

				/* Stop all other timers */
				timers.keySet()
					  .stream()
					  .filter(timer -> !Objects.equals(timer, this) && timer.running)
					  .forEach(Timesheetinator.Timer::stop);
			}
		}

		void stop()
		{
			running = false;
		}

		@Override
		public void handleEvent(Event event)
		{
			/* On button click, toggle state */
			if (!running)
				start();
		}
	}

	@Override
	protected void onPreStart()
	{
		checkForUpdate(true);
	}

	private void checkForUpdate(boolean startupCall)
	{
		if (WITHIN_JAR)
		{
			/* Check if an update is available */
			Install4jUtils i4j = new Install4jUtils(APP_ID, UPDATE_ID);

			Install4jUtils.UpdateInterval interval = startupCall ? TimesheetPropertyReader.updateInterval : Install4jUtils.UpdateInterval.STARTUP;

			i4j.setDefaultVersionNumber(VERSION_NUMBER);
			i4j.setUser(interval, "", 0);
			i4j.setURLs(UPDATER_URL, "");
			if (!startupCall)
			{
				i4j.setCallback(updateAvailable -> {
					if (!updateAvailable)
						DialogUtils.showInformation(RB.getString(RB.INFORMATION_NO_UPDATE_AVAILABLE));
				});
			}

			i4j.doStartUpCheck(Timesheetinator.class);
		}
	}
}
