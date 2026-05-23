import {Directive, ElementRef, inject, OnInit} from '@angular/core';

@Directive({
  standalone: true,
  selector: '[ponyAutoFocus]'
})
export class AutoFocusDirective implements OnInit {

  private readonly elementRef = inject(ElementRef);

  ngOnInit(): void {
    this.elementRef.nativeElement.focus();
  }
}
