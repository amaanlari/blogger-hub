package com.lari.bloggerhub.model;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Represents a user entity in the <b>Blogger Hub</b> application.
 * <p>
 * This class serves as the user model for storing and managing user data in a MongoDB database.
 * It implements the {@link UserDetails} interface to integrate with Spring Security for
 * authentication and authorization purposes.
 * </p>
 * <p>
 * Each user is assigned a unique identifier and can have roles such as FREE_USER or PREMIUM_USER,
 * which define their access level. The class also includes metadata such as timestamps for
 * creation and updates.
 * </p>
 */
@Document("blog_user")
public class BlogUser implements UserDetails {

    /**
     * Unique identifier for the user, stored as a MongoDB Object ID.
     */
    @Id
    private String id;

    /**
     * Unique username chosen by the user. Used for login and identification.
     */
    private String username;

    /**
     * Unique email address of the user. Used for communication and verification.
     */
    private String email;

    /**
     * Encrypted password of the user. Stored securely using a hashing algorithm.
     */
    private String password;

    /**
     * A short biography or personal description provided by the user.
     */
    private String bio;

    /**
     * URL pointing to the user's profile picture.
     */
    private String profilePicture;

    /**
     * Indicates whether the user's email address has been verified.
     * If {@code true}, the user can perform restricted actions like posting blogs.
     */
    private boolean isVerified;

    /**
     * List of roles assigned to the user, determining access permissions.
     * Default role is {@link Role#FREE_USER}.
     */
    private List<Role> roles;

    /**
     * Timestamp of when the user was created. Automatically populated by MongoDB.
     */
    @CreatedDate
    private Instant createdAt;

    /**
     * Timestamp of the last update made to the user's record. Automatically populated by MongoDB.
     */
    @LastModifiedDate
    private Instant updatedAt;

    /**
     * Default constructor initializing the user with a FREE_USER role.
     */
    public BlogUser() {
        roles = new ArrayList<>();
        roles.add(Role.FREE_USER);
    }

    /**
     * Gets the unique identifier of the user.
     *
     * @return the user's unique identifier.
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the unique identifier of the user.
     *
     * @param id the unique identifier to set.
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Gets the username of the user.
     *
     * @return the user's username.
     */
    public String getUsername() {
        return username;
    }

    /**
     * Sets the username of the user.
     *
     * @param username the username to set.
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Gets the email address of the user.
     *
     * @return the user's email address.
     */
    public String getEmail() {
        return email;
    }

    /**
     * Sets the email address of the user.
     *
     * @param email the email address to set.
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Gets the encrypted password of the user.
     *
     * @return the user's encrypted password.
     */
    public String getPassword() {
        return password;
    }

    /**
     * Sets the encrypted password of the user.
     *
     * @param password the encrypted password to set.
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Gets the biography of the user.
     *
     * @return the user's biography.
     */
    public String getBio() {
        return bio;
    }

    /**
     * Sets the biography of the user.
     *
     * @param bio the biography to set.
     */
    public void setBio(String bio) {
        this.bio = bio;
    }

    /**
     * Gets the URL of the user's profile picture.
     *
     * @return the URL of the user's profile picture.
     */
    public String getProfilePicture() {
        return profilePicture;
    }

    /**
     * Sets the URL of the user's profile picture.
     *
     * @param profilePicture the profile picture URL to set.
     */
    public void setProfilePicture(String profilePicture) {
        this.profilePicture = profilePicture;
    }

    /**
     * Checks if the user's email is verified.
     *
     * @return {@code true} if the email is verified; otherwise {@code false}.
     */
    public boolean isVerified() {
        return isVerified;
    }

    /**
     * Sets the email verification status of the user.
     *
     * @param verified {@code true} if the email is verified; otherwise {@code false}.
     */
    public void setVerified(boolean verified) {
        isVerified = verified;
    }

    /**
     * Gets the roles assigned to the user.
     *
     * @return the list of roles.
     */
    public List<Role> getRoles() {
        return roles;
    }

    /**
     * Sets the roles assigned to the user.
     *
     * @param roles the list of roles to set.
     */
    public void setRoles(List<Role> roles) {
        this.roles = roles;
    }

    /**
     * Gets the timestamp of when the user was created.
     *
     * @return the creation timestamp.
     */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * Sets the timestamp of when the user was created.
     *
     * @param createdAt the creation timestamp to set.
     */
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Gets the timestamp of the last update made to the user's record.
     *
     * @return the last update timestamp.
     */
    public Instant getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Sets the timestamp of the last update made to the user's record.
     *
     * @param updatedAt the last update timestamp to set.
     */
    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * Retrieves the authorities granted to the user based on their roles.
     *
     * <p>
     * This method converts the user's roles into {@link SimpleGrantedAuthority} objects,
     * which Spring Security uses to enforce role-based access control.
     * </p>
     *
     * @return a collection of {@link GrantedAuthority} objects.
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream().map(role -> new SimpleGrantedAuthority(role.name())).toList();
    }

    /**
     * Returns a string representation of the user.
     *
     * @return a string representing the user.
     */
    @Override
    public String toString() {
        return "BlogUser{" +
                "id='" + id + '\'' +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", bio='" + bio + '\'' +
                ", profilePicture='" + profilePicture + '\'' +
                ", isVerified=" + isVerified +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
