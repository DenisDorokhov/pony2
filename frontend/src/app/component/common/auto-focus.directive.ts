import {Directive, ElementRef, OnInit} from '@angular/core';

@Directive({
  standalone: true,
  selector: '[ponyAutoFocus]'
})
export class AutoFocusDirective implements OnInit {

  constructor(private elementRef: ElementRef) {
  }

  ngOnInit(): void {
    this.elementRef.nativeElement.focus();
  }
}
