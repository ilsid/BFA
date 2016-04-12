CREATE CLASS SequenceProvider;
CREATE PROPERTY SequenceProvider.name STRING;
CREATE PROPERTY SequenceProvider.value LONG;

INSERT INTO SequenceProvider(name, value) values('FlowRuntimeSequence', 0);

CREATE CLASS FlowRuntime EXTENDS V;
CREATE PROPERTY FlowRuntime.runtimeId LONG;
CREATE PROPERTY FlowRuntime.scriptName STRING;
CREATE PROPERTY FlowRuntime.scriptParameters EMBEDDEDLIST;
CREATE PROPERTY FlowRuntime.runtimeState STRING;
CREATE PROPERTY FlowRuntime.errorDetails EMBEDDEDLIST;

