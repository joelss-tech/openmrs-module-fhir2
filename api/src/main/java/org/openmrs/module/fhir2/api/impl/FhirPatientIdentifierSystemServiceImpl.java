/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.fhir2.api.impl;

import javax.annotation.Nonnull;

import java.util.Optional;

import lombok.AccessLevel;
import lombok.Setter;
import org.openmrs.PatientIdentifierType;
import org.openmrs.module.fhir2.api.FhirPatientIdentifierSystemService;
import org.openmrs.module.fhir2.api.dao.FhirPatientIdentifierSystemDao;
import org.openmrs.module.fhir2.model.FhirPatientIdentifierSystem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
@Setter(AccessLevel.PACKAGE)
public class FhirPatientIdentifierSystemServiceImpl implements FhirPatientIdentifierSystemService {
	
	@Autowired
	private FhirPatientIdentifierSystemDao dao;
	
	@Override
	@Transactional(readOnly = true)
	public String getUrlByPatientIdentifierType(@Nonnull PatientIdentifierType patientIdentifierType) {
		return dao.getUrlByPatientIdentifierType(patientIdentifierType);
	}
	
	@Override
	@Transactional(readOnly = true)
	public Optional<FhirPatientIdentifierSystem> getFhirPatientIdentifierSystem(
	        @Nonnull PatientIdentifierType patientIdentifierType) {
		return dao.getFhirPatientIdentifierSystem(patientIdentifierType);
	}
	
	@Override
	@Transactional
	public FhirPatientIdentifierSystem saveFhirPatientIdentifierSystem(
	        @Nonnull FhirPatientIdentifierSystem fhirPatientIdentifierSystem) {
		return dao.saveFhirPatientIdentifierSystem(fhirPatientIdentifierSystem);
	}
}
