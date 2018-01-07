import {Directive, ElementRef, Input, OnChanges, Renderer2, SimpleChanges} from '@angular/core';
import {ErrorDto} from '../core/error.dto';

@Directive({
  selector: '[ponyError]'
})
export class ErrorDirective implements OnChanges {

  @Input() ponyError: ErrorDto;
  @Input() ponyErrorField: string;
  @Input() ponyIgnoredErrorCodes: string[];

  private errorMessageElement: HTMLElement;

  constructor(private renderer: Renderer2, private elementRef: ElementRef) {
  }

  ngOnChanges(changes: SimpleChanges): void {
    this.clearState();
    const errorMessages = this.fetchErrorMessages().sort();
    if (errorMessages.length > 0) {
      if (this.ponyErrorField) {
        if (errorMessages.length > 1) {
          this.showFieldErrorList(errorMessages);
        } else {
          this.showFieldError(errorMessages[0]);
        }
      } else if (!this.ponyIgnoredErrorCodes || this.ponyIgnoredErrorCodes.indexOf(this.ponyError.code) < 0) {
        this.showGeneralErrorList(errorMessages);
      }
    }
  }

  private showFieldErrorList(errorMessages: string[]) {
    this.renderer.addClass(this.elementRef.nativeElement, 'has-error');
    this.errorMessageElement = document.createElement('ul');
    errorMessages.forEach(message => {
      const itemElement = document.createElement('li');
      itemElement.textContent = message;
      this.renderer.appendChild(this.errorMessageElement, itemElement);
    });
    this.renderer.addClass(this.errorMessageElement, 'help-block');
    this.renderer.appendChild(this.elementRef.nativeElement, this.errorMessageElement);
  }

  private showFieldError(errorMessage: string) {
    this.renderer.addClass(this.elementRef.nativeElement, 'has-error');
    this.errorMessageElement = document.createElement('span');
    this.errorMessageElement.textContent = errorMessage;
    this.renderer.addClass(this.errorMessageElement, 'help-block');
    this.renderer.appendChild(this.elementRef.nativeElement, this.errorMessageElement);
  }

  private showGeneralErrorList(errorMessages: string[]) {
    this.errorMessageElement = document.createElement('div');
    this.renderer.addClass(this.errorMessageElement, 'alert');
    this.renderer.addClass(this.errorMessageElement, 'alert-danger');
    const headerElement = document.createElement('strong');
    headerElement.textContent = 'Errors';
    this.renderer.appendChild(this.errorMessageElement, headerElement);
    const listElement = document.createElement('ul');
    errorMessages.forEach(message => {
      const itemElement = document.createElement('li');
      itemElement.textContent = message;
      this.renderer.appendChild(listElement, itemElement);
    });
    this.renderer.appendChild(this.errorMessageElement, listElement);
    this.renderer.insertBefore(this.elementRef.nativeElement, this.errorMessageElement, this.elementRef.nativeElement.children[0]);
  }

  private clearState() {
    this.renderer.removeClass(this.elementRef.nativeElement, 'has-error');
    if (this.errorMessageElement) {
      this.renderer.removeChild(this.elementRef.nativeElement, this.errorMessageElement);
      this.errorMessageElement = undefined;
    }
  }

  private fetchErrorMessages(): string[] {
    if (this.ponyErrorField) {
      if (this.ponyError && this.ponyError.fieldViolations) {
        return this.ponyError.fieldViolations
          .filter(fieldViolation => fieldViolation.field === this.ponyErrorField)
          .map(fieldViolation => fieldViolation.message);
      }
    } else if (this.ponyError && this.ponyError.message) {
      return [this.ponyError.message];
    }
    return [];
  }
}
