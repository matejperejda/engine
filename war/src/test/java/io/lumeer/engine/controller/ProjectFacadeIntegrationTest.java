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

import static io.lumeer.engine.api.LumeerConst.Project;
import static io.lumeer.engine.api.LumeerConst.UserRoles;
import static org.assertj.core.api.Assertions.assertThat;

import io.lumeer.engine.IntegrationTestBase;
import io.lumeer.engine.annotation.SystemDataStorage;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.engine.api.data.DataStorageDialect;
import io.lumeer.engine.api.exception.UserAlreadyExistsException;
import io.lumeer.engine.provider.DataStorageProvider;

import org.jboss.arquillian.junit.Arquillian;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;

/**
 * Tests for ProjectFacade
 */
@RunWith(Arquillian.class)
public class ProjectFacadeIntegrationTest extends IntegrationTestBase {

   @Inject
   @SystemDataStorage
   private DataStorage systemDataStorage;

   @Inject
   private DataStorageDialect dataStorageDialect;

   @Inject
   private DataStorageProvider dataStorageProvider;

   @Inject
   private ProjectFacade projectFacade;

   @Inject
   private OrganisationFacade organisationFacade;

   @Inject
   private UserRoleFacade userRoleFacade;

   @Before
   public void setUp() throws Exception {
      systemDataStorage.dropManyDocuments(Project.COLLECTION_NAME, dataStorageDialect.documentFilter("{}"));
      systemDataStorage.dropManyDocuments(UserRoles.COLLECTION_NAME, dataStorageDialect.documentFilter("{}"));
   }


   @Test
   public void basicMethodsTest() throws Exception {
      final String project1 = "project1";
      final String project2 = "project2";
      final String project3 = "project3";
      final String project4 = "project4";

      projectFacade.createProject(project1, "Project One");
      projectFacade.createProject(project2, "Project Two");

      Map<String, String> map = projectFacade.readProjectsMap(organisationFacade.getOrganisationId());
      assertThat(map).containsKeys(project1, project2);
      assertThat(map).containsEntry(project1, "Project One");
      assertThat(map).containsEntry(project2, "Project Two");

      assertThat(projectFacade.readProjectId("Project Two")).isEqualTo(project2);
      assertThat(projectFacade.readProjectId("Project One")).isEqualTo(project1);
      assertThat(projectFacade.readProjectId("Project Three")).isNull();

      assertThat(projectFacade.readProjectName(project1)).isEqualTo("Project One");
      assertThat(projectFacade.readProjectName(project2)).isEqualTo("Project Two");
      assertThat(projectFacade.readProjectName(project3)).isNull();

      projectFacade.updateProjectId(project1, project3);
      assertThat(projectFacade.readProjectName(project3)).isEqualTo("Project One");
      assertThat(projectFacade.readProjectName(project1)).isNull();

      projectFacade.renameProject(project2, "Project Two Renamed");
      assertThat(projectFacade.readProjectName(project2)).isEqualTo("Project Two Renamed");

      map = projectFacade.readProjectsMap(organisationFacade.getOrganisationId());
      assertThat(map).containsOnlyKeys(project2, project3);
      projectFacade.createProject(project4, "Project Four");
      map = projectFacade.readProjectsMap(organisationFacade.getOrganisationId());
      assertThat(map).containsOnlyKeys(project2, project3, project4);
      projectFacade.dropProject(project4);
      map = projectFacade.readProjectsMap(organisationFacade.getOrganisationId());
      assertThat(map).containsOnlyKeys(project2, project3);
   }

   @Test
   public void metadataMethodsTest() throws Exception {
      final String project1 = "project11";
      final String project2 = "project22";

      projectFacade.createProject(project1, "Project One");
      projectFacade.createProject(project2, "Project Two");

      // read and update one test
      assertThat(projectFacade.readProjectMetadata(project1, Project.ATTR_META_COLOR)).isNull();
      projectFacade.updateProjectMetadata(project1, Project.ATTR_META_COLOR, "000000");
      assertThat(projectFacade.readProjectMetadata(project1, Project.ATTR_META_COLOR)).isEqualTo("000000");
      assertThat(projectFacade.readProjectMetadata(project2, Project.ATTR_META_COLOR)).isNull();

      // read, update more and drop test
      projectFacade.updateProjectMetadata(project2, new DataDocument(Project.ATTR_META_COLOR, "ffffff")
            .append(Project.ATTR_META_ICON, "fa-user"));
      assertThat(projectFacade.readProjectMetadata(project2, Project.ATTR_META_COLOR)).isEqualTo("ffffff");
      assertThat(projectFacade.readProjectMetadata(project2, Project.ATTR_META_ICON)).isEqualTo("fa-user");
      projectFacade.dropProjectMetadata(project2, Project.ATTR_META_COLOR);
      assertThat(projectFacade.readProjectMetadata(project2, Project.ATTR_META_COLOR)).isNull();

      // default user roles test
      assertThat(projectFacade.readDefaultRoles(project1)).isNull();
      projectFacade.setDefaultRoles(project1, Arrays.asList("role1", "role2", "role3"));
      assertThat(projectFacade.readDefaultRoles(project1)).containsExactly("role1", "role2", "role3");
      projectFacade.setDefaultRoles(project1, Arrays.asList("role2", "role4", "role5"));
      assertThat(projectFacade.readDefaultRoles(project1)).containsExactly("role2", "role4", "role5");
   }

   @Test
   public void userManagementTest() throws Exception {
      final String project = "project111";
      projectFacade.createProject(project, "Project One");
      projectFacade.addUserToProject(project, "u1", Arrays.asList("r1", "r2", "r3"));
      projectFacade.addUserToProject(project, "u2", Arrays.asList("r4", "r2", "r3"));
      projectFacade.addUserToProject(project, "u3", Arrays.asList("r4", "r2"));

      //test creation and reading of all users and theirs roles
      Map<String, List<String>> users = projectFacade.readUsersRoles(project);
      assertThat(users).containsOnlyKeys("u1", "u2", "u3");
      assertThat(users.get("u1")).containsExactly("r1", "r2", "r3");
      assertThat(users.get("u2")).containsExactly("r4", "r2", "r3");
      assertThat(users.get("u3")).containsExactly("r4", "r2");

      //test read roles for user
      List<String> userRoles = projectFacade.readUserRoles(project, "u1");
      assertThat(userRoles).containsExactly("r1", "r2", "r3");
      userRoles = projectFacade.readUserRoles(project, "u2");
      assertThat(userRoles).containsExactly("r4", "r2", "r3");

      // test drop
      projectFacade.dropUserFromProject(project, "u3");
      users = projectFacade.readUsersRoles(project);
      assertThat(users).containsOnlyKeys("u1", "u2");

      // test add and remove roles inside user
      projectFacade.addRolesToUser(project, "u1", Arrays.asList("r10", "r11"));
      userRoles = projectFacade.readUserRoles(project, "u1");
      assertThat(userRoles).containsExactly("r1", "r2", "r3", "r10", "r11");
      projectFacade.removeRolesFromUser(project, "u1", Arrays.asList("r1", "r2", "r3"));
      userRoles = projectFacade.readUserRoles(project, "u1");
      assertThat(userRoles).containsExactly("r10", "r11");

      //has role with transitive support test
      userRoleFacade.createRole(organisationFacade.getOrganisationId(), project, "ur1", Arrays.asList("c1", "c2", "c3"));
      projectFacade.addUserToProject(project, "user100", Arrays.asList("ur1", "c4", "c5"));
      assertThat(projectFacade.hasUserRole(project, "user100", "c1")).isTrue();
      assertThat(projectFacade.hasUserRole(project, "user100", "c2")).isTrue();
      assertThat(projectFacade.hasUserRole(project, "user100", "ur1")).isTrue();
      assertThat(projectFacade.hasUserRole(project, "user100", "c4")).isTrue();
      assertThat(projectFacade.hasUserRole(project, "user100", "c6")).isFalse();
   }

   @Test(expected = UserAlreadyExistsException.class)
   public void userAlreadyExistsTest() throws Exception {
      String project = "project1111";
      projectFacade.createProject(project, "Project One");
      projectFacade.addUserToProject(project, "u1", Arrays.asList("r1", "r2", "r3"));
      projectFacade.addUserToProject(project, "u1", Arrays.asList("r1", "r2", "r3"));
   }

}
