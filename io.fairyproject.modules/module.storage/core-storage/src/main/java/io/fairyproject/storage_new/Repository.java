package io.fairyproject.storage_new;

import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public interface Repository<I, T> {

    @NotNull Optional<T> findById(@NotNull I id);

    @NotNull Find<T> find();

    void save(@NotNull T t);

    void saveAll(@NotNull Iterable<T> t);

    boolean hasById(@NotNull I id);

    void delete(@NotNull T t);

    void deleteById(@NotNull I id);

    int count();

    @NotNull Iterable<T> all();

}
