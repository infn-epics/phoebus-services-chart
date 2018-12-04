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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;

import se.esss.ics.masar.model.Config;
import se.esss.ics.masar.model.Snapshot;
import se.esss.ics.masar.model.SnapshotItem;
import se.esss.ics.masar.model.internal.SnapshotPv;
import se.esss.ics.masar.persistence.dao.ConfigDAO;
import se.esss.ics.masar.persistence.dao.SnapshotDAO;
import se.esss.ics.masar.persistence.dao.SnapshotDataConverter;
import se.esss.ics.masar.services.exception.NodeNotFoundException;
import se.esss.ics.masar.services.exception.SnapshotNotFoundException;

public class SnapshotJdbcDAO implements SnapshotDAO {

	@Autowired
	private SimpleJdbcInsert userNameInsert;

	@Autowired
	private JdbcTemplate jdbcTemplate;
	
	@Autowired
	private SimpleJdbcInsert snapshotInsert;

	@Autowired
	private SimpleJdbcInsert snapshotPvInsert;
	
	@Autowired
	private ConfigDAO configDAO;

	private static final int NO_USER = -1;

	@Override
	public void commitSnapshot(int snapshotId, String snapshotName, String userName, String comment) {
		
		Snapshot snapshot = getSnapshot(snapshotId, false);
		
		if(snapshot == null) {
			throw new SnapshotNotFoundException(String.format("Snapshot with id=%d not found", snapshotId));
		}

		int userId = getUserNameId(userName);
		if (userId == NO_USER) {
			userId = userNameInsert.executeAndReturnKey(Collections.singletonMap("name", userName)).intValue();
		}
		
		jdbcTemplate.update("update snapshot set name=?, username_id=?, comment=? where id=?", snapshotName, userId, comment, snapshotId);
	}

	private int getUserNameId(String userName) {
		try {
			return jdbcTemplate.queryForObject("select id from userName where name=?", new Object[] { userName },
					Integer.class);
		} catch (DataAccessException e) {
			return NO_USER;
		}
	}

	@Override
	public List<Snapshot> getSnapshots(int configId) {
		
		Config config = configDAO.getConfiguration(configId);
		
		if(config == null) {
			throw new NodeNotFoundException(String.format("Configuration with id=%d does not exist", configId));
		}
		
		return jdbcTemplate.query(
				"select snapshot.id, config_id, username_id, created, comment, approve, snapshot.name from snapshot join "
						+ "username on snapshot.username_id=username.id where snapshot.config_id=?",
				new Object[] { configId }, new SnapshotRowMapper());
	}

	@Override
	public Snapshot getSnapshot(int snapshotId, boolean committedOnly) {

		Snapshot snapshot;
		try {
			snapshot = committedOnly ? jdbcTemplate.queryForObject(
					"select snapshot.id, config_id, username_id, created, comment, approve, snapshot.name from snapshot join username on snapshot.username_id=username.id where snapshot.id=?",
					new Object[] { snapshotId }, new SnapshotRowMapper())
					: jdbcTemplate.queryForObject(
							"select snapshot.id, config_id, username_id, created, NULL as comment, approve, NULL as name from snapshot where snapshot.id=?",
							new Object[] { snapshotId }, new SnapshotRowMapper());
		} catch (DataAccessException e) {
			// No committed snapshot corresponding to snapshotId found
			return null;
		}

		List<SnapshotPv> snapshotPVs = jdbcTemplate.query(
				"select * from snapshot_pv join config_pv on snapshot_pv.config_pv_id=config_pv.id where snapshot_id=?",
				new Object[] { snapshotId }, new SnapshotPvRowMapper());

		snapshot.setSnapshotItems(snapshotPVs.stream().map(snapshotPv -> 
			SnapshotDataConverter.fromSnapshotPv(snapshotPv)).collect(Collectors.toList()));

		return snapshot;
	}

	@Override
	public void deleteSnapshot(int snapshotId) {
		jdbcTemplate.update("delete from snapshot where id=?", snapshotId);
	}
	
	@Override
	public Snapshot savePreliminarySnapshot(Config config, List<SnapshotItem> snapshotItems) {

		Map<String, Object> snapshotParams = new HashMap<>();
		snapshotParams.put("config_id", config.getId());
		snapshotParams.put("created", Timestamp.from(Instant.now()));
		
		int snapshotId = snapshotInsert.executeAndReturnKey(snapshotParams).intValue();

		Map<String, Object> params = new HashMap<>(6);
		params.put("snapshot_id", snapshotId);

		for (SnapshotItem snapshotItem : snapshotItems) {
			params.put("config_pv_id", snapshotItem.getConfigPvId());
			params.put("fetch_status", snapshotItem.isFetchStatus());
			if (snapshotItem.isFetchStatus()) {
				SnapshotPv snapshotPv = SnapshotDataConverter.fromVType(snapshotItem.getValue());
				params.put("severity", snapshotPv.getAlarmSeverity().toString());
				params.put("status", snapshotPv.getAlarmStatus().toString());
				params.put("time", snapshotPv.getTime());
				params.put("timens", snapshotPv.getTimens());
				params.put("sizes", snapshotPv.getSizes());
				params.put("data_type", snapshotPv.getDataType().toString());
				params.put("value", snapshotPv.getValue());
			}

			snapshotPvInsert.execute(params);
		}

		return getSnapshot(snapshotId, false);

	}
	
	@Override
	public Snapshot createPreliminarySnapshot(int configId) {
		
		Map<String, Object> snapshotParams = new HashMap<>();
		snapshotParams.put("config_id", configId);
		snapshotParams.put("created", Timestamp.from(Instant.now()));
		
		int snapshotId = snapshotInsert.executeAndReturnKey(snapshotParams).intValue();
		
		return getSnapshot(snapshotId, false);
	}

}
