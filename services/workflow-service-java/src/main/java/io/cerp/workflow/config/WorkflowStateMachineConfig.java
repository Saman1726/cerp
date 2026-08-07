package io.cerp.workflow.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.cerp.workflow.state.StateMachineEngine;
import io.cerp.workflow.state.WorkflowStateMachines;

/**
 * Wires the Workflow and WorkflowStep state graphs into a single {@link StateMachineEngine} bean.
 */
@Configuration
public class WorkflowStateMachineConfig {

    @Bean
    public StateMachineEngine workflowStateMachineEngine() {
        return new StateMachineEngine(WorkflowStateMachines.allMachines());
    }
}
