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
import org.eclipse.jface.layout.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.*;
import org.eclipse.swt.custom.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;

import java.sql.*;
import java.util.*;
import java.util.List;

import baz.timesheetinator.database.*;
import baz.timesheetinator.i18n.*;
import baz.timesheetinator.util.*;
import jhi.swtcommons.gui.layout.*;
import jhi.swtcommons.util.*;

/**
 * @author Sebastian Raubach
 */
public class SettingsDialog extends Dialog
{
	private Shell               parentShell;
	private Scale               scale;
	private Text                label;
	private List<Project>       activeProjects;
	private CheckboxTableViewer viewer;

	//make sure you dispose these buttons when viewer input changes
	private Map<Object, Button> tableButtons = new HashMap<>();

	private Button okButton;

	public SettingsDialog(Shell parentShell)
	{
		super(parentShell);

		this.parentShell = parentShell;
	}

	@Override
	protected void configureShell(Shell newShell)
	{
		super.configureShell(newShell);

		newShell.setMinimumSize(300, 400);
		newShell.setText(RB.getString(RB.DIALOG_SETTINGS_TITLE));
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
		container.setLayout(new GridLayout(1, false));
		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		createOpacityGroup(container);
		try
		{
			createProjectGroup(container);
		}
		catch (SQLException e)
		{
			e.printStackTrace();
		}

		return container;
	}

	private void createProjectGroup(Composite container) throws SQLException
	{
		Group group = new Group(container, SWT.NONE);
		group.setText(RB.getString(RB.DIALOG_SETTINGS_PROJECTS));
		group.setLayout(new GridLayout(3, false));

		Composite comp = new Composite(group, SWT.NONE);
		GridDataUtils.usePredefined(GridDataUtils.GridDataStyle.FILL_BOTH).applyTo(comp);

		Table table = new Table(comp, SWT.SINGLE | SWT.CHECK | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.BORDER);
		table.setHeaderVisible(true);
		viewer = new CheckboxTableViewer(table)
		{
			private void clearButtons()
			{
				tableButtons.values()
							.forEach(Button::dispose);
				tableButtons.clear();
			}

			@Override
			public void refresh()
			{
				clearButtons();
				super.refresh();
			}

			@Override
			public void refresh(boolean updateLabels)
			{
				clearButtons();
				super.refresh(updateLabels);
			}

			@Override
			protected void unmapAllElements()
			{
				clearButtons();
				super.unmapAllElements();
			}
		};

		activeProjects = Project.getAll();

		Project.sortByPosition(activeProjects);

		TableViewerColumn nameColumn = new TableViewerColumn(viewer, SWT.NONE);
		nameColumn.getColumn().setText(RB.getString(RB.COLUMN_PROJECT_NAME));
		nameColumn.setLabelProvider(new ColumnLabelProvider()
		{
			@Override
			public String getText(Object element)
			{
				Project p = (Project) element;
				return p.getName();
			}
		});

		TableColumnLayout tableLayout = new TableColumnLayout();
		comp.setLayout(tableLayout);

		ProjectNameEditingSupport editingSupport = new ProjectNameEditingSupport(viewer);
		nameColumn.setEditingSupport(editingSupport);

		TableViewerColumn visibleColumn = new TableViewerColumn(viewer, SWT.NONE);
		visibleColumn.getColumn().setText(RB.getString(RB.COLUMN_PROJECT_ACTIVE));
		visibleColumn.setLabelProvider(new ColumnLabelProvider()
		{
			@Override
			public void update(ViewerCell cell)
			{
				final Project project = (Project) cell.getElement();

				TableItem item = (TableItem) cell.getItem();
				Button button;
				if (tableButtons.containsKey(cell.getElement()))
				{
					button = tableButtons.get(cell.getElement());
				}
				else
				{
					button = new Button((Composite) cell.getViewerRow().getControl(), SWT.CHECK);
					tableButtons.put(cell.getElement(), button);

					button.addListener(SWT.Selection, e -> project.setVisibility(!project.isVisibility()));
				}
				TableEditor editor = new TableEditor(item.getParent());
				editor.grabHorizontal = true;
				editor.grabVertical = true;
				editor.setEditor(button, item, cell.getColumnIndex());
				editor.layout();

				button.setSelection(project.isVisibility());
			}
		});

		viewer.addCheckStateListener(e ->
		{
			for (Project p : activeProjects)
				p.setAutostart(false);

			((Project) e.getElement()).setAutostart(true);

			viewer.setCheckedElements(new Object[]{e.getElement()});
		});
		viewer.setContentProvider(ArrayContentProvider.getInstance());
		viewer.setInput(activeProjects);

		for (TableColumn c : viewer.getTable().getColumns())
			c.pack();

		tableLayout.setColumnData(nameColumn.getColumn(), new ColumnWeightData(3));
		tableLayout.setColumnData(visibleColumn.getColumn(), new ColumnWeightData(1));

		/* Set the selection */
		activeProjects.stream()
					  .filter(Project::isAutostart)
					  .forEach(p -> viewer.setCheckedElements(new Object[]{p}));

		Composite orderButtons = new Composite(group, SWT.NONE);
		orderButtons.setLayout(new FillLayout(SWT.VERTICAL));
		orderButtons.setLayoutData(new GridData(SWT.END, SWT.TOP, false, false));

		Button up = new Button(orderButtons, SWT.ARROW | SWT.UP);
		Button down = new Button(orderButtons, SWT.ARROW | SWT.DOWN);

		up.addListener(SWT.Selection, e -> moveItem(table.getSelectionIndex(), -1));

		down.addListener(SWT.Selection, e -> moveItem(table.getSelectionIndex(), 1));

		Button add = new Button(group, SWT.PUSH);
		add.setText(RB.getString(RB.BUTTON_ADD));
		Button del = new Button(group, SWT.PUSH);
		del.setEnabled(false);
		del.setText(RB.getString(RB.BUTTON_DELETE));

		add.addListener(SWT.Selection, e ->
		{
			int i = 0;

			for (Project p : activeProjects)
				i = Math.max(i, Math.abs(p.getId()));

			int id = i + 1;
			Project project = new Project(-id, RB.getString(RB.PROJECT_TEMPLATE, id), false, true, Project.getMaxPosition());
			activeProjects.add(project);
			viewer.refresh();
			okButton.setEnabled(true);
			del.setEnabled(true);

			viewer.getTable().setFocus();
			viewer.setSelection(new StructuredSelection(activeProjects.get(activeProjects.size() - 1)), true);
			viewer.editElement(project, 0);
		});

		del.addListener(SWT.Selection, e ->
		{
			IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();

			if (selection != null && selection.size() > 0)
			{
				Project p = (Project) selection.getFirstElement();
				activeProjects.remove(p);
				viewer.refresh();
			}

			if (((IStructuredSelection) viewer.getSelection()).size() < 1)
			{
				okButton.setEnabled(false);
				del.setEnabled(false);
			}
		});

		GridData d = new GridData(SWT.FILL, SWT.FILL, true, true);
		d.horizontalSpan = 2;
		comp.setLayoutData(d);
		group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		add.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		del.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
	}

	private void moveItem(int currentPosition, int direction)
	{
		if (currentPosition == 0 && direction == -1)
			return;
		if (currentPosition == activeProjects.size() - 1 && direction == 1)
			return;

		if (currentPosition >= 0)
		{
			Project current = activeProjects.get(currentPosition);
			activeProjects.remove(current);
			activeProjects.add(currentPosition + direction, current);
			viewer.refresh(true);
		}
	}

	private void createOpacityGroup(Composite container)
	{
		Group opacityGroup = new Group(container, SWT.NONE);
		opacityGroup.setText(RB.getString(RB.DIALOG_SETTINGS_OPACITY));
		opacityGroup.setLayout(new GridLayout(2, false));
		scale = new Scale(opacityGroup, SWT.HORIZONTAL);
		label = new Text(opacityGroup, SWT.READ_ONLY);

		scale.setPageIncrement(10);
		scale.setIncrement(10);
		scale.setMinimum(20);
		scale.setMaximum(255);
		scale.setSelection(TimesheetPropertyReader.opacity);

		label.setText(Integer.toString(scale.getSelection()));

		scale.addListener(SWT.Selection, (e) ->
		{
			if (parentShell != null)
				parentShell.setAlpha(scale.getSelection());
			label.setText(Integer.toString(scale.getSelection()));
			label.getParent().layout(true, true);
		});

		scale.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		label.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false));
		opacityGroup.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent)
	{
		okButton = createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
		okButton.setEnabled(false);
	}

	@Override
	protected void okPressed()
	{
		TimesheetPropertyReader.opacity = scale.getSelection();

		try
		{
			List<Project> databaseProjects = Project.getAll();

			for (Project p : activeProjects)
			{
				p.setPosition(activeProjects.indexOf(p));
				p.write();
			}

			for (Project p : databaseProjects)
			{
				if (!activeProjects.contains(p))
				{
					HistoryData.removeForProject(p);

					p.remove();
				}
			}
		}
		catch (SQLException e)
		{
			e.printStackTrace();
		}

		super.okPressed();
	}


	public class ProjectNameEditingSupport extends EditingSupport
	{

		private final TableViewer viewer;
		private final CellEditor  editor;

		public ProjectNameEditingSupport(TableViewer viewer)
		{
			super(viewer);
			this.viewer = viewer;
			this.editor = new TextCellEditor(viewer.getTable());
		}

		@Override
		protected CellEditor getCellEditor(Object element)
		{
			return editor;
		}

		@Override
		protected boolean canEdit(Object element)
		{
			return true;
		}

		@Override
		protected Object getValue(Object element)
		{
			return ((Project) element).getName();
		}

		@Override
		protected void setValue(Object element, Object userInputValue)
		{
			((Project) element).setName(String.valueOf(userInputValue));
			viewer.update(element, null);
		}
	}
}
