import {HttpErrorResponse} from '@angular/common/http';
import {Observable} from 'rxjs/Observable';

enum Code {
  UNEXPECTED = 'UNEXPECTED',
  BAD_REQUEST = 'BAD_RESULT',
  AUTHENTICATION_FAILED = 'AUTHENTICATION_FAILED',
  ACCESS_DENIED = 'ACCESS_DENIED',
  VALIDATION = 'VALIDATION',
  NOT_FOUND = 'NOT_FOUND',
  MAX_UPLOAD_SIZE_EXCEEDED = 'MAX_UPLOAD_SIZE_EXCEEDED',
  CONCURRENT_SCAN = 'CONCURRENT_SCAN',
}

class FieldViolation {
  field: string;
  code: string;
  message: string;
  arguments: string[];
}

export class ErrorDto {

  static Code = Code;
  static FieldViolation = FieldViolation;

  code: Code;
  message: string;
  arguments: string[];
  fieldViolations: FieldViolation[];

  static fieldHasViolation(error: ErrorDto, fieldRegex: string): boolean {
    if (!error) {
      return false;
    }
    const regex = new RegExp(fieldRegex);
    return error.fieldViolations
      .filter(fieldViolation => regex.test(fieldViolation.field))
      .length > 0;
  }

  static fromHttpErrorResponse(error: HttpErrorResponse): ErrorDto {
    if (typeof error.error === 'object') {
      return <ErrorDto>error.error;
    } else {
      return <ErrorDto>{code: Code.UNEXPECTED, message: 'Unexpected error occurred.'};
    }
  }

  static observableFromHttpErrorResponse(error: HttpErrorResponse): Observable<any> {
    return Observable.throw(ErrorDto.fromHttpErrorResponse(error));
  }
}
