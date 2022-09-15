package io.fairyproject.storage_new;

import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class RepositoryImpl<I, T> implements Repository<I, T> {
    @Override
    public @NotNull Optional<T> findById(@NotNull I id) {
        return Optional.empty();
    }

    @Override
    public @NotNull Find<T> find() {
        return null;
    }

    @Override
    public void save(@NotNull T t) {

    }

    @Override
    public void saveAll(@NotNull Iterable<T> t) {

    }

    @Override
    public int count() {
        return 0;
    }

    @Override
    public @NotNull Iterable<T> all() {
        return null;
    }
}
