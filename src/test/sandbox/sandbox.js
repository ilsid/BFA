function LineGroup(line, flowId) {
	this.constructor.call(this, SVG.create('lineGroup'));
	
	this.flowId = flowId;
	draw.state.addLineGroup(this);
	
	var DRAG_POINT_SIZE = 8;
	
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
			id: this.flowId,
			inElement: this.tail.incomingVertex.flowId,
			outElement: this.head.outgoingVertex.flowId,
			lines: []	
		};
		
		var line = this.head;
		do {
			var lineState = {
				x1: line.attr('x1'),
				y1: line.attr('y1'),
				x2: line.attr('x2'),
				y2: line.attr('y2')
			};
			
			if (line.label) {
				lineState.label = line.label[1].text();
			}
			
			state.lines.push(lineState);
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
		var point = draw.rect(DRAG_POINT_SIZE, DRAG_POINT_SIZE);
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
				selectLine(newLine);
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
			subType: this.subType,
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
			subType: this.subType,
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
		var elm = this.put(new SVG.RecordableRect).size(width, height);
		elm.flowId = flowId;
		draw.state.addRect(elm);
		
		return elm;
	},

	recordableCircle: function(size, flowId) {
		var elm = this.put(new SVG.RecordableCircle).rx(new SVG.Number(size).divide(2)).move(0, 0);
		elm.flowId = flowId;
		draw.state.addCircle(elm);
		
		return elm;
	},
	
	recordablePolygon: function(p, flowId) {
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
		
		this.outLineGroups.forEach(function(outLineGroup) {
			if (outLineGroup.head === outLineGroup.tail) {
				var inElm = this;
				var outElm = outLineGroup.head.outgoingVertex;
				var points = determineLinePoints(inElm, outElm);
				moveSingleArrow(outLineGroup.head, points);
			} else {
				moveLineGroupTail(this, outLineGroup.tail);
			}
		}, this);
		
		this.inLineGroups.forEach(function(inLineGroup) {
			if (inLineGroup.head === inLineGroup.tail) {
				var inElm = inLineGroup.head.incomingVertex;
				var outElm = this;
				var points = determineLinePoints(inElm, outElm);
				moveSingleArrow(inLineGroup.head, points);
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
		var origCx = elm.cx();
		elm.width(textWidth + 10);
		elm.cx(origCx);
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

function drawCircle(cx, cy, label, flowId, subType) {
	var circ = draw.recordableCircle(40, flowId);
	circ.customType = 'circle';
	circ.cx(cx);
	circ.cy(cy);
	circ.addClass('element');
	
	if (subType) {
		circ.subType = subType;
		circ.addClass(subType);
	}
	
	circ.selected = false;
	circ.inLineGroups = [];
	circ.outLineGroups = [];
	circ.on('mousedown', elementMouseDown);
	circ.on('mousemove', elementMouseMove);
	circ.on('unselect', elementUnselect);
	
	drawElementText(circ, label);
	
	return circ;
}

function drawRectangle(cx, cy, label, flowId, subType) {
	var rect = draw.recordableRect(100, 40, flowId);
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
	rect.inLineGroups = [];
	rect.outLineGroups = [];
	rect.on('mousedown', elementMouseDown);
	rect.on('mousemove', elementMouseMove);
	rect.on('unselect', elementUnselect);
	
	drawElementText(rect, label);
	
	return rect;
}

function drawDiamond(cx, cy, label, flowId) {
	var diam = draw.recordablePolygon('0,40 60,0, 120,40, 60,80', flowId);
	diam.customType = 'diamond';
	diam.addClass('element');
	diam.cx(cx);
	diam.cy(cy);
	diam.selected = false;
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
	
	line.incomingVertex = elm1;
	line.outgoingVertex = elm2;
	
	//FIXME: fix drawArrowHead() to abandon moveArrowHead() call
	drawArrowHead(line);
	moveArrowHead(line);
	
	if (label) {
		drawLineText(line, label);
	}
	
	var groupId = elm1.flowId + '-->' + elm2.flowId; 
	var group = new LineGroup(line, groupId);
	
	elm1.outLineGroups.push(group);
	elm2.inLineGroups.push(group);
	
	return line;
}

function drawLineGroup(state, elms) {
	var headState = state.lines[0];
	var line = draw.line(headState.x1, headState.y1, headState.x2, headState.y2)
				.stroke({width: 2});
	drawArrowHead(line);
	moveArrowHead(line);
	
	var group = new LineGroup(line, state.id);
	var label = headState.label;
	
	if (state.lines.length > 1) {
		
		for (var i = 1; i < state.lines.length; i++) {
			var lineState = state.lines[i];

			if (lineState.label) {
				label = lineState.label;
			}			
			
			var newLine = draw.line(lineState.x1, lineState.y1, lineState.x2, lineState.y2)
					.stroke({width: 2});
			group.addAfter(newLine, line);
			line = newLine;
		}
	}
	
	if (label) {
		drawLineText(group.tail, label);
	}
	
	var inElm = elms.get(state.inElement);
	var outElm = elms.get(state.outElement);
	
	group.head.outgoingVertex = outElm;
	group.tail.incomingVertex = inElm;
	
	inElm.outLineGroups.push(group);
	outElm.inLineGroups.push(group);
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
	var stateStr=JSON.stringify(state, null, 4);
	
	document.getElementById('flowState').value = stateStr;
}

function btnRestoreOnClick() {
	var state = JSON.parse(document.getElementById('flowState').value);
	var elms = new ElementsMap();
	
	state.circles.forEach(function(state) {
		var circ = drawCircle(state.cx, state.cy, state.label, state.id, state.subType);
		elms.put(state.id, circ);
	});
	
	state.rects.forEach(function(state) {
		var rect = drawRectangle(state.cx, state.cy, state.label, state.id, state.subType);
		elms.put(state.id, rect);
	});
	
	state.diamonds.forEach(function(state) {
		var diam = drawDiamond(state.cx, state.cy, state.label, state.id);
		elms.put(state.id, diam);
	});
	
	state.lineGroups.forEach(function(state) {
		drawLineGroup(state, elms);
	});
}

function btnClearOnClick() {
	document.getElementById('flow_editor_canvas').innerHTML = '';
	delete draw.state;
	
	var width = 1200, height = 700;
	draw = SVG('flow_editor_canvas').size(width, height);
	
	draw.on('mousedown', canvasMouseDown);
	draw.on('mousemove', function(event){ stopEventPropagation(event); });
	
	draw.state = new State();
}

function btnIconsOnClick() {
	drawFlowIcons();
}

function drawFlowElement(textPrefix, elementCssClass) {
	var elm = drawRectangle(selectedElement.cx() + selectedElement.width() + 50, selectedElement.cy(), 
			textPrefix + ' ' + elementCounter++, elementCssClass);

	if (selectedElement.customType == 'diamond' && selectedElement.outLineGroups.length == 0) {
		drawLine(selectedElement, elm, 'Yes');
	} else if (selectedElement.customType == 'diamond' && selectedElement.outLineGroups.length == 1) {
		drawLine(selectedElement, elm, 'No');
	} else if (selectedElement.customType == 'diamond' && selectedElement.outLineGroups.length == 2) {
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

function ElementsMap() {
	this._elements = [];
	
	this.put = function(elmId, flowElm) {
		var elm = {
			id: elmId,
			object: flowElm
		};
		
		this._elements.push(elm);
	};
	
	this.get = function(elmId) {
		var res = this._elements.find(function(e) {
			return e.id == elmId;
		});
		
		return res.object;
	};
	
	this.clear = function() {
		this._elements = [];
	};
}

function State() {
	this.rects = [];
	this.circles = [];
	this.diamonds = [];
	this.lineGroups = [];
	
	this.addRect = function(rect) {
		addElement(this.rects, rect);
	};
	
	this.addCircle = function(circ) {
		addElement(this.circles, circ);
	};
	
	this.addDiamond = function(diam) {
		addElement(this.diamonds, diam);
	};
	
	this.addLineGroup = function(grp) {
		addElement(this.lineGroups, grp);
	};
	
	this.removeRect = function(rect) {
		removeElement(this.rects, rect);
	};
	
	this.removeCircle = function(circ) {
		removeElement(this.circles, circ);
	};
	
	this.removeDiamond = function(diam) {
		removeElement(this.diamonds, diam);
	};
	
	this.removeLineGroup = function(grp) {
		removeElement(this.lineGroups, grp);
	},
	
	this.save = function() {
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
	};
	
	function addElement(arr, elm) {
		arr.push(elm);
	};
	
	function removeElement(arr, elm) {
		arr.some(function(e, i, a) {
			if (e === elm) {
				a.splice(i, 1);
				return true;
			}
		});
	};
}


function sandbox() {

	var width = 1200, height = 700;
	
	draw = SVG('flow_editor_canvas').size(width, height);
	draw.viewbox(0,0,width,height);
	
	draw.on('mousedown', canvasMouseDown);
	draw.on('mousemove', function(event){ stopEventPropagation(event); });
	
	draw.state = new State();
	
	//var background = draw.rect(width, height).fill('#FAFAFA');
	
	var currentX = 0;
	var currentY = 0;
	
	var startCx = width/6;
	var startCy = height/6;
	var rect1Cx = width/4;
	var rect1Cy = height/2;
	var rect12Cx = width/3.7;
	var rect12Cy = height/1.7;
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
	
	var start = drawCircle(startCx, startCy, 'Start', 'str001');
	var action1 = drawRectangle(rect1Cx, rect1Cy, 'Action 1', 'act001');
	var action2 = drawRectangle(rect2Cx, rect2Cy, 'Action 2222222222222222', 'act002');
	var cond = drawDiamond(diamCx, diamCy, 'Is Condition Met?', 'cnd001');
	var action12 = drawRectangle(rect12Cx, rect12Cy, 'Action 1', 'act001-02');
	var action3 = drawRectangle(rect3Cx, rect3Cy, 'Action 3', 'act003');
	var sub = drawRectangle(subCx, subCy, 'Sub-Process 1', 'sub001', 'subProcess');
	var end = drawCircle(endCx, endCy, 'End', 'end001', 'endState');
	
	selectedElement = null;
	elementCounter = 10;
	
	drawLine(start, action1);
	drawLine(action1, cond);
	drawLine(cond, action2, 'Yes');
	drawLine(cond, action3, 'No');
	drawLine(action3, action12);
	drawLine(action12, cond);
	drawLine(action2, sub);
	drawLine(sub, end);
	
	draw2 = SVG('icons_canvas').size(16, 16);
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

function drawFlowIcons() {
	var elementAttrs = {stroke: 'black', 'stroke-width': 1, fill: '#CEE3F6'};
	
//	var circ = draw2.circle(12);
//	circ.cx(8);
//	circ.cy(8);
//	circ.attr(elementAttrs);
	
//	var rect = draw2.rect(15, 10);
//	rect.cx(8);
//	rect.cy(8);
//	rect.rx(1);
//	rect.ry(1);
//	rect.attr(elementAttrs);

		
//	var sub = draw2.rect(15, 10);
//	sub.cx(8);
//	sub.cy(8);
//	sub.rx(1);
//	sub.ry(1);
//	var subAttrs = {stroke: 'black', 'stroke-width': 2, fill: '#CEE3F6', 'stroke-dasharray': '3,1'};
//	sub.attr(subAttrs);

	
//	var diam = draw2.polygon('0,7 8,0, 16,7, 8,14');
//	diam.cx(8);
//	diam.cy(8);
//	diam.attr(elementAttrs);
//	
	var circ = draw2.circle(12);
	circ.cx(8);
	circ.cy(8);
	var endCircAttrs = {stroke: 'black', 'stroke-width': 2, fill: '#CEE3F6'};
	circ.attr(endCircAttrs);

	// https://github.com/exupero/saveSvgAsPng
	saveSvgAsPng(document.getElementById('icons_canvas').firstChild, "icon.png");
}