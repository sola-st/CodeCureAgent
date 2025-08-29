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
  private static final String RESOURCE_CONTROL_CENTER = "control-center";
  private static final String RESOURCE_KAFKA_CONNECT = "kafka-connect";
  private static final String RESOURCE_CONNECTOR = "Connector";
  private static final String RESOURCE_SUBJECT = "Subject";
  private static final String PATTERN_TYPE_LITERAL = PatternType.LITERAL.name();

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
    scope.addResource(RESOURCE_SUBJECT, RESOURCE_SUBJECT + ":" + subject, PATTERN_TYPE_LITERAL);
    scope.build();

    return this;
  }

  public ClusterLevelRoleBuilder forAKafkaConnector(String connector) {
    Map<String, Map<String, String>> clusters =
        client.withClusterIDs().forKafkaConnect().forKafka().asMap();

    scope = new RequestScope();
    scope.setClusters(clusters);
    scope.addResource(RESOURCE_CONNECTOR, RESOURCE_CONNECTOR + ":" + connector, PATTERN_TYPE_LITERAL);
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
    scope.addResource(RESOURCE_CLUSTER, RESOURCE_CONTROL_CENTER, PATTERN_TYPE_LITERAL);
    scope.build();

    return this;
  }

  public ClusterLevelRoleBuilder forKafkaConnect() {
    Map<String, Map<String, String>> clusters =
        client.withClusterIDs().forKafkaConnect().forKafka().asMap();

    scope = new RequestScope();
    scope.setClusters(clusters);
    scope.addResource(RESOURCE_CLUSTER, RESOURCE_KAFKA_CONNECT, PATTERN_TYPE_LITERAL);

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
    scope.addResource(RESOURCE_CLUSTER, RESOURCE_KAFKA_CONNECT, PATTERN_TYPE_LITERAL);

    scope.build();

    return this;
  }

  public RequestScope getScope() {
    return scope;
  }
}