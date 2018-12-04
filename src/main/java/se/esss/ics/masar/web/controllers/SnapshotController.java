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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.ApiOperation;
import se.esss.ics.masar.model.ConfigPv;
import se.esss.ics.masar.model.Snapshot;
import se.esss.ics.masar.model.internal.SnapshotPv;
import se.esss.ics.masar.services.IServices;

@RestController
public class SnapshotController extends BaseController {

	@Autowired
	private IServices services;

	/**
	 * Creates a new snapshot for the specified configuration, and saves it in a "preliminary" state.
	 * Snapshots in a preliminary state are not visible when listing snapshots, see {@link ConfigurationController#getSnapshots(int)}.
	 *
	 * A {@link HttpStatus#BAD_REQUEST} is returned if the specified configuration id does not exist.
	 * 
	 * Note that a snapshot will be created even if all PVs listed in the configuration are off-line. Further,
	 * the list of {@link SnapshotPv}s will contain one element for each {@link ConfigPv} in the
	 * configuration. The fetchStatus {@link SnapshotPv} field can be used to determine if the
	 * PV has been successfully read.
	 * 
	 * Also note the synchronous behavior of this service; all PVs are read asynchronously, but the
	 * service does not return until all PVs have been read (or timed out).
	 * 
	 * @param configId The configuration id.
	 * @return A {@link Snapshot} object containing the PV values wrapped in a list of {@link SnapshotPv}s.
	 */
	@ApiOperation(value = "Take a snapshot, i.e. save preliminary.")
	@PutMapping("/snapshot/{configId}")
	public Snapshot takeSnapshot(@PathVariable int configId) {
		return services.takeSnapshot(configId);
	}

	/**
	 * Retrieves a {@link Snapshot} and its list of {@link SnapshotPv}s.
	 * 
	 * A {@link HttpStatus#NOT_FOUND} is returned if the specified snapshot id does not exist.
	 * 
	 * @param snapshotId The id of the snapshot
	 * @return A {@link Snapshot} object containing the PV values wrapped in a list of {@link SnapshotPv}s.
	 */
	@ApiOperation(value = "Get a snapshot, including its values.", consumes = JSON)
	@GetMapping("/snapshot/{snapshotId}")
	public Snapshot getSnapshot(@PathVariable int snapshotId) {

		return services.getSnapshot(snapshotId);
	}

	/**
	 * Deletes a {@link Snapshot} and all its PV values.
	 * 
	 * A {@link HttpStatus#NOT_FOUND} is returned if the specified snapshot id does not exist.
	 * 
	 * @param snapshotId The id of the snapshot
	 */
	@ApiOperation(value = "Delete a snapshot", consumes = JSON)
	@DeleteMapping("/snapshot/{snapshotId}")
	public void deleteSnapshot(@PathVariable int snapshotId) {

		services.deleteSnapshot(snapshotId);
	}

	/**
	 * Commits a snapshot such that it will be visible when listing snapshots for a configuration,
	 * see {@link ConfigurationController#getSnapshots(int)}.
	 * 
	 * A {@link HttpStatus#NOT_FOUND} is returned if the specified snapshot id does not exist.
	 * 
	 * A {@link HttpStatus#BAD_REQUEST} is returned if the user name or comment are null or of zero length.
	 *  
	 * @param snapshotId The id of the snapshot
	 * @param snapshotName Name of the snapshot
	 * @param userName Mandatory user name.
	 * @param comment Mandatory comment.
	 * @return The committed {@link Snapshot}.
	 */
	@ApiOperation(value = "Commit a snapshot, i.e. update with snapshot name, user name and comment.")
	@PostMapping("/snapshot/{snapshotId}")
	public Snapshot commitSnapshot(@PathVariable int snapshotId, 
			@RequestParam(required = true) String snapshotName,
			@RequestParam(required = true) String userName,
			@RequestParam(required = true) String comment) {
		
		if(snapshotName.length() == 0 || userName.length() == 0 || comment.length() == 0) {
			throw new IllegalArgumentException("Snapshot name, username and comment must be of non-zero length");
		}

		return services.commitSnapshot(snapshotId, snapshotName, userName, comment);
	}
}
