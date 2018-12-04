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

package se.esss.ics.masar.services.config;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import se.esss.ics.masar.epics.IEpicsService;
import se.esss.ics.masar.epics.config.EpicsConfiguration;
import se.esss.ics.masar.persistence.config.PersistenceConfiguration;
import se.esss.ics.masar.persistence.dao.ConfigDAO;
import se.esss.ics.masar.persistence.dao.SnapshotDAO;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextHierarchy({ @ContextConfiguration(classes = { ServicesConfiguration.class, PersistenceConfiguration.class, EpicsConfiguration.class}) })
@TestPropertySource(properties = {"dbengine = h2"})
public class ServicesConfigurationTest {
	
	@Autowired
	private IEpicsService epicsService;
	
	@Autowired
	private ConfigDAO configDAO;
	
	@Autowired
	private SnapshotDAO snapshotDAO;
	
	@Test
	public void testConfig() {
		assertNotNull(epicsService);
		assertNotNull(configDAO);
		assertNotNull(snapshotDAO);
	}
}
