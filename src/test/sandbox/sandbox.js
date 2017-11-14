
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

function determineLinePoints(elm1, elm2) {
	
	function getDistance(point1, point2) {
		return Math.sqrt(Math.pow(point1.x - point2.x, 2) + Math.pow(point1.y - point2.y, 2));
	}
	
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

function drawMouseDown() {
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
	stopEventPropagation(event);
	
	if (isLeftMouseButtonPressed(event)) {
		var dx = event.clientX - currentX;
		var dy = event.clientY - currentY;
		this.cx(this.cx()+dx).cy(this.cy()+dy);
		
		var line = this.line;
		var lineStart = line.pointAt(0);
		var elms = line.array().value;
		var lineEnd = new SVG.Point(elms[elms.length-1][1], elms[elms.length-1][2]);
		
		line.plot('M ' + lineStart.x + ' ' + lineStart.y +
				 ' L ' + this.cx() + ' ' + this.cy() +
				 ' L ' + lineEnd.x + ' ' + lineEnd.y);
		
		moveArrowHead(line);
		
		currentX = event.clientX;
		currentY = event.clientY;
	}
	
	stopEventPropagation(event);
}

function drawDragPoints(line) {
	var dragPoints = [];
	var TRACE_STEP = 20;
	var traceLength = TRACE_STEP;
	
	while (traceLength < line.length() - TRACE_STEP) {
		var tracePt = line.pointAt(traceLength);
		var point = draw.circle(7);
		point.line = line;
		point.cx(tracePt.x).cy(tracePt.y);
		point.addClass('dragPoint');
		point.on('mouseover', dragPointMouseOver);
		point.on('mouseout', dragPointMouseOut);
		point.on('mousedown', dragPointMouseDown);
		point.on('mousemove', dragPointMouseMove);
		dragPoints.push(point);
		traceLength = traceLength + TRACE_STEP;
	}
	
	line.dragPoints = dragPoints;
} 

function deleteDragPoints(line) {
	line.dragPoints.forEach(function(point) {
		point.remove();
	});
	
	delete line.dragPoints;
}

function lineMouseDown(event) {
	currentX = event.clientX;
	currentY = event.clientY;
	
	if (!this.selected) {
		this.selected = true;
		
		if (this.hasClass('unselectedLine')) {
			this.removeClass('unselectedLine');
			this.arrowHead.removeClass('unselectedArrowHead');
		}
		
		this.addClass('selectedLine');
		this.arrowHead.addClass('selectedArrowHead');
		
		if (selectedElement != null) {
			selectedElement.fire('unselect');
		}
		
		selectedElement = this;
		drawDragPoints(this);
	}	
	
	stopEventPropagation(event);
}

function moveArrowHead(line) {
	var elms = line.array().value;
	
	var lineStartX = elms[elms.length-2][1];
	var lineStartY = elms[elms.length-2][2];
	var lineEndX = elms[elms.length-1][1];
	var lineEndY = elms[elms.length-1][2];
	
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

function moveArrow(line, points) {
	line.plot('M ' + points.start.x + ' ' + points.start.y + 
			 ' L ' + points.end.x + ' ' + points.end.y);
	
	moveLineLabel(line);
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
			var inElm = this;
			var outElm = outLine.outgoingVertex;
			var points = determineLinePoints(inElm, outElm);
			moveArrow(outLine, points);
		}, this);
		
		this.inLines.forEach(function(inLine){
			var inElm = inLine.incomingVertex;
			var outElm = this;
			var points = determineLinePoints(inElm, outElm);
			moveArrow(inLine, points);
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

function lineUnselect() {
	this.selected = false;
	
	if (this.hasClass('selectedLine')) {
		this.removeClass('selectedLine');
		this.arrowHead.removeClass('selectedArrowHead');
	}
	
	deleteDragPoints(this);	
	this.addClass('unselectedLine');
	this.arrowHead.addClass('unselectedArrowHead');
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
	var elms = line.array().value;
	
	var lineStartX = elms[elms.length-2][1];
	var lineStartY = elms[elms.length-2][2];
	var lineEndX = elms[elms.length-1][1];
	var lineEndY = elms[elms.length-1][2];

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

	var line = draw.path('M ' + points.start.x + ' ' + points.start.y + 
						' L ' + points.end.x + ' ' + points.end.y)
						.stroke({width: 2}).fill('none');
	
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
	
	line.selected = false;
	line.on('mousedown', lineMouseDown);
	line.on('unselect', lineUnselect);
	
	return line;
}


function sandbox() {

	var width = 800, height = 500;
	
	draw = SVG('canvas').size(width, height)
	draw.viewbox(0,0,width,height)
	
	draw.on('mousedown', drawMouseDown);
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