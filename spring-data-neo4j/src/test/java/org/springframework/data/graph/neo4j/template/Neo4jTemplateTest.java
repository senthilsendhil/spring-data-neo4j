package org.springframework.data.graph.neo4j.template;

import org.junit.Test;
import org.neo4j.graphdb.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.springframework.data.graph.neo4j.template.Neo4jTemplateTest.Type.HAS;

public class Neo4jTemplateTest extends NeoApiTest {

    enum Type implements RelationshipType {
        HAS
    }

    @Test
    public void testRefNode() {
        Node refNodeById = new Neo4jTemplate(graph).execute(new GraphCallback<Node>() {
            public Node doWithGraph(GraphDatabaseService graph) throws Exception {
                Node refNode = graph.getReferenceNode();
                return graph.getNodeById(refNode.getId());
            }
        });
        assertEquals("same ref node", graph.getReferenceNode(), refNodeById);
    }

    @Test
    public void testSingleNode() {
        final Neo4jOperations template = new Neo4jTemplate(graph);
        template.doInTransaction(new GraphTransactionCallback<Void>() {
            public Void doWithGraph(Status status, GraphDatabaseService graph) throws Exception {
                Node refNode = graph.getReferenceNode();
                // TODO easy API Node node = graph.createNode(Property._("name", "Test"), Property._("size", 100));
                Node node = graph.createNode();
                node.setProperty("name", "Test");
                node.setProperty("size", 100);
                refNode.createRelationshipTo(node, HAS);

                final Relationship toTestNode = refNode.getSingleRelationship(HAS, Direction.OUTGOING);
                final Node nodeByRelationship = toTestNode.getEndNode();
                assertEquals("Test", nodeByRelationship.getProperty("name"));
                assertEquals(100, nodeByRelationship.getProperty("size"));
                return null;
            }
        });
        template.doInTransaction(new GraphTransactionCallback<Void>() {
            public Void doWithGraph(Status status, GraphDatabaseService graph) throws Exception {
                Node refNode = graph.getReferenceNode();
                final Relationship toTestNode = refNode.getSingleRelationship(HAS, Direction.OUTGOING);
                final Node nodeByRelationship = toTestNode.getEndNode();
                assertEquals("Test", nodeByRelationship.getProperty("name"));
                assertEquals(100, nodeByRelationship.getProperty("size"));
                return null;
            }
        });
    }

    @Test
    public void testRollback() {
        final Neo4jOperations template = new Neo4jTemplate(graph);
        template.doInTransaction(new GraphTransactionCallback.WithoutResult() {
            @Override
            public void doWithGraphWithoutResult(Status status, GraphDatabaseService graph) throws Exception {
                Node node = graph.getReferenceNode();
                node.setProperty("test", "test");
                assertEquals("test", node.getProperty("test"));
                status.mustRollback();
            }
        });
        template.execute(new GraphCallback.WithoutResult() {
            public void doWithGraphWithoutResult(final GraphDatabaseService graph) throws Exception {
                Node node = graph.getReferenceNode();
                assertFalse(node.hasProperty("test"));
            }
        });
    }
}
