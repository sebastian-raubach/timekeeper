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

package baz.timekeeper.dialog;

import org.eclipse.swt.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.*;

import java.io.*;
import java.util.*;

import baz.timekeeper.*;
import baz.timekeeper.i18n.*;
import jhi.swtcommons.gui.dialog.*;
import jhi.swtcommons.gui.layout.*;
import jhi.swtcommons.gui.viewer.*;
import jhi.swtcommons.util.*;

/**
 * @author Sebastian Raubach
 */
public class AboutDialog extends BannerDialog
{
	private TabFolder tabFolder;
	private Text      license;

	public AboutDialog(Shell parentShell)
	{
		super(parentShell, Resources.Images.LOGO_SMALL, Resources.Colors.HIGHLIGHT);
	}

	@Override
	protected Control createButtonBar(Composite parent)
	{
		/* We don't want a button bar, so just return null */
		return null;
	}

	@Override
	protected void configureShell(Shell shell)
	{
		super.configureShell(shell);
		shell.setText(RB.getString(RB.DIALOG_TITLE_ABOUT));
	}

	@Override
	protected Point getInitialLocation(Point initialSize)
	{
		/* Center the dialog based on the parent */
		return ShellUtils.getLocationCenteredTo(getParentShell(), initialSize);
	}

	@Override
	public void create()
	{
		super.create();

		if (OSUtils.isMac())
		{
			tabFolder.setSelection(1);
			tabFolder.setSelection(0);
		}
	}

	@Override
	protected void createContent(Composite composite)
	{
		GridLayoutUtils.useDefault().applyTo(composite);

        /* Add the app name */
		Label name = new Label(composite, SWT.WRAP);
		name.setText(RB.getString(RB.APPLICATION_TITLE) + " (" + Install4jUtils.getVersion(Timekeeper.class) + ")");

		Resources.Fonts.applyFontSize(name, 16);

		tabFolder = new TabFolder(composite, SWT.NONE);

		createAboutPart(tabFolder);
		createLicensePart(tabFolder);

		GridLayoutUtils.useDefault().applyTo(tabFolder);
		GridDataUtils.usePredefined(GridDataUtils.GridDataStyle.FILL_BOTH).applyTo(tabFolder);
		GridDataUtils.usePredefined(GridDataUtils.GridDataStyle.FILL_BOTH).applyTo(composite);
		GridDataUtils.usePredefined(GridDataUtils.GridDataStyle.CENTER_TOP).applyTo(name);
	}

	private void createAboutPart(TabFolder parent)
	{
		TabItem item = new TabItem(parent, SWT.NONE);
		item.setText(RB.getString(RB.DIALOG_ABOUT_TAB_ABOUT));

		Composite composite = new Composite(parent, SWT.NONE);

		/* Add some space */
		new Label(composite, SWT.NONE);

		 /* Add the description */
		Label desc = new Label(composite, SWT.WRAP);
		desc.setText(RB.getString(RB.DIALOG_ABOUT_MESSAGE));

        /* Add some space */
		new Label(composite, SWT.NONE);

        /* Add the copyright information */
		Label copyright = new Label(composite, SWT.WRAP);
		copyright.setText(RB.getString(RB.DIALOG_ABOUT_COPYRIGHT, Integer.toString(Calendar.getInstance().get(Calendar.YEAR))));

		Composite links = new Composite(composite, SWT.NONE);
		GridLayoutUtils.useValues(2, true).applyTo(links);

		Label github = new Label(links, SWT.NONE);
		github.setCursor(composite.getDisplay().getSystemCursor(SWT.CURSOR_HAND));
		github.setToolTipText(RB.getString(RB.URL_GITHUB));
		github.setImage(Resources.Images.GITHUB);
		github.addListener(SWT.MouseUp, e -> OSUtils.open(RB.getString(RB.URL_GITHUB)));

		Label email = new Label(links, SWT.NONE);
		email.setCursor(composite.getDisplay().getSystemCursor(SWT.CURSOR_HAND));
		email.setToolTipText(RB.getString(RB.URL_EMAIL));
		email.setImage(Resources.Images.EMAIL);
		email.addListener(SWT.MouseUp, e -> OSUtils.open("mailto:" + RB.getString(RB.URL_EMAIL)));

		/* Do some layout magic here */
		GridLayoutUtils.useDefault().applyTo(composite);
		GridDataUtils.usePredefined(GridDataUtils.GridDataStyle.FILL_BOTH).applyTo(composite);
		GridDataUtils.usePredefined(GridDataUtils.GridDataStyle.CENTER_TOP_FALSE).applyTo(links);

		item.setControl(composite);
	}

	/**
	 * Creates the {@link TabFolder} containing the license
	 *
	 * @param parent The parent {@link TabFolder}
	 */
	private void createLicensePart(TabFolder parent)
	{
		TabItem item = new TabItem(parent, SWT.NONE);
		item.setText(RB.getString(RB.DIALOG_ABOUT_TAB_LICENSE));

		Composite composite = new Composite(parent, SWT.NONE);

		LicenseFileComboViewer licenseFileComboViewer = new LicenseFileComboViewer(composite, SWT.NONE)
		{
			@Override
			protected LinkedHashMap<String, String> getLicenseData()
			{
				LinkedHashMap<String, String> result = new LinkedHashMap<>();

				result.put(RB.getString(RB.APPLICATION_TITLE), "LICENSE");
				result.put("Material Design Icons", "licenses/materialdesignicons.txt");

				return result;
			}

			@Override
			protected void showLicense(String text, String path)
			{
				try
				{
					license.setText(readLicense(path));
				}
				catch (Exception e)
				{
					license.setText(RB.getString(RB.ERROR_ABOUT_LICENSE));
					e.printStackTrace();
				}
			}
		};

		license = new Text(composite, SWT.WRAP | SWT.BORDER | SWT.V_SCROLL | SWT.READ_ONLY);
		license.setBackground(getShell().getDisplay().getSystemColor(SWT.COLOR_WHITE));

		licenseFileComboViewer.init();

        /* Do some layout magic here */
		GridLayoutUtils.useDefault().applyTo(composite);

		GridDataUtils.usePredefined(GridDataUtils.GridDataStyle.FILL_BOTH).applyTo(composite);
		GridDataUtils.usePredefined(GridDataUtils.GridDataStyle.FILL_TOP).applyTo(licenseFileComboViewer.getCombo());
		GridDataUtils.usePredefined(GridDataUtils.GridDataStyle.FILL_BOTH).heightHint(1).applyTo(license);

		item.setControl(composite);
	}

	private String readLicense(String path) throws IOException
	{
		StringBuilder builder = new StringBuilder();
		BufferedReader br;
		if (Timekeeper.WITHIN_JAR)
			br = new BufferedReader(new InputStreamReader(Timekeeper.class.getResourceAsStream("/" + path), "UTF-8"));
		else
			br = new BufferedReader(new FileReader(path));

		String line;

		while ((line = br.readLine()) != null)
		{
			builder.append(line).append("\n");
		}

		br.close();

		return builder.toString();
	}
}
