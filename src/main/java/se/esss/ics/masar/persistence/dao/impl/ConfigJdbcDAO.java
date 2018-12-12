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

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.transaction.annotation.Transactional;

import se.esss.ics.masar.model.Config;
import se.esss.ics.masar.model.ConfigPv;
import se.esss.ics.masar.model.Folder;
import se.esss.ics.masar.model.Node;
import se.esss.ics.masar.model.NodeType;
import se.esss.ics.masar.persistence.dao.ConfigDAO;
import se.esss.ics.masar.services.exception.NodeNotFoundException;

public class ConfigJdbcDAO implements ConfigDAO {

	@Autowired
	private SimpleJdbcInsert configurationInsert;

	@Autowired
	private SimpleJdbcInsert configurationEntryInsert;

	@Autowired
	private SimpleJdbcInsert configurationEntryRelationInsert;

	@Autowired
	private SimpleJdbcInsert nodeInsert;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	
	@Transactional
	@Override
	public Folder createFolder(final Folder folder) {

		int newNodeId = newNode(folder);
		return getFolder(newNodeId);
	}

	@Transactional
	@Override
	public Folder getFolder(int nodeId) {
		Node node = getNode(nodeId);
		if(node == null) {
			return null;
		}
		if (node instanceof Folder) {
			return (Folder) node;
		} else {
			throw new IllegalArgumentException(String.format("Node id=%d is not a folder node", nodeId));
		}
	}

	@Transactional
	@Override
	public Config getConfiguration(int nodeId) {

		Node node = getNode(nodeId);
		if(node == null) {
			return null;
		}
		if (node instanceof Config) {
			return (Config) node;
		} else {
			throw new IllegalArgumentException(String.format("Node id=%d is not a configuration node", nodeId));
		}

	}

	private Node getParentNode(int nodeId) {
		
		// Root folder is its own parent
		if(nodeId == Node.ROOT_NODE_ID) {
			Node node = new Node();
			node.setId(Node.ROOT_NODE_ID);
			return node;
		}

		try {
			int parentNodeId = jdbcTemplate.queryForObject(
					"select ancestor from node_closure where descendant=? and depth=1", new Object[] { nodeId },
					Integer.class);
			return getNode(parentNodeId);
		} catch (DataAccessException e) {
			return null;
		}
	}

	private int newNode(final Node node) {
		
		Node parentNode = getNode(node.getParentId());
		
		if (parentNode == null) {
			throw new IllegalArgumentException(String.format("Cannot create new node as parent id=%d does not exist.", node.getId()));
		}
		
		if(!parentNode.getNodeType().equals(NodeType.FOLDER)) {
			throw new IllegalArgumentException("Parent node is not a folder.");
		}

		// The node to be created cannot have same name and type as any of the parent's
		// child nodes

		List<Node> childNodes = getChildNodes(parentNode.getId());
		if (doesNameClash(node, childNodes)) {
			throw new IllegalArgumentException("Node of same name and type already exists in parent node.");
		}
	
		Timestamp now = Timestamp.from(Instant.now());

		Map<String, Object> params = new HashMap<>(2);
		params.put("type", node.getNodeType().toString());
		params.put("created", now);
		params.put("last_modified", now);
		params.put("name", node.getName());

		int newNodeId = nodeInsert.executeAndReturnKey(params).intValue();

		jdbcTemplate.update(
				"insert into node_closure (ancestor, descendant, depth) " + "select t.ancestor, " + newNodeId
						+ ", t.depth + 1  from node_closure as t where t.descendant = ? union all select ?, ?, 0",
				parentNode.getId(), newNodeId, newNodeId);

		// Update the last modified date of the parent folder
		jdbcTemplate.update("update node set last_modified=? where id=?", Timestamp.from(Instant.now()), parentNode.getId());

		return newNodeId;
	}

	/**
	 * Retrieves a {@link Node} associated with the specified node id. If the node exists and is a configuration node,
	 * the returned object is a {@link Config} node, including the list of {@link ConfigPv}s is also present. If on
	 * the other hand the node is a folder, a {@link Folder} object is returned, including the list of child nodes.
	 * @param nodeId The node id.
	 * @return <code>null</code> if the node id does not exist, otherwise either a {@link Folder} or a {@link Config} object.
	 */
	private Node getNode(int nodeId) {

		Node node;
		try {
			node = jdbcTemplate.queryForObject("select * from node where id=?", new Object[] { nodeId },
					new NodeRowMapper());
		} catch (DataAccessException e) {
			return null;
		}

		if (node.getNodeType().equals(NodeType.CONFIGURATION)) {

			Config config = jdbcTemplate.queryForObject(
					"select n.*, c.* from node as n join config as c on n.id=c.node_id where" + " n.id=? and n.type=?",
					new Object[] { nodeId, NodeType.CONFIGURATION.toString() }, new ConfigRowMapper());

			config.setConfigPvList(getConfigPvs(config.getId()));
			Node parentNode = getParentNode(config.getId());
			if(parentNode == null) {
				throw new NodeNotFoundException(String.format("Parent of existing configuration with id=%d not found. THIS SHOULD NOT HAPPEN!", config.getId()));
			}
			config.setParentId(parentNode.getId());
			return config;
		} else {
			Node parentNode = getParentNode(nodeId);
			if(parentNode == null) {
				throw new NodeNotFoundException(String.format("Parent of existing folder with id=%d not found. THIS SHOULD NOT HAPPEN!", nodeId));
			}
			return Folder.builder().created(node.getCreated()).lastModified(node.getLastModified()).id(node.getId())
					.childNodes(getChildNodes(node.getId())).parentId(parentNode.getId()).name(node.getName()).build();
		}
	}

	private List<Node> getChildNodes(int nodeId) {

		return jdbcTemplate.query("select n.* from node as n join node_closure as nc on n.id=nc.descendant where "
				+ "nc.ancestor=? and nc.depth=1", new Object[] { nodeId }, new NodeRowMapper());
	}

	@Override
	@Transactional
	public Config createConfiguration(Config config) {

		int newNodeId = newNode(config);

		Map<String, Object> params = new HashMap<>(4);
		params.put("node_id", newNodeId);
		params.put("description", config.getDescription());
		params.put("last_modified", Timestamp.from(Instant.now()));

		configurationInsert.execute(params);

		if (config.getConfigPvList() != null) {
			for (ConfigPv configPv : config.getConfigPvList()) {
				saveConfigPv(newNodeId, configPv);
			}
		}

		return getConfiguration(newNodeId);
	}

	private void saveConfigPv(int configId, ConfigPv configPv) {

		List<Integer> list = jdbcTemplate.queryForList("select id from config_pv where name=?",
				new Object[] { configPv.getPvName() }, Integer.class);

		int configPvId = 0;

		if (!list.isEmpty()) {
			configPvId = list.get(0);
		} else {
			Map<String, Object> params = new HashMap<>(4);
			params.put("name", configPv.getPvName());
			params.put("provider", configPv.getProvider().toString());

			configPvId = configurationEntryInsert.executeAndReturnKey(params).intValue();
		}

		Map<String, Object> params = new HashMap<>(2);
		params.put("config_id", configId);
		params.put("config_pv_id", configPvId);

		configurationEntryRelationInsert.execute(params);
	}

	private List<ConfigPv> getConfigPvs(int configId) {
		return jdbcTemplate.query("select * from config_pv "
				+ "join config_pv_relation on config_pv.id=config_pv_relation.config_pv_id where config_pv_relation.config_id=?",
				new Object[] { configId }, new ConfigPvRowMapper());
	}

	protected void deleteConfiguration(int nodeId) {

		List<Integer> configPvIds = jdbcTemplate.queryForList(
				"select config_pv_id from config_pv_relation where config_id=?", new Object[] { nodeId },
				Integer.class);

		jdbcTemplate.update("delete from node where id=? and type=?", nodeId, NodeType.CONFIGURATION.toString());

		deleteOrphanedPVs(configPvIds);
	}

	@Override
	public void deleteNode(int nodeId) {

		// Root node may not be deleted
		if (nodeId == Node.ROOT_NODE_ID) {
			throw new IllegalArgumentException("Root node cannot be deleted");
		}
		Node nodeToDelete = getNode(nodeId);
		
		if(nodeToDelete == null) {
			throw new NodeNotFoundException(String.format("Node with id=%d does not exist", nodeId));
		}
		
		int parentNodeId = nodeToDelete.getParentId();
		if (nodeToDelete instanceof Config) {
			deleteConfiguration(nodeId);
		} else {
			Folder folderToDelete = (Folder) nodeToDelete;
			for (Node node : folderToDelete.getChildNodes()) {
				deleteNode(node.getId());
			}
		}
		jdbcTemplate.update("delete from node where id=?", nodeId);

		// Update last modified date of the parent node
		jdbcTemplate.update("update node set last_modified=? where id=?", Timestamp.from(Instant.now()), parentNodeId);
	}

	private void deleteOrphanedPVs(Collection<Integer> pvList) {
		for (Integer pvId : pvList) {
			int count = jdbcTemplate.queryForObject("select count(*) from config_pv_relation where config_pv_id=?",
					new Object[] { pvId }, Integer.class);

			if (count == 0) {
				jdbcTemplate.update("delete from config_pv where id=?", pvId);
			}
		}
	}

	@Override
	@Transactional
	public Folder moveNode(int nodeId, int targetNodeId) {

		Node sourceNode = getNode(nodeId);
		
		if(sourceNode == null) {
			throw new NodeNotFoundException(String.format("Source node with id=%d not found", nodeId));
		}

		int parentNodeId = sourceNode.getParentId();

		Folder targetNode = getFolder(targetNodeId);
		
		if(targetNode == null) {
			throw new IllegalArgumentException(String.format("Traget node with id=%d not found", nodeId));
		}

		if (doesNameClash(sourceNode, targetNode.getChildNodes())) {
			throw new IllegalArgumentException("Node of same name and type already exists in target node.");
		}

		jdbcTemplate.update("delete from node_closure where "
				+ "descendant in (select descendant from node_closure where ancestor=?) "
				+ "and ancestor in (select ancestor from node_closure where descendant=? and ancestor != descendant)",
				nodeId, nodeId);

		jdbcTemplate.update("insert into node_closure (ancestor, descendant, depth) "
				+ "select supertree.ancestor, subtree.descendant, supertree.depth + subtree.depth + 1 AS depth "
				+ "from node_closure as supertree " + "cross join node_closure as subtree "
				+ "where supertree.descendant=? and subtree.ancestor=?", targetNodeId, nodeId);

		// Update the last modified date of the source and target folder.
		jdbcTemplate.update("update node set last_modified=? where id=? or id=?", Timestamp.from(Instant.now()), targetNodeId,
				parentNodeId);

		return getFolder(targetNodeId);
	}

	protected boolean doesNameClash(Node nodeToCheck, List<Node> existingNodes) {
		for (Node node : existingNodes) {
			if (node.getName().equals(nodeToCheck.getName()) && node.getNodeType().equals(nodeToCheck.getNodeType())) {
				return true;
			}
		}
		return false;
	}

	@Override
	@Transactional
	public Config updateConfiguration(Config updatedConfig) {
		
		Node node = getNode(updatedConfig.getId());
		
		if(node == null) {
			throw new NodeNotFoundException(String.format("Config with id=%d not found", updatedConfig.getId()));
		}
		else if(!node.getNodeType().equals(NodeType.CONFIGURATION)) {
			throw new IllegalArgumentException(String.format("Node with id=%d is not a configuration", updatedConfig.getId()));
		}

		Config existingConfig = (Config) node;

		Collection<ConfigPv> pvsToRemove = CollectionUtils.removeAll(existingConfig.getConfigPvList(),
				updatedConfig.getConfigPvList());
		Collection<Integer> pvIdsToRemove = CollectionUtils.collect(pvsToRemove, ConfigPv::getId);
		
		// Remove PVs from relation table
		pvIdsToRemove.stream().forEach(id -> jdbcTemplate.update("delete from config_pv_relation where config_id=? and config_pv_id=?", 
				existingConfig.getId(), id));

		// Check if any of the PVs is orphaned
		deleteOrphanedPVs(pvIdsToRemove);
		
		Collection<ConfigPv> pvsToAdd = 
				CollectionUtils.removeAll(updatedConfig.getConfigPvList(), existingConfig.getConfigPvList());
		
		// Add new PVs 
		pvsToAdd.stream().forEach(configPv -> saveConfigPv(existingConfig.getId(), configPv));

		jdbcTemplate.update("update config set description=? where node_id=?", updatedConfig.getDescription(), updatedConfig.getId());
		jdbcTemplate.update("update node set name=? where id=?", updatedConfig.getName(), updatedConfig.getId());

		return getConfiguration(updatedConfig.getId());
	}

	@Override
	@Transactional
	public Node renameNode(int nodeId, String name) {

		if (nodeId == Node.ROOT_NODE_ID) {
			throw new IllegalArgumentException("Cannot change name of root folder");
		}

		Node nodeToChange = getNode(nodeId);
		if(nodeToChange == null) {
			throw new NodeNotFoundException(String.format("Node with id=%d not found", nodeId));
		}

		Folder parentFolder = getFolder(nodeToChange.getParentId());

		// Create a node object used to check name clash
		Node tmp = new Node();
		tmp.setName(name);
		tmp.setNodeType(nodeToChange.getNodeType());

		if (doesNameClash(tmp, parentFolder.getChildNodes())) {
			throw new IllegalArgumentException(
					"Cannot change name of node as an existing node with same name and type exists.");
		}

		jdbcTemplate.update("update node set name=? where id=?", name, nodeId);

		return getNode(nodeId);
	}
}