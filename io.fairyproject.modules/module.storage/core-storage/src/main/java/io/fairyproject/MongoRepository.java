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

package io.fairyproject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.model.Filters;
import io.fairyproject.container.Autowired;
import io.fairyproject.jackson.JacksonService;
import io.fairyproject.mongo.AbstractMongoRepositoryProvider;
import lombok.Getter;
import org.bson.BsonDocument;
import org.mongojack.JacksonMongoCollection;
import org.mongojack.internal.MongoJackModule;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MongoRepository<T, I extends Serializable> extends AbstractRepository<T, I, AbstractMongoRepositoryProvider> {

    @Autowired
    private static JacksonService JACKSON_SERVICE;

    @Getter
    protected JacksonMongoCollection<T> collection;

    public MongoRepository(AbstractMongoRepositoryProvider repositoryProvider, Class<T> type, String repoId) {
        super(repositoryProvider, type, repoId);
    }

    public void init() {
        this.repositoryProvider.withLock(() -> {
            this.collection = this.repositoryProvider.createCollection(this.objectMapper(), this.repoId, this.type());
        });
    }

    public ObjectMapper objectMapper() {
        return JACKSON_SERVICE.getOrCreateJacksonMapper("mongo", MongoJackModule::configure);
    }

    @Override
    public <S extends T> S save(S pojo) {
        this.repositoryProvider.withLock(() -> this.collection.save(pojo));
        return pojo;
    }

    @Override
    public <S extends T> Iterable<S> saveAll(Iterable<S> pojoIterable) {
        this.repositoryProvider.withLock(() -> pojoIterable.forEach(pojo -> this.collection.save(pojo)));
        return pojoIterable;
    }

    @Override
    public Optional<T> findById(I id) {
        return Optional.ofNullable(this.repositoryProvider.withLock(() -> this.collection.findOneById(id)));
    }

    @Override
    public <Q> Optional<T> findByQuery(String query, Q value) {
        return Optional.ofNullable(this.repositoryProvider.withLock(() -> this.collection.findOne(Filters.eq(query, value))));
    }

    @Override
    public boolean existsById(I id) {
        return this.findById(id).isPresent();
    }

    @Override
    public Iterable<T> findAll() {
        return this.repositoryProvider.withLock(() -> this.collection.find());
    }

    @Override
    public Iterable<T> findAllById(List<I> ids) {
        List<T> result = new ArrayList<>();
        this.repositoryProvider.withLock(() -> {
            for (T t : this.collection.find(this.collection.createIdInQuery(ids))) {
                result.add(t);
            }
        });

        return result;
    }

    @Override
    public long count() {
        return this.repositoryProvider.withLock(() -> this.collection.countDocuments());
    }

    @Override
    public void deleteById(I id) {
        this.repositoryProvider.withLock(() -> this.collection.removeById(id));
    }

    @Override
    public <Q> void deleteByQuery(String query, Q value) {
        this.repositoryProvider.withLock(() -> this.collection.deleteMany(Filters.eq(query, value)));
    }

    @Override
    public void deleteAll() {
        this.repositoryProvider.withLock(() -> this.collection.deleteMany(new BsonDocument()));
    }

    public String queryId() {
        return "_id";
    }

}
