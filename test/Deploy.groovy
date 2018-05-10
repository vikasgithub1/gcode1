/**
 *
 *
 */

def deployGenericArtifacts(def valueMap, def targets, String stageName, def deployApps) {
    def WORKSPACE = pwd()
    for (int i=0; i<deployApps.size(); i++) {
      String deployApp = deployApps.getAt(i).sourceDir
      String version = deployApps.getAt(i).artifactVersion
      String role = deployApps.getAt(i).role.capitalize()
      String group = deployApps.getAt(i).group

      withEnv(["PATH=/usr/local/bin:/bin:/usr/bin:/usr/atria/bin",
               "JAVA_HOME=${tool envMap.jdkVersion}",
               "PATH+MAVEN=${tool envMap.mavenVersion}/bin:${env.JAVA_HOME}/bin"]) {
          try {
            ansiblePlaybook(
                    playbook: "${WORKSPACE}/ansible_scripts/${role}DeploymentPlaybook.yml",
                    inventory: "${WORKSPACE}/ansible_scripts/inventory/${targets}",
                    colorizedOutput: true,
                    extras: "--limit ${targets} -e 'JAVA_HOME=${env.JAVA_HOME} M2_HOME=${env.M2_HOME} app_group=${group}  stage=${targets} app=${deployApp} version=${version}'")
          } catch(ansibleError) {
            rollbackToPrevVersion(deployApps.getAt(i), targets, stageName)
            echo "Error occurred during deployment of " + deployApp + " with version " + version + "....roll back to previous version is completed"
          }
      }
    }
}

def rollback(def valueMap, def targets, String stageName) {
    def WORKSPACE = pwd()
    for (int i=0; i<valueMap.rollbackRepos.size(); i++) {
      rollbackToPrevVersion(valueMap.rollbackRepos.getAt(i), targets, stageName)
    }

}

def rollbackToPrevVersion(def rollbackRepo, def targets, String stageName) {

  withEnv(["PATH=/usr/local/bin:/bin:/usr/bin:/usr/atria/bin",
           "JAVA_HOME=${tool envMap.jdkVersion}",
           "PATH+MAVEN=${tool envMap.mavenVersion}/bin:${env.JAVA_HOME}/bin"]) {
      ansiblePlaybook(
              playbook: "${WORKSPACE}/ansible_scripts/RollbackPlaybook.yml",
              inventory: "${WORKSPACE}/ansible_scripts/inventory/${targets}",
              colorizedOutput: true,
              extras: "--limit ${targets} -e 'stage=${targets} app=${rollbackRepo.sourceDir} role=${rollbackRepo.role} app_group=${rollbackRepo.group} version=${rollbackRepo.artifactVersion}'")
  }

}

def restartServer(def valueMap, def targets, String stageName, def apps) {
    def WORKSPACE = pwd()
    for (int i=0; i<apps.size(); i++) {
      ansiblePlaybook(
              playbook: "${WORKSPACE}/ansible_scripts/RestartPlaybook.yml",
              inventory: "${WORKSPACE}/ansible_scripts/inventory/${targets}",
              colorizedOutput: true,
              extras: "--limit ${targets} -e 'app=${apps.getAt(i).sourceDir} role=${apps.getAt(i).role} app_group=${apps.getAt(i).group}'")
    }

}

def restartServerUsingSudo(def valueMap, def targets, String stageName, def apps) {
    def WORKSPACE = pwd()
    for (int i=0; i<apps.size(); i++) {
      ansiblePlaybook(
              playbook: "${WORKSPACE}/ansible_scripts/RestartPlaybookwithSudo.yml",
              inventory: "${WORKSPACE}/ansible_scripts/inventory/${targets}",
              colorizedOutput: true,
              extras: "-vv --limit ${targets} -e 'app=${apps.getAt(i).sourceDir} role=${apps.getAt(i).role} app_group=${apps.getAt(i).group}'")
    }

}

return this;
