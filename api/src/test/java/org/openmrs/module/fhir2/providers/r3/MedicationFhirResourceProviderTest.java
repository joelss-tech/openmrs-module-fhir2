/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.fhir2.providers.r3;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.param.TokenOrListParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.MethodNotAllowedException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.hl7.fhir.convertors.conv30_40.Medication30_40;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Medication;
import org.hl7.fhir.dstu3.model.OperationOutcome;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.module.fhir2.FhirConstants;
import org.openmrs.module.fhir2.api.FhirMedicationService;
import org.openmrs.module.fhir2.api.search.param.MedicationSearchParams;

@RunWith(MockitoJUnitRunner.class)
public class MedicationFhirResourceProviderTest {
	
	private static final String MEDICATION_UUID = "ce8bfad7-c87e-4af0-80cd-c2015c7dff93";
	
	private static final String WRONG_MEDICATION_UUID = "51f069dc-e204-40f4-90d6-080385bed91f";
	
	private static final String CODE = "5087AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
	
	private static final String LAST_UPDATED_DATE = "2020-09-03";
	
	private static final int PREFERRED_PAGE_SIZE = 10;
	
	private static final int COUNT = 1;
	
	private static final int START_INDEX = 0;
	
	private static final int END_INDEX = 10;
	
	@Mock
	private FhirMedicationService fhirMedicationService;
	
	private MedicationFhirResourceProvider resourceProvider;
	
	private org.hl7.fhir.r4.model.Medication medication;
	
	@Before
	public void setup() {
		resourceProvider = new MedicationFhirResourceProvider();
		resourceProvider.setMedicationService(fhirMedicationService);
		
		medication = new org.hl7.fhir.r4.model.Medication();
		medication.setId(MEDICATION_UUID);
	}
	
	private List<Medication> get(IBundleProvider results) {
		return results.getResources(START_INDEX, END_INDEX).stream().filter(it -> it instanceof Medication)
		        .map(it -> (Medication) it).collect(Collectors.toList());
	}
	
	@Test
	public void getResourceType_shouldReturnResourceType() {
		assertThat(resourceProvider.getResourceType(), equalTo(Medication.class));
		assertThat(resourceProvider.getResourceType().getName(), equalTo(Medication.class.getName()));
	}
	
	@Test
	public void getMedicationByUuid_shouldReturnMatchingMedication() {
		when(fhirMedicationService.get(MEDICATION_UUID)).thenReturn(medication);
		
		IdType id = new IdType();
		id.setValue(MEDICATION_UUID);
		Medication medication = resourceProvider.getMedicationById(id);
		assertThat(medication, notNullValue());
		assertThat(medication.getId(), notNullValue());
		assertThat(medication.getId(), equalTo(MEDICATION_UUID));
	}
	
	@Test(expected = ResourceNotFoundException.class)
	public void getMedicationByUuid_shouldThrowResourceNotFoundException() {
		IdType id = new IdType();
		id.setValue(WRONG_MEDICATION_UUID);
		Medication medication = resourceProvider.getMedicationById(id);
		assertThat(medication, nullValue());
	}
	
	@Test
	public void searchForMedication_shouldReturnMatchingBundleOfMedicationByCode() {
		TokenAndListParam code = new TokenAndListParam();
		code.addAnd(new TokenOrListParam().addOr(new TokenParam().setValue(CODE)));
		
		when(fhirMedicationService.searchForMedications(new MedicationSearchParams(code, null, null, null, null, null)))
		        .thenReturn(new MockIBundleProvider<>(Collections.singletonList(medication), PREFERRED_PAGE_SIZE, COUNT));
		
		IBundleProvider results = resourceProvider.searchForMedication(code, null, null, null, null, null);
		
		List<Medication> resultList = get(results);
		
		assertThat(results, notNullValue());
		assertThat(resultList, hasSize(greaterThanOrEqualTo(1)));
		assertThat(resultList.get(0).fhirType(), equalTo(FhirConstants.MEDICATION));
	}
	
	@Test
	public void searchForMedication_shouldReturnMatchingBundleOfMedicationByDosageForm() {
		TokenAndListParam dosageFormCode = new TokenAndListParam();
		dosageFormCode.addAnd(new TokenOrListParam().addOr(new TokenParam().setValue(CODE)));
		
		when(fhirMedicationService
		        .searchForMedications(new MedicationSearchParams(null, dosageFormCode, null, null, null, null))).thenReturn(
		            new MockIBundleProvider<>(Collections.singletonList(medication), PREFERRED_PAGE_SIZE, COUNT));
		
		IBundleProvider results = resourceProvider.searchForMedication(null, dosageFormCode, null, null, null, null);
		
		List<Medication> resultList = get(results);
		
		assertThat(results, notNullValue());
		assertThat(resultList, hasSize(greaterThanOrEqualTo(1)));
		assertThat(resultList.get(0).fhirType(), equalTo(FhirConstants.MEDICATION));
	}
	
	@Test
	public void searchForMedication_shouldReturnMatchingBundleOfMedicationByIngredientCode() {
		TokenAndListParam ingredientCode = new TokenAndListParam();
		ingredientCode.addAnd(new TokenOrListParam().addOr(new TokenParam().setValue(CODE)));
		
		when(fhirMedicationService
		        .searchForMedications(new MedicationSearchParams(null, null, ingredientCode, null, null, null))).thenReturn(
		            new MockIBundleProvider<>(Collections.singletonList(medication), PREFERRED_PAGE_SIZE, COUNT));
		
		IBundleProvider results = resourceProvider.searchForMedication(null, null, ingredientCode, null, null, null);
		
		List<Medication> resultList = get(results);
		
		assertThat(results, notNullValue());
		assertThat(resultList, hasSize(greaterThanOrEqualTo(1)));
		assertThat(resultList.get(0).fhirType(), equalTo(FhirConstants.MEDICATION));
	}
	
	@Test
	public void searchForMedication_shouldReturnMatchingBundleOfMedicationByUUID() {
		TokenAndListParam uuid = new TokenAndListParam();
		uuid.addAnd(new TokenParam().setValue(MEDICATION_UUID));
		
		when(fhirMedicationService.searchForMedications(new MedicationSearchParams(null, null, null, uuid, null, null)))
		        .thenReturn(new MockIBundleProvider<>(Collections.singletonList(medication), PREFERRED_PAGE_SIZE, COUNT));
		
		IBundleProvider results = resourceProvider.searchForMedication(null, null, null, uuid, null, null);
		
		List<Medication> resultList = get(results);
		
		assertThat(results, notNullValue());
		assertThat(resultList, hasSize(greaterThanOrEqualTo(1)));
		assertThat(resultList.get(0).fhirType(), equalTo(FhirConstants.MEDICATION));
	}
	
	@Test
	public void searchForMedication_shouldReturnMatchingBundleOfMedicationByLastUpdated() {
		DateRangeParam lastUpdated = new DateRangeParam().setLowerBound(LAST_UPDATED_DATE).setUpperBound(LAST_UPDATED_DATE);
		
		when(fhirMedicationService
		        .searchForMedications(new MedicationSearchParams(null, null, null, null, lastUpdated, null))).thenReturn(
		            new MockIBundleProvider<>(Collections.singletonList(medication), PREFERRED_PAGE_SIZE, COUNT));
		
		IBundleProvider results = resourceProvider.searchForMedication(null, null, null, null, lastUpdated, null);
		
		List<Medication> resultList = get(results);
		
		assertThat(results, notNullValue());
		assertThat(resultList, hasSize(greaterThanOrEqualTo(1)));
		assertThat(resultList.get(0).fhirType(), equalTo(FhirConstants.MEDICATION));
	}
	
	@Test
	public void searchForMedication_shouldAddRelatedResourcesForRevInclude() {
		when(fhirMedicationService.searchForMedications(any())).thenReturn(
		    new MockIBundleProvider<>(Arrays.asList(medication, new MedicationRequest()), PREFERRED_PAGE_SIZE, COUNT));
		
		HashSet<Include> revIncludes = new HashSet<>();
		revIncludes.add(new Include("MedicationRequest:medication"));
		
		IBundleProvider results = resourceProvider.searchForMedication(null, null, null, null, null, revIncludes);
		
		List<IBaseResource> resultList = results.getResources(START_INDEX, END_INDEX);
		
		assertThat(results, notNullValue());
		assertThat(resultList.size(), greaterThanOrEqualTo(2));
		assertThat(resultList.get(0).fhirType(), equalTo(FhirConstants.MEDICATION));
		assertThat(resultList.get(1).fhirType(), equalTo(FhirConstants.MEDICATION_REQUEST));
		assertThat(((Medication) resultList.iterator().next()).getId(), equalTo(MEDICATION_UUID));
	}
	
	@Test
	public void searchForMedication_shouldNotAddResourcesForEmptyRevInclude() {
		when(fhirMedicationService.searchForMedications(new MedicationSearchParams(null, null, null, null, null, null)))
		        .thenReturn(new MockIBundleProvider<>(Collections.singletonList(medication), PREFERRED_PAGE_SIZE, COUNT));
		
		HashSet<Include> revIncludes = new HashSet<>();
		
		IBundleProvider results = resourceProvider.searchForMedication(null, null, null, null, null, revIncludes);
		
		List<IBaseResource> resultList = results.getResources(START_INDEX, END_INDEX);
		
		assertThat(results, notNullValue());
		assertThat(resultList.size(), equalTo(1));
		assertThat(resultList.get(0).fhirType(), equalTo(FhirConstants.MEDICATION));
		assertThat(((Medication) resultList.iterator().next()).getId(), equalTo(MEDICATION_UUID));
	}
	
	@Test
	public void createMedication_shouldCreateNewMedication() {
		when(fhirMedicationService.create(any(org.hl7.fhir.r4.model.Medication.class))).thenReturn(medication);
		
		MethodOutcome result = resourceProvider.createMedication(Medication30_40.convertMedication(medication));
		
		assertThat(result, notNullValue());
		assertThat(result.getCreated(), is(true));
		assertThat(result.getResource(), notNullValue());
		assertThat(result.getResource().getIdElement().getIdPart(), equalTo(medication.getId()));
	}
	
	@Test
	public void updateMedication_shouldUpdateMedication() {
		medication.setStatus(org.hl7.fhir.r4.model.Medication.MedicationStatus.INACTIVE);
		
		when(fhirMedicationService.update(eq(MEDICATION_UUID), any(org.hl7.fhir.r4.model.Medication.class)))
		        .thenReturn(medication);
		
		MethodOutcome result = resourceProvider.updateMedication(new IdType().setValue(MEDICATION_UUID),
		    Medication30_40.convertMedication(medication));
		
		assertThat(result, notNullValue());
		assertThat(result.getResource(), notNullValue());
		assertThat(result.getResource().getIdElement().getIdPart(), equalTo(medication.getId()));
	}
	
	@Test(expected = InvalidRequestException.class)
	public void updateMedicationShouldThrowInvalidRequestForUuidMismatch() {
		when(fhirMedicationService.update(eq(WRONG_MEDICATION_UUID), any(org.hl7.fhir.r4.model.Medication.class)))
		        .thenThrow(InvalidRequestException.class);
		
		resourceProvider.updateMedication(new IdType().setValue(WRONG_MEDICATION_UUID),
		    Medication30_40.convertMedication(medication));
	}
	
	@Test(expected = InvalidRequestException.class)
	public void updateMedicationShouldThrowInvalidRequestForMissingId() {
		org.hl7.fhir.r4.model.Medication noIdMedication = new org.hl7.fhir.r4.model.Medication();
		
		when(fhirMedicationService.update(eq(MEDICATION_UUID), any(org.hl7.fhir.r4.model.Medication.class)))
		        .thenThrow(InvalidRequestException.class);
		
		resourceProvider.updateMedication(new IdType().setValue(MEDICATION_UUID),
		    Medication30_40.convertMedication(noIdMedication));
	}
	
	@Test(expected = MethodNotAllowedException.class)
	public void updateMedicationShouldThrowMethodNotAllowedIfDoesNotExist() {
		org.hl7.fhir.r4.model.Medication wrongMedication = new org.hl7.fhir.r4.model.Medication();
		wrongMedication.setId(WRONG_MEDICATION_UUID);
		
		when(fhirMedicationService.update(eq(WRONG_MEDICATION_UUID), any(org.hl7.fhir.r4.model.Medication.class)))
		        .thenThrow(MethodNotAllowedException.class);
		
		resourceProvider.updateMedication(new IdType().setValue(WRONG_MEDICATION_UUID),
		    Medication30_40.convertMedication(wrongMedication));
	}
	
	@Test
	public void deleteMedication_shouldDeleteRequestedMedication() {
		OperationOutcome result = resourceProvider.deleteMedication(new IdType().setValue(MEDICATION_UUID));
		
		assertThat(result, notNullValue());
		assertThat(result.getIssue(), notNullValue());
		assertThat(result.getIssueFirstRep().getSeverity(), equalTo(OperationOutcome.IssueSeverity.INFORMATION));
		assertThat(result.getIssueFirstRep().getDetails().getCodingFirstRep().getCode(), equalTo("MSG_DELETED"));
	}
}
