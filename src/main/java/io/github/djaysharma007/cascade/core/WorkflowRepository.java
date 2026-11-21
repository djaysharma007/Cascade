package io.github.djaysharma007.cascade.core;

import java.util.List;
import java.util.Optional;

/**
 * Port interface for workflow persistence.
 */
public interface WorkflowRepository {

    /**
     * Information on a file that failed to parse or validate during load.
     */
    record CorruptWorkflowFile(String fileName, String reason) {}

    /**
     * Saves a workflow atomically.
     *
     * @param workflow workflow to save, never null
     */
    void save(Workflow workflow);

    /**
     * Finds a workflow by its unique identifier.
     *
     * @param id workflow id, never null
     * @return Optional containing the workflow if found
     */
    Optional<Workflow> findById(WorkflowId id);

    /**
     * Retrieves all saved workflows.
     *
     * @return immutable list of all workflows
     */
    List<Workflow> findAll();

    /**
     * Deletes a workflow by its unique identifier.
     *
     * @param id workflow id, never null
     */
    void delete(WorkflowId id);

    /**
     * Returns all files that failed to load, along with failure reasons.
     *
     * @return list of corrupt file descriptors
     */
    List<CorruptWorkflowFile> getCorruptFiles();
}
