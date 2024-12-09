package com.lari.bloggerhub.enums;

/**
 * This enumeration represents the possible status values for a user account in the Blogger Hub
 * application.
 *
 * <p>The account status can be one of the following values: ACTIVE, INACTIVE, SUSPENDED, or DELETED.
 */
public enum AccountStatus {
  /** The account is active and the user can log in and access the application. */
  ACTIVE,
  /** The account is inactive and the user cannot log in or access the application. */
  INACTIVE,
  /** The account is suspended and the user cannot log in or access the application temporarily. */
  SUSPENDED,
  /** The account is deleted and the user data is removed from the application. */
  DELETED
}
