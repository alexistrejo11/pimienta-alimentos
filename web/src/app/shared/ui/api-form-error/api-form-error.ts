import { Component, input } from '@angular/core';

import { displayApiErrorMessage, type ParsedApiError } from '../../../core/http/parse-api-error';

@Component({
  selector: 'app-api-form-error',
  templateUrl: './api-form-error.html',
})
export class ApiFormErrorComponent {
  readonly error = input<ParsedApiError | null>(null);

  protected message(err: ParsedApiError): string {
    return displayApiErrorMessage(err);
  }
}
