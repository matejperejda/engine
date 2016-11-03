/*
 * -----------------------------------------------------------------------\
 * Lumeer
 *  
 * Copyright (C) 2016 the original author or authors.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * -----------------------------------------------------------------------/
 */
package io.lumeer.mongodb;

import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.data.DataStorage;

import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Updates;
import org.bson.BsonDocument;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.enterprise.inject.Model;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 *         <a href="mailto:kubedo8@gmail.com">Jakub Rodák</a>
 *         <a href="mailto:mat.per.vt@gmail.com">Matej Perejda</a>
 */
@Model
public class MongoDbStorage implements DataStorage {

   private final String CURSOR_KEY = "cursor";
   private final String FIRST_BATCH_KEY = "firstBatch";

   private MongoDatabase database;

   @PostConstruct
   public void connect() {
      MongoClient mongo = new MongoClient("localhost", 27017); // default connection
      database = mongo.getDatabase("lumeer");
   }

   @Override
   public List<String> getAllCollections() {
      return database.listCollectionNames().into(new ArrayList<>());
   }

   @Override
   public void createCollection(final String collectionName) {
      database.createCollection(collectionName);
   }

   @Override
   public void dropCollection(final String collectionName) {
      database.getCollection(collectionName).drop();
   }

   @Override
   public String createDocument(final String collectionName, final DataDocument dataDocument) {
      Document doc = new Document(dataDocument);
      database.getCollection(collectionName).insertOne(doc);
      return doc.getObjectId("_id").toString();
   }

   @Override
   public DataDocument readDocument(final String collectionName, final String documentId) {
      BasicDBObject filter = new BasicDBObject("_id", new ObjectId(documentId));
      Document document = database.getCollection(collectionName).find(filter).first();

      if (document == null) {
         return null;
      }

      return new DataDocument(document);
   }

   @Override
   public void updateDocument(final String collectionName, final DataDocument updatedDocument, final String documentId) {
      BasicDBObject filter = new BasicDBObject("_id", new ObjectId(documentId));
      BasicDBObject updateBson = new BasicDBObject("$set", new BasicDBObject(updatedDocument));
      database.getCollection(collectionName).updateOne(filter, updateBson);
   }

   @Override
   public void dropDocument(final String collectionName, final String documentId) {
      BasicDBObject filter = new BasicDBObject("_id", new ObjectId(documentId));
      database.getCollection(collectionName).deleteOne(filter);
   }

   @Override
   public void renameAttribute(final String collectionName, final String oldName, final String newName) {
      database.getCollection(collectionName).updateMany(BsonDocument.parse("{}"), Updates.rename(oldName, newName));
   }

   @Override
   public List<DataDocument> search(final String query) {
      final List<DataDocument> result = new ArrayList<>();

      Document cursor = (Document) database.runCommand(BsonDocument.parse(query)).get(CURSOR_KEY);

      ((ArrayList<Document>) cursor.get(FIRST_BATCH_KEY)).forEach(d -> result.add(new DataDocument(d)));

      return result;
   }

   @Override
   public List<DataDocument> search(final String collectionName, final String filter, final String sort, final int skip, final int limit) {
      final List<DataDocument> result = new ArrayList<>();

      MongoCollection<Document> collection = database.getCollection(collectionName);
      FindIterable<Document> documents = filter != null ? collection.find(BsonDocument.parse(filter)) : collection.find();
      documents.sort(sort != null ? BsonDocument.parse(sort) : null)
               .skip(skip)
               .limit(limit)
               .into(new ArrayList<>())
               .forEach(d -> result.add(new DataDocument(d)));

      return result;
   }

}