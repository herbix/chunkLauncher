package io.chaofan.chunklauncher.auth;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.chaofan.chunklauncher.CharSelectDialog;

/**
 * This class defines a method to login minecraft. The classes in
 * package io.chaofan.chunklauncher.auth which extends ServerAuth will
 * be adds into auth type combo box. You can choose this method
 * like predefined methods.
 * 
 * @author Chaos
 * @since ChunkLauncher 1.3.2
 */
public abstract class ServerAuth {

    private String name;
    private String pass;
    private String accessToken;
    private String playerName;
    private String uuid;
    private String userType;
    private Map<String, Collection<Object>> userProperties = new HashMap<>();

    /**
     * Login user name and password should be passed to this constructor.
     * The class will save these infomation for further use.
     *
     * @param name Login user name
     * @param pass Login password
     */
    public ServerAuth(String name, String pass) {
        this.name = name;
        this.pass = pass;
        this.playerName = name;
        this.uuid = null;
        this.accessToken = "-";
        this.setUserType("legacy");
    }

    /**
     * This session string will be passed to minecraft game.
     * @return The session
     */
    public String getSession() {
        return accessToken;
    }

    /**
     * This session string will be passed to minecraft game.
     * @return The session
     */
    public String getAccessToken() {
        return accessToken;
    }

    /**
     * @return Login user name
     */
    public String getName() {
        return name;
    }

    /**
     * @return Login password
     */
    public String getPass() {
        return pass;
    }

    /**
     * This session string will be passed to minecraft game.
     * @param accessToken The access token you need to set
     *        (default is '-')
     */
    protected void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    /**
     * This session string will be passed to minecraft game.
     * @param session The session token you need to set
     *        (default is '-')
     * @Deprecated use setAccessToken instead
     */
    @Deprecated
    protected void setSession(String session) {
        this.accessToken = session;
    }

    /**
     * The player name is the name showed in game.
     * @param playerName The name you need to set
     *        (default is same as login user name)
     */
    protected void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    /**
     * The player name is the name showed in game.
     * @return The name
     */
    public String getPlayerName() {
        return playerName;
    }

    /**
     * This uuid will passed to minecraft game.
     * @param uuid The uuid you need to set
     *        (default is UUID(0, 0))
     */
    protected void setUuid(String uuid) {
        this.uuid = uuid;
    }

    /**
     * This uuid will passed to minecraft game.
     * If login method doesn't specify a uuid, the method will
     * return a uuid based on player name.
     * @return The uuid
     */
    public String getUuid() {
        if (uuid == null) {
            return UUID.nameUUIDFromBytes(playerName.getBytes()).
                    toString().replace("-", "");
        }
        return uuid;
    }

    /**
     * When login, launcher will call this method. This method
     * will run in a new thread, so you can do anything that spents
     * time. But don't forget to call callback.authDone method
     * when login succeeds or fails.
     *
     * @param callback The interface that contains the callback function
     * @see AuthDoneCallback
     */
    public abstract void login(AuthDoneCallback callback);

    /**
     * You should override this function to change the name of
     * the method.
     * @return The auth type name showed in comboBox
     */
    public static String getAuthTypeName() {
        return "N/A";
    }

    /**
     * You should override this function to change the alias of
     * the method.
     * @return Alias of the auth type. Must be constant
     */
    public static String getAlias() {
        return "N/A";
    }

    /**
     * @param userType User type to be set
     */
    protected void setUserType(String userType) {
        this.userType = userType;
    }

    /**
     * @return Current user type
     */
    public String getUserType() {
        return userType;
    }

    /**
     * Objects saved in collection must be readable by json creator.
     * e.g. Array, Collection, Map, String, and basic types.
     * @return Current user properties
     */
    public Map<String, Collection<Object>> getUserProperties() {
        return userProperties;
    }

    /**
     * @return whether password is inputted in launcher.
     */
    public static boolean canInputPassword() { return true; }

    /**
     * Show a dialog, where user can choose one of his/her characters
     * from a character list.
     *
     * @param chars The character list
     * @return User selection, or <i>null</i> if user press cancel
     */
    @SuppressWarnings("unchecked")
    public <T> T selectFrom(List<T> chars) {
        CharSelectDialog dlg = new CharSelectDialog();
        for(T o : chars) {
            dlg.chars.addItem(o);
        }
        dlg.setVisible(true);
        if(dlg.selected) {
            return (T) dlg.chars.getSelectedItem();
        }
        return null;
    }

}
