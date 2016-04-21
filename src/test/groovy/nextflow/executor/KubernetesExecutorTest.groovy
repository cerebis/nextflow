package nextflow.executor
import java.nio.file.Files

import nextflow.Session
import nextflow.processor.ProcessConfig
import nextflow.processor.TaskConfig
import nextflow.processor.TaskProcessor
import nextflow.processor.TaskRun
import nextflow.util.CacheHelper
import nextflow.util.MemoryUnit
import spock.lang.Specification
/**
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
class KubernetesExecutorTest extends Specification {

    def 'should return the kill command' () {

        given:
        def executor = [:] as KubernetesExecutor

        expect:
        executor.getKillCommand() == ['kubectl', 'delete', 'job']
        executor.killTaskCommand('nxf-abc') == ['kubectl', 'delete', 'job', 'nxf-abc']
        executor.killTaskCommand(['nxf-123', 'nxf-xyz', 'nxf-7ad']) == ['kubectl', 'delete', 'job', 'nxf-123', 'nxf-xyz', 'nxf-7ad']
    }

    def 'should return the status command'() {

        setup:
        def executor = [:] as KubernetesExecutor

        expect:
        executor.queueStatusCommand(null) == ['kubectl', 'get', 'pods', '-a']
        executor.queueStatusCommand('long') == ['kubectl', 'get', 'pods', '-a']

    }

    def 'should create the kubernetes yaml job descriptor' () {

        given:
        def folder = Files.createTempDirectory('test')
        def executor = [:] as KubernetesExecutor
        def task = new TaskRun(name: 'Hello', workDir: folder, script: 'echo Hello world!')
        task.hash = CacheHelper.hasher('one').hash()
        task.config = new TaskConfig([container: 'ubuntu', cpus: 8, memory: '4GB'])
        task.processor = Mock(TaskProcessor)
        task.processor.getProcessEnvironment() >> [:]
        task.processor.getSession() >> Mock(Session)
        task.processor.getConfig() >> Mock(ProcessConfig)

        /*
         * simple bash run
         */
        when:
        executor.createBashWrapperBuilder(task).build()

        then:
        folder.resolve('.command.run').exists()
        folder.resolve('.command.kube').exists()
        folder.resolve('.command.kube').text == """
                    apiVersion: batch/v1
                    kind: Job
                    metadata:
                      name: nxf-c023b9b90411d8ffcba8dd937c8a75d8
                      labels:
                        app: nextflow
                    spec:
                      template:
                        spec:
                          restartPolicy: Never
                          containers:
                          - name: nxf-c023b9b90411d8ffcba8dd937c8a75d8
                            image: ubuntu
                            command: ["bash", ".command.run"]
                            workingDir: $folder
                            resources:
                              limits:
                                cpu: 8
                                memory: 4096Mi
                              requests:
                                cpu: 8
                                memory: 4096Mi
                            volumeMounts:
                            - mountPath: $folder
                              name: vol-1
                          volumes:
                          - name: vol-1
                            hostPath:
                              path: $folder
                    """

                    .stripIndent().leftTrim()

    }


    def 'should return a proper yaml content' () {


        when:
        def builder = [name: 'basic',image:'ubuntu', cmd:['do','this','and','that'], workDir:'/work/path'] as KubernetesExecutor.YamlBuilder
        def yaml = builder.create()
        then:
        yaml == '''
                apiVersion: batch/v1
                kind: Job
                metadata:
                  name: basic
                  labels:
                    app: nextflow
                spec:
                  template:
                    spec:
                      restartPolicy: Never
                      containers:
                      - name: basic
                        image: ubuntu
                        command: ["do", "this", "and", "that"]
                        workingDir: /work/path
                '''
                .stripIndent().leftTrim()

        when:
        builder = [name:'hello',image:'busybox', cmd:['touch','Hello'], workDir:  '/home/work/dir1', mounts:['vol1':'/home/data/work']] as KubernetesExecutor.YamlBuilder
        yaml = builder.create()
        then:
        yaml == '''
                apiVersion: batch/v1
                kind: Job
                metadata:
                  name: hello
                  labels:
                    app: nextflow
                spec:
                  template:
                    spec:
                      restartPolicy: Never
                      containers:
                      - name: hello
                        image: busybox
                        command: ["touch", "Hello"]
                        workingDir: /home/work/dir1
                        volumeMounts:
                        - mountPath: /home/data/work
                          name: vol1
                      volumes:
                      - name: vol1
                        hostPath:
                          path: /home/data/work
                '''
                .stripIndent().leftTrim()


        when:
        builder = [name:'Test',image:'busybox', cmd:['touch','Hello'], workDir:  '/work/here', mounts:['vol1':'/path1','vol2':'/path2','vol3':'/path3']] as KubernetesExecutor.YamlBuilder
        yaml = builder.create()
        then:
        yaml == '''
                apiVersion: batch/v1
                kind: Job
                metadata:
                  name: Test
                  labels:
                    app: nextflow
                spec:
                  template:
                    spec:
                      restartPolicy: Never
                      containers:
                      - name: Test
                        image: busybox
                        command: ["touch", "Hello"]
                        workingDir: /work/here
                        volumeMounts:
                        - mountPath: /path1
                          name: vol1
                        - mountPath: /path2
                          name: vol2
                        - mountPath: /path3
                          name: vol3
                      volumes:
                      - name: vol1
                        hostPath:
                          path: /path1
                      - name: vol2
                        hostPath:
                          path: /path2
                      - name: vol3
                        hostPath:
                          path: /path3
                '''
                .stripIndent().leftTrim()

    }


    def 'should add resources to yaml descriptor' () {

        when:
        def builder = [cpu:4, name:'busybox', image: 'debian', cmd: ['holy','command'], workDir: '/work/path'] as KubernetesExecutor.YamlBuilder
        def yaml = builder.create()
        then:
        yaml == '''
                apiVersion: batch/v1
                kind: Job
                metadata:
                  name: busybox
                  labels:
                    app: nextflow
                spec:
                  template:
                    spec:
                      restartPolicy: Never
                      containers:
                      - name: busybox
                        image: debian
                        command: ["holy", "command"]
                        workingDir: /work/path
                        resources:
                          limits:
                            cpu: 4
                          requests:
                            cpu: 4
                '''
                .stripIndent().leftTrim()

        when:
        builder = [mem: MemoryUnit.of('1GB'), name:'busybox', image: 'debian', cmd: ['holy','command'], workDir: '/work/path'] as KubernetesExecutor.YamlBuilder
        yaml = builder.create()
        then:
        yaml == '''
                apiVersion: batch/v1
                kind: Job
                metadata:
                  name: busybox
                  labels:
                    app: nextflow
                spec:
                  template:
                    spec:
                      restartPolicy: Never
                      containers:
                      - name: busybox
                        image: debian
                        command: ["holy", "command"]
                        workingDir: /work/path
                        resources:
                          limits:
                            memory: 1024Mi
                          requests:
                            memory: 1024Mi
                '''
                .stripIndent().leftTrim()

        when:
        builder = [cpu: 2, mem: MemoryUnit.of('3GB'), name:'busybox', image: 'debian', cmd: ['holy','command'], workDir: '/work/path', mounts: [vol1: '/path1']] as KubernetesExecutor.YamlBuilder
        yaml = builder.create()
        then:
        yaml == '''
                apiVersion: batch/v1
                kind: Job
                metadata:
                  name: busybox
                  labels:
                    app: nextflow
                spec:
                  template:
                    spec:
                      restartPolicy: Never
                      containers:
                      - name: busybox
                        image: debian
                        command: ["holy", "command"]
                        workingDir: /work/path
                        resources:
                          limits:
                            cpu: 2
                            memory: 3072Mi
                          requests:
                            cpu: 2
                            memory: 3072Mi
                        volumeMounts:
                        - mountPath: /path1
                          name: vol1
                      volumes:
                      - name: vol1
                        hostPath:
                          path: /path1
                '''
                .stripIndent().leftTrim()


    }



    def 'should parse queue output' () {

        given:
        def text = '''
        NAME                                         READY     STATUS             RESTARTS   AGE
        hello-2414436058-mh5nl                       0/1       CrashLoopBackOff   1          13s
        k8s-etcd-127.0.0.1                           1/1       Running            0          16h
        k8s-master-127.0.0.1                         4/4       Running            0          16h
        k8s-proxy-127.0.0.1                          1/1       Running            0          16h
        nxf-074dafc20052cec9cbca352df3032ba7-l2c2x   0/1       Pending            0          16h
        nxf-0e38534bbd7efdb27d301b03528f1de6-5b9js   0/1       Running            0          14h
        nxf-15083267fa92ca622458d282bf37be08-pv5yz   0/1       Completed          0          16h
        '''
                .stripIndent().leftTrim()

        when:
        def executor = [:] as KubernetesExecutor
        def status = executor.parseQueueStatus(text)

        then:
        status['nxf-074dafc20052cec9cbca352df3032ba7'] == AbstractGridExecutor.QueueStatus.PENDING
        status['nxf-0e38534bbd7efdb27d301b03528f1de6'] == AbstractGridExecutor.QueueStatus.RUNNING
        status['nxf-15083267fa92ca622458d282bf37be08'] == AbstractGridExecutor.QueueStatus.DONE
        status.size() == 3

    }
}
