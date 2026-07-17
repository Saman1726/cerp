package io.cerp.workflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Workflow Service – Construction workflow templates, instances, and step management.
 *
 * <p>
 * Per Section 23 (Construction Workflows) of ConstructHub Concept vNext.
 */
@SpringBootApplication
public class WorkflowServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(WorkflowServiceApplication.class, args);
    }
}
