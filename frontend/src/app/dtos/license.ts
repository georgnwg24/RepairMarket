export interface License {
  id?: number
  filename?: string;
  description?: string;
  status?: string;
  uploadTime?: string;
  downloadUrl?: string;
  username?: string;
}

export interface LicenseCreateEdit {
  filename: string;
  description?: string;
}

export function convertToCreateEdit(license: License): LicenseCreateEdit {
  return {
    filename: license.filename,
    description: license.description
  }
}
