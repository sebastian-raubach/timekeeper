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

package baz.timesheetinator.i18n;

import java.util.*;

/**
 * {@link RB} wraps the {@link ResourceBundle} of this application. Use {@link #getString(String, Object...)} and the constants of this class to
 * access the resources.
 *
 * @author Sebastian Raubach
 */
public class RB extends jhi.swtcommons.gui.i18n.RB
{
	public static final String APPLICATION_TITLE           = "application.title";
	public static final String APPLICATION_TITLE_NO_SPACES = "application.title.no.spaces";
	public static final String APPLICATION_VERSION         = "application.version";

	public static final String MENU_FILE              = "menu.file";
	public static final String MENU_FILE_SETTINGS     = "menu.file.settings";
	public static final String MENU_FILE_STOP_ALL     = "menu.file.close.all";
	public static final String MENU_FILE_HISTORY      = "menu.file.history";
	public static final String MENU_HELP              = "menu.help";
	public static final String MENU_HELP_ABOUT        = "menu.help.about";
	public static final String MENU_HELP_ONLINE_HELP  = "menu.help.online.help";
	public static final String BUTTON_START           = "button.start";
	public static final String BUTTON_ADD             = "button.add";
	public static final String BUTTON_DELETE          = "button.delete";
	public static final String DIALOG_TITLE_ABOUT     = "dialog.about.title";
	public static final String DIALOG_ABOUT_MESSAGE   = "dialog.about.message";
	public static final String DIALOG_ABOUT_COPYRIGHT = "dialog.about.copyright";
	public static final String DIALOG_ABOUT_TAB_ABOUT = "dialog.about.tab.about";
	public static final String DIALOG_ABOUT_TAB_LICENSE     = "dialog.about.tab.license";
	public static final String DIALOG_HISTORY_TITLE         = "dialog.history.title";
	public static final String DIALOG_HISTORY_ERROR_TITLE   = "dialog.history.error.title";
	public static final String DIALOG_HISTORY_ERROR_MESSAGE = "dialog.history.error.message";
	public static final String DIALOG_SETTINGS_TITLE        = "dialog.settings.title";
	public static final String DIALOG_SETTINGS_OPACITY      = "dialog.settings.opacity";
	public static final String DIALOG_SETTINGS_PROJECTS     = "dialog.settings.projects";
	public static final String COLUMN_DATE                  = "column.date";
	public static final String COLUMN_START                 = "column.start";
	public static final String COLUMN_END             = "column.end";
	public static final String COLUMN_SUM             = "column.sum";
	public static final String COLUMN_PROJECT_NAME    = "column.project.name";
	public static final String COLUMN_PROJECT_ACTIVE  = "column.project.active";
	public static final String PROJECT_TEMPLATE       = "project.template";
	public static final String URL_GITHUB             = "url.github";
	public static final String URL_EMAIL              = "url.email";
	public static final String URL_GITHUB_WIKI        = "url.github.wiki";
	public static final String ERROR_ABOUT_LICENSE    = "error.about.license";

	public static final List<Locale> SUPPORTED_LOCALES = new ArrayList<>();

	static
	{
		SUPPORTED_LOCALES.add(Locale.ENGLISH);
	}
}
