/**
 *
 */

def checkoutSourceRepos(def repos, String gitTool) {
  //git checkout is parallel and so avoid duplicate name for src dir
  def checkoutRepos = []
  for(int i=0; i<repos.size(); i++) {
    def repo = [:]
    repo.put("repoName", repos.getAt(i).repoName)
    repo.put("branchOrTagname", repos.getAt(i).branchOrTagname)
    repo.put("sourceDir", "sfta_" + repos.getAt(i).sourceDir)
    checkoutRepos.add(repo)
  }
  def checkoutArgs = [:]
  checkoutArgs.put("repo",checkoutRepos)
  checkoutArgs.put("gitTool","${gitTool}")
  checkoutArgs.put("credentialsId","${envMap.ectmcmg2CredentialsId}")
  libMap.gitCheckoutLib.execute(com.freddiemac.ba0099.pipeline.git.GitCheckoutLib.OptionType.checkoutSourceRepos, checkoutArgs)
  libMap.pipelineLogger.logMessage(libMap.loglevel_DEBUG, "SFTA git checkout completed...")
}

def checkoutSingleRepo(def repo, String gitTool) {
  /*def checkoutArgs = [:]
  checkoutArgs.put("repo",repo)
  checkoutArgs.put("gitTool","${gitTool}")
  checkoutArgs.put("credentialsId","${envMap.ectmcmg2CredentialsId}")
  libMap.gitCheckoutLib.execute(com.freddiemac.ba0099.pipeline.git.GitCheckoutLib.OptionType.checkoutSingleRepo, checkoutArgs)
  libMap.pipelineLogger.logMessage(libMap.loglevel_DEBUG, "SFTA git checkout completed...")*/
  dir("${repo.sourceDir}") {
      checkout([$class                           : 'GitSCM',
                branches                         : [[name: "${repo.branchOrTagname}"]],
                doGenerateSubmoduleConfigurations: false,
                extensions                       : [[$class: 'CheckoutOption', timeout: 15]],
                gitTool                          : "${gitTool}",
                submoduleCfg                     : [],
                userRemoteConfigs                : [
                        [credentialsId: "${envMap.ectmcmg2CredentialsId}",
                         url: "${repo.repoName}"]
                ]])
  }

}

return this;
