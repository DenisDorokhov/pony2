import {Directive, ElementRef, Input, OnChanges, Renderer2, SimpleChanges} from '@angular/core';
import {ErrorDto} from '../core/error.dto';

@Directive({
  selector: '[ponyError]'
})
export class ErrorDirective implements OnChanges {

  @Input() ponyError: ErrorDto;
  @Input() ponyErrorField: string;

  private errorMessageElement: HTMLElement;

  constructor(private renderer: Renderer2, private elementRef: ElementRef) {
  }

  ngOnChanges(changes: SimpleChanges): void {
    this.clearState();
    const errorMessages = this.fetchErrorMessages();
    if (errorMessages.length > 0) {
      this.renderer.addClass(this.elementRef.nativeElement, 'has-error');
      if (errorMessages.length > 1) {
        this.errorMessageElement = document.createElement('ul');
        errorMessages.forEach(message => {
          const itemElement = document.createElement('li');
          itemElement.textContent = message;
          this.renderer.appendChild(this.errorMessageElement, itemElement);
        });
      } else {
        this.errorMessageElement = document.createElement('span');
        this.errorMessageElement.textContent = errorMessages[0];
      }
      this.renderer.addClass(this.errorMessageElement, 'help-block');
      this.renderer.appendChild(this.elementRef.nativeElement, this.errorMessageElement);
    }
  }

  private clearState() {
    this.renderer.removeClass(this.elementRef.nativeElement, 'has-error');
    if (this.errorMessageElement) {
      this.renderer.removeChild(this.elementRef.nativeElement, this.errorMessageElement);
      this.errorMessageElement = undefined;
    }
  }

  private fetchErrorMessages(): string[] {
    const errorMessages: string[] = [];
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
