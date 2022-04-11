/*
 * MIT License
 *
 * Copyright (c) 2021 Imanity
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.fairyproject.container.object;

import io.fairyproject.container.ComponentHolder;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

public class ComponentContainerObject extends BaseContainerObject {

    private final ComponentHolder componentHolder;
    private final AtomicBoolean enabled;
    private final ReentrantLock lock;

    public ComponentContainerObject(Class<?> type, @Nullable Object instance, ComponentHolder componentHolder) {
        super(type, instance);

        this.componentHolder = componentHolder;
        this.enabled = new AtomicBoolean(false);
        this.lock = new ReentrantLock();
    }

    @Override
    public void onEnable() {
        super.onEnable();
        this.lock.lock();
        try {
            if (this.enabled.compareAndSet(false, true)) {
                this.componentHolder.onEnable(this.getInstance());
            }
        } finally {
            this.lock.unlock();
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        this.lock.lock();
        try {
            if (this.enabled.compareAndSet(true, false)) {
                this.componentHolder.onDisable(this.getInstance());
            }
        } finally {
            this.lock.unlock();
        }
    }
}
