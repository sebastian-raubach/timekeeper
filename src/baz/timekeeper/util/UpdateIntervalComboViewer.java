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
import org.eclipse.swt.*;
import org.eclipse.swt.widgets.*;

import jhi.swtcommons.gui.viewer.*;
import jhi.swtcommons.util.*;

/**
 * {@link UpdateIntervalComboViewer} extends {@link ComboViewer} and displays {@link jhi.swtcommons.util.Install4jUtils.UpdateInterval}s
 *
 * @author Sebastian Raubach
 */
public class UpdateIntervalComboViewer extends AdvancedComboViewer<Install4jUtils.UpdateInterval>
{
	private boolean                       changed       = false;
	private Install4jUtils.UpdateInterval prevSelection = null;

	public UpdateIntervalComboViewer(Composite parent, int style)
	{
		super(parent, style | SWT.READ_ONLY);

		this.setLabelProvider(new LabelProvider()
		{
			@Override
			public String getText(Object element)
			{
				if (element instanceof Install4jUtils.UpdateInterval)
				{
					return getDisplayText((Install4jUtils.UpdateInterval) element);
				}
				else
				{
					return super.getText(element);
				}
			}
		});

		fill();

        /* Listen for selection changes */
		this.addSelectionChangedListener(e -> {
			Install4jUtils.UpdateInterval selection = getSelectedItem();

			if (!selection.equals(prevSelection))
				changed = true;
		});
	}

	/**
	 * Fill the {@link ComboViewer}
	 */
	private void fill()
	{
		/* Get all the supported update intervals */
		Install4jUtils.UpdateInterval[] items = Install4jUtils.UpdateInterval.values();
		setInput(items);

        /* Select the first element (or the currently stored one) */
		Install4jUtils.UpdateInterval toSelect = TimesheetPropertyReader.updateInterval;

		if (toSelect != null)
			prevSelection = toSelect;
		else
			prevSelection = Install4jUtils.UpdateInterval.STARTUP;
		setSelection(new StructuredSelection(prevSelection));
	}

	/**
	 * Returns <code>true</code> if the user changed the selection, <code>false</code> otherwise
	 *
	 * @return <code>true</code> if the user changed the selection, <code>false</code> otherwise
	 */
	@SuppressWarnings("unused")
	public boolean isChanged()
	{
		return changed;
	}

	@Override
	protected String getDisplayText(Install4jUtils.UpdateInterval item)
	{
		return item.getResource();
	}
}