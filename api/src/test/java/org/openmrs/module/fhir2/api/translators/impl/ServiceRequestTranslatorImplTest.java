/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.fhir2.api.translators.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.exparity.hamcrest.date.DateMatchers;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.hl7.fhir.r4.model.Task;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.Order;
import org.openmrs.OrderType;
import org.openmrs.Patient;
import org.openmrs.Provider;
import org.openmrs.TestOrder;
import org.openmrs.module.fhir2.FhirConstants;
import org.openmrs.module.fhir2.FhirTestConstants;
import org.openmrs.module.fhir2.api.FhirTaskService;
import org.openmrs.module.fhir2.api.translators.ConceptTranslator;
import org.openmrs.module.fhir2.api.translators.EncounterReferenceTranslator;
import org.openmrs.module.fhir2.api.translators.PatientReferenceTranslator;
import org.openmrs.module.fhir2.api.translators.PractitionerReferenceTranslator;
import org.openmrs.module.fhir2.providers.r4.MockIBundleProvider;
import org.openmrs.order.OrderUtilTest;

@RunWith(MockitoJUnitRunner.class)
public class ServiceRequestTranslatorImplTest {
	
	public static final String TEST_ORDER_TYPE_UUID = "52a447d3-a64a-11e3-9aeb-50e549534c5e";
	
	private static final String SERVICE_REQUEST_UUID = "4e4851c3-c265-400e-acc9-1f1b0ac7f9c4";
	
	private static final String DISCONTINUED_TEST_ORDER_UUID = "efca4077-493c-496b-8312-856ee5d1cc27";
	
	private static final String TEST_ORDER_NUMBER = "ORD-1";
	
	private static final String DISCONTINUED_TEST_ORDER_NUMBER = "ORD-2";
	
	private static final String PRIOR_SERVICE_REQUEST_REFERENCE = FhirConstants.SERVICE_REQUEST + "/" + SERVICE_REQUEST_UUID;
	
	private static final String LOINC_CODE = "1000-1";
	
	private static final String PATIENT_UUID = "14d4f066-15f5-102d-96e4-000c29c2a5d7";
	
	private static final String ENCOUNTER_UUID = "y403fafb-e5e4-42d0-9d11-4f52e89d123r";
	
	private static final String PRACTITIONER_UUID = "b156e76e-b87a-4458-964c-a48e64a20fbb";
	
	private static final String ORGANIZATION_UUID = "44f7a79e-1de6-4b0b-9daf-bbcb7ed18b7e";
	
	private static final int PREFERRED_PAGE_SIZE = 10;
	
	private static final int COUNT = 1;
	
	private ServiceRequestTranslatorImpl translator;
	
	@Mock
	private FhirTaskService taskService;
	
	@Mock
	private ConceptTranslator conceptTranslator;
	
	@Mock
	private PatientReferenceTranslator patientReferenceTranslator;
	
	@Mock
	private EncounterReferenceTranslator<Encounter> encounterReferenceTranslator;
	
	@Mock
	private PractitionerReferenceTranslator<Provider> practitionerReferenceTranslator;
	
	private TestOrder order;
	
	private TestOrder discontinuedTestOrder;
	
	@Before
	public void setup() {
		translator = new ServiceRequestTranslatorImpl();
		translator.setConceptTranslator(conceptTranslator);
		translator.setTaskService(taskService);
		translator.setPatientReferenceTranslator(patientReferenceTranslator);
		translator.setEncounterReferenceTranslator(encounterReferenceTranslator);
		translator.setProviderReferenceTranslator(practitionerReferenceTranslator);
		translator.setOrderIdentifierTranslator(new OrderIdentifierTranslatorImpl());
		
		order = new TestOrder();
		order.setUuid(SERVICE_REQUEST_UUID);
		setOrderNumberByReflection(order, TEST_ORDER_NUMBER);
		
		OrderType ordertype = new OrderType();
		ordertype.setUuid(TEST_ORDER_TYPE_UUID);
		order.setOrderType(ordertype);
		
		discontinuedTestOrder = new TestOrder();
		discontinuedTestOrder.setUuid(DISCONTINUED_TEST_ORDER_UUID);
		setOrderNumberByReflection(discontinuedTestOrder, DISCONTINUED_TEST_ORDER_NUMBER);
		discontinuedTestOrder.setPreviousOrder(order);
	}
	
	@Test
	public void toFhirResource_shouldTranslateToFhirResourceWithReplacesFieldGivenDiscontinuedOrder() {
		discontinuedTestOrder.setAction(Order.Action.DISCONTINUE);
		
		List<Task> tasks = setUpBasedOnScenario(Task.TaskStatus.REJECTED);
		
		when(taskService.searchForTasks(any())).thenReturn(new MockIBundleProvider<>(tasks, PREFERRED_PAGE_SIZE, COUNT));
		
		ServiceRequest result = translator.toFhirResource(discontinuedTestOrder);
		
		assertThat(result, notNullValue());
		assertThat(result.getId(), notNullValue());
		assertThat(result.getId(), equalTo(DISCONTINUED_TEST_ORDER_UUID));
		assertThat(result.getReplaces().get(0).getReference(), equalTo(PRIOR_SERVICE_REQUEST_REFERENCE));
		assertThat(result.getReplaces().get(0).getIdentifier().getValue(), equalTo(TEST_ORDER_NUMBER));
	}
	
	@Test
	public void toFhirResource_shouldTranslateToFhirResourceWithReplacesFieldGivenRevisedOrder() {
		discontinuedTestOrder.setAction(Order.Action.REVISE);
		
		List<Task> tasks = setUpBasedOnScenario(Task.TaskStatus.ACCEPTED);
		
		when(taskService.searchForTasks(any())).thenReturn(new MockIBundleProvider<>(tasks, PREFERRED_PAGE_SIZE, COUNT));
		
		ServiceRequest result = translator.toFhirResource(discontinuedTestOrder);
		
		assertThat(result, notNullValue());
		assertThat(result.getId(), notNullValue());
		assertThat(result.getId(), equalTo(DISCONTINUED_TEST_ORDER_UUID));
		assertThat(result.getReplaces().get(0).getReference(), equalTo(PRIOR_SERVICE_REQUEST_REFERENCE));
		assertThat(result.getReplaces().get(0).getIdentifier().getValue(), equalTo(TEST_ORDER_NUMBER));
	}
	
	@Test
	public void toFhirResource_shouldTranslateToFhirResourceWithBasedOnFieldGivenRenewedOrder() {
		discontinuedTestOrder.setAction(Order.Action.RENEW);
		
		List<Task> tasks = setUpBasedOnScenario(Task.TaskStatus.ACCEPTED);
		
		when(taskService.searchForTasks(any())).thenReturn(new MockIBundleProvider<>(tasks, PREFERRED_PAGE_SIZE, COUNT));
		
		ServiceRequest result = translator.toFhirResource(discontinuedTestOrder);
		
		assertThat(result, notNullValue());
		assertThat(result.getId(), notNullValue());
		assertThat(result.getId(), equalTo(DISCONTINUED_TEST_ORDER_UUID));
		assertThat(result.getBasedOn().get(0).getReference(), equalTo(PRIOR_SERVICE_REQUEST_REFERENCE));
		assertThat(result.getBasedOn().get(0).getIdentifier().getValue(), equalTo(TEST_ORDER_NUMBER));
	}
	
	@Test
	public void toFhirResource_shouldTranslateOpenmrsTestOrderToFhirServiceRequest() {
		TestOrder order = new TestOrder();
		
		when(taskService.searchForTasks(any()))
		        .thenReturn(new MockIBundleProvider<>(Collections.emptyList(), PREFERRED_PAGE_SIZE, COUNT));
		
		ServiceRequest result = translator.toFhirResource(order);
		
		assertThat(result, notNullValue());
		assertThat(result.getIntent(), equalTo(ServiceRequest.ServiceRequestIntent.ORDER));
	}
	
	@Test
	public void toFhirResource_shouldTranslateOrderFromOnlyDateActivatedToActiveServiceRequest() {
		TestOrder newOrder = new TestOrder();
		
		when(taskService.searchForTasks(any()))
		        .thenReturn(new MockIBundleProvider<>(Collections.emptyList(), PREFERRED_PAGE_SIZE, COUNT));
		
		Calendar activationDate = Calendar.getInstance();
		activationDate.set(2000, Calendar.APRIL, 16);
		newOrder.setDateActivated(activationDate.getTime());
		
		ServiceRequest result = translator.toFhirResource(newOrder);
		
		assertThat(result, notNullValue());
		assertThat(result.getStatus(), equalTo(ServiceRequest.ServiceRequestStatus.ACTIVE));
	}
	
	@Test
	public void toFhirResource_shouldTranslateOrderFromAutoExpireToCompleteServiceRequest() throws Exception {
		TestOrder newOrder = new TestOrder();
		
		when(taskService.searchForTasks(any()))
		        .thenReturn(new MockIBundleProvider<>(Collections.emptyList(), PREFERRED_PAGE_SIZE, COUNT));
		
		Calendar date = Calendar.getInstance();
		date.set(2000, Calendar.APRIL, 16);
		newOrder.setDateActivated(date.getTime());
		date.set(2070, Calendar.APRIL, 16);
		newOrder.setAutoExpireDate(date.getTime());
		date.set(2010, Calendar.APRIL, 16);
		OrderUtilTest.setDateStopped(newOrder, date.getTime());
		
		ServiceRequest result = translator.toFhirResource(newOrder);
		
		assertThat(result, notNullValue());
		assertThat(result.getStatus(), equalTo(ServiceRequest.ServiceRequestStatus.COMPLETED));
	}
	
	@Test
	public void toFhirResource_shouldTranslateOrderToActiveServiceRequest() throws Exception {
		TestOrder newOrder = new TestOrder();
		
		when(taskService.searchForTasks(any()))
		        .thenReturn(new MockIBundleProvider<>(Collections.emptyList(), PREFERRED_PAGE_SIZE, COUNT));
		
		Calendar date = Calendar.getInstance();
		date.set(2000, Calendar.APRIL, 16);
		newOrder.setDateActivated(date.getTime());
		date.set(2070, Calendar.APRIL, 16);
		newOrder.setAutoExpireDate(date.getTime());
		date.set(2069, Calendar.APRIL, 16);
		OrderUtilTest.setDateStopped(newOrder, date.getTime());
		
		ServiceRequest result = translator.toFhirResource(newOrder);
		
		assertThat(result, notNullValue());
		assertThat(result.getStatus(), equalTo(ServiceRequest.ServiceRequestStatus.ACTIVE));
	}
	
	@Test
	public void toFhirResource_shouldTranslateOrderToCompletedServiceRequest() throws Exception {
		TestOrder newOrder = new TestOrder();
		
		when(taskService.searchForTasks(any()))
		        .thenReturn(new MockIBundleProvider<>(Collections.emptyList(), PREFERRED_PAGE_SIZE, COUNT));
		
		Calendar date = Calendar.getInstance();
		date.set(2000, Calendar.APRIL, 16);
		newOrder.setDateActivated(date.getTime());
		date.set(2011, Calendar.APRIL, 16);
		newOrder.setAutoExpireDate(date.getTime());
		date.set(2010, Calendar.APRIL, 16);
		OrderUtilTest.setDateStopped(newOrder, date.getTime());
		
		ServiceRequest result = translator.toFhirResource(newOrder);
		
		assertThat(result, notNullValue());
		assertThat(result.getStatus(), equalTo(ServiceRequest.ServiceRequestStatus.COMPLETED));
	}
	
	@Test
	public void toFhirResource_shouldTranslateWrongOrderFromActiveToUnknownServiceRequest() throws Exception {
		TestOrder newOrder = new TestOrder();
		
		when(taskService.searchForTasks(any()))
		        .thenReturn(new MockIBundleProvider<>(Collections.emptyList(), PREFERRED_PAGE_SIZE, COUNT));
		
		Calendar date = Calendar.getInstance();
		date.set(2000, Calendar.APRIL, 16);
		newOrder.setDateActivated(date.getTime());
		date.set(2015, Calendar.APRIL, 16);
		newOrder.setAutoExpireDate(date.getTime());
		date.set(2010, Calendar.APRIL, 16);
		newOrder.setAction(Order.Action.DISCONTINUE);
		OrderUtilTest.setDateStopped(newOrder, date.getTime());
		
		ServiceRequest result = translator.toFhirResource(newOrder);
		
		assertThat(result, notNullValue());
		assertThat(result.getStatus(), equalTo(ServiceRequest.ServiceRequestStatus.UNKNOWN));
	}
	
	@Test
	public void toFhirResource_shouldTranslateWrongOrderFromCompleteToUnknownServiceRequest() throws Exception {
		TestOrder newOrder = new TestOrder();
		
		when(taskService.searchForTasks(any()))
		        .thenReturn(new MockIBundleProvider<>(Collections.emptyList(), PREFERRED_PAGE_SIZE, COUNT));
		
		Calendar date = Calendar.getInstance();
		date.set(2000, Calendar.APRIL, 16);
		newOrder.setDateActivated(date.getTime());
		date.set(2070, Calendar.APRIL, 16);
		newOrder.setAutoExpireDate(date.getTime());
		date.set(2069, Calendar.APRIL, 16);
		newOrder.setAction(Order.Action.DISCONTINUE);
		OrderUtilTest.setDateStopped(newOrder, date.getTime());
		
		ServiceRequest result = translator.toFhirResource(newOrder);
		
		assertThat(result, notNullValue());
		assertThat(result.getStatus(), equalTo(ServiceRequest.ServiceRequestStatus.REVOKED));
	}
	
	@Test
	public void toFhirResource_shouldTranslateOrderFromOnlyAutoExpireToCompleteServiceRequest() throws Exception {
		TestOrder newOrder = new TestOrder();
		
		when(taskService.searchForTasks(any()))
		        .thenReturn(new MockIBundleProvider<>(Collections.emptyList(), PREFERRED_PAGE_SIZE, COUNT));
		
		Calendar date = Calendar.getInstance();
		date.set(2000, Calendar.APRIL, 16);
		newOrder.setDateActivated(date.getTime());
		date.set(2015, Calendar.APRIL, 16);
		newOrder.setAutoExpireDate(date.getTime());
		
		ServiceRequest result = translator.toFhirResource(newOrder);
		
		assertThat(result, notNullValue());
		assertThat(result.getStatus(), equalTo(ServiceRequest.ServiceRequestStatus.COMPLETED));
	}
	
	@Test
	public void toFhirResource_shouldTranslateOrderFromOnlyDateStoppedToCompleteServiceRequest() throws Exception {
		TestOrder newOrder = new TestOrder();
		
		when(taskService.searchForTasks(any()))
		        .thenReturn(new MockIBundleProvider<>(Collections.emptyList(), PREFERRED_PAGE_SIZE, COUNT));
		
		Calendar date = Calendar.getInstance();
		date.set(2000, Calendar.APRIL, 16);
		newOrder.setDateActivated(date.getTime());
		date.set(2015, Calendar.APRIL, 16);
		OrderUtilTest.setDateStopped(newOrder, date.getTime());
		
		ServiceRequest result = translator.toFhirResource(newOrder);
		
		assertThat(result, notNullValue());
		assertThat(result.getStatus(), equalTo(ServiceRequest.ServiceRequestStatus.COMPLETED));
	}
	
	@Test
	public void toFhirResource_shouldTranslateFromNoDataToActiveServiceRequest() {
		TestOrder newOrder = new TestOrder();
		
		when(taskService.searchForTasks(any()))
		        .thenReturn(new MockIBundleProvider<>(Collections.emptyList(), PREFERRED_PAGE_SIZE, COUNT));
		
		ServiceRequest result = translator.toFhirResource(newOrder);
		
		assertThat(result, notNullValue());
		assertThat(result.getStatus(), equalTo(ServiceRequest.ServiceRequestStatus.ACTIVE));
	}
	
	@Test
	public void toFhirResource_shouldTranslateCode() {
		Concept openmrsConcept = new Concept();
		TestOrder testOrder = new TestOrder();
		
		testOrder.setConcept(openmrsConcept);
		
		when(taskService.searchForTasks(any()))
		        .thenReturn(new MockIBundleProvider<>(Collections.emptyList(), PREFERRED_PAGE_SIZE, COUNT));
		
		CodeableConcept codeableConcept = new CodeableConcept();
		Coding loincCoding = codeableConcept.addCoding();
		loincCoding.setSystem(FhirTestConstants.LOINC_SYSTEM_URL);
		loincCoding.setCode(LOINC_CODE);
		
		when(conceptTranslator.toFhirResource(openmrsConcept)).thenReturn(codeableConcept);
		
		CodeableConcept result = translator.toFhirResource(testOrder).getCode();
		
		assertThat(result, notNullValue());
		assertThat(result.getCoding(), notNullValue());
		assertThat(result.getCoding(), hasItem(hasProperty("system", equalTo(FhirTestConstants.LOINC_SYSTEM_URL))));
		assertThat(result.getCoding(), hasItem(hasProperty("code", equalTo(LOINC_CODE))));
	}
	
	@Test
	public void toFhirResource_shouldTranslateOccurrence() {
		TestOrder testOrder = new TestOrder();
		Date fromDate = new Date();
		Date toDate = new Date();
		
		testOrder.setDateActivated(fromDate);
		testOrder.setAutoExpireDate(toDate);
		
		when(taskService.searchForTasks(any()))
		        .thenReturn(new MockIBundleProvider<>(Collections.emptyList(), PREFERRED_PAGE_SIZE, COUNT));
		
		Period result = translator.toFhirResource(testOrder).getOccurrencePeriod();
		
		assertThat(result, notNullValue());
		assertThat(result.getStart(), equalTo(fromDate));
		assertThat(result.getEnd(), equalTo(toDate));
	}
	
	@Test
	public void toFhirResource_shouldTranslateOccurrenceWithMissingEffectiveStart() {
		TestOrder testOrder = new TestOrder();
		Date toDate = new Date();
		
		testOrder.setAutoExpireDate(toDate);
		
		when(taskService.searchForTasks(any()))
		        .thenReturn(new MockIBundleProvider<>(Collections.emptyList(), PREFERRED_PAGE_SIZE, COUNT));
		
		Period result = translator.toFhirResource(testOrder).getOccurrencePeriod();
		
		assertThat(result, notNullValue());
		assertThat(result.getStart(), nullValue());
		assertThat(result.getEnd(), equalTo(toDate));
	}
	
	@Test
	public void toFhirResource_shouldTranslateOccurrenceWithMissingEffectiveEnd() {
		TestOrder testOrder = new TestOrder();
		Date fromDate = new Date();
		
		testOrder.setDateActivated(fromDate);
		
		when(taskService.searchForTasks(any()))
		        .thenReturn(new MockIBundleProvider<>(Collections.emptyList(), PREFERRED_PAGE_SIZE, COUNT));
		
		Period result = translator.toFhirResource(testOrder).getOccurrencePeriod();
		
		assertThat(result, notNullValue());
		assertThat(result.getStart(), equalTo(fromDate));
		assertThat(result.getEnd(), nullValue());
	}
	
	@Test
	public void toFhirResource_shouldTranslateOccurrenceFromScheduled() {
		TestOrder testOrder = new TestOrder();
		Date fromDate = new Date();
		Date toDate = new Date();
		
		testOrder.setUrgency(TestOrder.Urgency.ON_SCHEDULED_DATE);
		testOrder.setScheduledDate(fromDate);
		testOrder.setAutoExpireDate(toDate);
		
		when(taskService.searchForTasks(any()))
		        .thenReturn(new MockIBundleProvider<>(Collections.emptyList(), PREFERRED_PAGE_SIZE, COUNT));
		
		Period result = translator.toFhirResource(testOrder).getOccurrencePeriod();
		
		assertThat(result, notNullValue());
		assertThat(result.getStart(), equalTo(fromDate));
		assertThat(result.getEnd(), equalTo(toDate));
	}
	
	@Test
	public void toFhirResource_shouldTranslateSubject() {
		TestOrder order = new TestOrder();
		Patient subject = new Patient();
		Reference subjectReference = new Reference();
		
		subject.setUuid(PATIENT_UUID);
		order.setUuid(SERVICE_REQUEST_UUID);
		order.setPatient(subject);
		subjectReference.setType(FhirConstants.PATIENT).setReference(FhirConstants.PATIENT + "/" + PATIENT_UUID);
		
		when(taskService.searchForTasks(any()))
		        .thenReturn(new MockIBundleProvider<>(Collections.emptyList(), PREFERRED_PAGE_SIZE, COUNT));
		when(patientReferenceTranslator.toFhirResource(subject)).thenReturn(subjectReference);
		
		Reference result = translator.toFhirResource(order).getSubject();
		
		assertThat(result, notNullValue());
		assertThat(result.getReference(), containsString(PATIENT_UUID));
	}
	
	@Test
	public void toFhirResource_shouldTranslateEncounter() {
		TestOrder order = new TestOrder();
		Encounter encounter = new Encounter();
		Reference encounterReference = new Reference();
		
		encounter.setUuid(ENCOUNTER_UUID);
		order.setUuid(SERVICE_REQUEST_UUID);
		order.setEncounter(encounter);
		encounterReference.setType(FhirConstants.ENCOUNTER).setReference(FhirConstants.ENCOUNTER + "/" + ENCOUNTER_UUID);
		
		when(taskService.searchForTasks(any()))
		        .thenReturn(new MockIBundleProvider<>(Collections.emptyList(), PREFERRED_PAGE_SIZE, COUNT));
		when(encounterReferenceTranslator.toFhirResource(encounter)).thenReturn(encounterReference);
		
		Reference result = translator.toFhirResource(order).getEncounter();
		
		assertThat(result, notNullValue());
		assertThat(result.getReference(), containsString(ENCOUNTER_UUID));
	}
	
	@Test
	public void toFhirResource_shouldInheritPerformerFromTaskOwner() {
		TestOrder order = new TestOrder();
		order.setUuid(SERVICE_REQUEST_UUID);
		
		when(taskService.searchForTasks(any())).thenReturn(
		    new MockIBundleProvider<>(setUpPerformerScenario(ORGANIZATION_UUID), PREFERRED_PAGE_SIZE, COUNT));
		
		Collection<Reference> result = translator.toFhirResource(order).getPerformer();
		
		assertThat(result, notNullValue());
		assertThat(result, hasSize(1));
		assertThat(result.iterator().next().getReference(), containsString(ORGANIZATION_UUID));
	}
	
	@Test
	public void toFhirResource_shouldTranslateRequester() {
		
		TestOrder order = new TestOrder();
		Provider requester = new Provider();
		Reference requesterReference = new Reference();
		
		requester.setUuid(PRACTITIONER_UUID);
		order.setUuid(SERVICE_REQUEST_UUID);
		order.setOrderer(requester);
		requesterReference.setType(FhirConstants.PRACTITIONER)
		        .setReference(FhirConstants.PRACTITIONER + "/" + PRACTITIONER_UUID);
		
		when(taskService.searchForTasks(any()))
		        .thenReturn(new MockIBundleProvider<>(Collections.emptyList(), PREFERRED_PAGE_SIZE, COUNT));
		when(practitionerReferenceTranslator.toFhirResource(requester)).thenReturn(requesterReference);
		
		Reference result = translator.toFhirResource(order).getRequester();
		
		assertThat(result, notNullValue());
		assertThat(result.getReference(), containsString(PRACTITIONER_UUID));
	}
	
	private List<Task> setUpBasedOnScenario(Task.TaskStatus status) {
		Reference basedOnRef = new Reference();
		Task task = new Task();
		task.setStatus(status);
		basedOnRef.setReference("ServiceRequest/" + SERVICE_REQUEST_UUID);
		basedOnRef.setType("ServiceRequest");
		task.addBasedOn(basedOnRef);
		
		return Collections.singletonList(task);
	}
	
	private List<Task> setUpPerformerScenario(String performerUuid) {
		Reference performerRef = new Reference();
		Task task = new Task();
		
		performerRef.setReference(FhirConstants.ORGANIZATION + "/" + performerUuid);
		performerRef.setType(FhirConstants.ORGANIZATION);
		
		task.setOwner(performerRef);
		
		return Collections.singletonList(task);
	}
	
	@Test
	public void shouldTranslateOpenMrsDateChangedToLastUpdatedDate() {
		TestOrder order = new TestOrder();
		order.setDateChanged(new Date());
		
		when(taskService.searchForTasks(any()))
		        .thenReturn(new MockIBundleProvider<>(Collections.emptyList(), PREFERRED_PAGE_SIZE, COUNT));
		
		ServiceRequest result = translator.toFhirResource(order);
		assertThat(result, notNullValue());
		assertThat(result.getMeta().getLastUpdated(), DateMatchers.sameDay(new Date()));
	}
	
	private TestOrder setOrderNumberByReflection(TestOrder order, String orderNumber) {
		try {
			Class clazz = order.getClass();
			Field orderNumberField = clazz.getSuperclass().getDeclaredField("orderNumber");
			Boolean isAccessible = orderNumberField.isAccessible();
			if (!isAccessible) {
				orderNumberField.setAccessible(true);
			}
			orderNumberField.set(((Order) order), orderNumber);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return order;
	}
}
