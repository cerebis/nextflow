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

package nextflow.script

import groovy.util.logging.Slf4j
import nextflow.Session
/**
 * Defines the script execution context. By default provided the following variables
 * <li>{@code __$session}: the current execution session
 * <li>{@code params}: the parameters map specified by the users on the program CLI using the '--' prefix
 * <li>{@code args}: the list of programs arguments specified on the program CLI
 *
 * <p>
 *     These values cannot be overridden by definition
 *
 * Read more about 'binding variables'
 * http://groovy.codehaus.org/Scoping+and+the+Semantics+of+%22def%22
 *
 */
@Slf4j
class CliBinding extends Binding {

    final private Session session

    def CliBinding(Session session1) {
        super( new ReadOnlyMap( ['__$session':session1, args:[], params:[:]]) )
        this.session = session1
    }

    /**
     * The map of the CLI named parameters
     * @param values
     */
    def void setParams( Map values ) {
        (getVariables() as ReadOnlyMap).force( 'params', new ReadOnlyMap(values)  )
    }

    /**
     * The list of CLI arguments (unnamed)
     * @param values
     */
    def void setArgs( String ... values ) {
        (getVariables() as ReadOnlyMap).force( 'args', values  )
    }

    /**
     * Try to get a value in the current bindings, if does not exist try
     * to fallback on the session environment.
     *
     * @param name
     * @return
     */
    def getVariable( String name ) {

        if ( super.hasVariable(name) ) {
            return super.getVariable(name)
        }
        else if( fallbackMap()?.containsKey(name) ) {
            fallbackMap().get(name)
        }
        else {
            throw new MissingPropertyException(name, getClass())
        }
    }

    /**
     * The fallback session environment
     * */
    private Map fallbackMap() {
        session.config.env as Map
    }



    /**
     * A class for which value cannot be changed
     */
    static class ReadOnlyMap extends LinkedHashMap {

        List<String> readOnlyNames

        /**
         * @param map0 Initial map values
         * @param readOnlyNames1 The list of values that cannot be changed after obj construction,
         *          when {@code null} all the value in the initial map cannot be changed
         *
         */
        ReadOnlyMap( Map map0, List<String> readOnlyNames1 = null  ) {
            super(map0)
            readOnlyNames = new ArrayList( readOnlyNames1 != null ? readOnlyNames1 : map0.keySet() )
        }

        def put(Object name, Object value) {
            final RO = name in readOnlyNames
            if ( !RO ) {
                super.put(name,value)
            }
        }

        def force( Object name, Object value ) {
            super.put(name,value)
        }

    }

}