package com.prep.taskpulse.contract;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.Error;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SchemaRegistryConfig;
import com.networknt.schema.SpecificationVersion;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

class TaskAssignedContractTest {

    private static final Path CONTRACT_DIR = Path.of("contracts","task-events", "v1");
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static Schema schema;

    @BeforeAll
    static void loadSchema() throws Exception{
       JsonNode schemaNode = OBJECT_MAPPER.readTree
               (Files.readString(CONTRACT_DIR.resolve("task-assigned.schema.json")));

       SchemaRegistryConfig config = SchemaRegistryConfig.builder().formatAssertionsEnabled(true).build();
       SchemaRegistry registry = SchemaRegistry.withDefaultDialect
               (SpecificationVersion.DRAFT_2020_12, builder -> builder.schemaRegistryConfig(config));

       schema = registry.getSchema(schemaNode);
    }

    @Test
    void committedExample_conformsToTaskAssignedV1Schema() throws Exception{
        JsonNode example = OBJECT_MAPPER.readTree
                (Files.readString(CONTRACT_DIR.resolve("task-assigned.example.json")));
        List<Error> errors = schema.validate(example);
        assertThat(errors).isEmpty();
    }
}
