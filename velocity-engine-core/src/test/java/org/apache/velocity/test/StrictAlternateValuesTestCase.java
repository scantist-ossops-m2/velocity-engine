package org.apache.velocity.test;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.VelocityException;
import org.apache.velocity.runtime.RuntimeConstants;

/**
 * Base test case that provides utility methods for
 * the rest of the tests.
 *
 * @author <a href="mailto:dlr@finemaltcoding.com">Daniel Rall</a>
 * @author Nathan Bubna
 * @version $Id: AlternateValuesTestCase.java 1843764 2018-10-13 14:52:28Z cbrisson $
 */
public class StrictAlternateValuesTestCase extends BaseTestCase
{
    public StrictAlternateValuesTestCase(String name)
    {
        super(name);
    }

    @Override
    protected void setUpEngine(VelocityEngine engine)
    {
        engine.setProperty(RuntimeConstants.RUNTIME_REFERENCES_STRICT, Boolean.TRUE);
    }

    protected void setUpContext(VelocityContext context)
    {
        context.put("foo", null);
    }

    public void testDefault()
    {
        assertEvalEquals("<foo>", "<${foo|'foo'}>");
        assertEvalEquals("bar", "#set($bar='bar')${foo|$bar}");
        assertEvalEquals("bar", "#set($bar='bar')${foo|${bar}}");
        assertEvalException ("${foo.bar.baz()[5]|'hop'}", VelocityException.class);
        assertEvalEquals("{foo}", "{${foo|'foo'}}");
        assertEvalException ("$foo", VelocityException.class);
    }

}