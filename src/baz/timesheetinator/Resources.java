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

import org.eclipse.swt.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.*;

import java.io.*;
import java.util.*;

import jhi.swtcommons.util.*;

/**
 * @author Sebastian Raubach
 */
public class Resources
{
	/**
	 * Disposes all {@link Image}s that were loaded during execution (if they haven't already been disposed)
	 */
	public static void disposeResources()
	{
		Images.disposeAll();
		Colors.disposeAll();
	}

	/**
	 * {@link Fonts} is a utility class to handle {@link Font}s
	 *
	 * @author Sebastian Raubach
	 */
	public static class Fonts
	{
		/**
		 * Applies the given {@link Font} size to the given {@link Control}. A new {@link Font} instance is created internally, but disposed via a
		 * {@link Listener} for {@link SWT#Dispose} attached to the {@link Control}.
		 *
		 * @param control  The {@link Control}
		 * @param fontSize The {@link Font} size
		 */
		public static void applyFontSize(Control control, int fontSize)
		{
			/* Increase the font size */
			FontData[] fontData = control.getFont().getFontData();
			fontData[0].setHeight(fontSize);
			final Font font = new Font(control.getShell().getDisplay(), fontData[0]);
			control.setFont(font);

			control.addListener(SWT.Dispose, event ->
			{
				if (!font.isDisposed())
					font.dispose();
			});
		}
	}

	public static class Images
	{
		private static Map<String, Image> CACHE = new HashMap<>();

		public static Image LOGO       = getImage("img/logo.png");
		public static Image GITHUB     = getImage("img/github.png");
		public static Image EMAIL      = getImage("img/email.png");
		public static Image LOGO_SMALL = ResourceUtils.resize(getImage("img/logo.png", false), 100, 100);

		public static Image getImage(String path, boolean cache)
		{
			Image result = cache ? CACHE.get(path) : null;

			if (result == null)
			{
				if (Timesheetinator.WITHIN_JAR)
				{
					InputStream stream = Resources.class.getClassLoader().getResourceAsStream(path);
					if (stream != null)
					{
						result = new Image(null, stream);
					}
				}
				else
				{
					result = new Image(null, path);
				}

				if (result != null && cache)
					CACHE.put(path, result);
			}

			return result;
		}

//		/**
//		 * Loads and returns the {@link Image} with the given name
//		 *
//		 * @param name The image name
//		 * @return The {@link Image} object
//		 */
//		public static Image loadImage(String name)
//		{
//			/* Check if we've already created that Image before */
//			Image newImage = CACHE.get(name);
//			if (newImage == null || newImage.isDisposed())
//			{
//				/* If not, try to load it */
//				try
//				{
//					/* Check if this code is in the jar */
//					if (Timesheetinator.WITHIN_JAR)
//					{
//						newImage = new Image(null, (ImageDataProvider) zoom ->
//						{
//							InputStream stream = Timesheetinator.class.getClassLoader().getResourceAsStream(name);
//
//							Image image = new Image(null, stream);
//							ImageData data = image.getImageData();
//							image.dispose();
//
//							return data;
//						});
//					}
//					/* Else, we aren't using a jar */
//					else
//					{
//						newImage = new Image(null, (ImageFileNameProvider) zoom -> name);
//					}
//
//                    /* Remember that we loaded this image */
//					CACHE.put(name, newImage);
//				}
//				catch (SWTException ex)
//				{
//					return null;
//				}
//			}
//
//			return newImage;
//		}

		public static Image getImage(String path)
		{
			return getImage(path, true);
		}

		public static void disposeAll()
		{
			CACHE.values()
				 .stream()
				 .filter(i -> !i.isDisposed())
				 .forEach(Image::dispose);
		}
	}

	/**
	 * {@link Colors} is a utility class to handle {@link Color}s
	 *
	 * @author Sebastian Raubach
	 */
	public static class Colors
	{
		/** Color cache */
		private static final Map<String, Color> CACHE = new HashMap<>();

		public static Color HIGHLIGHT = loadColor("#ffffff");

		/**
		 * Loads and returns the {@link Color} with the given hex
		 *
		 * @param color The color hex
		 * @return The {@link Color} object
		 */
		public static Color loadColor(String color)
		{
			Color newColor = CACHE.get(color);
			if (newColor == null || newColor.isDisposed())
			{
				java.awt.Color col;
				try
				{
					col = java.awt.Color.decode(color);
				}
				catch (Exception e)
				{
					col = java.awt.Color.WHITE;
				}
				int red = col.getRed();
				int blue = col.getBlue();
				int green = col.getGreen();

				newColor = new Color(null, red, green, blue);

				CACHE.put(color, newColor);
			}

			return newColor;
		}

		private static void disposeAll()
		{
			CACHE.values()
				 .stream()
				 .filter(col -> !col.isDisposed())
				 .forEach(Color::dispose);

			CACHE.clear();
		}
	}
}
