package io.github.lemostic.toolsuite.modules.devops.deploy.repository;

/**
 * Repository 层异常
 */
public class RepositoryException extends RuntimeException {
    
    public RepositoryException(String message) {
        super(message);
    }
    
    public RepositoryException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public RepositoryException(Throwable cause) {
        super(cause);
    }
}
