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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyBoolean;
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
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import se.esss.ics.masar.model.ConfigPv;
import se.esss.ics.masar.model.Node;
import se.esss.ics.masar.model.NodeType;
import se.esss.ics.masar.model.SnapshotItem;
import se.esss.ics.masar.persistence.dao.NodeDAO;
import se.esss.ics.masar.services.IServices;
import se.esss.ics.masar.services.config.ServicesTestConfig;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextHierarchy({ @ContextConfiguration(classes = { ServicesTestConfig.class}) })
public class ServicesTest {
	
	@Autowired
	private IServices services;
	
	@Autowired
	private NodeDAO nodeDAO;
	
	private Node configFromClient;
	
	private Node config1;
	
	private Node configWithParent;
	
	List<ConfigPv> configPvList;
	
	
	@Before
	public void setUp() throws Exception{
		
		ConfigPv configPv = ConfigPv.builder()
				.pvName("pvName")
				.build();
		
		configFromClient = Node.builder()
				.nodeType(NodeType.CONFIGURATION)
				.build();
		
		configFromClient.setId(1);
		configFromClient.setCreated(new Date());
		
		
		config1 = Node.builder()
				.nodeType(NodeType.CONFIGURATION)
				.build();
		
		config1.setId(1);
		
		configWithParent = Node.builder()
				.nodeType(NodeType.CONFIGURATION)
				.build();
		
		configPvList = Arrays.asList(configPv);
		
		
		when(nodeDAO.createNode("a", configFromClient)).thenReturn(configFromClient);
		when(nodeDAO.createNode("a", configWithParent)).thenReturn(configWithParent);
	
	}
	
	
	@Test(expected = IllegalArgumentException.class)
	public void testCreateConfigurationNoParent() {
		services.createNode("x", configFromClient);
	}
	
	@Test
	public void testCreateConfiguration() {
		when(nodeDAO.getNode("a")).thenReturn(Node.builder().id(1).uniqueId("a").build());
		services.createNode("a", configWithParent);
	}
	
	@Test
	public void testGetConfigNotNull() {
		
		when(nodeDAO.getNode("a")).thenReturn(configFromClient);
		
		Node config = services.getNode("a");
		assertEquals(1, config.getId());
	}

	
	@Test
	public void testTakeSnapshot() {
		when(nodeDAO.getNode("a")).thenReturn(configFromClient);
		when(nodeDAO.savePreliminarySnapshot(configFromClient.getUniqueId(), Collections.emptyList()))
			.thenReturn(Node.builder().nodeType(NodeType.SNAPSHOT).id(777).build());
		services.takeSnapshot("a");
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testTakeSnapshotConfigNotFound() {
		services.takeSnapshot("x");
	}
	
	@Test
	public void testCommitSnapshot() {
		
		when(nodeDAO.getSnapshot("a", false)).thenReturn(Node.builder().nodeType(NodeType.SNAPSHOT).id(777).build());
		
		services.commitSnapshot("a", "snapshot name", "comment", "user");
		
		verify(nodeDAO, atLeast(1)).getSnapshot(anyString(), anyBoolean());
		verify(nodeDAO, times(1)).commitSnapshot(anyString(), anyString(), anyString(), anyString());
		
		reset(nodeDAO);
	}
	
	@Test
	public void testGetSnapshots() {
		
		services.getSnapshots(anyString());
		
		verify(nodeDAO, times(1)).getSnapshots(anyString());
		
		reset(nodeDAO);
	}
	
	@Test
	public void testGetSnapshotNotFound() {
		
		when(nodeDAO.getSnapshot("s", false)).thenReturn(null);
		
		try {
			services.getSnapshot("s");
			fail("Exception expected here");
		} catch (Exception e) {
			
		}
	
		reset(nodeDAO);
	}
	
	@Test
	public void testGetSnapshot() {
		
		when(nodeDAO.getSnapshot("s", true)).thenReturn(mock(Node.class));
		
		Node snapshot = services.getSnapshot("s");
		
		assertNotNull(snapshot);
	
		reset(nodeDAO);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void createNewFolderNoParentSpecified() {
		
		Node folderFromClient = Node.builder().name("SomeFolder").build();
		
		services.createNode(null, folderFromClient);
	}
	
	@Test
	public void testCreateNewFolder() {
		
		Node folderFromClient = Node.builder().name("SomeFolder").build();
		
		when(nodeDAO.getNode("p")).thenReturn(Node.builder().build());
		when(nodeDAO.createNode("p", folderFromClient)).thenReturn(folderFromClient);
		
		services.createNode("p", folderFromClient);
		
		verify(nodeDAO, atLeast(1)).createNode("p", folderFromClient);
		
		reset(nodeDAO);
	}
	
	@Test
	public void testGetFolder() {
			
		when(nodeDAO.getNode("a")).thenReturn(Node.builder().id(77).uniqueId("a").build());
		assertNotNull(services.getNode("a"));

	}
	
	
	@Test
	public void testGetNonExsitingFolder() {
			
		when(nodeDAO.getNode("a")).thenReturn(null);
		assertNull(services.getNode("a"));
		
		reset(nodeDAO);
	}
	
	@Test
	public void testDeleteConfiguration() {
			
		services.deleteNode("a");
		
		verify(nodeDAO, atLeast(1)).deleteNode("a");
		
		reset(nodeDAO);
	}
	
	@Test
	public void testDeleteFolder() {
			
		services.deleteNode("a");
		
		verify(nodeDAO, atLeast(1)).deleteNode("a");
		
		reset(nodeDAO);
	}
	
	@Test
	public void testMoveNode() {
			
		services.moveNode("a", "b", "username");
		
		verify(nodeDAO, atLeast(1)).moveNode("a", "b", "username");
		
		reset(nodeDAO);
	}
	
	@Test
	public void testUpdateNode() {
		Node node = Node.builder().build();
		services.updateNode(node);
		
		verify(nodeDAO, atLeast(1)).updateNode(node);
		
		reset(nodeDAO);
	}
	
	@Test 
	public void testTimestamp() {
		long timeNanoSeconds = 1538037556314456383L;
		
		System.out.println(new Date(timeNanoSeconds / 1000000));
	}
	
	@Test
	public void testUpdateConfiguration() {
		
		when(nodeDAO.updateConfiguration(config1, configPvList)).thenReturn(config1);
		
		assertNotNull(services.updateConfiguration(config1, configPvList));
	}
	
	@Test
	public void testGetSnapshotItems() {
		when(nodeDAO.getSnapshotItems("a")).thenReturn(Collections.emptyList());
		
		assertNotNull(services.getSnapshotItems("a"));
	}
	
}
