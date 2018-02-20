(function () {
	bfa = {
		activeFlowEditorTab: undefined,

		TIME_PATTERN: {datePattern: 'HH:mm:ss', selector: 'date', locale: 'en-us'},
		// FIXME: date format should be obtained dynamically from server
		DATE_PATTERN: {datePattern: 'yyyy-MM-dd', selector: 'date', locale: 'en-us'}
	};
	
	createTree('script', createScriptTab, undefined, 'getSource');
	createTree('entity', createEntityTab, 'toolbarIconEntity', 'getSource');	
	createTree('action', createActionTab, 'toolbarIconAction', 'getInfo');
	
	initDiagramElementPropertiesPane();
	initFlowEditorEventHandlers();
	
}());


function isEmptyObject(obj) {
	var keys = [];
	for (var key in obj) {
		keys.push(key);
		break;
	}
	return keys.length == 0;
}

function convertArrayToLines(arr) { 
	var lines = '';
	if (arr) {
		arr.forEach(function(item, i, arr) {
			lines = lines + item + '<br/>';
		});
	}
	return lines;
}	

function convertMillisecondsToTime(millis) { 
	var result = '';
	require(['dojo/date/locale'],
		function(locale) {
			if (millis) {
				result = locale.format(new Date(millis), bfa.TIME_PATTERN);
			}
	});
	
	return result;
}	

function getSelectedGroupName(tree, groupType) {
	var selectedItem = tree.selectedItems[0];
	
	if (selectedItem) {
		if (selectedItem.type == groupType) {
			return selectedItem.name;
		} else {
			// reference to parent item
			return tree.path[tree.path.length - 2].name;
		}
	} else {
		return null;
	}
}

function getSelectedGroupItem(tree) {
	var selectedItem = tree.selectedItems[0];
	
	if (selectedItem) {
		if (tree.model.mayHaveChildren(selectedItem)) {
			return selectedItem;
		} else {
			// reference to parent item
			return tree.path[tree.path.length - 2];
		}
	} else {
		return null;
	}
}

function escapeScriptSource(source)  {
	return source.replace(/"/g,'\\"').replace(/\\{2}"/g,'\\\\\\"')
		.replace(/\n/g, '\\n').replace(/\r/g, '\\r').replace(/\t/g, '\\t');
}

function escapeFlowDiagramState(source) {
	return escapeScriptSource(source);
}

function writeInfo(message) {
	require([ 'dijit/registry', 'dojo/dom-construct', 'dojo/domReady!'],
		function(registry, domConstruct) {
			domConstruct.place('<tr class="consoleMessage"><td><pre>' + message + '</pre></td></tr>', 
				'infoTable', 'first');
			registry.byId('bottomTabContainer').selectChild(registry.byId('infoTab'));	
		});
}

function writeInfoInBackground(message) {
	require([ 'dojo/dom-construct', 'dojo/domReady!'],
		function(domConstruct) {
			domConstruct.place('<tr class="consoleMessage"><td><pre>' + message + '</pre></td></tr>', 
				'infoTable', 'first');
		});
}

function writeErrorMessage(msg) {
	require([ 'dijit/registry', 'dojo/dom-construct', 'dojo/domReady!'],
		function(registry, domConstruct) {
			domConstruct.place('<tr class="consoleMessage"><td><pre>' + msg + '</pre></td></tr>' 
				+ '<tr class="consoleMessage"><td>'
				+ '----------------------------------------------------------------</td></tr>'
				,'errorTable', 'first');
			registry.byId('bottomTabContainer').selectChild(registry.byId('errorTab'));	
		});
}

function writeError(error, message) {
	if (error.response.status == 500) {
		writeErrorMessage('Internal server error. Please contact your system administrator.');
		return;
	}
	
	if (message) {
		writeErrorMessage(message + ': ' + error.response.text);
	} else {
		writeErrorMessage(error.response.text);
	}
}

function escapeEntitySource(source)  {
	return source.replace(/"/g,'\\"');
}

function createScriptEditor(scriptSource, parentPanel) {
	require(['dojo/dom-construct', 'dojo/domReady!'],
		function(domConstruct) {
			var parentId = parentPanel.id;
			var scriptHolderId = parentId + '-script-editor';
			var scriptHolder = domConstruct.toDom('<div id="' + scriptHolderId + 
				'" ace-script-editor="true"></div>');
			domConstruct.place(scriptHolder, parentId);
			
			var editor = ace.edit(scriptHolderId);
			editor.setTheme("ace/theme/eclipse");
			editor.getSession().setMode("ace/mode/java");
			
			document.getElementById(scriptHolderId).style.top = 0;
			
			if (scriptSource) {
				editor.setValue(scriptSource);
				editor.gotoLine(1);
			}
			
			editor.getSession().on('change', function() {
				fireEditorEvent('flowSourceChange');
			});
			
			parentPanel.editor = editor;
	});
}

function drawFlowChart(scriptName, canvasId) {
	require(['dojo/request/xhr', 'dojo/dom-construct', 'dojo/domReady!'],
		function(request, domConstruct) {
			request('service/script/admin/getFlowChart', {
					headers: { 'Content-Type': 'application/json' },
					method: 'POST',
					data: '{"name": "' + scriptName + '"}'
				}).then(function(resp){
					var chart = domConstruct.toDom('<div class="mermaid">' + resp + '</div>');
					domConstruct.place(chart, canvasId);
					
					mermaid.init();
					
					// A workaround to avoid chart picture scaling
					var svg = chart.children[0];
					var viewBoxValues = svg.getAttribute('viewBox').split(' ');
					svg.style.height = viewBoxValues[3];
				}, function(err) {
					writeError(err);
				});
		});
}

function drawFlowDiagram(scriptName, canvas) {
	require(['dojo/request/xhr', 'dojo/dom-construct', 'dojo/domReady!'],
		function(request, domConstruct) {
			request('service/script/admin/getDiagram', {
					headers: { 'Content-Type': 'application/json' },
					method: 'POST',
					data: '{"name": "' + scriptName + '"}'
				}).then(function(resp){
					if (resp.length > 0) {
						var diagram = JSON.parse(resp);
						drawDiagram(canvas, diagram)
					} else {
						drawMockDiagram(canvas);
					}
				}, function(err) {
					writeError(err);
				});
		});
}

function fetchFlowsRuntimeRecords() {
	require(['dojo/request/xhr', 'dijit/registry', 'dojo/data/ObjectStore', 
			'dojo/store/Memory', 'dojo/date/locale', 'dojo/domReady!'],
		function(request, registry, ObjectStore, Memory, locale) {
			var status = document.getElementById('runtimeMonitorTab_status').value;
			var dateObj = registry.byId('runtimeMonitorTab_startDate').get('value');
			var date = locale.format(dateObj, bfa.DATE_PATTERN);
			
			request('service/script/runtime/fetch', {
					headers: { 'Content-Type': 'application/json' },
					method: 'POST',
					handleAs: 'json',
					data: '{"criteria": {"startDate": "' + date + '", "status": "' + status + '"}}'
				}).then(function(resp){
				
					var grid = registry.byId('runtimeMonitorTab_dataGrid');
					
					delete grid.store;
					grid.setStore(new ObjectStore({ 
						objectStore: new Memory({ data: []}),
						modified: false
					}));
					
					var records = resp.result;
					records.forEach(function(rec, i, records) {
						grid.store.newItem(
							{fldScriptName: rec.scriptName, 
							 fldRuntimeId: rec.runtimeId, 
							 fldStartTime: rec.startTime, 
							 fldEndTime: rec.endTime, 
							 fldParams: rec.parameters,
							 fldCallStack: rec.callStack, 
							 fldErrors: rec.errorDetails, 
							 fldStatus: rec.status,
							 fldUser: rec.userName});
					});
										
					grid.store.save();
					grid.render();
					
				}, function(err) {
					writeError(err);
				});
		});
}

function startMonitoring(serverURL) {
	require(['dijit/registry'],
		function(registry) {
			//TODO: handle connection error
			monitorConnection = new WebSocket(serverURL);
			
			var filterBtn = registry.byId('runtimeMonitorTab_filterBtn');
			var dateBox = registry.byId('runtimeMonitorTab_startDate');
			filterBtn.set('disabled', true);
			dateBox.set('disabled', true);
			
			monitorConnection.onmessage = function(e){
				writeInfoInBackground(e.data);
			}
			
			//TODO: make the interval configurable
			var intervalMillis = 4000;
			var monitorTimeframe = null;
			var startDate = new Date();
			var status = document.getElementById('runtimeMonitorTab_status').value;
			
			monitorInterval = window.setInterval(function() {
				if (monitorTimeframe != null) {
					monitorTimeframe.minTime = monitorTimeframe.maxTime;
					monitorTimeframe.maxTime = new Date().getTime();
				} else {
					var maxTimeMillis = new Date().getTime();
					var minTime = new Date();
					minTime.setMilliseconds(minTime.getMilliseconds() - 2 * intervalMillis);
					var minTimeMillis = minTime.getTime();
					
					monitorTimeframe = {
						minTime: minTimeMillis, 
						maxTime:  maxTimeMillis
					};
				}
				
				// A case when a date is changed during the monitoring session
				if (startDate.getDate() != new Date().getDate()) {
					startDate = new Date();
				}
				
				var query = {
					criteria: {
						startDate: startDate,
						status: status,
						minStartTime: monitorTimeframe.minTime,
						maxStartTime: monitorTimeframe.maxTime
					}
				};
				
				monitorConnection.send(JSON.stringify(query));
				
			}, intervalMillis);
		
		});
}

function stopMonitoring() {
	require(['dijit/registry'],
		function(registry) {
			var filterBtn = registry.byId('runtimeMonitorTab_filterBtn');
			var dateBox = registry.byId('runtimeMonitorTab_startDate');
						
			filterBtn.set('disabled', false);
			dateBox.set('disabled', false);
			
			if (monitorInterval) {
				window.clearInterval(monitorInterval);
				delete monitorInterval;
			}
			
			if (monitorConnection) {
				monitorConnection.close();
				delete monitorConnection;
				writeInfoInBackground('Monitor connection closed');
			}
		});
}

function changeRealTimeMonitoringMode() {
	require(['dijit/registry', 'dojo/request/xhr'],
		function(registry, request) {
			var checkBox = registry.byId('runtimeMonitorTab_realtimeCheck');
						
			if (checkBox.checked) {
				request('service/script/runtime/getMonitoringServerUrl', {
					method: 'GET'
				}).then(function(resp){
					var url = resp;
					//var url = 'ws://localhost:8027/bfa/monitor';
					startMonitoring(url);
				}, function(err) {
					checkBox.set('checked', false);	
					writeErrorMessage('Failed to get the monitoring server info: ' + err);
				});
			} else {
				stopMonitoring();
			}
		});
}

function showCreateGroupDialog(objectType) {
	require([ 'dijit/registry'],
		function(registry) {
			var tree = registry.byId(objectType + 'Tree');
			var groupType = objectType.toUpperCase() + '_GROUP';
			var selectedGroupName = getSelectedGroupName(tree, groupType);
			var groupText = registry.byId('createGroupDialog_textGroupName');
			var parentGroupText = registry.byId('createGroupDialog_textParentGroup');
			var topLevelCheckBox = registry.byId('createGroupDialog_checkTopLevel');
								
			if (selectedGroupName != null) {
				parentGroupText.set('value', selectedGroupName);
				topLevelCheckBox.set('checked', false);
				topLevelCheckBox.set('disabled', false);
			} else {
				parentGroupText.set('value', '');
				topLevelCheckBox.set('checked', true);
				topLevelCheckBox.set('disabled', true);
			}
			groupText.set('value', '');
			groupText.reset();
			var dialog = registry.byId('createGroupDialog');
			dialog.currentObjectType = objectType;
			dialog.show();
		});
}

function showCreateScriptDialog() {
	require([ 'dijit/registry'],
		function(registry) {
			var tree = registry.byId('scriptTree');
			var selectedGroupName = getSelectedGroupName(tree, 'SCRIPT_GROUP');
			var groupText = registry.byId('createScriptDialog_textGroupName');
			var scriptText = registry.byId('createScriptDialog_textScriptName');
			
			groupText.set('value', selectedGroupName);
			scriptText.set('value', '');
			scriptText.reset();
			registry.byId('createScriptDialog').show();
		});
}

function showUpdateScriptDialog() {
	require([ 'dijit/registry'],
		function(registry) {
			var tab = registry.byId('tabContainer').selectedChildWidget;
			registry.byId('updateScriptDialog_textGroupName').set('value', tab.objectInfo.group);
			registry.byId('updateScriptDialog_textScriptName').set('value', tab.objectInfo.name);
			registry.byId('updateScriptDialog').show();
		});
}

function showCreateEntityDialog() {
	require([ 'dijit/registry'],
		function(registry) {
			var tree = registry.byId('entityTree');
			var selectedGroupName = getSelectedGroupName(tree, 'ENTITY_GROUP');
			var groupText = registry.byId('createEntityDialog_textGroupName');
			var nameText = registry.byId('createEntityDialog_textEntityName');
			
			groupText.set('value', selectedGroupName);
			nameText.set('value', '');
			nameText.reset();
			registry.byId('createEntityDialog').show();
		});
}

function showUpdateEntityDialog() {
	require([ 'dijit/registry'],
		function(registry) {
			var tab = registry.byId('tabContainer').selectedChildWidget;
			registry.byId('updateEntityDialog_textGroupName').set('value', tab.objectInfo.group);
			registry.byId('updateEntityDialog_textEntityName').set('value', tab.objectInfo.name);
			registry.byId('updateEntityDialog').show();
		});
}

function showNewEntityFieldDialog() {
	require([ 'dijit/registry'],
		function(registry) {
			var txtName = registry.byId('newEntityFieldDialog_textName');
			var txtType = registry.byId('newEntityFieldDialog_textType');
			txtName.set('value', '');
			txtName.reset();
			txtType.set('value', '');
			txtType.reset();
			
			registry.byId('newEntityFieldDialog').show();
		});
}

function showDeleteDialog(objectType) {
	require([ 'dijit/registry', 'dijit/ConfirmDialog', 'dojo/request/xhr'],
		function(registry, ConfirmDialog, request) {
			var tree = registry.byId(objectType + 'Tree');
			var selectedItem = tree.selectedItems[0];
			var objectName = selectedItem.name;
			
			if (!bfa['confirmDeletionDialog']) {
				bfa['confirmDeletionDialog'] = new ConfirmDialog();
			}
			
			var dialog = bfa['confirmDeletionDialog'];
			dialog.set('title', 'Deletion of ' + objectType);
			dialog.set('content', '<img src="res/icons/warn_16x16.png" />&nbsp;Please confirm deletion of ' 
				+ objectType + ' [' + objectName + ']');
			
			dialog.onExecute = function() {
				request('service/' + objectType + '/admin/delete', {
					headers: { 'Content-Type': 'text/plain' },
					method: 'POST',
					data: objectName,
					handleAs: 'json'
				}).then(function(resp) {
					dialog.hide();
					
					var tree = registry.byId(objectType + 'Tree');
					var groupItem = getSelectedGroupItem(tree);
					tree.refreshChildrenSubTree(groupItem.name);
					
					var btnDelete = registry.byId(objectType + 'Toolbar_delete');
					var btnNew = registry.byId(objectType + 'Toolbar_new');
					btnDelete.set('disabled', true);
					btnNew.set('disabled', true);
					
					var tabContainer = registry.byId('tabContainer');
					var tabId = 'tab_' + objectType + '_' + objectName;
					var tab = registry.byId(tabId);
					if (tab) {
						tabContainer.removeChild(tab);
						tab.destroyRecursive();
					}
					writeInfo(objectType.charAt(0).toUpperCase() + objectType.slice(1) 
						+ ' [' + objectName + '] is deleted');
				}, function(err) {
					dialog.hide();
					writeError(err);
				});
			};
			
			dialog.show();
		});
}

function hideDialog(id) {
	require([ 'dijit/registry'],
		function(registry) {
			registry.byId(id).hide();
		});
}		

function onCreateGroupDialog_checkTopLevelClick() {
	require([ 'dijit/registry'],
		function(registry) {
			var checkBox = registry.byId('createGroupDialog_checkTopLevel');
			var objectType = registry.byId('createGroupDialog').currentObjectType;
			var tree = registry.byId(objectType + 'Tree');
			var parentGroupText = registry.byId('createGroupDialog_textParentGroup');
			if (checkBox.checked) {
				parentGroupText.set('value', '');
			} else {
				var groupType = objectType.toUpperCase() + '_GROUP';
				var selectedGroupName = getSelectedGroupName(tree, groupType);
				parentGroupText.set('value', selectedGroupName);
			}
		});
}

function onCreateGroupDialog_btnOkClick() {
	require([ 'dijit/registry', 'dojo/request/xhr'],
		function(registry, request) {
			var groupText = registry.byId('createGroupDialog_textGroupName');
			if (!groupText.isValid()) {
				return;
			}
			
			var objectType = registry.byId('createGroupDialog').currentObjectType;
			var parentGroupName = null;
			var tree = registry.byId(objectType + 'Tree');
			if (!registry.byId('createGroupDialog_checkTopLevel').checked) {
				var groupType = objectType.toUpperCase() + '_GROUP';
				parentGroupName = getSelectedGroupName(tree, groupType);
			} 
			
			var groupName = groupText.get('value');
			if (parentGroupName != null) {
				groupName = parentGroupName + '::' + groupName;
			}
			
			request('service/' + objectType + '/admin/createGroup', {
					headers: { 'Content-Type': 'text/plain' },
					method: 'POST',
					data: groupName
				}).then(function(resp){
					var parentId = parentGroupName != null ? parentGroupName : tree.rootName; 
					tree.refreshChildrenSubTree(parentId);
					hideDialog('createGroupDialog');
					writeInfo('Group [' + groupName + '] is created');
				}, function(err) {
					hideDialog('createGroupDialog');
					writeError(err);
				});
		});
}

function onCreateScriptDialog_btnOkClick() {
	require([ 'dijit/registry', 'dojo/request/xhr'],
		function(registry, request) {
			var txtName = registry.byId('createScriptDialog_textScriptName');
			if (!txtName.isValid()) {
				return;
			}
			
			var tree = registry.byId('scriptTree');
			var groupName = getSelectedGroupName(tree, 'SCRIPT_GROUP');
			// group name can't be null, as script creation is enabled only if some group is selected
			var scriptShortName = txtName.get('value');
			var scriptName = groupName + '::' + scriptShortName;
			
			var tabContainer = registry.byId('tabContainer');
			var scriptTab = tabContainer.selectedChildWidget;
			var scriptEditor = scriptTab.getChildren()[1].getChildren()[0].editor;
			var scriptSource = scriptEditor.getValue();
			var scriptBody = escapeScriptSource(scriptSource);
			var flowCanvas = scriptTab.getChildren()[1].getChildren()[2].getChildren()[0].getChildren()[0].canvas;
			var flowDiagramState = flowCanvas.state.save(); 
			var flowDiagramStateStr = escapeFlowDiagramState(JSON.stringify(flowDiagramState, null, 4));
			
			request('service/script/admin/create', {
					headers: { 'Content-Type': 'application/json' },
					method: 'POST',
					data: '{"name": "' + scriptName + '", "body": "' + scriptBody + '", "flowDiagram": "' + flowDiagramStateStr + '"}'
				}).then(function(resp){
					tree.refreshChildrenSubTree(groupName);
					hideDialog('createScriptDialog');
					var tabIdx = tabContainer.getIndexOfChild(scriptTab);
					tabContainer.removeChild(scriptTab);
					scriptTab.destroyRecursive();
					createScriptTab(scriptShortName, 'tab_script_' + scriptName, scriptSource, tabIdx);
					writeInfo('Script [' + scriptName + '] is created');
				}, function(err) {
					hideDialog('createScriptDialog');
					writeError(err);
				});
		});
}

function onUpdateScriptDialog_btnOkClick() {
	require([ 'dijit/registry', 'dojo/request/xhr'],
		function(registry, request) {
			var groupName = registry.byId('updateScriptDialog_textGroupName').get('value');
			var scriptShortName = registry.byId('updateScriptDialog_textScriptName').get('value');
			var scriptName = groupName + '::' + scriptShortName;
			
			var tabContainer = registry.byId('tabContainer');
			var scriptTab = tabContainer.selectedChildWidget;
			var scriptEditor = scriptTab.getChildren()[1].getChildren()[0].editor;
			var scriptSource = scriptEditor.getValue();
			var scriptBody = escapeScriptSource(scriptSource);
			var flowCanvas = scriptTab.getChildren()[1].getChildren()[2].getChildren()[0].getChildren()[0].canvas;
			var flowDiagramState = flowCanvas.state.save(); 
			var flowDiagramStateStr = escapeFlowDiagramState(JSON.stringify(flowDiagramState, null, 4));
			
			request('service/script/admin/update', {
					headers: { 'Content-Type': 'application/json' },
					method: 'POST',
					data: '{"name": "' + scriptName + '", "body": "' + scriptBody + '", "flowDiagram": "' + flowDiagramStateStr + '"}'
				}).then(function(resp){
					hideDialog('updateScriptDialog');
					var tabIdx = tabContainer.getIndexOfChild(scriptTab);
					tabContainer.removeChild(scriptTab);
					scriptTab.destroyRecursive();
					createScriptTab(scriptShortName, 'tab_script_' + scriptName, scriptSource, tabIdx);
					writeInfo('Script [' + scriptName + '] is updated');
				}, function(err) {
					hideDialog('updateScriptDialog');
					writeError(err);
				});
		});
}

function onCreateEntityDialog_btnOkClick() {
	require([ 'dijit/registry', 'dojo/request/xhr'],
		function(registry, request) {
			var txtName = registry.byId('createEntityDialog_textEntityName');
			if (!txtName.isValid()) {
				return;
			}
			
			var tree = registry.byId('entityTree');
			var groupName = getSelectedGroupName(tree, 'ENTITY_GROUP');
			// group name can't be null, as script creation is enabled only if some group is selected
			var entityShortName = txtName.get('value');
			var entityName = groupName + '::' + entityShortName;
			
			var tabContainer = registry.byId('tabContainer');
			var entityTab = tabContainer.selectedChildWidget;
			var grid = entityTab.getChildren()[1];
			var entitySource = buildEntitySource(grid);
			var entityBody = escapeEntitySource(entitySource);
			
			request('service/entity/admin/create', {
					headers: { 'Content-Type': 'application/json' },
					method: 'POST',
					data: '{"name": "' + entityName +'", "body": "' + entityBody + '"}'
				}).then(function(resp){
					tree.refreshChildrenSubTree(groupName);
					hideDialog('createEntityDialog');
					var tabIdx = tabContainer.getIndexOfChild(entityTab);
					tabContainer.removeChild(entityTab);
					entityTab.destroyRecursive();
					createEntityTab(entityShortName, 'tab_entity_' + entityName, entitySource, tabIdx);
					writeInfo('Entity [' + entityName + '] is created');
				}, function(err) {
					hideDialog('createEntityDialog');
					writeError(err);
				});
		});
}

function onUpdateEntityDialog_btnOkClick() {
	require([ 'dijit/registry', 'dojo/request/xhr'],
		function(registry, request) {
			var groupName = registry.byId('updateEntityDialog_textGroupName').get('value');
			var entityShortName = registry.byId('updateEntityDialog_textEntityName').get('value');
			var entityName = groupName + '::' + entityShortName;
			
			var tabContainer = registry.byId('tabContainer');
			var entityTab = tabContainer.selectedChildWidget;
			var grid = entityTab.getChildren()[1];
			var entitySource = buildEntitySource(grid);
			var entityBody = escapeEntitySource(entitySource);
			
			request('service/entity/admin/update', {
					headers: { 'Content-Type': 'application/json' },
					method: 'POST',
					data: '{"name": "' + entityName +'", "body": "' + entityBody + '"}'
				}).then(function(resp){
					hideDialog('updateEntityDialog');
					var tabIdx = tabContainer.getIndexOfChild(entityTab);
					tabContainer.removeChild(entityTab);
					entityTab.destroyRecursive();
					createEntityTab(entityShortName, 'tab_entity_' + entityName, entitySource, tabIdx);
					writeInfo('Entity [' + entityName + '] is updated');
				}, function(err) {
					hideDialog('updateEntityDialog');
					writeError(err);
				});
		});
}

function runScript() {
	require([ 'dijit/registry', 'dojo/request/xhr'],
		function(registry, request) {
			var dialog = registry.byId('scriptParamsDialog');
			var paramValues = [];
			
			if (dialog.parameters) {
				var validValues = true;
				for (paramName in dialog.parameters) {
					var txt = registry.byId('txtScriptInputParam_' + paramName);
					validValues = validValues && txt.isValid();
					var value = txt.get('value');
					paramValues.push('"' + value + '"');
				}
				
				if (!validValues) {
					return;
				} 
			}
			
			var tab = registry.byId('tabContainer').selectedChildWidget;
			var scriptName = tab.objectInfo.group + '::' + tab.objectInfo.name;
			
			request('service/script/runtime/run', {
					headers: { 'Content-Type': 'application/json' },
					method: 'POST',
					data: '{"name": "' + scriptName + '", "inputParameters": [' + paramValues + ']}'
				}).then(function(resp){
					delete dialog.parameters;
					dialog.hide();
					writeInfo('Script [' + scriptName + '] is completed');
				}, function(err) {
					dialog.hide();
					writeError(err, 'Script [' + scriptName + '] is failed');
				});
	});	
}

function onScriptParamsDialog_show() {
	require([ 'dijit/registry', 'dojox/layout/TableContainer', 'dijit/form/ValidationTextBox', 'dojo/domReady!'],
		function(registry, TableContainer, ValidationTextBox) {
			var paramsPane = registry.byId('scriptParamsDialog_paramsTablePane');
			// Re-creation of parameters table using the current set of script parameters
			dojo.forEach(registry.findWidgets(paramsPane.domNode), function(w) {
				w.destroyRecursive();
			});
			
			var dialog = registry.byId('scriptParamsDialog');
			
			var paramsTable = new TableContainer({
				cols: 1
			});
			
			for (var paramName in dialog.parameters) {
				var paramType = dialog.parameters[paramName];
				
				var txt = new ValidationTextBox({
					id: 'txtScriptInputParam_' + paramName,
					required: true,
					title: paramName + ' (' + paramType + '):',
					style: 'width: 300px;',
					missingMessage: 'The value is required'
				});
				
				paramsTable.addChild(txt);	
			}

			paramsPane.addChild(paramsTable);
			paramsTable.startup();	
			dialog.resize();	
	});
}

function onRunScriptBtnClick() {
	require([ 'dijit/registry', 'dojo/request/xhr', 'dojo/domReady!'],
		function(registry, request) {
			var tab = registry.byId('tabContainer').selectedChildWidget;
			var scriptName = tab.objectInfo.group + '::' + tab.objectInfo.name;
			
			request('service/script/admin/getInputParameters', {
					headers: { 'Content-Type': 'text/plain' },
					method: 'POST',
					data: scriptName
				}).then(function(resp){
					var params = dojo.fromJson(resp);
					
					if (!isEmptyObject(params)) {
						var dialog = registry.byId('scriptParamsDialog');
						dialog.parameters = params;
						dialog.show();
					} else {
						runScript();
					}
				}, function(err) {
					writeError(err, 'Failed to determine the input parameters for script [' 
						+ scriptName + ']');
				});
			
		});
}

function buildEntitySource(grid) {
	var entity = {};
	for (var i = 0; i < grid.rowCount; i++) {
		var item = grid.getItem(i);
		entity[item.fldName] = item.fldType; 
	}
	
	return dojo.toJson(entity);
}

function initDiagramElementPropertiesPane() {
	require(['dojo/ready', 'dijit/registry'], function(ready, registry) {
		ready(function(){
			var elmPropsPane = registry.byId('elementPropertiesTab');
			var tabContainer = registry.byId('bottomTabContainer');
			tabContainer.removeChild(elmPropsPane);
		});
	});

}

function initFlowEditorEventHandlers() {
	require(['dijit/registry'], function(registry) {
		
		var flowSourceChangeHandler = function(event) {
			var activeTab = bfa.activeFlowEditorTab;
			var btnSave = activeTab.getChildren()[0].getChildren()[0]; 
			
			if (btnSave.get('disabled')) {
				btnSave.set('disabled', false);
				activeTab.set('title', '*' + activeTab.title);
			}
		}
		
		document.addEventListener('diagramElementSelect', function(event) {
			var elmPropsPane = registry.byId('elementPropertiesTab');
			var bottomTabContainer = registry.byId('bottomTabContainer');
			bottomTabContainer.addChild(elmPropsPane);
			bottomTabContainer.selectChild(elmPropsPane);
		});
		
		document.addEventListener('diagramElementUnselect', function(event) {
			var elmPropsPane = registry.byId('elementPropertiesTab');
			var bottomTabContainer = registry.byId('bottomTabContainer');
			bottomTabContainer.removeChild(elmPropsPane);
			bottomTabContainer.selectChild(bottomTabContainer.getChildren()[0]);
		});
		
		document.addEventListener('flowSourceChange', flowSourceChangeHandler);
		document.addEventListener('diagramElementMove', flowSourceChangeHandler);
		
	});

}

function createScriptTab(tabTitle, tabId, scriptSource, indexInContainer) {
	require([ 'dijit/registry', 'dijit/layout/ContentPane', 'dijit/Toolbar', 
	          'dijit/form/Button', 'dijit/layout/TabContainer', 
	          'dijit/layout/BorderContainer', 'dojo/dom-construct', 'dojo/domReady!'],
		function(registry, ContentPane, Toolbar, Button, TabContainer, BorderContainer, domConstruct) {
			var tabContainer = registry.byId('tabContainer');
			var groupName = getSelectedGroupName(registry.byId('scriptTree'), 'SCRIPT_GROUP');
			
			var tab = new ContentPane({
				id: tabId,
				title: tabTitle,
				closable: true,
				iconClass: 'dijitLeaf',
				className: 'nestedPane',
				style: 'overflow: auto;',
				objectInfo: {name: tabTitle, group: groupName}
			});
			
			var isExistingScript = (tabId != undefined);
			
			var btnSave = new Button({
				label: 'Save',
				showLabel: false,
				iconClass: 'dijitIconSave'
			});
			
			var btnRun = new Button({
				label: 'Run',
				showLabel: false,
				iconClass: 'toolbarIconRun',
				onClick: onRunScriptBtnClick
			});
			
			if (isExistingScript) {
				//btnSave.set('disabled', true);
				btnSave.onClick = showUpdateScriptDialog;
			} else {
				//btnRun.set('disabled', true);
				btnSave.onClick = showCreateScriptDialog;
			}
			btnSave.set('disabled', true);
			
			btnSave.startup();
			btnRun.startup();

			var toolBar = new Toolbar({
				className: 'nestedPane'
			});
			toolBar.addChild(btnSave);
			toolBar.addChild(btnRun);
			toolBar.startup();
			
			var flowEditorArea = new TabContainer({
				tabStrip: true,
				region: 'bottom',
				//FIXME: 95% is a magic number here. 100% causes overflow
				style: 'height: 95%;',
				className: 'nestedPane flowEditorArea',
				tabPosition: 'bottom'
			});
			flowEditorArea.startup();
			
			var scriptTab = new ContentPane({
				title: 'Source',
				style: 'overflow: auto;',
				className: 'nestedPane'
			});
			scriptTab.startup();
			
			var chartTab = new ContentPane({
				title: 'Chart View',
				style: 'overflow: auto;',
				className: 'nestedPane',
				chartCreated: false,
				
				onShow: function() {
					if (!this.chartCreated && isExistingScript) {
						var scriptName = groupName + '::' + tabTitle;
						drawFlowChart(scriptName, this.id);
						this.chartCreated = true;
					}
				}
			});
			chartTab.startup();
			
			var designerTab = new ContentPane({
				title: 'Designer',
				className: 'nestedPane'	
			});
			
			var drawingContainer = new BorderContainer({
				style: 'overflow: auto;',
				className: 'nestedPane'
			});
			
			var canvasPane = new ContentPane({
				style: 'overflow: auto;',
				className: 'nestedPane',
				region: 'center',
				
				onShow: function() {
					if (!this.canvas) {
						//TODO: calculate canvas size
						this.canvas = SVG(this.id).size(1500, 500);
						
						if (isExistingScript) {
							var scriptName = groupName + '::' + tabTitle;
							drawFlowDiagram(scriptName, this.canvas);
						} else {
							drawEmptyDiagram(this.canvas);
						}
					}
				},
			
				onFocus: function() {
					// global var defined in flow_editor.js
					draw = this.canvas;
				}
			});
			canvasPane.startup();
			
			var palettePane = new ContentPane({
				style: 'overflow: auto;',
				className: 'nestedPane',
				region: 'right',
			});
			
			var paletteButtonNames = ['Start', 'Action', 'Condition', 'Sub-Process', 'End'];
			
			paletteButtonNames.forEach(function(name){
				var btnId = (name == 'Sub-Process') ? 'Subprocess' : name;
				
				var btn = new Button({ 
					label: name,
					showLabel: false,
					iconClass: 'toolbarIconFlow' + btnId
				});
				btn.on('click', window['btnNew' + btnId + 'OnClick']);
				btn.startup();
				
				domConstruct.place(btn.domNode, palettePane.domNode);
				domConstruct.place('<br/>', palettePane.domNode);
			});
			
			domConstruct.place('<hr/>', palettePane.domNode);
			
			var btnDeleteElm = new Button({ 
				label: 'Delete',
				showLabel: false,
				iconClass: 'toolbarIconDeleteEntity'
			});
			btnDeleteElm.startup();
			
			domConstruct.place(btnDeleteElm.domNode, palettePane.domNode);
			
			palettePane.startup();
			
			drawingContainer.addChild(canvasPane);
			drawingContainer.addChild(palettePane);
			drawingContainer.startup();
			
			designerTab.addChild(drawingContainer);
			designerTab.startup();

			flowEditorArea.addChild(scriptTab);
			flowEditorArea.addChild(chartTab);
			flowEditorArea.addChild(designerTab);
			flowEditorArea.startup();
			
			tab.addChild(toolBar);
			tab.addChild(flowEditorArea);
			
			if (indexInContainer > -1) {
				tabContainer.addChild(tab, indexInContainer);
			} else {
				tabContainer.addChild(tab);
			}
			
			tabContainer.selectChild(tab);
			
			bfa.activeFlowEditorTab = tab;
			
			createScriptEditor(scriptSource, scriptTab);
	});
}

function createEntityTab(tabTitle, tabId, entitySource, indexInContainer) {
	require([ 'dijit/registry', 'dijit/layout/ContentPane', 'dijit/Toolbar', 
			'dijit/form/Button', 'dojo/domReady!'],
		function(registry, ContentPane, Toolbar, Button) {
			var tabContainer = registry.byId('tabContainer');
			var groupName = getSelectedGroupName(registry.byId('entityTree'), 'ENTITY_GROUP');
			
			var tab = new ContentPane({
				id: tabId,
				title: tabTitle,
				closable: true,
				iconClass: 'toolbarIconEntity',
				style: 'overflow: auto;',
				objectInfo: {name: tabTitle, group: groupName}
			});
			
			var isExistingEntity = (tabId != undefined);
			
			var btnSave = new Button({
				label: 'Save',
				showLabel: false,
				iconClass: 'dijitIconSave'
			});
			
			if (isExistingEntity) {
				btnSave.set('disabled', true);
				btnSave.onClick = showUpdateEntityDialog;
			} else {
				btnSave.onClick = showCreateEntityDialog;
			}
			
			var btnNewField = new Button({
				label: 'New field',
				showLabel: false,
				iconClass: 'toolbarIconAdd',
				onClick: function() {
					showNewEntityFieldDialog();
				}
			});
								
			var toolBar = new Toolbar({ className: 'nestedPane' });
			toolBar.addChild(btnSave);
			toolBar.addChild(btnNewField);
			toolBar.startup();
			tab.addChild(toolBar);
			
			createEntityGrid(tab, entitySource, isExistingEntity);
			
			if (indexInContainer > -1) {
				tabContainer.addChild(tab, indexInContainer);
			} else {
				tabContainer.addChild(tab);
			}
			tabContainer.selectChild(tab);
	});
}

function createEntityGrid(tab, entitySource, isExistingEntity) {
	require(['dijit/registry', 'dojox/grid/DataGrid', 'dojo/data/ObjectStore', 
		'dojo/store/Memory', 'dijit/form/Button', 'dojo/domReady!'],
		function(registry, DataGrid, ObjectStore, Memory, Button) {
		
			function getDeleteButton() {
				var btn = new Button({
					label: 'Delete field',
					showLabel: false,
					iconClass: 'toolbarIconDeleteEntity',
					onClick: function() {
						var entityTab = registry.byId('tabContainer').selectedChildWidget;
						var grid = entityTab.getChildren()[1];
						var selectedItem = grid.selection.getSelected()[0];
						var store = grid.store;
						store.deleteItem(selectedItem);
						store.save();
						grid.render();
					}
				});
				btn._destroyOnRemove=true;
				
				return btn;
			}
			
			var gridItems = [];
			if (entitySource) {
				var entity = dojo.fromJson(entitySource);
				for (var key in entity) {
					gridItems.push({fldName: key, fldType: entity[key]});
				}
			}
			
			var data = {identifier: 'fldName', items: gridItems };
			var store = new ObjectStore({ 
				handleStoreChange: function() {
					if (isExistingEntity && !this.modified) {
						var btnSave = tab.getChildren()[0].getChildren()[0];
						btnSave.set('disabled', false);
						tab.set('title', '*' + tab.title);
						this.modified = true;
					}
				},
				
				objectStore: new Memory({ data: data.items, idProperty: 'fldName' }),
				modified: false,
				onDelete: function() { this.handleStoreChange(); },
				onNew: function() { this.handleStoreChange(); } 
			});
			
			var layout = [{
				defaultCell: {styles: 'text-align: left;', width: '300px'},
				cells: [
					{name: 'Field Name', field: 'fldName'},
					{name: 'Field Type', field: 'fldType'},
					{name: ' ', field: 'fldDelete', formatter: getDeleteButton, 
						styles: 'text-align: center; margin: 0; padding: 0;', width: '40px'}
				]
			}];

			var grid = new DataGrid({
				store: store,
				query: {fldName: '*'},
				structure: layout,
				selectionMode: 'single',
				autoWidth: true,
				autoHeight: true,
				
				onRowMouseOver: function(e) {
					// The selection is needed for "Delete field" button. See getDeleteButton()
					var selectedIndex = e.grid.selection.selectedIndex;
					if (selectedIndex != e.rowIndex) {
						e.grid.selection.setSelected(e.rowIndex, true);
						e.grid.selection.setSelected(selectedIndex, false);
					}
				}
			});

			tab.addChild(grid);
			grid.startup();
	});
}

function onNewEntityFieldDialog_btnOk() {
	require([ 'dijit/registry' ],
		function(registry) {
			var txtName = registry.byId('newEntityFieldDialog_textName');
			var txtType = registry.byId('newEntityFieldDialog_textType');
			
			if (!(txtName.isValid() && txtType.isValid())) {
				return;
			}
			
			var tabContainer = registry.byId('tabContainer');
			var entityTab = tabContainer.selectedChildWidget;
			var grid = entityTab.getChildren()[1];
			grid.store.newItem({fldName: txtName.get('value'), fldType: txtType.get('value')});
			grid.store.save();
			grid.render();
			
			hideDialog('newEntityFieldDialog');
	});	
}

function createActionTab(tabTitle, tabId, actionInfo, indexInContainer) {
	require([ 'dijit/registry', 'dijit/layout/ContentPane', 'dijit/Toolbar', 
			'dijit/form/Button', 'dijit/form/TextBox', 'dojo/dom-construct', 'dojo/domReady!'],
		function(registry, ContentPane, Toolbar, Button, TextBox, domConstruct) {
			var tabContainer = registry.byId('tabContainer');
			var groupName = getSelectedGroupName(registry.byId('actionTree'), 'ACTION_GROUP');
			
			var tab = new ContentPane({
				id: tabId,
				title: tabTitle,
				closable: true,
				iconClass: 'toolbarIconAction',
				style: 'overflow: auto;',
				objectInfo: {name: tabTitle, group: groupName}
			});
			
			var isExistingAction = (tabId != undefined);
			
			var btnSave = new Button({
				label: 'Update',
				showLabel: false,
				iconClass: 'toolbarIconUpdate',
				onClick: function() {
					showUpdateActionDialog();
				}
			});
			if (isExistingAction) {
				btnSave.set('disabled', false);
			}
								
			var toolBar = new Toolbar({ className: 'nestedPane' });
			toolBar.addChild(btnSave);
			toolBar.startup();
			tab.addChild(toolBar);
			
			var action = dojo.fromJson(actionInfo);
			
			var txtClassName = new TextBox({
				disabled: true,
				value: action.implementationClassName,
				style: 'width: 500px;'
			});
			txtClassName.startup();
			tab.addChild(txtClassName);
			
			domConstruct.place('<hr/><label for="' + txtClassName.id + '">Implementation class: &nbsp;&nbsp;</label>', 
				txtClassName.domNode, 'before');
				
			if (action.dependencies.length > 0) {
				domConstruct.place('<br/><br/>', txtClassName.domNode, 'after');
				createActionDependenciesGrid(tab, action.dependencies);	
			} else {
				domConstruct.place('<br/><br/><label>No dependencies</label>', txtClassName.domNode, 'after');
			}	
			
			if (indexInContainer > -1) {
				tabContainer.addChild(tab, indexInContainer);
			} else {
				tabContainer.addChild(tab);
			}
			tabContainer.selectChild(tab);
	});
}

function showCreateActionDialog() {
	require([ 'dijit/registry'],
		function(registry) {
			var tree = registry.byId('actionTree');
			var selectedGroupName = getSelectedGroupName(tree, 'ACTION_GROUP');
			var groupText = registry.byId('createActionDialog_textGroupName');
			var nameText = registry.byId('createActionDialog_textActionName');
			var fileText = registry.byId('createActionDialog_textFileName');
			
			groupText.set('value', selectedGroupName);
			nameText.set('value', '');
			nameText.reset();
			fileText.set('value', '');
			fileText.reset();
			registry.byId('createActionPackageUploader').reset();
			registry.byId('createActionDialog').show();
		});
}

function onCreateActionForm_selectFile(files, uploader) {
	require([ 'dijit/registry' ],
		function(registry) {
			registry.byId('createActionDialog_textFileName').set('value', files[0].name);
	});
}

function onCreateActionForm_uploadCompele(event, uploader) {
	require([ 'dijit/registry', 'dojo/request/xhr' ],
		function(registry, request) {
			// event is a response from server
			var status = event.value;
			
			if (status === 'OK') {
				var tree = registry.byId('actionTree');
				var groupName = getSelectedGroupName(tree, 'ACTION_GROUP');
				
				tree.refreshChildrenSubTree(groupName);
				hideDialog('createActionDialog');
				
				var actionShortName = registry.byId('createActionDialog_textActionName').get('value');
				var actionName = groupName + '::' + actionShortName;

				request('service/action/admin/getInfo', {
					headers: { 'Content-Type': 'application/json' },
					method: 'POST',
					data: '{"name": "' + actionName + '"}'
				}).then(function(resp){
					createActionTab(actionShortName, 'tab_action_' + actionName, resp);
					writeInfo('Action [' + actionName + '] is uploaded');
				}, function(err) {
					writeError(err, 'Failed to get action [' + actionName + '] info from server');
				});
			} else {
				hideDialog('createActionDialog');
				writeErrorMessage(status);
			}
	});
}

function onCreateActionForm_btnOk() {
	require([ 'dijit/registry' ],
		function(registry) {
			var form = registry.byId('createActionForm');
			
			if (form.validate()) {					
				var txtName = registry.byId('createActionDialog_textActionName');
				var tree = registry.byId('actionTree');
				var groupName = getSelectedGroupName(tree, 'ACTION_GROUP');
				// group name can't be null, as action creation is enabled only if some group is selected
				var actionShortName = txtName.get('value');
				var actionName = groupName + '::' + actionShortName;
				// Setting form's "name" parameter
				registry.byId('createActionDialog_actionFullName').set('value', actionName);
				
				var uploader = registry.byId('createActionPackageUploader');
				uploader.submit(form);
			}
	});	
}

function showUpdateActionDialog() {
	require([ 'dijit/registry'],
		function(registry) {
			var tab = registry.byId('tabContainer').selectedChildWidget;
			registry.byId('updateActionDialog_textGroupName').set('value', tab.objectInfo.group);
			registry.byId('updateActionDialog_textActionName').set('value', tab.objectInfo.name);
			
			var fileText = registry.byId('updateActionDialog_textFileName');
			fileText.set('value', '');
			fileText.reset();
			registry.byId('updateActionPackageUploader').reset();
			registry.byId('updateActionDialog').show();
		});
}

function onUpdateActionForm_selectFile(files, uploader) {
	require([ 'dijit/registry' ],
		function(registry) {
			registry.byId('updateActionDialog_textFileName').set('value', files[0].name);
	});
}

function onUpdateActionForm_uploadCompele(event, uploader) {
	require([ 'dijit/registry', 'dojo/request/xhr' ],
		function(registry, request) {
			// event is a response from server
			var status = event.value;
			
			if (status === 'OK') {
				hideDialog('updateActionDialog');
				
				var groupName = registry.byId('updateActionDialog_textGroupName').get('value');
				var actionShortName = registry.byId('updateActionDialog_textActionName').get('value');
				var actionName = groupName + '::' + actionShortName;

				request('service/action/admin/getInfo', {
					headers: { 'Content-Type': 'application/json' },
					method: 'POST',
					data: '{"name": "' + actionName + '"}'
				}).then(function(resp){
					// Re-creating action tab
					var tabContainer = registry.byId('tabContainer');
					var actionTab = tabContainer.selectedChildWidget;
					var tabIdx = tabContainer.getIndexOfChild(actionTab);
					tabContainer.removeChild(actionTab);
					actionTab.destroyRecursive();
					createActionTab(actionShortName, 'tab_action_' + actionName, resp, tabIdx);
					writeInfo('Action [' + actionName + '] is updated');
				}, function(err) {
					writeError(err, 'Failed to get action [' + actionName + '] info from server');
				});
			} else {
				hideDialog('updateActionDialog');
				writeErrorMessage(status);
			}
	});
}

function onUpdateActionForm_btnOk() {
	require([ 'dijit/registry' ],
		function(registry) {
			var form = registry.byId('updateActionForm');
			
			if (form.validate()) {	
				var groupName = registry.byId('updateActionDialog_textGroupName').get('value');
				var actionShortName = registry.byId('updateActionDialog_textActionName').get('value');
				var actionName = groupName + '::' + actionShortName;
				// Setting form's "name" parameter
				registry.byId('updateActionDialog_actionFullName').set('value', actionName);
				
				var uploader = registry.byId('updateActionPackageUploader');
				uploader.submit(form);
			}
	});	
}

function createActionDependenciesGrid(tab, dependencies) {
	require(['dijit/registry', 'dojox/grid/DataGrid', 'dojo/data/ObjectStore', 'dojo/store/Memory', 'dojo/domReady!'],
		function(registry, DataGrid, ObjectStore, Memory) {
		
			var gridItems = [];
			for (var i = 0; i < dependencies.length; i++) {
				gridItems.push({fldDep: dependencies[i]});
			}
		
			var store = new ObjectStore({ objectStore: new Memory({ data: gridItems, idProperty: 'fldDep' }) });
			
			var layout = [{
				defaultCell: {styles: 'text-align: left;', width: '300px'},
				cells: [
					{name: 'Dependencies', field: 'fldDep'}
				]
			}];

			var grid = new DataGrid({
				store: store,
				query: {fldDep: '*'},
				structure: layout,
				selectionMode: 'single',
				autoWidth: true,
				autoHeight: true
			});

			tab.addChild(grid);
			grid.startup();
	});
}		

function createTree(objectType, itemDblClickHandler, leafIconClass, itemDblClickOperationName) {
	require([ 'dojo/request/xhr', 'dojo/dom', 'dojo/store/Memory', 'dijit/tree/ObjectStoreModel', 'dijit/Tree', 'dojo/store/Observable', 
		'dijit/registry', 'dojo/domReady!'], 
		function(request, dom, Memory, ObjectStoreModel, Tree, Observable, registry) {
		
			var rootName = '__root';
			
			var treeStore = new Memory({
				data: [{name: rootName}],
				idProperty: 'name',
				
				getChildren: function(object){
					return request('service/' + objectType + '/admin/getItems', {
						headers: { 'Content-Type': 'text/plain' },
						method: 'POST',
						data: object.name,
						handleAs: 'json'
					}).then(function(resp) {
						return resp;
					}, function(err) {
						writeError(err);
					});
				}
			});
			
			var treeModel = new ObjectStoreModel({
				store: treeStore,
				getLabel: function(item){ return item.title; },
				mayHaveChildren: function(item){ return item.type == objectType.toUpperCase() + '_GROUP'; },
				query: {name: rootName}
			});
			
			tree = new Tree({
				id: objectType + 'Tree',
				model: treeModel,
				showRoot: false,
				openOnClick: false,
				rootName: rootName,
				style: 'height: 100%;',
				
				onFocus: function() {
					var btnNew = registry.byId(objectType + 'Toolbar_new');
					if (btnNew.disabled) {
						btnNew.set('disabled', false);
					}
				},
				
				onClick: function(item, node) {
					//Warning! Depends on internal implementation of Tree: _expandNode() function
					if (this.model.mayHaveChildren(item)) {
						if (node.isExpanded) {
							node.collapse();	
						} else {
							this._expandNode(node);
						}
					}
					
					var btnDeleteGroup = registry.byId(objectType + 'Toolbar_deleteGroup');
					var btnDelete = registry.byId(objectType + 'Toolbar_delete');
					if (this.model.mayHaveChildren(item)) {
						btnDeleteGroup.set('disabled', false);
						btnDelete.set('disabled', true);
					} else {
						btnDeleteGroup.set('disabled', true);
						btnDelete.set('disabled', false);
					}
				},
				
				onDblClick: function(item) {
					if (this.model.mayHaveChildren(item)) {
						return;
					}
					
					var tabContainer = registry.byId('tabContainer');
					var tabId = 'tab_' + objectType + '_' + item.name;
					
					var oldTab = registry.byId(tabId);
					if (oldTab) {
						tabContainer.selectChild(oldTab);
						return;
					}
					
					request('service/' + objectType + '/admin/' + itemDblClickOperationName, {
						headers: { 'Content-Type': 'application/json' },
						method: 'POST',
						data: '{"name": "' + item.name + '"}'
					}).then(function(resp){
						itemDblClickHandler(item.title, tabId, resp);
					}, function(err) {
						writeError(err);
					});
				},

				refreshChildrenSubTree: function(itemId) {
					//Warning! Depends on internal implementation of Tree and ObjectStoreModel
					delete this.model.childrenCache[itemId];
					var node = this._itemNodesMap[itemId][0];
					delete node._loadDeferred;
					this._expandNode(node);
				}
			});
			
			if (leafIconClass) {
				tree.getIconClass = function(item, opened) {
					return this.model.mayHaveChildren(item) ? 
						(opened ? 'dijitFolderOpened' : 'dijitFolderClosed') : leafIconClass;
				}	
			}

			tree.placeAt(dom.byId(objectType + 'TreeContainer'));
			tree.startup();
	});	
}