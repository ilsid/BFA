function LineGroup(line) {
	this.constructor.call(this, SVG.create('lineGroup'));
	
	var DRAG_POINT_DIAMETER = 6;
	
	line.group = this;
	line.on('mousedown', mouseDown);
	
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
		selectLine(newLine);
	};
	
	this.remove = function() {
		var line = this.head;
		do {
			line.remove();
			line = line.nextLine;
		} while (line);
	};
	
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
				selectLine(line, event);
				line = line.nextLine;
			} while (line);
			
			drawDragPoints(group);
		}

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

function drawCircle(cx, cy, label, additionalStyle) {
	var circ = draw.circle(40);
	circ.customType = 'circle';
	circ.cx(cx);
	circ.cy(cy);
	circ.addClass('element');
	
	if (additionalStyle) {
		circ.addClass(additionalStyle);
	}
	
	circ.selected = false;
	circ.inLines = [];
	circ.outLines = [];
	circ.on('mousedown', elementMouseDown);
	circ.on('mousemove', elementMouseMove);
	circ.on('unselect', elementUnselect);
	
	drawElementText(circ, label);
	
	return circ;
}

function drawRectangle(cx, cy, label, additionalStyle) {
	var rect = draw.rect(100, 40);
	rect.cx(cx);
	rect.cy(cy);
	rect.rx(10);
	rect.ry(10);
	rect.addClass('element');
	
	if (additionalStyle) {
		rect.addClass(additionalStyle);
	}
	
	rect.selected = false;
	rect.inLines = [];
	rect.outLines = [];
	rect.on('mousedown', elementMouseDown);
	rect.on('mousemove', elementMouseMove);
	rect.on('unselect', elementUnselect);
	
	drawElementText(rect, label);
	
	return rect;
}

function drawDiamond(cx, cy, label) {
	var diam = draw.polygon('0,40 60,0, 120,40, 60,80');
	diam.customType = 'diamond';
	diam.addClass('element');
	diam.cx(cx);
	diam.cy(cy);
	diam.selected = false;
	diam.inLines = [];
	diam.outLines = [];
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
	
	new LineGroup(line);
	
	return line;
}

function sandbox() {

	var width = 800, height = 500;
	
	draw = SVG('flow_editor_canvas').size(width, height);
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
	
	drawLine(start, action1);
	drawLine(action1, cond);
	drawLine(cond, action2, 'Yes');
	drawLine(cond, action3, 'No');
	drawLine(action3, action1);
	drawLine(action2, sub);
	drawLine(sub, end);
	//drawLine(action3, end);
	
	//alert(document.getElementById('canvas').innerHTML);

}