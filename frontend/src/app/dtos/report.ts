export enum ReportType {
  JOB_REQUEST = 'JOB_REQUEST',
  MESSAGE     = 'MESSAGE'
}

export interface ReportCreate {
  jobRequestId: number;
  type: ReportType;
  reason: string;
}

export interface ReportMessageCreate {
  messageId: number;
  chatId: number;
  reason: string;
}

export interface ReportListDto {
  id: number
  reporterUsername: string;
  targetUsername: string;
  type: ReportType;
  reason: string;
  isOpen: boolean;
  reportedAt: string;
  jobRequestTitle: string;
}

export interface ReportDetail {
  id: number;
  reporterUsername: string;
  targetUsername: string;
  type: ReportType;
  reason: string;
  isOpen: boolean;
  reportedAt: string;

  jobId?: number;
  jobTitle?: string;
  description?: string;
  category?: string;
  status?: string;
  deadline?: string;

  messageId?: number;
  message?: string;
  mediaName?: string;
  mediaUrl?: string;
  timestamp?: string;
}

export interface ReportSummary {
  id: number;
  reporterUsername: string;
  targetUsername: string;
  type: ReportType;
  isOpen: boolean;
  reportedAt: string;
}
