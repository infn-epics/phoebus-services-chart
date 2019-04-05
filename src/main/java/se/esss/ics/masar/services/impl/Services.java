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

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import se.esss.ics.masar.epics.IEpicsService;
import se.esss.ics.masar.model.ConfigPv;
import se.esss.ics.masar.model.Node;
import se.esss.ics.masar.model.NodeType;
import se.esss.ics.masar.model.SnapshotItem;
import se.esss.ics.masar.persistence.dao.NodeDAO;
import se.esss.ics.masar.services.IServices;
import se.esss.ics.masar.services.exception.NodeNotFoundException;
import se.esss.ics.masar.services.exception.SnapshotNotFoundException;

public class Services implements IServices {

	@Autowired
	private NodeDAO nodeDAO;

	@Autowired
	private IEpicsService epicsService;

	private Logger logger = LoggerFactory.getLogger(Services.class);
	
	@Override
	public Node getParentNode(String uniqueNodeId) {
		return nodeDAO.getParentNode(uniqueNodeId);
	}
	
	@Override
	@Transactional
	public Node takeSnapshot(String configUniqueId) {

		Node config = nodeDAO.getNode(configUniqueId);

		if (config == null) {
			String message = String.format("Snapshot with id=%s not found", configUniqueId);
			logger.error(message);
			throw new IllegalArgumentException(message);
		}
		
		logger.info(String.format("Reading PVs for configuration id=%d", config.getId()));

		List<ConfigPv> configPvs = nodeDAO.getConfigPvs(configUniqueId);
		
		long start = System.currentTimeMillis();
		List<SnapshotItem> snapshotItems = epicsService.readPvs(configPvs);

		Node snapshot = nodeDAO.savePreliminarySnapshot(config.getUniqueId(), snapshotItems);
		logger.info(String.format("Took new preliminary snapshot: %s, time elapsed: %d ms", snapshot.toString(),
				(System.currentTimeMillis() - start)));
		return snapshot;
	}

	@Override
	public void commitSnapshot(String snapshotUniqueId, String snapshotName, String userName, String comment) {
		
		Node snapshot = nodeDAO.getSnapshot(snapshotUniqueId, false);
		if(snapshot == null) {
			String message = String.format("Snapshot with id=%s not found", snapshotUniqueId);
			logger.info(message);
			throw new NodeNotFoundException(message);
		}
		nodeDAO.commitSnapshot(snapshotUniqueId, snapshotName, userName, comment);

		logger.info(String.format("Committed snapshot: %s, id=%d", snapshotName, snapshot.getId()));
	}

	@Override
	public List<Node> getSnapshots(String configUniqueId) {
		logger.info(String.format("Obtaining snapshot for config id=%s", configUniqueId));
		return nodeDAO.getSnapshots(configUniqueId);
	}

	@Override
	public Node getSnapshot(String snapshotUniqueId) {
		Node snapshot = nodeDAO.getSnapshot(snapshotUniqueId, true);
		if (snapshot == null) {
			String message = String.format("Snapshot with id=%s not found or is not committed", snapshotUniqueId);
			logger.error(message);
			throw new SnapshotNotFoundException(message);
		}
		logger.info(String.format("Retrieved snapshot id=%s", snapshotUniqueId));
		return snapshot;
	}

	@Override
	public Node createNode(String parentsUniqueId, Node node) {

		Node parentFolder = nodeDAO.getNode(parentsUniqueId);
		if (parentFolder == null || !parentFolder.getNodeType().equals(NodeType.FOLDER)) {
			String message = String.format("Cannot create new folder as parent folder with id=%s does not exist.",
					parentsUniqueId);
			logger.error(message);
			throw new IllegalArgumentException(message);
		}

		node = nodeDAO.createNode(parentsUniqueId, node);
		logger.info(String.format("Created new node: %s", node.toString()));
		return node;
	}

	@Override
	@Transactional
	public Node moveNode(String nodeId, String targetNodeId, String userName) {
		logger.info(String.format("Moving node id %s to raget node id %s", nodeId, targetNodeId));
		return nodeDAO.moveNode(nodeId, targetNodeId, userName);
	}

	@Override
	@Transactional
	public void deleteNode(String nodeId) {
		logger.info(String.format("Deleting node id=%s", nodeId));
		nodeDAO.deleteNode(nodeId);
	}

	@Override
	@Transactional
	public Node updateConfiguration(Node configToUpdate, List<ConfigPv> configPvs) {
		logger.info(String.format("Updating configuration unique id: %s", configToUpdate.getUniqueId()));
		return nodeDAO.updateConfiguration(configToUpdate, configPvs);
	}

	@Override
	public Node updateNode(Node nodeToUpdate) {
		logger.info(String.format("Updating node unique id: %s", nodeToUpdate.getUniqueId()));
		return nodeDAO.updateNode(nodeToUpdate);
	}

	@Override
	public Node getNode(String nodeId) {
		logger.info(String.format("Getting node %s", nodeId));
		return nodeDAO.getNode(nodeId);
	}

	@Override
	public List<Node> getChildNodes(String nodeUniqueId){
		logger.info(String.format("Getting child nodes for node unique id=%s", nodeUniqueId));
		return nodeDAO.getChildNodes(nodeUniqueId);
	}
	
	@Override
	public Node tagSnapshotAsGolden(String snapshotUniqueId, boolean isGolden) {
		logger.info(String.format("Tagging snapshot %s as golden.", snapshotUniqueId));
		return nodeDAO.tagAsGolden(snapshotUniqueId, isGolden);
	}
	
	@Override
	public Node getRootNode() {
		return nodeDAO.getRootNode();
	}
	
	@Override
	public List<ConfigPv> getConfigPvs(String configUniqueId){
		return nodeDAO.getConfigPvs(configUniqueId);
	}
	
	@Override
	public List<SnapshotItem> getSnapshotItems(String snapshotUniqueId){
		return nodeDAO.getSnapshotItems(snapshotUniqueId);
	}
	
	@Override
	public ConfigPv updateSingleConfigPv(String currentPvName, String newPvName, String currentReadbackPvName, String newReadbackPvName) {
		return nodeDAO.updateSingleConfigPv(currentPvName, newPvName, currentReadbackPvName, newReadbackPvName);
	}
	
	@Override
	public Node saveSnapshot(String configUniqueId, List<SnapshotItem> snapshotItems, String snapshotName, String userName, String comment) {
		return nodeDAO.saveSnapshot(configUniqueId, snapshotItems, snapshotName, comment, userName);
	}
}
