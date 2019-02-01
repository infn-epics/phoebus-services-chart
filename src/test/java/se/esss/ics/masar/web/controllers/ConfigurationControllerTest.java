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

package se.esss.ics.masar.web.controllers;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import org.epics.vtype.Alarm;
import org.epics.vtype.AlarmSeverity;
import org.epics.vtype.AlarmStatus;
import org.epics.vtype.Display;
import org.epics.vtype.Time;
import org.epics.vtype.VDouble;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import se.esss.ics.masar.application.swagger.SwaggerConfig;
import se.esss.ics.masar.model.Config;
import se.esss.ics.masar.model.ConfigPv;
import se.esss.ics.masar.model.Folder;
import se.esss.ics.masar.model.Node;
import se.esss.ics.masar.model.Snapshot;
import se.esss.ics.masar.model.SnapshotItem;
import se.esss.ics.masar.services.IServices;
import se.esss.ics.masar.services.exception.NodeNotFoundException;
import se.esss.ics.masar.web.config.ControllersTestConfig;

@RunWith(SpringRunner.class)
@ContextHierarchy({ @ContextConfiguration(classes = { ControllersTestConfig.class, SwaggerConfig.class }) })
@WebMvcTest(ConfigurationController.class)

/**
 * Main purpose of the tests in this class is to verify that REST end points are
 * maintained, i.e. that URLs are not changed and that they return the correct
 * data.
 * 
 * @author Georg Weiss, European Spallation Source
 *
 */
public class ConfigurationControllerTest {

	@Autowired
	private IServices services;

	@Autowired
	private MockMvc mockMvc;

	private Node nodeFromClient;

	private Config configFromClient;

	private Folder folderFromClient;

	private Config config1;

	private Snapshot snapshot;

	private ObjectMapper objectMapper = new ObjectMapper();

	private static final String JSON = "application/json;charset=UTF-8";

	@Before
	public void setUp() {
		
		Time time = Time.of(Instant.ofEpochSecond(1000, 7000));
		Alarm alarm = Alarm.of(AlarmSeverity.NONE, AlarmStatus.NONE, "name");
		Display display = Display.none();

		ConfigPv configPv = ConfigPv.builder().pvName("pvName")
				.build();

		configFromClient = Config.builder().id(10).configPvList(Arrays.asList(configPv)).userName("myusername")
				.description("description").name("config name").build();

		config1 = Config.builder().configPvList(Arrays.asList(configPv)).description("description").userName("myusername")
				.build();

	
		folderFromClient = Folder.builder().name("SomeFolder").userName("myusername").id(11).parentId(0).build();
	
		SnapshotItem item1 = SnapshotItem.builder()
				.fetchStatus(true)
				.configPvId(config1.getId())
				.value(VDouble.of(7.7, alarm, time, display))
				.build();

		snapshot = Snapshot.builder().comment("comment")
				.name("name")
				.snapshotItems(Arrays.asList(item1))
				.build();

		when(services.createNewConfiguration(configFromClient)).thenReturn(config1);
		when(services.getConfiguration(1)).thenReturn(config1);

		Node parentNode = new Node();
		parentNode.setName("Parent");
		parentNode.setId(1);

		nodeFromClient = new Node();
		nodeFromClient.setId(77);
		nodeFromClient.setName("Folder");
		nodeFromClient.setParentId(parentNode.getId());
	}

	@Test
	public void testCreateFolder() throws Exception {
		
		when(services.createFolder(folderFromClient)).thenReturn(folderFromClient);

		MockHttpServletRequestBuilder request = put("/folder").contentType(JSON)
				.content(objectMapper.writeValueAsString(folderFromClient));

		MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andExpect(content().contentType(JSON))
				.andReturn();

		String s = result.getResponse().getContentAsString();
		// Make sure response contains expected data
		objectMapper.readValue(s, Folder.class);
	}
	
	@Test
	public void testCreateFolderNoUsername() throws Exception {
		
		Folder folder = Folder.builder().name("SomeFolder").id(11).parentId(0).build();

		MockHttpServletRequestBuilder request = put("/folder").contentType(JSON)
				.content(objectMapper.writeValueAsString(folder));

		mockMvc.perform(request).andExpect(status().isBadRequest());
	}
	
	@Test
	public void testCreateFolderParentIdDoesNotExist() throws Exception {
		
		when(services.createFolder(folderFromClient)).thenThrow(new IllegalArgumentException("Parent folder does not exist"));

		MockHttpServletRequestBuilder request = put("/folder").contentType(JSON)
				.content(objectMapper.writeValueAsString(folderFromClient));

		mockMvc.perform(request).andExpect(status().isBadRequest());
	}

	@Test
	public void testCreateConfig() throws Exception {

		MockHttpServletRequestBuilder request = put("/config").contentType(JSON)
				.content(objectMapper.writeValueAsString(configFromClient));

		MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andExpect(content().contentType(JSON))
				.andReturn();

		// Make sure response contains expected data
		objectMapper.readValue(result.getResponse().getContentAsString(), Config.class);
	}
	
	@Test
	public void testCreateConfigNoUsername() throws Exception {
		
		Config config = Config.builder().id(10)
		.description("description").build();

		MockHttpServletRequestBuilder request = put("/config").contentType(JSON)
				.content(objectMapper.writeValueAsString(config));

		mockMvc.perform(request).andExpect(status().isBadRequest());
	}

	@Test
	public void testGetNonExistingConfig() throws Exception {

		when(services.getConfiguration(2)).thenThrow(new NodeNotFoundException("lasdfk"));

		MockHttpServletRequestBuilder request = get("/config/2").contentType(JSON);

		mockMvc.perform(request).andExpect(status().isNotFound());
	}

	@Test
	public void testGetSnapshots() throws Exception {

		when(services.getSnapshots(1)).thenReturn(Arrays.asList(snapshot));

		MockHttpServletRequestBuilder request = get("/config/1/snapshots").contentType(JSON);

		MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andExpect(content().contentType(JSON))
				.andReturn();

		// Make sure response contains expected data
		objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<List<Snapshot>>() {
		});
	}

	@Test
	public void testGetSnapshotsForNonExistingConfig() throws Exception {

		when(services.getSnapshots(2)).thenThrow(new NodeNotFoundException("lasdfk"));

		MockHttpServletRequestBuilder request = get("/config/2/snapshots").contentType(JSON);

		mockMvc.perform(request).andExpect(status().isNotFound());
	}

	@Test
	public void testDeleteConfiguration() throws Exception {
		MockHttpServletRequestBuilder request = delete("/config/1");

		mockMvc.perform(request).andExpect(status().isOk());
	}

	@Test
	public void testDeleteFolder() throws Exception {
		MockHttpServletRequestBuilder request = delete("/folder/1");

		mockMvc.perform(request).andExpect(status().isOk());
		
		doThrow(new IllegalArgumentException()).when(services).deleteNode(0);
			
		request = delete("/folder/0");
		
		mockMvc.perform(request).andExpect(status().isBadRequest());
		
	}

	@Test
	public void testGetFolder() throws Exception {
		when(services.getFolder(1)).thenReturn(Folder.builder().id(1).build());

		MockHttpServletRequestBuilder request = get("/folder/1");

		MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andExpect(content().contentType(JSON))
				.andReturn();

		// Make sure response contains expected data
		objectMapper.readValue(result.getResponse().getContentAsString(), Folder.class);
	}

	@Test
	public void testGetConfiguration() throws Exception {
		when(services.getConfiguration(1)).thenReturn(Config.builder().build());

		MockHttpServletRequestBuilder request = get("/config/1");

		MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andExpect(content().contentType(JSON))
				.andReturn();

		// Make sure response contains expected data
		objectMapper.readValue(result.getResponse().getContentAsString(), Config.class);
	}
	
	@Test
	public void testGetNonExistingConfiguration() throws Exception {
		when(services.getConfiguration(1)).thenThrow(NodeNotFoundException.class);

		MockHttpServletRequestBuilder request = get("/config/1");

		mockMvc.perform(request).andExpect(status().isNotFound());
		
		Mockito.reset(services);
	}
	
	@Test
	public void testGetNonExistingFolder() throws Exception {
		when(services.getFolder(1)).thenThrow(NodeNotFoundException.class);

		MockHttpServletRequestBuilder request = get("/folder/1");

		mockMvc.perform(request).andExpect(status().isNotFound());
		
		when(services.getFolder(7)).thenThrow(IllegalArgumentException.class);
		
		request = get("/folder/7");
		
		mockMvc.perform(request).andExpect(status().isBadRequest());
		
		Mockito.reset(services);
	}

	@Test
	public void testMoveNode() throws Exception {
		when(services.moveNode(1, 2, "username")).thenReturn(Folder.builder().id(2).build());

		MockHttpServletRequestBuilder request = post("/node/1").param("to", "2").param("username", "username");

		MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andExpect(content().contentType(JSON))
				.andReturn();

		// Make sure response contains expected data
		objectMapper.readValue(result.getResponse().getContentAsString(), Folder.class);
	}
	
	@Test
	public void testMoveNodeNoUsername() throws Exception {
		MockHttpServletRequestBuilder request = post("/node/1").param("to", "2");

		mockMvc.perform(request).andExpect(status().isBadRequest());
	}
	
	@Test
	public void testUpdateConfig() throws Exception {
		
		Config config = Config.builder().userName("myusername").id(0).build();
		
		when(services.updateConfiguration(Mockito.any(Config.class))).thenReturn(config);

		MockHttpServletRequestBuilder request = post("/config").contentType(JSON)
				.content(objectMapper.writeValueAsString(config));

		MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andExpect(content().contentType(JSON))
				.andReturn();

		// Make sure response contains expected data
		objectMapper.readValue(result.getResponse().getContentAsString(), Config.class);
	}

	@Test
	public void testGetFolderIllegalArgument() throws Exception {
		when(services.getFolder(666)).thenThrow(IllegalArgumentException.class);

		MockHttpServletRequestBuilder request = get("/folder/666");

		mockMvc.perform(request).andExpect(status().isBadRequest());

	}
	
	@Test
	public void testRenameNodeIllegalArgument1() throws Exception{
		when(services.renameNode(1, "whatever", "username")).thenThrow(IllegalArgumentException.class);
		
		MockHttpServletRequestBuilder request = post("/node/1/rename?name=whatever&username=username");

		mockMvc.perform(request).andExpect(status().isBadRequest());
	}
	
	@Test
	public void testRenameNodeIllegalArgument2() throws Exception{
		
		MockHttpServletRequestBuilder request = post("/node/1/rename?name=whatever");

		mockMvc.perform(request).andExpect(status().isBadRequest());
	}
	
	@Test
	public void testRenameNodeNoRequestParam() throws Exception{
	
		MockHttpServletRequestBuilder request = post("/node/1/rename");

		mockMvc.perform(request).andExpect(status().isBadRequest());
	}
	
	@Test
	public void testRenameNode() throws Exception{
		
		Node node = new Node();
		node.setName("foo");
		
		when(services.renameNode(1, "whatever", "username")).thenReturn(node);
		
		MockHttpServletRequestBuilder request = post("/node/1/rename?name=whatever&username=username");
		
		MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andExpect(content().contentType(JSON))
				.andReturn();

		// Make sure response contains expected data
		objectMapper.readValue(result.getResponse().getContentAsString(), Node.class);
		
	}
}
