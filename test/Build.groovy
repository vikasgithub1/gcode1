/**
 *
 */

def buildGenericPackages(def valueMap) {
  def buildParallel = [:]
  //build repos
  for(int i=0; i<valueMap.buildRepos.size(); i++) {
    valueMap.buildRepos.getAt(i).artifactVersion = "${valueMap.releaseVersion}-${env.BUILD_NUMBER}"
    buildParallel[valueMap.buildRepos.getAt(i).sourceDir] = buildParallelRepos(valueMap.buildRepos.getAt(i), valueMap.releaseVersion)
  }
  buildParallel.failFast = true
  parallel buildParallel

}

def buildParallelRepos(def repo, def releaseVersion) {
  return {
    dir("sfta_${repo.sourceDir}/${repo.sourceDir}") {
    withEnv([
      "PATH=/usr/local/bin:/bin:/usr/bin:/usr/atria/bin",
      "JAVA_HOME=${tool envMap.jdkVersion}",
      "PATH+MAVEN=${tool envMap.mavenVersion}/bin:${env.JAVA_HOME}/bin",
      "BUILD_LABEL=${env.BUILD_NUMBER}"
    ]) {

        if (repo.sourceDir.equals("process")) {
          sh "${tool envMap.mavenVersion}/bin/mvn versions:set -DnewVersion=${releaseVersion}-${env.BUILD_LABEL} -DgenerateBackupPoms=false -DprocessParent=true"
          sh "${tool envMap.mavenVersion}/bin/mvn clean install -Punix-tar1 -Dmaven.test.skip=true"
          sh "${tool envMap.mavenVersion}/bin/mvn clean install -Punix-tar2 -Dmaven.test.skip=true"
        } else if (repo.sourceDir.equals("service")) {
          sh "${tool envMap.mavenVersion}/bin/mvn versions:set -f cdw-service/pom.xml -DnewVersion=${releaseVersion}-${env.BUILD_LABEL} -DgenerateBackupPoms=false -DprocessParent=true"
          sh "${tool envMap.mavenVersion}/bin/mvn versions:set -f prds-service/pom.xml -DnewVersion=${releaseVersion}-${env.BUILD_LABEL} -DgenerateBackupPoms=false -DprocessParent=true"
          sh "${tool envMap.mavenVersion}/bin/mvn versions:set -f sfta-tds-service/pom.xml -DnewVersion=${releaseVersion}-${env.BUILD_LABEL} -DgenerateBackupPoms=false -DprocessParent=true"
          sh "${tool envMap.mavenVersion}/bin/mvn versions:set -f clean-service/pom.xml -DnewVersion=${releaseVersion}-${env.BUILD_LABEL} -DgenerateBackupPoms=false -DprocessParent=true"
          sh "${tool envMap.mavenVersion}/bin/mvn clean install -f cdw-service/pom.xml -Punix -Dmaven.test.skip=true"
          sh "${tool envMap.mavenVersion}/bin/mvn clean install -f prds-service/pom.xml -Punix -Dmaven.test.skip=true"
          sh "${tool envMap.mavenVersion}/bin/mvn clean install -f sfta-tds-service/pom.xml -Punix -Dmaven.test.skip=true"
          sh "${tool envMap.mavenVersion}/bin/mvn clean install -f clean-service/pom.xml -Punix -Dmaven.test.skip=true"
        } else {
          sh "${tool envMap.mavenVersion}/bin/mvn versions:set -DnewVersion=${releaseVersion}-${env.BUILD_LABEL} -DgenerateBackupPoms=false -DprocessParent=true"
          sh "${tool envMap.mavenVersion}/bin/mvn clean install -Punix -Dmaven.test.skip=true"
        }
      }
    }
  }
}

def copyAndTarArtifacts(def valueMap) {
  for(int i=0; i<valueMap.buildRepos.size(); i++) {
    def repo = valueMap.buildRepos.getAt(i)
    if(repo.sourceDir.equals("gui") || repo.sourceDir.equals("orchestrator") || repo.sourceDir.equals("report")) {
        dir("ntad_build/ui") {
          sh "cp ../../sfta_${repo.sourceDir}/${repo.sourceDir}/target/*.war ."
        }
    } else if(repo.sourceDir.equals("cache")) {
        dir("ntad_build/cache") {
          sh "cp ../../sfta_${repo.sourceDir}/${repo.sourceDir}/target/*.war ."
        }
    } else if(repo.sourceDir.equals("process")) {
        dir("ntad_build/process") {
          sh "cp ../../sfta_${repo.sourceDir}/${repo.sourceDir}/ad/target/*.war ."
          sh "cp ../../sfta_${repo.sourceDir}/${repo.sourceDir}/cr/target/*.war ."
          sh "cp ../../sfta_${repo.sourceDir}/${repo.sourceDir}/dl/target/*.war ."
          sh "cp ../../sfta_${repo.sourceDir}/${repo.sourceDir}/fig/target/*.war ."
          sh "cp ../../sfta_${repo.sourceDir}/${repo.sourceDir}/lq/target/*.war ."
          sh "cp ../../sfta_${repo.sourceDir}/${repo.sourceDir}/ms/target/*.war ."
          sh "cp ../../sfta_${repo.sourceDir}/${repo.sourceDir}/po/target/*.war ."
          sh "cp ../../sfta_${repo.sourceDir}/${repo.sourceDir}/pu/target/*.war ."
          sh "cp ../../sfta_${repo.sourceDir}/${repo.sourceDir}/ri/target/*.war ."
          sh "cp ../../sfta_${repo.sourceDir}/${repo.sourceDir}/rs/target/*.war ."
          sh "cp ../../sfta_${repo.sourceDir}/${repo.sourceDir}/etl/target/*.war ."
          sh "cp ../../sfta_${repo.sourceDir}/${repo.sourceDir}/li/target/*.war ."
          sh "cp ../../sfta_${repo.sourceDir}/${repo.sourceDir}/sc/target/*.war ."
          sh "cp ../../sfta_${repo.sourceDir}/${repo.sourceDir}/lm/target/*.war ."
          sh "cp ../../sfta_${repo.sourceDir}/${repo.sourceDir}/se/target/*.war ."
          sh "cp ../../sfta_${repo.sourceDir}/${repo.sourceDir}/re/target/*.war ."
          sh "cp ../../sfta_${repo.sourceDir}/${repo.sourceDir}/rc/target/*.war ."
          sh "cp ../../sfta_${repo.sourceDir}/${repo.sourceDir}/hf/target/*.war ."
          sh "cp ../../sfta_${repo.sourceDir}/${repo.sourceDir}/va/target/*.war ."
          sh "tar -czvf sfta-process-${repo.artifactVersion}.tar.gz *.war"
        }
    }
  }
}

return this;
