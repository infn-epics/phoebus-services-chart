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

package se.esss.ics.masar.services.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import se.esss.ics.masar.model.Config;
import se.esss.ics.masar.model.ConfigPv;
import se.esss.ics.masar.model.Folder;
import se.esss.ics.masar.model.Snapshot;
import se.esss.ics.masar.persistence.dao.ConfigDAO;
import se.esss.ics.masar.persistence.dao.SnapshotDAO;
import se.esss.ics.masar.services.IServices;
import se.esss.ics.masar.services.config.ServicesTestConfig;
import se.esss.ics.masar.services.exception.NodeNotFoundException;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextHierarchy({ @ContextConfiguration(classes = { ServicesTestConfig.class}) })
public class ServicesTest {
	
	@Autowired
	private IServices services;
	
	@Autowired
	private ConfigDAO configDAO;
	
	@Autowired
	private SnapshotDAO snapshotDAO;
		
	private Config configFromClient;
	
	private Config config1;
	
	private Config configWithParent;
	
	
	@Before
	public void setUp() throws Exception{
		
		ConfigPv configPv = ConfigPv.builder()
				.pvName("pvName")
				.build();
		
		configFromClient = Config.builder()
				.configPvList(Arrays.asList(configPv))
				.description("description")
				.build();
		
		configFromClient.setId(1);
		configFromClient.setCreated(new Date());
		
		
		config1 = Config.builder()
				.configPvList(Arrays.asList(configPv))
				.description("description")
				.build();
		
		config1.setId(1);
		
		configWithParent = Config.builder()
				.configPvList(Arrays.asList(configPv))
				.description("description")
				.parentId(1)
				.build();
		
		when(configDAO.createConfiguration(configFromClient)).thenReturn(configFromClient);
		when(configDAO.createConfiguration(configWithParent)).thenReturn(configWithParent);
	
	}
	
	
	@Test(expected = IllegalArgumentException.class)
	public void testCreateConfigurationNoParent() {
		services.createNewConfiguration(configFromClient);
	}
	
	@Test
	public void testCreateConfiguration() {
		when(configDAO.getFolder(1)).thenReturn(Folder.builder().id(1).build());
		services.createNewConfiguration(configWithParent);
	}
	
	@Test
	public void testGetConfigNotNull() {
		
		when(configDAO.getConfiguration(1)).thenReturn(configFromClient);
		
		Config config = services.getConfiguration(1);
		assertEquals(1, config.getId());
	}
	
	@Test(expected = NodeNotFoundException.class)
	public void testGetNonExistingConfiguration() {
		
		when(configDAO.getConfiguration(1)).thenReturn(null);
		services.getConfiguration(1);
	}

	
	@Test
	public void testTakeSnapshot() {
		when(configDAO.getConfiguration(1)).thenReturn(configFromClient);
		when(snapshotDAO.savePreliminarySnapshot(configFromClient, Collections.emptyList()))
			.thenReturn(Snapshot.builder().id(777).build());
		services.takeSnapshot(1);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testTakeSnapshotConfigNotFound() {
		
		services.takeSnapshot(2);
	}
	

	@Test
	public void testDeleteSnapshot() {
		
		services.deleteSnapshot(1);
		
		verify(snapshotDAO, times(1)).deleteSnapshot(1);
		
		reset(snapshotDAO);
	}
	
	@Test
	public void testCommitSnapshot() {
		
		when(snapshotDAO.getSnapshot(anyInt(), anyBoolean())).thenReturn(Snapshot.builder().id(777).build());
		
		services.commitSnapshot(anyInt(), anyString(), anyString(), anyString());
		
		verify(snapshotDAO, times(1)).commitSnapshot(anyInt(), anyString(), anyString(), anyString());
		verify(snapshotDAO, atLeast(1)).getSnapshot(anyInt(), anyBoolean());
		
		reset(snapshotDAO);
	}
	
	@Test
	public void testGetSnapshots() {
		
		services.getSnapshots(anyInt());
		
		verify(snapshotDAO, times(1)).getSnapshots(anyInt());
		
		reset(snapshotDAO);
	}
	
	@Test
	public void testGetSnapshotNotFound() {
		
		when(snapshotDAO.getSnapshot(77, false)).thenReturn(null);
		
		try {
			services.getSnapshot(77);
			fail("Exception expected here");
		} catch (Exception e) {
			
		}
	
		reset(snapshotDAO);
	}
	
	@Test
	public void testGetSnapshot() {
		
		when(snapshotDAO.getSnapshot(177, true)).thenReturn(mock(Snapshot.class));
		
		Snapshot snapshot = services.getSnapshot(177);
		
		assertNotNull(snapshot);
	
		reset(snapshotDAO);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void createNewFolderNoParentSpecified() {
		
		Folder folderFromClient = Folder.builder().name("SomeFolder").build();
		
		services.createFolder(folderFromClient);
	}
	
	@Test
	public void testCreateNewFolder() {
		
		Folder folderFromClient = Folder.builder().name("SomeFolder")
				.parentId(0).build();
		
		when(configDAO.getFolder(0)).thenReturn(Folder.builder().parentId(0).build());
		
		services.createFolder(folderFromClient);
		
		verify(configDAO, atLeast(1)).createFolder(folderFromClient);
		
		reset(configDAO);
	}
	
	@Test
	public void testGetFolder() {
			
		when(configDAO.getFolder(77)).thenReturn(Folder.builder().id(77).build());
		assertNotNull(services.getFolder(77));

	}
	
	
	@Test(expected = NodeNotFoundException.class)
	public void testGetNonExsitingFolder() {
			
		when(configDAO.getFolder(77)).thenReturn(null);
		services.getFolder(77);
		
		reset(configDAO);
	}
	
	@Test
	public void testDeleteConfiguration() {
			
		services.deleteNode(1);
		
		verify(configDAO, atLeast(1)).deleteNode(1);
		
		reset(configDAO);
	}
	
	@Test
	public void testDeleteFolder() {
			
		services.deleteNode(1);
		
		verify(configDAO, atLeast(1)).deleteNode(1);
		
		reset(configDAO);
	}
	
	@Test
	public void testMoveNode() {
			
		services.moveNode(1, 2, "username");
		
		verify(configDAO, atLeast(1)).moveNode(1, 2, "username");
		
		reset(configDAO);
	}
	
	@Test
	public void testRenameNode() {
			
		services.renameNode(1, "whatever", "username");
		
		verify(configDAO, atLeast(1)).renameNode(1, "whatever", "username");
		
		reset(configDAO);
	}
	
	@Test 
	public void testTimestamp() {
		long timeNanoSeconds = 1538037556314456383L;
		
		System.out.println(new Date(timeNanoSeconds / 1000000));
	}
	
	@Test
	public void testUpdateConfiguration() {
		when(configDAO.updateConfiguration(config1)).thenReturn(config1);
		
		assertNotNull(services.updateConfiguration(config1));
	}
	
}
