/**
 * Copyright (C) 2018 European Spallation Source ERIC.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package se.esss.ics.masar.persistence.dao.impl;

import static org.junit.Assert.*;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.sql.ResultSet;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.epics.vtype.Alarm;
import org.epics.vtype.AlarmSeverity;
import org.epics.vtype.AlarmStatus;
import org.epics.vtype.Display;
import org.epics.vtype.Time;
import org.epics.vtype.VDouble;
import org.epics.vtype.VInt;
import org.flywaydb.test.FlywayTestExecutionListener;
import org.flywaydb.test.annotation.FlywayTest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;

import se.esss.ics.masar.model.Config;
import se.esss.ics.masar.model.ConfigPv;
import se.esss.ics.masar.model.Folder;
import se.esss.ics.masar.model.Node;
import se.esss.ics.masar.model.Provider;
import se.esss.ics.masar.model.Snapshot;
import se.esss.ics.masar.model.SnapshotItem;
import se.esss.ics.masar.persistence.config.PersistenceConfiguration;
import se.esss.ics.masar.persistence.config.PersistenceTestConfig;
import se.esss.ics.masar.persistence.dao.ConfigDAO;
import se.esss.ics.masar.persistence.dao.SnapshotDAO;
import se.esss.ics.masar.services.exception.NodeNotFoundException;
import se.esss.ics.masar.services.exception.SnapshotNotFoundException;

@RunWith(SpringJUnit4ClassRunner.class)
@EnableConfigurationProperties
@ContextHierarchy({ @ContextConfiguration(classes = { PersistenceConfiguration.class, PersistenceTestConfig.class }) })
@TestPropertySource(properties = { "dbengine = h2" })
@TestExecutionListeners({ DependencyInjectionTestExecutionListener.class, FlywayTestExecutionListener.class })
public class DAOTest {

	@Autowired
	private ConfigDAO configDAO;

	@Autowired
	private SnapshotDAO snapshotDAO;
	
	private Alarm alarm;
	private Time time;
	private Display display;

	@Before
	public void init() {
		time = Time.of(Instant.ofEpochSecond(1000, 7000));
		alarm = Alarm.of(AlarmSeverity.NONE, AlarmStatus.NONE, "name");
		display = Display.none();
	}


	@Test(expected = IllegalArgumentException.class)
	@FlywayTest(invokeCleanDB = true)
	public void testCreateConfigNoParentFound() {

		ConfigPv configPv = ConfigPv.builder().pvName("pvName")
				.build();

		Config config = Config.builder().configPvList(Arrays.asList(configPv)).description("description")
			.build();

		config.setParentId(2);
		// The parent node does not exist in the database, so this throws an exception
		configDAO.createConfiguration(config);
	}

	@Test
	@FlywayTest(invokeCleanDB = true)
	public void testNewFolder(){
		
		Folder root = configDAO.getFolder(Node.ROOT_NODE_ID);
		
		Date lastModified = root.getLastModified();
	
		Folder folder = Folder.builder().name("SomeFolder").parentId(Node.ROOT_NODE_ID)
				.userName("username")
				.build();

		Node newNode = configDAO.createFolder(folder);
		
		root = configDAO.getFolder(Node.ROOT_NODE_ID);

		assertNotNull(newNode);
		
		// Check that the parent folder's last modified date is updated
		assertTrue(root.getLastModified().getTime() > lastModified.getTime());
	}

	@FlywayTest(invokeCleanDB = true)
	public void testNewFolderNoParentImpliesRootFolerAsParent() {

		Folder folderFromClient = Folder.builder().name("SomeFolder").build();

		Folder newFolder = configDAO.createFolder(folderFromClient);
		
		assertEquals(Node.ROOT_NODE_ID, newFolder.getId());

	}

	@Test(expected = IllegalArgumentException.class)
	@FlywayTest(invokeCleanDB = true)
	public void testNewFolderWithDuplicateName() {

		Folder folderFromClient = Folder.builder().name("SomeFolder").parentId(Node.ROOT_NODE_ID)
				.build();

		// Create a new folder
		configDAO.createFolder(folderFromClient);

		// Try to create a new folder with the same name in the same parent directory
		configDAO.createFolder(folderFromClient);

	}

	@Test
	@FlywayTest(invokeCleanDB = true)
	public void testNewFolderNoDuplicateName() {

		
		Folder folder1 = Folder.builder().name("Folder 1").parentId(Node.ROOT_NODE_ID).build();

		Folder folder2 = Folder.builder().name("Folder 2").parentId(Node.ROOT_NODE_ID).build();

		// Create a new folder
		assertNotNull(configDAO.createFolder(folder1));

		// Try to create a new folder with a different name in the same parent directory
		assertNotNull(configDAO.createFolder(folder2));

	}
	
	@Test(expected = IllegalArgumentException.class)
	@FlywayTest(invokeCleanDB = true)
	public void testNewFolderParentIsConfiguration() {

		
		ConfigPv configPv = ConfigPv.builder().pvName("pvName").build();

		Config config = Config.builder().description("description").parentId(Node.ROOT_NODE_ID)
				.name("My config").configPvList(Arrays.asList(configPv)).build();

		Config newConfig = configDAO.createConfiguration(config);
		
		Folder folder1 = Folder.builder().name("Folder 1").parentId(newConfig.getId()).build();
		
		configDAO.createFolder(folder1);

	}
	
	@Test(expected = IllegalArgumentException.class)
	@FlywayTest(invokeCleanDB = true)
	public void testNewFolderParentDoesNotExist() {
		
		Folder folder1 = Folder.builder().name("Folder 1").parentId(Integer.MAX_VALUE).build();
		
		configDAO.createFolder(folder1);

	}

	@Test
	@FlywayTest(invokeCleanDB = true)
	public void testNewConfig() {

		
		ConfigPv configPv = ConfigPv.builder().pvName("pvName").build();

		Config config = Config.builder()
				.description("description")
				.parentId(Node.ROOT_NODE_ID)
				.name("My config")
				.userName("username")
				.configPvList(Arrays.asList(configPv)).build();

		Config newConfig = configDAO.createConfiguration(config);

		assertFalse(newConfig.getConfigPvList().isEmpty());

		int configPvId = newConfig.getConfigPvList().get(0).getId();

		config = Config.builder().description("description").parentId(Node.ROOT_NODE_ID)
				.name("My config 2").configPvList(Arrays.asList(configPv)).build();

		newConfig = configDAO.createConfiguration(config);

		// Verify that a new ConfigPv has NOT been created

		assertEquals(configPvId, newConfig.getConfigPvList().get(0).getId());
	}

	@Test
	@FlywayTest(invokeCleanDB = true)
	public void testNewConfigNoConfigPvs() {

		Config config = Config.builder()
				.description("description")
				.parentId(Node.ROOT_NODE_ID)
				.name("My config")
				.build();

		Config newConfig = configDAO.createConfiguration(config);

		assertTrue(newConfig.getConfigPvList().isEmpty());
	}

	@Test
	@FlywayTest(invokeCleanDB = true)
	public void testDeleteConfiguration() {

		Config config = Config.builder().description("description").build();

		config.setParentId(Node.ROOT_NODE_ID);
		config.setName("My config");

		config = configDAO.createConfiguration(config);

		configDAO.deleteNode(config.getId());

		assertNull(configDAO.getConfiguration(config.getId()));

	}


	@Test
	@FlywayTest(invokeCleanDB = true)
	public void testDeleteConfigurationAndPvs() {

		ConfigPv configPv = ConfigPv.builder().pvName("pvName").build();

		Config config = Config.builder().description("description")
				.configPvList(Arrays.asList(configPv)).build();

		config.setParentId(Node.ROOT_NODE_ID);
		config.setName("My config");

		config = configDAO.createConfiguration(config);

		configDAO.deleteNode(config.getId());

	}

	@Test(expected = NodeNotFoundException.class)
	@FlywayTest(invokeCleanDB = true)
	public void testDeleteNonExistingFolder() {

		configDAO.deleteNode(-1);
	}

	@Test
	@FlywayTest(invokeCleanDB = true)
	public void testDeleteFolder() {

		Folder root = configDAO.getFolder(Node.ROOT_NODE_ID);

		Date rootLastModified = root.getLastModified();

		Folder folder1 = Folder.builder().name("SomeFolder").parentId(Node.ROOT_NODE_ID).build();

		folder1 = configDAO.createFolder(folder1);

		Folder folder2 = Folder.builder().name("SomeFolder").parentId(folder1.getId()).build();

		folder2 = configDAO.createFolder(folder2);

		Config config = configDAO
				.createConfiguration(Config.builder().name("Config").description("Desc").parentId(folder2.getId()).build());

		configDAO.deleteNode(folder1.getId());

		root = configDAO.getFolder(Node.ROOT_NODE_ID);
	
		assertTrue(root.getLastModified().getTime() > rootLastModified.getTime());

		assertNull(configDAO.getConfiguration(config.getId()));
		
		assertNull(configDAO.getFolder(folder2.getId()));
			
	}

	@Test
	@FlywayTest(invokeCleanDB = true)
	public void testDeleteConfigurationLeaveReferencedPVs() {

		Node parentNode = configDAO.getFolder(Node.ROOT_NODE_ID);

		ConfigPv configPv1 = ConfigPv.builder().pvName("pvName").build();

		ConfigPv configPv2 = ConfigPv.builder().pvName("pvName2")
				.build();

		Config config1 = Config.builder().description("description")
				.configPvList(Arrays.asList(configPv1, configPv2)).build();

		config1.setParentId(parentNode.getId());
		config1.setName("My config");

		config1 = configDAO.createConfiguration(config1);

		Config config2 = Config.builder().description("description")
				.configPvList(Arrays.asList(configPv2)).build();

		config2.setParentId(parentNode.getId());
		config2.setName("My config 2");

		config2 = configDAO.createConfiguration(config2);

		configDAO.deleteNode(config1.getId());

		Config config = configDAO.getConfiguration(config2.getId());

		assertEquals(1, config.getConfigPvList().size());

	}

	@Test
	@FlywayTest(invokeCleanDB = false)
	public void testGetNodeAsConfig() {

		Folder parentNode = configDAO.getFolder(Node.ROOT_NODE_ID);

		Config config = Config.builder().name("My config 3").parentId(parentNode.getId()).description("description")
				.build();

		Config newConfig = configDAO.createConfiguration(config);

		Config configFromDB = configDAO.getConfiguration(newConfig.getId());

		assertEquals(newConfig, configFromDB);
	}

	@Test
	@FlywayTest(invokeCleanDB = true)
	public void testSaveSnapshot() {

		Config config = Config.builder().name("My config 3").parentId(Node.ROOT_NODE_ID)
				.description("description")
				.configPvList(Arrays.asList(ConfigPv.builder().pvName("whatever").build())).build();

		config = configDAO.createConfiguration(config);
		
		SnapshotItem item1 = SnapshotItem.builder()
				.configPvId(config.getId())
				.fetchStatus(true)
				.value(VDouble.of(7.7, alarm, time, display))
				.build();

		Snapshot newSnapshot = snapshotDAO.savePreliminarySnapshot(config, Arrays.asList(item1));
		assertEquals(7.7, ((VDouble)newSnapshot.getSnapshotItems().get(0).getValue()).getValue().doubleValue(), 0.01);

		Snapshot fullSnapshot = snapshotDAO.getSnapshot(newSnapshot.getId(), true);
		assertNull(fullSnapshot);

		List<Snapshot> snapshots = snapshotDAO.getSnapshots(config.getId());
		assertTrue(snapshots.isEmpty());

		snapshotDAO.commitSnapshot(newSnapshot.getId(), "snapshot name", "user", "comment");

		fullSnapshot = snapshotDAO.getSnapshot(newSnapshot.getId(), true);
		assertEquals(1, fullSnapshot.getSnapshotItems().size());

		snapshots = snapshotDAO.getSnapshots(config.getId());
		assertEquals(1, snapshots.size());

		snapshotDAO.deleteSnapshot(newSnapshot.getId());

		snapshots = snapshotDAO.getSnapshots(config.getId());
		assertTrue(snapshots.isEmpty());
		
		item1 = SnapshotItem.builder()
				.configPvId(config.getId())
				.fetchStatus(false)
				.build();
		
		newSnapshot = snapshotDAO.savePreliminarySnapshot(config, Arrays.asList(item1));
		
		snapshotDAO.commitSnapshot(newSnapshot.getId(), "snapshot name", "user", "comment");

	}
	
	@Test(expected = SnapshotNotFoundException.class)
	public void testCommitNonExistingSnapshot() {
		snapshotDAO.commitSnapshot(-1, "", "", "");
	}

	@Test(expected = NodeNotFoundException.class)
	@FlywayTest(invokeCleanDB = true)
	public void testGetSnapshotsNoSnapshots() {

		snapshotDAO.getSnapshots(-1);
	}

	@Test(expected = NodeNotFoundException.class)
	@FlywayTest(invokeCleanDB = true)
	public void testMoveNodeIllegalSource() {

		Folder folderFromClient = Folder.builder().name("SomeFolder").parentId(Node.ROOT_NODE_ID)
				.build();

		Folder folder1 = configDAO.createFolder(folderFromClient);

		configDAO.moveNode(-1, folder1.getId(), "username");
	}
	
	
	@Test(expected = IllegalArgumentException.class)
	@FlywayTest(invokeCleanDB = true)
	public void testMoveNodeIllegalTarget() {

		Folder folderFromClient = Folder.builder().name("SomeFolder").parentId(Node.ROOT_NODE_ID)
				.build();

		Folder folder1 = configDAO.createFolder(folderFromClient);

		configDAO.moveNode(folder1.getId(), -1, "username");
	}

	@Test(expected = IllegalArgumentException.class)
	@FlywayTest(invokeCleanDB = true)
	public void testMoveNodeNameClash1() {

		Folder root = configDAO.getFolder(Node.ROOT_NODE_ID);

		Folder folder1 = Folder.builder().name("SomeFolder").parentId(Node.ROOT_NODE_ID).build();

		folder1 = configDAO.createFolder(folder1);

		Folder folder2 = Folder.builder().name("SomeFolder").parentId(folder1.getId()).build();

		folder2 = configDAO.createFolder(folder2);

		configDAO.createConfiguration(Config.builder().name("Config").description("Desc").parentId(folder2.getId()).build());

		configDAO.moveNode(folder2.getId(), root.getId(), "username");
	}

	@Test(expected = IllegalArgumentException.class)
	@FlywayTest(invokeCleanDB = true)
	public void testMoveNodeNameClash2() {

		Folder root = configDAO.getFolder(Node.ROOT_NODE_ID);

		configDAO.createConfiguration(Config.builder().name("Config").description("Desc").parentId(Node.ROOT_NODE_ID).build());

		Folder folder1 = configDAO.createFolder(Folder.builder().name("SomeFolder").parentId(Node.ROOT_NODE_ID).build());

		Config config2 = configDAO
				.createConfiguration(Config.builder().name("Config").description("Desc").parentId(folder1.getId()).build());

		configDAO.moveNode(config2.getId(), root.getId(), "username");

	}

	@Test
	@FlywayTest(invokeCleanDB = true)
	public void testMoveNode()  throws Exception{

		Folder root = configDAO.getFolder(Node.ROOT_NODE_ID);

		Folder folder1 = Folder.builder().name("SomeFolder").parentId(Node.ROOT_NODE_ID).build();

		folder1 = configDAO.createFolder(folder1);

		root = configDAO.getFolder(Node.ROOT_NODE_ID);

		// Root node has one child node
		assertEquals(1, root.getChildNodes().size());

		Folder folder2 = Folder.builder().name("SomeFolder2").parentId(folder1.getId()).userName("dummy").build();

		folder2 = configDAO.createFolder(folder2);

		folder1 = configDAO.getFolder(folder1.getId());

		// SomeFolder has one child node
		assertEquals(1, folder1.getChildNodes().size());

		configDAO.createConfiguration(Config.builder().name("Config").description("Desc").parentId(folder2.getId()).build());

		Date lastModifiedOfSource = folder1.getLastModified();
		Date lastModifiedOfTarget = root.getLastModified();
		
		Date lastModified = folder2.getLastModified();

		Thread.sleep(100);
		
		folder2 = configDAO.moveNode(folder2.getId(), Node.ROOT_NODE_ID, "username");
		
		assertNotEquals(lastModified, folder2.getLastModified());

		root = configDAO.getFolder(Node.ROOT_NODE_ID);
		folder1 = configDAO.getFolder(folder1.getId());

		// After move the target's last_modified should have been updated
		assertTrue(root.getLastModified().getTime() > lastModifiedOfTarget.getTime());

		// After move the source's last_modified should have been updated
		assertTrue(folder1.getLastModified().getTime() > lastModifiedOfSource.getTime());
		
		// After the move, the target's username should have been updated
		assertEquals("username", folder1.getUserName());

		root = configDAO.getFolder(Node.ROOT_NODE_ID);

		// After move root node has two child nodes
		assertEquals(2, root.getChildNodes().size());

		folder1 = configDAO.getFolder(folder1.getId());
		// After mode SomeFolder has no child nodes
		assertTrue(folder1.getChildNodes().isEmpty());

	}

	@Test
	@FlywayTest(invokeCleanDB = true)
	public void testUpdateConfig() throws Exception{

		Folder folder1 = configDAO.createFolder(Folder.builder().name("SomeFolder").parentId(Node.ROOT_NODE_ID).build());

		ConfigPv configPv1 = ConfigPv.builder().pvName("configPv1").build();
		ConfigPv configPv2 = ConfigPv.builder().pvName("configPv2").build();

		Config config = Config.builder().name("My config").parentId(folder1.getId()).description("description")
				.name("name")
				.configPvList(Arrays.asList(configPv1, configPv2)).build();

		config = configDAO.createConfiguration(config);
		
		Date lastModified = config.getLastModified();
		
		SnapshotItem item1 = SnapshotItem.builder()
				.configPvId(config.getId())
				.fetchStatus(true)
				.value(VDouble.of(7.7, alarm, time, display))
				.build();
		
		SnapshotItem item2 = SnapshotItem.builder()
				.configPvId(config.getId())
				.fetchStatus(true)
				.value(VInt.of(7, alarm, time, display))
				.build();

		Snapshot snapshot = snapshotDAO.savePreliminarySnapshot(config, Arrays.asList(item1, item2));

		assertEquals(7.7, ((VDouble)snapshot.getSnapshotItems().get(0).getValue()).getValue().doubleValue(), 0.01);
		assertEquals(7, ((VInt)snapshot.getSnapshotItems().get(1).getValue()).getValue().intValue());

		Snapshot fullSnapshot = snapshotDAO.getSnapshot(snapshot.getId(), true);

		assertNull(fullSnapshot);

		List<Snapshot> snapshots = snapshotDAO.getSnapshots(config.getId());

		assertTrue(snapshots.isEmpty());

		snapshotDAO.commitSnapshot(snapshot.getId(), "snapshot name", "user", "comment");

		fullSnapshot = snapshotDAO.getSnapshot(snapshot.getId(), false);

		assertNotNull(fullSnapshot);
		assertEquals(2, fullSnapshot.getSnapshotItems().size());

		Config updatedConfig = Config.builder().id(config.getId()).name("My updated config")
				.parentId(folder1.getId()).description("Updated description")
				.configPvList(Arrays.asList(configPv1)).build();
		
		lastModified = config.getLastModified();
		
		Thread.sleep(100);
		updatedConfig = configDAO.updateConfiguration(updatedConfig);
		
		assertNotEquals(lastModified, updatedConfig.getLastModified());

		assertEquals("My updated config", updatedConfig.getName());
		
		// Verify that last modified time has been updated
		assertTrue(updatedConfig.getLastModified().getTime() > lastModified.getTime());
		
		// Verify the list of PVs
		assertEquals(1, updatedConfig.getConfigPvList().size());
		
		ConfigPv configPv3 = ConfigPv.builder().pvName("configPv3").build();
		
		updatedConfig = Config.builder().id(config.getId()).name("My updated config")
				.parentId(folder1.getId()).description("Updated description")
				.configPvList(Arrays.asList(configPv1, configPv2, configPv3)).build();
		
		updatedConfig = configDAO.updateConfiguration(updatedConfig);
		
		// Verify the list of PVs
		assertEquals(3, updatedConfig.getConfigPvList().size());

	}
	
	@Test(expected = NodeNotFoundException.class)
	public void testUpdateNonExistinConfiguration() {
		
		configDAO.updateConfiguration(Config.builder().id(-1).build());
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testUpdateNodethatIsNotConfiguration() {
		
		configDAO.updateConfiguration(Config.builder().id(Node.ROOT_NODE_ID).build());
	}

	@Test(expected = IllegalArgumentException.class)
	@FlywayTest(invokeCleanDB = true)
	public void testDeleteRootNode() {

		// Try to delete root folder (id = 0)
		configDAO.deleteNode(Node.ROOT_NODE_ID);

	}

	@Test(expected = IllegalArgumentException.class)
	public void testGetConfigThatIsNotAConfig() {

		configDAO.getConfiguration(Node.ROOT_NODE_ID);

	}
	
	@Test
	public void testNonExistingFolder() {

		assertNull(configDAO.getFolder(-1));

	}

	@Test(expected = IllegalArgumentException.class)
	@FlywayTest(invokeCleanDB = true)
	public void testGetFolderThatIsNotAFolder() {

		Folder folder1 = configDAO.createFolder(Folder.builder().name("SomeFolder").parentId(Node.ROOT_NODE_ID).build());

		ConfigPv configPv1 = ConfigPv.builder().pvName("configPv1").build();

		Config config = Config.builder().name("My config").parentId(folder1.getId()).description("description")
				.configPvList(Arrays.asList(configPv1)).build();

		config = configDAO.createConfiguration(config);

		configDAO.getFolder(config.getId());

	}

	@Test
	public void testNameClash() {

		Node n1 = Folder.builder().name("n1").build();
		Node n2 = Folder.builder().name("n1").build();
		Node n3 = Folder.builder().name("n3").build();

		ConfigJdbcDAO jdbcDAO = (ConfigJdbcDAO) configDAO;

		assertTrue(jdbcDAO.doesNameClash(n1, Arrays.asList(n2, n3)));
		assertFalse(jdbcDAO.doesNameClash(n1, Arrays.asList(n3)));

		Node n4 = Config.builder().name("n1").build();

		assertFalse(jdbcDAO.doesNameClash(n1, Arrays.asList(n4)));
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testRenameRootFolder() {
		configDAO.renameNode(Node.ROOT_NODE_ID, null, "username");
	}
	
	@Test(expected = IllegalArgumentException.class)
	@FlywayTest(invokeCleanDB = true)
	public void testRenameFolderNameAlreadyExists() {
			
		configDAO.createFolder(Folder.builder().name("Folder1").parentId(Node.ROOT_NODE_ID).build());
		
		Folder folder2 = configDAO.createFolder(Folder.builder().name("Folder2").parentId(Node.ROOT_NODE_ID).build());
		
		configDAO.renameNode(folder2.getId(), "Folder1", "username");
	}
	
	@Test(expected = IllegalArgumentException.class)
	@FlywayTest(invokeCleanDB = true)
	public void testRenameConfigNameAlreadyExists() {
			
		configDAO.createConfiguration(Config.builder().description("description")
				.name("Config1").parentId(Node.ROOT_NODE_ID).build());
		
		Config config2 = configDAO.createConfiguration(Config.builder().
				description("description").name("Config2").parentId(Node.ROOT_NODE_ID).build());
		
		configDAO.renameNode(config2.getId(), "Config1", "username");
	}
	
	@Test
	@FlywayTest(invokeCleanDB = true)
	public void testRenameFolder(){
			
		Folder folder1 = configDAO.createFolder(Folder.builder()
				.userName("dummy")
				.name("Folder1").parentId(Node.ROOT_NODE_ID).build());
		
		Date lastModified = folder1.getLastModified();
		
		configDAO.createConfiguration(Config.builder().description("whatever")
				.name("Config1").parentId(Node.ROOT_NODE_ID).build());
		
		configDAO.renameNode(folder1.getId(), "NewName", "username");
		
		folder1 = configDAO.getFolder(folder1.getId());
		
		assertTrue(folder1.getLastModified().getTime() > lastModified.getTime());
		assertEquals("username", folder1.getUserName());
		
	}
	
	@Test
	@FlywayTest(invokeCleanDB = true)
	public void testCreatePriliminarySnapshot() {
		Config config = configDAO.createConfiguration(Config.builder()
				.description("description")
				.name("Config1")
				.parentId(Node.ROOT_NODE_ID)
				.configPvList(Arrays.asList(ConfigPv.builder()
						.pvName("pvName")
						.provider(Provider.ca)
						.build()))
				.build());
		
		Snapshot snapshot = snapshotDAO.createPreliminarySnapshot(config.getId());
		
		assertEquals(config.getId(), snapshot.getConfigId());
		assertTrue(snapshot.getId() != 0);
	}
}
