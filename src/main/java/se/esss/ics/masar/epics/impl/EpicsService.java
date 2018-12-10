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
package se.esss.ics.masar.epics.impl;

import java.util.ArrayList;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.epics.gpclient.GPClient;
import org.epics.vtype.VType;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import se.esss.ics.masar.epics.IEpicsService;
import se.esss.ics.masar.model.Config;
import se.esss.ics.masar.model.ConfigPv;
import se.esss.ics.masar.model.SnapshotItem;

public class EpicsService implements IEpicsService {

	@Autowired
	private ExecutorService executorPool;
	
	@Override
	public List<SnapshotItem> readPvs(Config config) {
		
		LoggerFactory.getLogger(EpicsService.class).info(String.format("Reading %d PVs for configuration id=%d", config.getConfigPvList().size(), config.getId()));
		ExecutorCompletionService<SnapshotItem> ecs = new ExecutorCompletionService<>(executorPool);
		for (ConfigPv configPv : config.getConfigPvList()) {
			ecs.submit(new SnapshotPvCallable(configPv));
		}

		List<SnapshotItem> snapshotPvs = new ArrayList<>();
		for (int i = 0; i < config.getConfigPvList().size(); ++i) {
			try {
				SnapshotItem item = ecs.take().get();
				if (item != null) {
					snapshotPvs.add(item);
				}
			} catch (Exception e) {
				LoggerFactory.getLogger(EpicsService.class)
						.error(String.format("Encountered exception when collecting PVs: %s", e.getMessage()));
			}
		}
				
		return snapshotPvs;
	}
	

	private class SnapshotPvCallable implements Callable<SnapshotItem> {

		private ConfigPv configPv;

		public SnapshotPvCallable(ConfigPv configPv) {
			this.configPv = configPv;
		}

		@Override
		public SnapshotItem call() {
			Future<VType> value = GPClient.readOnce(configPv.getProvider().toString() + "://" + configPv.getPvName());

			try {
				VType vType = value.get(5L, TimeUnit.SECONDS);
				return SnapshotItem.builder().configPvId(configPv.getId()).fetchStatus(true).value(vType).build();
			} catch (Exception ex) {
				return SnapshotItem.builder().configPvId(configPv.getId()).fetchStatus(false).build();
			}
		}
	}
}
