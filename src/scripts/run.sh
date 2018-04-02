#!/bin/sh
java -cp "lib/*" -Dbfa.config=config/config.properties -Dbfa.log=config/log4j.xml com.ilsid.bfa.main.Application