/*
 * Yet Another UserAgent Analyzer
 * Copyright (C) 2013-2020 Niels Basjes
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

module nl.basjes.parse.useragent.yauaa {
    exports nl.basjes.parse.useragent;
    exports nl.basjes.parse.useragent.analyze;

    opens nl.basjes.parse.useragent.analyze to com.esotericsoftware.kryo;
    opens nl.basjes.parse.useragent to org.apache.commons.lang3;

    requires org.slf4j;
    requires org.antlr.antlr4.runtime;
    requires org.apache.commons.text;
    requires org.apache.commons.lang3;
    requires org.apache.commons.collections4;
    requires org.yaml.snakeyaml;
    requires spring.core;

    requires org.apache.httpcomponents.client5.httpclient5;
    requires com.esotericsoftware.kryo;
    requires nl.basjes.collections.prefixmap;
}
