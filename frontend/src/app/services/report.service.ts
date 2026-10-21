import {Injectable} from '@angular/core';
import {HttpClient, HttpParams} from '@angular/common/http';
import {Observable} from 'rxjs';
import {environment} from 'src/environments/environment';
import {ReportCreate, ReportDetail, ReportMessageCreate, ReportListDto} from '../dtos/report';
import {Page} from "../dtos/page";

const baseUri = `${environment.backendUrl}/api/v1/reports`;

@Injectable({
  providedIn: 'root',
})
export class ReportService {
  constructor(private http: HttpClient) {
  }

  /**
   * File a new report.
   * @param report payload for report creation
   */
  create(report: ReportCreate): Observable<ReportDetail> {
    return this.http.post<ReportDetail>(baseUri, report);
  }

  /**
   * File a new report for a chat message
   * @param report payload for report creation
   */
  reportMessage(report: ReportMessageCreate): Observable<ReportDetail> {
    return this.http.post<ReportDetail>(`${baseUri}/messages`, report);
  }

  /**
   * Get all reports filed by the current user.
   */
  getMyReports(): Observable<ReportListDto[]> {
    return this.http.get<ReportListDto[]>(`${baseUri}/me`);
  }

  /**
   * Get all reports in the system (admin access expected).
   */
  getAllReports(): Observable<ReportListDto[]> {
    return this.http.get<ReportListDto[]>(baseUri);
  }

  getPageOfReports(offset: number, limit: number, status: boolean, searchTerm?: string): Observable<Page<ReportListDto>> {
    let params = new HttpParams()
      .set('offset', offset.toString())
      .set('limit', limit.toString())
      .set('status', status.toString());
    if (searchTerm && searchTerm.trim().length) {
      console.log("searchTerm: " + searchTerm + "");
      params = params.set('username', searchTerm.trim());
    }
    return this.http.get<Page<ReportListDto>>(
      `${baseUri}`,  { params });
  }

  /**
   * Get all reports targeting a specific user.
   * @param targetId ID of the user being reported
   */
  getReportsByTarget(targetId: number): Observable<ReportListDto[]> {
    return this.http.get<ReportListDto[]>(`${baseUri}/target/${targetId}`);
  }

  /**
   +   * Get a single report’s detail (full jobRequest fields).
   +   * @param reportId ID of the report
   +   */
  getReportDetail(reportId: number): Observable<ReportDetail> {
    return this.http.get<ReportDetail>(`${baseUri}/${reportId}`);
  }

  /**
   * Close an open report (admin access expected).
   * @param reportId ID of the report to close
   */
  closeReport(reportId: number): Observable<void> {
    return this.http.post<void>(`${baseUri}/${reportId}/close`, null);
  }

  /**
   * Close an open report (admin access expected).
   * @param reportId ID of the report to close
   */
  openReport(reportId: number): Observable<void> {
    return this.http.post<void>(`${baseUri}/${reportId}/open`, null);
  }
}
