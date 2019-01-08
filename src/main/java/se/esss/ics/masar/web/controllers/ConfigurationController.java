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

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.ApiOperation;
import se.esss.ics.masar.model.Config;
import se.esss.ics.masar.model.ConfigPv;
import se.esss.ics.masar.model.Folder;
import se.esss.ics.masar.model.Node;
import se.esss.ics.masar.model.Snapshot;
import se.esss.ics.masar.services.IServices;

@RestController
public class ConfigurationController extends BaseController {

	@Autowired
	private IServices services;

	/**
	 * Create a new folder in the tree structure.
	 * 
	 * A {@link HttpStatus#BAD_REQUEST} is returned if:
	 * <ul>
	 * <li>The parent node does not exist</li>
	 * <li>The parent node is not a {@link Folder}</li>
	 * <li>A folder with the same name already exists in the parent folder</li>
	 * </ul>
	 * 
	 * @param folder
	 *            A {@link Folder} object. The {@link Folder#name} field must be
	 *            non-null, and the {@link Folder#parentId} must specify an existing
	 *            folder.
	 * @return The new folder in the tree.
	 */
	@ApiOperation(value = "Create a new folder", consumes = JSON, produces = JSON)
	@PutMapping("/folder")
	public Folder createFolder(@RequestBody final Folder folder) {
		if(folder.getUserName() == null || folder.getUserName().isEmpty()) {
			throw new IllegalArgumentException("User name must be non-null and of non-zero length");
		}
		return services.createFolder(folder);
	}

	/**
	 * Recursively deletes a folder and and all its child folders and configurations.
	 * 
	 * A {@link HttpStatus#BAD_REQUEST} is returned if the specified node id is zero (i.e. root node).
	 * 
	 * A {@link HttpStatus#NOT_FOUND} is returned if the specified node id does not exist.
	 * @param nodeId The id of the folder
	 */
	@ApiOperation(value = "Delete a folder and its sub-tree")
	@DeleteMapping("/folder/{nodeId}")
	public void deleteFolder(@PathVariable final int nodeId) {
		services.deleteNode(nodeId);
	}

	/**
	 * Get a folder and its child nodes.
	 * 
	 * A {@link HttpStatus#NOT_FOUND} is returned if the specified node id does not exist.
	 * 
	 * @param nodeId
	 *            The database id of the folder.
	 * @return A {@link Folder} object or <code>null</code> if the node id is not
	 *         associated with an existing folder. The returned object will contain
	 *         existing child nodes as well as the parent node, which is
	 *         <code>null</code> for the root folder.
	 */
	@ApiOperation(value = "Get a folder and its child nodes", produces = JSON)
	@GetMapping("/folder/{nodeId}")
	public Folder getFolder(@PathVariable final int nodeId) {
		return services.getFolder(nodeId);
	}

	
	/**
	 * Create a new {@link Config} node. It is recommended that the {@link Config#configPvList} is
	 * non-empty in order to avoid updates of the configuration at a later stage.
	 * 
	 * A {@link HttpStatus#BAD_REQUEST} is returned the parent node of the configuration 
	 * does not exist, or if the parent node is a {@link Config} node.
	 * 
	 * @param configuration The {@link Config} object to create/save.
	 * @return A {@link Config} object.
	 */
	@ApiOperation(value = "Create a new configuration", consumes = JSON)
	@PutMapping("/config")
	public Config saveConfiguration(@RequestBody final Config configuration) {
		if(configuration.getUserName() == null || configuration.getUserName().isEmpty()) {
			throw new IllegalArgumentException("User name must be non-null and of non-zero length");
		}
		return services.createNewConfiguration(configuration);
	}

	/**
	 * Retrieve a configuration and its list of {@link ConfigPv}s.
	 * 
	 * A {@link HttpStatus#NOT_FOUND} is returned if the specified node id does not exist.
	 * 
	 * A {@link HttpStatus#BAD_REQUEST} is returned if the specified node id is a folder node.
	 * 
	 * @param nodeId The id of the {@link Config}.
	 * @return A {@link Config} object representing the persisted configuration.
	 */
	@ApiOperation(value = "Get configuration and its list of PVs", produces = JSON)
	@GetMapping("/config/{nodeId}")
	public Config getConfiguration(@PathVariable final int nodeId) {
		return services.getConfiguration(nodeId);
	}

	/**
	 * Updates a configuration. For instance, user may change the name of the
	 * configuration or modify the list of PVs. NOTE: in case PVs are removed from
	 * the configuration, the corresponding snapshot values are also deleted.
	 * 
	 * A {@link HttpStatus#NOT_FOUND} is returned if the specified node id does not exist.
	 * 
	 * A {@link HttpStatus#BAD_REQUEST} is returned if the specified node id is a configuration node, or if a user name has not
	 * been specified in the config data.
	 * 
	 * @param configuration The configuration object holding updated data (name, PV list etc).
	 * @return The updated configuration.
	 */
	@ApiOperation(value = "Update configuration (e.g. modify PV list or rename configuration)", consumes = JSON, produces = JSON)
	@PostMapping("/config")
	public Config updateConfiguration(@RequestBody Config configuration) {
		if(configuration.getUserName() == null || configuration.getUserName().isEmpty()) {
			throw new IllegalArgumentException("User name must be non-null and of non-zero length");
		}
		return services.updateConfiguration(configuration);
	}

	/**
	 * Recursively deletes a node (configuration or folder) and all its child nodes. NOTE: if the node id points to a configuration,
	 * all snapshots associated with that configuration will also be deleted.
	 * 
	 * A {@link HttpStatus#NOT_FOUND} is returned if the specified node id does not exist.
	 * 
	 * A {@link HttpStatus#BAD_REQUEST} is returned if the specified node id is the tree root id (0).
	 * 
	 * @param nodeId The non-zero id of the node to delete
	 */
	@ApiOperation(value = "Delete a configuration and all snapshots associated with it.")
	@DeleteMapping("/config/{nodeId}")
	public void deleteNode(@PathVariable final int nodeId) {
		services.deleteNode(nodeId);
	}

	/**
	 * Returns a potentially empty list of {@link Snapshot}s associated with the specified configuration node id.
	 * 
	 * A {@link HttpStatus#NOT_FOUND} is returned if the specified configuration does not exist.
	 * 
	 * A {@link HttpStatus#BAD_REQUEST} is returned if the specified node id is not a configuration.
	 * 
	 * @param nodeId The id of the configuration
	 * @return A potentially empty list of {@link Snapshot}s for the specified configuration.
	 */
	@ApiOperation(value = "Get all snapshots for a config. NOTE: preliminary snapshots are not included.", produces = JSON)
	@GetMapping("/config/{nodeId}/snapshots")
	public List<Snapshot> getSnapshots(@PathVariable int nodeId) {
		return services.getSnapshots(nodeId);
	}

	/**
	 * Moves a node and its sub-tree to a another parent (target) folder.
	 * 
	 * A {@link HttpStatus#NOT_FOUND} is returned if the specified source node does not exist.
	 * 
	 * A {@link HttpStatus#BAD_REQUEST} is returned if:
	 * <ol>
	 * <li>The specified target node does not exist</li>
	 * <li>The specified target node is not a folder node</li>
	 * <li>The specified target node already contains a child node with same name and type (i.e. configuration or folder)</li>
	 * </ol>
	 * 
	 * @param nodeId The id of the source node
	 * @param to The new parent (target) node id
	 * @param userName The (account) name of the user performing the operation.
	 * @return A {@link Folder} object representing the parent (target) folder.
	 */
	@ApiOperation(value = "Moves a node (and the sub-tree in case of a folder node) to another parent folder.", produces = JSON)
	@PostMapping("/node/{nodeId}")
	public Folder moveNode(@PathVariable int nodeId, 
			@RequestParam(value = "to", required = true) int to, 
			@RequestParam(value = "username", required = true) String userName) {
		return services.moveNode(nodeId, to, userName);
	}

	/**
	 * Renames a node.
	 * 
	 * A {@link HttpStatus#BAD_REQUEST} is returned if a node of the same name and type already exists in the parent folder,
	 * or if the node in question is the root node (0).
	 * 
	 * @param nodeId Node id of the node to rename. Must be non-zero.
	 * @param name The new name of the node.
	 * @param userName The (account) name of the user performing the operation.
	 * @return A {@link Node} object representing the renamed node.
	 */
	@ApiOperation(value = "Renames a Node. The parent directory must not contain a node with same name and type.", produces = JSON)
	@PostMapping("/node/{nodeId}/rename")
	public Node renameNode(@PathVariable int nodeId, 
			@RequestParam(value = "name", required = true) String name,
			@RequestParam(value = "username", required = true) String userName) {
		return services.renameNode(nodeId, name, userName);
	}
}
