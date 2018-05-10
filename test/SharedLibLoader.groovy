/**
 *
 */
import com.freddiemac.ba0099.pipeline.logging.PipelineLogger
import com.freddiemac.ba0099.pipeline.git.GitCheckoutLib
import com.freddiemac.ba0099.pipeline.service.ServiceNow

def initialize() {
  def valueMap = [:]
  def loginitParams = [:]
  loginitParams.put("pipelineName", "${env.JOB_NAME}")
  loginitParams.put("applicationName", "SFTA")
  def pipelineLogger = PipelineLogger.of(this, steps, loginitParams)
  valueMap.put("pipelineLogger", pipelineLogger)
  valueMap.put("loglevel_DEBUG", PipelineLogger.LogLevel.DEBUG)
  valueMap.put("loglevel_ERROR", PipelineLogger.LogLevel.ERROR)
  valueMap.put("loglevel_INFO", PipelineLogger.LogLevel.INFO)

  def gitCheckoutLib = GitCheckoutLib.of(this, steps, pipelineLogger)
  valueMap.put("gitCheckoutLib", gitCheckoutLib)

  def serviceNowInstance = ServiceNow.getInstance(this, steps, pipelineLogger)
  valueMap.put("serviceNowInstance", serviceNowInstance)

  return valueMap
}

return this;
