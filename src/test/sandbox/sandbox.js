function LineGroup(line, flowId) {
	this.constructor.call(this, SVG.create('lineGroup'));
	
	this.flowId = flowId;
	draw.state.addLineGroup(this);
	
	var DRAG_POINT_DIAMETER = 6;
	
	line.group = this;
	line.on('mousedown', mouseDown);
	line.on('mouseover', mouseOver);
	line.on('mouseout', mouseOut);
	
	// head is a line with arrow head
	this.head = line;
	this.tail = line;
	this.on('unselect', unselect);
	
	this.getHead = function() {
		return this.head;
	};
	
	this.getTail = function() {
		return this.tail;
	};
	
	this.addAfter = function(newLine, line) {
		var oldNext = line.nextLine;
		line.nextLine = newLine;
		if (oldNext) {
			newLine.nextLine = oldNext;
		} else {
			this.tail = newLine;
		}
		
		newLine.group = this;
		newLine.on('mousedown', mouseDown);
		newLine.on('mouseover', mouseOver);
		newLine.on('mouseout', mouseOut);
		selectLine(newLine);
	};
	
	this.remove = function() {
		var line = this.head;
		do {
			line.remove();
			line = line.nextLine;
		} while (line);
		
		draw.state.removeLineGroup(this);
	};
	
	this.getState = function() {
		var state = {
			flowId: this.flowId,
			inElement: this.tail.incomingVertex.flowId,
			outElement: this.head.outgoingVertex.flowId,
			lines: []	
		};
		
		var line = this.head;
		do {
			var lineCoords = [line.attr('x1'), line.attr('y1'), 
			                  line.attr('x2'), line.attr('y2')];
			state.lines.push(lineCoords);
			line = line.nextLine;
		} while (line);
		
		return state;
	}
	
	function selectLine(line) {
		if (line.hasClass('unselectedLine')) {
			line.removeClass('unselectedLine');
			if (line.arrowHead) {
				line.arrowHead.removeClass('unselectedArrowHead');
			}
		}
		
		line.addClass('selectedLine');
		if (line.arrowHead) {
			line.arrowHead.addClass('selectedArrowHead');
		}
	}
	
	function unselectLine(line) {
		if (line.hasClass('selectedLine')) {
			line.removeClass('selectedLine');
			if (line.arrowHead) {
				line.arrowHead.removeClass('selectedArrowHead');
			}
		}
		
		line.addClass('unselectedLine');
		if (line.arrowHead) {
			line.arrowHead.addClass('unselectedArrowHead');
		}
	}

	function unselect() {
		var line = this.head;
		do {
			unselectLine(line);
			line = line.nextLine;
		} while (line);
		
		deleteDragPoints(this);
		this.selected = false;
	}
	
	function mouseDown(event) {
		currentX = event.clientX;
		currentY = event.clientY;
		
		var group = this.group;
		
		if (!group.selected) {
			if (selectedElement != null) {
				selectedElement.fire('unselect');
			}
			
			group.selected = true;
			selectedElement = group;
			
			var line = group.head;
			do {
				selectLine(line);
				line = line.nextLine;
			} while (line);
			
			drawDragPoints(group);
		}

		stopEventPropagation(event);
	}
	
	function addRemoveCssClass(lineInGroup, clsToRemove, clsToAdd) {
		var group = lineInGroup.group;
		var line = group.head;
		do {
			line.removeClass(clsToRemove);
			line.addClass(clsToAdd);
			line = line.nextLine;
		} while (line);
	}
	
	function mouseOver(event) {
		addRemoveCssClass(this, 'mouseOutLine', 'mouseOverLine');
		stopEventPropagation(event);
	}
	
	function mouseOut(event) {
		addRemoveCssClass(this, 'mouseOverLine', 'mouseOutLine');
		stopEventPropagation(event);
	}
	
	function drawDragPoints(group) {
		var points = [];
		
		var line = group.head;
		var prevLine;
		do {
			var point = drawPoint(line.cx(), line.cy());
			points.push(point);
			line.dragPoint = point;
			point.line = line;
						
			var nextLine = line.nextLine; 
			if (nextLine) {
				point = drawPoint(line.attr("x1"), line.attr("y1"));
				points.push(point);
				point.outLine = line;
				point.inLine = nextLine;
			}
					
			line = nextLine;
		} while (line);
		
		group.dragPoints = points;
	} 
	
	function drawMidDragPoint(line) {
		var point = drawPoint(line.cx(), line.cy());
		line.dragPoint = point;
		point.line = line;
		line.group.dragPoints.push(point);
	}
	
	function moveMidDragPoint(line) {
		line.dragPoint.cx(line.cx()).cy(line.cy());
	}
	
	function drawPoint(cx, cy) {
		var point = draw.circle(DRAG_POINT_DIAMETER);
		point.cx(cx).cy(cy);
		point.addClass('dragPoint');
		point.on('mouseover', dragPointMouseOver);
		point.on('mouseout', dragPointMouseOut);
		point.on('mousedown', dragPointMouseDown);
		point.on('mousemove', dragPointMouseMove);
		
		return point;
	}
	
	function deleteDragPoints(group) {
			group.dragPoints.forEach(function(point) {
				point.remove();
			});
			
			delete group.dragPoints;
	}
	
	function removeLineText(line) {
		var elms = line.label;
		elms[0].remove();
		elms[1].remove();
		elms = [];
		delete line.label;
	}

	function getLineTextValue(line) {
		return line.label[1].text();
	}

	function dragPointMouseOver(event) {
		if (this.hasClass('unselectedDragPoint')) {
			this.removeClass('unselectedDragPoint');
		}
		this.addClass('selectedDragPoint');

		stopEventPropagation(event);
	}

	function dragPointMouseOut(event) {
		if (this.hasClass('selectedDragPoint')) {
			this.removeClass('selectedDragPoint');
		}
		this.addClass('unselectedDragPoint');
		
		stopEventPropagation(event);
	}

	function dragPointMouseDown(event) {
		currentX = event.clientX;
		currentY = event.clientY;
		
		stopEventPropagation(event);
	}

	function dragPointMouseMove(event) {
		if (isLeftMouseButtonPressed(event)) {
			
			var dx = event.clientX - currentX;
			var dy = event.clientY - currentY;
			
			this.cx(this.cx()+dx).cy(this.cy()+dy);
			
			if (this.line) {
				var line = this.line;
				var newLine = draw.line(line.attr("x1"), line.attr("y1"), currentX, currentY)
							.stroke({width: 2});
				var group = line.group;
				group.addAfter(newLine, line);
				line.plot(currentX, currentY, line.attr("x2"), line.attr("y2"));
				
				if (line.incomingVertex) {
					newLine.incomingVertex = line.incomingVertex;
					delete line.incomingVertex;
				}
				
				if (line.label) {
					var text = getLineTextValue(line);
					removeLineText(line);
					drawLineText(newLine, text);
				}
				
				this.inLine = newLine;
				this.outLine = line;
				
				// This is not mid point any more
				delete this.line;
				drawMidDragPoint(line);
				drawMidDragPoint(newLine);
			} else {
				var inLine = this.inLine;
				var outLine = this.outLine;
				inLine.plot(inLine.attr("x1"), inLine.attr("y1"), this.cx(), this.cy());
				outLine.plot(this.cx(), this.cy(), outLine.attr("x2"), outLine.attr("y2"));
				moveMidDragPoint(inLine);
				moveMidDragPoint(outLine);
			}
			
			if (this.outLine.outgoingVertex) {
				moveLineGroupHead(this.outLine.outgoingVertex, this.outLine);
			}
			
			if (this.inLine.incomingVertex) {
				moveLineGroupTail(this.inLine.incomingVertex, this.inLine);
			}
			
			currentX = event.clientX;
			currentY = event.clientY;
		}
		
		stopEventPropagation(event);
	}

}

LineGroup.prototype = Object.create(SVG.Shape.prototype);

SVG.RecordableRect = function() {
	SVG.Rect.call(this);
	this.type = 'recordableRect';
};

SVG.RecordableCircle = function() {
	SVG.Circle.call(this);
	this.type = 'recordableCircle';
};

SVG.RecordablePolygon = function() {
	SVG.Polygon.call(this);
	this.type = 'recordablePolygon';
};

SVG.RecordableRect.prototype = Object.create(SVG.Rect.prototype);
SVG.RecordableCircle.prototype = Object.create(SVG.Circle.prototype);
SVG.RecordablePolygon.prototype = Object.create(SVG.Polygon.prototype);

SVG.extend(SVG.RecordableRect, {
	remove: function() {
		draw.state.removeRect(this);
		SVG.Rect.prototype.remove.call(this);
	},
	
	getState: function() {
		var state = {
			id: this.flowId,	
			cx: this.cx(),
			cy: this.cy(),
			height: this.height(),
			width: this.width(),
			label: this.text.text(),
			inLineGroupIds: [],
			outLineGroupIds: []
		};
		
		if (this.subType) {
			state.subType = this.subType;
		}
		
		this.inLineGroups.forEach(function(grp) {
			state.inLineGroupIds.push(grp.flowId);
		});
		this.outLineGroups.forEach(function(grp) {
			state.outLineGroupIds.push(grp.flowId);
		});
		
		return state;
	}
});

SVG.extend(SVG.RecordableCircle, {
	remove: function() {
		draw.state.removeCircle(this);
		SVG.Circle.prototype.remove.call(this);
	},

	getState: function() {
		var state = {
			id: this.flowId,
			cx: this.cx(),
			cy: this.cy(),
			radius: this.attr('r'),
			label: this.text.text(),
			inLineGroupIds: [],
			outLineGroupIds: []
		};
		
		this.inLineGroups.forEach(function(grp) {
			state.inLineGroupIds.push(grp.flowId);
		});
		this.outLineGroups.forEach(function(grp) {
			state.outLineGroupIds.push(grp.flowId);
		});
					
		return state;
	}
});

SVG.extend(SVG.RecordablePolygon, {
	remove: function() {
		draw.state.removeDiamond(this);
		SVG.Polygon.prototype.remove.call(this);
	},

	getState: function() {
		var pa = this.array().value;
		
		var state = {
			id: this.flowId,
			cx: this.cx(),
			cy: this.cy(),
			
			points: pa[0][0] + ',' + pa[0][1] + ' ' +
					pa[1][0] + ',' + pa[1][1] + ' ' +
					pa[2][0] + ',' + pa[2][1] + ' ' +
					pa[3][0] + ',' + pa[3][1],
			
			label: this.text.text(),
			inLineGroupIds: [],
			outLineGroupIds: []
		};
		
		if (this.subType) {
			state.subType = this.subType;
		}
		
		this.inLineGroups.forEach(function(grp) {
			state.inLineGroupIds.push(grp.flowId);
		});
		this.outLineGroups.forEach(function(grp) {
			state.outLineGroupIds.push(grp.flowId);
		});
					
		return state;
	}
});

SVG.extend(SVG.Container, {
	recordableRect: function(width, height, flowId) {
		console.log('recordableRect: ' + width + ', ' + height);
		var elm = this.put(new SVG.RecordableRect).size(width, height);
		elm.flowId = flowId;
		draw.state.addRect(elm);
		
		return elm;
	},

	recordableCircle: function(size, flowId) {
		console.log('recordableCircle: ' + size);
		var elm = this.put(new SVG.RecordableCircle).rx(new SVG.Number(size).divide(2)).move(0, 0);
		elm.flowId = flowId;
		draw.state.addCircle(elm);
		
		return elm;
	},
	
	recordablePolygon: function(p, flowId) {
		console.log('recordablePolygon: ' + p);
		var elm = this.put(new SVG.RecordablePolygon).plot(p);
		elm.flowId = flowId;
		draw.state.addDiamond(elm);
		
		return elm;
	}
});


function stopEventPropagation(event)
{
   if (event.stopPropagation) {
       event.stopPropagation();
   }
   else if (window.event){
      window.event.cancelBubble = true;
   }
}

function isLeftMouseButtonPressed(event) {
    if ('buttons' in event) {
        return event.buttons === 1;
    } else if ('which' in event) {
        return event.which === 1;
    } else {
        return event.button === 1;
    }
}

function AttachPointsProvider() {

	function Point(x, y) {
		this.x = x;
		this.y = y;
	}

	// midpoints of each rectangle's edge + 1/4 width points of horizontal edges
	function getRectPoints(rect) {
		return [new Point(rect.cx()+rect.width()/2, rect.cy()),
				new Point(rect.cx(), rect.cy()+rect.height()/2),
				new Point(rect.cx()-rect.width()/4, rect.cy()+rect.height()/2),
				new Point(rect.cx()+rect.width()/4, rect.cy()+rect.height()/2),
				new Point(rect.cx()-rect.width()/2, rect.cy()),
				new Point(rect.cx(), rect.cy()-rect.height()/2),
				new Point(rect.cx()-rect.width()/4, rect.cy()-rect.height()/2),
				new Point(rect.cx()+rect.width()/4, rect.cy()-rect.height()/2)];
	}
	
	// vertex points of diamond
	function getDiamondPoints(diam) {
		var pts = diam.array().value;
		
		
		// diamond vertex points + midpoints of each diamond's edge 
		return [new Point(pts[0][0], pts[0][1]),
				new Point(pts[1][0], pts[1][1]),
				new Point(pts[2][0], pts[2][1]),
				new Point(pts[3][0], pts[3][1]),
		        new Point((pts[0][0]+pts[1][0])/2, (pts[0][1]+pts[1][1])/2),
				new Point((pts[1][0]+pts[2][0])/2, (pts[1][1]+pts[2][1])/2),
				new Point((pts[2][0]+pts[3][0])/2, (pts[2][1]+pts[3][1])/2),
				new Point((pts[3][0]+pts[0][0])/2, (pts[3][1]+pts[0][1])/2)];
	}
	
	function getCirclePoints(circ) {
		var radius = circ.height() / 2;
		
		return [new Point(circ.cx()+radius, circ.cy()),
		        new Point(circ.cx()-radius, circ.cy()),
		        new Point(circ.cx(), circ.cy()+radius),
		        new Point(circ.cx(), circ.cy()-radius)];
	}
	
	var provider = {
	
		getAttachPoints: function(element) {
			if (element.customType && element.customType == 'diamond') {
				return getDiamondPoints(element);
			}
			else if (element.customType && element.customType == 'circle') {
				return getCirclePoints(element);
			}
			else {
				return getRectPoints(element);
			}
		}
	
	};
	
	return provider;	
}

function getDistance(point1, point2) {
	return Math.sqrt(Math.pow(point1.x - point2.x, 2) + Math.pow(point1.y - point2.y, 2));
}

function determineLinePoints(elm1, elm2) {
	var pointsProvider = new AttachPointsProvider();
	var points1 = pointsProvider.getAttachPoints(elm1);
	var points2 = pointsProvider.getAttachPoints(elm2);
	
	var linePoints = {};
	var minDistance = Number.MAX_VALUE;
	
	for (var i = 0; i < points1.length; i++) {
		var point1 = points1[i];
		for (var j = 0; j < points2.length; j++) {
			var point2 = points2[j];
			var distance = getDistance(point1, point2);
			if (distance < minDistance) {
				minDistance = distance;
				linePoints.start = point1;
				linePoints.end = point2;
			}	
		}
	}
	
	return linePoints;
}

function determineSecondLinePoint(elm, firstPoint) {
	var points = new AttachPointsProvider().getAttachPoints(elm);
	var startPoint;
	
	var minDistance = Number.MAX_VALUE;
	
	for (var i = 0; i < points.length; i++) {
		var distance = getDistance(points[i], firstPoint);
		if (distance < minDistance) {
			minDistance = distance;
			startPoint = points[i];
		}	
	}
	
	return startPoint;
}

function canvasMouseDown() {
	if (selectedElement != null) {
		selectedElement.fire('unselect');
		selectedElement = null;
	}
}

function elementMouseDown(event) {
	currentX = event.clientX;
	currentY = event.clientY;

	if (!this.selected) {
		this.selected = true;
		
		if (this.hasClass('unselectedElement')) {
			this.removeClass('unselectedElement');
			this.text.removeClass('unselectedElementText');
		}
		
		this.addClass('selectedElement');
		this.text.addClass('selectedElementText');
		
		if (selectedElement != null) {
			selectedElement.fire('unselect');
		}
		
		selectedElement = this;
	}
	
	stopEventPropagation(event);
}


function moveArrowHead(line) {
	var lineStartX = line.attr('x1');
	var lineStartY = line.attr('y1');
	var lineEndX = line.attr('x2');
	var lineEndY = line.attr('y2');
	
	var angleRad = (lineEndX != lineStartX) ? 
					Math.atan(Math.abs((lineEndY - lineStartY)) / Math.abs((lineEndX - lineStartX))) : 
					Math.PI / 2;
	
	var arrowLineLength = 8;
	var arrowLineX1 = lineEndX >= lineStartX ? lineEndX - arrowLineLength * Math.cos(angleRad) : 
												lineEndX + arrowLineLength * Math.cos(angleRad);
	var arrowLineY1 = lineEndY >= lineStartY ? lineEndY - arrowLineLength * Math.sin(angleRad) : 
												lineEndY + arrowLineLength * Math.sin(angleRad);
	
	var arrowLine = draw.line(arrowLineX1, arrowLineY1, lineEndX, lineEndY);
	arrowLine.transform({rotation: 90});
	
	var matrix = new SVG.Matrix(arrowLine);
	var pos1 = new SVG.Point(arrowLine.attr('x1'), arrowLine.attr('y1'));
	pos1 = pos1.transform(matrix);
	var pos2 = new SVG.Point(arrowLine.attr('x2'), arrowLine.attr('y2'));
	pos2 = pos2.transform(matrix);
	
	line.arrowHead.plot(lineEndX + ',' + lineEndY + ' ' 
					+ pos1.x + ',' + pos1.y + ' ' 
					+ pos2.x + ',' + pos2.y)
	
	arrowLine.remove();
}

function moveLineLabel(line) {
	if (line.label) {
		var label = line.label;
		label.forEach(function(elm) {
			elm.cx(line.cx());
			elm.cy(line.cy());
		});
	}
} 

function moveSingleArrow(line, points) {
	line.plot(points.start.x, points.start.y, points.end.x, points.end.y);
	
	moveLineLabel(line);
	moveArrowHead(line);
}

function moveLineGroupTail(inElm, line) {
	var endPoint = {x: line.attr('x2'), y: line.attr('y2')};
	var startPoint = determineSecondLinePoint(inElm, endPoint);

	line.plot(startPoint.x, startPoint.y, endPoint.x, endPoint.y);
	moveLineLabel(line);
}

function moveLineGroupHead(outElm, line) {
	var startPoint = {x: line.attr('x1'), y: line.attr('y1')};
	var endPoint = determineSecondLinePoint(outElm, startPoint);
	
	line.plot(startPoint.x, startPoint.y, endPoint.x, endPoint.y);
	moveArrowHead(line);
}

function elementMouseMove(event) {
	if (this.selected && isLeftMouseButtonPressed(event)) {
		var dx = event.clientX - currentX;
		var dy = event.clientY - currentY;
		
		this.cx(this.cx() + dx).cy(this.cy() + dy);
		
		var text = this.text;
		text.cx(this.cx()).cy(this.cy());
		
		this.outLines.forEach(function(outLine){
			var outLineGroup = outLine.group;

			if (outLineGroup.head === outLineGroup.tail) {
				var inElm = this;
				var outElm = outLine.outgoingVertex;
				var points = determineLinePoints(inElm, outElm);
				moveSingleArrow(outLine, points);
			} else {
				moveLineGroupTail(this, outLineGroup.tail);
			}
		}, this);
		
		this.inLines.forEach(function(inLine){
			var inLineGroup = inLine.group;
			
			if (inLineGroup.head === inLineGroup.tail) {
				var inElm = inLine.incomingVertex;
				var outElm = this;
				var points = determineLinePoints(inElm, outElm);
				moveSingleArrow(inLine, points);
			} else {
				moveLineGroupHead(this, inLineGroup.head);
			}
		}, this);
		
		currentX = event.clientX;
		currentY = event.clientY;
	}
	
	stopEventPropagation(event);
}

function elementUnselect() {
	this.selected = false;
	
	if (this.hasClass('selectedElement')) {
		this.removeClass('selectedElement');
		this.text.removeClass('selectedElementText');
	}
	
	this.addClass('unselectedElement');
	this.text.addClass('unselectedElementText');
}

function elementTextMouseDown(event) {
	stopEventPropagation(event);
	currentX = event.clientX;
	currentY = event.clientY;
	this.containerElement.fire('mousedown'); 
}

function elementTextMouseMove(event) {
	stopEventPropagation(event);
	currentX = event.clientX;
	currentY = event.clientY;
	this.containerElement.fire('mousemove'); 
}

function elementTextDblClick(event) {
	alert(this.text());
}

function drawElementText(elm, label) {
	var text = draw.text(label);
	text.addClass('labelText');
	
	var textWidth = text.bbox().width;
	var elmWidth = elm.width();
	
	if (textWidth >= elmWidth)  {
		elm.width(textWidth + 10);
	}
	
	text.cx(elm.cx());
	text.cy(elm.cy());
	
	elm.text = text;
	text.containerElement = elm;
	
	text.on('mousedown', elementTextMouseDown);
	text.on('mousemove', elementTextMouseMove);
	text.on('dblclick', elementTextDblClick);
}

function drawLineText(line, label) {
	var textTplt = draw.text('Yes');
	
	var rect = draw.rect(10, 10);
	rect.rx(10);
	rect.ry(10);
	rect.width(textTplt.bbox().width);
	rect.height(textTplt.bbox().height);
	rect.cx(line.cx());
	rect.cy(line.cy());
	rect.addClass('labelBackground');

	textTplt.remove();
	
	var text = draw.text(label);
	text.addClass('lineLabelText');
	text.cx(line.cx());
	text.cy(line.cy());
	
	var elements = [];
	elements.push(rect);
	elements.push(text);
	
	line.label = elements;
}

function drawCircle(cx, cy, label, subType) {
	var circ = draw.recordableCircle(40);
	circ.customType = 'circle';
	circ.cx(cx);
	circ.cy(cy);
	circ.addClass('element');
	
	if (subType) {
		circ.subType = subType;
		circ.addClass(subType);
	}
	
	circ.selected = false;
	circ.inLines = [];
	circ.outLines = [];
	//TODO: remove redundant inLines/outLines and move related logic to inLineGroups/outLineGroups
	circ.inLineGroups = [];
	circ.outLineGroups = [];
	circ.on('mousedown', elementMouseDown);
	circ.on('mousemove', elementMouseMove);
	circ.on('unselect', elementUnselect);
	
	drawElementText(circ, label);
	
	return circ;
}

function drawRectangle(cx, cy, label, subType) {
	//var rect = draw.rect(100, 40);
	var rect = draw.recordableRect(100, 40);
	rect.cx(cx);
	rect.cy(cy);
	rect.rx(10);
	rect.ry(10);
	rect.addClass('element');
	
	if (subType) {
		rect.subType = subType;
		rect.addClass(subType);
	}
	
	rect.selected = false;
	rect.inLines = [];
	rect.outLines = [];
	//TODO: remove redundant inLines/outLines and move related logic to inLineGroups/outLineGroups
	rect.inLineGroups = [];
	rect.outLineGroups = [];
	rect.on('mousedown', elementMouseDown);
	rect.on('mousemove', elementMouseMove);
	rect.on('unselect', elementUnselect);
	
	drawElementText(rect, label);
	
	return rect;
}

function drawDiamond(cx, cy, label) {
	var diam = draw.recordablePolygon('0,40 60,0, 120,40, 60,80');
	diam.customType = 'diamond';
	diam.addClass('element');
	diam.cx(cx);
	diam.cy(cy);
	diam.selected = false;
	diam.inLines = [];
	diam.outLines = [];
	//TODO: remove redundant inLines/outLines and move related logic to inLineGroups/outLineGroups
	diam.inLineGroups = [];
	diam.outLineGroups = [];
	diam.on('mousedown', elementMouseDown);
	diam.on('mousemove', elementMouseMove);
	diam.on('unselect', elementUnselect);
	
	drawElementText(diam, label);
	
	return diam;
}

function drawArrowHead(line) {
	var lineStartX = line.attr('x1');
	var lineStartY = line.attr('y1');
	var lineEndX = line.attr('x2');
	var lineEndY = line.attr('y2');

	var angleRad = (lineEndY - lineStartY) / (lineEndX - lineStartX);
	
	var arrowLineLength = 8;
	var arrowLineX1 = lineEndX - arrowLineLength * Math.cos(angleRad);
	var arrowLineY1 = lineEndY - arrowLineLength * Math.sin(angleRad);
	var arrowLine = draw.line(arrowLineX1, arrowLineY1, lineEndX, lineEndY)
					.stroke({width: 1});
	
	arrowLine.transform({rotation: 90});
	var matrix = new SVG.Matrix(arrowLine);
	var pos1 = new SVG.Point(arrowLine.attr('x1'), arrowLine.attr('y1'));
	pos1 = pos1.transform(matrix);
	var pos2 = new SVG.Point(arrowLine.attr('x2'), arrowLine.attr('y2'));
	pos2 = pos2.transform(matrix);
	
	var arrowHead = draw.polygon(lineEndX + ',' + lineEndY + ' ' 
								+ pos1.x + ',' + pos1.y + ' ' 
								+ pos2.x + ',' + pos2.y)
							.fill('black').stroke({width: 1});
	
	line.arrowHead = arrowHead;
	
	arrowLine.remove();
}

function drawLine(elm1, elm2, label) {
	var points = determineLinePoints(elm1, elm2);
	var line = draw.line(points.start.x, points.start.y, points.end.x, points.end.y)
						.stroke({width: 2});
	
	elm1.outLines.push(line);
	elm2.inLines.push(line);
	line.incomingVertex = elm1;
	line.outgoingVertex = elm2;
	
	//FIXME: fix drawArrowHead() to abandon moveArrowHead() call
	drawArrowHead(line);
	moveArrowHead(line);
	
	if (label) {
		drawLineText(line, label);
	}
	
	var group = new LineGroup(line);
	//TODO: remove redundant inLines/outLines and move related logic to inLineGroups/outLineGroups
	elm1.outLineGroups.push(group);
	elm2.inLineGroups.push(group);
	
	return line;
}

function btnNewStartOnClick() {
	var start = drawCircle(50, 50, 'Start');
	start.fire('mousedown');
}

function btnNewEndOnClick() {
	var end = drawCircle(selectedElement.cx() + selectedElement.width() + 50, selectedElement.cy(), 
						'End', 'endState');
	
	drawLine(selectedElement, end);
	end.fire('mousedown');
}

function btnSaveOnClick() {
	var state = draw.state.save();
	console.log('State: ' + JSON.stringify(state, null, 4));
}

function drawFlowElement(textPrefix, elementCssClass) {
	var elm = drawRectangle(selectedElement.cx() + selectedElement.width() + 50, selectedElement.cy(), 
			textPrefix + ' ' + elementCounter++, elementCssClass);

	if (selectedElement.customType == 'diamond' && selectedElement.outLines.length == 0) {
		drawLine(selectedElement, elm, 'Yes');
	} else if (selectedElement.customType == 'diamond' && selectedElement.outLines.length == 1) {
		drawLine(selectedElement, elm, 'No');
	} else if (selectedElement.customType == 'diamond' && selectedElement.outLines.length == 2) {
		alert('No more elements allowed');
		elm.remove();
		elm.text.remove();
		elementCounter--;
		return;
	} else {
		drawLine(selectedElement, elm);
	}
	
	elm.fire('mousedown');
}

function btnNewActionOnClick() {
	drawFlowElement('Action');
}

function btnNewSubprocessOnClick() {
	drawFlowElement('Sub-Process', 'subProcess');
}

function btnNewConditionOnClick() {
	var diam = drawDiamond(selectedElement.cx() + selectedElement.width() + 50, selectedElement.cy(), 
							'Is Condition Met?');
	
	drawLine(selectedElement, diam);
	diam.fire('mousedown');
}

function sandbox() {

	var width = 1200, height = 700;
	
	draw = SVG('flow_editor_canvas').size(width, height);
	draw.viewbox(0,0,width,height);
	
	draw.on('mousedown', canvasMouseDown);
	draw.on('mousemove', function(event){ stopEventPropagation(event); });
	
	draw.state = {
		rects: [],
		circles: [],
		diamonds: [],
		lineGroups: [],
		
		addRect: function(rect) {
			this._addElement(this.rects, rect);
		},
		
		addCircle: function(circ) {
			this._addElement(this.circles, circ);
		},
		
		addDiamond: function(diam) {
			this._addElement(this.diamonds, diam);
		},
		
		addLineGroup: function(grp) {
			this._addElement(this.lineGroups, grp);
		},
		
		removeRect: function(rect) {
			this._removeElement(this.rects, rect);
		},
		
		removeCircle: function(circ) {
			this._removeElement(this.circles, circ);
		},
		
		removeDiamond: function(diam) {
			this._removeElement(this.diamonds, diam);
		},
		
		removeLineGroup: function(grp) {
			this._removeElement(this.lineGroups, grp);
		},
		
		save: function() {
			var rectStates = [];
			this.rects.forEach(function(rect){
				rectStates.push(rect.getState());
			});
			
			var circleStates = [];
			this.circles.forEach(function(circ) {
				circleStates.push(circ.getState());
			});
			
			var diamStates = [];
			this.diamonds.forEach(function(diam) {
				diamStates.push(diam.getState());
			});
			
			var lineGroupStates = [];
			this.lineGroups.forEach(function(grp) {
				lineGroupStates.push(grp.getState());
			});
			
			var res = {
				rects: rectStates,
				circles: circleStates,
				diamonds: diamStates,
				lineGroups: lineGroupStates
			};
			
			return res;
		},
		
		_addElement: function(arr, elm) {
			arr.push(elm);
		},
		
		_removeElement: function(arr, elm) {
			arr.some(function(e, i, a) {
				if (e === elm) {
					a.splice(i, 1);
					return true;
				}
			});
		}
	
	};
	
	var background = draw.rect(width, height).fill('#FAFAFA');
	
	var currentX = 0;
	var currentY = 0;
	
	var startCx = width/6;
	var startCy = height/6;
	var rect1Cx = width/4;
	var rect1Cy = height/2;
	var rect2Cx = width/2.5;
	var rect2Cy = height/2.5;
	var rect3Cx = width/1.5;
	var rect3Cy = height/1.5;
	var diamCx = width/2.5;
	var diamCy = height/1.5;
	var subCx = width/1.2;
	var subCy = height/2;
	var endCx = width/1.2;
	var endCy = height/1.2;
	
	var start = drawCircle(startCx, startCy, 'Start');
	var action1 = drawRectangle(rect1Cx, rect1Cy, 'Action 1');
	var action2 = drawRectangle(rect2Cx, rect2Cy, 'Action 2222222222222222');
	var action3 = drawRectangle(rect3Cx, rect3Cy, 'Action 3');
	var cond = drawDiamond(diamCx, diamCy, 'Is Condition Met?');
	var sub = drawRectangle(subCx, subCy, 'Sub-Process 1', 'subProcess');
	var end = drawCircle(endCx, endCy, 'End', 'endState');
	
	//var diamArr = cond.array();
	//end.remove();
	//action1.remove();
	
	selectedElement = null;
	elementCounter = 10;
	
	drawLine(start, action1);
	drawLine(action1, cond);
	drawLine(cond, action2, 'Yes');
	drawLine(cond, action3, 'No');
	drawLine(action3, action1);
	drawLine(action2, sub);
	drawLine(sub, end);
	
	var state = draw.state.save();
	console.log(state);
}

function drawMockDiagram(scriptName, canvasId) {

	var width = 1500, height = 500;
	
	draw = SVG(canvasId).size(width, height);
	
	draw.viewbox(0,0,width,height);
	
	draw.on('mousedown', canvasMouseDown);
	draw.on('mousemove', function(event){ stopEventPropagation(event); });
	
	var background = draw.rect(width, height).fill('#FAFAFA');
	
	var currentX = 0;
	var currentY = 0;
	
	var startCx = width/6;
	var startCy = height/6;
	var rect1Cx = width/4;
	var rect1Cy = height/2;
	var rect2Cx = width/2.5;
	var rect2Cy = height/2.5;
	var rect3Cx = width/1.5;
	var rect3Cy = height/1.5;
	var diamCx = width/2.5;
	var diamCy = height/1.5;
	var subCx = width/1.2;
	var subCy = height/2;
	var endCx = width/1.2;
	var endCy = height/1.2;
	
	var start = drawCircle(startCx, startCy, 'Start');
	var action1 = drawRectangle(rect1Cx, rect1Cy, 'Action 1');
	var action2 = drawRectangle(rect2Cx, rect2Cy, 'Action 2222222222222222');
	var action3 = drawRectangle(rect3Cx, rect3Cy, 'Action 3');
	var cond = drawDiamond(diamCx, diamCy, 'Is Condition Met?');
	var sub = drawRectangle(subCx, subCy, 'Sub-Process 1', 'subProcess');
	var end = drawCircle(endCx, endCy, 'End', 'endState');
	
	selectedElement = null;
	elementCounter = 10;
	
	drawLine(start, action1);
	drawLine(action1, cond);
	drawLine(cond, action2, 'Yes');
	drawLine(cond, action3, 'No');
	drawLine(action3, action1);
	drawLine(action2, sub);
	drawLine(sub, end);

}