def sendNotificationAndWaitForApproval(String selEnv, def envs, def filterEnvs, def approvalTimeout, boolean isRollback, def recipients) {
    return {
        def selectedEnv = null
        for (int i = 0; i < envs.size(); i++) {
            if (selEnv.equals(envs.getAt(i).env)) {
                selectedEnv = envs.getAt(i)
                break
            }
        }
        if(selectedEnv != null) {
            if(selectedEnv.approval == true || isRollback) { //If Approval is Required
                echo "sftaapproval ${selectedEnv.approval}"
                String inputMessage = "Deployment Approval Request for ${selEnv}"
                if(isRollback) {
                    inputMessage = "Rollback To Previous Version Approval Request for ${selEnv}"
                }

                timeout(time: Integer.parseInt("${approvalTimeout.time}"), unit: "${approvalTimeout.unit}") {
                    def deploymentChoice = waitForApproval(inputMessage, "${approvalTimeout.approverGroup}", recipients)
                    if("Yes".equals(deploymentChoice) || "Yes".equals(deploymentChoice.Deployment)) { // Old Jenkins || New Jenkins non prod
                        filterEnvs.add(selEnv)
                        String submitterId = null
                        if(deploymentChoice instanceof String) { // Old jenkins
                            submitterId = auditMetadataUtility.getApproveUser()
                        }
                        else { // New Jenkins
                            echo "${deploymentChoice.submitterId}"
                            submitterId = deploymentChoice.submitterId
                        }
                        auditMetadataUtility.addApproverToAuditMap(selEnv, submitterId)
                    }
                    else {
                        def approver = "${deploymentChoice.submitterId}"
                        echo "${selEnv.toUpperCase()} deployment approval request is rejected by ${approver}"
                    }
                }
            }
            else { //No approval required
                filterEnvs.add(selEnv)
                auditMetadataUtility.addApproverToAuditMap(selEnv, "N/A")
            }
        }
        else { // Assume this section should not execute
            //TODO:
            utilFunctions.error("Need to investigate about this environment: ${selEnv}")
        }
    }
}

def waitForApproval(def inputMessage, String approverGroup, recipients) {
    def emailbody = readFile encoding: 'UTF-8', file: "src/resources/approval.html"
    emailbody = emailbody.replaceAll("<build_url>", "${env.BUILD_URL}input , to approve the deployment.")
    emailNotification.sendEmail(
            inputMessage,
            emailbody,
            recipients)
    def choice = new ChoiceParameterDefinition('Deployment', ['Yes', 'No'] as String[], '<b style="color:red">Note: Choose deployment option and select Proceed for either option</b>')
    if(approverGroup != null && approverGroup.trim().length() > 0 && approverGroup != "\"\"") {
        def deploymentChoice = input(id: 'DeploymentChoice',
                message: "${inputMessage}",
                parameters: [choice],
                submitterParameter: 'submitterId',
                submitter: "${approverGroup}")
        return deploymentChoice
    }
    else {
        def deploymentChoice = input(id: 'DeploymentChoice',
                message: "${inputMessage}",
                submitterParameter: 'submitterId',
                parameters: [choice])
        return deploymentChoice
    }
}

def promotionApproval(String inputMessage, def promotionApproval, def valueMap, String approverGroup) {
    echo "${promotionApproval}"
    def promotionChoice
    String submitterId
    recipients = "harinath_reddy_paidela@freddiemac.com"
    def emailbody = readFile encoding: 'UTF-8', file: "src/resources/approval.html"
    emailbody = emailbody.replaceAll("<build_url>", "${env.BUILD_URL}input , to approve the artifact promotion.")
    emailNotification.sendEmail(
            inputMessage,
            emailbody,
            recipients)
    timeout(time: Integer.parseInt("${promotionApproval.time}"), unit: "${promotionApproval.unit}") {
        def choice = new ChoiceParameterDefinition('Promotion', ['No', 'Yes'] as String[], '<b style="color:red">Note: Choose artifact promotion option and select Proceed for either option</b>')
        if(approverGroup != null && approverGroup.trim().length() > 0 && approverGroup != "\"\"") {
            promotionChoice = input(id: 'ArtifactPromotionChoice',
                    message: "${inputMessage}",
                    parameters: [choice],
                    submitterParameter: 'submitterId',
                    submitter: "${approverGroup}")
        }
        else {
            promotionChoice = input(id: 'DeploymentChoice',
                    message: "${inputMessage}",
                    submitterParameter: 'submitterId',
                    parameters: [choice])
        }
        if("Yes".equals(promotionChoice) || "Yes".equals(promotionChoice.Promotion)) { // Old Jenkins || New Jenkins non prod
            if(promotionChoice instanceof String) { // Old jenkins
                submitterId = auditMetadataUtility.getApproveUser()
            }
            else { // New Jenkins
                echo "${promotionChoice.submitterId}"
                submitterId = promotionChoice.submitterId
            }
            valueMap.put("PromotionApproved",true)
            valueMap.put("PromotionApprovedBy","$submitterId")
        }
        else {
            def approver = "${promotionChoice.submitterId}"
            echo "Artifact Promotion approval request is rejected by ${approver}"
            valueMap.put("PromotionApproved",false)
            valueMap.put("PromotionApprovedBy","$approver")
        }

    }
}

return this;
