export interface Property {
  id?: number;
  countryCode: string;
  postalCode?: string;
  area: string;
  address?: string;
}

export interface PropertyCreateEdit {
  id?: number;
  countryCode?: string;
  postalCode?: string;
  area?: string;
  address?: string;
}

/**
 * Represents the clean JSON response from the backend's
 * GET /lookup-area endpoint.
 * Example: { "areaName": "Wien, Wieden" }
 */
export interface Area {
  areaNames: string[];
}

export function convertToPropertyCreateEdit(property: Property): PropertyCreateEdit {
  return {
    countryCode: property.countryCode,
    postalCode: property.postalCode,
    area: property.area,
    address: property.address
  };
}



