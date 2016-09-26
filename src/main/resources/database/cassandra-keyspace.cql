CREATE KEYSPACE bfa 
WITH REPLICATION = {'class': 'SimpleStrategy', 'replication_factor': 1};

USE bfa;

CREATE TABLE running_flows (
	runtime_id UUID,
	user_name VARCHAR,
	script_name VARCHAR,
	parameters LIST<VARCHAR>,
	/* part of partition key, format YYYYMMDD */
	start_date VARCHAR,
	start_time TIMESTAMP,
	call_stack LIST<VARCHAR>,
	completed BOOLEAN,
	
	PRIMARY KEY ((start_date, runtime_id), start_time)
) WITH CLUSTERING ORDER BY (start_time DESC);


CREATE TABLE completed_flows (
	runtime_id UUID,
	user_name VARCHAR,
	script_name VARCHAR,
	parameters LIST<VARCHAR>,
	/* part of partition key, format YYYYMMDD */
	start_date VARCHAR,
	start_time TIMESTAMP,
	call_stack LIST<VARCHAR>,
	end_time TIMESTAMP,
	
	PRIMARY KEY ((start_date, runtime_id), start_time)
) WITH CLUSTERING ORDER BY (start_time DESC);


CREATE TABLE failed_flows (
	runtime_id UUID,
	user_name VARCHAR,
	script_name VARCHAR,
	parameters LIST<VARCHAR>,
	/* part of partition key, format YYYYMMDD */
	start_date VARCHAR,
	start_time TIMESTAMP,
	call_stack LIST<VARCHAR>,
	end_time TIMESTAMP,
	error_details LIST<VARCHAR>,
	
	PRIMARY KEY (start_date, start_time)
) WITH CLUSTERING ORDER BY (start_time DESC);

