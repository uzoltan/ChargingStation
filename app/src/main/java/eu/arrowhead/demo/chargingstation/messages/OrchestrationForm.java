package eu.arrowhead.demo.chargingstation.messages;

import eu.arrowhead.demo.chargingstation.messages.models.ArrowheadService;
import eu.arrowhead.demo.chargingstation.messages.models.ArrowheadSystem;


public class OrchestrationForm {

	private ArrowheadService service;
	private ArrowheadSystem provider;
	private String serviceURI;
	private String authorizationToken;
	private String instruction;
	private String signature;

	public OrchestrationForm() {
	}

	public OrchestrationForm(ArrowheadService service, ArrowheadSystem provider, String serviceURI,
							 String authorizationToken, String instruction) {
		this.service = service;
		this.provider = provider;
		this.serviceURI = serviceURI;
		this.authorizationToken = authorizationToken;
		this.instruction = instruction;
	}

	public ArrowheadService getService() {
		return service;
	}

	public void setService(ArrowheadService service) {
		this.service = service;
	}

	public ArrowheadSystem getProvider() {
		return provider;
	}

	public void setProvider(ArrowheadSystem provider) {
		this.provider = provider;
	}

	public String getServiceURI() {
		return serviceURI;
	}

	public void setServiceURI(String serviceURI) {
		this.serviceURI = serviceURI;
	}

	public String getAuthorizationToken() {
		return authorizationToken;
	}

	public void setAuthorizationToken(String authorizationToken) {
		this.authorizationToken = authorizationToken;
	}

	public String getInstruction() {
		return instruction;
	}

	public void setInstruction(String instruction) {
		this.instruction = instruction;
	}

	public String getSignature() {
		return signature;
	}

	public void setSignature(String signature) {
		this.signature = signature;
	}
	
	
}
