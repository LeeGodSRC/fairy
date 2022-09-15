package io.fairyproject.storage_new;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public interface Find<T> {

    @Contract("-> this")
    Find<T> byId();

    @Contract("_, _ -> this")
    Find<T> by(String field, Object value);

    @NotNull Optional<T> one();

    @NotNull Iterable<T> all();

}
