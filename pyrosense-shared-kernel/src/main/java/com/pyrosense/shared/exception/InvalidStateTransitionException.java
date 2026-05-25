package com.pyrosense.shared.exception;

/**
 * Thrown when an entity cannot transition to the requested state.
 */
public class InvalidStateTransitionException extends BusinessException {

    public InvalidStateTransitionException(String entityType, String currentState, String targetState) {
        super(ErrorCode.INVALID_STATE_TRANSITION,
                entityType + " cannot transition from " + currentState + " to " + targetState);
    }
}
