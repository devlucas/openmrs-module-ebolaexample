/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */

package org.openmrs.module.ebolaexample.fragment.controller.overview;

import org.apache.commons.lang.time.DateFormatUtils;
import org.openmrs.Location;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifierType;
import org.openmrs.PersonAttribute;
import org.openmrs.PersonAttributeType;
import org.openmrs.api.context.Context;
import org.openmrs.module.appframework.context.AppContextModel;
import org.openmrs.module.appui.UiSessionContext;
import org.openmrs.module.coreapps.contextmodel.PatientContextModel;
import org.openmrs.module.coreapps.contextmodel.VisitContextModel;
import org.openmrs.module.ebolaexample.metadata.EbolaMetadata;
import org.openmrs.module.emrapi.adt.AdtService;
import org.openmrs.module.emrapi.patient.PatientDomainWrapper;
import org.openmrs.module.emrapi.visit.VisitDomainWrapper;
import org.openmrs.module.metadatadeploy.MetadataUtils;
import org.openmrs.ui.framework.annotation.FragmentParam;
import org.openmrs.ui.framework.annotation.InjectBeans;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.fragment.FragmentConfiguration;
import org.openmrs.ui.framework.fragment.FragmentModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Ideally you pass in a PatientDomainWrapper as the "patient" config parameter. But if you pass in
 * a Patient, then this controller will wrap that for you.
 */
public class PatientHeaderFragmentController {

    public void controller(FragmentConfiguration config,
                           @FragmentParam(required = false, value = "appContextModel") AppContextModel appContextModel,
                           @FragmentParam("patient") Object patient,
                           @InjectBeans PatientDomainWrapper wrapper,
                           @SpringBean("adtService") AdtService adtService, UiSessionContext sessionContext,
                           FragmentModel model) {

        if (patient instanceof Patient) {
            wrapper.setPatient((Patient) patient);
            config.addAttribute("patient", wrapper);
        } else {
            wrapper = (PatientDomainWrapper) patient;
        }

        VisitDomainWrapper activeVisit = (VisitDomainWrapper) config.getAttribute("activeVisit");
        if (activeVisit == null) {
            try {
                Location visitLocation = adtService.getLocationThatSupportsVisits(sessionContext.getSessionLocation());
                activeVisit = adtService.getActiveVisit(wrapper.getPatient(), visitLocation);
            } catch (IllegalArgumentException ex) {
                // location does not support visits
            }
        }

        if (appContextModel == null) {
            AppContextModel contextModel = sessionContext.generateAppContextModel();
            contextModel.put("patient", new PatientContextModel(wrapper.getPatient()));
            contextModel.put("visit", activeVisit == null ? null : new VisitContextModel(activeVisit));
            model.addAttribute("appContextModel", contextModel);
        }

        if (activeVisit != null) {
            config.addAttribute("activeVisit", activeVisit);
            config.addAttribute("activeVisitStartDatetime",
                    DateFormatUtils.format(activeVisit.getStartDatetime(), "dd MMM yyyy hh:mm a", Context.getLocale()));
        }

        List<ExtraPatientIdentifierType> extraPatientIdentifierTypes = new ArrayList<ExtraPatientIdentifierType>();

        config.addAttribute("extraPatientIdentifierTypes", extraPatientIdentifierTypes);
        config.addAttribute("extraPatientIdentifiersMappedByType", wrapper.getExtraIdentifiersMappedByType(sessionContext.getSessionLocation()));
        config.addAttribute("defaultDashboard", null);

        PersonAttribute nextOfKinName = wrapper.getPatient().getAttribute(MetadataUtils.existing(PersonAttributeType.class, EbolaMetadata._PersonAttributeType.NEXT_OF_KIN_NAME));
        PersonAttribute nextOfKinPhone = wrapper.getPatient().getAttribute(MetadataUtils.existing(PersonAttributeType.class, EbolaMetadata._PersonAttributeType.NEXT_OF_KIN_PHONE));
        model.addAttribute("nextOfKinName", nextOfKinName);
        model.addAttribute("nextOfKinPhone", nextOfKinPhone);
    }

    public class ExtraPatientIdentifierType {

        private PatientIdentifierType patientIdentifierType;

        private boolean editable = false;

        public ExtraPatientIdentifierType(PatientIdentifierType type, boolean editable) {
            this.patientIdentifierType = type;
            this.editable = editable;
        }

        public PatientIdentifierType getPatientIdentifierType() {
            return patientIdentifierType;
        }

        public void setPatientIdentifierType(PatientIdentifierType patientIdentifierType) {
            this.patientIdentifierType = patientIdentifierType;
        }

        public boolean isEditable() {
            return editable;
        }

        public void setEditable(boolean editable) {
            this.editable = editable;
        }
    }

}