/*
 * Copyright (c) 2013-2016, Centre for Genomic Regulation (CRG).
 * Copyright (c) 2013-2016, Paolo Di Tommaso and the respective authors.
 *
 *   This file is part of 'Nextflow'.
 *
 *   Nextflow is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   Nextflow is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with Nextflow.  If not, see <http://www.gnu.org/licenses/>.
 */

package nextflow.executor
import java.nio.file.Path

import groovy.transform.PackageScope
import nextflow.processor.TaskRun
/**
 * Execute a task script by running it on the SGE/OGE cluster
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
class SgeExecutor extends BaseGridExecutor {

    /**
     * Gets the directives to submit the specified task to the cluster for execution
     *
     * @param task A {@link TaskRun} to be submitted
     * @param result The {@link List} instance to which add the job directives
     * @return A {@link List} containing all directive tokens and values.
     */
    protected List<String> getDirectives(TaskRun task, List<String> result) {

        result << '-wd' << task.workDir.toString()
        result << '-N' << getJobNameFor(task)
        result << '-o' << task.workDir.resolve(TaskRun.CMD_LOG).toString()
        result << '-j' << 'y'
        result << '-terse' << ''    // note: directive need to be returned as pairs
        result << '-V' << ''        // for this reason an empty string value is added for flag options

        /*
         * By using command line option -notify SIGUSR1 will be sent to your script prior to SIGSTOP
         * and SIGUSR2 will be sent to your script prior to SIGKILL
         */
        result << '-notify' << ''

        // the requested queue name
        if( task.config.queue ) {
            result << '-q' << (task.config.queue as String)
        }

        //number of cpus for multiprocessing/multi-threading
        if ( task.config.penv ) {
            result << "-pe" << "${task.config.penv} ${task.config.cpus}"
        }
        else if( task.config.cpus>1 ) {
            result << "-l" << "slots=${task.config.cpus}"
        }

        // max task duration
        if( task.config.time ) {
            final time = task.config.getTime()
            result << "-l" << "h_rt=${time.format('HH:mm:ss')}"
        }

        // task max memory
        if( task.config.memory ) {
            result << "-l" << "virtual_free=${task.config.memory.toString().replaceAll(/[\sB]/,'')}"
        }

        // -- at the end append the command script wrapped file name
        if( task.config.clusterOptions ) {
            result << task.config.clusterOptions.toString() << ''
        }

        return result
    }


    /*
     * Prepare the 'qsub' cmdline
     */
    List<String> getSubmitCommandLine(TaskRun task, Path scriptFile ) {

        // The '-terse' command line control the output of the qsub command line, when
        // used it only return the ID of the submitted job.
        // NOTE: In some SGE implementations the '-terse' only works on the qsub command line
        // and it is ignored when used in the script job as directive, fir this reason it
        // should not be remove from here
        return ['qsub', '-terse', scriptFile.name]
    }

    protected String getHeaderToken() { '#$' }


    /**
     * Parse the string returned by the {@code qsub} command and extract the job ID string
     *
     * @param text The string returned when submitting the job
     * @return The actual job ID string
     */
    @Override
    def parseJobId( String text ) {
        // return always the last line
        String id
        def lines = text.trim().readLines()
        def entry = lines[-1].trim()
        if( entry ) {
            if( entry.toString().isLong() )
                return entry

            if( entry.startsWith('Your job') && entry.endsWith('has been submitted') && (id=entry.tokenize().get(2)) )
                return id
        }

        throw new IllegalStateException("Invalid SGE submit response:\n$text\n\n")
    }


    @PackageScope
    String getKillCommand() { 'qdel' }

    @Override
    protected List<String> queueStatusCommand(Object queue) {
        def result = ['qstat']
        if( queue )
            result << '-q' << queue.toString()

        return result
    }

    static protected Map DECODE_STATUS = [
            'r': QueueStatus.RUNNING,
            'qw': QueueStatus.PENDING,
            'hqw': QueueStatus.HOLD,
            'Eqw': QueueStatus.ERROR
    ]

    @Override
    protected Map<?, QueueStatus> parseQueueStatus(String text) {

        def result = [:]
        text?.eachLine{ String row, int index ->
            if( index< 2 ) return
            def cols = row.trim().split(/\s+/)
            if( cols.size()>5 ) {
                result.put( cols[0], DECODE_STATUS[cols[4]] )
            }
        }

        return result
    }

}
