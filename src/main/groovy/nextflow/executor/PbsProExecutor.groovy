package nextflow.executor

import groovy.util.logging.Slf4j
import nextflow.processor.TaskRun

/**
 * Created by cerebis on 16/05/2016.
 */
@Slf4j
class PbsProExecutor extends PbsExecutor {
    static final int MAX_NAME_LENGTH = 15

    /**
     * Job names for PBSPro cannot be longer than 15 characters and
     * cannot use certain characters. The first character must be alphanumeric
     *
     * @param task
     * @return
     */
    @Override
    protected String getJobNameFor(TaskRun task) {
        String name = ('nf' + task.getName().replaceAll(/\W/) {''}).toString()
        if (name.size() <= MAX_NAME_LENGTH) {
            return name
        }
        else {
            return name.substring(0, MAX_NAME_LENGTH)
        }
    }

    @Override
    protected String getBeforeScriptlet(TaskRun task) {
        return "cd " + task.workDir.toString()
    }

    @Override
    protected List<String> getDirectives( TaskRun task, List<String> result ) {
        assert result !=null

        result << '-N' << getJobNameFor(task)
        result << '-o' << task.workDir.resolve(TaskRun.CMD_LOG).toString()
        result << '-j' << 'oe'
        result << '-V' << ''

        // the requested queue name
        if( task.config.queue ) {
            result << '-q'  << (String)task.config.queue
        }

        if( task.config.cpus > 1 ) {
            result << '-l' << "nodes=1:ppn=${task.config.cpus}"
        }

        // max task duration
        if( task.config.time ) {
            final duration = task.config.getTime()
            result << "-l" << "walltime=${duration.format('HH:mm:ss')}"
        }

        // task max memory
        if( task.config.memory ) {
            result << "-l" << "mem=${task.config.memory.toString().replaceAll(/[\s]/,'').toLowerCase()}"
        }

        // -- at the end append the command script wrapped file name
        if( task.config.clusterOptions ) {
            result << task.config.clusterOptions.toString() << ''
        }

        return result
    }
}
