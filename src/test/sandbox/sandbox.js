
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

	// midpoints of each rectangle's edge
	function getEdgeMidpoints(rect) {
		return [new Point(rect.cx()+rect.width()/2, rect.cy()),
				new Point(rect.cx(), rect.cy()+rect.height()/2),
				new Point(rect.cx()-rect.width()/2, rect.cy()),
				new Point(rect.cx(), rect.cy()-rect.height()/2)];
	}
	
	// vertex points of diamond
	function getVertexPoints(diam) {
		var pts = diam.array().value;
		
		return [new Point(pts[0][0], pts[0][1]),
				new Point(pts[1][0], pts[1][1]),
				new Point(pts[2][0], pts[2][1]),
				new Point(pts[3][0], pts[3][1])];
	}
	
	var provider = {
	
		getAttachPoints: function(element) {
			if (element.customType && element.customType == 'diamond') {
				return getVertexPoints(element);
			}
			else {
				return getEdgeMidpoints(element);
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

function moveArrowHead(line) {
	var lineStart = line.pointAt(0);
	var lineEnd = line.pointAt(line.length());
	
	var lineStartX = lineStart.x;
	var lineStartY = lineStart.y;
	var lineEndX = lineEnd.x;
	var lineEndY = lineEnd.y;
	
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

function moveArrow(line, points) {
	line.plot('M ' + points.start.x + ' ' + points.start.y + 
			 ' L ' + points.end.x + ' ' + points.end.y);
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

function drawRectangle(cx, cy, label) {
	var rect = draw.rect(100, 40);
	rect.cx(cx);
	rect.cy(cy);
	rect.rx(10);
	rect.ry(10);
	rect.addClass('element');
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
	var lineStart = line.pointAt(0);
	var lineEnd = line.pointAt(line.length());
	
	var lineStartX = lineStart.x;
	var lineStartY = lineStart.y;
	var lineEndX = lineEnd.x;
	var lineEndY = lineEnd.y;

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

function drawLine(elm1, elm2) {
	var points = determineLinePoints(elm1, elm2);

	var line = draw.path('M ' + points.start.x + ' ' + points.start.y + 
						' L ' + points.end.x + ' ' + points.end.y).stroke({width: 2});

	elm1.outLines.push(line);
	elm2.inLines.push(line);
	line.incomingVertex = elm1;
	line.outgoingVertex = elm2;
	
	//FIXME: fix drawArrowHead() to abandon moveArrowHead() call
	drawArrowHead(line);
	moveArrowHead(line);
	
	return line;
}


function sandbox() {

	var width = 800, height = 500;
	
	draw = SVG('canvas').size(width, height)
	draw.viewbox(0,0,width,height)
	
	draw.on('mousedown', drawMouseDown);
	
	var background = draw.rect(width, height).fill('#FAFAFA');
	
	var currentX = 0;
	var currentY = 0;
	
	var rect1Cx = width/4;
	var rect1Cy = height/4;
	var rect2Cx = width/2.5;
	var rect2Cy = height/2.5;
	var rect3Cx = width/1.5;
	var rect3Cy = height/1.5;
	var diamCx = width/2.5;
	var diamCy = height/1.5;
	
	var rect1 = drawRectangle(rect1Cx, rect1Cy, 'Action 1');
	var rect2 = drawRectangle(rect2Cx, rect2Cy, 'Action 2222222222222222');
	var rect3 = drawRectangle(rect3Cx, rect3Cy, 'Action 3');
	var diam = drawDiamond(diamCx, diamCy, 'Is Condition Met?');
	
	selectedElement = null;
	
	drawLine(rect1, diam);
	drawLine(diam, rect2);
	drawLine(diam, rect3);

}