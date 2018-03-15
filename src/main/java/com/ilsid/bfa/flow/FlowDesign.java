package com.ilsid.bfa.flow;

import java.util.List;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

/**
 * Flow design representation.
 * 
 * @author illia.sydorovych
 *
 */
@JsonIgnoreProperties({ "rects", "circles", "diamonds", "lineGroups" })
public class FlowDesign {
	
	private List<Object> rects;
	
	private List<Object> circles;
	
	private List<Object> diamonds;
	
	private List<Object> lineGroups;
	
	private FlowDefinition flow;

	public List<Object> getRects() {
		return rects;
	}

	public void setRects(List<Object> rects) {
		this.rects = rects;
	}

	public List<Object> getCircles() {
		return circles;
	}

	public void setCircles(List<Object> circles) {
		this.circles = circles;
	}

	public List<Object> getDiamonds() {
		return diamonds;
	}

	public void setDiamonds(List<Object> diamonds) {
		this.diamonds = diamonds;
	}

	public List<Object> getLineGroups() {
		return lineGroups;
	}

	public void setLineGroups(List<Object> lineGroups) {
		this.lineGroups = lineGroups;
	}

	public FlowDefinition getFlow() {
		return flow;
	}

	public void setFlow(FlowDefinition flow) {
		this.flow = flow;
	}
}
