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

package baz.timesheetinator.database;

/**
 * @author Sebastian Raubach
 */
public abstract class DatabaseObject
{
	protected Integer id;

	public DatabaseObject(Integer id)
	{
		this.id = id;
	}

	public Integer getId()
	{
		return id;
	}

	public DatabaseObject setId(Integer id)
	{
		this.id = id;
		return this;
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		DatabaseObject that = (DatabaseObject) o;

		return id.equals(that.id);

	}

	@Override
	public int hashCode()
	{
		return id.hashCode();
	}

	@Override
	public String toString()
	{
		return "DatabaseObject{" +
				"id=" + id +
				'}';
	}
}
