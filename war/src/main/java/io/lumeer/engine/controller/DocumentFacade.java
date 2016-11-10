/*
 * -----------------------------------------------------------------------\
 * Lumeer
 *  
 * Copyright (C) 2016 - 2017 the original author or authors.
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
package io.lumeer.engine.controller;

import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.engine.exception.CollectionNotFoundException;
import io.lumeer.engine.exception.DocumentNotFoundException;
import io.lumeer.engine.exception.UnsuccessfulOperationException;
import io.lumeer.engine.util.ErrorMessageBuilder;
import io.lumeer.engine.util.Utils;

import java.io.Serializable;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;

/**
 * @author <a href="mailto:kubedo8@gmail.com">Jakub Rodák</a>
 */
@SessionScoped
public class DocumentFacade implements Serializable {

   @Inject
   private DataStorage dataStorage;

   @Inject
   private CollectionFacade collectionFacade;

   @Inject
   private VersionFacade versionFacade;

   @Inject
   private DocumentMetadataFacade documentMetadataFacade;

   //@Inject
   private String userName = "testUser";

   /**
    * Creates and inserts a new document to specified collection.
    *
    * @param collectionName
    *       the name of the collection where the document will be created
    * @param document
    *       the DataDocument object representing a document to be created
    * @return the id of the newly created document
    */
   public String createDocument(final String collectionName, final DataDocument document) throws CollectionNotFoundException, UnsuccessfulOperationException {
      if (!collectionFacade.isDatabaseCollection(collectionName)) {
         throw new CollectionNotFoundException(ErrorMessageBuilder.collectionNotFoundString(collectionName));
      }
      document.put(documentMetadataFacade.DOCUMENT_UPDATE_DATE_KEY, Utils.getCurrentTimeString());
      document.put(documentMetadataFacade.DOCUMENT_UPDATED_BY_USER_KEY, userName);
      document.put(versionFacade.getVersionMetadataString(), 0);
      String documentId = dataStorage.createDocument(collectionName, document);
      if (documentId == null) {
         throw new UnsuccessfulOperationException(ErrorMessageBuilder.createDocumentUnsuccesfulString());
      }
      return documentId;
   }

   /**
    * Reads the specified document in given collection by its id.
    *
    * @param collectionName
    *       the name of the collection where the document is located
    * @param documentId
    *       the id of the read document
    * @return the DataDocument object representing the read document
    * @throws CollectionNotFoundException
    *       if collection is not found in database
    * @throws DocumentNotFoundException
    *       if document is not found in database
    */
   public DataDocument readDocument(final String collectionName, final String documentId) throws CollectionNotFoundException, DocumentNotFoundException {
      if (!collectionFacade.isDatabaseCollection(collectionName)) {
         throw new CollectionNotFoundException(ErrorMessageBuilder.collectionNotFoundString(collectionName));
      }
      DataDocument dataDocument = dataStorage.readDocument(collectionName, documentId);
      if (dataDocument == null) {
         throw new DocumentNotFoundException(ErrorMessageBuilder.documentNotFoundString());
      }
      return dataDocument;
   }

   /**
    * Modifies an existing document in given collection by its id.
    *
    * @param collectionName
    *       the name of the collection where the existing document is located
    * @param updatedDocument
    *       the DataDocument object representing a document with changes to update
    */
   public void updateDocument(final String collectionName, final DataDocument updatedDocument) throws CollectionNotFoundException, DocumentNotFoundException {
      if (!collectionFacade.isDatabaseCollection(collectionName)) {
         throw new CollectionNotFoundException(ErrorMessageBuilder.collectionNotFoundString(collectionName));
      }
      String documentId = updatedDocument.getString("_id");
      if (dataStorage.readDocument(collectionName, documentId) == null) {
         throw new DocumentNotFoundException(ErrorMessageBuilder.documentNotFoundString());
      }
      updatedDocument.put(documentMetadataFacade.DOCUMENT_UPDATE_DATE_KEY, Utils.getCurrentTimeString());
      updatedDocument.put(documentMetadataFacade.DOCUMENT_UPDATED_BY_USER_KEY, userName);
      versionFacade.newDocumentVersion(collectionName, updatedDocument);
   }

   /**
    * Drops an existing document in given collection by its id.
    *
    * @param collectionName
    *       the name of the collection where the document is located
    * @param documentId
    *       the id of the document to drop
    * @throws CollectionNotFoundException
    *       if collection is not found in database
    * @throws DocumentNotFoundException
    *       if document is not found in database
    * @throws UnsuccessfulOperationException
    *       if document stay in collection after drop
    */
   public void dropDocument(final String collectionName, final String documentId) throws CollectionNotFoundException, DocumentNotFoundException, UnsuccessfulOperationException {
      if (!collectionFacade.isDatabaseCollection(collectionName)) {
         throw new CollectionNotFoundException(ErrorMessageBuilder.collectionNotFoundString(collectionName));
      }
      DataDocument dataDocument = dataStorage.readDocument(collectionName, documentId);
      if (dataDocument == null) {
         throw new DocumentNotFoundException(ErrorMessageBuilder.documentNotFoundString());
      }
      versionFacade.newDocumentVersion(collectionName, dataDocument);
      dataStorage.dropDocument(collectionName, documentId);

      dataDocument = dataStorage.readDocument(collectionName, documentId);
      if (dataDocument != null) {
         throw new UnsuccessfulOperationException(ErrorMessageBuilder.dropDocumentUnsuccesfulString());
      }
   }

}