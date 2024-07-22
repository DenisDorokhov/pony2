import {HttpErrorResponse} from '@angular/common/http';
import {Observable, throwError} from 'rxjs';

export interface ErrorDto {
  code: ErrorDto.Code;
  message: string;
  arguments: string[];
  fieldViolations: ErrorDto.FieldViolation[];
}

export namespace ErrorDto {

  export enum Code {
    UNEXPECTED = 'UNEXPECTED',
    BAD_REQUEST = 'BAD_RESULT',
    AUTHENTICATION_FAILED = 'AUTHENTICATION_FAILED',
    ACCESS_DENIED = 'ACCESS_DENIED',
    VALIDATION = 'VALIDATION',
    NOT_FOUND = 'NOT_FOUND',
    MAX_UPLOAD_SIZE_EXCEEDED = 'MAX_UPLOAD_SIZE_EXCEEDED',
    CONCURRENT_SCAN = 'CONCURRENT_SCAN',
  }

  export interface FieldViolation {
    field: string;
    code: string;
    message: string;
    arguments: string[];
  }

  export function fieldHasViolation(error: ErrorDto | undefined, fieldRegex: string): boolean {
    if (error) {
      const regex = new RegExp(fieldRegex);
      return error.fieldViolations
        .filter(fieldViolation => fieldViolation ? regex.test(fieldViolation.field) : false)
        .length > 0;
    } else {
      return false;
    }
  }

  export function fromHttpErrorResponse(error: HttpErrorResponse): ErrorDto {
    if (typeof error.error === 'object') {
      return <ErrorDto>error.error;
    } else {
      return <ErrorDto>{
        code: ErrorDto.Code.UNEXPECTED,
        message: 'Unexpected error occurred.',
        arguments: [],
        fieldViolations: [],
      };
    }
  }

  export function observableFromHttpErrorResponse(error: HttpErrorResponse): Observable<never> {
    return throwError(ErrorDto.fromHttpErrorResponse(error));
  }

  export function authenticationFailed(): ErrorDto {
    return {
      'code': ErrorDto.Code.AUTHENTICATION_FAILED,
      'message': 'Authentication failed.',
      'arguments': [],
      'fieldViolations': []
    };
  }
}

export interface PageDto {
  pageIndex: number;
  pageSize: number;
  totalPages: number;
}
