import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable,} from 'rxjs';
import {environment} from 'src/environments/environment';
import {Area, Property, PropertyCreateEdit} from '../dtos/property';

const baseUri = environment.backendUrl + "/api/v1/properties"

@Injectable({
  providedIn: 'root',
})

export class PropertyService {

  constructor(private http: HttpClient) {
  }

  listProperties(): Observable<Property[]> {
    return this.http.get<Property[]>(`${baseUri}`);
  }

  getProperty(id: number): Observable<Property> {
    return this.http.get<Property>(`${baseUri}/${id}`);
  }

  createProperty(createEdit: PropertyCreateEdit): Observable<Property> {
    return this.http.post<Property>(`${baseUri}`, createEdit);
  }

  updateProperty(createEdit: PropertyCreateEdit, id: number): Observable<Property> {
    return this.http.put<Property>(`${baseUri}/${id}/edit`, createEdit);
  }

  deleteProperty(id: number):Observable<Property>{
    return this.http.delete<Property>(`${baseUri}/${id}`)

  }


  /**
   * Looks up an area name from the backend based on postal code and country.
   * @param postalCode The postal code to look up.
   * @param countryCode The 2-letter country code.
   * @returns An Observable of AreaLookupDto containing the area name.
   */
  lookupArea(postalCode: string, countryCode: string): Observable<Area> {
    return this.http.get<Area>(`${baseUri}/lookup-area`, {
      params: { postalCode, countryCode }
    });
  }
}
