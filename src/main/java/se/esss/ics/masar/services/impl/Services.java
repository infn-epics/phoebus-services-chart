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
import se.esss.ics.masar.model.Config;
import se.esss.ics.masar.model.Folder;
import se.esss.ics.masar.model.Node;
import se.esss.ics.masar.model.Snapshot;
import se.esss.ics.masar.model.SnapshotItem;
import se.esss.ics.masar.persistence.dao.ConfigDAO;
import se.esss.ics.masar.persistence.dao.SnapshotDAO;
import se.esss.ics.masar.services.IServices;
import se.esss.ics.masar.services.exception.NodeNotFoundException;
import se.esss.ics.masar.services.exception.SnapshotNotFoundException;


public class Services implements IServices{

	@Autowired
	private ConfigDAO configDAO;
	
	
	@Autowired
	private SnapshotDAO snapshotDAO;
	
	@Autowired
	private IEpicsService epicsService;
	
	private Logger logger = LoggerFactory.getLogger(Services.class);
	
		
	@Override
	@Transactional
	public Config createNewConfiguration(Config config) {
		
		Folder parentFolder = configDAO.getFolder(config.getParentId());
		if(parentFolder == null) {
			String message = "Parent folder for configuration does not exist";
			logger.error(message);
			throw new IllegalArgumentException(message);
		}
		Config newConfig = configDAO.createConfiguration(config);
		logger.info(String.format("Created new configuration: %s", newConfig.toString()));
		return newConfig;
	}
	
	@Override
	public Config getConfiguration(int nodeId) {
		
		Config config = configDAO.getConfiguration(nodeId);
		if(config == null) {
			String message = String.format("Configuration with id=%d not found", nodeId);
			logger.error(message);
			throw new NodeNotFoundException(message);
		}
		return config;
	}
	
	@Override
	@Transactional
	public Snapshot takeSnapshot(int nodeId) {
		
		Config config = configDAO.getConfiguration(nodeId);
		
		if(config == null) {
			String message = String.format("Snapshot with id=%d not found", nodeId);
			logger.error(message);
			throw new IllegalArgumentException(message);
		}
		
		long start = System.currentTimeMillis();
		List<SnapshotItem> snapshotItems = epicsService.readPvs(config);
		
		Snapshot snapshot = snapshotDAO.savePreliminarySnapshot(config, snapshotItems);
		logger.info(String.format("Took new preliminary snapshot: %s, time elapsed: %d ms", snapshot.toString(), (System.currentTimeMillis() - start)));
		return snapshot;
	
	}
		
	@Override
	public Snapshot commitSnapshot(int snapshotId, String snapshotName, String userName, String comment) {
		snapshotDAO.commitSnapshot(snapshotId, snapshotName, userName, comment);
		
		Snapshot snapshot = snapshotDAO.getSnapshot(snapshotId, false);
		logger.info(String.format("Committed snapshot: %s, id=%d", snapshotName, snapshotId));
		return snapshot;
	}
	
	@Override
	public void deleteSnapshot(int snapshotId) {
		logger.info(String.format("Deleting snapshot id=%d", snapshotId));
		snapshotDAO.deleteSnapshot(snapshotId);
	}
	
	@Override
	public List<Snapshot> getSnapshots(int configId){
		logger.info(String.format("Obtaining snapshot for config id=%d", configId));
		return snapshotDAO.getSnapshots(configId);
	}
	
	@Override
	public Snapshot getSnapshot(int snapshotId){
		Snapshot snapshot = snapshotDAO.getSnapshot(snapshotId, true);
		if(snapshot == null) {
			String message = String.format("Snapshot with id=%d not found", snapshotId);
			logger.error(message);
			throw new SnapshotNotFoundException(message);
		}
		logger.info(String.format("Retrieved snapshot id=%d", snapshotId));
		return snapshot;
	}
	
	@Override
	public Folder createFolder(Folder folder) {
		
		Folder parentFolder = configDAO.getFolder(folder.getParentId());
		if(parentFolder == null) {
			String message = String.format("Cannot create new folder as parent folder with id=%d does not exist.", folder.getParentId());
			logger.error(message);
			throw new IllegalArgumentException(message);
		}
	
		Folder newFolder = configDAO.createFolder(folder);
		logger.info(String.format("Created new folder: %s", folder.toString()));
		return newFolder;
	}
	
	@Override
	public Folder getFolder(int nodeId) {
		Folder folder = configDAO.getFolder(nodeId);
		if(folder == null) {
			String message = String.format("Folder with id=%d does not exist", nodeId);
			logger.error(message);
			throw new NodeNotFoundException(message);
		}
		logger.info(String.format("Retrieved folder id=%d", nodeId));
		return folder;
	}
	
	
	@Override
	@Transactional
	public Folder moveNode(int nodeId, int targetNodeId, String userName) {
		logger.info(String.format("Moving node id %d to raget node id%d", nodeId, targetNodeId));
		return configDAO.moveNode(nodeId, targetNodeId, userName);
	}
	
	@Override
	@Transactional
	public void deleteNode(int nodeId) {
		logger.info(String.format("Deleting node id=%d", nodeId));
		configDAO.deleteNode(nodeId);
	}
	
	@Override
	@Transactional
	public Config updateConfiguration(Config config) {
		logger.info(String.format("Updating configuration id: %d", config.getId()));
		return configDAO.updateConfiguration(config);
	}
	
	@Override
	public Node renameNode(int nodeId, String name, String userName) {
		logger.info(String.format("Renaming node id: %d to %s", nodeId, name));
		return configDAO.renameNode(nodeId, name, userName);
	}
}
