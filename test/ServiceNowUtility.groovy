/**
 *
 */

def createOrUpdateTicketIfValid(def valueMap) {
    def serviceNowDetails = valueMap.servicenow

    if((serviceNowDetails.createTicketIndicator) && !utilFunctions.isValueExist(serviceNowDetails.prodTicketNum) && !utilFunctions.isValueExist(serviceNowDetails.prodTicketID)){
        createServiceNowTicket(serviceNowDetails,valueMap)
        valueMap.put("isServiceNowUploadRequired",true)
    }else if (utilFunctions.isValueExist(serviceNowDetails.prodTicketNum) && utilFunctions.isValueExist(serviceNowDetails.prodTicketID)) {        //Find status
        String status = checkServiceNowTicketStatus(serviceNowDetails.ticketType, serviceNowDetails.prodTicketID)
        auditMap.put("ChangeTicket",serviceNowDetails.prodTicketNum)
        auditMap.put("ScheduledStartDateAndTime",serviceNowDetails.scheduledStartDateAndTime)
        valueMap.put("ticketId",serviceNowDetails.prodTicketID)
        valueMap.put("ticketType",serviceNowDetails.ticketType)
        if("prod".equals(envMap.type)){
            if(!("implementation".equals(status))){
                libMap.pipelineLogger.logMessage(libMap.loglevel_ERROR, "Change Ticket is not in implementation state.")
                error "Change Ticket is not in implementation state."
                valueMap.put("isServiceNowUploadRequired",false)
            }
        }else{
            if(("scheduled".equals(status)
                    || "implementation".equals(status)
                    || "completed".equals(status)
                    || "closed".equals(status)
                    || "cancelled".equals(status))){
                echo """************************************************************
                WARNING WARNING WARNING
                Warning: The Change Ticket is in $status state".
                ************************************************************"""
                valueMap.put("isServiceNowUploadRequired",false)
            }
        }
    }else if (utilFunctions.isValueExist(serviceNowDetails.prodTicketNum) || utilFunctions.isValueExist(serviceNowDetails.prodTicketID)) {
        if("prod".equals(envMap.type)){
            libMap.pipelineLogger.logMessage(libMap.loglevel_ERROR, "Change Ticket is not populated in JSON file. Pipeline cannot proceed without ChangeTicket")
            error "Change Ticket is not populated in JSON file. Pipeline cannot proceed without ChangeTicket"
        }else{
            echo """************************************************************
            WARNING WARNING WARNING
            Warning: The Change Ticket is not created for this release. Please create the Change Ticket".
            ************************************************************"""

        }
        valueMap.put("isServiceNowUploadRequired",false)
        auditMap.put("ChangeTicket","N/A")
        auditMap.put("ScheduledStartDateAndTime","N/A")
    }

    /*if(serviceNowDetails.createTicketIndicator) {
        // New Ticket
        if ((!serviceNowDetails.prodTicketID?.trim()) && (!serviceNowDetails.prodTicketNum?.trim())) {
            createServiceNowTicket(serviceNowDetails,valueMap)
        } else if ((serviceNowDetails.prodTicketID?.trim()) && (serviceNowDetails.prodTicketNum?.trim())) {
            //Find status
            String status = checkServiceNowTicketStatus(serviceNowDetails.ticketType, serviceNowDetails.prodTicketID)
            auditMap.put("ChangeTicket",serviceNowDetails.prodTicketNum)
            auditMap.put("ScheduledStartDateAndTime",serviceNowDetails.scheduledStartDateAndTime)
            valueMap.put("ticketId",serviceNowDetails.prodTicketID)
            valueMap.put("ticketType",serviceNowDetails.ticketType)
            if(!("scheduled".equals(status)
                    || "implementation".equals(status)
                    || "completed".equals(status)
                    || "closed".equals(status)
                    || "cancelled".equals(status))) {
                //TODO - Determine updating ticket with attachments
            }
        }
        else if ((serviceNowDetails.prodTicketID?.trim()) || (serviceNowDetails.prodTicketNum?.trim())) {
            libMap.pipelineLogger.logMessage(libMap.loglevel_ERROR, "Create servicenow ticket error - When createTicketIndicator is true, both prodTicketID and prodTicketNum need to be empty or null")
            error "Create servicenow ticket error - When createTicketIndicator is true, both prodTicketID and prodTicketNum need to be empty or null"
        }
    }else{
        auditMap.put("ChangeTicket","N/A")
        auditMap.put("ScheduledStartDateAndTime","N/A")
    }*/
}

def createServiceNowTicket(def serviceNowDetails, def valueMap) {
    codeBaselineId = metadataFileName
    try {
        def serviceNowChangeTicketObj =
            groovy.json.JsonOutput.toJson([
                short_description: serviceNowDetails.summary,
                u_change_line_of_support2: serviceNowDetails.lineOfSupport,
                cmdb_ci: serviceNowDetails.affectedCI,
                u_project_type: serviceNowDetails.projectType,
                type: serviceNowDetails.type,
                u_methodology: serviceNowDetails.methodology,
                u_environment: serviceNowDetails.environment,
                u_operational_subcategory: serviceNowDetails.operationalSubCategory,
                assignment_group: serviceNowDetails.changeGroup,
                u_operational_category: serviceNowDetails.operationalCategory,
                end_date: convertScheduledDateAndTimeToUTC(serviceNowDetails.scheduledEndDateAndTime),
                start_date: convertScheduledDateAndTimeToUTC(serviceNowDetails.scheduledStartDateAndTime),
                u_code_baseline_id: codeBaselineId,
                assigned_to: serviceNowDetails.changeCoordinator
            ])

        echo "${serviceNowChangeTicketObj}"
        echo "${envMap.serviceNowAccountName}"
        echo "${serviceNowDetails.ticketType}"

        def createServiceNowTicketResult = libMap.serviceNowInstance.serviceNowTicket(
                                          "${envMap.serviceNowAccountName}",
                                          "${serviceNowDetails.ticketType}",
                                          "${serviceNowChangeTicketObj}")

        echo "${createServiceNowTicketResult}"

        if( createServiceNowTicketResult != null ) {
            def errorCheck = checkErrorInResult(createServiceNowTicketResult)
            if(errorCheck) {
                error "${createServiceNowTicketResult}"
            }
            libMap.pipelineLogger.logMessage(libMap.loglevel_DEBUG, "SFTA create new ServiceNow ticket completed...")
            def serviceNowResultJson = new groovy.json.JsonSlurperClassic().parseText(createServiceNowTicketResult)
            CHANGE_TICKET = serviceNowResultJson.changeTickeNumber
            TICKET_ID = serviceNowResultJson.systemId
            TICKET_TYPE = serviceNowDetails.ticketType
            //TODO: Determine updating change ticket
            echo "Change ticket = ${CHANGE_TICKET}"
            echo "Change sys id = ${TICKET_ID}"

            auditMap.put("ChangeTicket", CHANGE_TICKET)
            auditMap.put("ScheduledStartDateAndTime",serviceNowDetails.scheduledStartDateAndTime)
            valueMap.put("ticketId",TICKET_ID)
            valueMap.put("ticketType",serviceNowDetails.ticketType)

            gitUpdateUtility.updateBuildMetadataServiceNowSection(
                    CHANGE_TICKET,
                    TICKET_ID)
        }
    }
    catch(createTicketErr){
        throw createTicketErr
    }
}

def checkServiceNowTicketStatus(String ticketType, String ticketId) {
    String status = null
    def serviceNowTicketStatusResult =
            checkServiceNowTicketStatusStep(
                    serviceNowAccount: envMap.serviceNowAccountName,
                    serviceNowTicketID: "${ticketId}",
                    ticketType: "${ticketType}")

    if(serviceNowTicketStatusResult != null) {
        def errorCheck = checkErrorInResult(serviceNowTicketStatusResult)
        if(errorCheck){
            error "${serviceNowTicketStatusResult}"
        }
        def serviceNowStatusJsonText = new groovy.json.JsonSlurperClassic().parseText(serviceNowTicketStatusResult)
        echo "Ticket status = ${serviceNowStatusJsonText.result.u_stage}"
        status = serviceNowStatusJsonText.result.u_stage
    } else{
        error "${serviceNowTicketStatusResult}"
    }
    return status
}

def uploadFilesToServiceNowTicket(String ticketId, String ticketType, String listOfAttachments){
    def workspace = pwd()
    try {
        def uploadFilesToTicketStatus = uploadFilesToServiceNowTicketStep attachments: "${listOfAttachments}", serviceNowAccount: envMap.serviceNowAccountName, serviceNowTicketID: "${ticketId}", ticketType: "${ticketType}"
        echo "Upload status = ${uploadFilesToTicketStatus}"

    }catch(uploadFilesErr){
        err = uploadFilesErr
        libMap.pipelineLogger.logMessage(libMap.loglevel_ERROR, "uploadFilesToServiceNowTicket failed..." + uploadFilesErr)
        throw err
    }
}

def convertScheduledDateAndTimeToUTC(String scheduledDateAndTimeValue){
    if(scheduledDateAndTimeValue?.trim()) {
        java.text.DateFormat dateFormatter = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        Date actualDate = dateFormatter.parse(scheduledDateAndTimeValue)
        dateFormatter.setTimeZone(java.util.TimeZone.getTimeZone("UTC"))
        return dateFormatter.format(actualDate)
    }
}

@NonCPS
def checkErrorInResult(def serviceNowTicketResult) {
  def regexMatcher = (serviceNowTicketResult =~ /error/)
  regexMatcher ? regexMatcher : null
}

return this;
