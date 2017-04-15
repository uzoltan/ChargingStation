package eu.arrowhead.demo.chargingstation.messages;

import eu.arrowhead.demo.chargingstation.messages.models.ArrowheadService;
import eu.arrowhead.demo.chargingstation.messages.models.ArrowheadSystem;

public class ServiceRequestForm {

	private ArrowheadSystem requesterSystem;
	private ArrowheadService requestedService;
	
	public ServiceRequestForm (){
	}

	public ServiceRequestForm(ArrowheadSystem requesterSystem, ArrowheadService requestedService) {
		this.requesterSystem = requesterSystem;
		this.requestedService = requestedService;
	}

	public ArrowheadSystem getRequesterSystem() {
		return requesterSystem;
	}

	public void setRequesterSystem(ArrowheadSystem requesterSystem) {
		this.requesterSystem = requesterSystem;
	}

	public ArrowheadService getRequestedService() {
		return requestedService;
	}

	public void setRequestedService(ArrowheadService requestedService) {
		this.requestedService = requestedService;
	}

	
}
