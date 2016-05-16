package nextflow.executor

import nextflow.processor.TaskRun

/**
 * A simple class which implements only one method in a trivial form. This could
 * be taken as the default method implementation of  {@link AbstractGridExecutor}.
 */
abstract class BaseGridExecutor extends AbstractGridExecutor {

    /**
     * Trivial default return empty string, assuming most classes will not
     * require this method.
     *
     * @return null
     */
    protected String getBeforeScriptlet(TaskRun task) { "" }

}
