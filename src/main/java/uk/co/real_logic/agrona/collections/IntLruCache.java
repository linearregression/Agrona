/*
 * Copyright 2015 Real Logic Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.co.real_logic.agrona.collections;

import uk.co.real_logic.agrona.LangUtil;
import uk.co.real_logic.agrona.generation.DoNotSub;

import java.util.function.IntFunction;

public final class IntLruCache<T extends AutoCloseable> implements AutoCloseable
{
    @DoNotSub private final int capacity;
    private final IntFunction<T> factory;
    private final int[] keys;
    private final AutoCloseable[] values;

    @DoNotSub private int size;

    public IntLruCache(
        @DoNotSub final int capacity,
        final IntFunction<T> factory)
    {
        this.capacity = capacity;
        this.factory = factory;
        keys = new int[capacity];
        values = new AutoCloseable[capacity];

        size = 0;
    }

    @SuppressWarnings("unchecked")
    public T lookup(final int key)
    {
        @DoNotSub int size = this.size;
        final int[] keys = this.keys;
        final AutoCloseable[] values = this.values;

        for (@DoNotSub int i = 0; i < size; i++)
        {
            if (keys[i] == key)
            {
                final T value = (T) values[i];

                makeMostRecent(key, value, i);

                return value;
            }
        }

        final T value = factory.apply(key);

        if (size == capacity)
        {
            try
            {
                values[size - 1].close();
            }
            catch (Exception e)
            {
                LangUtil.rethrowUnchecked(e);
            }
        }
        else
        {
            size++;
            this.size = size;
        }

        makeMostRecent(key, value, size - 1);

        return value;
    }

    private void makeMostRecent(
        final int key,
        final Object value,
        @DoNotSub final int fromIndex)
    {
        final int[] keys = this.keys;
        final Object[] values = this.values;

        for (@DoNotSub int i = fromIndex; i > 0; i--)
        {
            keys[i] = keys[i - 1];
            values[i] = values[i - 1];
        }

        keys[0] = key;
        values[0] = value;
    }

    @DoNotSub public int capacity()
    {
        return capacity;
    }

    public void close()
    {
        for (@DoNotSub int i = 0; i < size; i++)
        {
            try
            {
                values[i].close();
            }
            catch (Exception e)
            {
                LangUtil.rethrowUnchecked(e);
            }
        }
    }
}
