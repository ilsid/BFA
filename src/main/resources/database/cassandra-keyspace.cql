CREATE KEYSPACE bfa 
WITH REPLICATION = {'class': 'SimpleStrategy', 'replication_factor': 1};

USE bfa;

CREATE TABLE running_flows (
	runtime_id UUID,
	user_name VARCHAR,
	script_name VARCHAR,
	parameters LIST<VARCHAR>,
	/* partition key, format YYYYMMDD */
	start_date VARCHAR,
	start_time TIMESTAMP,
	call_stack LIST<VARCHAR>,
	
	PRIMARY KEY ((start_date), runtime_id, start_time)
) WITH CLUSTERING ORDER BY (runtime_id ASC, start_time DESC);


CREATE TABLE completed_flows_by_date (
	runtime_id UUID,
	user_name VARCHAR,
	script_name VARCHAR,
	parameters LIST<VARCHAR>,
	/* partition key, format YYYYMMDD */
	start_date VARCHAR,
	start_time TIMESTAMP,
	call_stack LIST<VARCHAR>,
	status VARCHAR,
	end_time TIMESTAMP,
	error_details LIST<VARCHAR>,
	
	PRIMARY KEY ((start_date), status, runtime_id, start_time)
) WITH CLUSTERING ORDER BY (status ASC, runtime_id ASC, start_time DESC);


CREATE TABLE completed_flows_by_status (
	runtime_id UUID,
	user_name VARCHAR,
	script_name VARCHAR,
	parameters LIST<VARCHAR>,
	/* a part of partition key, format YYYYMMDD */
	start_date VARCHAR,
	start_time TIMESTAMP,
	call_stack LIST<VARCHAR>,
	status VARCHAR,
	end_time TIMESTAMP,
	error_details LIST<VARCHAR>,
	
	PRIMARY KEY ((status, start_date), runtime_id, start_time)
) WITH CLUSTERING ORDER BY (runtime_id ASC, start_time DESC);
