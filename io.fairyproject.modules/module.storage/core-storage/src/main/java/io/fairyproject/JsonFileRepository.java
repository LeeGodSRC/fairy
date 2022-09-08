package io.fairyproject;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.fairyproject.jackson.JacksonService;
import io.fairyproject.providers.JsonFileRepositoryProvider;
import io.fairyproject.util.exceptionally.SneakyThrowUtil;
import io.fairyproject.util.exceptionally.ThrowingRunnable;
import org.bson.BsonString;
import org.bson.BsonValue;
import org.bson.UuidRepresentation;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.json.JsonReader;
import org.bson.json.JsonWriter;
import org.mongojack.internal.MongoJackModule;
import org.mongojack.internal.stream.JacksonCodec;
import org.mongojack.internal.stream.JacksonDecoder;
import org.mongojack.internal.stream.JacksonEncoder;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public class JsonFileRepository<T, I extends Serializable> extends AbstractRepository<T, I, JsonFileRepositoryProvider> {

    private static JacksonService JACKSON_SERVICE;

    private Path path;
    private JacksonCodec<T> codec;

    public JsonFileRepository(JsonFileRepositoryProvider repositoryProvider, Class<T> type, String repoId) {
        super(repositoryProvider, type, repoId);
    }

    @Override
    public void init() {
        this.path = this.repositoryProvider.directory().resolve(this.repoId);
        ThrowingRunnable.sneaky(() -> Files.createDirectories(this.path)).run();

        ObjectMapper objectMapper = JACKSON_SERVICE.getOrCreateJacksonMapper("jsonFile", MongoJackModule::configure);
        JacksonEncoder<T> encoder = new JacksonEncoder<>(this.type, this.type, objectMapper, UuidRepresentation.JAVA_LEGACY);
        JacksonDecoder<T> decoder = new JacksonDecoder<>(this.type, this.type, objectMapper, UuidRepresentation.JAVA_LEGACY);
        this.codec = new JacksonCodec<>(encoder, decoder);
    }

    @Override
    public <S extends T> S save(S pojo) {
        BsonValue id = this.codec.getDocumentId(pojo);
        BsonString idString = id.asString();
        try (BufferedWriter fileWriter = Files.newBufferedWriter(this.path.resolve(idString + ".json"))) {
            this.codec.encode(new JsonWriter(fileWriter), pojo, EncoderContext.builder().build());
            return pojo;
        } catch (IOException e) {
            SneakyThrowUtil.sneakyThrow(e);
            return null;
        }
    }

    @Override
    public Optional<T> findById(I id) {
        Path path = this.path.resolve(id.toString() + ".json");
        if (Files.exists(path)) {
            try (BufferedReader reader = Files.newBufferedReader(path)) {
                return Optional.of(this.codec.decode(new JsonReader(reader), DecoderContext.builder().build()));
            } catch (IOException e) {
                SneakyThrowUtil.sneakyThrow(e);
            }
        }
        return Optional.empty();
    }

    @Override
    public <Q> Optional<T> findByQuery(String query, Q value) {
        return Optional.empty();
    }

    @Override
    public boolean existsById(I id) {
        return false;
    }

    @Override
    public Iterable<T> findAll() {
        return null;
    }

    @Override
    public Iterable<T> findAllById(List<I> ids) {
        return null;
    }

    @Override
    public long count() {
        return 0;
    }

    @Override
    public void deleteById(I id) {

    }

    @Override
    public <Q> void deleteByQuery(String query, Q value) {

    }

    @Override
    public void deleteAll() {

    }
}
