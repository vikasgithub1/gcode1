/**
 *
 */

def updateBuildMetadataServiceNowSection(String changeTicketNum, String changeTicketID){
    unstash 'buildMetadataJsonFile'
    def buildJsonFile = readFile encoding: 'UTF-8', file: "${pipelineBuildMetadataFilename}"
    def buildJsonText = new groovy.json.JsonSlurperClassic().parseText(buildJsonFile)
    buildJsonText.servicenow.prodTicketID = changeTicketID
    buildJsonText.servicenow.prodTicketNum = changeTicketNum
    buildJsonText.servicenow.createTicketIndicator = false
    def formattedJson = getFormattedJson(buildJsonText)
    writeFile file: "${pipelineBuildMetadataFilename}", text: "${formattedJson}"
    stash includes: "${pipelineBuildMetadataFilename}", name: 'buildMetadataJsonFile'
    def gitCommitMessage = "Updated service now change ticket and number for ${env.BUILD_NUMBER}"
    pushUpdatedChangesToGitRepository(gitCommitMessage)
}

def updateBuildMetadataDeployStatus(String currentStageName){
    unstash 'buildMetadataJsonFile'
    def buildJsonFile = readFile encoding: 'UTF-8', file: "${pipelineBuildMetadataFilename}"
    def buildJsonText = new groovy.json.JsonSlurperClassic().parseText(buildJsonFile)
    def buildEnvironments = buildJsonText.environments
    for(int i=0; i<buildEnvironments.size(); i++){
        if(buildEnvironments.getAt(i).stage.toLowerCase().equals(currentStageName.toLowerCase())){
            buildEnvironments.getAt(i).status = "deployed"
            break
        }
    }
    def formattedJson = getFormattedJson(buildJsonText)
    writeFile file: "${pipelineBuildMetadataFilename}", text: "${formattedJson}"
    stash includes: "${pipelineBuildMetadataFilename}", name: 'buildMetadataJsonFile'
    def gitCommitMessage = "Updated deployment status of $currentStageName for ${env.BUILD_NUMBER}"
    pushUpdatedChangesToGitRepository(gitCommitMessage)
}

def updateBuildMetadataArtifactVersion(def valueMap){
    unstash 'buildMetadataJsonFile'
    def buildJsonFile = readFile encoding: 'UTF-8', file: "${pipelineBuildMetadataFilename}"
    def buildJsonText = new groovy.json.JsonSlurperClassic().parseText(buildJsonFile)
    buildJsonText.build.artifactVersion = valueMap.artifactVersion
    def formattedJson = getFormattedJson(buildJsonText)
    writeFile file: "${pipelineBuildMetadataFilename}", text: "${formattedJson}"
    stash includes: "${pipelineBuildMetadataFilename}", name: 'buildMetadataJsonFile'
    def gitCommitMessage = "Updated artifactversion for ${env.BUILD_NUMBER}"
    pushUpdatedChangesToGitRepository(gitCommitMessage)
}
@NonCPS
def getFormattedJson(def unformattedJson){
    def jsonFormatter = new groovy.json.JsonBuilder(unformattedJson)
    echo "${jsonFormatter.toPrettyString()}"
    return jsonFormatter.toPrettyString()
}

def pushUpdatedChangesToGitRepository(def commitMessage) {
    String branchName = pipelineBranchName.replaceAll("remotes/origin/", "")
    echo "${branchName}"
    echo "${envMap.pipelineRepoName}"
    echo "${envMap.ectmcmg2CredentialsId}"

    git(changelog: false,
        credentialsId: "${envMap.ectmcmg2CredentialsId}",
        poll: false,
        branch: "${branchName}",
        url: "${envMap.pipelineRepoName}")

    echo "Checkout successful"

    unstash 'buildMetadataJsonFile'

    //unknown@unknown.example.com
    sshagent(["${envMap.ectmcmg2CredentialsId}"]) {
        sh "/usr/local/git_194/bin/git show-ref"
        sh "/usr/local/git_194/bin/git -c user.name='ectmcmg2' -c user.email='unknown@unknown.example.com' add ${pipelineBuildMetadataFilename}"
        sh "/usr/local/git_194/bin/git -c user.name='ectmcmg2' -c user.email='unknown@unknown.example.com' commit -am '${commitMessage}'"
        String repo = envMap.pipelineRepoName.replaceAll("ectmcmg2@","")
        sh "/usr/local/git_194/bin/git config remote.origin.url ${repo}"
        echo "${repo}"
        retry(3){
            sh "/usr/local/git_194/bin/git -c user.name='ectmcmg2' -c user.email='unknown@unknown.example.com' pull   ${envMap.pipelineRepoName} ${branchName}"
            sh "/usr/local/git_194/bin/git -c user.name='ectmcmg2' -c user.email='unknown@unknown.example.com' push  ${envMap.pipelineRepoName} ${branchName}"
        }
    }
}

return this;
