import {AfterContentInit, contentChild, DestroyRef, Directive, inject} from '@angular/core';
import {DOCUMENT} from '@angular/common';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {NgbDropdown, NgbDropdownAnchor, NgbDropdownMenu} from '@ng-bootstrap/ng-bootstrap';
import {EMPTY, fromEvent, merge} from 'rxjs';
import {filter, startWith, switchMap} from 'rxjs/operators';

@Directive({
  selector: '[ponyDropdownCloseOnScroll]'
})
export class DropdownCloseOnScrollDirective implements AfterContentInit {

  private readonly dropdown = inject(NgbDropdown, {self: true});
  private readonly document = inject(DOCUMENT);
  private readonly destroyRef = inject(DestroyRef);
  private readonly menu = contentChild.required(NgbDropdownMenu);
  private readonly anchor = contentChild.required(NgbDropdownAnchor);

  ngAfterContentInit(): void {
    this.dropdown.openChange.pipe(
      startWith(this.dropdown.isOpen()),
      switchMap(open => {
        if (!open) {
          return EMPTY;
        }
        const outsidePress = merge(
          fromEvent(this.document, 'mousedown', {capture: true}),
          fromEvent(this.document, 'touchstart', {capture: true}),
        ).pipe(filter(event =>
          !this.menu().nativeElement.contains(event.target as Node) &&
          !this.anchor().nativeElement.contains(event.target as Node)
        ));
        return merge(
          outsidePress,
          fromEvent(this.document.body, 'wheel'),
          fromEvent(this.document.defaultView!, 'resize'),
        );
      }),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe(() => this.dropdown.close());
  }
}
