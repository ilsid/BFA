CREATE KEYSPACE bfa 
WITH REPLICATION = {'class': 'SimpleStrategy', 'replication_factor': 1};

USE bfa;

CREATE TABLE flow_runtime_by_date (
	runtime_id UUID,
	user_name VARCHAR,
	script_name VARCHAR,
	parameters LIST<VARCHAR>,
	status VARCHAR,
	start_date VARCHAR,
	start_time TIMESTAMP,
	end_time TIMESTAMP,
	call_stack LIST<VARCHAR>,
	error_details LIST<VARCHAR>,
	
	PRIMARY KEY ((start_date), status, runtime_id, start_time)
) WITH CLUSTERING ORDER BY (status ASC, runtime_id ASC, start_time DESC);


CREATE TABLE flow_runtime_by_status (
	runtime_id UUID,
	user_name VARCHAR,
	script_name VARCHAR,
	parameters LIST<VARCHAR>,
	status VARCHAR,
	start_date VARCHAR,
	start_time TIMESTAMP,
	end_time TIMESTAMP,
	call_stack LIST<VARCHAR>,
	error_details LIST<VARCHAR>,
	
	PRIMARY KEY ((status, start_date), runtime_id, start_time)
) WITH CLUSTERING ORDER BY (runtime_id ASC, start_time DESC);
