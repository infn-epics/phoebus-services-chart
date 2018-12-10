/**
 * This project implements the MASAR (MAchine Save And Restore) service as a Spring Boot application.
 * 
 * <h1>Client aspects</h1>
 * All REST end points in jmasar expose their API using JSON formatted data. XML is not supported.
 * 
 * Clients should use the jmasar-model library to facilitate development. It is available 
 * <a href="https://gitlab.esss.lu.se/ics-software/jmasar-model">here</a>.
 * 
 * <h1>Key concepts</h1>
 * 
 * <p>
 * The jmasar service implemented in this project manages three key entities: <b>folders</b>, <b>configurations</b> and <b>snapshots</b>.
 * </p>
 * 
 * <p>
 * A <b>folder</b> is a container that may contain other folders (sub-folders) or configurations. A fodler has a name, which must be unique for
 * the folder that contains it, but is referenced in the API by its numerical id.
 * </p>
 * 
 * <p>
 * A <b>configuration</b> lists the PVs that make up the data set (aka save set) 
 * subject for snapshot. A PV - identified by it's unique name - can be listed in 
 * multiple configurations. A configuration has a name, which must be unique for
 * the folder that contains it, but is referenced in the API by its numerical id.
 * </p>
 * 
 * <p>
 * A <b>snapshot</b> consists of the data associated by the list of PVs in a configuration, and that is read at the time the snapshot was requested by the user. 
 * An arbitrary number of time stamped snapshots can be created for each configuration.</p>
 * 
 * <p>
 * Configurations and snapshots are persisted to a database using a tree model. Each node in the tree is either a folder or a configuration. 
 * A snapshot is however <i>not</i> treated as a node in the tree. Consequently a configuration node cannot have child nodes. The service always contains a (folder) root node
 * that cannot be deleted through the service APIs.
 * </p>
 * 
 * <p>
 * Nodes can be moved in the same manner as objects in a file system. A folder can be moved to another parent folder, or a configuration (together with its snapshots) can be moved
 * to a different folder. 
 * Since snapshots are not treated as nodes, they cannot be moved to a different configuration. That would not make sense anyway as the data in the snapshot is bound to the list of PVs in the
 * associated configuration.
 * </p>
 * 
 * <p>
 * A configurations can be updated by changing its name or by modifying its list of PVs. When PVs are deleted from the configuration, the saved data for those PVs in existing snapshots will
 * also be deleted.
 * </p>
 * 
 * <p>
 * When a configuration is deleted, all associated snapshots are also deleted. When a folder is deleted, the entire sub-tree of that folder is also deleted. Clients
 * should hence prompt the user of the consequences of deleting configurations and folders.
 * </p>
 * 
 * <h2>The service</h2>
 * 
 * <p>
 * All operations needed to manage the MASAR data are provided as HTTP end-points in a RESTful service. The list of operations includes creating, updating and deleting nodes in the
 * tree, as well as perform save (i.e. take snapshot). Restore operations are <b>not</b> provided and should instead be executed by a client application based on the saved data in a snapshot.
 * </p>
 * 
 * <h1>Database implementation</h1>
 * <p>
 * A tree as described above can be modeled in various ways in a relational database. This implementation uses a closure table to hold relations between the
 * nodes in the tree.
 * See <a href="https://www.slideshare.net/billkarwin/models-for-hierarchical-data" target="_blank">this presentation</a> for a discussion of various models (including the closure table approach).
 * </p>
 * 
 * <p>
 * The service has been verified on Postgresql 9.6 (Mac OS 10.13 and Centos 7.5) and Mysql 8 (Mac OS 10.13 and Ubuntu 18.04).
 * The in-memory database H2 (version 1.4) is used for unit testing.
 * </p>
 * 
 * <h1>Client work flow example</h1>
 * 
 * <p>
 * It should be noted that jmasar is an open service and all data created is visible to anyone logged into the ESS domain.
 * </p>
 * 
 * <p>
 * In the following, REST end points are all relative to the root of the jmasar service. At ESS, the jmasar service root is
 * http://jmasar.tn.esss.lu.se.
 * </p>
 * 
 * <p>
 * In order to save a snapshot of PV values, the user (client) must first define the list of PVs in a configuration (aka save set). The PV names shall
 * be valid names that can be accessed over EPICS channel access (ca) or EPICS pvaccess (pva). Consider the below work flow detailing
 * the steps needed to 
 * </p>
 * 
 * <p>
 * <b>Work flow </b>
 * <ol>
 * 
 * <li><b>Optional.</b> Create a folder for the configuration. Take care to choose folder names that make sense, avoid names
 * like "test1" or "My folder". Create a {@link se.esss.ics.masar.model.Folder} object specifying {@link se.esss.ics.masar.model.Folder#parentId}
 * and {@link se.esss.ics.masar.model.Folder#name} and submit this in a PUT request to the REST end point <code>/folder</code>.
 * The service will return a {@link se.esss.ics.masar.model.Folder} object representing the newly created and persisted folder.</li>
 * 
 * <li>Identify a folder for the configuration. Folders are uniquely identified by their numerical id (where the root folder has id=0). To list
 * existing folders in a folder with id=x, use the REST end point <code>/folder/x</code>.</li>
 * 
 * <li>Create a {@link se.esss.ics.masar.model.ConfigPv} object for each PV that should be included in the configuration.
 * Specify {@link se.esss.ics.masar.model.ConfigPv#pvName} and {@link se.esss.ics.masar.model.ConfigPv#provider} for each
 * {@link se.esss.ics.masar.model.ConfigPv} object. Wrap the {@link se.esss.ics.masar.model.ConfigPv} objects in a 
 * {@link java.util.List}.</li>
 * 
 * <li>Create a {@link se.esss.ics.masar.model.Config} object specifying {@link se.esss.ics.masar.model.Config#parentId},
 * {@link se.esss.ics.masar.model.Config#name} and {@link se.esss.ics.masar.model.Config#configPvList}. Submit this as a PUT request to 
 * the REST end point <code>/config</code>. The service will return a {@link se.esss.ics.masar.model.Config} object representing
 * the newly created and persisted configuration.</li>
 * 
 * <li>To take a snapshot for a configuration with id=y, make a PUT call to the REST end point <code>/snapshot/y</code>. The
 * service will return a newly created and persisted  {@link se.esss.ics.masar.model.Snapshot} object. However, the snapshot
 * will be in a preliminary state, which means it will not be listed when querying a configuration for its list of snapshots.</li>
 * 
 * <li>To put a snapshot with id=z in a committed state, make a POST request to the REST end point <code>/snapshot/z</code> providing
 * three mandatory request parameters like so: <code>/snapshot/z?snapshotName=some_name&amp;userName=logged_in_user&amp;comment=some_comment</code>.
 * The service returns the committed and persisted {@link se.esss.ics.masar.model.Snapshot} object.
 * </li>
 * 
 * <li>New snapshots for the same configuration can be created by repeating the two previous steps.</li>
 * 
 * <li>A snapshot with id=a that for some reason is not needed can - regardless of its current state - be deleted by sending a DELETE request
 * to the end point <code>/snapshot/a</code>.</li>
 * 
 * </ol>
 * 
 */
package se.esss.ics.masar;