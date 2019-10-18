package io.chaofan.chunklauncher.auth;

/**
 * 
 * @author Chaos
 * @since ChunkLauncher 1.3.2
 */
public interface AuthDoneCallback {

    /**
     * When login is done, ServerAuth class object should call this
     * method to make the runner do next operations.
     * @param authObject ServerAuth object self
     * @param succeed Whether the login is succeed
     */
    void authDone(ServerAuth authObject, boolean succeed);

}
