![Coverage](https://sonarcloud.io/api/project_badges/measure?metric=coverage&project=Netcracker_qubership-core-process-orchestrator)
![duplicated_lines_density](https://sonarcloud.io/api/project_badges/measure?metric=duplicated_lines_density&project=Netcracker_qubership-core-process-orchestrator)
![vulnerabilities](https://sonarcloud.io/api/project_badges/measure?metric=vulnerabilities&project=Netcracker_qubership-core-process-orchestrator)
![bugs](https://sonarcloud.io/api/project_badges/measure?metric=bugs&project=Netcracker_qubership-core-process-orchestrator)
![code_smells](https://sonarcloud.io/api/project_badges/measure?metric=code_smells&project=Netcracker_qubership-core-process-orchestrator)

# Process Orchestration Framework

A Java-based process orchestration framework that provides task scheduling, execution management, and process flow control capabilities.

## Overview

This framework allows you to define and execute complex process workflows with dependent tasks, timeouts, and state management. It uses a database backend for persistence and provides robust error handling and process control.

## Key Components

### Core Classes

- `ProcessOrchestrator` - Main orchestration engine that manages process execution
- `ProcessDefinition` - Defines process structure and task dependencies 
- `Process` - Handles the execution of process instances
- `TaskExecutorService` - Custom executor service for task management with timeout capabilities

### Data Models

- `ProcessInstanceImpl` - Process instance implementation with state management
- `TaskInstanceImpl` - Task instance implementation with execution state tracking
- `DataContext` - Context storage for process and task data

### Repositories

- `ContextRepository` - Manages persistence of context data
- `ProcessInstanceRepository` - Handles process instance persistence
- `TaskInstanceRepository` - Manages task instance persistence

### Execution Components

- `TaskExecutionWrapper` - Wraps task execution with callback handling
- `TerminateRunnable` - Handles graceful task termination

## Features

- Task dependency management
- Process and task state persistence
- Timeout handling for tasks
- Asynchronous task execution
- Process termination capabilities
- Version control for data consistency
- Context data management
- Error handling and recovery

## Database Schema

The framework uses the following tables:
- `po_context` - Stores context data
- `pe_process_instance` - Stores process instances
- `pe_task_instance` - Stores task instances

