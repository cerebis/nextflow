
/*
 * Copyright (c) 2012, the authors.
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

/**
 *
 *  @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */


def x=1;
def y=2
def z=3

def out1 = 1

def task( Closure<String> closure ) {


    closure.delegate = new MyDelegate()
    def result = closure.call()

    println result
}

class MyDelegate implements GroovyObject {

    def method

    Object invokeMethod(String name, Object args) {
        println "method: $name ($args) - ${args.getClass().isArray()}"
        return 'x'
    }

    Object getProperty(String name) {
        return ">>$name<<"
    }

}


task {

    stdin(channelIn)
    stdout(channelOut)

    output( blastResult: 'file_*' )
    output( channelOut: 'file.log')

    def q = 99

    """
    do this ${input 'channel99'} - ${x}
    do that > ${output(channelOut)}
     $id
    do that >> ${output channel2, as: value}

    do that ${shell variable}


    exit $q
    """

}



 task {

     input genomes to x
     output 'files*' to channel

 }