/*******************************************************************************
 * Copyright 2017 xlate.io LLC, http://www.xlate.io
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package io.xlate.edi.schema;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;

final class Externalizer {

	private Externalizer() {}

	static void writeUTF(String value, ObjectOutput out) throws IOException {
		if (value == null) {
			out.writeBoolean(false);
		} else {
			out.writeBoolean(true);
			out.writeUTF(value);
		}
	}

	static String readUTF(ObjectInput in) throws IOException {
		if (in.readBoolean()) {
			return in.readUTF();
		}
		return null;
	}

	static void writeStrings(Collection<String> strings, ObjectOutput out) throws IOException {
		out.writeInt(strings.size());

		for (String string : strings) {
			writeUTF(string, out);
		}
	}

	static <T extends Collection<String>> T readStrings(Class<T> type, ObjectInput in) throws IOException {
		int valueCount = in.readInt();
		T strings;

		try {
			strings = type.getConstructor(int.class).newInstance(valueCount);
		} catch (InstantiationException | IllegalAccessException
				| IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException e) {
			throw new IllegalArgumentException(e);
		}

		for (int i = 0; i < valueCount; i++) {
			strings.add(readUTF(in));
		}

		return strings;
	}

	static <E extends Externalizable, C extends Collection<E>> void writeExternalizables(C exts, ObjectOutput out) throws IOException {
		out.writeInt(exts.size());

		for (Externalizable ext : exts) {
			ext.writeExternal(out);
		}
	}

	static <E extends Externalizable, C extends Collection<E>> C readExternalizables(
			Class<E> entryType, Class<C> collectionType, ObjectInput in) throws IOException {
		int extCount = in.readInt();
		C exts;

		try {
			exts = collectionType.getConstructor(int.class).newInstance(extCount);
			Constructor<E> constr = entryType.getConstructor();

			for (int i = 0; i < extCount; i++) {
				E ext = constr.newInstance();
				ext.readExternal(in);
				exts.add(ext);
			}
		} catch (InstantiationException | IllegalAccessException
				| IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException | ClassNotFoundException e) {
			throw new IllegalArgumentException(e);
		}

		return exts;
	}
}
