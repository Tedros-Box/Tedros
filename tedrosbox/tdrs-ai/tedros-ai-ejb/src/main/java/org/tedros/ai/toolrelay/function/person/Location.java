package org.tedros.ai.toolrelay.function.person;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/**
 * Copia de {@code org.tedros.person.ai.function.Location} (app-person-fx) —
 * mesmos campos/tipos para schema identico.
 *
 * @author Davis Gordon
 */
@JsonClassDescription("Geographic filters for the search.")
public class Location {

    @JsonPropertyDescription("The neighborhood, district, or borough name.")
	private String neighborhood;

	@JsonPropertyDescription("The 2-letter Country ISO Code (e.g., 'US', 'BR', 'DE'). Do not use full country names.")
	private String countryIso2Code;

	@JsonPropertyDescription("The administrative area, state, province, or region.")
	private String adminArea;

	@JsonPropertyDescription("The city or municipality name.")
	private String city;

	public Location() {

	}

	public String getNeighborhood() {
		return neighborhood;
	}

	public void setNeighborhood(String neighborhood) {
		this.neighborhood = neighborhood;
	}

	public String getCountryIso2Code() {
		return countryIso2Code;
	}

	public void setCountryIso2Code(String countryIso2Code) {
		this.countryIso2Code = countryIso2Code;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public String getAdminArea() {
		return adminArea;
	}

	public void setAdminArea(String adminArea) {
		this.adminArea = adminArea;
	}

}
