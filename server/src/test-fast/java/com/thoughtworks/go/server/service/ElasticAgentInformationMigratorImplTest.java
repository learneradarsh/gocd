/*
 * Copyright 2019 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.update.ReplaceElasticAgentInformationCommand;
import com.thoughtworks.go.domain.Plugin;
import com.thoughtworks.go.plugin.access.elastic.ElasticAgentExtension;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.server.dao.PluginSqlMapDao;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.util.json.JsonHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.HashMap;
import java.util.Map;

import static com.thoughtworks.go.plugin.domain.common.PluginConstants.ELASTIC_AGENT_EXTENSION;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

class ElasticAgentInformationMigratorImplTest {
    @Mock
    private PluginSqlMapDao pluginSqlMapDao;
    @Mock
    private ClusterProfilesService clusterProfilesService;
    @Mock
    private ElasticProfileService elasticProfileService;
    @Mock
    private ElasticAgentExtension elasticAgentExtension;
    @Mock
    private PluginManager pluginManager;
    @Mock
    private GoConfigService goConfigService;

    private ElasticAgentInformationMigratorImpl elasticAgentInformationMigrator;
    private GoPluginDescriptor goPluginDescriptor;
    private String PLUGIN_ID = "plugin-id";

    @BeforeEach
    void setUp() {
        initMocks(this);
        goPluginDescriptor = new GoPluginDescriptor(PLUGIN_ID, "1.0.0", null, "/var/lib", null, false);
        elasticAgentInformationMigrator = new ElasticAgentInformationMigratorImpl(pluginSqlMapDao, clusterProfilesService, elasticProfileService, elasticAgentExtension, pluginManager, goConfigService);
    }

    @Test
    void shouldDoNothingForNonElasticAgentPlugins() {
        when(pluginManager.isPluginOfType(ELASTIC_AGENT_EXTENSION, goPluginDescriptor.id())).thenReturn(false);

        elasticAgentInformationMigrator.migrate(goPluginDescriptor);

        verifyZeroInteractions(goConfigService);
    }

    @Test
    void shouldPerformReplaceElasticAgentInformationCommandToMigrateElasticAgentInformation() {
        Map<String, String> configuration = new HashMap<>();
        configuration.put("k1", "v1");
        configuration.put("k2", "v2");

        when(pluginManager.isPluginOfType(ELASTIC_AGENT_EXTENSION, goPluginDescriptor.id())).thenReturn(true);
        when(pluginSqlMapDao.findPlugin(PLUGIN_ID)).thenReturn(new Plugin(PLUGIN_ID, JsonHelper.toJsonString(configuration)));

        elasticAgentInformationMigrator.migrate(goPluginDescriptor);

        verify(goConfigService, times(1)).updateConfig(any(ReplaceElasticAgentInformationCommand.class), any(Username.class));
    }

    @Test
    void shouldPerformReplaceElasticAgentInformationCommandToMigrateElasticAgentInformationWhenNoPluginSettingsAreConfigured() {
        when(pluginManager.isPluginOfType(ELASTIC_AGENT_EXTENSION, goPluginDescriptor.id())).thenReturn(true);
        when(pluginSqlMapDao.findPlugin(PLUGIN_ID)).thenReturn(new Plugin(PLUGIN_ID, null));

        elasticAgentInformationMigrator.migrate(goPluginDescriptor);

        verify(goConfigService, times(1)).updateConfig(any(ReplaceElasticAgentInformationCommand.class), any(Username.class));
    }
}