package com.purbon.kafka.topology.roles.rbac;

import static com.purbon.kafka.topology.api.mds.ClusterIDs.CONNECT_CLUSTER_ID_LABEL;

import com.purbon.kafka.topology.api.mds.MDSApiClient;
import com.purbon.kafka.topology.api.mds.RequestScope;
import com.purbon.kafka.topology.model.users.Connector;
import com.purbon.kafka.topology.roles.TopologyAclBinding;
import java.util.Map;
import java.util.Optional;
import org.apache.kafka.common.resource.PatternType;

public class ClusterLevelRoleBuilder {

  private static final String RESOURCE_CLUSTER = "Cluster";
  private static final String RESOURCE_CONNECTOR = "Connector";
  private static final String RESOURCE_SUBJECT = "Subject";
  private static final String CONTROL_CENTER = "control-center";
  private static final String KAFKA_CONNECT = "kafka-connect";

  private final String principal;
  private final String role;
  private final MDSApiClient client;
  private RequestScope scope;

  public ClusterLevelRoleBuilder(String principal, String role, MDSApiClient client) {
    this.principal = principal;
    this.role = role;
    this.client = client;
    this.scope = new RequestScope();
  }

  public ClusterLevelRoleBuilder forSchemaRegistry() {
    Map<String, Map<String, String>> clusters =
        client.withClusterIDs().forSchemaRegistry().forKafka().asMap();

    scope = new RequestScope();
    scope.setClusters(clusters);
    scope.build();

    return this;
  }

  public ClusterLevelRoleBuilder forSchemaSubject(String subject) {
    Map<String, Map<String, String>> clusters =
        client.withClusterIDs().forSchemaRegistry().forKafka().asMap();

    scope = new RequestScope();
    scope.setClusters(clusters);
    scope.addResource(RESOURCE_SUBJECT, RESOURCE_SUBJECT + ":" + subject, PatternType.LITERAL.name());
    scope.build();

    return this;
  }

  public ClusterLevelRoleBuilder forAKafkaConnector(String connector) {
    Map<String, Map<String, String>> clusters =
        client.withClusterIDs().forKafkaConnect().forKafka().asMap();

    String patternType = PatternType.LITERAL.name();

    scope = new RequestScope();
    scope.setClusters(clusters);
    scope.addResource(RESOURCE_CONNECTOR, RESOURCE_CONNECTOR + ":" + connector, patternType);
    scope.build();

    return this;
  }

  public TopologyAclBinding apply() {

    return client.bindClusterRole(principal, role, scope);
  }

  public ClusterLevelRoleBuilder forKafka() {
    Map<String, Map<String, String>> clusters = client.withClusterIDs().forKafka().asMap();

    scope = new RequestScope();
    scope.setClusters(clusters);
    scope.build();

    return this;
  }

  public ClusterLevelRoleBuilder forControlCenter() {
    Map<String, Map<String, String>> clusters = client.withClusterIDs().forKafka().asMap();

    scope = new RequestScope();
    scope.setClusters(clusters);
    scope.addResource(RESOURCE_CLUSTER, CONTROL_CENTER, PatternType.LITERAL.name());
    scope.build();

    return this;
  }

  public ClusterLevelRoleBuilder forKafkaConnect() {
    Map<String, Map<String, String>> clusters =
        client.withClusterIDs().forKafkaConnect().forKafka().asMap();

    scope = new RequestScope();
    scope.setClusters(clusters);
    scope.addResource(RESOURCE_CLUSTER, KAFKA_CONNECT, PatternType.LITERAL.name());

    scope.build();

    return this;
  }

  public ClusterLevelRoleBuilder forKafkaConnect(Connector connector) {
    Map<String, Map<String, String>> clusters =
        client.withClusterIDs().forKafkaConnect().forKafka().asMap();

    Optional<String> connectClusterIdOptional = connector.getCluster_id();
    connectClusterIdOptional.ifPresent(
        s -> clusters.get("clusters").put(CONNECT_CLUSTER_ID_LABEL, s));

    scope = new RequestScope();
    scope.setClusters(clusters);
    scope.addResource(RESOURCE_CLUSTER, KAFKA_CONNECT, PatternType.LITERAL.name());

    scope.build();

    return this;
  }

  public RequestScope getScope() {
    return scope;
  }
}
