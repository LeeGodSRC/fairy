package io.fairyproject.providers;

import com.google.common.collect.ImmutableMap;
import io.fairyproject.AbstractRepositoryProvider;
import io.fairyproject.Repository;
import io.fairyproject.util.exceptionally.ThrowingRunnable;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

@Getter
@Accessors(fluent = true)
public class JsonFileRepositoryProvider extends AbstractRepositoryProvider {

    private Path directory;

    public JsonFileRepositoryProvider(String id) {
        super(id);
    }

    @Override
    public void build0() {
        ThrowingRunnable.sneaky(() -> Files.createDirectories(this.directory)).run();
    }

    @Override
    public <E, ID extends Serializable> Repository<E, ID> createRepository(Class<E> entityType, String repoId) {
        return null;
    }

    @Override
    public Map<String, String> getDefaultOptions() {
        return ImmutableMap.of("path", "data");
    }

    @Override
    public void registerOptions(Map<String, String> map) {
        this.directory = Paths.get(map.get("path"));
    }

    @Override
    public void close() throws Exception {
        // do nothing
    }
}
