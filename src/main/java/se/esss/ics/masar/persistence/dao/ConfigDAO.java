package se.esss.ics.masar.persistence.dao;

import se.esss.ics.masar.model.Config;
import se.esss.ics.masar.model.Folder;
import se.esss.ics.masar.model.Node;

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
public interface ConfigDAO {
	
	public Folder createFolder(Folder node);

	public Config createConfiguration(Config config);

	/**
	 * Retrieves a folder identified by the node id. 
	 * @param nodeId If there is no node corresponding to the node id, an {@link IllegalArgumentException} is thrown.
	 * @return A {@link Folder} object.
	 */
	public Folder getFolder(int nodeId);
	
	/**
	 * Retrieves a configuration identified by the node id. 
	 * @param nodeId If there is no node corresponding to the node id, an {@link IllegalArgumentException} is thrown.
	 * @return A {@link Config} object.
	 */
	public Config getConfiguration(int nodeId);
	

	/**
	 * Deletes a {@link Node}, folder or configuration. If the node is a folder, 
	 * the entire sub-tree of the folder is deleted, including the snapshots 
	 * associated with configurations in the sub-tree.
	 * 
	 * @param nodeId
	 *            The node id node to delete.
	 */
	public void deleteNode(int nodeId);

	public Folder moveNode(int nodeId, int targetNodeId);

	/**
	 * Updates an existing configuration, e.g. changes its name or list of PVs.
	 * @param config The updated configuration data
	 * @return The updated configuration object
	 */
	public Config updateConfiguration(Config config);
	
	/**
	 * Renames an existing node.
	 * @param nodeId The node id of the node subject to change. The root folder's name cannot be changed.
	 * @param name The new name of the node. The name and node type must be unique in the parent folder.
	 * @return The updated {@link Node} object.
	 */
	public Node renameNode(int nodeId, String name);
}
