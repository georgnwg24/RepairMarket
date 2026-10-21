export interface UserRegistrationDto {
  username: string;
  password: string;
  confirmPassword: string;
  email: string;
  role: Role;
  firstName?: string;
  lastName?: string;
  countryCode?: string;
  postalCode?: string;
  area?: string;
}

export enum Role {
  CUSTOMER = 'CUSTOMER',
  WORKER = 'WORKER',
}
