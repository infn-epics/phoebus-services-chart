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

package se.esss.ics.masar.persistence.config;


import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;

import com.fasterxml.jackson.databind.ObjectMapper;

import se.esss.ics.masar.persistence.dao.ConfigDAO;
import se.esss.ics.masar.persistence.dao.SnapshotDAO;
import se.esss.ics.masar.persistence.dao.impl.ConfigJdbcDAO;
import se.esss.ics.masar.persistence.dao.impl.SnapshotJdbcDAO;

@Configuration
public class JdbcDAOTestConfig {


	@Bean
	public ConfigDAO configDAO() {

		return new ConfigJdbcDAO();
	}
	
	@Bean
	public SnapshotDAO snapshotDAO() {
		return new SnapshotJdbcDAO();
	}
	
	@Bean
	public JdbcTemplate jdbcTemplate() {
		return Mockito.mock(JdbcTemplate.class);
	}
	
	@Bean
	public SimpleJdbcInsert configurationEntryRelationInsert() {
		return Mockito.mock(SimpleJdbcInsert.class);
	}
	
	@Bean
	public SimpleJdbcInsert configurationInsert() {
		return Mockito.mock(SimpleJdbcInsert.class);
	}

	@Bean
	public SimpleJdbcInsert configurationEntryInsert() {
		return Mockito.mock(SimpleJdbcInsert.class);
	}
	
	@Bean
	public SimpleJdbcInsert snapshotPvInsert() {
		return Mockito.mock(SimpleJdbcInsert.class);
	}

	@Bean
	public ObjectMapper objectMapper() {
		return new ObjectMapper();
	}
}
