/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shardingsphere.test.integration.env.schema;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.shardingsphere.infra.database.type.DatabaseType;
import org.apache.shardingsphere.test.integration.env.EnvironmentPath;
import org.apache.shardingsphere.test.integration.env.IntegrateTestEnvironment;
import org.apache.shardingsphere.test.integration.env.datasource.builder.ActualDataSourceBuilder;
import org.h2.tools.RunScript;

import javax.sql.DataSource;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;

/**
 * Schema environment manager.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SchemaEnvironmentManager {
    
    /**
     * Get data source names.
     * 
     * @param scenario scenario
     * @return data source names
     * @throws IOException IO exception
     * @throws JAXBException JAXB exception
     */
    public static Collection<String> getDataSourceNames(final String scenario) throws IOException, JAXBException {
        return unmarshal(EnvironmentPath.getDatabasesFile(scenario)).getDatabases();
    }
    
    private static SchemaEnvironment unmarshal(final String schemaEnvironmentConfigFile) throws IOException, JAXBException {
        try (FileReader reader = new FileReader(schemaEnvironmentConfigFile)) {
            return (SchemaEnvironment) JAXBContext.newInstance(SchemaEnvironment.class).createUnmarshaller().unmarshal(reader);
        }
    }
    
    /**
     * Execute init SQLs.
     * 
     * @throws IOException IO exception
     * @throws JAXBException JAXB exception
     * @throws SQLException SQL exception
     */
    public static void executeInitSQLs() throws IOException, JAXBException, SQLException {
        for (String each : IntegrateTestEnvironment.getInstance().getScenarios()) {
            executeInitSQLs(each);
        }
    }
    
    private static void executeInitSQLs(final String scenario) throws IOException, JAXBException, SQLException {
        for (DatabaseType each : IntegrateTestEnvironment.getInstance().getDatabaseEnvironments().keySet()) {
            if ("H2".equals(each.getName())) {
                executeInitSQLForSchemaNotSupportedDatabase(scenario, each);
                return;
            }
            // TODO use multiple threads to improve performance
            DataSource dataSource = ActualDataSourceBuilder.build(null, scenario, each);
            File file = new File(EnvironmentPath.getInitSQLFile(each, scenario));
            executeSQLScript(dataSource, file);
        }
    }
    
    private static void executeInitSQLForSchemaNotSupportedDatabase(final String scenario, final DatabaseType databaseType) throws IOException, JAXBException, SQLException {
        File file = new File(EnvironmentPath.getInitSQLFile(databaseType, scenario));
        for (String each : getDataSourceNames(scenario)) {
            // TODO use multiple threads to improve performance
            DataSource dataSource = ActualDataSourceBuilder.build(each, scenario, databaseType);
            executeSQLScript(dataSource, file);
        }
    }
    
    private static void executeSQLScript(final DataSource dataSource, final File file) throws SQLException, IOException {
        try (Connection connection = dataSource.getConnection();
             FileReader reader = new FileReader(file)) {
            RunScript.execute(connection, reader);
        }
    }
}