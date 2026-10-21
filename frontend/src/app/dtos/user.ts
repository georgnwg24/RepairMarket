export enum Role {
  CUSTOMER = 'CUSTOMER',
  WORKER = 'WORKER',
  ADMIN = 'ADMIN'
}

/**
 * Represents the comprehensive User object used within the frontend application.
 */
export interface User {
  username?: string;
  email?: string;
  firstName?: string;
  lastName?: string;
  role?: string;
  countryCode?: string;
  postalCode?: string;
  area?: string;
}

/**
 * DTO for sending user profile updates to the backend.
 * Contains all fields that a user is allowed to edit.
 * USERNAME IS NOT INCLUDED as it should not be changed.
 */
export interface UserEdit {
  email?: string;
  firstName?: string;
  lastName?: string;
  countryCode?: string;
  postalCode?: string;
  area?: string;
}

/**
 * Converts the frontend User object to the UserEdit DTO for the update request.
 * @param user The full user object from the component state.
 * @returns A UserEdit object ready to be sent to the API.
 */
export function convertToUpdate(user: User): UserEdit {
  return {
    email: user.email,
    firstName: user.firstName,
    lastName: user.lastName,
    countryCode: user.countryCode,
    postalCode: user.postalCode,
    area: user.area
  };
}

/**
 * DTO for displaying a list of users, for example in an admin panel.
 */
export interface UserListDto {
  id: number;
  username: string;
  email: string;
  role: Role;
  firstName?: string;
  lastName?: string;
  countryCode?: string;
  postalCode?: string;
  area?: string;
  banned: boolean;
}
