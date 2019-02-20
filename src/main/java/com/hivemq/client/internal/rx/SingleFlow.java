/*
 * Copyright 2018 The MQTT Bee project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.hivemq.client.internal.rx;

import io.reactivex.SingleObserver;
import io.reactivex.disposables.Disposable;
import org.jetbrains.annotations.NotNull;

/**
 * @author Silvio Giebl
 */
public class SingleFlow<T> implements Disposable {

    private final @NotNull SingleObserver<? super T> observer;
    private volatile boolean disposed;

    public SingleFlow(final @NotNull SingleObserver<? super T> observer) {
        this.observer = observer;
    }

    public void onSuccess(final @NotNull T t) {
        observer.onSuccess(t);
    }

    public void onError(final @NotNull Throwable t) {
        observer.onError(t);
    }

    @Override
    public void dispose() {
        disposed = true;
    }

    @Override
    public boolean isDisposed() {
        return disposed;
    }

    public boolean isCancelled() {
        return isDisposed();
    }
}