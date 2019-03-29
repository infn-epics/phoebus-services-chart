/*
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

package db.migration.common;

import java.util.List;
import java.util.UUID;

import org.flywaydb.core.api.migration.spring.BaseSpringJdbcMigration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @author georgweiss
 * Created 8 Mar 2019
 */
public class V11_0__set_unique_id_on_node extends BaseSpringJdbcMigration{
	
	
	/* (non-Javadoc)
	 * @see org.flywaydb.core.api.migration.spring.SpringJdbcMigration#migrate(org.springframework.jdbc.core.JdbcTemplate)
	 */
	@Override
	public void migrate(JdbcTemplate jdbcTemplate) throws Exception {
		
		List<Integer> ids = jdbcTemplate.queryForList("select id from node", Integer.class);
		
		for(Integer id : ids) {
			jdbcTemplate.update("update node set unique_id=? where id=?", new Object[] {UUID.randomUUID().toString(), id});
		}
	}
}
