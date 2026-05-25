package com.pyrosense.shared.exception;

/**
 * Thrown when a requested resource does not exist.
 */
public class NotFoundException extends BusinessException {

    private final String resourceType;
    private final Object resourceId;

    public NotFoundException(String resourceType, Object resourceId) {
        super(ErrorCode.NOT_FOUND, resourceType + " not found with id: " + resourceId);
        this.resourceType = resourceType;
        this.resourceId = resourceId;
    }

    public String getResourceType() {
        return resourceType;
    }

    public Object getResourceId() {
        return resourceId;
    }
}
