CREATE CLASS SequenceProvider;
CREATE PROPERTY SequenceProvider.name STRING;
CREATE PROPERTY SequenceProvider.value LONG;

INSERT INTO SequenceProvider(name, value) values('FlowRuntimeSequence', 0);

CREATE CLASS FlowRuntime;
CREATE PROPERTY FlowRuntime.runtimeId LONG;
CREATE PROPERTY FlowRuntime.scriptName STRING;
CREATE PROPERTY FlowRuntime.parameters EMBEDDEDLIST;
CREATE PROPERTY FlowRuntime.status STRING;
CREATE PROPERTY FlowRuntime.startTime DATETIME;
CREATE PROPERTY FlowRuntime.endTime DATETIME;
CREATE PROPERTY FlowRuntime.callStack EMBEDDEDLIST;
CREATE PROPERTY FlowRuntime.errorDetails EMBEDDEDLIST;

CREATE INDEX FlowRuntime.runtimeId NOTUNIQUE;
CREATE INDEX FlowRuntime.runtimeId_scriptName ON FlowRuntime (runtimeId, scriptName) UNIQUE;

